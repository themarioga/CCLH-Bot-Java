package org.themarioga.cclh.bot.game.service.intf;

import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

public interface CCLHBotMessageService {

	void sendMessage(long chatId, String text);

	void sendMessage(long chatId, String text, InlineKeyboardMarkup inlineKeyboardMarkup);

	void sendMessageAsync(long chatId, String text, Callback callback);

	void editMessage(long chatId, int messageId, String text);

	void editMessage(long chatId, int messageId, String text, InlineKeyboardMarkup inlineKeyboardMarkup);

	void deleteMessage(long chatId, int messageId);

	void answerCallbackQuery(String callbackQueryId);

	void answerCallbackQuery(String callbackQueryId, String text);

	String sanitizeTextFromCommand(String command, String text);

	interface Callback {

		void success(BotApiMethod<Message> method, Message response);

		void failure(BotApiMethod<Message> method, Exception e);

	}

}
