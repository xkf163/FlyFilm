package com.fly;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;

@SpringBootApplication
@ServletComponentScan(basePackages = "com.fly.*")
public class FilmApplication {

	public static void main(String[] args) {

		SpringApplication.run(FilmApplication.class, args);

	}
}
