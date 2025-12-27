package com.deepapp.vn.io;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import java.io.File;

@SpringBootApplication
@EnableScheduling
public class DeepappMainApplication {

	public static void main(String[] args) {
		// Ensure required directories exist before starting Spring
		ensureDirectories();
		
		SpringApplication.run(DeepappMainApplication.class, args);
	}
	
	private static void ensureDirectories() {
		// Create SQLite database directory
		File dbDir = new File("/tmp/deepapp");
		if (!dbDir.exists()) {
			boolean created = dbDir.mkdirs();
			if (created) {
				System.out.println("Created database directory: " + dbDir.getAbsolutePath());
			} else {
				System.err.println("Failed to create database directory: " + dbDir.getAbsolutePath());
			}
		}
		
		// Create uploads directory
		File uploadDir = new File("/tmp/deepapp/uploads");
		if (!uploadDir.exists()) {
			boolean created = uploadDir.mkdirs();
			if (created) {
				System.out.println("Created uploads directory: " + uploadDir.getAbsolutePath());
			} else {
				System.err.println("Failed to create uploads directory: " + uploadDir.getAbsolutePath());
			}
		}
	}

}
