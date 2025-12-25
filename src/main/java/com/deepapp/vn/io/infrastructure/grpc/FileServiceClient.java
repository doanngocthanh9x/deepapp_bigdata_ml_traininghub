package com.deepapp.vn.io.infrastructure.grpc;

import com.deepapp.hub.FileChunk;
import com.deepapp.hub.FileServiceGrpc;
import com.deepapp.hub.UploadResponse;
import com.deepapp.hub.DownloadRequest;
import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * FileService client for uploading/downloading files to/from gRPC Hub
 * Uses streaming to handle large files efficiently
 */
public class FileServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(FileServiceClient.class);
    private static final int DEFAULT_CHUNK_SIZE = 32 * 1024; // 32KB chunks
    private static final int UPLOAD_TIMEOUT_SECONDS = 300; // 5 minutes

    private final ManagedChannel channel;
    private final FileServiceGrpc.FileServiceStub asyncStub;

    public FileServiceClient(ManagedChannel channel) {
        this.channel = channel;
        this.asyncStub = FileServiceGrpc.newStub(channel);
    }

    /**
     * Upload file to gRPC Hub with streaming
     * 
     * @param fileData File content as byte array
     * @param filename Original filename
     * @return CompletableFuture with file ID from server
     */
    public CompletableFuture<String> uploadFile(byte[] fileData, String filename) {
        return uploadFile(fileData, filename, DEFAULT_CHUNK_SIZE);
    }

    /**
     * Upload file to gRPC Hub with streaming and custom chunk size
     * 
     * @param fileData File content as byte array
     * @param filename Original filename
     * @param chunkSize Size of each chunk in bytes
     * @return CompletableFuture with file ID from server
     */
    public CompletableFuture<String> uploadFile(byte[] fileData, String filename, int chunkSize) {
        CompletableFuture<String> future = new CompletableFuture<>();
        
        logger.info("Starting file upload: {} ({} bytes, chunk size: {})", 
                    filename, fileData.length, chunkSize);

        final String[] fileId = new String[1];
        final CountDownLatch latch = new CountDownLatch(1);
        final Exception[] error = new Exception[1];

        // Response observer to receive upload result
        StreamObserver<UploadResponse> responseObserver = new StreamObserver<UploadResponse>() {
            @Override
            public void onNext(UploadResponse response) {
                if (response.getSuccess()) {
                    fileId[0] = response.getFileId();
                    logger.info("File uploaded successfully: {} -> {}", 
                                response.getOriginalFilename(), response.getFileId());
                } else {
                    error[0] = new IOException("Upload failed: " + response.getMessage());
                }
            }

            @Override
            public void onError(Throwable t) {
                logger.error("Upload error for {}: {}", filename, t.getMessage());
                error[0] = new RuntimeException("Upload error: " + t.getMessage(), t);
                latch.countDown();
            }

            @Override
            public void onCompleted() {
                logger.debug("Upload stream completed for {}", filename);
                latch.countDown();
            }
        };

        // Request observer to stream file chunks
        StreamObserver<FileChunk> requestObserver = asyncStub.upload(responseObserver);

        try {
            // Stream file in chunks
            ByteArrayInputStream inputStream = new ByteArrayInputStream(fileData);
            byte[] buffer = new byte[chunkSize];
            int chunkNumber = 0;
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                FileChunk.Builder chunkBuilder = FileChunk.newBuilder()
                    .setData(ByteString.copyFrom(buffer, 0, bytesRead))
                    .setChunkNumber(chunkNumber);

                // First chunk includes filename and total size
                if (chunkNumber == 0) {
                    chunkBuilder.setFilename(filename)
                               .setTotalSize(fileData.length);
                }

                FileChunk chunk = chunkBuilder.build();
                requestObserver.onNext(chunk);
                
                chunkNumber++;
                
                // Add small delay every 50 chunks to avoid flow control issues
                if (chunkNumber % 50 == 0) {
                    try {
                        Thread.sleep(10); // 10ms delay every 50 chunks
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Upload interrupted", e);
                    }
                }
                
                if (chunkNumber % 100 == 0) {
                    logger.debug("Uploaded {} chunks ({} KB) for {}", 
                                chunkNumber, (chunkNumber * chunkSize) / 1024, filename);
                }
            }

            // Mark end of stream
            requestObserver.onCompleted();
            logger.info("All {} chunks sent for {}", chunkNumber, filename);

            // Wait for response
            if (!latch.await(UPLOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                future.completeExceptionally(
                    new IOException("Upload timeout after " + UPLOAD_TIMEOUT_SECONDS + " seconds"));
                return future;
            }

            if (error[0] != null) {
                future.completeExceptionally(error[0]);
            } else if (fileId[0] != null) {
                future.complete(fileId[0]);
            } else {
                future.completeExceptionally(new IOException("No file ID received from server"));
            }

        } catch (Exception e) {
            logger.error("Upload exception for {}: {}", filename, e.getMessage(), e);
            requestObserver.onError(e);
            future.completeExceptionally(e);
        }

        return future;
    }

    /**
     * Download file from gRPC Hub
     * 
     * @param fileId File ID returned from upload
     * @return CompletableFuture with file content as byte array
     */
    public CompletableFuture<byte[]> downloadFile(String fileId) {
        CompletableFuture<byte[]> future = new CompletableFuture<>();
        
        logger.info("Starting file download: {}", fileId);

        DownloadRequest request = DownloadRequest.newBuilder()
            .setFileId(fileId)
            .build();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        final Exception[] error = new Exception[1];
        final CountDownLatch latch = new CountDownLatch(1);

        StreamObserver<FileChunk> responseObserver = new StreamObserver<FileChunk>() {
            private int chunkCount = 0;

            @Override
            public void onNext(FileChunk chunk) {
                try {
                    outputStream.write(chunk.getData().toByteArray());
                    chunkCount++;
                    
                    if (chunkCount % 100 == 0) {
                        logger.debug("Downloaded {} chunks for {}", chunkCount, fileId);
                    }
                } catch (IOException e) {
                    logger.error("Error writing chunk for {}: {}", fileId, e.getMessage());
                    error[0] = e;
                }
            }

            @Override
            public void onError(Throwable t) {
                logger.error("Download error for {}: {}", fileId, t.getMessage());
                error[0] = new RuntimeException("Download error: " + t.getMessage(), t);
                latch.countDown();
            }

            @Override
            public void onCompleted() {
                logger.info("Download completed: {} ({} chunks, {} bytes)", 
                           fileId, chunkCount, outputStream.size());
                latch.countDown();
            }
        };

        asyncStub.download(request, responseObserver);

        // Wait in background thread
        CompletableFuture.runAsync(() -> {
            try {
                if (!latch.await(UPLOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                    future.completeExceptionally(
                        new IOException("Download timeout after " + UPLOAD_TIMEOUT_SECONDS + " seconds"));
                    return;
                }

                if (error[0] != null) {
                    future.completeExceptionally(error[0]);
                } else {
                    future.complete(outputStream.toByteArray());
                }
            } catch (InterruptedException e) {
                future.completeExceptionally(e);
            }
        });

        return future;
    }
}
