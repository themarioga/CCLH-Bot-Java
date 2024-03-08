package org.themarioga.cclh.bot.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.bots.TelegramWebhookBot;
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
import org.themarioga.cclh.bot.service.intf.ApplicationService;
import org.themarioga.cclh.bot.service.intf.BotService;
import org.themarioga.cclh.bot.game.constants.ResponseErrorI18n;
import org.themarioga.cclh.bot.util.BotUtils;
import org.themarioga.cclh.bot.util.CallbackQueryHandler;
import org.themarioga.cclh.bot.util.CommandHandler;

import java.util.Map;

public class WebhookBotServiceImpl extends TelegramWebhookBot implements BotService {

    private static final Logger logger = LoggerFactory.getLogger(WebhookBotServiceImpl.class);

    private final String user;
    private final String path;

	private final Map<String, CommandHandler> commands;
    private final Map<String, CallbackQueryHandler> callbackQueries;

    public WebhookBotServiceImpl(String token, String user, String internalPath, ApplicationService applicationService) {
        super(token);

        this.user = user;
        this.path = internalPath;

	    commands = applicationService.getBotCommands();
        callbackQueries = applicationService.getCallbackQueries();
    }

    @Override
    public BotApiMethod<?> onWebhookUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().getText() != null && update.getMessage().getText().startsWith("/")) {
            String[] command = update.getMessage().getText().replace("@cclhbot", "").split(" ");
            CommandHandler commandHandler = commands.get(command[0]);
            if (commandHandler != null) {
                commandHandler.callback(update.getMessage(), command.length > 1 ? command[1] : null);
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

        return null;
    }

    @Override
    public String getBotUsername() {
        return user;
    }

    @Override
    public String getBotPath() {
        return path;
    }

    @Override
    public void sendMessage(long chatId, String text) {
	    try {
            SendMessage sendMessage = new SendMessage(String.valueOf(chatId), text);
            sendMessage.enableHtml(true);
            executeAsync(sendMessage);
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
            executeAsync(editMessageText);
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
            executeAsync(editMessageText);
        } catch (TelegramApiException e) {
            logger.error(e.getMessage(), e);
        }
    }

    @Override
    public void deleteMessage(long chatId, int messageId) {
        try {
            executeAsync(new DeleteMessage(String.valueOf(chatId), messageId));
        } catch (TelegramApiException e) {
            logger.error(e.getMessage(), e);
        }
    }

    @Override
    public void answerCallbackQuery(String callbackQueryId) {
        try {
            executeAsync(new AnswerCallbackQuery(callbackQueryId));
        } catch (TelegramApiException e) {
            logger.error(e.getMessage(), e);
        }
    }

    @Override
    public void answerCallbackQuery(String callbackQueryId, String text) {
        try {
            AnswerCallbackQuery answerCallbackQuery = new AnswerCallbackQuery(callbackQueryId);
            answerCallbackQuery.setText(text);
            executeAsync(answerCallbackQuery);
        } catch (TelegramApiException e) {
            logger.error(e.getMessage(), e);
        }
    }

}
