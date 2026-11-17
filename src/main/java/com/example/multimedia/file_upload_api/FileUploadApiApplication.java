package com.example.multimedia.file_upload_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // drives EscalationService's hourly SLA-breach reminder job
public class FileUploadApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(FileUploadApiApplication.class, args);
	}

}
