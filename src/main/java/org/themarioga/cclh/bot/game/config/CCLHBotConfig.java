package org.themarioga.cclh.bot.game.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.updates.SetWebhook;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.themarioga.bot.service.impl.LongPollingBotServiceImpl;
import org.themarioga.bot.service.impl.WebhookBotServiceImpl;
import org.themarioga.bot.service.intf.ApplicationService;
import org.themarioga.bot.util.BotUtils;

@Configuration
public class CCLHBotConfig {

	private final Logger logger = LoggerFactory.getLogger(CCLHBotConfig.class);

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

	@Bean("cclhGameBot")
	@DependsOn({"telegramBotsApiLongPolling", "cclhBotApplicationService"})
	@ConditionalOnProperty(prefix = "cclh.bot", name="type", havingValue = "longpolling")
	public LongPollingBotServiceImpl longPollingBotService(
			@Qualifier("telegramBotsApiLongPolling") TelegramBotsApi telegramBotsApi,
			@Qualifier("cclhBotApplicationService") ApplicationService applicationService) {
		logger.info("Iniciando bot longpolling...");

		LongPollingBotServiceImpl longPollingBotService = new LongPollingBotServiceImpl(token, name, applicationService);

		try {
			telegramBotsApi.registerBot(longPollingBotService);
		} catch (TelegramApiException e) {
			logger.error(e.getMessage(), e);
		}

		return longPollingBotService;
	}

	// Webhook instantiation

	@Bean("cclhGameBot")
	@DependsOn({"telegramBotsApiWebhook", "cclhBotApplicationService"})
	@ConditionalOnProperty(prefix = "cclh.bot", name="type", havingValue = "webhook")
	public WebhookBotServiceImpl webhookBotService(
			@Qualifier("telegramBotsApiWebhook") TelegramBotsApi telegramBotsApi,
			@Qualifier("cclhBotApplicationService") ApplicationService applicationService) {
		logger.info("Iniciando bot webhook en la url {}...", webhookURL);

		WebhookBotServiceImpl webhookBotService = new WebhookBotServiceImpl(token, name, path, applicationService);

		SetWebhook.SetWebhookBuilder webhookBuilder = SetWebhook.builder().url(webhookURL);

		InputFile certificate = BotUtils.getCertificate(webhookCertPath);
		if (certificate != null) {
			logger.info("Sending certificate {}...", webhookCertPath);
			webhookBuilder.certificate(certificate);
		}

		try {
			telegramBotsApi.registerBot(webhookBotService, webhookBuilder.build());
		} catch (TelegramApiException e) {
			logger.error(e.getMessage(), e);
		}

		return webhookBotService;
	}

}
