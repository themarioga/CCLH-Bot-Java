package org.themarioga.cclh.bot.dictionaries.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ForceReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;
import org.telegram.telegrambots.meta.updateshandlers.SentCallback;
import org.themarioga.bot.service.intf.BotService;
import org.themarioga.cclh.bot.dictionaries.service.intf.DictionariesBotMessageService;

@Service
public class DictionariesBotMessageServiceImpl implements DictionariesBotMessageService {

	private static final Logger logger = LoggerFactory.getLogger(DictionariesBotMessageServiceImpl.class);

	private BotService botService;

	@Override
	public void sendMessage(long chatId, String text) {
		try {
			SendMessage sendMessage = new SendMessage(String.valueOf(chatId), text);
			sendMessage.enableHtml(true);
			botService.execute(sendMessage);
		} catch (TelegramApiException e) {
			logger.error(e.getMessage(), e);
		}
	}

	@Override
	public void sendMessage(long chatId, String text, InlineKeyboardMarkup inlineKeyboardMarkup) {
		try {
			SendMessage sendMessage = new SendMessage(String.valueOf(chatId), text);
			sendMessage.enableHtml(true);
			sendMessage.setReplyMarkup(inlineKeyboardMarkup);
			botService.execute(sendMessage);
		} catch (TelegramApiException e) {
			logger.error(e.getMessage(), e);
		}
	}

	@Override
	public void sendMessageWithForceReply(long chatId, String text) {
		try {
			SendMessage sendMessage = new SendMessage(String.valueOf(chatId), text);
			sendMessage.enableHtml(true);
			sendMessage.setReplyMarkup(new ForceReplyKeyboard());
			botService.execute(sendMessage);
		} catch (TelegramApiException e) {
			logger.error(e.getMessage(), e);
		}
	}

	@Override
	public void sendMessageAsync(long chatId, String text, Callback callback) {
		try {
			SendMessage sendMessage = new SendMessage(String.valueOf(chatId), text);
			sendMessage.enableHtml(true);
			botService.executeAsync(sendMessage, new SentCallback<Message>() {
				@Override
				public void onResult(BotApiMethod<Message> method, Message response) {
					callback.success(method, response);
				}

				@Override
				public void onError(BotApiMethod<Message> method, TelegramApiRequestException apiException) {
					callback.failure(method, apiException);
				}

				@Override
				public void onException(BotApiMethod<Message> method, Exception exception) {
					callback.failure(method, exception);
				}
			});
		} catch (TelegramApiException e) {
			logger.error(e.getMessage(), e);
		}
	}

	@Override
	public void editMessage(long chatId, int messageId, String text) {
		try {
			EditMessageText editMessageText = new EditMessageText();
			editMessageText.setChatId(chatId);
			editMessageText.setMessageId(messageId);
			editMessageText.setText(text);
			editMessageText.enableHtml(true);
			botService.execute(editMessageText);
		} catch (TelegramApiException e) {
			logger.error(e.getMessage(), e);
		}
	}

	@Override
	public void editMessage(long chatId, int messageId, String text, InlineKeyboardMarkup inlineKeyboardMarkup) {
		try {
			EditMessageText editMessageText = new EditMessageText();
			editMessageText.setChatId(chatId);
			editMessageText.setMessageId(messageId);
			editMessageText.setText(text);
			editMessageText.enableHtml(true);
			editMessageText.setReplyMarkup(inlineKeyboardMarkup);
			botService.execute(editMessageText);
		} catch (TelegramApiException e) {
			logger.error(e.getMessage(), e);
		}
	}

	@Override
	public void deleteMessage(long chatId, int messageId) {
		try {
			botService.execute(new DeleteMessage(String.valueOf(chatId), messageId));
		} catch (TelegramApiException e) {
			logger.error(e.getMessage(), e);
		}
	}

	@Override
	public void answerCallbackQuery(String callbackQueryId) {
		try {
			botService.execute(new AnswerCallbackQuery(callbackQueryId));
		} catch (TelegramApiException e) {
			logger.error(e.getMessage(), e);
		}
	}

	@Override
	public void answerCallbackQuery(String callbackQueryId, String text) {
		try {
			AnswerCallbackQuery answerCallbackQuery = new AnswerCallbackQuery(callbackQueryId);
			answerCallbackQuery.setText(text);
			botService.execute(answerCallbackQuery);
		} catch (TelegramApiException e) {
			logger.error(e.getMessage(), e);
		}
	}

	@Override
	public void setPendingReply(Long userId, String command) {
		botService.setPendingReply(userId, command);
	}

	@Autowired
	@Qualifier("dictionariesBot")
	public void setBotService(BotService botService) {
		this.botService = botService;
	}

}
