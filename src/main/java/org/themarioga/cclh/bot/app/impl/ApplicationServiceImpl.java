package org.themarioga.cclh.bot.app.impl;

import com.pengrad.telegrambot.Callback;
import com.pengrad.telegrambot.model.Chat;
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
import org.themarioga.cclh.commons.enums.GamePunctuationTypeEnum;
import org.themarioga.cclh.commons.enums.GameStatusEnum;
import org.themarioga.cclh.commons.enums.GameTypeEnum;
import org.themarioga.cclh.commons.enums.TableStatusEnum;
import org.themarioga.cclh.commons.exceptions.ApplicationException;
import org.themarioga.cclh.commons.exceptions.dictionary.DictionaryDoesntExistsException;
import org.themarioga.cclh.commons.exceptions.game.*;
import org.themarioga.cclh.commons.exceptions.player.PlayerAlreadyExistsException;
import org.themarioga.cclh.commons.exceptions.player.PlayerAlreadyPlayedCardException;
import org.themarioga.cclh.commons.exceptions.player.PlayerAlreadyVotedCardException;
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

    private BotService botService;
    private CCLHService cclhService;
    private ApplicationService applicationService;

    @Override
    public void run() {
        logger.info("Starting Bot...");

        try {
            botService.startBot(getBotCommands(), getCallbackQueries());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

        logger.info("Bot Started");
    }

    @Override
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ApplicationException.class)
    public void registerUser(long userId, String username) {
        try {
            cclhService.registerUser(userId, username);

            botService.sendMessage(new SendMessage(userId, ResponseMessageI18n.PLAYER_WELCOME));
        } catch (UserAlreadyExistsException e) {
            logger.error("El usuario {} ({}) esta intentando registrarse de nuevo.", userId, username);

            botService.sendMessage(new SendMessage(userId, ResponseErrorI18n.USER_ALREADY_REGISTERED));

            throw e;
        } catch (ApplicationException e) {
            botService.sendMessage(new SendMessage(userId, e.getMessage()));

            throw e;
        }
    }

    @Override
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ApplicationException.class)
    public void deleteMyGames(long userId) {
        TelegramGame telegramGame = cclhService.getGameByCreatorId(userId);

        if (telegramGame == null) {
            logger.error("No existe juego asociado al usuario {}", userId);

            botService.sendMessage(new SendMessage(userId, ResponseErrorI18n.PLAYER_NO_GAMES));

            return;
        }

        try {
            List<TelegramPlayer> telegramPlayerList = cclhService.deleteGame(telegramGame);

            sendDeleteMessages(telegramGame, telegramPlayerList);
        } catch (ApplicationException e) {
            botService.sendMessage(new SendMessage(userId, e.getMessage()));

            throw e;
        }
    }

    @Override
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ApplicationException.class)
    public void startCreatingGame(long roomId, String roomTitle, long creatorId) {
        botService.sendMessageAsync(new SendMessage(roomId, ResponseMessageI18n.GAME_CREATING),
                new Callback<SendMessage, SendResponse>() {
                    @Override
                    public void onResponse(SendMessage groupRequest, SendResponse groupResponse) {
                        botService.sendMessageAsync(new SendMessage(creatorId, ResponseMessageI18n.GAME_CREATING),
                                new Callback<SendMessage, SendResponse>() {
                                    @Override
                                    public void onResponse(SendMessage privateRequest, SendResponse privateResponse) {
                                        botService.sendMessageAsync(new SendMessage(creatorId,
                                                        ResponseMessageI18n.PLAYER_JOINING),
                                                new Callback<SendMessage, SendResponse>() {
                                                    @Override
                                                    public void onResponse(SendMessage playerRequest, SendResponse playerResponse) {
                                                        applicationService.createGame(roomId,
                                                                roomTitle,
                                                                creatorId,
                                                                groupResponse.message().messageId(),
                                                                privateResponse.message().messageId(),
                                                                playerResponse.message().messageId());
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
    }

    @Override
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ApplicationException.class)
    public void createGame(long roomId, String roomTitle, long creatorId, int groupMessageId, int privateMessageId, int playerMessageId) {
        try {
            TelegramGame telegramGame =
                    cclhService.createGame(roomId, roomTitle, creatorId, groupMessageId, privateMessageId, playerMessageId);

            sendMainMenu(telegramGame);

            botService.sendMessage(new EditMessageText(creatorId, privateMessageId, ResponseMessageI18n.PLAYER_JOINED));
        } catch (GameAlreadyExistsException e) {
            logger.error("Ya existe una partida para al sala {} ({}) o creado por {}.",
                    roomId, roomTitle, creatorId);

            botService.sendMessage(new EditMessageText(roomId,
                    groupMessageId,
                    ResponseErrorI18n.GAME_ALREADY_CREATED));

            botService.sendMessage(new EditMessageText(creatorId,
                    privateMessageId,
                    ResponseErrorI18n.GAME_ALREADY_CREATED));

            throw e;
        } catch (PlayerAlreadyExistsException e) {
            logger.error("El jugador {} que intenta crear la partida en la sala {}({}) ya está en otra partida.",
                    creatorId, roomId, roomTitle);

            botService.sendMessage(new EditMessageText(roomId,
                    groupMessageId,
                    ResponseErrorI18n.PLAYER_ALREADY_PLAYING));

            botService.sendMessage(new EditMessageText(creatorId,
                    privateMessageId,
                    ResponseErrorI18n.PLAYER_ALREADY_PLAYING));

            throw e;
        } catch (ApplicationException e) {
            botService.sendMessage(new SendMessage(creatorId, e.getMessage()));

            throw e;
        }
    }

    @Override
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ApplicationException.class)
    public void gameCreatedQuery(long roomId, long userId, String callbackQueryId) {
        TelegramGame telegramGame = cclhService.getGame(roomId);

        if (telegramGame == null) {
            logger.error("No hay partida activa en la sala {}", roomId);

            botService.sendMessage(new AnswerCallbackQuery(callbackQueryId)
                    .text(ResponseErrorI18n.GAME_DOESNT_EXISTS));

            return;
        }

        if (userId == telegramGame.getGame().getCreator().getId()) {
            sendMainMenu(telegramGame);
        } else {
            botService.sendMessage(new AnswerCallbackQuery(callbackQueryId)
                    .text(ResponseErrorI18n.GAME_ONLY_CREATOR_CAN_CONFIGURE));
        }
    }

    @Override
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ApplicationException.class)
    public void gameConfigureQuery(long roomId, long userId, String callbackQueryId) {
        TelegramGame telegramGame = cclhService.getGame(roomId);

        if (telegramGame == null) {
            logger.error("No hay partida activa en la sala {}", roomId);

            botService.sendMessage(new AnswerCallbackQuery(callbackQueryId)
                    .text(ResponseErrorI18n.GAME_DOESNT_EXISTS));

            return;
        }

        if (userId == telegramGame.getGame().getCreator().getId()) {
            sendConfigMenu(telegramGame);
        } else {
            botService.sendMessage(new AnswerCallbackQuery(callbackQueryId)
                    .text(ResponseErrorI18n.GAME_ONLY_CREATOR_CAN_CONFIGURE));
        }
    }

    @Override
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ApplicationException.class)
    public void gameSelectModeQuery(long roomId, long userId, String callbackQueryId) {
        TelegramGame telegramGame = cclhService.getGame(roomId);

        if (telegramGame == null) {
            logger.error("No hay partida activa en la sala {}", roomId);

            botService.sendMessage(new AnswerCallbackQuery(callbackQueryId)
                    .text(ResponseErrorI18n.GAME_DOESNT_EXISTS));

            return;
        }

        if (userId == telegramGame.getGame().getCreator().getId()) {
            InlineKeyboardMarkup groupInlineKeyboard = new InlineKeyboardMarkup(
                    new InlineKeyboardButton[]{ new InlineKeyboardButton("Democracia").callbackData("game_change_mode__0") },
                    new InlineKeyboardButton[]{ new InlineKeyboardButton("Clásico").callbackData("game_change_mode__1") },
                    new InlineKeyboardButton[]{ new InlineKeyboardButton("Dictadura").callbackData("game_change_mode__2") },
                    new InlineKeyboardButton[]{ new InlineKeyboardButton("⬅ Volver").callbackData("game_configure") }
            );

            botService.sendMessage(
                    new EditMessageText(telegramGame.getGame().getRoom().getId(), telegramGame.getGroupMessageId(),
                            getGameCreatedGroupMessage(telegramGame) + "\n Selecciona modo de juego:")
                            .parseMode(ParseMode.HTML)
                            .replyMarkup(groupInlineKeyboard));
        } else {
            botService.sendMessage(new AnswerCallbackQuery(callbackQueryId)
                    .text(ResponseErrorI18n.GAME_ONLY_CREATOR_CAN_CONFIGURE));
        }
    }

    @Override
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ApplicationException.class)
    public void gameSelectPunctuationModeQuery(long roomId, long userId, String callbackQueryId) {
        TelegramGame telegramGame = cclhService.getGame(roomId);

        if (telegramGame == null) {
            logger.error("No hay partida activa en la sala {}", roomId);

            botService.sendMessage(new AnswerCallbackQuery(callbackQueryId)
                    .text(ResponseErrorI18n.GAME_DOESNT_EXISTS));

            return;
        }

        if (userId == telegramGame.getGame().getCreator().getId()) {
            InlineKeyboardMarkup groupInlineKeyboard = new InlineKeyboardMarkup(
                    new InlineKeyboardButton[]{ new InlineKeyboardButton("Rondas").callbackData("game_sel_n_rounds") },
                    new InlineKeyboardButton[]{ new InlineKeyboardButton("Puntos").callbackData("game_sel_n_points") },
                    new InlineKeyboardButton[]{ new InlineKeyboardButton("⬅ Volver").callbackData("game_configure") }
            );

            botService.sendMessage(
                    new EditMessageText(telegramGame.getGame().getRoom().getId(), telegramGame.getGroupMessageId(),
                            getGameCreatedGroupMessage(telegramGame) + "\n Selecciona modo de puntuación:")
                            .parseMode(ParseMode.HTML)
                            .replyMarkup(groupInlineKeyboard));
        } else {
            botService.sendMessage(new AnswerCallbackQuery(callbackQueryId)
                    .text(ResponseErrorI18n.GAME_ONLY_CREATOR_CAN_CONFIGURE));
        }
    }

    @Override
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ApplicationException.class)
    public void gameSelectNRoundsToEndQuery(long roomId, long userId, String callbackQueryId) {
        TelegramGame telegramGame = cclhService.getGame(roomId);

        if (telegramGame == null) {
            logger.error("No hay partida activa en la sala {}", roomId);

            botService.sendMessage(new AnswerCallbackQuery(callbackQueryId)
                    .text(ResponseErrorI18n.GAME_DOESNT_EXISTS));

            return;
        }

        if (userId == telegramGame.getGame().getCreator().getId()) {
            InlineKeyboardMarkup groupInlineKeyboard = new InlineKeyboardMarkup(
                    new InlineKeyboardButton[]{
                            new InlineKeyboardButton("1").callbackData("game_change_max_rounds__1"),
                            new InlineKeyboardButton("2").callbackData("game_change_max_rounds__2"),
                            new InlineKeyboardButton("3").callbackData("game_change_max_rounds__3"),
                    },
                    new InlineKeyboardButton[]{
                            new InlineKeyboardButton("4").callbackData("game_change_max_rounds__4"),
                            new InlineKeyboardButton("5").callbackData("game_change_max_rounds__5"),
                            new InlineKeyboardButton("6").callbackData("game_change_max_rounds__6"),
                    },
                    new InlineKeyboardButton[]{
                            new InlineKeyboardButton("7").callbackData("game_change_max_rounds__7"),
                            new InlineKeyboardButton("8").callbackData("game_change_max_rounds__8"),
                            new InlineKeyboardButton("9").callbackData("game_change_max_rounds__9"),
                    },
                    new InlineKeyboardButton[]{ new InlineKeyboardButton("⬅ Volver").callbackData("game_configure") }
            );

            botService.sendMessage(
                    new EditMessageText(telegramGame.getGame().getRoom().getId(), telegramGame.getGroupMessageId(),
                            getGameCreatedGroupMessage(telegramGame) + "\n Selecciona rondas de la partida:")
                            .parseMode(ParseMode.HTML)
                            .replyMarkup(groupInlineKeyboard));
        } else {
            botService.sendMessage(new AnswerCallbackQuery(callbackQueryId)
                    .text(ResponseErrorI18n.GAME_ONLY_CREATOR_CAN_CONFIGURE));
        }
    }

    @Override
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ApplicationException.class)
    public void gameSelectNPointsToWinQuery(long roomId, long userId, String callbackQueryId) {
        TelegramGame telegramGame = cclhService.getGame(roomId);

        if (telegramGame == null) {
            logger.error("No hay partida activa en la sala {}", roomId);

            botService.sendMessage(new AnswerCallbackQuery(callbackQueryId)
                    .text(ResponseErrorI18n.GAME_DOESNT_EXISTS));

            return;
        }

        if (userId == telegramGame.getGame().getCreator().getId()) {
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
                            getGameCreatedGroupMessage(telegramGame) + "\n Selecciona nº de puntos para ganar:")
                            .parseMode(ParseMode.HTML)
                            .replyMarkup(groupInlineKeyboard));
        } else {
            botService.sendMessage(new AnswerCallbackQuery(callbackQueryId)
                    .text(ResponseErrorI18n.GAME_ONLY_CREATOR_CAN_CONFIGURE));
        }
    }

    @Override
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ApplicationException.class)
    public void gameSelectDictionaryQuery(long roomId, long userId, String callbackQueryId, String data) {
        TelegramGame telegramGame = cclhService.getGame(roomId);

        if (telegramGame == null) {
            logger.error("No hay partida activa en la sala {}", roomId);

            botService.sendMessage(new AnswerCallbackQuery(callbackQueryId)
                    .text(ResponseErrorI18n.GAME_DOESNT_EXISTS));

            return;
        }

        if (userId == telegramGame.getGame().getCreator().getId()) {
            int pageNumber = Integer.parseInt(data);
            int dictionariesPerPage = cclhService.getDictionariesPerPage();
            int totalDictionaries = (int) cclhService.getDictionaryCount(userId);
            int firstResult = (pageNumber - 1) * dictionariesPerPage;
            List<Dictionary> dictionaryList = cclhService.getDictionariesPaginated(userId, firstResult, dictionariesPerPage);

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
                            getGameCreatedGroupMessage(telegramGame) + "\n Selecciona el mazo:")
                            .parseMode(ParseMode.HTML)
                            .replyMarkup(groupInlineKeyboard));
        } else {
            botService.sendMessage(new AnswerCallbackQuery(callbackQueryId)
                    .text(ResponseErrorI18n.GAME_ONLY_CREATOR_CAN_CONFIGURE));
        }
    }

    @Override
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ApplicationException.class)
    public void gameSelectMaxPlayersQuery(long roomId, long userId, String callbackQueryId) {
        TelegramGame telegramGame = cclhService.getGame(roomId);

        if (telegramGame == null) {
            logger.error("No hay partida activa en la sala {}", roomId);

            botService.sendMessage(new AnswerCallbackQuery(callbackQueryId)
                    .text(ResponseErrorI18n.GAME_DOESNT_EXISTS));

            return;
        }

        if (userId == telegramGame.getGame().getCreator().getId()) {
            InlineKeyboardMarkup groupInlineKeyboard = new InlineKeyboardMarkup(
                    new InlineKeyboardButton[]{
                            new InlineKeyboardButton("3").callbackData("game_change_max_players__3"),
                            new InlineKeyboardButton("4").callbackData("game_change_max_players__4"),
                            new InlineKeyboardButton("5").callbackData("game_change_max_players__5"),
                    },
                    new InlineKeyboardButton[]{
                            new InlineKeyboardButton("6").callbackData("game_change_max_players__6"),
                            new InlineKeyboardButton("7").callbackData("game_change_max_players__7"),
                            new InlineKeyboardButton("8").callbackData("game_change_max_players__8"),
                    },
                    new InlineKeyboardButton[]{
                            new InlineKeyboardButton("9").callbackData("game_change_max_players__9"),
                    },
                    new InlineKeyboardButton[]{ new InlineKeyboardButton("⬅ Volver").callbackData("game_configure") }
            );

            botService.sendMessage(
                    new EditMessageText(telegramGame.getGame().getRoom().getId(), telegramGame.getGroupMessageId(),
                            getGameCreatedGroupMessage(telegramGame) + "\n Selecciona nº máximo de jugadores:")
                            .parseMode(ParseMode.HTML)
                            .replyMarkup(groupInlineKeyboard));
        } else {
            botService.sendMessage(new AnswerCallbackQuery(callbackQueryId)
                    .text(ResponseErrorI18n.GAME_ONLY_CREATOR_CAN_CONFIGURE));
        }
    }

    @Override
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ApplicationException.class)
    public void gameChangeMode(long roomId, long userId, String callbackQueryId, String data) {
        TelegramGame telegramGame = cclhService.getGame(roomId);

        if (telegramGame == null) {
            logger.error("No hay partida activa en la sala {}", roomId);

            botService.sendMessage(new AnswerCallbackQuery(callbackQueryId)
                    .text(ResponseErrorI18n.GAME_DOESNT_EXISTS));

            return;
        }

        if (userId == telegramGame.getGame().getCreator().getId()) {
            try {
                cclhService.setType(telegramGame, GameTypeEnum.getEnum(Integer.parseInt(data)));

                sendConfigMenu(telegramGame);
            } catch (GameAlreadyStartedException e) {
                logger.error("La partida de la sala {} ya estaba iniciada cuando se intentó cambiar el modo", roomId);

                botService.sendMessage(new AnswerCallbackQuery(callbackQueryId)
                        .text(ResponseErrorI18n.GAME_ALREADY_STARTED));

                throw e;
            } catch (ApplicationException e) {
                botService.sendMessage(new AnswerCallbackQuery(callbackQueryId)
                        .text(e.getMessage()));

                throw e;
            }
        } else {
            botService.sendMessage(new AnswerCallbackQuery(callbackQueryId)
                    .text(ResponseErrorI18n.GAME_ONLY_CREATOR_CAN_CONFIGURE));
        }
    }

    @Override
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ApplicationException.class)
    public void gameChangeDictionary(long roomId, long userId, String callbackQueryId, String data) {
        TelegramGame telegramGame = cclhService.getGame(roomId);

        if (telegramGame == null) {
            logger.error("No hay partida activa en la sala {}", roomId);

            botService.sendMessage(new AnswerCallbackQuery(callbackQueryId)
                    .text(ResponseErrorI18n.GAME_DOESNT_EXISTS));

            return;
        }

        if (userId == telegramGame.getGame().getCreator().getId()) {
            try {
                cclhService.setDictionary(telegramGame, Integer.parseInt(data));

                sendConfigMenu(telegramGame);
            } catch (GameAlreadyStartedException e) {
                logger.error("La partida de la sala {} ya estaba iniciada cuando se intentó cambiar el modo", roomId);

                botService.sendMessage(new AnswerCallbackQuery(callbackQueryId)
                        .text(ResponseErrorI18n.GAME_ALREADY_STARTED));

                throw e;
            } catch (DictionaryDoesntExistsException e) {
                logger.error("El diccionario {} no existe para la sala {}", data, roomId);

                botService.sendMessage(new AnswerCallbackQuery(callbackQueryId)
                        .text(ResponseErrorI18n.UNKNOWN_ERROR));

                throw e;
            } catch (ApplicationException e) {
                botService.sendMessage(new AnswerCallbackQuery(callbackQueryId)
                        .text(e.getMessage()));

                throw e;
            }
        } else {
            botService.sendMessage(new AnswerCallbackQuery(callbackQueryId)
                    .text(ResponseErrorI18n.GAME_ONLY_CREATOR_CAN_CONFIGURE));
        }
    }

    @Override
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ApplicationException.class)
    public void gameChangeMaxPlayers(long roomId, long userId, String callbackQueryId, String data) {
        TelegramGame telegramGame = cclhService.getGame(roomId);

        if (telegramGame == null) {
            logger.error("No hay partida activa en la sala {}", roomId);

            botService.sendMessage(new AnswerCallbackQuery(callbackQueryId)
                    .text(ResponseErrorI18n.GAME_DOESNT_EXISTS));

            return;
        }

        if (userId == telegramGame.getGame().getCreator().getId()) {
            try {
                cclhService.setMaxNumberOfPlayers(telegramGame, Integer.parseInt(data));

                sendConfigMenu(telegramGame);
            } catch (GameAlreadyStartedException e) {
                logger.error("La partida de la sala {} ya estaba iniciada cuando se intentó cambiar el modo", roomId);

                botService.sendMessage(new AnswerCallbackQuery(callbackQueryId)
                        .text(ResponseErrorI18n.GAME_ALREADY_STARTED));

                throw e;
            } catch (GameAlreadyFilledException e) {
                botService.sendMessage(new AnswerCallbackQuery(callbackQueryId)
                        .text(ResponseErrorI18n.GAME_ALREADY_FILLED));

                throw e;
            } catch (ApplicationException e) {
                botService.sendMessage(new AnswerCallbackQuery(callbackQueryId)
                        .text(e.getMessage()));

                throw e;
            }
        } else {
            botService.sendMessage(new AnswerCallbackQuery(callbackQueryId)
                    .text(ResponseErrorI18n.GAME_ONLY_CREATOR_CAN_CONFIGURE));
        }
    }

    @Override
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ApplicationException.class)
    public void gameChangeNRoundsToEnd(long roomId, long userId, String callbackQueryId, String data) {
        TelegramGame telegramGame = cclhService.getGame(roomId);

        if (telegramGame == null) {
            logger.error("No hay partida activa en la sala {}", roomId);

            botService.sendMessage(new AnswerCallbackQuery(callbackQueryId)
                    .text(ResponseErrorI18n.GAME_DOESNT_EXISTS));

            return;
        }

        if (userId == telegramGame.getGame().getCreator().getId()) {
            try {
                cclhService.setNumberOfRoundsToEnd(telegramGame, Integer.parseInt(data));

                sendConfigMenu(telegramGame);
            } catch (GameAlreadyStartedException e) {
                logger.error("La partida de la sala {} ya estaba iniciada cuando se intentó cambiar el modo", roomId);

                botService.sendMessage(new AnswerCallbackQuery(callbackQueryId)
                        .text(ResponseErrorI18n.GAME_ALREADY_STARTED));

                throw e;
            } catch (ApplicationException e) {
                botService.sendMessage(new AnswerCallbackQuery(callbackQueryId)
                        .text(e.getMessage()));

                throw e;
            }
        } else {
            botService.sendMessage(new AnswerCallbackQuery(callbackQueryId)
                    .text(ResponseErrorI18n.GAME_ONLY_CREATOR_CAN_CONFIGURE));
        }
    }

    @Override
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ApplicationException.class)
    public void gameChangeNCardsToWin(long roomId, long userId, String callbackQueryId, String data) {
        TelegramGame telegramGame = cclhService.getGame(roomId);

        if (telegramGame == null) {
            logger.error("No hay partida activa en la sala {}", roomId);

            botService.sendMessage(new AnswerCallbackQuery(callbackQueryId)
                    .text(ResponseErrorI18n.GAME_DOESNT_EXISTS));

            return;
        }

        if (userId == telegramGame.getGame().getCreator().getId()) {
            try {
                cclhService.setNumberOfCardsToWin(telegramGame, Integer.parseInt(data));

                sendConfigMenu(telegramGame);
            } catch (GameAlreadyStartedException e) {
                logger.error("La partida de la sala {} ya estaba iniciada cuando se intentó cambiar el modo", roomId);

                botService.sendMessage(new AnswerCallbackQuery(callbackQueryId)
                        .text(ResponseErrorI18n.GAME_ALREADY_STARTED));

                throw e;
            } catch (ApplicationException e) {
                botService.sendMessage(new AnswerCallbackQuery(callbackQueryId)
                        .text(e.getMessage()));

                throw e;
            }
        } else {
            botService.sendMessage(new AnswerCallbackQuery(callbackQueryId)
                    .text(ResponseErrorI18n.GAME_ONLY_CREATOR_CAN_CONFIGURE));
        }
    }

    @Override
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ApplicationException.class)
    public void gameDeleteGroupQuery(long roomId, long userId, String callbackQueryId) {
        TelegramGame telegramGame = cclhService.getGame(roomId);

        if (telegramGame == null) {
            logger.error("No hay partida activa en la sala {}", roomId);

            botService.sendMessage(new AnswerCallbackQuery(callbackQueryId)
                    .text(ResponseErrorI18n.GAME_DOESNT_EXISTS));

            return;
        }

        if (userId == telegramGame.getGame().getCreator().getId()) {
            try {
                List<TelegramPlayer> telegramPlayerList = cclhService.deleteGame(telegramGame);

                sendDeleteMessages(telegramGame, telegramPlayerList);
            } catch (ApplicationException e) {
                botService.sendMessage(new AnswerCallbackQuery(callbackQueryId)
                        .text(e.getMessage()));

                throw e;
            }
        } else {
            if (telegramGame.getGame().getStatus().equals(GameStatusEnum.STARTED)) {
                try {
                    cclhService.voteForDeletion(telegramGame, userId);

                    botService.sendMessage(new AnswerCallbackQuery(callbackQueryId)
                            .text(ResponseMessageI18n.PLAYER_VOTED_DELETION));

                    if (telegramGame.getGame().getStatus().equals(GameStatusEnum.DELETING)) {
                        List<TelegramPlayer> telegramPlayerList = cclhService.deleteGame(telegramGame);

                        sendDeleteMessages(telegramGame, telegramPlayerList);
                    } else {
                        sendMainMenu(telegramGame);
                    }
                } catch (GameNotStartedException e) {
                    botService.sendMessage(new AnswerCallbackQuery(callbackQueryId)
                            .text(ResponseErrorI18n.GAME_DOESNT_EXISTS));

                    throw e;
                } catch (PlayerAlreadyVotedDeleteException e) {
                    botService.sendMessage(new AnswerCallbackQuery(callbackQueryId)
                            .text(ResponseErrorI18n.PLAYER_ALREADY_VOTED_DELETION));

                    throw e;
                } catch (ApplicationException e) {
                    botService.sendMessage(new AnswerCallbackQuery(callbackQueryId)
                            .text(e.getMessage()));

                    throw e;
                }
            } else {
                botService.sendMessage(new AnswerCallbackQuery(callbackQueryId)
                        .text(ResponseErrorI18n.GAME_ONLY_CREATOR_CAN_DELETE));
            }
        }
    }

    @Override
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ApplicationException.class)
    public void gameDeletePrivateQuery(long userId, String callbackQueryId) {
        TelegramGame telegramGame = cclhService.getGameByCreatorId(userId);

        if (telegramGame == null) {
            logger.error("No existe juego asociado al usuario {}", userId);

            botService.sendMessage(new SendMessage(userId, ResponseErrorI18n.PLAYER_NO_GAMES));

            return;
        }

        try {
            List<TelegramPlayer> telegramPlayerList = cclhService.deleteGame(telegramGame);

            sendDeleteMessages(telegramGame, telegramPlayerList);
        } catch (ApplicationException e) {
            botService.sendMessage(new AnswerCallbackQuery(callbackQueryId)
                    .text(e.getMessage()));

            throw e;
        }
    }

    @Override
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ApplicationException.class)
    public void gameJoinQuery(long roomId, long userId, String callbackQueryId) {
        TelegramGame telegramGame = cclhService.getGame(roomId);

        if (telegramGame == null) {
            logger.error("No hay partida activa en la sala {}", roomId);

            botService.sendMessage(new AnswerCallbackQuery(callbackQueryId)
                    .text(ResponseErrorI18n.GAME_DOESNT_EXISTS));

            return;
        }

        if (userId != telegramGame.getGame().getCreator().getId()) {
            botService.sendMessageAsync(
                    new SendMessage(userId, ResponseMessageI18n.PLAYER_JOINING),
                    new Callback<SendMessage, SendResponse>() {
                        @Override
                        public void onResponse(SendMessage playerRequest, SendResponse playerResponse) {
                            try {
                                applicationService.joinGame(
                                        roomId,
                                        userId,
                                        callbackQueryId,
                                        playerResponse.message().messageId());
                            } catch (PlayerAlreadyExistsException e) {
                                botService.sendMessage(new AnswerCallbackQuery(callbackQueryId)
                                        .text(ResponseErrorI18n.PLAYER_ALREADY_JOINED));

                                throw e;
                            }
                        }

                        @Override
                        public void onFailure(SendMessage groupRequest, IOException e) {
                            logger.error("Fallo al enviar mensaje", e);
                        }
                    });
        } else {
            botService.sendMessage(new AnswerCallbackQuery(callbackQueryId)
                    .text(ResponseErrorI18n.PLAYER_ALREADY_JOINED));
        }
    }

    @Override
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ApplicationException.class)
    public void joinGame(long roomId, long userId, String callbackQueryId, int playerMessageId) {
        TelegramGame telegramGame = cclhService.getGame(roomId);

        try {
            cclhService.joinGame(telegramGame, userId, playerMessageId);

            InlineKeyboardMarkup privateInlineKeyboard = new InlineKeyboardMarkup(
                    new InlineKeyboardButton("Dejar la partida").callbackData("game_leave")
            );
            botService.sendMessage(new EditMessageText(userId, playerMessageId, ResponseMessageI18n.PLAYER_JOINED)
                    .replyMarkup(privateInlineKeyboard));

            sendMainMenu(telegramGame);
        } catch (UserDoesntExistsException e) {
            botService.sendMessage(new AnswerCallbackQuery(callbackQueryId)
                    .text(ResponseErrorI18n.GAME_USER_DOESNT_EXISTS));

            throw e;
        } catch (GameAlreadyStartedException e) {
            botService.sendMessage(new AnswerCallbackQuery(callbackQueryId)
                    .text(ResponseErrorI18n.GAME_ALREADY_STARTED));

            throw e;
        } catch (GameAlreadyFilledException e) {
            botService.sendMessage(new AnswerCallbackQuery(callbackQueryId)
                    .text(ResponseErrorI18n.GAME_ALREADY_FILLED));

            throw e;
        } catch (ApplicationException e) {
            botService.sendMessage(new AnswerCallbackQuery(callbackQueryId)
                    .text(e.getMessage()));

            throw e;
        }
    }

    @Override
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ApplicationException.class)
    public void gameStartQuery(long roomId, long userId, String callbackQueryId) {
        TelegramGame telegramGame = cclhService.getGame(roomId);

        if (telegramGame == null) {
            logger.error("No hay partida activa en la sala {}", roomId);

            botService.sendMessage(new AnswerCallbackQuery(callbackQueryId)
                    .text(ResponseErrorI18n.GAME_DOESNT_EXISTS));

            return;
        }

        if (userId == telegramGame.getGame().getCreator().getId()) {
            try {
                cclhService.startGame(telegramGame);

                if (telegramGame.getGame().getStatus().equals(GameStatusEnum.STARTED)) {
                    startRound(telegramGame);
                } else {
                    logger.error("La partida no se ha iniciado correctamente");

                    botService.sendMessage(new AnswerCallbackQuery(callbackQueryId)
                            .text(ResponseErrorI18n.UNKNOWN_ERROR));
                }
            } catch (GameAlreadyStartedException e) {
                botService.sendMessage(new AnswerCallbackQuery(callbackQueryId)
                        .text(ResponseErrorI18n.GAME_ALREADY_STARTED));

                throw e;
            } catch (GameNotFilledException e) {
                botService.sendMessage(new AnswerCallbackQuery(callbackQueryId)
                        .text(ResponseErrorI18n.GAME_NOT_FILLED));

                throw e;
            } catch (GameNotStartedException e) {
                botService.sendMessage(new AnswerCallbackQuery(callbackQueryId)
                        .text(ResponseErrorI18n.GAME_NOT_STARTED));

                throw e;
            } catch (ApplicationException e) {
                botService.sendMessage(new AnswerCallbackQuery(callbackQueryId)
                        .text(e.getMessage()));

                throw e;
            }
        } else {
            logger.error("Intentando iniciar una partida en {} que no le corresponde a {}", roomId, userId);

            botService.sendMessage(new AnswerCallbackQuery(callbackQueryId)
                    .text(ResponseErrorI18n.GAME_ONLY_CREATOR_CAN_START));
        }
    }

    @Override
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ApplicationException.class)
    public void playerPlayCardQuery(long userId, String callbackQueryId, String data) {
        TelegramGame telegramGame = cclhService.getByPlayerUser(userId);

        if (telegramGame == null) {
            logger.error("No existe juego asociado al usuario {}", userId);

            botService.sendMessage(new AnswerCallbackQuery(callbackQueryId).text(ResponseErrorI18n.PLAYER_NO_GAMES));

            return;
        }

        try {
            cclhService.playCard(telegramGame, userId, Integer.parseInt(data));

            if (telegramGame.getGame().getTable().getStatus().equals(TableStatusEnum.PLAYING)) {
                TelegramPlayer telegramPlayer = cclhService.getPlayer(userId);

                PlayedCard playedCard = getPlayedCardByPlayer(telegramGame, telegramPlayer);

                if (playedCard != null) {
                    botService.sendMessage(new EditMessageText(telegramPlayer.getPlayer().getUser().getId(),
                            telegramPlayer.getMessageId(),
                            StringUtils.formatMessage(ResponseMessageI18n.PLAYER_SELECTED_CARD,
                                    telegramGame.getGame().getTable().getCurrentBlackCard().getText(),
                                    playedCard.getCard().getText())));
                } else {
                    logger.error("No se ha encontrado el jugador o la carta jugada");

                    botService.sendMessage(new AnswerCallbackQuery(callbackQueryId)
                            .text(ResponseErrorI18n.PLAYER_DOES_NOT_EXISTS));
                }
            } else if (telegramGame.getGame().getTable().getStatus().equals(TableStatusEnum.VOTING)) {
                if (telegramGame.getGame().getType().equals(GameTypeEnum.DEMOCRACY)) {
                    botService.sendMessage(
                            new EditMessageText(
                                    telegramGame.getGame().getRoom().getId(),
                                    telegramGame.getBlackCardMessageId(),
                                    getGameVoteCardMessage(telegramGame))
                                    .parseMode(ParseMode.HTML));

                    List<TelegramPlayer> telegramPlayers = cclhService.getPlayers(telegramGame);

                    for (TelegramPlayer tgPlayer : telegramPlayers) {
                        sendVotesToPlayer(telegramGame, tgPlayer);
                    }
                } else if (telegramGame.getGame().getType().equals(GameTypeEnum.CLASSIC)
                        || telegramGame.getGame().getType().equals(GameTypeEnum.DICTATORSHIP)) {
                    sendVotesToPlayer(telegramGame, cclhService.getPlayer(telegramGame.getGame().getCreator().getId()));
                }
            } else {
                logger.error("Juego en estado incorrecto: {}", telegramGame.getGame().getId());
            }
        } catch (PlayerAlreadyPlayedCardException e) {
            botService.sendMessage(new AnswerCallbackQuery(callbackQueryId)
                    .text(ResponseErrorI18n.PLAYER_ALREADY_PLAYED_CARD));

            throw e;
        } catch (ApplicationException e) {
            botService.sendMessage(new AnswerCallbackQuery(callbackQueryId)
                    .text(e.getMessage()));

            throw e;
        }
    }

    @Override
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ApplicationException.class)
    public void playerVoteCardQuery(long userId, String callbackQueryId, String data) {
        TelegramGame telegramGame = cclhService.getByPlayerUser(userId);

        if (telegramGame == null) {
            logger.error("No existe juego asociado al usuario {}", userId);

            botService.sendMessage(new SendMessage(userId, ResponseErrorI18n.PLAYER_NO_GAMES));

            return;
        }

        try {
            cclhService.voteCard(telegramGame, userId, Integer.parseInt(data));

            if (telegramGame.getGame().getTable().getStatus().equals(TableStatusEnum.VOTING)) {
                sendMessageToVoter(telegramGame, userId, callbackQueryId);
            } else if (telegramGame.getGame().getTable().getStatus().equals(TableStatusEnum.ENDING)) {
                sendMessageToVoter(telegramGame, userId, callbackQueryId);

                PlayedCard mostVotedCard = cclhService.getMostVotedCard(telegramGame);

                botService.sendMessage(
                        new EditMessageText(
                                telegramGame.getGame().getRoom().getId(),
                                telegramGame.getBlackCardMessageId(),
                                getGameEndRoundMessage(telegramGame, mostVotedCard))
                                .parseMode(ParseMode.HTML));

                cclhService.endRound(telegramGame);

                if (telegramGame.getGame().getStatus().equals(GameStatusEnum.STARTED)) {
                    startRound(telegramGame);
                } else if (telegramGame.getGame().getStatus().equals(GameStatusEnum.ENDED)) {
                    Player winner = getWinnerPlayer(telegramGame);

                    if (winner != null) {
                        botService.sendMessage(new SendMessage(telegramGame.getGame().getRoom().getId(),
                                getGameEndGameMessage(winner)).parseMode(ParseMode.HTML));

                        List<TelegramPlayer> telegramPlayerList = cclhService.deleteGame(telegramGame);

                        sendEndMessages(telegramGame, telegramPlayerList);
                    } else {
                        logger.error("Juego en estado incorrecto: {}", telegramGame.getGame().getId());

                        botService.sendMessage(new AnswerCallbackQuery(callbackQueryId)
                                .text(ResponseErrorI18n.UNKNOWN_ERROR));
                    }
                }
            } else {
                logger.error("Juego en estado incorrecto: {}", telegramGame.getGame().getId());

                botService.sendMessage(new AnswerCallbackQuery(callbackQueryId)
                        .text(ResponseErrorI18n.UNKNOWN_ERROR));
            }
        } catch (PlayerAlreadyVotedCardException e) {
            botService.sendMessage(new AnswerCallbackQuery(callbackQueryId)
                    .text(ResponseErrorI18n.PLAYER_ALREADY_VOTED_CARD));

            throw e;
        } catch (ApplicationException e) {
            botService.sendMessage(new AnswerCallbackQuery(callbackQueryId)
                    .text(e.getMessage()));

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

            applicationService.registerUser(message.from().id(), BotUtils.getUsername(message.from()));
        });

        commands.put("/create", message -> {
            if (message.chat().type().equals(Chat.Type.Private)) {
                logger.error("Comando /start enviado en lugar incorrecto por {}", BotUtils.getUserInfo(message.from()));

                botService.sendMessage(new SendMessage(message.chat().id(), ResponseErrorI18n.COMMAND_SHOULD_BE_ON_GROUP));

                return;
            }

            applicationService.startCreatingGame(message.chat().id(), message.chat().title(), message.from().id());
        });

        commands.put("/deleteMyGames", message -> {
            if (!message.chat().type().equals(Chat.Type.Private)) {
                logger.error("Comando /start enviado en lugar incorrecto por {}", BotUtils.getUserInfo(message.from()));

                botService.sendMessage(new SendMessage(message.chat().id(), ResponseErrorI18n.COMMAND_SHOULD_BE_ON_PRIVATE));

                return;
            }

            applicationService.deleteMyGames(message.from().id());
        });

        commands.put("/help", message -> botService.sendMessage(new SendMessage(message.chat().id(), ResponseMessageI18n.HELP)));

        return commands;
    }

    private Map<String, CallbackQueryHandler> getCallbackQueries() {
        Map<String, CallbackQueryHandler> callbackQueryHandlerMap = new HashMap<>();

        callbackQueryHandlerMap.put("game_created", (callbackQuery, data) -> {
            applicationService.gameCreatedQuery(callbackQuery.message().chat().id(), callbackQuery.from().id(),
                    callbackQuery.id());

            botService.sendMessage(new AnswerCallbackQuery(callbackQuery.id()));
        });

        callbackQueryHandlerMap.put("game_configure", (callbackQuery, data) -> {
            applicationService.gameConfigureQuery(callbackQuery.message().chat().id(), callbackQuery.from().id(),
                    callbackQuery.id());

            botService.sendMessage(new AnswerCallbackQuery(callbackQuery.id()));
        });

        callbackQueryHandlerMap.put("game_sel_mode", (callbackQuery, data) -> {
            applicationService.gameSelectModeQuery(callbackQuery.message().chat().id(), callbackQuery.from().id(),
                    callbackQuery.id());

            botService.sendMessage(new AnswerCallbackQuery(callbackQuery.id()));
        });

        callbackQueryHandlerMap.put("game_sel_point_type", (callbackQuery, data) -> {
            applicationService.gameSelectPunctuationModeQuery(callbackQuery.message().chat().id(), callbackQuery.from().id(),
                    callbackQuery.id());

            botService.sendMessage(new AnswerCallbackQuery(callbackQuery.id()));
        });

        callbackQueryHandlerMap.put("game_sel_dictionary", (callbackQuery, data) -> {
            applicationService.gameSelectDictionaryQuery(callbackQuery.message().chat().id(), callbackQuery.from().id(),
                    callbackQuery.id(), data);

            botService.sendMessage(new AnswerCallbackQuery(callbackQuery.id()));
        });

        callbackQueryHandlerMap.put("game_sel_max_players", (callbackQuery, data) -> {
            applicationService.gameSelectMaxPlayersQuery(callbackQuery.message().chat().id(), callbackQuery.from().id(),
                    callbackQuery.id());

            botService.sendMessage(new AnswerCallbackQuery(callbackQuery.id()));
        });

        callbackQueryHandlerMap.put("game_sel_n_rounds", (callbackQuery, data) -> {
            applicationService.gameSelectNRoundsToEndQuery(callbackQuery.message().chat().id(), callbackQuery.from().id(),
                    callbackQuery.id());

            botService.sendMessage(new AnswerCallbackQuery(callbackQuery.id()));
        });

        callbackQueryHandlerMap.put("game_sel_n_points", (callbackQuery, data) -> {
            applicationService.gameSelectNPointsToWinQuery(callbackQuery.message().chat().id(), callbackQuery.from().id(),
                    callbackQuery.id());

            botService.sendMessage(new AnswerCallbackQuery(callbackQuery.id()));
        });

        callbackQueryHandlerMap.put("game_change_mode", (callbackQuery, data) -> {
            applicationService.gameChangeMode(callbackQuery.message().chat().id(), callbackQuery.from().id(),
                    callbackQuery.id(), data);

            botService.sendMessage(new AnswerCallbackQuery(callbackQuery.id()));
        });

        callbackQueryHandlerMap.put("game_change_dictionary", (callbackQuery, data) -> {
            applicationService.gameChangeDictionary(callbackQuery.message().chat().id(), callbackQuery.from().id(),
                    callbackQuery.id(), data);

            botService.sendMessage(new AnswerCallbackQuery(callbackQuery.id()));
        });

        callbackQueryHandlerMap.put("game_change_max_players", (callbackQuery, data) -> {
            applicationService.gameChangeMaxPlayers(callbackQuery.message().chat().id(), callbackQuery.from().id(),
                    callbackQuery.id(), data);

            botService.sendMessage(new AnswerCallbackQuery(callbackQuery.id()));
        });

        callbackQueryHandlerMap.put("game_change_max_rounds", (callbackQuery, data) -> {
            applicationService.gameChangeNRoundsToEnd(callbackQuery.message().chat().id(), callbackQuery.from().id(),
                    callbackQuery.id(), data);

            botService.sendMessage(new AnswerCallbackQuery(callbackQuery.id()));
        });

        callbackQueryHandlerMap.put("game_change_max_points", (callbackQuery, data) -> {
            applicationService.gameChangeNCardsToWin(callbackQuery.message().chat().id(), callbackQuery.from().id(),
                    callbackQuery.id(), data);

            botService.sendMessage(new AnswerCallbackQuery(callbackQuery.id()));
        });

        callbackQueryHandlerMap.put("game_join", (callbackQuery, data) -> {
            applicationService.gameJoinQuery(callbackQuery.message().chat().id(), callbackQuery.from().id(),
                    callbackQuery.id());

            botService.sendMessage(new AnswerCallbackQuery(callbackQuery.id()));
        });

        callbackQueryHandlerMap.put("game_start", (callbackQuery, data) -> {
            applicationService.gameStartQuery(callbackQuery.message().chat().id(), callbackQuery.from().id(),
                    callbackQuery.id());

            botService.sendMessage(new AnswerCallbackQuery(callbackQuery.id()));
        });

        callbackQueryHandlerMap.put("play_card", (callbackQuery, data) -> {
            applicationService.playerPlayCardQuery(callbackQuery.from().id(), callbackQuery.id(), data);

            botService.sendMessage(new AnswerCallbackQuery(callbackQuery.id()));
        });

        callbackQueryHandlerMap.put("vote_card", (callbackQuery, data) -> {
            applicationService.playerVoteCardQuery(callbackQuery.from().id(), callbackQuery.id(), data);

            botService.sendMessage(new AnswerCallbackQuery(callbackQuery.id()));
        });

        callbackQueryHandlerMap.put("game_delete_group", (callbackQuery, data) -> {
            applicationService.gameDeleteGroupQuery(callbackQuery.message().chat().id(), callbackQuery.from().id(),
                    callbackQuery.id());

            botService.sendMessage(new AnswerCallbackQuery(callbackQuery.id()));
        });

        callbackQueryHandlerMap.put("game_delete_private", (callbackQuery, data) -> {
            applicationService.gameDeletePrivateQuery(callbackQuery.from().id(), callbackQuery.id());

            botService.sendMessage(new AnswerCallbackQuery(callbackQuery.id()));
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

        String msg = getGameCreatedGroupMessage(telegramGame);
        if (!telegramGame.getGame().getStatus().equals(GameStatusEnum.STARTED)) {
            if (telegramGame.getGame().getPlayers().size() > 1) {
                msg += "\n\n" + getGameCreatedCurrentPlayerNumberMessage(telegramGame);
            }
        } else {
            if (!telegramGame.getGame().getDeletionVotes().isEmpty()) {
                msg += "\n\n" + getGameCreatedCurrentVoteDeletionNumberMessage(telegramGame);
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
                new InlineKeyboardButton[]{ new InlineKeyboardButton("Cambiar modo de juego").callbackData("game_sel_mode") },
                new InlineKeyboardButton[]{ new InlineKeyboardButton("Cambiar modo de puntuacion").callbackData("game_sel_point_type") },
                new InlineKeyboardButton[]{ new InlineKeyboardButton("Cambiar mazo de cartas").callbackData("game_sel_dictionary__1") },
                new InlineKeyboardButton[]{ new InlineKeyboardButton("Cambiar nº máximo de jugadores").callbackData("game_sel_max_players") },
                new InlineKeyboardButton[]{ new InlineKeyboardButton("⬅ Volver").callbackData("game_created") }
        );

        botService.sendMessage(
                new EditMessageText(telegramGame.getGame().getRoom().getId(), telegramGame.getGroupMessageId(),
                        getGameCreatedGroupMessage(telegramGame))
                    .parseMode(ParseMode.HTML)
                    .replyMarkup(groupInlineKeyboard));
    }

    private void sendDeleteMessages(TelegramGame telegramGame, List<TelegramPlayer> telegramPlayerList) {
        // Delete game messages
        for (TelegramPlayer telegramPlayer : telegramPlayerList) {
            botService.sendMessage(new DeleteMessage(telegramPlayer.getPlayer().getUser().getId(),
                    telegramPlayer.getMessageId()));
        }
        if (telegramGame.getGame().getStatus().equals(GameStatusEnum.STARTED)) {
            botService.sendMessage(new DeleteMessage(telegramGame.getGame().getRoom().getId(),
                    telegramGame.getBlackCardMessageId()));
        }

        // Edit game messages
        botService.sendMessage(new EditMessageText(telegramGame.getGame().getRoom().getId(),
                telegramGame.getGroupMessageId(),
                ResponseMessageI18n.GAME_DELETED));
        botService.sendMessage(new EditMessageText(telegramGame.getGame().getCreator().getId(),
                telegramGame.getPrivateMessageId(),
                ResponseMessageI18n.GAME_DELETED));
    }

    private void sendEndMessages(TelegramGame telegramGame, List<TelegramPlayer> telegramPlayerList) {
        // Delete game messages
        for (TelegramPlayer telegramPlayer : telegramPlayerList) {
            botService.sendMessage(new EditMessageText(telegramPlayer.getPlayer().getUser().getId(),
                    telegramPlayer.getMessageId(),
                    ""));
        }

        // Edit game messages
        botService.sendMessage(new DeleteMessage(telegramGame.getGame().getRoom().getId(),
                telegramGame.getGroupMessageId()));
        botService.sendMessage(new DeleteMessage(telegramGame.getGame().getCreator().getId(),
                telegramGame.getPrivateMessageId()));
    }

    private void startRound(TelegramGame telegramGame) {
        if (telegramGame.getGame().getTable().getStatus().equals(TableStatusEnum.STARTING)) {
            cclhService.startRound(telegramGame);

            if (telegramGame.getGame().getTable().getStatus().equals(TableStatusEnum.PLAYING)) {
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
            } else {
                logger.error("La ronda no se ha iniciado correctamente");
            }
        } else {
            logger.error("La partida no se ha iniciado correctamente");
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
                    tgPlayer.getMessageId(), getPlayerVoteCardMessage(telegramGame, playedCard))
                    .replyMarkup(cardInlineKeyboard));
        } else {
            logger.error("No se ha encontrado la carta del jugador");
        }
    }

    private void sendMessageToVoter(TelegramGame telegramGame, long userId, String callbackQueryId) {
        TelegramPlayer telegramPlayer = cclhService.getPlayer(userId);

        PlayedCard playedCard = getPlayedCardByPlayer(telegramGame, telegramPlayer);
        VotedCard votedCard = getVotedCardByPlayer(telegramGame, telegramPlayer);

        if (playedCard != null && votedCard != null) {
            botService.sendMessage(new EditMessageText(telegramPlayer.getPlayer().getUser().getId(),
                    telegramPlayer.getMessageId(), getPlayerVotedCardMessage(telegramGame, playedCard, votedCard)));
        } else {
            logger.error("No se ha encontrado el jugador o la carta jugada");

            botService.sendMessage(new AnswerCallbackQuery(callbackQueryId)
                    .text(ResponseErrorI18n.PLAYER_DOES_NOT_EXISTS));
        }
    }

    private String getGameCreatedGroupMessage(TelegramGame telegramGame) {
        String msg = ResponseMessageI18n.GAME_CREATED_GROUP;

        msg += "\n";

        msg += MessageFormat.format(ResponseMessageI18n.GAME_SELECTED_MODE,
                ResponseMessageI18n.getGameTypeName(telegramGame.getGame().getType()));

        msg += "\n";

        msg += MessageFormat.format(ResponseMessageI18n.GAME_SELECTED_DICTIONARY,
                telegramGame.getGame().getDictionary().getName());

        msg += "\n";

        if (telegramGame.getGame().getPunctuationType() == GamePunctuationTypeEnum.POINTS) {
            msg += MessageFormat.format(ResponseMessageI18n.GAME_SELECTED_POINTS_TO_WIN,
                    telegramGame.getGame().getNumberOfCardsToWin());
        } else if (telegramGame.getGame().getPunctuationType() == GamePunctuationTypeEnum.ROUNDS) {
            msg += MessageFormat.format(ResponseMessageI18n.GAME_SELECTED_ROUNDS_TO_END,
                    telegramGame.getGame().getNumberOfCardsToWin());
        }

        msg += "\n";

        msg += MessageFormat.format(ResponseMessageI18n.GAME_SELECTED_MAX_PLAYER_NUMBER,
                telegramGame.getGame().getMaxNumberOfPlayers());

        return msg;
    }

    private String getGameCreatedCurrentPlayerNumberMessage(TelegramGame telegramGame) {
        return MessageFormat.format(ResponseMessageI18n.GAME_CREATED_CURRENT_PLAYER_NUMBER,
                telegramGame.getGame().getPlayers().size());
    }

    private String getGameCreatedCurrentVoteDeletionNumberMessage(TelegramGame telegramGame) {
        return MessageFormat.format(ResponseMessageI18n.GAME_CREATED_CURRENT_VOTE_DELETION_NUMBER,
                telegramGame.getGame().getDeletionVotes().size());
    }

    private String getGameVoteCardMessage(TelegramGame telegramGame) {
        StringBuilder playedCards = new StringBuilder();
        for (PlayedCard playedCard : telegramGame.getGame().getTable().getPlayedCards()) {
            playedCards
                    .append("<b>")
                    .append(playedCard.getCard().getText())
                    .append("</b>")
                    .append("\n");
        }

        return MessageFormat.format(ResponseMessageI18n.GAME_VOTE_CARD,
                telegramGame.getGame().getTable().getCurrentRoundNumber(),
                telegramGame.getGame().getTable().getCurrentBlackCard().getText(),
                playedCards);
    }

    private String getGameEndRoundMessage(TelegramGame telegramGame, PlayedCard mostVotedCard) {
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

        return MessageFormat.format(ResponseMessageI18n.GAME_END_ROUND,
                telegramGame.getGame().getTable().getCurrentRoundNumber(),
                mostVotedCard.getPlayer().getUser().getName(),
                mostVotedCard.getCard().getText(),
                telegramGame.getGame().getTable().getCurrentBlackCard().getText(),
                playedCards,
                playerPoints);
    }

    private String getPlayerVoteCardMessage(TelegramGame telegramGame, PlayedCard playedCard) {
        return MessageFormat.format(ResponseMessageI18n.PLAYER_VOTE_CARD,
                telegramGame.getGame().getTable().getCurrentRoundNumber(),
                telegramGame.getGame().getTable().getCurrentBlackCard().getText(),
                playedCard.getCard().getText());
    }

    private String getPlayerVotedCardMessage(TelegramGame telegramGame, PlayedCard playedCard, VotedCard votedCard) {
        return MessageFormat.format(ResponseMessageI18n.PLAYER_VOTED_CARD,
                telegramGame.getGame().getTable().getCurrentRoundNumber(),
                telegramGame.getGame().getTable().getCurrentBlackCard().getText(),
                playedCard.getCard().getText(),
                votedCard.getCard().getText());
    }

    private String getGameEndGameMessage(Player winner) {
        return MessageFormat.format(ResponseMessageI18n.GAME_END_GAME, winner.getUser().getName());
    }

    private Player getWinnerPlayer(TelegramGame telegramGame) {
        Optional<Player> e = telegramGame.getGame().getPlayers().stream()
                .max(Comparator.comparing(Player::getPoints));

        return e.orElse(null);
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

    @Autowired
    public void setBotService(BotService botService) {
        this.botService = botService;
    }

    @Autowired
    public void setCCLHService(CCLHService cclhService) {
        this.cclhService = cclhService;
    }

    @Autowired
    public void setApplicationService(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

}
