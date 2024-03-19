package org.themarioga.cclh.bot.dictionaries.config;

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
import org.themarioga.bot.util.BotCreationUtils;

@Configuration
public class DictionariesBotConfig {

	@Value("${dictionaries.bot.enabled}")
	private Boolean enabled;

	@Value("${dictionaries.bot.token}")
	private String token;

	@Value("${dictionaries.bot.name}")
	private String name;

	@Value("${dictionaries.bot.path}")
	private String path;

	@Value("${dictionaries.bot.webhook.url}")
	private String webhookURL;

	@Value("${dictionaries.bot.webhook.cert.path}")
	private String webhookCertPath;

	// Long polling instantiation

	@Bean("dictionariesBot")
	@DependsOn({"telegramBotsApiLongPolling", "dictionariesBotApplicationService"})
	@ConditionalOnProperty(prefix = "telegram.bot", name="type", havingValue = "longpolling")
	public LongPollingBotServiceImpl longPollingBotService(
			@Qualifier("telegramBotsApiLongPolling") TelegramBotsApi telegramBotsApi,
			@Qualifier("dictionariesBotApplicationService") ApplicationService applicationService) {
		return BotCreationUtils.createLongPollingBot(enabled, token, name, telegramBotsApi, applicationService);
	}

	// Webhook instantiation

	@Bean("dictionariesBot")
	@DependsOn({"telegramBotsApiWebhook", "dictionariesBotApplicationService"})
	@ConditionalOnProperty(prefix = "telegram.bot", name="type", havingValue = "webhook")
	public WebhookBotServiceImpl webhookBotService(
			@Qualifier("telegramBotsApiWebhook") TelegramBotsApi telegramBotsApi,
			@Qualifier("dictionariesBotApplicationService") ApplicationService applicationService) {
		return BotCreationUtils.createWebhookBot(enabled, token, name, path, webhookURL, webhookCertPath, telegramBotsApi, applicationService);
	}

}
