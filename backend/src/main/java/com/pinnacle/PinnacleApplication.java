package com.pinnacle;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PinnacleApplication {
    public static void main(String[] args) {
        SpringApplication.run(PinnacleApplication.class, args);
    }
}
