package org.themarioga.cclh.bot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;

@SpringBootApplication(scanBasePackages = {"org.themarioga.cclh"}, exclude = { SecurityAutoConfiguration.class })
@EntityScan(basePackages = {"org.themarioga.cclh"})
public class CCLHBot {

    public static void main(String[] args) {
        SpringApplication.run(CCLHBot.class, args);
    }

}