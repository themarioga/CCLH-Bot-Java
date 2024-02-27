package org.themarioga.cclh.bot.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.generics.WebhookBot;

@Profile("pro")
@RestController
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
