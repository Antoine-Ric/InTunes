package com.situnes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SiTunesApplication {
    public static void main(String[] args) {
        SpringApplication.run(SiTunesApplication.class, args);
    }
}
