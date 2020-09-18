package com.wonders;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class FileWatchApplication {

	public static void main(String[] args) throws Exception {
		SpringApplication.run(FileWatchApplication.class, args);
	}

}
