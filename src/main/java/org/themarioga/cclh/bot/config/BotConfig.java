package org.themarioga.cclh.bot.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.updates.SetWebhook;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.LongPollingBot;
import org.telegram.telegrambots.meta.generics.Webhook;
import org.telegram.telegrambots.meta.generics.WebhookBot;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import org.telegram.telegrambots.updatesreceivers.ServerlessWebhook;
import org.themarioga.cclh.bot.app.impl.LongPollingBotServiceImpl;
import org.themarioga.cclh.bot.app.impl.WebhookBotServiceImpl;
import org.themarioga.cclh.bot.app.intf.ApplicationService;
import org.themarioga.cclh.bot.util.BotUtils;

@Configuration
public class BotConfig {

	@Value("${cclh.bot.token}")
	private String token;

	@Value("${cclh.bot.name}")
	private String name;

	@Value("${cclh.bot.path}")
	private String path;

	@Value("${cclh.bot.webhook.url}")
	private String webhookURL;

	@Value("${cclh.bot.webhook.cert.path}")
	private String webhookCertPath;

	// Long polling instantiation

	@Bean
	@DependsOn("applicationServiceImpl")
	@ConditionalOnProperty(prefix = "cclh.bot", name="type", havingValue = "longpolling")
	public LongPollingBotServiceImpl longPollingBotService(ApplicationService applicationService) {
		return new LongPollingBotServiceImpl(token, name, applicationService);
	}

	@Bean
	@ConditionalOnMissingBean(TelegramBotsApi.class)
	@ConditionalOnProperty(prefix = "cclh.bot", name="type", havingValue = "longpolling")
	public TelegramBotsApi telegramBotsApiDev(LongPollingBot longPollingBot) throws TelegramApiException {
		TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
		telegramBotsApi.registerBot(longPollingBot);

		return telegramBotsApi;
	}

	// Pro instantiation

	@Bean
	@DependsOn("applicationServiceImpl")
	@ConditionalOnProperty(prefix = "cclh.bot", name="type", havingValue = "webhook")
	public WebhookBotServiceImpl webhookBotService(ApplicationService applicationService) {
		return new WebhookBotServiceImpl(token, name, path, applicationService);
	}

	@Bean
	@ConditionalOnProperty(prefix = "cclh.bot", name="type", havingValue = "webhook")
	public Webhook serverlessWebhook() {
		return new ServerlessWebhook();
	}

	@Bean
	@DependsOn({"serverlessWebhook", "webhookBotService"})
	@ConditionalOnMissingBean(TelegramBotsApi.class)
	@ConditionalOnProperty(prefix = "cclh.bot", name="type", havingValue = "webhook")
	public TelegramBotsApi telegramBotsApiPro(Webhook webhook, WebhookBot webhookBotService) throws TelegramApiException {
		SetWebhook.SetWebhookBuilder webhookBuilder = SetWebhook.builder().url(webhookURL);

		InputFile certificate = BotUtils.getCertificate(webhookCertPath);
		if (certificate != null) {
			webhookBuilder.certificate(certificate);
		}

		TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class, webhook);
		telegramBotsApi.registerBot(webhookBotService, webhookBuilder.build());

		return telegramBotsApi;
	}

}
