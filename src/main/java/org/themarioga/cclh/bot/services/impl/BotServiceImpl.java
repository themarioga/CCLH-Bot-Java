package org.themarioga.cclh.bot.services.impl;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.DeleteWebhook;
import com.pengrad.telegrambot.request.SendMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.themarioga.cclh.bot.constants.ResponseErrorI18n;
import org.themarioga.cclh.bot.model.CommandHandler;
import org.themarioga.cclh.bot.util.BotUtils;
import org.themarioga.cclh.bot.services.intf.BotService;

import java.util.Map;

@Service
public class BotServiceImpl implements BotService {

    private static Logger logger = LoggerFactory.getLogger(BotServiceImpl.class);

    @Value("${cclh.bot.token}")
    private String token;

    @Value("${cclh.bot.webhook.url}")
    private String webhookURL;

    @Value("${cclh.bot.webhook.certPath}")
    private String webhookCertPath;

    private TelegramBot bot;
    private UpdatesListener updatesListener;

    @Override
    public void registerCallbacks(Map<String, CommandHandler> commands) {
        logger.debug("Registrando callbacks...");

        Assert.isNull(bot, "El bot de telegram no debe estar iniciado");

        updatesListener = updates -> {
            int lastUpdateId = UpdatesListener.CONFIRMED_UPDATES_ALL;
            for (Update update : updates) {
                if (update.message() != null && update.message().text().startsWith("/")) {
                    CommandHandler commandHandler = commands.get(update.message().text().split("@")[0]);
                    if (commandHandler != null) {
                        commandHandler.callback(update.message());
                    } else {
                        logger.error("Comando desconocido {} enviado por {}",
                                update.message().text(),
                                BotUtils.getUserInfo(update.message().from()));

                        sendTextResponse(update.message().chat().id(), ResponseErrorI18n.COMMAND_DOES_NOT_EXISTS);
                    }
                } else if (update.inlineQuery() != null) {

                } else if (update.callbackQuery() != null) {

                }

                lastUpdateId = update.updateId();
            }

            return lastUpdateId;
        };
    }

    @Override
    public void startBot() {
        logger.debug("Initializing telegram bot...");

        Assert.notNull(updatesListener, "No se puede iniciar el bot sin listener");

        bot = new TelegramBot(token);

        if (webhookURL != null && !webhookURL.isBlank()) {
            BotUtils.registerWebhook(bot, webhookURL, webhookCertPath);
        } else {
            bot.execute(new DeleteWebhook().dropPendingUpdates(true));
        }

        bot.setUpdatesListener(updatesListener, e -> {
            if (e.response() != null) {
                logger.error("[{}] {}", e.response().errorCode(), e.response().description());
            } else {
                logger.error(e.getMessage(), e);
            }
        });
    }

    @Override
    public void sendTextResponse(Long chatId, String text) {
        logger.debug("Enviando el texto de respuesta {} al chat {}", text, chatId);

        Assert.notNull(bot, "El bot de telegram no está iniciado");

        bot.execute(new SendMessage(chatId, text));
    }

}
