package com.deepapp.vn.io;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DeepappMainApplication {

	public static void main(String[] args) {
		SpringApplication.run(DeepappMainApplication.class, args);
	}

}
