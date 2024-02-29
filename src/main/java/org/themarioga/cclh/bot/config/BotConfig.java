package org.themarioga.cclh.bot.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.Webhook;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import org.telegram.telegrambots.updatesreceivers.ServerlessWebhook;

@Configuration
public class BotConfig {

	// Long polling instantiation

	@Bean("telegramBotsApiLongPolling")
	@ConditionalOnMissingBean(TelegramBotsApi.class)
	@ConditionalOnProperty(prefix = "cclh.bot", name="type", havingValue = "longpolling")
	public TelegramBotsApi telegramBotsApiLongPolling() throws TelegramApiException {
		return new TelegramBotsApi(DefaultBotSession.class);
	}

	// Webhook instantiation

	@Bean
	@ConditionalOnProperty(prefix = "cclh.bot", name="type", havingValue = "webhook")
	public Webhook serverlessWebhook() {
		return new ServerlessWebhook();
	}

	@Bean("telegramBotsApiWebhook")
	@DependsOn({"serverlessWebhook", "webhookBotService"})
	@ConditionalOnMissingBean(TelegramBotsApi.class)
	@ConditionalOnProperty(prefix = "cclh.bot", name="type", havingValue = "webhook")
	public TelegramBotsApi telegramBotsApiWebhook(Webhook webhook) throws TelegramApiException {
		return new TelegramBotsApi(DefaultBotSession.class, webhook);
	}

}
