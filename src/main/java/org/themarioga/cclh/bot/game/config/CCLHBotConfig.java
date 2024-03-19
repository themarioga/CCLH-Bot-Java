package org.themarioga.cclh.bot.game.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.themarioga.bot.service.impl.LongPollingBotServiceImpl;
import org.themarioga.bot.service.impl.WebhookBotServiceImpl;
import org.themarioga.bot.service.intf.ApplicationService;
import org.themarioga.bot.util.BotUtils;

@Configuration
public class CCLHBotConfig {

	@Value("${cclh.bot.enabled}")
	private Boolean enabled;

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
	@ConditionalOnProperty(prefix = "telegram.bot", name="type", havingValue = "longpolling")
	public LongPollingBotServiceImpl longPollingBotService(
			@Qualifier("telegramBotsApiLongPolling") TelegramBotsApi telegramBotsApi,
			@Qualifier("cclhBotApplicationService") ApplicationService applicationService) {
		return BotUtils.createLongPollingBot(enabled, token, name, telegramBotsApi, applicationService);
	}

	// Webhook instantiation

	@Bean("cclhGameBot")
	@DependsOn({"telegramBotsApiWebhook", "cclhBotApplicationService"})
	@ConditionalOnProperty(prefix = "telegram.bot", name="type", havingValue = "webhook")
	public WebhookBotServiceImpl webhookBotService(
			@Qualifier("telegramBotsApiWebhook") TelegramBotsApi telegramBotsApi,
			@Qualifier("cclhBotApplicationService") ApplicationService applicationService) {
		return BotUtils.createWebhookBot(enabled, token, name, path, webhookURL, webhookCertPath, telegramBotsApi, applicationService);
	}

}
