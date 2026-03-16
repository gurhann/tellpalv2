package com.tellpal.v2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.modulith.Modulithic;

@SpringBootApplication
@Modulithic
public class TellPalApplication {

    public static void main(String[] args) {
        SpringApplication.run(TellPalApplication.class, args);
    }
}
