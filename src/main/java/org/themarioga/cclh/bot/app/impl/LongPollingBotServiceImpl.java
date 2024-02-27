package org.themarioga.cclh.bot.app.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;
import org.telegram.telegrambots.meta.updateshandlers.SentCallback;
import org.themarioga.cclh.bot.app.intf.ApplicationService;
import org.themarioga.cclh.bot.app.intf.BotService;
import org.themarioga.cclh.bot.constants.ResponseErrorI18n;
import org.themarioga.cclh.bot.util.BotUtils;
import org.themarioga.cclh.bot.util.CallbackQueryHandler;
import org.themarioga.cclh.bot.util.CommandHandler;

import java.util.Map;

public class LongPollingBotServiceImpl extends TelegramLongPollingBot implements BotService {

    private static final Logger logger = LoggerFactory.getLogger(LongPollingBotServiceImpl.class);

    private final String user;

	private final Map<String, CommandHandler> commands;
    private final Map<String, CallbackQueryHandler> callbackQueries;

    public LongPollingBotServiceImpl(String token, String user, ApplicationService applicationService) {
        super(token);

        this.user = user;

	    commands = applicationService.getBotCommands();
        callbackQueries = applicationService.getCallbackQueries();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().getText() != null && update.getMessage().getText().startsWith("/")) {
            CommandHandler commandHandler = commands.get(update.getMessage().getText().split("@")[0]);
            if (commandHandler != null) {
                commandHandler.callback(update.getMessage());
            } else {
                logger.error("Comando desconocido {} enviado por {}",
                        update.getMessage().getText(),
                        BotUtils.getUserInfo(update.getMessage().getFrom()));

                sendMessageAsync(update.getMessage().getChatId(), ResponseErrorI18n.COMMAND_DOES_NOT_EXISTS, new Callback() {
                    @Override
                    public void success(BotApiMethod<Message> method, Message response) {
                        // Nada
                    }

                    @Override
                    public void failure(BotApiMethod<Message> botApiMethod, Exception e) {
                        logger.error(e.getMessage(), e);
                    }
                });
            }
        } else if (update.hasCallbackQuery()) {
            String[] query = update.getCallbackQuery().getData().split("__");
            CallbackQueryHandler callbackQueryHandler = callbackQueries.get(query[0]);
            if (callbackQueryHandler != null) {
                callbackQueryHandler.callback(update.getCallbackQuery(), query.length > 1 ? query[1] : null);
            } else {
                logger.error("Querie desconocida {} enviado por {}",
                        update.getCallbackQuery().getData(),
                        BotUtils.getUserInfo(update.getCallbackQuery().getFrom()));

                answerCallbackQuery(update.getCallbackQuery().getId(), ResponseErrorI18n.COMMAND_DOES_NOT_EXISTS);
            }
        }
    }

    @Override
    public String getBotUsername() {
        return user;
    }

    @Override
    public void sendMessage(long chatId, String text) {
	    try {
            SendMessage sendMessage = new SendMessage(String.valueOf(chatId), text);
            sendMessage.enableHtml(true);
		    execute(sendMessage);
	    } catch (TelegramApiException e) {
            logger.error(e.getMessage(), e);
	    }
    }

    @Override
    public void sendMessageAsync(long chatId, String text, Callback callback) {
        try {
            SendMessage sendMessage = new SendMessage(String.valueOf(chatId), text);
            sendMessage.enableHtml(true);
            executeAsync(sendMessage, new SentCallback<Message>() {
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
            execute(editMessageText);
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
            execute(editMessageText);
        } catch (TelegramApiException e) {
            logger.error(e.getMessage(), e);
        }
    }

    @Override
    public void deleteMessage(long chatId, int messageId) {
        try {
            execute(new DeleteMessage(String.valueOf(chatId), messageId));
        } catch (TelegramApiException e) {
            logger.error(e.getMessage(), e);
        }
    }

    @Override
    public void answerCallbackQuery(String callbackQueryId) {
        try {
            execute(new AnswerCallbackQuery(callbackQueryId));
        } catch (TelegramApiException e) {
            logger.error(e.getMessage(), e);
        }
    }

    @Override
    public void answerCallbackQuery(String callbackQueryId, String text) {
        try {
            AnswerCallbackQuery answerCallbackQuery = new AnswerCallbackQuery(callbackQueryId);
            answerCallbackQuery.setText(text);
            execute(answerCallbackQuery);
        } catch (TelegramApiException e) {
            logger.error(e.getMessage(), e);
        }
    }

}
