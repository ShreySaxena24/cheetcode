package com.shrary.cheetcode;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CheetcodeApplication {

	public static void main(String[] args) {
		SpringApplication.run(CheetcodeApplication.class, args);
	}

}
