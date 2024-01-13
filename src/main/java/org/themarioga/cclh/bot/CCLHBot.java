package org.themarioga.cclh.bot;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.themarioga.cclh.bot.services.intf.ApplicationService;

@SpringBootApplication(scanBasePackages = {"org.themarioga.cclh"})
public class CCLHBot {

    public static void main(String[] args) {
        SpringApplication.run(CCLHBot.class, args);
    }

    @Bean
    public CommandLineRunner commandLineRunner(ApplicationService botService) {
        return args -> botService.run();
    }

}