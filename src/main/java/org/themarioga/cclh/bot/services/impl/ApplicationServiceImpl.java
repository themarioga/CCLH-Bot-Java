package org.themarioga.cclh.bot.services.impl;

import com.pengrad.telegrambot.Callback;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.request.EditMessageText;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.themarioga.cclh.bot.constants.ResponseErrorI18n;
import org.themarioga.cclh.bot.constants.ResponseMessageI18n;
import org.themarioga.cclh.bot.dto.TgGameDTO;
import org.themarioga.cclh.bot.model.CommandHandler;
import org.themarioga.cclh.bot.model.TelegramGame;
import org.themarioga.cclh.bot.services.intf.ApplicationService;
import org.themarioga.cclh.bot.services.intf.BotService;
import org.themarioga.cclh.bot.services.intf.CCLHService;
import org.themarioga.cclh.bot.util.BotUtils;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

@Service
public class ApplicationServiceImpl implements ApplicationService {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationServiceImpl.class);

    private final BotService botService;
    private final CCLHService cclhService;

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

                botService.sendMessage(new SendMessage(message.chat().id(), ResponseErrorI18n.COMMAND_SHOULD_BE_ON_PRIVATE));
            }

            if (cclhService.registerUser(message.from().id(), BotUtils.getUsername(message.from()))) {
                botService.sendMessage(new SendMessage(message.chat().id(), ResponseMessageI18n.WELCOME));
            } else {
                botService.sendMessage(new SendMessage(message.chat().id(), ResponseErrorI18n.USER_ALREADY_REGISTERED));
            }
        });
        commands.put("/create", message -> {
            if (message.chat().type().equals(Chat.Type.Private)) {
                logger.error("Comando /start enviado en lugar incorrecto por {}", BotUtils.getUserInfo(message.from()));

                botService.sendMessage(new SendMessage(message.chat().id(), ResponseErrorI18n.COMMAND_SHOULD_BE_ON_GROUP));
            }

            botService.sendMessageAsync(new SendMessage(message.chat().id(), ResponseMessageI18n.GAME_CREATING),
                    new Callback<SendMessage, SendResponse>() {
                @Override
                public void onResponse(SendMessage groupRequest, SendResponse groupResponse) {
                    botService.sendMessageAsync(new SendMessage(message.from().id(), ResponseMessageI18n.GAME_CREATING),
                            new Callback<SendMessage, SendResponse>() {
                                @Override
                                public void onResponse(SendMessage privateRequest, SendResponse privateResponse) {
                                    TelegramGame tgGame = cclhService.createGame(message.chat().id(), message.chat().title(), message.from().id(),
                                            groupResponse.message().messageId(), privateResponse.message().messageId());
                                    if (tgGame != null) {
                                        InlineKeyboardMarkup groupInlineKeyboard = new InlineKeyboardMarkup(
                                                new InlineKeyboardButton[]{new InlineKeyboardButton("Unirse a la partida").callbackData("join_game")},
                                                new InlineKeyboardButton[]{new InlineKeyboardButton("Configurar la partida").callbackData("configurate_game")},
                                                new InlineKeyboardButton[]{new InlineKeyboardButton("Borrar juego").callbackData("delete_game_group")}
                                        );
                                        botService.sendMessage(new EditMessageText(message.chat().id(), groupResponse.message().messageId(),
                                                MessageFormat.format(ResponseMessageI18n.GAME_CREATED_GROUP,
                                                        ResponseMessageI18n.getGameTypeName(tgGame.getGame().getType()),
                                                        tgGame.getGame().getDictionary().getName(),
                                                        tgGame.getGame().getNumberOfCardsToWin(),
                                                        tgGame.getGame().getMaxNumberOfPlayers()))
                                                .replyMarkup(groupInlineKeyboard));

                                        InlineKeyboardMarkup privateInlineKeyboard = new InlineKeyboardMarkup(
                                                new InlineKeyboardButton("Borrar juego").callbackData("delete_game_private")
                                        );
                                        botService.sendMessage(new EditMessageText(message.from().id(), privateResponse.message().messageId(),
                                                ResponseMessageI18n.GAME_CREATED_PRIVATE).replyMarkup(privateInlineKeyboard));
                                    } else {
                                        botService.sendMessage(new EditMessageText(message.chat().id(), groupResponse.message().messageId(),
                                                ResponseErrorI18n.GAME_ALREADY_CREATED));

                                        botService.sendMessage(new EditMessageText(message.from().id(), privateResponse.message().messageId(),
                                                ResponseErrorI18n.GAME_ALREADY_CREATED));
                                    }
                                }

                                @Override
                                public void onFailure(SendMessage privateRequest, IOException e) {
                                    logger.error("Fallo al enviar mensaje", e);
                                }
                            });
                }

                @Override
                public void onFailure(SendMessage groupRequest, IOException e) {
                    logger.error("Fallo al enviar mensaje", e);
                }
            });
        });
        commands.put("/deleteMyGames", message -> {
            if (!message.chat().type().equals(Chat.Type.Private)) {
                logger.error("Comando /start enviado en lugar incorrecto por {}", BotUtils.getUserInfo(message.from()));

                botService.sendMessage(new SendMessage(message.chat().id(), ResponseErrorI18n.COMMAND_SHOULD_BE_ON_PRIVATE));
            }

            TgGameDTO tgGameDTO = cclhService.deleteGameByUserId(message.from().id());
            botService.sendMessage(new EditMessageText(tgGameDTO.getRoomId(), tgGameDTO.getGroupMessageId(),
                    ResponseErrorI18n.GAME_DELETED));
            botService.sendMessage(new EditMessageText(tgGameDTO.getCreatorId(), tgGameDTO.getPrivateMessageId(),
                    ResponseErrorI18n.GAME_DELETED));
        });
        commands.put("/help", message -> botService.sendMessage(new SendMessage(message.chat().id(), ResponseMessageI18n.HELP)));
        return commands;
    }
}
