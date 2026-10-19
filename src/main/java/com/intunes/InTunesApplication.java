package com.intunes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class InTunesApplication {
    public static void main(String[] args) {
        SpringApplication.run(InTunesApplication.class, args);
    }
}
