package com.festops;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for FestOps — an incident management and dispatch platform
 * for a cultural festival.
 */
@SpringBootApplication
public class FestOpsApplication {

    public static void main(String[] args) {
        SpringApplication.run(FestOpsApplication.class, args);
    }
}
