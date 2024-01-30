package org.themarioga.cclh.bot.app.impl;

import com.pengrad.telegrambot.Callback;
import com.pengrad.telegrambot.model.CallbackQuery;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.request.AnswerCallbackQuery;
import com.pengrad.telegrambot.request.EditMessageText;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.themarioga.cclh.bot.app.intf.ApplicationService;
import org.themarioga.cclh.bot.constants.ResponseErrorI18n;
import org.themarioga.cclh.bot.constants.ResponseMessageI18n;
import org.themarioga.cclh.bot.model.TelegramPlayer;
import org.themarioga.cclh.bot.util.CallbackQueryHandler;
import org.themarioga.cclh.bot.util.CommandHandler;
import org.themarioga.cclh.bot.model.TelegramGame;
import org.themarioga.cclh.bot.app.intf.BotService;
import org.themarioga.cclh.bot.app.intf.CCLHService;
import org.themarioga.cclh.bot.util.BotUtils;
import org.themarioga.cclh.commons.enums.GameTypeEnum;
import org.themarioga.cclh.commons.exceptions.ApplicationException;
import org.themarioga.cclh.commons.exceptions.game.GameAlreadyExistsException;
import org.themarioga.cclh.commons.exceptions.player.PlayerAlreadyExistsException;
import org.themarioga.cclh.commons.exceptions.user.UserAlreadyExistsException;
import org.themarioga.cclh.commons.models.Deck;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ApplicationServiceImpl implements ApplicationService {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationServiceImpl.class);

    private final BotService botService;
    private final CCLHService cclhService;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    ApplicationService applicationService;

    @Autowired
    public ApplicationServiceImpl(BotService botService, CCLHService cclhService) {
        this.botService = botService;
        this.cclhService = cclhService;
    }

    @Override
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ApplicationException.class)
    public void run() {
        logger.info("Starting Bot...");

        botService.startBot(getBotCommands(), getCallbackQueries());

        logger.info("Bot Started");
    }

    @Override
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ApplicationException.class)
    public void createGame(Message message, SendResponse groupResponse, SendResponse privateResponse, SendResponse playerResponse) {
        TelegramGame telegramGame = cclhService.createGame(message.chat().id(),
                message.chat().title(), message.from().id(),
                groupResponse.message().messageId(),
                privateResponse.message().messageId(),
                playerResponse.message().messageId());

        sendMainMenu(telegramGame);

        botService.sendMessage(new EditMessageText(playerResponse.message().from().id(),
                playerResponse.message().messageId(), ResponseMessageI18n.PLAYER_CREATED));
    }

    @Override
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ApplicationException.class)
    public void joinGame(CallbackQuery callbackQuery, SendResponse playerResponse) {
        TelegramGame telegramGame = cclhService.getGame(callbackQuery.message().chat().id());

        TelegramPlayer telegramPlayer = cclhService.joinGame(telegramGame, callbackQuery.from().id(),
                playerResponse.message().messageId());

        InlineKeyboardMarkup privateInlineKeyboard = new InlineKeyboardMarkup(
                new InlineKeyboardButton("Dejar la partida").callbackData("game_leave")
        );
        botService.sendMessage(new EditMessageText(telegramPlayer.getPlayer().getUser().getId(),
                telegramPlayer.getMessageId(), ResponseMessageI18n.PLAYER_CREATED)
                .replyMarkup(privateInlineKeyboard));

        sendMainMenu(telegramGame);
    }

    private Map<String, CommandHandler> getBotCommands() {
        Map<String, CommandHandler> commands = new HashMap<>();
        commands.put("/start", message -> {
            if (!message.chat().type().equals(Chat.Type.Private)) {
                logger.error("Comando /start enviado en lugar incorrecto por {}", BotUtils.getUserInfo(message.from()));

                botService.sendMessage(new SendMessage(message.chat().id(), ResponseErrorI18n.COMMAND_SHOULD_BE_ON_PRIVATE));

                return;
            }

            try {
                cclhService.registerUser(message.from().id(), BotUtils.getUsername(message.from()));

                botService.sendMessage(new SendMessage(message.chat().id(), ResponseMessageI18n.WELCOME));
            } catch (UserAlreadyExistsException e) {
                logger.error("El usuario {} ({}) esta intentando registrarse de nuevo.", message.from().id(),
                        BotUtils.getUsername(message.from()));

                botService.sendMessage(new SendMessage(message.chat().id(), ResponseErrorI18n.USER_ALREADY_REGISTERED));
            }
        });
        commands.put("/create", message -> {
            if (message.chat().type().equals(Chat.Type.Private)) {
                logger.error("Comando /start enviado en lugar incorrecto por {}", BotUtils.getUserInfo(message.from()));

                botService.sendMessage(new SendMessage(message.chat().id(), ResponseErrorI18n.COMMAND_SHOULD_BE_ON_GROUP));

                return;
            }

            botService.sendMessageAsync(new SendMessage(message.chat().id(), ResponseMessageI18n.GAME_CREATING),
                new Callback<SendMessage, SendResponse>() {
                @Override
                public void onResponse(SendMessage groupRequest, SendResponse groupResponse) {
                    botService.sendMessageAsync(new SendMessage(message.from().id(), ResponseMessageI18n.GAME_CREATING),
                        new Callback<SendMessage, SendResponse>() {
                            @Override
                            public void onResponse(SendMessage privateRequest, SendResponse privateResponse) {
                                botService.sendMessageAsync(new SendMessage(message.from().id(),
                                                ResponseMessageI18n.PLAYER_CREATING),
                                        new Callback<SendMessage, SendResponse>() {
                                            @Override
                                            public void onResponse(SendMessage playerRequest, SendResponse playerResponse) {
                                                try {
                                                    applicationService.createGame(message, groupResponse, privateResponse, playerResponse);
                                                } catch (GameAlreadyExistsException e) {
                                                    logger.error("Ya existe una partida para al sala {} ({}) o creado por {}.",
                                                            message.chat().id(), message.chat().title(), message.from().id());

                                                    botService.sendMessage(new EditMessageText(message.chat().id(),
                                                            groupResponse.message().messageId(),
                                                            ResponseErrorI18n.GAME_ALREADY_CREATED));

                                                    botService.sendMessage(new EditMessageText(message.from().id(),
                                                            privateResponse.message().messageId(),
                                                            ResponseErrorI18n.GAME_ALREADY_CREATED));
                                                }
                                            }

                                            @Override
                                            public void onFailure(SendMessage groupRequest, IOException e) {
                                                logger.error("Fallo al enviar mensaje", e);
                                            }
                                        });
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

                return;
            }

            TelegramGame telegramGame = cclhService.getGameByCreatorId(message.from().id());
            if (telegramGame != null) {
                List<TelegramPlayer> telegramPlayerList = cclhService.deleteGame(telegramGame);

                for (TelegramPlayer telegramPlayer : telegramPlayerList) {
                    botService.sendMessage(new EditMessageText(telegramPlayer.getPlayer().getUser().getId(),
                            telegramPlayer.getMessageId(),
                            ResponseMessageI18n.GAME_DELETED));
                }

                botService.sendMessage(new EditMessageText(telegramGame.getGame().getRoom().getId(),
                        telegramGame.getGroupMessageId(),
                        ResponseMessageI18n.GAME_DELETED));
                botService.sendMessage(new EditMessageText(telegramGame.getGame().getCreator().getId(),
                        telegramGame.getPrivateMessageId(),
                        ResponseMessageI18n.GAME_DELETED));
            } else {
                botService.sendMessage(new SendMessage(message.chat().id(), ResponseErrorI18n.GAME_NO_GAMES));
            }
        });
        commands.put("/help", message -> botService.sendMessage(new SendMessage(message.chat().id(), ResponseMessageI18n.HELP)));
        return commands;
    }

    private Map<String, CallbackQueryHandler> getCallbackQueries() {
        Map<String, CallbackQueryHandler> callbackQueryHandlerMap = new HashMap<>();

        callbackQueryHandlerMap.put("game_created", (callbackQuery, data) -> {
            TelegramGame telegramGame = cclhService.getGame(callbackQuery.message().chat().id());

            sendMainMenu(telegramGame);
        });

        callbackQueryHandlerMap.put("game_configure", (callbackQuery, data) -> {
            TelegramGame telegramGame = cclhService.getGame(callbackQuery.message().chat().id());

            if (callbackQuery.from().id().equals(telegramGame.getGame().getCreator().getId())) {
                sendConfigMenu(telegramGame);
            } else {
                botService.sendMessage(new AnswerCallbackQuery(callbackQuery.id())
                        .text(ResponseErrorI18n.GAME_ONLY_CREATOR_CAN_CONFIGURE));
            }
        });

        callbackQueryHandlerMap.put("game_sel_mode", (callbackQuery, data) -> {
            TelegramGame telegramGame = cclhService.getGame(callbackQuery.message().chat().id());

            if (callbackQuery.from().id().equals(telegramGame.getGame().getCreator().getId())) {
                InlineKeyboardMarkup groupInlineKeyboard = new InlineKeyboardMarkup(
                        new InlineKeyboardButton[]{ new InlineKeyboardButton("Democracia").callbackData("game_change_mode__0") },
                        new InlineKeyboardButton[]{ new InlineKeyboardButton("Clásico").callbackData("game_change_mode__1") },
                        new InlineKeyboardButton[]{ new InlineKeyboardButton("Dictadura").callbackData("game_change_mode__2") },
                        new InlineKeyboardButton[]{ new InlineKeyboardButton("⬅ Volver").callbackData("game_configure") }
                );

                botService.sendMessage(
                        new EditMessageText(telegramGame.getGame().getRoom().getId(), telegramGame.getGroupMessageId(),
                                getCreatedGameMessage(telegramGame) + "\n Selecciona modo de juego:")
                                .replyMarkup(groupInlineKeyboard));
            } else {
                botService.sendMessage(new AnswerCallbackQuery(callbackQuery.id())
                        .text(ResponseErrorI18n.GAME_ONLY_CREATOR_CAN_CONFIGURE));
            }
        });

        callbackQueryHandlerMap.put("game_sel_deck", (callbackQuery, data) -> {
            TelegramGame telegramGame = cclhService.getGame(callbackQuery.message().chat().id());

            if (callbackQuery.from().id().equals(telegramGame.getGame().getCreator().getId())) {
                int pageNumber = Integer.parseInt(data);
                int decksPerPage = cclhService.getDecksPerPage();
                int totalDecks = (int) cclhService.getDeckCount(callbackQuery.from().id());
                int firstResult = (pageNumber - 1) * decksPerPage;
                List<Deck> deckList = cclhService.getDeckPaginated(callbackQuery.from().id(), firstResult, decksPerPage);

                InlineKeyboardMarkup groupInlineKeyboard = new InlineKeyboardMarkup();
                if (pageNumber > 1) {
                    groupInlineKeyboard.addRow(new InlineKeyboardButton("⬅").callbackData("game_sel_deck__" + (pageNumber - 1)));
                }
                for (Deck deck : deckList) {
                    groupInlineKeyboard.addRow(new InlineKeyboardButton(deck.getName()).callbackData("game_change_deck__" + deck.getId()));
                }
                if (totalDecks > pageNumber * decksPerPage) {
                    groupInlineKeyboard.addRow(new InlineKeyboardButton("➡").callbackData("game_sel_deck__" + (pageNumber + 1)));
                }
                groupInlineKeyboard.addRow(new InlineKeyboardButton("⬅ Volver").callbackData("game_configure"));

                botService.sendMessage(
                        new EditMessageText(telegramGame.getGame().getRoom().getId(), telegramGame.getGroupMessageId(),
                                getCreatedGameMessage(telegramGame) + "\n Selecciona el mazo:")
                                .replyMarkup(groupInlineKeyboard));
            } else {
                botService.sendMessage(new AnswerCallbackQuery(callbackQuery.id())
                        .text(ResponseErrorI18n.GAME_ONLY_CREATOR_CAN_CONFIGURE));
            }
        });

        callbackQueryHandlerMap.put("game_sel_max_players", (callbackQuery, data) -> {
            TelegramGame telegramGame = cclhService.getGame(callbackQuery.message().chat().id());

            if (callbackQuery.from().id().equals(telegramGame.getGame().getCreator().getId())) {
                InlineKeyboardMarkup groupInlineKeyboard = new InlineKeyboardMarkup(
                        new InlineKeyboardButton[]{
                                new InlineKeyboardButton("1").callbackData("game_change_max_players__1"),
                                new InlineKeyboardButton("2").callbackData("game_change_max_players__2"),
                                new InlineKeyboardButton("3").callbackData("game_change_max_players__3"),
                        },
                        new InlineKeyboardButton[]{
                                new InlineKeyboardButton("4").callbackData("game_change_max_players__4"),
                                new InlineKeyboardButton("5").callbackData("game_change_max_players__5"),
                                new InlineKeyboardButton("6").callbackData("game_change_max_players__6"),
                        },
                        new InlineKeyboardButton[]{
                                new InlineKeyboardButton("7").callbackData("game_change_max_players__7"),
                                new InlineKeyboardButton("8").callbackData("game_change_max_players__8"),
                                new InlineKeyboardButton("9").callbackData("game_change_max_players__9"),
                        },
                        new InlineKeyboardButton[]{ new InlineKeyboardButton("⬅ Volver").callbackData("game_configure") }
                );

                botService.sendMessage(
                        new EditMessageText(telegramGame.getGame().getRoom().getId(), telegramGame.getGroupMessageId(),
                                getCreatedGameMessage(telegramGame) + "\n Selecciona nº máximo de jugadores:")
                                .replyMarkup(groupInlineKeyboard));
            } else {
                botService.sendMessage(new AnswerCallbackQuery(callbackQuery.id())
                        .text(ResponseErrorI18n.GAME_ONLY_CREATOR_CAN_CONFIGURE));
            }
        });

        callbackQueryHandlerMap.put("game_sel_max_points", (callbackQuery, data) -> {
            TelegramGame telegramGame = cclhService.getGame(callbackQuery.message().chat().id());

            if (callbackQuery.from().id().equals(telegramGame.getGame().getCreator().getId())) {
                InlineKeyboardMarkup groupInlineKeyboard = new InlineKeyboardMarkup(
                        new InlineKeyboardButton[]{
                                new InlineKeyboardButton("1").callbackData("game_change_max_points__1"),
                                new InlineKeyboardButton("2").callbackData("game_change_max_points__2"),
                                new InlineKeyboardButton("3").callbackData("game_change_max_points__3"),
                        },
                        new InlineKeyboardButton[]{
                                new InlineKeyboardButton("4").callbackData("game_change_max_points__4"),
                                new InlineKeyboardButton("5").callbackData("game_change_max_points__5"),
                                new InlineKeyboardButton("6").callbackData("game_change_max_points__6"),
                        },
                        new InlineKeyboardButton[]{
                                new InlineKeyboardButton("7").callbackData("game_change_max_points__7"),
                                new InlineKeyboardButton("8").callbackData("game_change_max_points__8"),
                                new InlineKeyboardButton("9").callbackData("game_change_max_points__9"),
                        },
                        new InlineKeyboardButton[]{ new InlineKeyboardButton("⬅ Volver").callbackData("game_configure") }
                );

                botService.sendMessage(
                        new EditMessageText(telegramGame.getGame().getRoom().getId(), telegramGame.getGroupMessageId(),
                                getCreatedGameMessage(telegramGame) + "\n Selecciona nº máximo de jugadores:")
                                .replyMarkup(groupInlineKeyboard));
            } else {
                botService.sendMessage(new AnswerCallbackQuery(callbackQuery.id())
                        .text(ResponseErrorI18n.GAME_ONLY_CREATOR_CAN_CONFIGURE));
            }
        });

        callbackQueryHandlerMap.put("game_change_mode", (callbackQuery, data) -> {
            TelegramGame telegramGame = cclhService.getGame(callbackQuery.message().chat().id());

            if (callbackQuery.from().id().equals(telegramGame.getGame().getCreator().getId())) {
                cclhService.setType(telegramGame, GameTypeEnum.getEnum(Integer.parseInt(data)));

                sendConfigMenu(telegramGame);
            } else {
                botService.sendMessage(new AnswerCallbackQuery(callbackQuery.id())
                        .text(ResponseErrorI18n.GAME_ONLY_CREATOR_CAN_CONFIGURE));
            }
        });

        callbackQueryHandlerMap.put("game_change_deck", (callbackQuery, data) -> {
            TelegramGame telegramGame = cclhService.getGame(callbackQuery.message().chat().id());

            if (callbackQuery.from().id().equals(telegramGame.getGame().getCreator().getId())) {
                cclhService.setDeck(telegramGame, Integer.parseInt(data));

                sendConfigMenu(telegramGame);
            } else {
                botService.sendMessage(new AnswerCallbackQuery(callbackQuery.id())
                        .text(ResponseErrorI18n.GAME_ONLY_CREATOR_CAN_CONFIGURE));
            }
        });

        callbackQueryHandlerMap.put("game_change_max_players", (callbackQuery, data) -> {
            TelegramGame telegramGame = cclhService.getGame(callbackQuery.message().chat().id());

            if (callbackQuery.from().id().equals(telegramGame.getGame().getCreator().getId())) {
                cclhService.setMaxNumberOfPlayers(telegramGame, Integer.parseInt(data));

                sendConfigMenu(telegramGame);
            } else {
                botService.sendMessage(new AnswerCallbackQuery(callbackQuery.id())
                        .text(ResponseErrorI18n.GAME_ONLY_CREATOR_CAN_CONFIGURE));
            }
        });

        callbackQueryHandlerMap.put("game_change_max_points", (callbackQuery, data) -> {
            TelegramGame telegramGame = cclhService.getGame(callbackQuery.message().chat().id());

            if (callbackQuery.from().id().equals(telegramGame.getGame().getCreator().getId())) {
                cclhService.setNumberOfCardsToWin(telegramGame, Integer.parseInt(data));

                sendConfigMenu(telegramGame);
            } else {
                botService.sendMessage(new AnswerCallbackQuery(callbackQuery.id())
                        .text(ResponseErrorI18n.GAME_ONLY_CREATOR_CAN_CONFIGURE));
            }
        });

        callbackQueryHandlerMap.put("game_delete_group", (callbackQuery, data) -> {
            TelegramGame telegramGame = cclhService.getGame(callbackQuery.message().chat().id());

            if (callbackQuery.from().id().equals(telegramGame.getGame().getCreator().getId())) {
                List<TelegramPlayer> telegramPlayerList = cclhService.deleteGame(telegramGame);

                for (TelegramPlayer telegramPlayer : telegramPlayerList) {
                    botService.sendMessage(new EditMessageText(telegramPlayer.getPlayer().getUser().getId(),
                            telegramPlayer.getMessageId(),
                            ResponseMessageI18n.GAME_DELETED));
                }

                botService.sendMessage(new EditMessageText(telegramGame.getGame().getRoom().getId(),
                        telegramGame.getGroupMessageId(),
                        ResponseMessageI18n.GAME_DELETED));
                botService.sendMessage(new EditMessageText(telegramGame.getGame().getCreator().getId(),
                        telegramGame.getPrivateMessageId(),
                        ResponseMessageI18n.GAME_DELETED));
            } else {
                botService.sendMessage(new AnswerCallbackQuery(callbackQuery.id())
                        .text(ResponseErrorI18n.GAME_ONLY_CREATOR_CAN_CONFIGURE));
            }
        });

        callbackQueryHandlerMap.put("game_delete_private", (callbackQuery, data) -> {
            TelegramGame telegramGame = cclhService.getGameByCreatorId(callbackQuery.from().id());

            if (callbackQuery.from().id().equals(telegramGame.getGame().getCreator().getId())) {
                List<TelegramPlayer> telegramPlayerList = cclhService.deleteGame(telegramGame);

                for (TelegramPlayer telegramPlayer : telegramPlayerList) {
                    botService.sendMessage(new EditMessageText(telegramPlayer.getPlayer().getUser().getId(),
                            telegramPlayer.getMessageId(),
                            ResponseMessageI18n.GAME_DELETED));
                }

                botService.sendMessage(new EditMessageText(telegramGame.getGame().getRoom().getId(),
                        telegramGame.getGroupMessageId(),
                        ResponseMessageI18n.GAME_DELETED));
                botService.sendMessage(new EditMessageText(telegramGame.getGame().getCreator().getId(),
                        telegramGame.getPrivateMessageId(),
                        ResponseMessageI18n.GAME_DELETED));
            } else {
                botService.sendMessage(new AnswerCallbackQuery(callbackQuery.id())
                        .text(ResponseErrorI18n.GAME_ONLY_CREATOR_CAN_CONFIGURE));
            }
        });

        callbackQueryHandlerMap.put("game_join", (callbackQuery, data) -> botService.sendMessageAsync(new SendMessage(callbackQuery.from().id(), ResponseMessageI18n.PLAYER_CREATING),
            new Callback<SendMessage, SendResponse>() {
                @Override
                public void onResponse(SendMessage playerRequest, SendResponse playerResponse) {
                    try {
                        applicationService.joinGame(callbackQuery, playerResponse);
                    } catch (PlayerAlreadyExistsException e) {
                        botService.sendMessage(new AnswerCallbackQuery(callbackQuery.id())
                                .text(ResponseErrorI18n.PLAYER_ALREADY_JOINED));
                    }
                }

                @Override
                public void onFailure(SendMessage groupRequest, IOException e) {
                    logger.error("Fallo al enviar mensaje", e);
                }
            }));

        return callbackQueryHandlerMap;
    }

    private void sendMainMenu(TelegramGame telegramGame) {
        InlineKeyboardMarkup groupInlineKeyboard = new InlineKeyboardMarkup(
                new InlineKeyboardButton[]{ new InlineKeyboardButton("Unirse a la partida").callbackData("game_join") },
                new InlineKeyboardButton[]{ new InlineKeyboardButton("Configurar la partida").callbackData("game_configure") },
                new InlineKeyboardButton[]{ new InlineKeyboardButton("Borrar juego").callbackData("game_delete_group") }
        );
        botService.sendMessage(new EditMessageText(telegramGame.getGame().getRoom().getId(),
                telegramGame.getGroupMessageId(), getCreatedGameMessage(telegramGame))
                .replyMarkup(groupInlineKeyboard));

        InlineKeyboardMarkup privateInlineKeyboard = new InlineKeyboardMarkup(
                new InlineKeyboardButton("Borrar juego").callbackData("game_delete_private")
        );
        botService.sendMessage(new EditMessageText(telegramGame.getGame().getCreator().getId(),
                telegramGame.getPrivateMessageId(), ResponseMessageI18n.GAME_CREATED_PRIVATE)
                .replyMarkup(privateInlineKeyboard));
    }

    private void sendConfigMenu(TelegramGame telegramGame) {
        InlineKeyboardMarkup groupInlineKeyboard = new InlineKeyboardMarkup(
                new InlineKeyboardButton[]{ new InlineKeyboardButton("Cambiar modo").callbackData("game_sel_mode") },
                new InlineKeyboardButton[]{ new InlineKeyboardButton("Cambiar mazo de cartas").callbackData("game_sel_deck__1") },
                new InlineKeyboardButton[]{ new InlineKeyboardButton("Cambiar nº máximo de jugadores").callbackData("game_sel_max_players") },
                new InlineKeyboardButton[]{ new InlineKeyboardButton("Cambiar nº de puntos para ganar").callbackData("game_sel_max_points") },
                new InlineKeyboardButton[]{ new InlineKeyboardButton("⬅ Volver").callbackData("game_created") }
        );

        botService.sendMessage(
                new EditMessageText(telegramGame.getGame().getRoom().getId(), telegramGame.getGroupMessageId(),
                        getCreatedGameMessage(telegramGame))
                    .replyMarkup(groupInlineKeyboard));
    }

    private String getCreatedGameMessage(TelegramGame telegramGame) {
        return MessageFormat.format(ResponseMessageI18n.GAME_CREATED_GROUP,
                ResponseMessageI18n.getGameTypeName(telegramGame.getGame().getType()),
                telegramGame.getGame().getDeck().getName(),
                telegramGame.getGame().getNumberOfCardsToWin(),
                telegramGame.getGame().getMaxNumberOfPlayers(),
                telegramGame.getGame().getPlayers().size());
    }

}
