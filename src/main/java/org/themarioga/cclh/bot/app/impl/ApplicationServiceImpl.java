package org.themarioga.cclh.bot.app.impl;

import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.themarioga.cclh.bot.app.intf.ApplicationService;
import org.themarioga.cclh.bot.app.intf.BotService;
import org.themarioga.cclh.bot.app.intf.CCLHService;
import org.themarioga.cclh.bot.constants.ResponseErrorI18n;
import org.themarioga.cclh.bot.constants.ResponseMessageI18n;
import org.themarioga.cclh.bot.model.TelegramGame;
import org.themarioga.cclh.bot.model.TelegramPlayer;
import org.themarioga.cclh.bot.util.BotUtils;
import org.themarioga.cclh.bot.util.CallbackQueryHandler;
import org.themarioga.cclh.bot.util.CommandHandler;
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
import org.themarioga.cclh.commons.models.Dictionary;
import org.themarioga.cclh.commons.models.*;

import java.text.MessageFormat;
import java.util.*;

@Service
public class ApplicationServiceImpl implements ApplicationService {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationServiceImpl.class);

    BotService botService;
    CCLHService cclhService;
    ApplicationService applicationService;

    @Override
    public Map<String, CommandHandler> getBotCommands() {
        Map<String, CommandHandler> commands = new HashMap<>();

        commands.put("/start", message -> {
            if (!message.getChat().getType().equals("private")) {
                logger.error("Comando /start enviado en lugar incorrecto por {}", BotUtils.getUserInfo(message.getFrom()));

                botService.sendMessage(message.getChat().getId(), ResponseErrorI18n.COMMAND_SHOULD_BE_ON_PRIVATE);

                return;
            }

            applicationService.registerUser(message.getFrom().getId(), BotUtils.getUsername(message.getFrom()));
        });

        commands.put("/create", message -> {
            if (message.getChat().getType().equals("private")) {
                logger.error("Comando /start enviado en lugar incorrecto por {}", BotUtils.getUserInfo(message.getFrom()));

                botService.sendMessage(message.getChat().getId(), ResponseErrorI18n.COMMAND_SHOULD_BE_ON_GROUP);

                return;
            }

            applicationService.startCreatingGame(message.getChat().getId(), message.getChat().getTitle(), message.getFrom().getId());
        });

        commands.put("/deleteMyGames", message -> {
            if (!message.getChat().getType().equals("private")) {
                logger.error("Comando /start enviado en lugar incorrecto por {}", BotUtils.getUserInfo(message.getFrom()));


                botService.sendMessage(message.getChat().getId(), ResponseErrorI18n.COMMAND_SHOULD_BE_ON_PRIVATE);

                return;
            }

            applicationService.deleteMyGames(message.getFrom().getId());
        });

        commands.put("/help", message -> botService.sendMessage(message.getChat().getId(), ResponseMessageI18n.HELP));

        return commands;
    }

    @Override
    public Map<String, CallbackQueryHandler> getCallbackQueries() {
        Map<String, CallbackQueryHandler> callbackQueryHandlerMap = new HashMap<>();

        callbackQueryHandlerMap.put("game_created", (callbackQuery, data) -> {
            applicationService.gameCreatedQuery(callbackQuery.getMessage().getChatId(), callbackQuery.getFrom().getId(),
                    callbackQuery.getId());

            botService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("game_configure", (callbackQuery, data) -> {
            applicationService.gameConfigureQuery(callbackQuery.getMessage().getChatId(), callbackQuery.getFrom().getId(),
                    callbackQuery.getId());

            botService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("game_sel_mode", (callbackQuery, data) -> {
            applicationService.gameSelectModeQuery(callbackQuery.getMessage().getChatId(), callbackQuery.getFrom().getId(),
                    callbackQuery.getId());

            botService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("game_sel_point_type", (callbackQuery, data) -> {
            applicationService.gameSelectPunctuationModeQuery(callbackQuery.getMessage().getChatId(), callbackQuery.getFrom().getId(),
                    callbackQuery.getId());

            botService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("game_sel_dictionary", (callbackQuery, data) -> {
            applicationService.gameSelectDictionaryQuery(callbackQuery.getMessage().getChatId(), callbackQuery.getFrom().getId(),
                    callbackQuery.getId(), data);

            botService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("game_sel_max_players", (callbackQuery, data) -> {
            applicationService.gameSelectMaxPlayersQuery(callbackQuery.getMessage().getChatId(), callbackQuery.getFrom().getId(),
                    callbackQuery.getId());

            botService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("game_sel_n_rounds", (callbackQuery, data) -> {
            applicationService.gameSelectNRoundsToEndQuery(callbackQuery.getMessage().getChatId(), callbackQuery.getFrom().getId(),
                    callbackQuery.getId());

            botService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("game_sel_n_points", (callbackQuery, data) -> {
            applicationService.gameSelectNPointsToWinQuery(callbackQuery.getMessage().getChatId(), callbackQuery.getFrom().getId(),
                    callbackQuery.getId());

            botService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("game_change_mode", (callbackQuery, data) -> {
            applicationService.gameChangeMode(callbackQuery.getMessage().getChatId(), callbackQuery.getFrom().getId(),
                    callbackQuery.getId(), data);

            botService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("game_change_dictionary", (callbackQuery, data) -> {
            applicationService.gameChangeDictionary(callbackQuery.getMessage().getChatId(), callbackQuery.getFrom().getId(),
                    callbackQuery.getId(), data);

            botService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("game_change_max_players", (callbackQuery, data) -> {
            applicationService.gameChangeMaxPlayers(callbackQuery.getMessage().getChatId(), callbackQuery.getFrom().getId(),
                    callbackQuery.getId(), data);

            botService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("game_change_max_rounds", (callbackQuery, data) -> {
            applicationService.gameChangeNRoundsToEnd(callbackQuery.getMessage().getChatId(), callbackQuery.getFrom().getId(),
                    callbackQuery.getId(), data);

            botService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("game_change_max_points", (callbackQuery, data) -> {
            applicationService.gameChangeNCardsToWin(callbackQuery.getMessage().getChatId(), callbackQuery.getFrom().getId(),
                    callbackQuery.getId(), data);

            botService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("game_join", (callbackQuery, data) -> {
            applicationService.gameJoinQuery(callbackQuery.getMessage().getChatId(), callbackQuery.getFrom().getId(),
                    callbackQuery.getId());

            botService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("game_start", (callbackQuery, data) -> {
            applicationService.gameStartQuery(callbackQuery.getMessage().getChatId(), callbackQuery.getFrom().getId(),
                    callbackQuery.getId());

            botService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("play_card", (callbackQuery, data) -> {
            applicationService.playerPlayCardQuery(callbackQuery.getFrom().getId(), callbackQuery.getId(), data);

            botService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("vote_card", (callbackQuery, data) -> {
            applicationService.playerVoteCardQuery(callbackQuery.getFrom().getId(), callbackQuery.getId(), data);

            botService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("game_delete_group", (callbackQuery, data) -> {
            applicationService.gameDeleteGroupQuery(callbackQuery.getMessage().getChatId(), callbackQuery.getFrom().getId(),
                    callbackQuery.getId());

            botService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("game_delete_private", (callbackQuery, data) -> {
            applicationService.gameDeletePrivateQuery(callbackQuery.getFrom().getId(), callbackQuery.getId());

            botService.answerCallbackQuery(callbackQuery.getId());
        });

        return callbackQueryHandlerMap;
    }

    @Override
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ApplicationException.class)
    public void registerUser(long userId, String username) {
        try {
            cclhService.registerUser(userId, username);

            botService.sendMessage(userId, ResponseMessageI18n.PLAYER_WELCOME);
        } catch (UserAlreadyExistsException e) {
            logger.error("El usuario {} ({}) esta intentando registrarse de nuevo.", userId, username);

            botService.sendMessage(userId, ResponseErrorI18n.USER_ALREADY_REGISTERED);

            throw e;
        } catch (ApplicationException e) {
            botService.sendMessage(userId, e.getMessage());

            throw e;
        }
    }

    @Override
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ApplicationException.class)
    public void deleteMyGames(long userId) {
        TelegramGame telegramGame = cclhService.getGameByCreatorId(userId);

        if (telegramGame == null) {
            logger.error("No existe juego asociado al usuario {}", userId);

            botService.sendMessage(userId, ResponseErrorI18n.PLAYER_NO_GAMES);

            return;
        }

        try {
            List<TelegramPlayer> telegramPlayerList = cclhService.deleteGame(telegramGame);

            sendDeleteMessages(telegramGame, telegramPlayerList);
        } catch (ApplicationException e) {
            botService.sendMessage(userId, e.getMessage());

            throw e;
        }
    }

    @Override
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ApplicationException.class)
    public void startCreatingGame(long roomId, String roomTitle, long creatorId) {
        botService.sendMessageAsync(roomId, ResponseMessageI18n.GAME_CREATING, new BotService.Callback() {
            @Override
            public void success(BotApiMethod<Message> method, Message groupResponse) {
                botService.sendMessageAsync(creatorId, ResponseMessageI18n.GAME_CREATING, new BotService.Callback() {
                    @Override
                    public void success(BotApiMethod<Message> method, Message privateResponse) {
                        botService.sendMessageAsync(creatorId, ResponseMessageI18n.PLAYER_JOINING, new BotService.Callback() {
                            @Override
                            public void success(BotApiMethod<Message> method, Message playerResponse) {
                                applicationService.createGame(roomId, roomTitle, creatorId, groupResponse.getMessageId(),
                                        privateResponse.getMessageId(), playerResponse.getMessageId());
                            }

                            @Override
                            public void failure(BotApiMethod<Message> method, Exception e) {
                                logger.error("Fallo al enviar mensaje", e);
                            }
                        });
                    }

                    @Override
                    public void failure(BotApiMethod<Message> method, Exception e) {
                        logger.error("Fallo al enviar mensaje", e);
                    }
                });
            }

            @Override
            public void failure(BotApiMethod<Message> method, Exception e) {
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

            botService.editMessage(creatorId, privateMessageId, ResponseMessageI18n.PLAYER_JOINED);
        } catch (GameAlreadyExistsException e) {
            logger.error("Ya existe una partida para al sala {} ({}) o creado por {}.",
                    roomId, roomTitle, creatorId);

            botService.editMessage(roomId,
                    groupMessageId,
                    ResponseErrorI18n.GAME_ALREADY_CREATED);

            botService.editMessage(creatorId,
                    privateMessageId,
                    ResponseErrorI18n.GAME_ALREADY_CREATED);

            throw e;
        } catch (PlayerAlreadyExistsException e) {
            logger.error("El jugador {} que intenta crear la partida en la sala {}({}) ya está en otra partida.",
                    creatorId, roomId, roomTitle);

            botService.editMessage(roomId,
                    groupMessageId,
                    ResponseErrorI18n.PLAYER_ALREADY_PLAYING);

            botService.editMessage(creatorId,
                    privateMessageId,
                    ResponseErrorI18n.PLAYER_ALREADY_PLAYING);

            throw e;
        } catch (ApplicationException e) {
            botService.sendMessage(creatorId, e.getMessage());

            throw e;
        }
    }

    @Override
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ApplicationException.class)
    public void gameCreatedQuery(long roomId, long userId, String callbackQueryId) {
        TelegramGame telegramGame = cclhService.getGame(roomId);

        if (telegramGame == null) {
            logger.error("No hay partida activa en la sala {}", roomId);

            botService.answerCallbackQuery(callbackQueryId, ResponseErrorI18n.GAME_DOESNT_EXISTS);

            return;
        }

        if (userId == telegramGame.getGame().getCreator().getId()) {
            sendMainMenu(telegramGame);
        } else {
            botService.answerCallbackQuery(callbackQueryId, ResponseErrorI18n.GAME_ONLY_CREATOR_CAN_CONFIGURE);
        }
    }

    @Override
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ApplicationException.class)
    public void gameConfigureQuery(long roomId, long userId, String callbackQueryId) {
        TelegramGame telegramGame = cclhService.getGame(roomId);

        if (telegramGame == null) {
            logger.error("No hay partida activa en la sala {}", roomId);

            botService.answerCallbackQuery(callbackQueryId, ResponseErrorI18n.GAME_DOESNT_EXISTS);

            return;
        }

        if (userId == telegramGame.getGame().getCreator().getId()) {
            sendConfigMenu(telegramGame);
        } else {
            botService.answerCallbackQuery(callbackQueryId, ResponseErrorI18n.GAME_ONLY_CREATOR_CAN_CONFIGURE);
        }
    }

    @Override
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ApplicationException.class)
    public void gameSelectModeQuery(long roomId, long userId, String callbackQueryId) {
        TelegramGame telegramGame = cclhService.getGame(roomId);

        if (telegramGame == null) {
            logger.error("No hay partida activa en la sala {}", roomId);

            botService.answerCallbackQuery(callbackQueryId, ResponseErrorI18n.GAME_DOESNT_EXISTS);

            return;
        }

        if (userId == telegramGame.getGame().getCreator().getId()) {
            InlineKeyboardMarkup groupInlineKeyboard = InlineKeyboardMarkup.builder()
                    .keyboardRow(Collections.singletonList(InlineKeyboardButton.builder().text("Democracia")
                            .callbackData("game_change_mode__0").build()))
                    .keyboardRow(Collections.singletonList(InlineKeyboardButton.builder().text("Clásico")
                            .callbackData("game_change_mode__1").build()))
                    .keyboardRow(Collections.singletonList(InlineKeyboardButton.builder().text("Dictadura")
                            .callbackData("game_change_mode__2").build()))
                    .keyboardRow(Collections.singletonList(InlineKeyboardButton.builder().text("⬅ Volver")
                            .callbackData("game_configure").build()))
                    .build();

            botService.editMessage(telegramGame.getGame().getRoom().getId(), telegramGame.getGroupMessageId(),
                            getGameCreatedGroupMessage(telegramGame) + "\n Selecciona modo de juego:", groupInlineKeyboard);
        } else {
            botService.answerCallbackQuery(callbackQueryId, ResponseErrorI18n.GAME_ONLY_CREATOR_CAN_CONFIGURE);
        }
    }

    @Override
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ApplicationException.class)
    public void gameSelectPunctuationModeQuery(long roomId, long userId, String callbackQueryId) {
        TelegramGame telegramGame = cclhService.getGame(roomId);

        if (telegramGame == null) {
            logger.error("No hay partida activa en la sala {}", roomId);

            botService.answerCallbackQuery(callbackQueryId, ResponseErrorI18n.GAME_DOESNT_EXISTS);

            return;
        }

        if (userId == telegramGame.getGame().getCreator().getId()) {
            InlineKeyboardMarkup groupInlineKeyboard = InlineKeyboardMarkup.builder()
                    .keyboardRow(Collections.singletonList(InlineKeyboardButton.builder()
                            .text("Rondas").callbackData("game_sel_n_rounds").build()))
                    .keyboardRow(Collections.singletonList(InlineKeyboardButton.builder()
                            .text("Puntos").callbackData("game_sel_n_points").build()))
                    .keyboardRow(Collections.singletonList(InlineKeyboardButton.builder()
                            .text("⬅ Volver").callbackData("game_configure").build()))
                    .build();

            botService.editMessage(telegramGame.getGame().getRoom().getId(), telegramGame.getGroupMessageId(),
                    getGameCreatedGroupMessage(telegramGame) + "\n Selecciona modo de puntuación:",
                    groupInlineKeyboard);
        } else {
            botService.answerCallbackQuery(callbackQueryId, ResponseErrorI18n.GAME_ONLY_CREATOR_CAN_CONFIGURE);
        }
    }

    @Override
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ApplicationException.class)
    public void gameSelectNRoundsToEndQuery(long roomId, long userId, String callbackQueryId) {
        TelegramGame telegramGame = cclhService.getGame(roomId);

        if (telegramGame == null) {
            logger.error("No hay partida activa en la sala {}", roomId);

            botService.answerCallbackQuery(callbackQueryId, ResponseErrorI18n.GAME_DOESNT_EXISTS);

            return;
        }

        if (userId == telegramGame.getGame().getCreator().getId()) {
            InlineKeyboardMarkup groupInlineKeyboard = InlineKeyboardMarkup.builder()
                    .keyboardRow(Arrays.asList(
                            InlineKeyboardButton.builder().text("1").callbackData("game_change_max_rounds__1").build(),
		                    InlineKeyboardButton.builder().text("2").callbackData("game_change_max_rounds__2").build(),
		                    InlineKeyboardButton.builder().text("3").callbackData("game_change_max_rounds__3").build()))
                    .keyboardRow(Arrays.asList(
                            InlineKeyboardButton.builder().text("4").callbackData("game_change_max_rounds__4").build(),
                            InlineKeyboardButton.builder().text("5").callbackData("game_change_max_rounds__5").build(),
                            InlineKeyboardButton.builder().text("6").callbackData("game_change_max_rounds__6").build()))
                    .keyboardRow(Arrays.asList(
                            InlineKeyboardButton.builder().text("7").callbackData("game_change_max_rounds__7").build(),
                            InlineKeyboardButton.builder().text("8").callbackData("game_change_max_rounds__8").build(),
                            InlineKeyboardButton.builder().text("9").callbackData("game_change_max_rounds__9").build()))
                    .keyboardRow(Collections.singletonList(
                            InlineKeyboardButton.builder().text("⬅ Volver").callbackData("game_configure").build()))
                    .build();

            botService.editMessage(telegramGame.getGame().getRoom().getId(), telegramGame.getGroupMessageId(),
                    getGameCreatedGroupMessage(telegramGame) + "\n Selecciona rondas de la partida:",
                    groupInlineKeyboard);
        } else {
            botService.answerCallbackQuery(callbackQueryId, ResponseErrorI18n.GAME_ONLY_CREATOR_CAN_CONFIGURE);
        }
    }

    @Override
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ApplicationException.class)
    public void gameSelectNPointsToWinQuery(long roomId, long userId, String callbackQueryId) {
        TelegramGame telegramGame = cclhService.getGame(roomId);

        if (telegramGame == null) {
            logger.error("No hay partida activa en la sala {}", roomId);

            botService.answerCallbackQuery(callbackQueryId, ResponseErrorI18n.GAME_DOESNT_EXISTS);

            return;
        }

        if (userId == telegramGame.getGame().getCreator().getId()) {
            InlineKeyboardMarkup groupInlineKeyboard = InlineKeyboardMarkup.builder()
                    .keyboardRow(Arrays.asList(
                            InlineKeyboardButton.builder().text("1").callbackData("game_change_max_points__1").build(),
                            InlineKeyboardButton.builder().text("2").callbackData("game_change_max_points__2").build(),
                            InlineKeyboardButton.builder().text("3").callbackData("game_change_max_points__3").build()))
                    .keyboardRow(Arrays.asList(
                            InlineKeyboardButton.builder().text("4").callbackData("game_change_max_points__4").build(),
                            InlineKeyboardButton.builder().text("5").callbackData("game_change_max_points__5").build(),
                            InlineKeyboardButton.builder().text("6").callbackData("game_change_max_points__6").build()))
                    .keyboardRow(Arrays.asList(
                            InlineKeyboardButton.builder().text("7").callbackData("game_change_max_points__7").build(),
                            InlineKeyboardButton.builder().text("8").callbackData("game_change_max_points__8").build(),
                            InlineKeyboardButton.builder().text("9").callbackData("game_change_max_points__9").build()))
                    .keyboardRow(Collections.singletonList(
                            InlineKeyboardButton.builder().text("⬅ Volver").callbackData("game_configure").build()))
                    .build();

            botService.editMessage(telegramGame.getGame().getRoom().getId(), telegramGame.getGroupMessageId(),
                    getGameCreatedGroupMessage(telegramGame) + "\n Selecciona nº de puntos para ganar:",
                    groupInlineKeyboard);
        } else {
            botService.answerCallbackQuery(callbackQueryId, ResponseErrorI18n.GAME_ONLY_CREATOR_CAN_CONFIGURE);
        }
    }

    @Override
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ApplicationException.class)
    public void gameSelectDictionaryQuery(long roomId, long userId, String callbackQueryId, String data) {
        TelegramGame telegramGame = cclhService.getGame(roomId);

        if (telegramGame == null) {
            logger.error("No hay partida activa en la sala {}", roomId);

            botService.answerCallbackQuery(callbackQueryId, ResponseErrorI18n.GAME_DOESNT_EXISTS);

            return;
        }

        if (userId == telegramGame.getGame().getCreator().getId()) {
            int pageNumber = Integer.parseInt(data);
            int dictionariesPerPage = cclhService.getDictionariesPerPage();
            int totalDictionaries = (int) cclhService.getDictionaryCount(userId);
            int firstResult = (pageNumber - 1) * dictionariesPerPage;
            List<Dictionary> dictionaryList = cclhService.getDictionariesPaginated(userId, firstResult, dictionariesPerPage);

            InlineKeyboardMarkup.InlineKeyboardMarkupBuilder groupInlineKeyboard = InlineKeyboardMarkup.builder();
            if (pageNumber > 1) {
                groupInlineKeyboard.keyboardRow(Collections.singletonList(InlineKeyboardButton.builder()
                        .text("⬅").callbackData("game_sel_dictionary__" + (pageNumber - 1)).build()));
            }
            for (Dictionary dictionary : dictionaryList) {
                groupInlineKeyboard.keyboardRow(Collections.singletonList(InlineKeyboardButton.builder()
                        .text(dictionary.getName()).callbackData("game_change_dictionary__" + dictionary.getId()).build()));
            }
            if (totalDictionaries > pageNumber * dictionariesPerPage) {
                groupInlineKeyboard.keyboardRow(Collections.singletonList(InlineKeyboardButton.builder()
                        .text("➡").callbackData("game_sel_dictionary__" + (pageNumber + 1)).build()));
            }
            groupInlineKeyboard.keyboardRow(Collections.singletonList(InlineKeyboardButton.builder()
                    .text("⬅ Volver").callbackData("game_configure").build()));

            botService.editMessage(telegramGame.getGame().getRoom().getId(), telegramGame.getGroupMessageId(),
                    getGameCreatedGroupMessage(telegramGame) + "\n Selecciona el mazo:", groupInlineKeyboard.build());
        } else {
            botService.answerCallbackQuery(callbackQueryId, ResponseErrorI18n.GAME_ONLY_CREATOR_CAN_CONFIGURE);
        }
    }

    @Override
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ApplicationException.class)
    public void gameSelectMaxPlayersQuery(long roomId, long userId, String callbackQueryId) {
        TelegramGame telegramGame = cclhService.getGame(roomId);

        if (telegramGame == null) {
            logger.error("No hay partida activa en la sala {}", roomId);

            botService.answerCallbackQuery(callbackQueryId, ResponseErrorI18n.GAME_DOESNT_EXISTS);

            return;
        }

        if (userId == telegramGame.getGame().getCreator().getId()) {
            InlineKeyboardMarkup groupInlineKeyboard = InlineKeyboardMarkup.builder()
                    .keyboardRow(Arrays.asList(
                            InlineKeyboardButton.builder().text("3").callbackData("game_change_max_players__3").build(),
                            InlineKeyboardButton.builder().text("4").callbackData("game_change_max_players__4").build(),
                            InlineKeyboardButton.builder().text("5").callbackData("game_change_max_players__5").build()))
                    .keyboardRow(Arrays.asList(
                            InlineKeyboardButton.builder().text("6").callbackData("game_change_max_players__6").build(),
                            InlineKeyboardButton.builder().text("7").callbackData("game_change_max_players__7").build(),
                            InlineKeyboardButton.builder().text("8").callbackData("game_change_max_players__8").build()))
                    .keyboardRow(Collections.singletonList(
		                    InlineKeyboardButton.builder().text("9").callbackData("game_change_max_players__9").build()))
                    .keyboardRow(Collections.singletonList(
                            InlineKeyboardButton.builder().text("⬅ Volver").callbackData("game_configure").build()))
                    .build();

            botService.editMessage(telegramGame.getGame().getRoom().getId(), telegramGame.getGroupMessageId(),
                    getGameCreatedGroupMessage(telegramGame) + "\n Selecciona nº máximo de jugadores:",
                    groupInlineKeyboard);
        } else {
            botService.answerCallbackQuery(callbackQueryId, ResponseErrorI18n.GAME_ONLY_CREATOR_CAN_CONFIGURE);
        }
    }

    @Override
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ApplicationException.class)
    public void gameChangeMode(long roomId, long userId, String callbackQueryId, String data) {
        TelegramGame telegramGame = cclhService.getGame(roomId);

        if (telegramGame == null) {
            logger.error("No hay partida activa en la sala {}", roomId);

            botService.answerCallbackQuery(callbackQueryId, ResponseErrorI18n.GAME_DOESNT_EXISTS);

            return;
        }

        if (userId == telegramGame.getGame().getCreator().getId()) {
            try {
                cclhService.setType(telegramGame, GameTypeEnum.getEnum(Integer.parseInt(data)));

                sendConfigMenu(telegramGame);
            } catch (GameAlreadyStartedException e) {
                logger.error("La partida de la sala {} ya estaba iniciada cuando se intentó cambiar el modo", roomId);

                botService.answerCallbackQuery(callbackQueryId, ResponseErrorI18n.GAME_ALREADY_STARTED);

                throw e;
            } catch (ApplicationException e) {
                botService.answerCallbackQuery(callbackQueryId, e.getMessage());

                throw e;
            }
        } else {
            botService.answerCallbackQuery(callbackQueryId, ResponseErrorI18n.GAME_ONLY_CREATOR_CAN_CONFIGURE);
        }
    }

    @Override
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ApplicationException.class)
    public void gameChangeDictionary(long roomId, long userId, String callbackQueryId, String data) {
        TelegramGame telegramGame = cclhService.getGame(roomId);

        if (telegramGame == null) {
            logger.error("No hay partida activa en la sala {}", roomId);

            botService.answerCallbackQuery(callbackQueryId, ResponseErrorI18n.GAME_DOESNT_EXISTS);

            return;
        }

        if (userId == telegramGame.getGame().getCreator().getId()) {
            try {
                cclhService.setDictionary(telegramGame, Integer.parseInt(data));

                sendConfigMenu(telegramGame);
            } catch (GameAlreadyStartedException e) {
                logger.error("La partida de la sala {} ya estaba iniciada cuando se intentó cambiar el modo", roomId);

                botService.answerCallbackQuery(callbackQueryId, ResponseErrorI18n.GAME_ALREADY_STARTED);

                throw e;
            } catch (DictionaryDoesntExistsException e) {
                logger.error("El diccionario {} no existe para la sala {}", data, roomId);

                botService.answerCallbackQuery(callbackQueryId, ResponseErrorI18n.UNKNOWN_ERROR);

                throw e;
            } catch (ApplicationException e) {
                botService.answerCallbackQuery(callbackQueryId, e.getMessage());

                throw e;
            }
        } else {
            botService.answerCallbackQuery(callbackQueryId, ResponseErrorI18n.GAME_ONLY_CREATOR_CAN_CONFIGURE);
        }
    }

    @Override
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ApplicationException.class)
    public void gameChangeMaxPlayers(long roomId, long userId, String callbackQueryId, String data) {
        TelegramGame telegramGame = cclhService.getGame(roomId);

        if (telegramGame == null) {
            logger.error("No hay partida activa en la sala {}", roomId);

            botService.answerCallbackQuery(callbackQueryId, ResponseErrorI18n.GAME_DOESNT_EXISTS);

            return;
        }

        if (userId == telegramGame.getGame().getCreator().getId()) {
            try {
                cclhService.setMaxNumberOfPlayers(telegramGame, Integer.parseInt(data));

                sendConfigMenu(telegramGame);
            } catch (GameAlreadyStartedException e) {
                logger.error("La partida de la sala {} ya estaba iniciada cuando se intentó cambiar el modo", roomId);

                botService.answerCallbackQuery(callbackQueryId, ResponseErrorI18n.GAME_ALREADY_STARTED);

                throw e;
            } catch (GameAlreadyFilledException e) {
                botService.answerCallbackQuery(callbackQueryId, ResponseErrorI18n.GAME_ALREADY_FILLED);

                throw e;
            } catch (ApplicationException e) {
                botService.answerCallbackQuery(callbackQueryId, e.getMessage());

                throw e;
            }
        } else {
            botService.answerCallbackQuery(callbackQueryId, ResponseErrorI18n.GAME_ONLY_CREATOR_CAN_CONFIGURE);
        }
    }

    @Override
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ApplicationException.class)
    public void gameChangeNRoundsToEnd(long roomId, long userId, String callbackQueryId, String data) {
        TelegramGame telegramGame = cclhService.getGame(roomId);

        if (telegramGame == null) {
            logger.error("No hay partida activa en la sala {}", roomId);

            botService.answerCallbackQuery(callbackQueryId, ResponseErrorI18n.GAME_DOESNT_EXISTS);

            return;
        }

        if (userId == telegramGame.getGame().getCreator().getId()) {
            try {
                cclhService.setNumberOfRoundsToEnd(telegramGame, Integer.parseInt(data));

                sendConfigMenu(telegramGame);
            } catch (GameAlreadyStartedException e) {
                logger.error("La partida de la sala {} ya estaba iniciada cuando se intentó cambiar el modo", roomId);

                botService.answerCallbackQuery(callbackQueryId, ResponseErrorI18n.GAME_ALREADY_STARTED);

                throw e;
            } catch (ApplicationException e) {
                botService.answerCallbackQuery(callbackQueryId, e.getMessage());

                throw e;
            }
        } else {
            botService.answerCallbackQuery(callbackQueryId, ResponseErrorI18n.GAME_ONLY_CREATOR_CAN_CONFIGURE);
        }
    }

    @Override
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ApplicationException.class)
    public void gameChangeNCardsToWin(long roomId, long userId, String callbackQueryId, String data) {
        TelegramGame telegramGame = cclhService.getGame(roomId);

        if (telegramGame == null) {
            logger.error("No hay partida activa en la sala {}", roomId);

            botService.answerCallbackQuery(callbackQueryId, ResponseErrorI18n.GAME_DOESNT_EXISTS);

            return;
        }

        if (userId == telegramGame.getGame().getCreator().getId()) {
            try {
                cclhService.setNumberOfCardsToWin(telegramGame, Integer.parseInt(data));

                sendConfigMenu(telegramGame);
            } catch (GameAlreadyStartedException e) {
                logger.error("La partida de la sala {} ya estaba iniciada cuando se intentó cambiar el modo", roomId);

                botService.answerCallbackQuery(callbackQueryId, ResponseErrorI18n.GAME_ALREADY_STARTED);

                throw e;
            } catch (ApplicationException e) {
                botService.answerCallbackQuery(callbackQueryId, e.getMessage());

                throw e;
            }
        } else {
            botService.answerCallbackQuery(callbackQueryId, ResponseErrorI18n.GAME_ONLY_CREATOR_CAN_CONFIGURE);
        }
    }

    @Override
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ApplicationException.class)
    public void gameDeleteGroupQuery(long roomId, long userId, String callbackQueryId) {
        TelegramGame telegramGame = cclhService.getGame(roomId);

        if (telegramGame == null) {
            logger.error("No hay partida activa en la sala {}", roomId);

            botService.answerCallbackQuery(callbackQueryId, ResponseErrorI18n.GAME_DOESNT_EXISTS);

            return;
        }

        if (userId == telegramGame.getGame().getCreator().getId()) {
            try {
                List<TelegramPlayer> telegramPlayerList = cclhService.deleteGame(telegramGame);

                sendDeleteMessages(telegramGame, telegramPlayerList);
            } catch (ApplicationException e) {
                botService.answerCallbackQuery(callbackQueryId, e.getMessage());

                throw e;
            }
        } else {
            if (telegramGame.getGame().getStatus().equals(GameStatusEnum.STARTED)) {
                try {
                    cclhService.voteForDeletion(telegramGame, userId);

                    botService.answerCallbackQuery(callbackQueryId, ResponseMessageI18n.PLAYER_VOTED_DELETION);

                    if (telegramGame.getGame().getStatus().equals(GameStatusEnum.DELETING)) {
                        List<TelegramPlayer> telegramPlayerList = cclhService.deleteGame(telegramGame);

                        sendDeleteMessages(telegramGame, telegramPlayerList);
                    } else {
                        sendMainMenu(telegramGame);
                    }
                } catch (GameNotStartedException e) {
                    botService.answerCallbackQuery(callbackQueryId, ResponseErrorI18n.GAME_DOESNT_EXISTS);

                    throw e;
                } catch (PlayerAlreadyVotedDeleteException e) {
                    botService.answerCallbackQuery(callbackQueryId, ResponseErrorI18n.PLAYER_ALREADY_VOTED_DELETION);

                    throw e;
                } catch (ApplicationException e) {
                    botService.answerCallbackQuery(callbackQueryId, e.getMessage());

                    throw e;
                }
            } else {
                botService.answerCallbackQuery(callbackQueryId, ResponseErrorI18n.GAME_ONLY_CREATOR_CAN_DELETE);
            }
        }
    }

    @Override
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ApplicationException.class)
    public void gameDeletePrivateQuery(long userId, String callbackQueryId) {
        TelegramGame telegramGame = cclhService.getGameByCreatorId(userId);

        if (telegramGame == null) {
            logger.error("No existe juego asociado al usuario {}", userId);

            botService.sendMessage(userId, ResponseErrorI18n.PLAYER_NO_GAMES);

            return;
        }

        try {
            List<TelegramPlayer> telegramPlayerList = cclhService.deleteGame(telegramGame);

            sendDeleteMessages(telegramGame, telegramPlayerList);
        } catch (ApplicationException e) {
            botService.answerCallbackQuery(callbackQueryId, e.getMessage());

            throw e;
        }
    }

    @Override
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ApplicationException.class)
    public void gameJoinQuery(long roomId, long userId, String callbackQueryId) {
        TelegramGame telegramGame = cclhService.getGame(roomId);

        if (telegramGame == null) {
            logger.error("No hay partida activa en la sala {}", roomId);

            botService.answerCallbackQuery(callbackQueryId, ResponseErrorI18n.GAME_DOESNT_EXISTS);

            return;
        }

        if (userId != telegramGame.getGame().getCreator().getId()) {
            botService.sendMessageAsync(userId, ResponseMessageI18n.PLAYER_JOINING, new BotService.Callback() {
                @Override
                public void success(BotApiMethod<Message> method, Message response) {
                    try {
                        applicationService.joinGame(
                                roomId,
                                userId,
                                callbackQueryId,
                                response.getMessageId());
                    } catch (PlayerAlreadyExistsException e) {
                        botService.answerCallbackQuery(callbackQueryId, ResponseErrorI18n.PLAYER_ALREADY_JOINED);

                        throw e;
                    }
                }

                @Override
                public void failure(BotApiMethod<Message> method, Exception e) {
                    logger.error("Fallo al enviar mensaje", e);
                }
            });
        } else {
            botService.answerCallbackQuery(callbackQueryId, ResponseErrorI18n.PLAYER_ALREADY_JOINED);
        }
    }

    @Override
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ApplicationException.class)
    public void joinGame(long roomId, long userId, String callbackQueryId, int playerMessageId) {
        TelegramGame telegramGame = cclhService.getGame(roomId);

        try {
            cclhService.joinGame(telegramGame, userId, playerMessageId);

            InlineKeyboardMarkup privateInlineKeyboard = InlineKeyboardMarkup.builder()
                            .keyboardRow(Collections.singletonList(InlineKeyboardButton.builder().text("Dejar la partida")
                                    .callbackData("game_leave").build()))
                    .build();

            botService.editMessage(userId, playerMessageId, ResponseMessageI18n.PLAYER_JOINED, privateInlineKeyboard);

            sendMainMenu(telegramGame);
        } catch (UserDoesntExistsException e) {
            botService.answerCallbackQuery(callbackQueryId, ResponseErrorI18n.GAME_USER_DOESNT_EXISTS);

            throw e;
        } catch (GameAlreadyStartedException e) {
            botService.answerCallbackQuery(callbackQueryId, ResponseErrorI18n.GAME_ALREADY_STARTED);

            throw e;
        } catch (GameAlreadyFilledException e) {
            botService.answerCallbackQuery(callbackQueryId, ResponseErrorI18n.GAME_ALREADY_FILLED);

            throw e;
        } catch (ApplicationException e) {
            botService.answerCallbackQuery(callbackQueryId, e.getMessage());

            throw e;
        }
    }

    @Override
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ApplicationException.class)
    public void gameStartQuery(long roomId, long userId, String callbackQueryId) {
        TelegramGame telegramGame = cclhService.getGame(roomId);

        if (telegramGame == null) {
            logger.error("No hay partida activa en la sala {}", roomId);

            botService.answerCallbackQuery(callbackQueryId, ResponseErrorI18n.GAME_DOESNT_EXISTS);

            return;
        }

        if (userId == telegramGame.getGame().getCreator().getId()) {
            try {
                cclhService.startGame(telegramGame);

                if (telegramGame.getGame().getStatus().equals(GameStatusEnum.STARTED)) {
                    startRound(telegramGame);
                } else {
                    logger.error("La partida no se ha iniciado correctamente");

                    botService.answerCallbackQuery(callbackQueryId, ResponseErrorI18n.UNKNOWN_ERROR);
                }
            } catch (GameAlreadyStartedException e) {
                botService.answerCallbackQuery(callbackQueryId, ResponseErrorI18n.GAME_ALREADY_STARTED);

                throw e;
            } catch (GameNotFilledException e) {
                botService.answerCallbackQuery(callbackQueryId, ResponseErrorI18n.GAME_NOT_FILLED);

                throw e;
            } catch (GameNotStartedException e) {
                botService.answerCallbackQuery(callbackQueryId, ResponseErrorI18n.GAME_NOT_STARTED);

                throw e;
            } catch (ApplicationException e) {
                botService.answerCallbackQuery(callbackQueryId, e.getMessage());

                throw e;
            }
        } else {
            logger.error("Intentando iniciar una partida en {} que no le corresponde a {}", roomId, userId);

            botService.answerCallbackQuery(callbackQueryId, ResponseErrorI18n.GAME_ONLY_CREATOR_CAN_START);
        }
    }

    @Override
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ApplicationException.class)
    public void playerPlayCardQuery(long userId, String callbackQueryId, String data) {
        TelegramGame telegramGame = cclhService.getByPlayerUser(userId);

        if (telegramGame == null) {
            logger.error("No existe juego asociado al usuario {}", userId);

            botService.answerCallbackQuery(callbackQueryId, ResponseErrorI18n.PLAYER_NO_GAMES);

            return;
        }

        try {
            cclhService.playCard(telegramGame, userId, Integer.parseInt(data));

            if (telegramGame.getGame().getTable().getStatus().equals(TableStatusEnum.PLAYING)) {
                TelegramPlayer telegramPlayer = cclhService.getPlayer(userId);

                PlayedCard playedCard = getPlayedCardByPlayer(telegramGame, telegramPlayer);

                if (playedCard != null) {
                    botService.editMessage(telegramPlayer.getPlayer().getUser().getId(),
                            telegramPlayer.getMessageId(),
                            StringUtils.formatMessage(ResponseMessageI18n.PLAYER_SELECTED_CARD,
                                    telegramGame.getGame().getTable().getCurrentBlackCard().getText(),
                                    playedCard.getCard().getText()));
                } else {
                    logger.error("No se ha encontrado el jugador o la carta jugada");

                    botService.answerCallbackQuery(callbackQueryId, ResponseErrorI18n.PLAYER_DOES_NOT_EXISTS);
                }
            } else if (telegramGame.getGame().getTable().getStatus().equals(TableStatusEnum.VOTING)) {
                if (telegramGame.getGame().getType().equals(GameTypeEnum.DEMOCRACY)) {
                    botService.editMessage(
                                    telegramGame.getGame().getRoom().getId(),
                                    telegramGame.getBlackCardMessageId(),
                                    getGameVoteCardMessage(telegramGame));

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
            botService.answerCallbackQuery(callbackQueryId, ResponseErrorI18n.PLAYER_ALREADY_PLAYED_CARD);

            throw e;
        } catch (ApplicationException e) {
            botService.answerCallbackQuery(callbackQueryId, e.getMessage());

            throw e;
        }
    }

    @Override
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ApplicationException.class)
    public void playerVoteCardQuery(long userId, String callbackQueryId, String data) {
        TelegramGame telegramGame = cclhService.getByPlayerUser(userId);

        if (telegramGame == null) {
            logger.error("No existe juego asociado al usuario {}", userId);

            botService.sendMessage(userId, ResponseErrorI18n.PLAYER_NO_GAMES);

            return;
        }

        try {
            cclhService.voteCard(telegramGame, userId, Integer.parseInt(data));

            if (telegramGame.getGame().getTable().getStatus().equals(TableStatusEnum.VOTING)) {
                sendMessageToVoter(telegramGame, userId, callbackQueryId);
            } else if (telegramGame.getGame().getTable().getStatus().equals(TableStatusEnum.ENDING)) {
                sendMessageToVoter(telegramGame, userId, callbackQueryId);

                PlayedCard mostVotedCard = cclhService.getMostVotedCard(telegramGame);

                botService.editMessage(
                                telegramGame.getGame().getRoom().getId(),
                                telegramGame.getBlackCardMessageId(),
                                getGameEndRoundMessage(telegramGame, mostVotedCard));

                cclhService.endRound(telegramGame);

                if (telegramGame.getGame().getStatus().equals(GameStatusEnum.STARTED)) {
                    startRound(telegramGame);
                } else if (telegramGame.getGame().getStatus().equals(GameStatusEnum.ENDED)) {
                    Player winner = getWinnerPlayer(telegramGame);

                    if (winner != null) {
                        botService.sendMessage(telegramGame.getGame().getRoom().getId(), getGameEndGameMessage(winner));

                        List<TelegramPlayer> telegramPlayerList = cclhService.deleteGame(telegramGame);

                        sendEndMessages(telegramGame, telegramPlayerList);
                    } else {
                        logger.error("Juego en estado incorrecto: {}", telegramGame.getGame().getId());

                        botService.answerCallbackQuery(callbackQueryId, ResponseErrorI18n.UNKNOWN_ERROR);
                    }
                }
            } else {
                logger.error("Juego en estado incorrecto: {}", telegramGame.getGame().getId());

                botService.answerCallbackQuery(callbackQueryId, ResponseErrorI18n.UNKNOWN_ERROR);
            }
        } catch (PlayerAlreadyVotedCardException e) {
            botService.answerCallbackQuery(callbackQueryId, ResponseErrorI18n.PLAYER_ALREADY_VOTED_CARD);

            throw e;
        } catch (ApplicationException e) {
            botService.answerCallbackQuery(callbackQueryId, e.getMessage());

            throw e;
        }
    }

    private void sendMainMenu(TelegramGame telegramGame) {
        InlineKeyboardMarkup.InlineKeyboardMarkupBuilder groupInlineKeyboard = InlineKeyboardMarkup.builder();

        if (telegramGame.getGame().getStatus().equals(GameStatusEnum.CREATED)) {
            if (telegramGame.getGame().getPlayers().size() < telegramGame.getGame().getMaxNumberOfPlayers()) {
                groupInlineKeyboard.keyboardRow(Collections.singletonList(InlineKeyboardButton.builder()
                        .text("Unirse a la partida").callbackData("game_join").build()));
            }

            groupInlineKeyboard.keyboardRow(Collections.singletonList(InlineKeyboardButton.builder()
                    .text("Configurar la partida").callbackData("game_configure").build()));

            if (telegramGame.getGame().getPlayers().size() >= cclhService.getMinNumberOfPlayers()) {
                groupInlineKeyboard.keyboardRow(Collections.singletonList(InlineKeyboardButton.builder()
                        .text("Iniciar la partida").callbackData("game_start").build()));
            }
        }

        groupInlineKeyboard.keyboardRow(Collections.singletonList(InlineKeyboardButton.builder()
                .text("Borrar partida").callbackData("game_delete_group").build()));

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

        botService.editMessage(telegramGame.getGame().getRoom().getId(),
                telegramGame.getGroupMessageId(), msg, groupInlineKeyboard.build());

        InlineKeyboardMarkup privateInlineKeyboard = InlineKeyboardMarkup.builder()
                        .keyboardRow(Collections.singletonList(InlineKeyboardButton.builder()
                                .text("Borrar juego").callbackData("game_delete_private").build()))
                .build();

        botService.editMessage(telegramGame.getGame().getCreator().getId(),
                telegramGame.getPrivateMessageId(), ResponseMessageI18n.PLAYER_CREATED_GAME, privateInlineKeyboard);
    }

    private void sendConfigMenu(TelegramGame telegramGame) {
        InlineKeyboardMarkup groupInlineKeyboard = InlineKeyboardMarkup.builder()
                .keyboardRow(Collections.singletonList(InlineKeyboardButton.builder()
                        .text("Cambiar modo de juego").callbackData("game_sel_mode").build()))
                .keyboardRow(Collections.singletonList(InlineKeyboardButton.builder()
                        .text("Cambiar modo de puntuacion").callbackData("game_sel_point_type").build()))
                .keyboardRow(Collections.singletonList(InlineKeyboardButton.builder()
                        .text("Cambiar mazo de cartas").callbackData("game_sel_dictionary__1").build()))
                .keyboardRow(Collections.singletonList(InlineKeyboardButton.builder()
                        .text("Cambiar nº máximo de jugadores").callbackData("game_sel_max_players").build()))
                .keyboardRow(Collections.singletonList(InlineKeyboardButton.builder()
                        .text("⬅ Volver").callbackData("game_created").build()))
                        .build();

        botService.editMessage(telegramGame.getGame().getRoom().getId(), telegramGame.getGroupMessageId(),
                        getGameCreatedGroupMessage(telegramGame), groupInlineKeyboard);
    }

    private void sendDeleteMessages(TelegramGame telegramGame, List<TelegramPlayer> telegramPlayerList) {
        // Delete game messages
        for (TelegramPlayer telegramPlayer : telegramPlayerList) {
            botService.deleteMessage(telegramPlayer.getPlayer().getUser().getId(),
                    telegramPlayer.getMessageId());
        }
        if (telegramGame.getGame().getStatus().equals(GameStatusEnum.STARTED)) {
            botService.deleteMessage(telegramGame.getGame().getRoom().getId(),
                    telegramGame.getBlackCardMessageId());
        }

        // Edit game messages
        botService.editMessage(telegramGame.getGame().getRoom().getId(),
                telegramGame.getGroupMessageId(),
                ResponseMessageI18n.GAME_DELETED);
        botService.editMessage(telegramGame.getGame().getCreator().getId(),
                telegramGame.getPrivateMessageId(),
                ResponseMessageI18n.GAME_DELETED);
    }

    private void sendEndMessages(TelegramGame telegramGame, List<TelegramPlayer> telegramPlayerList) {
        // Delete game messages
        for (TelegramPlayer telegramPlayer : telegramPlayerList) {
            botService.editMessage(telegramPlayer.getPlayer().getUser().getId(), telegramPlayer.getMessageId(), "");
        }

        // Edit game messages
        botService.deleteMessage(telegramGame.getGame().getRoom().getId(), telegramGame.getGroupMessageId());
        botService.deleteMessage(telegramGame.getGame().getCreator().getId(), telegramGame.getPrivateMessageId());
    }

    private void startRound(TelegramGame telegramGame) {
        if (telegramGame.getGame().getTable().getStatus().equals(TableStatusEnum.STARTING)) {
            cclhService.startRound(telegramGame);

            if (telegramGame.getGame().getTable().getStatus().equals(TableStatusEnum.PLAYING)) {
                String msg = StringUtils.formatMessage(ResponseMessageI18n.GAME_SELECT_CARD,
                        telegramGame.getGame().getTable().getCurrentBlackCard().getText());

                botService.sendMessageAsync(telegramGame.getGame().getRoom().getId(), msg, new BotService.Callback() {
                    @Override
                    public void success(BotApiMethod<Message> method, Message response) {
                        cclhService.setBlackCardMessage(telegramGame, response.getMessageId());
                    }

                    @Override
                    public void failure(BotApiMethod<Message> method, Exception e) {
                        logger.error("Fallo al enviar mensaje", e);
                    }
                });

                List<TelegramPlayer> telegramPlayers = new ArrayList<>(cclhService.getPlayers(telegramGame));

                if (!telegramGame.getGame().getType().equals(GameTypeEnum.DEMOCRACY)) {
                    telegramPlayers.removeIf(telegramPlayer -> telegramPlayer.getPlayer().getId()
                            .equals(telegramGame.getGame().getTable().getCurrentPresident().getId()));
                }
                for (TelegramPlayer telegramPlayer : telegramPlayers) {
                    InlineKeyboardMarkup.InlineKeyboardMarkupBuilder playerInlineKeyboard = InlineKeyboardMarkup.builder();
                    for (PlayerHandCard card : telegramPlayer.getPlayer().getHand()) {
                        playerInlineKeyboard.keyboardRow(Collections.singletonList(InlineKeyboardButton.builder()
                                .text(card.getCard().getText())
                                .callbackData("play_card__" + card.getCard().getId()).build()));
                    }

                    botService.editMessage(telegramPlayer.getPlayer().getUser().getId(),
                            telegramPlayer.getMessageId(),
                            StringUtils.formatMessage(ResponseMessageI18n.PLAYER_SELECT_CARD,
                                    telegramGame.getGame().getTable().getCurrentBlackCard().getText()),
                            playerInlineKeyboard.build());
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

            InlineKeyboardMarkup.InlineKeyboardMarkupBuilder cardInlineKeyboard = InlineKeyboardMarkup.builder();
            for (PlayedCard otherPlayerPlayedCard : playedCards) {
                if (!otherPlayerPlayedCard.getPlayer().getId().equals(tgPlayer.getPlayer().getId())) {
                    cardInlineKeyboard.keyboardRow(Collections.singletonList(InlineKeyboardButton.builder()
                            .text(otherPlayerPlayedCard.getCard().getText())
                            .callbackData("vote_card__" + otherPlayerPlayedCard.getCard().getId()).build()));
                }
            }

            botService.editMessage(tgPlayer.getPlayer().getUser().getId(),
                    tgPlayer.getMessageId(), getPlayerVoteCardMessage(telegramGame, playedCard),
                    cardInlineKeyboard.build());
        } else {
            logger.error("No se ha encontrado la carta del jugador");
        }
    }

    private void sendMessageToVoter(TelegramGame telegramGame, long userId, String callbackQueryId) {
        TelegramPlayer telegramPlayer = cclhService.getPlayer(userId);

        PlayedCard playedCard = getPlayedCardByPlayer(telegramGame, telegramPlayer);
        VotedCard votedCard = getVotedCardByPlayer(telegramGame, telegramPlayer);

        if (playedCard != null && votedCard != null) {
            botService.editMessage(telegramPlayer.getPlayer().getUser().getId(),
                    telegramPlayer.getMessageId(), getPlayerVotedCardMessage(telegramGame, playedCard, votedCard));
        } else {
            logger.error("No se ha encontrado el jugador o la carta jugada");

            botService.answerCallbackQuery(callbackQueryId, ResponseErrorI18n.PLAYER_DOES_NOT_EXISTS);
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
