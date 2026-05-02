package com.pollnet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PollnetApplication {

    public static void main(String[] args) {
        SpringApplication.run(PollnetApplication.class, args);
    }
}
