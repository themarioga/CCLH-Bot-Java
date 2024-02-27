package org.themarioga.cclh.bot.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Profile;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.updates.SetWebhook;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.Webhook;
import org.telegram.telegrambots.meta.generics.WebhookBot;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import org.telegram.telegrambots.updatesreceivers.ServerlessWebhook;
import org.themarioga.cclh.bot.app.impl.LongPollingBotServiceImpl;
import org.themarioga.cclh.bot.app.impl.WebhookBotServiceImpl;
import org.themarioga.cclh.bot.app.intf.ApplicationService;
import org.themarioga.cclh.bot.app.intf.BotService;

import java.io.File;

@Configuration
public class BotConfig {

	@Value("${cclh.bot.token}")
	private String token;

	@Value("${cclh.bot.name}")
	private String name;

	@Value("${cclh.bot.path}")
	private String path;

	@Value("${cclh.bot.webhook.url:#{null}}")
	private String webhookURL;

	@Value("${cclh.bot.webhook.path:#{null}}")
	private String webhookPath;

	// Dev instantiation

	@Bean
	@Profile("dev")
	@DependsOn("applicationServiceImpl")
	public BotService longPollingBotService(ApplicationService applicationService) {
		return new LongPollingBotServiceImpl(token, name, applicationService);
	}

	@Bean
	@Profile("dev")
	@ConditionalOnMissingBean(TelegramBotsApi.class)
	public TelegramBotsApi telegramBotsApiDev() throws TelegramApiException {
		return new TelegramBotsApi(DefaultBotSession.class);
	}

	// Pro instantiation

	@Bean
	@Profile("pro")
	@DependsOn("applicationServiceImpl")
	public WebhookBotServiceImpl webhookBotService(ApplicationService applicationService) {
		return new WebhookBotServiceImpl(token, name, path, applicationService);
	}

	@Bean
	@Profile("pro")
	public Webhook serverlessWebhook() {
		return new ServerlessWebhook();
	}

	@Bean
	@Profile("pro")
	@DependsOn({"serverlessWebhook", "webhookBotService"})
	@ConditionalOnMissingBean(TelegramBotsApi.class)
	public TelegramBotsApi telegramBotsApiPro(Webhook webhook, WebhookBot webhookBotService) throws TelegramApiException {
		TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class, webhook);
		telegramBotsApi.registerBot(webhookBotService, SetWebhook.builder().url(webhookURL).certificate(new InputFile(new File(webhookPath))).build());

		return telegramBotsApi;
	}

}
