package com.example.doclayout;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DocLayoutApplication {
    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(DocLayoutApplication.class);
        application.setHeadless(false);
        application.run(args);
    }
}
