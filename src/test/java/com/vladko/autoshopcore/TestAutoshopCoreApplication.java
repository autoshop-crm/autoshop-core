package com.vladko.autoshopcore;

import org.springframework.boot.SpringApplication;

public class TestAutoshopCoreApplication {

    public static void main(String[] args) {
        SpringApplication.from(AutoshopCoreApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
