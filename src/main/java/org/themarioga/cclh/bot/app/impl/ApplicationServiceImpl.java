package org.themarioga.cclh.bot.app.impl;

import com.pengrad.telegrambot.Callback;
import com.pengrad.telegrambot.model.CallbackQuery;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.AnswerCallbackQuery;
import com.pengrad.telegrambot.request.DeleteMessage;
import com.pengrad.telegrambot.request.EditMessageText;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
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
import org.themarioga.cclh.bot.util.StringUtils;
import org.themarioga.cclh.commons.enums.GameStatusEnum;
import org.themarioga.cclh.commons.enums.GameTypeEnum;
import org.themarioga.cclh.commons.enums.TableStatusEnum;
import org.themarioga.cclh.commons.exceptions.ApplicationException;
import org.themarioga.cclh.commons.exceptions.game.GameAlreadyExistsException;
import org.themarioga.cclh.commons.exceptions.game.GameAlreadyFilledException;
import org.themarioga.cclh.commons.exceptions.game.GameAlreadyStartedException;
import org.themarioga.cclh.commons.exceptions.player.PlayerAlreadyExistsException;
import org.themarioga.cclh.commons.exceptions.player.PlayerAlreadyPlayedCardException;
import org.themarioga.cclh.commons.exceptions.player.PlayerAlreadyVotedDeleteException;
import org.themarioga.cclh.commons.exceptions.user.UserAlreadyExistsException;
import org.themarioga.cclh.commons.exceptions.user.UserDoesntExistsException;
import org.themarioga.cclh.commons.models.*;
import org.themarioga.cclh.commons.models.Dictionary;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.*;

@Service
public class ApplicationServiceImpl implements ApplicationService {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationServiceImpl.class);

    private final BotService botService;
    private final CCLHService cclhService;

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
                playerResponse.message().messageId(), ResponseMessageI18n.PLAYER_JOINED));
    }

    @Override
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ApplicationException.class)
    public void joinGame(CallbackQuery callbackQuery, SendResponse playerResponse) {
        TelegramGame telegramGame = cclhService.getGame(callbackQuery.message().chat().id());

        try {
            cclhService.joinGame(telegramGame, callbackQuery.from().id(),
                    playerResponse.message().messageId());

            InlineKeyboardMarkup privateInlineKeyboard = new InlineKeyboardMarkup(
                    new InlineKeyboardButton("Dejar la partida").callbackData("game_leave")
            );
            botService.sendMessage(new EditMessageText(callbackQuery.from().id(),
                    playerResponse.message().messageId(), ResponseMessageI18n.PLAYER_JOINED)
                    .replyMarkup(privateInlineKeyboard));

            sendMainMenu(telegramGame);
        } catch (UserDoesntExistsException e) {
            logger.error(e.getMessage(), e);

            botService.sendMessage(new AnswerCallbackQuery(callbackQuery.id())
                    .text(ResponseErrorI18n.GAME_USER_DOESNT_EXISTS));

            throw e;
        }
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

                botService.sendMessage(new SendMessage(message.chat().id(), ResponseMessageI18n.PLAYER_WELCOME));
            } catch (UserAlreadyExistsException e) {
                logger.error("El usuario {} ({}) esta intentando registrarse de nuevo.", message.from().id(),
                        BotUtils.getUsername(message.from()));

                botService.sendMessage(new SendMessage(message.chat().id(), ResponseErrorI18n.USER_ALREADY_REGISTERED));

                throw e;
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
                                                ResponseMessageI18n.PLAYER_JOINING),
                                        new Callback<SendMessage, SendResponse>() {
                                            @Override
                                            public void onResponse(SendMessage playerRequest, SendResponse playerResponse) {
                                                try {
                                                    applicationService.createGame(message, groupResponse,
                                                            privateResponse, playerResponse);
                                                } catch (GameAlreadyExistsException e) {
                                                    logger.error("Ya existe una partida para al sala {} ({}) o creado por {}.",
                                                            message.chat().id(), message.chat().title(), message.from().id());

                                                    botService.sendMessage(new EditMessageText(message.chat().id(),
                                                            groupResponse.message().messageId(),
                                                            ResponseErrorI18n.GAME_ALREADY_CREATED));

                                                    botService.sendMessage(new EditMessageText(message.from().id(),
                                                            privateResponse.message().messageId(),
                                                            ResponseErrorI18n.GAME_ALREADY_CREATED));

                                                    throw e;
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

                sendDeleteMessages(telegramGame, telegramPlayerList);
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

            if (telegramGame != null && callbackQuery.from().id().equals(telegramGame.getGame().getCreator().getId())) {
                InlineKeyboardMarkup groupInlineKeyboard = new InlineKeyboardMarkup(
                        new InlineKeyboardButton[]{ new InlineKeyboardButton("Democracia").callbackData("game_change_mode__0") },
                        new InlineKeyboardButton[]{ new InlineKeyboardButton("Clásico").callbackData("game_change_mode__1") },
                        new InlineKeyboardButton[]{ new InlineKeyboardButton("Dictadura").callbackData("game_change_mode__2") },
                        new InlineKeyboardButton[]{ new InlineKeyboardButton("⬅ Volver").callbackData("game_configure") }
                );

                botService.sendMessage(
                        new EditMessageText(telegramGame.getGame().getRoom().getId(), telegramGame.getGroupMessageId(),
                                getCreatedGameMessage(telegramGame) + "\n Selecciona modo de juego:")
                                .parseMode(ParseMode.HTML)
                                .replyMarkup(groupInlineKeyboard));
            } else {
                botService.sendMessage(new AnswerCallbackQuery(callbackQuery.id())
                        .text(ResponseErrorI18n.GAME_ONLY_CREATOR_CAN_CONFIGURE));
            }
        });

        callbackQueryHandlerMap.put("game_sel_dictionary", (callbackQuery, data) -> {
            TelegramGame telegramGame = cclhService.getGame(callbackQuery.message().chat().id());

            if (telegramGame != null && callbackQuery.from().id().equals(telegramGame.getGame().getCreator().getId())) {
                int pageNumber = Integer.parseInt(data);
                int dictionariesPerPage = cclhService.getDictionariesPerPage();
                int totalDictionaries = (int) cclhService.getDictionaryCount(callbackQuery.from().id());
                int firstResult = (pageNumber - 1) * dictionariesPerPage;
                List<Dictionary> dictionaryList = cclhService.getDictionariesPaginated(callbackQuery.from().id(), firstResult, dictionariesPerPage);

                InlineKeyboardMarkup groupInlineKeyboard = new InlineKeyboardMarkup();
                if (pageNumber > 1) {
                    groupInlineKeyboard.addRow(new InlineKeyboardButton("⬅").callbackData("game_sel_dictionary__" + (pageNumber - 1)));
                }
                for (Dictionary dictionary : dictionaryList) {
                    groupInlineKeyboard.addRow(new InlineKeyboardButton(dictionary.getName()).callbackData("game_change_dictionary__" + dictionary.getId()));
                }
                if (totalDictionaries > pageNumber * dictionariesPerPage) {
                    groupInlineKeyboard.addRow(new InlineKeyboardButton("➡").callbackData("game_sel_dictionary__" + (pageNumber + 1)));
                }
                groupInlineKeyboard.addRow(new InlineKeyboardButton("⬅ Volver").callbackData("game_configure"));

                botService.sendMessage(
                        new EditMessageText(telegramGame.getGame().getRoom().getId(), telegramGame.getGroupMessageId(),
                                getCreatedGameMessage(telegramGame) + "\n Selecciona el mazo:")
                                .parseMode(ParseMode.HTML)
                                .replyMarkup(groupInlineKeyboard));
            } else {
                botService.sendMessage(new AnswerCallbackQuery(callbackQuery.id())
                        .text(ResponseErrorI18n.GAME_ONLY_CREATOR_CAN_CONFIGURE));
            }
        });

        callbackQueryHandlerMap.put("game_sel_max_players", (callbackQuery, data) -> {
            TelegramGame telegramGame = cclhService.getGame(callbackQuery.message().chat().id());

            if (telegramGame != null && callbackQuery.from().id().equals(telegramGame.getGame().getCreator().getId())) {
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
                                .parseMode(ParseMode.HTML)
                                .replyMarkup(groupInlineKeyboard));
            } else {
                botService.sendMessage(new AnswerCallbackQuery(callbackQuery.id())
                        .text(ResponseErrorI18n.GAME_ONLY_CREATOR_CAN_CONFIGURE));
            }
        });

        callbackQueryHandlerMap.put("game_sel_max_points", (callbackQuery, data) -> {
            TelegramGame telegramGame = cclhService.getGame(callbackQuery.message().chat().id());

            if (telegramGame != null && callbackQuery.from().id().equals(telegramGame.getGame().getCreator().getId())) {
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
                                getCreatedGameMessage(telegramGame) + "\n Selecciona nº de puntos para ganar:")
                                .parseMode(ParseMode.HTML)
                                .replyMarkup(groupInlineKeyboard));
            } else {
                botService.sendMessage(new AnswerCallbackQuery(callbackQuery.id())
                        .text(ResponseErrorI18n.GAME_ONLY_CREATOR_CAN_CONFIGURE));
            }
        });

        callbackQueryHandlerMap.put("game_change_mode", (callbackQuery, data) -> {
            TelegramGame telegramGame = cclhService.getGame(callbackQuery.message().chat().id());

            if (telegramGame != null && callbackQuery.from().id().equals(telegramGame.getGame().getCreator().getId())) {
                cclhService.setType(telegramGame, GameTypeEnum.getEnum(Integer.parseInt(data)));

                sendConfigMenu(telegramGame);
            } else {
                botService.sendMessage(new AnswerCallbackQuery(callbackQuery.id())
                        .text(ResponseErrorI18n.GAME_ONLY_CREATOR_CAN_CONFIGURE));
            }
        });

        callbackQueryHandlerMap.put("game_change_dictionary", (callbackQuery, data) -> {
            TelegramGame telegramGame = cclhService.getGame(callbackQuery.message().chat().id());

            if (telegramGame != null && callbackQuery.from().id().equals(telegramGame.getGame().getCreator().getId())) {
                cclhService.setDictionary(telegramGame, Integer.parseInt(data));

                sendConfigMenu(telegramGame);
            } else {
                botService.sendMessage(new AnswerCallbackQuery(callbackQuery.id())
                        .text(ResponseErrorI18n.GAME_ONLY_CREATOR_CAN_CONFIGURE));
            }
        });

        callbackQueryHandlerMap.put("game_change_max_players", (callbackQuery, data) -> {
            TelegramGame telegramGame = cclhService.getGame(callbackQuery.message().chat().id());

            if (telegramGame != null && callbackQuery.from().id().equals(telegramGame.getGame().getCreator().getId())) {
                try {
                    cclhService.setMaxNumberOfPlayers(telegramGame, Integer.parseInt(data));

                    sendConfigMenu(telegramGame);
                } catch (GameAlreadyFilledException e) {
                    botService.sendMessage(new AnswerCallbackQuery(callbackQuery.id())
                            .text(ResponseErrorI18n.GAME_ALREADY_FILLED));

                    throw e;
                }
            } else {
                botService.sendMessage(new AnswerCallbackQuery(callbackQuery.id())
                        .text(ResponseErrorI18n.GAME_ONLY_CREATOR_CAN_CONFIGURE));
            }
        });

        callbackQueryHandlerMap.put("game_change_max_points", (callbackQuery, data) -> {
            TelegramGame telegramGame = cclhService.getGame(callbackQuery.message().chat().id());

            if (telegramGame != null && callbackQuery.from().id().equals(telegramGame.getGame().getCreator().getId())) {
                cclhService.setNumberOfCardsToWin(telegramGame, Integer.parseInt(data));

                sendConfigMenu(telegramGame);
            } else {
                botService.sendMessage(new AnswerCallbackQuery(callbackQuery.id())
                        .text(ResponseErrorI18n.GAME_ONLY_CREATOR_CAN_CONFIGURE));
            }
        });

        callbackQueryHandlerMap.put("game_delete_group", (callbackQuery, data) -> {
            TelegramGame telegramGame = cclhService.getGame(callbackQuery.message().chat().id());

            if (telegramGame != null && callbackQuery.from().id().equals(telegramGame.getGame().getCreator().getId())) {
                List<TelegramPlayer> telegramPlayerList = cclhService.deleteGame(telegramGame);

                sendDeleteMessages(telegramGame, telegramPlayerList);
            } else {
                if (telegramGame != null && telegramGame.getGame().getStatus().equals(GameStatusEnum.STARTED)) {
                    try {
                        cclhService.voteForDeletion(telegramGame, callbackQuery.from().id());

                        botService.sendMessage(new AnswerCallbackQuery(callbackQuery.id())
                                .text(ResponseMessageI18n.PLAYER_VOTED_DELETION));

                        if (telegramGame.getGame().getStatus().equals(GameStatusEnum.DELETING)) {
                            List<TelegramPlayer> telegramPlayerList = cclhService.getPlayers(telegramGame);

                            sendDeleteMessages(telegramGame, telegramPlayerList);
                        } else {
                            sendMainMenu(telegramGame);
                        }
                    } catch (PlayerAlreadyVotedDeleteException e) {
                        logger.error(e.getMessage(), e);

                        botService.sendMessage(new AnswerCallbackQuery(callbackQuery.id())
                                .text(ResponseErrorI18n.PLAYER_ALREADY_VOTED_DELETION));

                        throw e;
                    }
                } else {
                    botService.sendMessage(new AnswerCallbackQuery(callbackQuery.id())
                            .text(ResponseErrorI18n.GAME_ONLY_CREATOR_CAN_DELETE));
                }
            }
        });

        callbackQueryHandlerMap.put("game_delete_private", (callbackQuery, data) -> {
            TelegramGame telegramGame = cclhService.getGameByCreatorId(callbackQuery.from().id());

            if (telegramGame != null && callbackQuery.from().id().equals(telegramGame.getGame().getCreator().getId())) {
                List<TelegramPlayer> telegramPlayerList = cclhService.deleteGame(telegramGame);

                sendDeleteMessages(telegramGame, telegramPlayerList);
            } else {
                botService.sendMessage(new AnswerCallbackQuery(callbackQuery.id())
                        .text(ResponseErrorI18n.GAME_ONLY_CREATOR_CAN_DELETE));
            }
        });

        callbackQueryHandlerMap.put("game_join", (callbackQuery, data) -> botService.sendMessageAsync(
                new SendMessage(callbackQuery.from().id(), ResponseMessageI18n.PLAYER_JOINING),
            new Callback<SendMessage, SendResponse>() {
                @Override
                public void onResponse(SendMessage playerRequest, SendResponse playerResponse) {
                    try {
                        applicationService.joinGame(callbackQuery, playerResponse);
                    } catch (PlayerAlreadyExistsException e) {
                        botService.sendMessage(new AnswerCallbackQuery(callbackQuery.id())
                                .text(ResponseErrorI18n.PLAYER_ALREADY_JOINED));

                        throw e;
                    }
                }

                @Override
                public void onFailure(SendMessage groupRequest, IOException e) {
                    logger.error("Fallo al enviar mensaje", e);
                }
            }));

        callbackQueryHandlerMap.put("game_start", (callbackQuery, data) -> {
            botService.sendMessage(new AnswerCallbackQuery(callbackQuery.id())
                    .text(ResponseMessageI18n.GAME_STARTING));

            TelegramGame telegramGame = cclhService.getGame(callbackQuery.message().chat().id());

            if (telegramGame != null && callbackQuery.from().id().equals(telegramGame.getGame().getCreator().getId())) {
                try {
                    cclhService.startGame(telegramGame);

                    if (telegramGame.getGame().getStatus().equals(GameStatusEnum.STARTED)) {
                        startRound(telegramGame);
                    } else {
                        logger.error("La partida no se ha iniciado correctamente");
                    }
                } catch (GameAlreadyStartedException e) {
                    botService.sendMessage(new AnswerCallbackQuery(callbackQuery.id())
                            .text(ResponseErrorI18n.GAME_ALREADY_STARTED));

                    throw e;
                }
            } else {
                botService.sendMessage(new AnswerCallbackQuery(callbackQuery.id())
                        .text(ResponseErrorI18n.GAME_ONLY_CREATOR_CAN_CONFIGURE));
            }
        });

        callbackQueryHandlerMap.put("play_card", (callbackQuery, data) -> {
            botService.sendMessage(new AnswerCallbackQuery(callbackQuery.id())
                    .text(ResponseMessageI18n.PLAYER_PLAYING));

            TelegramGame telegramGame = cclhService.getByPlayerUser(callbackQuery.from().id());

            if (telegramGame != null) {
                try {
                    cclhService.playCard(telegramGame, callbackQuery.from().id(), Integer.parseInt(data));

                    if (telegramGame.getGame().getTable().getStatus().equals(TableStatusEnum.PLAYING)) {
                        TelegramPlayer telegramPlayer = cclhService.getPlayer(callbackQuery.from().id());

                        PlayedCard playedCard = getPlayedCardByPlayer(telegramGame, telegramPlayer);

                        if (playedCard != null) {
                            botService.sendMessage(new EditMessageText(telegramPlayer.getPlayer().getUser().getId(),
                                    telegramPlayer.getMessageId(),
                                    StringUtils.formatMessage(ResponseMessageI18n.PLAYER_SELECTED_CARD,
                                            telegramGame.getGame().getTable().getCurrentBlackCard().getText(),
                                            playedCard.getCard().getText())));
                        } else {
                            logger.error("No se ha encontrado el jugador o la carta jugada");

                            botService.sendMessage(new AnswerCallbackQuery(callbackQuery.id())
                                    .text(ResponseErrorI18n.PLAYER_DOES_NOT_EXISTS));
                        }
                    } else if (telegramGame.getGame().getTable().getStatus().equals(TableStatusEnum.VOTING)) {
                        if (telegramGame.getGame().getType().equals(GameTypeEnum.DEMOCRACY)) {
                            sendPlayedCardsMessageToGroup(telegramGame);

                            sendVotesToAllPlayers(telegramGame);
                        } else if (telegramGame.getGame().getType().equals(GameTypeEnum.CLASSIC)
                                    || telegramGame.getGame().getType().equals(GameTypeEnum.DICTATORSHIP)) {
                            sendVotesToPlayer(telegramGame, cclhService.getPlayer(telegramGame.getGame().getCreator().getId()));
                        }
                    } else {
                        logger.error("Juego en estado incorrecto: {}", telegramGame.getGame().getId());
                    }
                } catch (PlayerAlreadyPlayedCardException e) {
                    logger.error(e.getMessage(), e);

                    botService.sendMessage(new AnswerCallbackQuery(callbackQuery.id())
                            .text(ResponseErrorI18n.PLAYER_ALREADY_PLAYED_CARD));

                    throw e;
                }
            } else {
                botService.sendMessage(new AnswerCallbackQuery(callbackQuery.id())
                        .text(ResponseErrorI18n.PLAYER_DOES_NOT_EXISTS));
            }
        });

        callbackQueryHandlerMap.put("vote_card", (callbackQuery, data) -> {
            botService.sendMessage(new AnswerCallbackQuery(callbackQuery.id())
                    .text(ResponseMessageI18n.PLAYER_VOTING));

            TelegramGame telegramGame = cclhService.getByPlayerUser(callbackQuery.from().id());

            if (telegramGame != null) {
                try {
                    cclhService.voteCard(telegramGame, callbackQuery.from().id(), Integer.parseInt(data));

                    if (telegramGame.getGame().getTable().getStatus().equals(TableStatusEnum.VOTING)) {
                        sendMessageToVoter(telegramGame, callbackQuery);
                    } else if (telegramGame.getGame().getTable().getStatus().equals(TableStatusEnum.ENDING)) {
                        sendMessageToVoter(telegramGame, callbackQuery);
                        sendVotedCardsMessageToGroup(telegramGame);

                        cclhService.endRound(telegramGame);

                        if (telegramGame.getGame().getStatus().equals(GameStatusEnum.STARTED)) {
                            startRound(telegramGame);
                        } else if (telegramGame.getGame().getStatus().equals(GameStatusEnum.ENDED)) {
                            logger.debug("ACABA");
                            // ToDo: when everyone have voted
                        }
                    } else {
                        logger.error("Juego en estado incorrecto: {}", telegramGame.getGame().getId());
                    }
                } catch (PlayerAlreadyPlayedCardException e) {
                    logger.error(e.getMessage(), e);

                    botService.sendMessage(new AnswerCallbackQuery(callbackQuery.id())
                            .text(ResponseErrorI18n.PLAYER_ALREADY_PLAYED_CARD));

                    throw e;
                }
            } else {
                botService.sendMessage(new AnswerCallbackQuery(callbackQuery.id())
                        .text(ResponseErrorI18n.PLAYER_DOES_NOT_EXISTS));
            }
        });

        return callbackQueryHandlerMap;
    }

    private void sendMainMenu(TelegramGame telegramGame) {
        InlineKeyboardMarkup groupInlineKeyboard = new InlineKeyboardMarkup();

        if (telegramGame.getGame().getStatus().equals(GameStatusEnum.CREATED)) {
            if (telegramGame.getGame().getPlayers().size() < telegramGame.getGame().getMaxNumberOfPlayers()) {
                groupInlineKeyboard.addRow(new InlineKeyboardButton("Unirse a la partida").callbackData("game_join"));
            }

            groupInlineKeyboard.addRow(new InlineKeyboardButton("Configurar la partida").callbackData("game_configure"));

            if (telegramGame.getGame().getPlayers().size() >= cclhService.getMinNumberOfPlayers()) {
                groupInlineKeyboard.addRow(new InlineKeyboardButton("Iniciar la partida").callbackData("game_start"));
            }
        }

        groupInlineKeyboard.addRow(new InlineKeyboardButton("Borrar partida").callbackData("game_delete_group"));

        String msg = getCreatedGameMessage(telegramGame);
        if (!telegramGame.getGame().getStatus().equals(GameStatusEnum.STARTED)) {
            if (telegramGame.getGame().getPlayers().size() > 1) {
                msg += "\n\n" + getPlayerNumberMessage(telegramGame);
            }
        } else {
            if (!telegramGame.getGame().getDeletionVotes().isEmpty()) {
                msg += "\n\n" + getVoteDeletionNumberMessage(telegramGame);
            }
        }

        botService.sendMessage(new EditMessageText(telegramGame.getGame().getRoom().getId(),
                telegramGame.getGroupMessageId(), msg)
                .parseMode(ParseMode.HTML)
                .replyMarkup(groupInlineKeyboard));

        InlineKeyboardMarkup privateInlineKeyboard = new InlineKeyboardMarkup(
                new InlineKeyboardButton("Borrar juego").callbackData("game_delete_private")
        );
        botService.sendMessage(new EditMessageText(telegramGame.getGame().getCreator().getId(),
                telegramGame.getPrivateMessageId(), ResponseMessageI18n.PLAYER_CREATED_GAME)
                .replyMarkup(privateInlineKeyboard));
    }

    private void sendConfigMenu(TelegramGame telegramGame) {
        InlineKeyboardMarkup groupInlineKeyboard = new InlineKeyboardMarkup(
                new InlineKeyboardButton[]{ new InlineKeyboardButton("Cambiar modo").callbackData("game_sel_mode") },
                new InlineKeyboardButton[]{ new InlineKeyboardButton("Cambiar mazo de cartas").callbackData("game_sel_dictionary__1") },
                new InlineKeyboardButton[]{ new InlineKeyboardButton("Cambiar nº máximo de jugadores").callbackData("game_sel_max_players") },
                new InlineKeyboardButton[]{ new InlineKeyboardButton("Cambiar nº de puntos para ganar").callbackData("game_sel_max_points") },
                new InlineKeyboardButton[]{ new InlineKeyboardButton("⬅ Volver").callbackData("game_created") }
        );

        botService.sendMessage(
                new EditMessageText(telegramGame.getGame().getRoom().getId(), telegramGame.getGroupMessageId(),
                        getCreatedGameMessage(telegramGame))
                    .parseMode(ParseMode.HTML)
                    .replyMarkup(groupInlineKeyboard));
    }

    private void sendDeleteMessages(TelegramGame telegramGame, List<TelegramPlayer> telegramPlayerList) {
        for (TelegramPlayer telegramPlayer : telegramPlayerList) {
            botService.sendMessage(new EditMessageText(telegramPlayer.getPlayer().getUser().getId(),
                    telegramPlayer.getMessageId(),
                    ResponseMessageI18n.GAME_DELETED));
        }

        botService.sendMessage(new EditMessageText(telegramGame.getGame().getRoom().getId(),
                telegramGame.getGroupMessageId(),
                ResponseMessageI18n.GAME_DELETED));
        if (telegramGame.getGame().getStatus().equals(GameStatusEnum.STARTED)) {
            botService.sendMessage(new DeleteMessage(telegramGame.getGame().getRoom().getId(),
                    telegramGame.getBlackCardMessageId()));
        }
        botService.sendMessage(new EditMessageText(telegramGame.getGame().getCreator().getId(),
                telegramGame.getPrivateMessageId(),
                ResponseMessageI18n.GAME_DELETED));
    }

    private void startRound(TelegramGame telegramGame) {
        if (telegramGame.getGame().getTable().getStatus().equals(TableStatusEnum.STARTING)) {
            cclhService.startRound(telegramGame);

            if (telegramGame.getGame().getTable().getStatus().equals(TableStatusEnum.PLAYING)) {
                sendBlackCardToGroup(telegramGame);

                sendCardsToPlayers(telegramGame);
            } else {
                logger.error("La ronda no se ha iniciado correctamente");
            }
        } else {
            logger.error("La partida no se ha iniciado correctamente");
        }
    }

    private void sendBlackCardToGroup(TelegramGame telegramGame) {
        String msg = StringUtils.formatMessage(ResponseMessageI18n.GAME_SELECT_CARD,
                telegramGame.getGame().getTable().getCurrentBlackCard().getText());

        botService.sendMessageAsync(new SendMessage(telegramGame.getGame().getRoom().getId(), msg)
                        .parseMode(ParseMode.HTML),
            new Callback<SendMessage, SendResponse>() {
                @Override
                public void onResponse(SendMessage blackCardRequest, SendResponse blackCardResponse) {
                    cclhService.setBlackCardMessage(telegramGame, blackCardResponse.message().messageId());
                }

                @Override
                public void onFailure(SendMessage blackCardRequest, IOException e) {
                    logger.error("Fallo al enviar mensaje", e);
                }
            });
    }

    private void sendCardsToPlayers(TelegramGame telegramGame) {
        List<TelegramPlayer> telegramPlayers = new ArrayList<>(cclhService.getPlayers(telegramGame));
        if (!telegramGame.getGame().getType().equals(GameTypeEnum.DEMOCRACY)) {
            telegramPlayers.removeIf(telegramPlayer -> telegramPlayer.getPlayer().getId()
                    .equals(telegramGame.getGame().getTable().getCurrentPresident().getId()));
        }
        for (TelegramPlayer telegramPlayer : telegramPlayers) {
            InlineKeyboardMarkup playerInlineKeyboard = new InlineKeyboardMarkup();
            for (PlayerHandCard card : telegramPlayer.getPlayer().getHand()) {
                playerInlineKeyboard.addRow(new InlineKeyboardButton(card.getCard().getText())
                        .callbackData("play_card__" + card.getCard().getId()));
            }

            botService.sendMessage(new EditMessageText(telegramPlayer.getPlayer().getUser().getId(),
                    telegramPlayer.getMessageId(),
                    StringUtils.formatMessage(ResponseMessageI18n.PLAYER_SELECT_CARD,
                            telegramGame.getGame().getTable().getCurrentBlackCard().getText()))
                    .replyMarkup(playerInlineKeyboard));
        }
    }

    private void sendPlayedCardsMessageToGroup(TelegramGame telegramGame) {
        StringBuilder playedCards = new StringBuilder();
        for (PlayedCard playedCard : telegramGame.getGame().getTable().getPlayedCards()) {
            playedCards
                    .append("<b>")
                    .append(playedCard.getCard().getText())
                    .append("</b>")
                    .append("\n");
        }

        String msg = MessageFormat.format(ResponseMessageI18n.GAME_VOTE_CARD,
                telegramGame.getGame().getTable().getCurrentBlackCard().getText(),
                playedCards);

        botService.sendMessage(
                new EditMessageText(
                        telegramGame.getGame().getRoom().getId(),
                        telegramGame.getBlackCardMessageId(),
                        msg).parseMode(ParseMode.HTML));
    }

    private void sendVotedCardsMessageToGroup(TelegramGame telegramGame) {
        VotedCard votedCard = cclhService.getMostVotedCard(telegramGame);

        StringBuilder playedCards = new StringBuilder();
        for (PlayedCard playedCard : telegramGame.getGame().getTable().getPlayedCards()) {
            playedCards
                    .append("<b>")
                    .append(playedCard.getCard().getText())
                    .append("</b>")
                    .append(" - ")
                    .append(playedCard.getPlayer().getUser().getName())
                    .append("\n");
        }

        StringBuilder playerPoints = new StringBuilder();
        for (Player player : telegramGame.getGame().getPlayers()) {
            playerPoints
                    .append("<b>")
                    .append(player.getUser().getName())
                    .append("</b>")
                    .append(": ")
                    .append(player.getPoints())
                    .append("\n");
        }

        String msg = MessageFormat.format(ResponseMessageI18n.GAME_END_ROUND,
                votedCard.getPlayer().getUser().getName(),
                votedCard.getCard().getText(),
                telegramGame.getGame().getTable().getCurrentBlackCard().getText(),
                playedCards,
                playerPoints);

        botService.sendMessage(
                new EditMessageText(
                        telegramGame.getGame().getRoom().getId(),
                        telegramGame.getBlackCardMessageId(),
                        msg).parseMode(ParseMode.HTML));
    }

    private void sendVotesToAllPlayers(TelegramGame telegramGame) {
        List<TelegramPlayer> telegramPlayers = cclhService.getPlayers(telegramGame);

        for (TelegramPlayer tgPlayer : telegramPlayers) {
            sendVotesToPlayer(telegramGame, tgPlayer);
        }
    }

    private void sendVotesToPlayer(TelegramGame telegramGame, TelegramPlayer tgPlayer) {
        PlayedCard playedCard = getPlayedCardByPlayer(telegramGame, tgPlayer);
        if (playedCard != null) {
            List<PlayedCard> playedCards = telegramGame.getGame().getTable().getPlayedCards();

            InlineKeyboardMarkup cardInlineKeyboard = new InlineKeyboardMarkup();
            for (PlayedCard otherPlayerPlayedCard : playedCards) {
                if (!otherPlayerPlayedCard.getPlayer().getId().equals(tgPlayer.getPlayer().getId())) {
                    cardInlineKeyboard.addRow(new InlineKeyboardButton(otherPlayerPlayedCard.getCard().getText())
                            .callbackData("vote_card__" + otherPlayerPlayedCard.getCard().getId()));
                }
            }

            botService.sendMessage(new EditMessageText(tgPlayer.getPlayer().getUser().getId(),
                    tgPlayer.getMessageId(),
                    StringUtils.formatMessage(ResponseMessageI18n.PLAYER_VOTE_CARD,
                            telegramGame.getGame().getTable().getCurrentBlackCard().getText(),
                            playedCard.getCard().getText()))
                    .replyMarkup(cardInlineKeyboard));
        } else {
            logger.error("No se ha encontrado la carta del jugador");
        }
    }

    private void sendMessageToVoter(TelegramGame telegramGame, CallbackQuery callbackQuery) {
        TelegramPlayer telegramPlayer = cclhService.getPlayer(callbackQuery.from().id());

        PlayedCard playedCard = getPlayedCardByPlayer(telegramGame, telegramPlayer);
        VotedCard votedCard = getVotedCardByPlayer(telegramGame, telegramPlayer);

        if (playedCard != null && votedCard != null) {
            botService.sendMessage(new EditMessageText(telegramPlayer.getPlayer().getUser().getId(),
                    telegramPlayer.getMessageId(),
                    StringUtils.formatMessage(ResponseMessageI18n.PLAYER_VOTED_CARD,
                            telegramGame.getGame().getTable().getCurrentBlackCard().getText(),
                            playedCard.getCard().getText(),
                            votedCard.getCard().getText())));
        } else {
            logger.error("No se ha encontrado el jugador o la carta jugada");

            botService.sendMessage(new AnswerCallbackQuery(callbackQuery.id())
                    .text(ResponseErrorI18n.PLAYER_DOES_NOT_EXISTS));
        }
    }

    private String getCreatedGameMessage(TelegramGame telegramGame) {
        return MessageFormat.format(ResponseMessageI18n.GAME_CREATED_GROUP,
                ResponseMessageI18n.getGameTypeName(telegramGame.getGame().getType()),
                telegramGame.getGame().getDictionary().getName(),
                telegramGame.getGame().getNumberOfCardsToWin(),
                telegramGame.getGame().getMaxNumberOfPlayers());
    }

    private String getPlayerNumberMessage(TelegramGame telegramGame) {
        return MessageFormat.format(ResponseMessageI18n.GAME_CREATED_CURRENT_PLAYER_NUMBER,
                telegramGame.getGame().getPlayers().size());
    }

    private String getVoteDeletionNumberMessage(TelegramGame telegramGame) {
        return MessageFormat.format(ResponseMessageI18n.GAME_CREATED_CURRENT_VOTE_DELETION_NUMBER,
                telegramGame.getGame().getDeletionVotes().size());
    }

    private PlayedCard getPlayedCardByPlayer(TelegramGame telegramGame, TelegramPlayer telegramPlayer) {
        Optional<PlayedCard> e = telegramGame.getGame().getTable().getPlayedCards().stream()
                .filter(playedCard -> playedCard.getPlayer().getId()
                        .equals(telegramPlayer.getPlayer().getId())).findFirst();

	    return e.orElse(null);
    }

    private VotedCard getVotedCardByPlayer(TelegramGame telegramGame, TelegramPlayer telegramPlayer) {
        Optional<VotedCard> e = telegramGame.getGame().getTable().getVotedCards().stream()
                .filter(votedCard -> votedCard.getPlayer().getId()
                        .equals(telegramPlayer.getPlayer().getId())).findFirst();

        return e.orElse(null);
    }

}
