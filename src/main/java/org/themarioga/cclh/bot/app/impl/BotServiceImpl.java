package org.themarioga.cclh.bot.app.impl;

import com.pengrad.telegrambot.Callback;
import com.pengrad.telegrambot.Cancellable;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.AnswerCallbackQuery;
import com.pengrad.telegrambot.request.BaseRequest;
import com.pengrad.telegrambot.request.DeleteWebhook;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.BaseResponse;
import com.pengrad.telegrambot.response.SendResponse;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.themarioga.cclh.bot.constants.ResponseErrorI18n;
import org.themarioga.cclh.bot.util.CallbackQueryHandler;
import org.themarioga.cclh.bot.util.CommandHandler;
import org.themarioga.cclh.bot.util.BotUtils;
import org.themarioga.cclh.bot.app.intf.BotService;
import org.themarioga.cclh.commons.exceptions.ApplicationException;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
public class BotServiceImpl implements BotService {

    private static final Logger logger = LoggerFactory.getLogger(BotServiceImpl.class);

    @Value("${cclh.bot.token}")
    private String token;

    @Value("${cclh.bot.webhook.url}")
    private String webhookURL;

    @Value("${cclh.bot.webhook.certPath}")
    private String webhookCertPath;

    @Autowired
    public BotService botService;

    private TelegramBot bot;

    @Override
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ApplicationException.class)
    public void startBot(Map<String, CommandHandler> commands, Map<String, CallbackQueryHandler> callbackQueries) {
        logger.trace("Initializing telegram bot...");

        bot = new TelegramBot(token);

        if (webhookURL != null && !webhookURL.isBlank()) {
            BotUtils.registerWebhook(bot, webhookURL, webhookCertPath);
        } else {
            bot.execute(new DeleteWebhook().dropPendingUpdates(true));
        }

        bot.setUpdatesListener(updates -> botService.handleUpdates(commands, callbackQueries, updates), e -> {
            if (e.response() != null) {
                logger.error("[{}] {}", e.response().errorCode(), e.response().description());
            } else {
                logger.error(e.getMessage(), e);
            }
        });
    }

    @Override
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ApplicationException.class)
    public int handleUpdates(Map<String, CommandHandler> commands, Map<String, CallbackQueryHandler> callbackQueries, List<Update> updates) {
        logger.trace("Handling updates...");

        int lastUpdateId = UpdatesListener.CONFIRMED_UPDATES_ALL;
        for (Update update : updates) {
            if (update.message() != null && update.message().text() != null && update.message().text().startsWith("/")) {
                CommandHandler commandHandler = commands.get(update.message().text().split("@")[0]);
                if (commandHandler != null) {
                    commandHandler.callback(update.message());
                } else {
                    logger.error("Comando desconocido {} enviado por {}",
                            update.message().text(),
                            BotUtils.getUserInfo(update.message().from()));

                    sendMessageAsync(new SendMessage(update.message().chat().id(), ResponseErrorI18n.COMMAND_DOES_NOT_EXISTS), new Callback<SendMessage, SendResponse>() {
                        @Override
                        public void onResponse(SendMessage request, SendResponse response) {
                            logger.trace("Error enviado correctamente");
                        }

                        @Override
                        public void onFailure(SendMessage request, IOException e) {
                            logger.error("No se ha podido enviar el mensaje.", e);
                        }
                    });
                }
            } else if (update.callbackQuery() != null) {
               String[] query = update.callbackQuery().data().split("__");
                CallbackQueryHandler callbackQueryHandler = callbackQueries.get(query[0]);
                if (callbackQueryHandler != null) {
                    callbackQueryHandler.callback(update.callbackQuery(), query.length > 1 ? query[1] : null);
                } else {
                    logger.error("Querie desconocida {} enviado por {}",
                            update.callbackQuery().data(),
                            BotUtils.getUserInfo(update.callbackQuery().message().from()));

                    sendMessage(new AnswerCallbackQuery(update.callbackQuery().id())
                            .text(ResponseErrorI18n.COMMAND_DOES_NOT_EXISTS));
                }
            }

            lastUpdateId = update.updateId();
        }

        return lastUpdateId;
    }

    @Override
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ApplicationException.class)
    public <T extends BaseRequest<T, R>, R extends BaseResponse> Cancellable sendMessage(T request) {
        logger.trace("Enviando mensaje");

        return bot.execute(request, new Callback<T, R>() {
            @Override
            public void onResponse(T request, R response) {
                logger.trace("Mensaje enviado correctamente");
            }

            @Override
            public void onFailure(T request, IOException e) {
                logger.error("Error al enviar mensaje", e);
            }
        });
    }

    @Override
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ApplicationException.class)
    public <T extends BaseRequest<T, R>, R extends BaseResponse> R sendMessageSync(BaseRequest<T, R> request) {
        logger.trace("Enviando mensaje sincrono");

        return bot.execute(request);
    }

    @Override
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ApplicationException.class)
    public <T extends BaseRequest<T, R>, R extends BaseResponse> Cancellable sendMessageAsync(T request, Callback<T, R> callback) {
        logger.trace("Enviando mensaje asincrono");

        return bot.execute(request, callback);
    }

}
