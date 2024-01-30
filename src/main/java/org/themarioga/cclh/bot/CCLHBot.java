package org.themarioga.cclh.bot;

import jakarta.transaction.Transactional;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.themarioga.cclh.bot.app.intf.ApplicationService;
import org.themarioga.cclh.commons.exceptions.ApplicationException;

@SpringBootApplication(scanBasePackages = {"org.themarioga.cclh"})
@EntityScan(basePackages = {"org.themarioga.cclh"})
public class CCLHBot {

    public static void main(String[] args) {
        SpringApplication.run(CCLHBot.class, args);
    }

    @Bean
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ApplicationException.class)
    public CommandLineRunner commandLineRunner(ApplicationService botService) {
        return args -> botService.run();
    }

}