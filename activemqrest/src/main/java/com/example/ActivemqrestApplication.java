package com.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ImportResource;

@SpringBootApplication
@ComponentScan(basePackages = {"com.example"})
public class ActivemqrestApplication {

	public static void main(String[] args) {
		SpringApplication.run(ActivemqrestApplication.class, args);
	}

}
