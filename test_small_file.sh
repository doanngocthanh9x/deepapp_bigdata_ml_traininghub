#!/bin/bash
# Test with small file (< 1MB) using direct base64 transfer

echo "Creating test PDF (small size)..."
cat > /tmp/test.pdf << 'EOF'
%PDF-1.4
1 0 obj
<<
/Type /Catalog
/Pages 2 0 R
>>
endobj
2 0 obj
<<
/Type /Pages
/Kids [3 0 R]
/Count 1
>>
endobj
3 0 obj
<<
/Type /Page
/Parent 2 0 R
/MediaBox [0 0 595 842]
/Contents 4 0 R
>>
endobj
4 0 obj
<<
/Length 44
>>
stream
BT
/F1 12 Tf
100 700 Td
(Test Document) Tj
ET
endstream
endobj
xref
0 5
0000000000 65535 f 
0000000009 00000 n 
0000000058 00000 n 
0000000115 00000 n 
0000000214 00000 n 
trailer
<<
/Size 5
/Root 1 0 R
>>
startxref
308
%%EOF
EOF

FILE_SIZE=$(stat -c%s /tmp/test.pdf)
echo "Test file size: $FILE_SIZE bytes"

if [ $FILE_SIZE -gt 1048576 ]; then
    echo "ERROR: File too large! Must be < 1MB"
    exit 1
fi

echo ""
echo "Testing document processing with small file..."
curl -X POST http://localhost:8080/ZZ/A0/ZZA0_0100/stream \
  -H "Content-Type: multipart/form-data" \
  -F "file=@/tmp/test.pdf" \
  --no-buffer \
  2>&1 | head -100

echo ""
echo "Test complete!"
