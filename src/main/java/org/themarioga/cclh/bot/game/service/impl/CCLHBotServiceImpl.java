package org.themarioga.cclh.bot.game.service.impl;

import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.themarioga.bot.service.intf.BotService;
import org.themarioga.bot.util.BotUtils;
import org.themarioga.bot.util.StringUtils;
import org.themarioga.cclh.bot.game.model.TelegramGame;
import org.themarioga.cclh.bot.game.model.TelegramPlayer;
import org.themarioga.cclh.bot.game.service.intf.CCLHBotService;
import org.themarioga.cclh.bot.game.constants.CCLHBotResponseErrorI18n;
import org.themarioga.cclh.bot.game.constants.CCLHBotResponseMessageI18n;
import org.themarioga.cclh.bot.game.service.intf.TelegramGameService;
import org.themarioga.cclh.bot.game.service.intf.TelegramPlayerService;
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
import org.themarioga.cclh.commons.services.intf.ConfigurationService;
import org.themarioga.cclh.commons.services.intf.DictionaryService;
import org.themarioga.cclh.commons.services.intf.RoomService;
import org.themarioga.cclh.commons.services.intf.UserService;

import java.text.MessageFormat;
import java.util.*;

@Service
public class CCLHBotServiceImpl implements CCLHBotService {

    private final Logger logger = LoggerFactory.getLogger(CCLHBotServiceImpl.class);

    private BotService botService;
    private CCLHBotService cclhBotService;
    private UserService userService;
    private RoomService roomService;
    private DictionaryService dictionaryService;
    private ConfigurationService configurationService;
    private TelegramGameService telegramGameService;
    private TelegramPlayerService telegramPlayerService;

    private Boolean canSendGlobalMessages = Boolean.TRUE;

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
    public void registerUser(long userId, String username) {
        try {
            userService.createOrReactivate(userId, username);

            botService.sendMessage(userId, CCLHBotResponseMessageI18n.PLAYER_WELCOME);
        } catch (UserAlreadyExistsException e) {
            logger.error("El usuario {} ({}) esta intentando registrarse de nuevo.", userId, username);

            botService.sendMessage(userId, CCLHBotResponseErrorI18n.USER_ALREADY_REGISTERED);

            throw e;
        } catch (ApplicationException e) {
            botService.sendMessage(userId, e.getMessage());

            throw e;
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
    public void deleteMyGames(long userId) {
        TelegramGame telegramGame = telegramGameService.getGameByCreatorId(userId);

        if (telegramGame == null) {
            logger.error("No existe juego asociado al usuario {}", userId);

            botService.sendMessage(userId, CCLHBotResponseErrorI18n.PLAYER_NO_GAMES);

            return;
        }

        try {
            List<TelegramPlayer> telegramPlayerList = telegramPlayerService.deletePlayers(telegramGame);

            telegramGameService.deleteGame(telegramGame);

            sendDeleteMessages(telegramGame, telegramPlayerList);
        } catch (ApplicationException e) {
            botService.sendMessage(userId, e.getMessage());

            throw e;
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
    public void deleteGameByCreatorUsername(String username) {
        TelegramGame telegramGame = telegramGameService.getGameByCreatorUsername(username);

        if (telegramGame == null) {
            logger.error("No existe juego asociado al usuario {}", username);

            return;
        }

        try {
            List<TelegramPlayer> telegramPlayerList = telegramPlayerService.deletePlayers(telegramGame);

            telegramGameService.deleteGame(telegramGame);

            sendDeleteMessages(telegramGame, telegramPlayerList);

            botService.sendMessage(cclhBotService.getBotCreatorId(), MessageFormat.format(CCLHBotResponseMessageI18n.GAME_DELETION_USER, username));
        } catch (ApplicationException e) {
            botService.sendMessage(telegramGame.getGame().getCreator().getId(), e.getMessage());

            throw e;
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
    public void deleteAllGames() {
        List<TelegramGame> telegramGameList = telegramGameService.getGameList();

        for (TelegramGame telegramGame : telegramGameList) {
            try {
                List<TelegramPlayer> telegramPlayerList = telegramPlayerService.deletePlayers(telegramGame);

                telegramGameService.deleteGame(telegramGame);

                sendDeleteMessages(telegramGame, telegramPlayerList);

                botService.sendMessage(telegramGame.getGame().getCreator().getId(),
                        CCLHBotResponseMessageI18n.GAME_DELETION_FORCED);

                botService.sendMessage(cclhBotService.getBotCreatorId(), CCLHBotResponseMessageI18n.GAME_DELETION_ALL);
            } catch (ApplicationException e) {
                botService.sendMessage(telegramGame.getGame().getCreator().getId(), e.getMessage());

                throw e;
            }
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
    public void startCreatingGame(long roomId, String roomTitle, long creatorId) {
        botService.sendMessageAsync(roomId, CCLHBotResponseMessageI18n.GAME_CREATING, new BotService.Callback() {
            @Override
            public void success(BotApiMethod<Message> method, Message groupResponse) {
                botService.sendMessageAsync(creatorId, CCLHBotResponseMessageI18n.GAME_CREATING, new BotService.Callback() {
                    @Override
                    public void success(BotApiMethod<Message> method, Message privateResponse) {
                        botService.sendMessageAsync(creatorId, CCLHBotResponseMessageI18n.PLAYER_JOINING, new BotService.Callback() {
                            @Override
                            public void success(BotApiMethod<Message> method, Message playerResponse) {
                                try {
                                    cclhBotService.createGame(roomId, roomTitle, creatorId, BotUtils.getUsername(playerResponse.getChat()),
                                            privateResponse.getMessageId(), groupResponse.getMessageId(), playerResponse.getMessageId());
                                } catch (Exception e) {
                                    logger.error(e.getMessage(), e);
                                }
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
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
    public void createGame(long roomId, String roomTitle, long creatorId, String creatorName, int privateMessageId, int groupMessageId, int playerMessageId) {
        try {
            TelegramGame telegramGame = telegramGameService.createGame(roomId, roomTitle, creatorId, groupMessageId, privateMessageId);

            TelegramPlayer telegramPlayer = telegramPlayerService.createPlayer(telegramGame, creatorId, creatorName, playerMessageId);

            telegramGameService.addPlayer(telegramGame, telegramPlayer);

            sendMainMenu(telegramGame);

            sendCreatorPrivateMenu(telegramGame);

            botService.editMessage(creatorId, playerMessageId, CCLHBotResponseMessageI18n.PLAYER_JOINED);
        } catch (GameAlreadyExistsException e) {
            logger.error("Ya existe una partida para al sala {} ({}) o creado por {}.",
                    roomId, roomTitle, creatorId);

            botService.editMessage(roomId,
                    groupMessageId,
                    CCLHBotResponseErrorI18n.GAME_ALREADY_CREATED);

            botService.editMessage(creatorId,
                    privateMessageId,
                    CCLHBotResponseErrorI18n.GAME_ALREADY_CREATED);

            throw e;
        } catch (PlayerAlreadyExistsException e) {
            logger.error("El jugador {} que intenta crear la partida en la sala {}({}) ya está en otra partida.",
                    creatorId, roomId, roomTitle);

            botService.editMessage(roomId,
                    groupMessageId,
                    CCLHBotResponseErrorI18n.PLAYER_ALREADY_PLAYING);

            botService.editMessage(creatorId,
                    privateMessageId,
                    CCLHBotResponseErrorI18n.PLAYER_ALREADY_PLAYING);

            throw e;
        } catch (ApplicationException e) {
            botService.sendMessage(creatorId, e.getMessage());

            throw e;
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
    public void gameCreatedQuery(long roomId, long userId, String callbackQueryId) {
        TelegramGame telegramGame = telegramGameService.getGame(roomId);

        if (telegramGame == null) {
            logger.error("No hay partida activa en la sala {}", roomId);

            botService.answerCallbackQuery(callbackQueryId, CCLHBotResponseErrorI18n.GAME_DOESNT_EXISTS);

            return;
        }

        if (userId == telegramGame.getGame().getCreator().getId()) {
            sendMainMenu(telegramGame);
        } else {
            botService.answerCallbackQuery(callbackQueryId, CCLHBotResponseErrorI18n.GAME_ONLY_CREATOR_CAN_CONFIGURE);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
    public void gameConfigureQuery(long roomId, long userId, String callbackQueryId) {
        TelegramGame telegramGame = telegramGameService.getGame(roomId);

        if (telegramGame == null) {
            logger.error("No hay partida activa en la sala {}", roomId);

            botService.answerCallbackQuery(callbackQueryId, CCLHBotResponseErrorI18n.GAME_DOESNT_EXISTS);

            return;
        }

        if (userId == telegramGame.getGame().getCreator().getId()) {
            sendConfigMenu(telegramGame);
        } else {
            botService.answerCallbackQuery(callbackQueryId, CCLHBotResponseErrorI18n.GAME_ONLY_CREATOR_CAN_CONFIGURE);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
    public void gameSelectModeQuery(long roomId, long userId, String callbackQueryId) {
        TelegramGame telegramGame = telegramGameService.getGame(roomId);

        if (telegramGame == null) {
            logger.error("No hay partida activa en la sala {}", roomId);

            botService.answerCallbackQuery(callbackQueryId, CCLHBotResponseErrorI18n.GAME_DOESNT_EXISTS);

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
            botService.answerCallbackQuery(callbackQueryId, CCLHBotResponseErrorI18n.GAME_ONLY_CREATOR_CAN_CONFIGURE);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
    public void gameSelectPunctuationModeQuery(long roomId, long userId, String callbackQueryId) {
        TelegramGame telegramGame = telegramGameService.getGame(roomId);

        if (telegramGame == null) {
            logger.error("No hay partida activa en la sala {}", roomId);

            botService.answerCallbackQuery(callbackQueryId, CCLHBotResponseErrorI18n.GAME_DOESNT_EXISTS);

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
            botService.answerCallbackQuery(callbackQueryId, CCLHBotResponseErrorI18n.GAME_ONLY_CREATOR_CAN_CONFIGURE);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
    public void gameSelectNRoundsToEndQuery(long roomId, long userId, String callbackQueryId) {
        TelegramGame telegramGame = telegramGameService.getGame(roomId);

        if (telegramGame == null) {
            logger.error("No hay partida activa en la sala {}", roomId);

            botService.answerCallbackQuery(callbackQueryId, CCLHBotResponseErrorI18n.GAME_DOESNT_EXISTS);

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
            botService.answerCallbackQuery(callbackQueryId, CCLHBotResponseErrorI18n.GAME_ONLY_CREATOR_CAN_CONFIGURE);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
    public void gameSelectNPointsToWinQuery(long roomId, long userId, String callbackQueryId) {
        TelegramGame telegramGame = telegramGameService.getGame(roomId);

        if (telegramGame == null) {
            logger.error("No hay partida activa en la sala {}", roomId);

            botService.answerCallbackQuery(callbackQueryId, CCLHBotResponseErrorI18n.GAME_DOESNT_EXISTS);

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
            botService.answerCallbackQuery(callbackQueryId, CCLHBotResponseErrorI18n.GAME_ONLY_CREATOR_CAN_CONFIGURE);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
    public void gameSelectDictionaryQuery(long roomId, long userId, String callbackQueryId, String data) {
        TelegramGame telegramGame = telegramGameService.getGame(roomId);

        if (telegramGame == null) {
            logger.error("No hay partida activa en la sala {}", roomId);

            botService.answerCallbackQuery(callbackQueryId, CCLHBotResponseErrorI18n.GAME_DOESNT_EXISTS);

            return;
        }

        if (userId == telegramGame.getGame().getCreator().getId()) {
            int pageNumber = Integer.parseInt(data);
            int dictionariesPerPage = getGameDictionariesPerPage();
            long totalDictionaries = dictionaryService.getDictionaryCount(userService.getById(userId));
            int firstResult = (pageNumber - 1) * dictionariesPerPage;
            List<Dictionary> dictionaryList = dictionaryService.getDictionariesPaginated(userService.getById(userId), firstResult, dictionariesPerPage);

            InlineKeyboardMarkup.InlineKeyboardMarkupBuilder groupInlineKeyboard = InlineKeyboardMarkup.builder();
            if (pageNumber > 1) {
                groupInlineKeyboard.keyboardRow(Collections.singletonList(InlineKeyboardButton.builder()
                        .text("⬅").callbackData("game_sel_dictionary__" + (pageNumber - 1)).build()));
            }
            for (Dictionary dictionary : dictionaryList) {
                groupInlineKeyboard.keyboardRow(Collections.singletonList(InlineKeyboardButton.builder()
                        .text(dictionary.getName()).callbackData("game_change_dictionary__" + dictionary.getId()).build()));
            }
            if (totalDictionaries > (long) pageNumber * dictionariesPerPage) {
                groupInlineKeyboard.keyboardRow(Collections.singletonList(InlineKeyboardButton.builder()
                        .text("➡").callbackData("game_sel_dictionary__" + (pageNumber + 1)).build()));
            }
            groupInlineKeyboard.keyboardRow(Collections.singletonList(InlineKeyboardButton.builder()
                    .text("⬅ Volver").callbackData("game_configure").build()));

            botService.editMessage(telegramGame.getGame().getRoom().getId(), telegramGame.getGroupMessageId(),
                    getGameCreatedGroupMessage(telegramGame) + "\n Selecciona el mazo:", groupInlineKeyboard.build());
        } else {
            botService.answerCallbackQuery(callbackQueryId, CCLHBotResponseErrorI18n.GAME_ONLY_CREATOR_CAN_CONFIGURE);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
    public void gameSelectMaxPlayersQuery(long roomId, long userId, String callbackQueryId) {
        TelegramGame telegramGame = telegramGameService.getGame(roomId);

        if (telegramGame == null) {
            logger.error("No hay partida activa en la sala {}", roomId);

            botService.answerCallbackQuery(callbackQueryId, CCLHBotResponseErrorI18n.GAME_DOESNT_EXISTS);

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
            botService.answerCallbackQuery(callbackQueryId, CCLHBotResponseErrorI18n.GAME_ONLY_CREATOR_CAN_CONFIGURE);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
    public void gameChangeMode(long roomId, long userId, String callbackQueryId, String data) {
        TelegramGame telegramGame = telegramGameService.getGame(roomId);

        if (telegramGame == null) {
            logger.error("No hay partida activa en la sala {}", roomId);

            botService.answerCallbackQuery(callbackQueryId, CCLHBotResponseErrorI18n.GAME_DOESNT_EXISTS);

            return;
        }

        if (userId == telegramGame.getGame().getCreator().getId()) {
            try {
                telegramGameService.setType(telegramGame, GameTypeEnum.getEnum(Integer.parseInt(data)));

                sendConfigMenu(telegramGame);
            } catch (GameAlreadyStartedException e) {
                logger.error("La partida de la sala {} ya estaba iniciada cuando se intentó cambiar el modo", roomId);

                botService.answerCallbackQuery(callbackQueryId, CCLHBotResponseErrorI18n.GAME_ALREADY_STARTED);

                throw e;
            } catch (ApplicationException e) {
                botService.answerCallbackQuery(callbackQueryId, e.getMessage());

                throw e;
            }
        } else {
            botService.answerCallbackQuery(callbackQueryId, CCLHBotResponseErrorI18n.GAME_ONLY_CREATOR_CAN_CONFIGURE);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
    public void gameChangeDictionary(long roomId, long userId, String callbackQueryId, String data) {
        TelegramGame telegramGame = telegramGameService.getGame(roomId);

        if (telegramGame == null) {
            logger.error("No hay partida activa en la sala {}", roomId);

            botService.answerCallbackQuery(callbackQueryId, CCLHBotResponseErrorI18n.GAME_DOESNT_EXISTS);

            return;
        }

        if (userId == telegramGame.getGame().getCreator().getId()) {
            try {
                telegramGameService.setDictionary(telegramGame, Integer.parseInt(data));

                sendConfigMenu(telegramGame);
            } catch (GameAlreadyStartedException e) {
                logger.error("La partida de la sala {} ya estaba iniciada cuando se intentó cambiar el modo", roomId);

                botService.answerCallbackQuery(callbackQueryId, CCLHBotResponseErrorI18n.GAME_ALREADY_STARTED);

                throw e;
            } catch (DictionaryDoesntExistsException e) {
                logger.error("El diccionario {} no existe para la sala {}", data, roomId);

                botService.answerCallbackQuery(callbackQueryId, CCLHBotResponseErrorI18n.UNKNOWN_ERROR);

                throw e;
            } catch (ApplicationException e) {
                botService.answerCallbackQuery(callbackQueryId, e.getMessage());

                throw e;
            }
        } else {
            botService.answerCallbackQuery(callbackQueryId, CCLHBotResponseErrorI18n.GAME_ONLY_CREATOR_CAN_CONFIGURE);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
    public void gameChangeMaxPlayers(long roomId, long userId, String callbackQueryId, String data) {
        TelegramGame telegramGame = telegramGameService.getGame(roomId);

        if (telegramGame == null) {
            logger.error("No hay partida activa en la sala {}", roomId);

            botService.answerCallbackQuery(callbackQueryId, CCLHBotResponseErrorI18n.GAME_DOESNT_EXISTS);

            return;
        }

        if (userId == telegramGame.getGame().getCreator().getId()) {
            try {
                telegramGameService.setMaxNumberOfPlayers(telegramGame, Integer.parseInt(data));

                sendConfigMenu(telegramGame);
            } catch (GameAlreadyStartedException e) {
                logger.error("La partida de la sala {} ya estaba iniciada cuando se intentó cambiar el modo", roomId);

                botService.answerCallbackQuery(callbackQueryId, CCLHBotResponseErrorI18n.GAME_ALREADY_STARTED);

                throw e;
            } catch (GameAlreadyFilledException e) {
                botService.answerCallbackQuery(callbackQueryId, CCLHBotResponseErrorI18n.GAME_ALREADY_FILLED);

                throw e;
            } catch (ApplicationException e) {
                botService.answerCallbackQuery(callbackQueryId, e.getMessage());

                throw e;
            }
        } else {
            botService.answerCallbackQuery(callbackQueryId, CCLHBotResponseErrorI18n.GAME_ONLY_CREATOR_CAN_CONFIGURE);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
    public void gameChangeNRoundsToEnd(long roomId, long userId, String callbackQueryId, String data) {
        TelegramGame telegramGame = telegramGameService.getGame(roomId);

        if (telegramGame == null) {
            logger.error("No hay partida activa en la sala {}", roomId);

            botService.answerCallbackQuery(callbackQueryId, CCLHBotResponseErrorI18n.GAME_DOESNT_EXISTS);

            return;
        }

        if (userId == telegramGame.getGame().getCreator().getId()) {
            try {
                telegramGameService.setNumberOfRoundsToEnd(telegramGame, Integer.parseInt(data));

                sendConfigMenu(telegramGame);
            } catch (GameAlreadyStartedException e) {
                logger.error("La partida de la sala {} ya estaba iniciada cuando se intentó cambiar el modo", roomId);

                botService.answerCallbackQuery(callbackQueryId, CCLHBotResponseErrorI18n.GAME_ALREADY_STARTED);

                throw e;
            } catch (ApplicationException e) {
                botService.answerCallbackQuery(callbackQueryId, e.getMessage());

                throw e;
            }
        } else {
            botService.answerCallbackQuery(callbackQueryId, CCLHBotResponseErrorI18n.GAME_ONLY_CREATOR_CAN_CONFIGURE);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
    public void gameChangeNCardsToWin(long roomId, long userId, String callbackQueryId, String data) {
        TelegramGame telegramGame = telegramGameService.getGame(roomId);

        if (telegramGame == null) {
            logger.error("No hay partida activa en la sala {}", roomId);

            botService.answerCallbackQuery(callbackQueryId, CCLHBotResponseErrorI18n.GAME_DOESNT_EXISTS);

            return;
        }

        if (userId == telegramGame.getGame().getCreator().getId()) {
            try {
                telegramGameService.setNumberOfCardsToWin(telegramGame, Integer.parseInt(data));

                sendConfigMenu(telegramGame);
            } catch (GameAlreadyStartedException e) {
                logger.error("La partida de la sala {} ya estaba iniciada cuando se intentó cambiar el modo", roomId);

                botService.answerCallbackQuery(callbackQueryId, CCLHBotResponseErrorI18n.GAME_ALREADY_STARTED);

                throw e;
            } catch (ApplicationException e) {
                botService.answerCallbackQuery(callbackQueryId, e.getMessage());

                throw e;
            }
        } else {
            botService.answerCallbackQuery(callbackQueryId, CCLHBotResponseErrorI18n.GAME_ONLY_CREATOR_CAN_CONFIGURE);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
    public void gameDeleteGroupQuery(long roomId, long userId, String callbackQueryId) {
        TelegramGame telegramGame = telegramGameService.getGame(roomId);

        if (telegramGame == null) {
            logger.error("No hay partida activa en la sala {}", roomId);

            botService.answerCallbackQuery(callbackQueryId, CCLHBotResponseErrorI18n.GAME_DOESNT_EXISTS);

            return;
        }

        if (userId == telegramGame.getGame().getCreator().getId()) {
            try {
                List<TelegramPlayer> telegramPlayerList = telegramPlayerService.deletePlayers(telegramGame);

                telegramGameService.deleteGame(telegramGame);

                sendDeleteMessages(telegramGame, telegramPlayerList);
            } catch (ApplicationException e) {
                botService.answerCallbackQuery(callbackQueryId, e.getMessage());

                throw e;
            }
        } else {
            if (telegramGame.getGame().getStatus().equals(GameStatusEnum.STARTED)) {
                try {
                    telegramGameService.voteForDeletion(telegramGame, userId);

                    botService.answerCallbackQuery(callbackQueryId, CCLHBotResponseMessageI18n.PLAYER_VOTED_DELETION);

                    if (telegramGame.getGame().getStatus().equals(GameStatusEnum.DELETING)) {
                        List<TelegramPlayer> telegramPlayerList = telegramPlayerService.deletePlayers(telegramGame);

                        telegramGameService.deleteGame(telegramGame);

                        sendDeleteMessages(telegramGame, telegramPlayerList);
                    } else {
                        sendMainMenu(telegramGame);
                    }
                } catch (GameNotStartedException e) {
                    botService.answerCallbackQuery(callbackQueryId, CCLHBotResponseErrorI18n.GAME_DOESNT_EXISTS);

                    throw e;
                } catch (PlayerAlreadyVotedDeleteException e) {
                    botService.answerCallbackQuery(callbackQueryId, CCLHBotResponseErrorI18n.PLAYER_ALREADY_VOTED_DELETION);

                    throw e;
                } catch (ApplicationException e) {
                    botService.answerCallbackQuery(callbackQueryId, e.getMessage());

                    throw e;
                }
            } else {
                botService.answerCallbackQuery(callbackQueryId, CCLHBotResponseErrorI18n.GAME_ONLY_CREATOR_CAN_DELETE);
            }
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
    public void gameDeletePrivateQuery(long userId, String callbackQueryId) {
        TelegramGame telegramGame = telegramGameService.getGameByCreatorId(userId);

        if (telegramGame == null) {
            logger.error("No existe juego asociado al usuario {}", userId);

            botService.sendMessage(userId, CCLHBotResponseErrorI18n.PLAYER_NO_GAMES);

            return;
        }

        try {
            List<TelegramPlayer> telegramPlayerList = telegramPlayerService.deletePlayers(telegramGame);

            telegramGameService.deleteGame(telegramGame);

            sendDeleteMessages(telegramGame, telegramPlayerList);
        } catch (ApplicationException e) {
            botService.answerCallbackQuery(callbackQueryId, e.getMessage());

            throw e;
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
    public void gameJoinQuery(long roomId, long userId, String callbackQueryId) {
        TelegramGame telegramGame = telegramGameService.getGame(roomId);

        if (telegramGame == null) {
            logger.error("No hay partida activa en la sala {}", roomId);

            botService.answerCallbackQuery(callbackQueryId, CCLHBotResponseErrorI18n.GAME_DOESNT_EXISTS);

            return;
        }

        if (userId != telegramGame.getGame().getCreator().getId()) {
            botService.sendMessageAsync(userId, CCLHBotResponseMessageI18n.PLAYER_JOINING, new BotService.Callback() {
                @Override
                public void success(BotApiMethod<Message> method, Message response) {
                    try {
                        cclhBotService.joinGame(
                                roomId,
                                userId,
                                BotUtils.getUsername(response.getChat()),
                                response.getMessageId(), callbackQueryId);
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    }
                }

                @Override
                public void failure(BotApiMethod<Message> method, Exception e) {
                    logger.error("Fallo al enviar mensaje", e);
                }
            });
        } else {
            botService.answerCallbackQuery(callbackQueryId, CCLHBotResponseErrorI18n.PLAYER_ALREADY_JOINED);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
    public void joinGame(long roomId, long userId, String username, int playerMessageId, String callbackQueryId) {
        TelegramGame telegramGame = telegramGameService.getGame(roomId);

        try {
            TelegramPlayer telegramPlayer = telegramPlayerService.createPlayer(telegramGame, userId, username, playerMessageId);

            telegramGameService.addPlayer(telegramGame, telegramPlayer);

            InlineKeyboardMarkup privateInlineKeyboard = InlineKeyboardMarkup.builder()
                    .keyboardRow(Collections.singletonList(InlineKeyboardButton.builder().text("Dejar la partida")
                            .callbackData("game_leave").build()))
                    .build();

            botService.editMessage(userId, playerMessageId, CCLHBotResponseMessageI18n.PLAYER_JOINED, privateInlineKeyboard);

            sendMainMenu(telegramGame);
        } catch (UserDoesntExistsException e) {
            botService.answerCallbackQuery(callbackQueryId, CCLHBotResponseErrorI18n.GAME_USER_DOESNT_EXISTS);

            throw e;
        } catch (PlayerAlreadyExistsException e) {
            botService.answerCallbackQuery(callbackQueryId, CCLHBotResponseErrorI18n.PLAYER_ALREADY_JOINED);

            throw e;
        } catch (GameAlreadyStartedException e) {
            botService.answerCallbackQuery(callbackQueryId, CCLHBotResponseErrorI18n.GAME_ALREADY_STARTED);

            throw e;
        } catch (GameAlreadyFilledException e) {
            botService.answerCallbackQuery(callbackQueryId, CCLHBotResponseErrorI18n.GAME_ALREADY_FILLED);

            throw e;
        } catch (ApplicationException e) {
            botService.answerCallbackQuery(callbackQueryId, e.getMessage());

            throw e;
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
    public void leaveGame(long userId, String callbackQueryId) {
        TelegramGame telegramGame = telegramGameService.getByPlayerUser(userId);

        try {
            TelegramPlayer telegramPlayer = telegramPlayerService.getByUser(userId);

            telegramGameService.removePlayer(telegramGame, telegramPlayer);
            telegramPlayerService.deletePlayer(telegramPlayer);

            botService.answerCallbackQuery(callbackQueryId, "Has dejado la partida");
            botService.deleteMessage(telegramPlayer.getPlayer().getUser().getId(), telegramPlayer.getMessageId());

            sendMainMenu(telegramGame);
        } catch (ApplicationException e) {
            botService.answerCallbackQuery(callbackQueryId, e.getMessage());

            throw e;
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
    public void gameStartQuery(long roomId, long userId, String callbackQueryId) {
        TelegramGame telegramGame = telegramGameService.getGame(roomId);

        if (telegramGame == null) {
            logger.error("No hay partida activa en la sala {}", roomId);

            botService.answerCallbackQuery(callbackQueryId, CCLHBotResponseErrorI18n.GAME_DOESNT_EXISTS);

            return;
        }

        if (userId == telegramGame.getGame().getCreator().getId()) {
            try {
                telegramGameService.startGame(telegramGame);

                if (telegramGame.getGame().getStatus().equals(GameStatusEnum.STARTED)) {
                    startRound(telegramGame);
                } else {
                    logger.error("La partida no se ha iniciado correctamente");

                    botService.answerCallbackQuery(callbackQueryId, CCLHBotResponseErrorI18n.UNKNOWN_ERROR);
                }
            } catch (GameAlreadyStartedException e) {
                botService.answerCallbackQuery(callbackQueryId, CCLHBotResponseErrorI18n.GAME_ALREADY_STARTED);

                throw e;
            } catch (GameNotFilledException e) {
                botService.answerCallbackQuery(callbackQueryId, CCLHBotResponseErrorI18n.GAME_NOT_FILLED);

                throw e;
            } catch (GameNotStartedException e) {
                botService.answerCallbackQuery(callbackQueryId, CCLHBotResponseErrorI18n.GAME_NOT_STARTED);

                throw e;
            } catch (ApplicationException e) {
                botService.answerCallbackQuery(callbackQueryId, e.getMessage());

                throw e;
            }
        } else {
            logger.error("Intentando iniciar una partida en {} que no le corresponde a {}", roomId, userId);

            botService.answerCallbackQuery(callbackQueryId, CCLHBotResponseErrorI18n.GAME_ONLY_CREATOR_CAN_START);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class, isolation = Isolation.READ_UNCOMMITTED)
    public void playerPlayCardQuery(long userId, String callbackQueryId, String data) {
        TelegramGame telegramGame = telegramGameService.getByPlayerUser(userId);

        if (telegramGame == null) {
            logger.error("No existe juego asociado al usuario {}", userId);

            botService.answerCallbackQuery(callbackQueryId, CCLHBotResponseErrorI18n.PLAYER_NO_GAMES);

            return;
        }

        try {
            telegramGameService.playCard(telegramGame, userId, Integer.parseInt(data));

            if (telegramGame.getGame().getTable().getStatus().equals(TableStatusEnum.PLAYING)) {
                if (telegramGameService.checkIfEveryoneHavePlayedACard(telegramGame)) {
                    logger.error("Juego en estado incorrecto: {}", telegramGame.getGame().getId());

                    botService.answerCallbackQuery(callbackQueryId, CCLHBotResponseErrorI18n.UNKNOWN_ERROR);

                    throw new ApplicationException("Error desconocido");
                }

                TelegramPlayer telegramPlayer = telegramPlayerService.getByUser(userId);

                PlayedCard playedCard = getPlayedCardByPlayer(telegramGame, telegramPlayer);

                if (playedCard != null) {
                    botService.editMessage(telegramPlayer.getPlayer().getUser().getId(),
                            telegramPlayer.getMessageId(),
                            StringUtils.formatMessage(CCLHBotResponseMessageI18n.PLAYER_SELECTED_CARD,
                                    telegramGame.getGame().getTable().getCurrentRoundNumber(),
                                    telegramGame.getGame().getTable().getCurrentBlackCard().getText(),
                                    playedCard.getCard().getText()));
                } else {
                    logger.error("No se ha encontrado el jugador o la carta jugada");

                    botService.answerCallbackQuery(callbackQueryId, CCLHBotResponseErrorI18n.PLAYER_DOES_NOT_EXISTS);
                }
            } else if (telegramGame.getGame().getTable().getStatus().equals(TableStatusEnum.VOTING)) {
                if (telegramGame.getGame().getType().equals(GameTypeEnum.DEMOCRACY)) {
                    botService.editMessage(
                            telegramGame.getGame().getRoom().getId(),
                            telegramGame.getBlackCardMessageId(),
                            getGameVoteCardMessage(telegramGame));

                    List<TelegramPlayer> telegramPlayers = telegramPlayerService.getPlayers(telegramGame);

                    for (TelegramPlayer tgPlayer : telegramPlayers) {
                        sendVotesToPlayer(telegramGame, tgPlayer);
                    }
                } else if (telegramGame.getGame().getType().equals(GameTypeEnum.CLASSIC)
                        || telegramGame.getGame().getType().equals(GameTypeEnum.DICTATORSHIP)) {
                    sendVotesToPlayer(telegramGame, telegramPlayerService.getByUser(telegramGame.getGame().getCreator().getId()));
                }
            } else {
                logger.error("Juego en estado incorrecto: {}", telegramGame.getGame().getId());
            }
        } catch (PlayerAlreadyPlayedCardException e) {
            botService.answerCallbackQuery(callbackQueryId, CCLHBotResponseErrorI18n.PLAYER_ALREADY_PLAYED_CARD);

            throw e;
        } catch (ApplicationException e) {
            botService.answerCallbackQuery(callbackQueryId, e.getMessage());

            throw e;
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class, isolation = Isolation.READ_UNCOMMITTED)
    public void playerVoteCardQuery(long userId, String callbackQueryId, String data) {
        TelegramGame telegramGame = telegramGameService.getByPlayerUser(userId);

        if (telegramGame == null) {
            logger.error("No existe juego asociado al usuario {}", userId);

            botService.sendMessage(userId, CCLHBotResponseErrorI18n.PLAYER_NO_GAMES);

            return;
        }

        try {
            telegramGameService.voteCard(telegramGame, userId, Integer.parseInt(data));

            if (telegramGame.getGame().getTable().getStatus().equals(TableStatusEnum.VOTING)) {
                if (telegramGameService.checkIfEveryoneHaveVotedACard(telegramGame)) {
                    logger.error("Juego en estado incorrecto: {}", telegramGame.getGame().getId());

                    botService.answerCallbackQuery(callbackQueryId, CCLHBotResponseErrorI18n.UNKNOWN_ERROR);

                    throw new ApplicationException("Error desconocido");
                }

                sendMessageToVoter(telegramGame, userId, callbackQueryId);
            } else if (telegramGame.getGame().getTable().getStatus().equals(TableStatusEnum.ENDING)) {
                sendMessageToVoter(telegramGame, userId, callbackQueryId);

                PlayedCard mostVotedCard = telegramGameService.getMostVotedCard(telegramGame);

                if (mostVotedCard == null) throw new ApplicationException("No se ha localizado ninguna carta ganadora");

                telegramPlayerService.incrementPoints(mostVotedCard.getPlayer());

                botService.editMessage(
                        telegramGame.getGame().getRoom().getId(),
                        telegramGame.getBlackCardMessageId(),
                        getGameEndRoundMessage(telegramGame, mostVotedCard));

                telegramGameService.endRound(telegramGame);

                if (telegramGame.getGame().getStatus().equals(GameStatusEnum.STARTED)) {
                    startRound(telegramGame);
                } else if (telegramGame.getGame().getStatus().equals(GameStatusEnum.ENDED)) {
                    Player winner = getWinnerPlayer(telegramGame);

                    if (winner != null) {
                        botService.sendMessage(telegramGame.getGame().getRoom().getId(), getGameEndGameMessage(winner));

                        List<TelegramPlayer> telegramPlayerList = telegramPlayerService.deletePlayers(telegramGame);

                        telegramGameService.deleteGame(telegramGame);

                        sendEndMessages(telegramGame, telegramPlayerList);
                    } else {
                        logger.error("Juego en estado incorrecto: {}", telegramGame.getGame().getId());

                        botService.answerCallbackQuery(callbackQueryId, CCLHBotResponseErrorI18n.UNKNOWN_ERROR);

                        throw new ApplicationException("Error desconocido");
                    }
                }
            } else {
                logger.error("Juego en estado incorrecto: {}", telegramGame.getGame().getId());

                botService.answerCallbackQuery(callbackQueryId, CCLHBotResponseErrorI18n.UNKNOWN_ERROR);

                throw new ApplicationException("Error desconocido");
            }
        } catch (PlayerAlreadyVotedCardException e) {
            botService.answerCallbackQuery(callbackQueryId, CCLHBotResponseErrorI18n.PLAYER_ALREADY_VOTED_CARD);

            throw e;
        } catch (ApplicationException e) {
            botService.answerCallbackQuery(callbackQueryId, e.getMessage());

            throw e;
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
    public void sendMessageToEveryone(String msg) {
        List<User> userList = userService.getAllUsers();

        for (User user : userList) {
            if (Boolean.TRUE.equals(canSendGlobalMessages)) {
                botService.sendMessageAsync(user.getId(), msg, new BotService.Callback() {
                    @Override
                    public void success(BotApiMethod<Message> method, Message response) {
                        userService.setActive(user, true);
                    }

                    @Override
                    public void failure(BotApiMethod<Message> method, Exception e) {
                        userService.setActive(user, false);

                        logger.error("El usuario {}({}) será desactivado por el error {}", user.getName(), user.getId(), e.getMessage(), e);
                    }
                });
            }
        }

        List<Room> roomList = roomService.getAllRooms();

        for (Room room : roomList) {
            if (Boolean.TRUE.equals(canSendGlobalMessages)) {
                botService.sendMessageAsync(room.getId(), msg, new BotService.Callback() {
                    @Override
                    public void success(BotApiMethod<Message> method, Message response) {
                        roomService.setActive(room, true);
                    }

                    @Override
                    public void failure(BotApiMethod<Message> method, Exception e) {
                        logger.error("La sala {}({}) ha sido bloqueada por el error {}", room.getName(), room.getId(), e.getMessage(), e);

                        roomService.setActive(room, false);

                        botService.sendMessage(cclhBotService.getBotCreatorId(), "Desactivando la sala " + room.getName());
                    }
                });
            }
        }
    }

    @Override
    public void toggleGlobalMessages() {
        canSendGlobalMessages = !canSendGlobalMessages;

        if (Boolean.TRUE.equals(canSendGlobalMessages)) {
            botService.sendMessage(cclhBotService.getBotCreatorId(), "Activados mensajes globales");
        } else {
            botService.sendMessage(cclhBotService.getBotCreatorId(), "Desctivados mensajes globales");
        }
    }

    @Override
    public void sendHelpMessage(long roomId) {
        botService.sendMessage(roomId, getHelpMessage());
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

            if (telegramGame.getGame().getPlayers().size() >= getGameMinNumberOfPlayers()) {
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
    }

    private void sendCreatorPrivateMenu(TelegramGame telegramGame) {
        InlineKeyboardMarkup privateInlineKeyboard = InlineKeyboardMarkup.builder()
                .keyboardRow(Collections.singletonList(InlineKeyboardButton.builder()
                        .text("Borrar juego").callbackData("game_delete_private").build()))
                .build();

        botService.editMessage(telegramGame.getGame().getCreator().getId(),
                telegramGame.getPrivateMessageId(), CCLHBotResponseMessageI18n.PLAYER_CREATED_GAME, privateInlineKeyboard);
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
                CCLHBotResponseMessageI18n.GAME_DELETED);
        botService.editMessage(telegramGame.getGame().getCreator().getId(),
                telegramGame.getPrivateMessageId(),
                CCLHBotResponseMessageI18n.GAME_DELETED);
    }

    private void sendEndMessages(TelegramGame telegramGame, List<TelegramPlayer> telegramPlayerList) {
        // Delete game messages
        for (TelegramPlayer telegramPlayer : telegramPlayerList) {
            botService.deleteMessage(telegramPlayer.getPlayer().getUser().getId(), telegramPlayer.getMessageId());
        }

        // Edit game messages
        botService.deleteMessage(telegramGame.getGame().getRoom().getId(), telegramGame.getGroupMessageId());
        botService.deleteMessage(telegramGame.getGame().getCreator().getId(), telegramGame.getPrivateMessageId());
    }

    private void startRound(TelegramGame telegramGame) {
        if (telegramGame.getGame().getTable().getStatus().equals(TableStatusEnum.STARTING)) {
            telegramGameService.startRound(telegramGame);

            sendMainMenu(telegramGame);

            if (telegramGame.getGame().getTable().getStatus().equals(TableStatusEnum.PLAYING)) {
                String msg = StringUtils.formatMessage(CCLHBotResponseMessageI18n.GAME_SELECT_CARD,
                        telegramGame.getGame().getTable().getCurrentRoundNumber(),
                        telegramGame.getGame().getTable().getCurrentBlackCard().getText());

                botService.sendMessageAsync(telegramGame.getGame().getRoom().getId(), msg, new BotService.Callback() {
                    @Override
                    public void success(BotApiMethod<Message> method, Message response) {
                        telegramGameService.setBlackCardMessage(telegramGame, response.getMessageId());
                    }

                    @Override
                    public void failure(BotApiMethod<Message> method, Exception e) {
                        logger.error("Fallo al enviar mensaje", e);
                    }
                });

                List<TelegramPlayer> telegramPlayers = new ArrayList<>(telegramPlayerService.getPlayers(telegramGame));

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
                            StringUtils.formatMessage(CCLHBotResponseMessageI18n.PLAYER_SELECT_CARD,
                                    telegramGame.getGame().getTable().getCurrentRoundNumber(),
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
        TelegramPlayer telegramPlayer = telegramPlayerService.getByUser(userId);

        PlayedCard playedCard = getPlayedCardByPlayer(telegramGame, telegramPlayer);
        VotedCard votedCard = getVotedCardByPlayer(telegramGame, telegramPlayer);

        if (playedCard != null && votedCard != null) {
            botService.editMessage(telegramPlayer.getPlayer().getUser().getId(),
                    telegramPlayer.getMessageId(), getPlayerVotedCardMessage(telegramGame, playedCard, votedCard));
        } else {
            logger.error("No se ha encontrado el jugador o la carta jugada");

            botService.answerCallbackQuery(callbackQueryId, CCLHBotResponseErrorI18n.PLAYER_DOES_NOT_EXISTS);
        }
    }

    private String getGameCreatedGroupMessage(TelegramGame telegramGame) {
        String msg = CCLHBotResponseMessageI18n.GAME_CREATED_GROUP;

        msg += "\n";

        msg += MessageFormat.format(CCLHBotResponseMessageI18n.GAME_SELECTED_MODE,
                CCLHBotResponseMessageI18n.getGameTypeName(telegramGame.getGame().getType()));

        msg += "\n";

        msg += MessageFormat.format(CCLHBotResponseMessageI18n.GAME_SELECTED_DICTIONARY,
                telegramGame.getGame().getDictionary().getName());

        msg += "\n";

        if (telegramGame.getGame().getPunctuationType() == GamePunctuationTypeEnum.POINTS) {
            msg += MessageFormat.format(CCLHBotResponseMessageI18n.GAME_SELECTED_POINTS_TO_WIN,
                    telegramGame.getGame().getNumberOfCardsToWin());
        } else if (telegramGame.getGame().getPunctuationType() == GamePunctuationTypeEnum.ROUNDS) {
            msg += MessageFormat.format(CCLHBotResponseMessageI18n.GAME_SELECTED_ROUNDS_TO_END,
                    telegramGame.getGame().getNumberOfRounds());
        }

        msg += "\n";

        msg += MessageFormat.format(CCLHBotResponseMessageI18n.GAME_SELECTED_MAX_PLAYER_NUMBER,
                telegramGame.getGame().getMaxNumberOfPlayers());

        return msg;
    }

    private String getGameCreatedCurrentPlayerNumberMessage(TelegramGame telegramGame) {
        return MessageFormat.format(CCLHBotResponseMessageI18n.GAME_CREATED_CURRENT_PLAYER_NUMBER,
                telegramGame.getGame().getPlayers().size());
    }

    private String getGameCreatedCurrentVoteDeletionNumberMessage(TelegramGame telegramGame) {
        return MessageFormat.format(CCLHBotResponseMessageI18n.GAME_CREATED_CURRENT_VOTE_DELETION_NUMBER,
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

        return MessageFormat.format(CCLHBotResponseMessageI18n.GAME_VOTE_CARD,
                telegramGame.getGame().getTable().getCurrentRoundNumber(),
                telegramGame.getGame().getTable().getCurrentBlackCard().getText(),
                playedCards);
    }

    private String getGameEndRoundMessage(TelegramGame telegramGame, PlayedCard mostVotedCard) {
        StringBuilder playedCardsSB = new StringBuilder();
        for (PlayedCard playedCard : telegramGame.getGame().getTable().getPlayedCards()) {
            playedCardsSB
                    .append("<b>")
                    .append(playedCard.getCard().getText())
                    .append("</b>")
                    .append(" - ")
                    .append(playedCard.getPlayer().getUser().getName())
                    .append("\n");
        }

        StringBuilder playerPointsSB = new StringBuilder();
        for (Player player : telegramGame.getGame().getPlayers()) {
            playerPointsSB
                    .append("<b>")
                    .append(player.getUser().getName())
                    .append("</b>")
                    .append(": ")
                    .append(player.getPoints())
                    .append("\n");
        }

        return MessageFormat.format(CCLHBotResponseMessageI18n.GAME_END_ROUND,
                mostVotedCard.getPlayer().getUser().getName(),
                mostVotedCard.getCard().getText(),
                telegramGame.getGame().getTable().getCurrentRoundNumber(),
                telegramGame.getGame().getTable().getCurrentBlackCard().getText(),
                playedCardsSB,
                playerPointsSB);
    }

    private String getPlayerVoteCardMessage(TelegramGame telegramGame, PlayedCard playedCard) {
        return MessageFormat.format(CCLHBotResponseMessageI18n.PLAYER_VOTE_CARD,
                telegramGame.getGame().getTable().getCurrentRoundNumber(),
                telegramGame.getGame().getTable().getCurrentBlackCard().getText(),
                playedCard.getCard().getText());
    }

    private String getPlayerVotedCardMessage(TelegramGame telegramGame, PlayedCard playedCard, VotedCard votedCard) {
        return MessageFormat.format(CCLHBotResponseMessageI18n.PLAYER_VOTED_CARD,
                telegramGame.getGame().getTable().getCurrentRoundNumber(),
                telegramGame.getGame().getTable().getCurrentBlackCard().getText(),
                playedCard.getCard().getText(),
                votedCard.getCard().getText());
    }

    private String getGameEndGameMessage(Player winner) {
        return MessageFormat.format(CCLHBotResponseMessageI18n.GAME_END_GAME, winner.getUser().getName());
    }

    private String getHelpMessage() {
        return MessageFormat.format(CCLHBotResponseMessageI18n.HELP, getBotName(), getBotVersion(), getBotHelpURL(), getBotCreatorName());
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

    private int getGameMinNumberOfPlayers() {
        return Integer.parseInt(configurationService.getConfiguration("game_min_number_of_players"));
    }

    private int getGameDictionariesPerPage() {
        return Integer.parseInt(configurationService.getConfiguration("game_dictionaries_per_page"));
    }

    private String getBotName() {
        return configurationService.getConfiguration("cclh_bot_name") +
                " (" + configurationService.getConfiguration("cclh_bot_alias") + ")";
    }

    private String getBotVersion() {
        return configurationService.getConfiguration("cclh_bot_version");
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
    public Long getBotCreatorId() {
        return Long.parseLong(configurationService.getConfiguration("cclh_bot_owner_id"));
    }

    private String getBotCreatorName() {
        return configurationService.getConfiguration("cclh_bot_owner_alias");
    }

    private String getBotHelpURL() {
        return configurationService.getConfiguration("cclh_bot_help_url");
    }

    @Autowired
    public void setBotService(BotService botService) {
        this.botService = botService;
    }

    @Autowired
    public void setCclhBotService(CCLHBotService cclhBotService) {
        this.cclhBotService = cclhBotService;
    }

    @Autowired
    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    @Autowired
    public void setRoomService(RoomService roomService) {
        this.roomService = roomService;
    }

    @Autowired
    public void setDictionaryService(DictionaryService dictionaryService) {
        this.dictionaryService = dictionaryService;
    }

    @Autowired
    public void setConfigurationService(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    @Autowired
    public void setTelegramGameService(TelegramGameService telegramGameService) {
        this.telegramGameService = telegramGameService;
    }

    @Autowired
    public void setTelegramPlayerService(TelegramPlayerService telegramPlayerService) {
        this.telegramPlayerService = telegramPlayerService;
    }

}
