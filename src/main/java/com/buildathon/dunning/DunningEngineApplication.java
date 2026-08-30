package com.buildathon.dunning;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DunningEngineApplication {

    public static void main(String[] args) {
        SpringApplication.run(DunningEngineApplication.class, args);
    }
}
