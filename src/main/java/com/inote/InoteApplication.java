package com.inote;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class InoteApplication {

    public static void main(String[] args) {
        SpringApplication.run(InoteApplication.class, args);
    }

}
