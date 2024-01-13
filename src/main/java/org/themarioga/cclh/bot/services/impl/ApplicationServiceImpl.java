package org.themarioga.cclh.bot.services.impl;

import com.pengrad.telegrambot.model.Chat;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.themarioga.cclh.bot.constants.ResponseErrorI18n;
import org.themarioga.cclh.bot.constants.ResponseMessageI18n;
import org.themarioga.cclh.bot.model.CommandHandler;
import org.themarioga.cclh.bot.services.intf.ApplicationService;
import org.themarioga.cclh.bot.services.intf.BotService;
import org.themarioga.cclh.bot.services.intf.CCLHService;
import org.themarioga.cclh.bot.util.BotUtils;

import java.util.HashMap;
import java.util.Map;

@Service
public class ApplicationServiceImpl implements ApplicationService {

    private static Logger logger = LoggerFactory.getLogger(ApplicationServiceImpl.class);

    private BotService botService;
    private CCLHService cclhService;

    @Autowired
    public ApplicationServiceImpl(BotService botService, CCLHService cclhService) {
        this.botService = botService;
        this.cclhService = cclhService;
    }

    @Override
    public void run() {
        logger.info("Starting Bot...");

        botService.registerCallbacks(getBotCommands());

        botService.startBot();

        logger.info("Bot Started");
    }

    @NotNull
    private Map<String, CommandHandler> getBotCommands() {
        Map<String, CommandHandler> commands = new HashMap<>();
        commands.put("/start", message -> {
            if (!message.chat().type().equals(Chat.Type.Private)) {
                logger.error("Comando /start enviado en lugar incorrecto por {}", BotUtils.getUserInfo(message.from()));

                botService.sendTextResponse(message.chat().id(), ResponseErrorI18n.COMMAND_SHOULD_BE_ON_PRIVATE);
            }

            cclhService.registerUser(message.from().id(), BotUtils.getUsername(message.from()));
            botService.sendTextResponse(message.chat().id(), ResponseMessageI18n.WELCOME);
        });
        commands.put("/create", message -> {
            if (message.chat().type().equals(Chat.Type.Private)) {
                logger.error("Comando /start enviado en lugar incorrecto por {}", BotUtils.getUserInfo(message.from()));

                botService.sendTextResponse(message.chat().id(), ResponseErrorI18n.COMMAND_SHOULD_BE_ON_GROUP);
            }

            // ToDo:
        });
        commands.put("/deleteMyGames", message -> {
            if (!message.chat().type().equals(Chat.Type.Private)) {
                logger.error("Comando /start enviado en lugar incorrecto por {}", BotUtils.getUserInfo(message.from()));

                botService.sendTextResponse(message.chat().id(), ResponseErrorI18n.COMMAND_SHOULD_BE_ON_PRIVATE);
            }

            // ToDo:
        });
        commands.put("/help", message -> botService.sendTextResponse(message.chat().id(), ResponseMessageI18n.HELP));
        return commands;
    }
}
