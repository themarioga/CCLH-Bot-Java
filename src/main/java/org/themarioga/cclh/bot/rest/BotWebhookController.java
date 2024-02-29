package org.themarioga.cclh.bot.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.generics.WebhookBot;

@RestController
@ConditionalOnProperty(prefix = "cclh.bot", name="type", havingValue = "webhook")
public class BotWebhookController {

	private WebhookBot webhookBot;

	@PostMapping("/callback/telegram")
	public void update(@RequestBody Update update) {
		webhookBot.onWebhookUpdateReceived(update);
	}

	@Autowired
	public void setWebhookBot(WebhookBot webhookBot) {
		this.webhookBot = webhookBot;
	}

}
