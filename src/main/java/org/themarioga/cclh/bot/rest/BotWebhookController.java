package org.themarioga.cclh.bot.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiValidationException;
import org.telegram.telegrambots.updatesreceivers.ServerlessWebhook;

@RestController
@ConditionalOnProperty(prefix = "cclh.bot", name="type", havingValue = "webhook")
public class BotWebhookController {

	private static final Logger logger = LoggerFactory.getLogger(BotWebhookController.class);

	private ServerlessWebhook serverlessWebhook;

	@PostMapping("/callback/{path}")
	public void update(@PathVariable("path") String path, @RequestBody Update update) {
		try {
			serverlessWebhook.updateReceived(path, update);
		} catch (TelegramApiValidationException e) {
			logger.error("Error localizado en una llamada al webhook, {}", e.getMessage(), e);
		}
	}

	@Autowired
	public void setServerlessWebhook(ServerlessWebhook serverlessWebhook) {
		this.serverlessWebhook = serverlessWebhook;
	}

}
