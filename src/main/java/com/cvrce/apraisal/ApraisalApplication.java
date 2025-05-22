package com.cvrce.apraisal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ApraisalApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApraisalApplication.class, args);
	}

}
