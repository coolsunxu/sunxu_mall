package com.example.sunxu_mall;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@EnableRetry
@SpringBootApplication
public class SunxuMallApplication {

    public static void main(String[] args) {
        SpringApplication.run(SunxuMallApplication.class, args);
    }

}
