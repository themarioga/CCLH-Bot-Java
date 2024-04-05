package org.themarioga.cclh.bot.game.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.themarioga.bot.util.BotMessageUtils;
import org.themarioga.cclh.bot.game.model.TelegramGame;
import org.themarioga.cclh.bot.game.model.TelegramPlayer;
import org.themarioga.cclh.bot.game.service.intf.CCLHBotMessageService;
import org.themarioga.cclh.bot.game.service.intf.CCLHBotService;
import org.themarioga.cclh.bot.game.service.intf.TelegramGameService;
import org.themarioga.cclh.bot.game.service.intf.TelegramPlayerService;
import org.themarioga.cclh.bot.security.CCLHSecurityUtils;
import org.themarioga.cclh.bot.security.CCLHUserDetails;
import org.themarioga.cclh.bot.security.CCLHUserRole;
import org.themarioga.cclh.bot.service.intf.I18NService;
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
import org.themarioga.cclh.commons.services.intf.*;

import java.text.MessageFormat;
import java.util.*;

@Service
public class CCLHBotServiceImpl implements CCLHBotService {

    private final Logger logger = LoggerFactory.getLogger(CCLHBotServiceImpl.class);

    private CCLHBotMessageService cclhBotMessageService;
    private CCLHBotService cclhBotService;
    private UserService userService;
    private RoomService roomService;
    private I18NService i18NService;
    private DictionaryService dictionaryService;
    private ConfigurationService configurationService;
    private TelegramGameService telegramGameService;
    private TelegramPlayerService telegramPlayerService;

    private Boolean canSendGlobalMessages = Boolean.TRUE;

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
    public void registerUser(long userId, String username, String lang) {
        try {
            userService.createOrReactivate(userId, username, i18NService.getLanguage(lang));

            cclhBotMessageService.sendMessage(userId, i18NService.get("PLAYER_WELCOME", lang));
        } catch (UserAlreadyExistsException e) {
            logger.error("El usuario {} ({}) esta intentando registrarse de nuevo.", userId, username);

            cclhBotMessageService.sendMessage(userId, i18NService.get("USER_ALREADY_REGISTERED"));

            throw e;
        } catch (ApplicationException e) {
            cclhBotMessageService.sendMessage(userId, e.getMessage());

            throw e;
        }
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS, rollbackFor = ApplicationException.class)
    public void loginUser(long userId) {
        CCLHSecurityUtils.setUserDetails(
                userService.getById(userId),
                userId == getBotCreatorId() ? CCLHUserRole.ADMIN : CCLHUserRole.USER);
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS, rollbackFor = ApplicationException.class)
    public void changeUserLanguageMessage() {
        if (!CCLHSecurityUtils.isLoggedIn()) throw new UserDoesntExistsException();

        InlineKeyboardMarkup.InlineKeyboardMarkupBuilder keyboardBuilder = InlineKeyboardMarkup.builder();
        List<Lang> languages = i18NService.getLanguages();
        for (Lang lang : languages) {
            keyboardBuilder.keyboardRow(Collections.singletonList(InlineKeyboardButton.builder().text(lang.getName())
                    .callbackData("change_user_lang__" + lang.getId()).build()));
        }

        cclhBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("USER_LANG_CHANGE"),
                keyboardBuilder.build());
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
    public void changeUserLanguage(int messageId, String lang) {
        if (!CCLHSecurityUtils.isLoggedIn()) throw new UserDoesntExistsException();

        CCLHSecurityUtils.setUserDetails(
                userService.setLanguage(userService.getById(CCLHSecurityUtils.getId()), i18NService.getLanguage(lang)),
                CCLHSecurityUtils.getId() == getBotCreatorId() ? CCLHUserRole.ADMIN : CCLHUserRole.USER);

        cclhBotMessageService.deleteMessage(CCLHSecurityUtils.getId(), messageId);
        cclhBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("USER_LANG_CHANGED"));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
    public void startCreatingGame(long roomId, String roomTitle) {
        if (!CCLHSecurityUtils.isLoggedIn()) throw new UserDoesntExistsException();
        Long userId = CCLHSecurityUtils.getId();
        Lang lang = CCLHSecurityUtils.getLang();
        CCLHUserDetails userDetails = CCLHSecurityUtils.getUserDetails();

        cclhBotMessageService.sendMessageAsync(roomId, i18NService.get("GAME_CREATING", lang), new CCLHBotMessageService.Callback() {
            @Override
            public void success(BotApiMethod<Message> method, Message groupResponse) {
                cclhBotMessageService.sendMessageAsync(userId, i18NService.get("GAME_CREATING", lang),
                        new CCLHBotMessageService.Callback() {
                    @Override
                    public void success(BotApiMethod<Message> method, Message privateResponse) {
                        cclhBotMessageService.sendMessageAsync(userId, i18NService.get("PLAYER_JOINING", lang),
                                new CCLHBotMessageService.Callback() {
                            @Override
                            public void success(BotApiMethod<Message> method, Message playerResponse) {
                                try {
                                    CCLHSecurityUtils.setUserDetails(userDetails);

                                    cclhBotService.createGame(roomId, roomTitle, userId,
                                            BotMessageUtils.getUsername(playerResponse.getChat()),
                                            privateResponse.getMessageId(), groupResponse.getMessageId(),
                                            playerResponse.getMessageId());
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
    public void createGame(long roomId, String roomTitle, long creatorId, String creatorName, int privateMessageId,
                           int groupMessageId, int playerMessageId) {
        if (!CCLHSecurityUtils.isLoggedIn()) throw new UserDoesntExistsException();

        try {
            TelegramGame telegramGame = telegramGameService.createGame(roomId, roomTitle, creatorId, groupMessageId,
                    privateMessageId);

            TelegramPlayer telegramPlayer = telegramPlayerService.createPlayer(telegramGame, creatorId, creatorName,
                    playerMessageId);

            telegramGameService.addPlayer(telegramGame, telegramPlayer);

            sendMainMenu(telegramGame);

            sendCreatorPrivateMenu(telegramGame);

            cclhBotMessageService.editMessage(creatorId, playerMessageId, i18NService.get("PLAYER_JOINED"));
        } catch (GameAlreadyExistsException e) {
            logger.error("Ya existe una partida para al sala {} ({}) o creado por {}.",
                    roomId, roomTitle, creatorId);

            cclhBotMessageService.editMessage(roomId,
                    groupMessageId,
                    i18NService.get("GAME_ALREADY_CREATED"));

            cclhBotMessageService.editMessage(creatorId,
                    privateMessageId,
                    i18NService.get("GAME_ALREADY_CREATED"));

            throw e;
        } catch (PlayerAlreadyExistsException e) {
            logger.error("El jugador {} que intenta crear la partida en la sala {}({}) ya está en otra partida.",
                    creatorId, roomId, roomTitle);

            cclhBotMessageService.editMessage(roomId,
                    groupMessageId,
                    i18NService.get("PLAYER_ALREADY_PLAYING"));

            cclhBotMessageService.editMessage(creatorId,
                    privateMessageId,
                    i18NService.get("PLAYER_ALREADY_PLAYING"));

            throw e;
        } catch (ApplicationException e) {
            cclhBotMessageService.sendMessage(creatorId, e.getMessage());

            throw e;
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
    public void gameMenuQuery(long roomId, String callbackQueryId) {
        if (!CCLHSecurityUtils.isLoggedIn()) throw new UserDoesntExistsException();

        TelegramGame telegramGame = getGameByRoomIdAndCheckOwnership(roomId, callbackQueryId);

        sendMainMenu(telegramGame);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
    public void gameConfigureQuery(long roomId, String callbackQueryId) {
        if (!CCLHSecurityUtils.isLoggedIn()) throw new UserDoesntExistsException();

        TelegramGame telegramGame = getGameByRoomIdAndCheckOwnership(roomId, callbackQueryId);

        sendConfigMenu(telegramGame);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
    public void gameSelectModeQuery(long roomId, String callbackQueryId) {
        if (!CCLHSecurityUtils.isLoggedIn()) throw new UserDoesntExistsException();

        TelegramGame telegramGame = getGameByRoomIdAndCheckOwnership(roomId, callbackQueryId);

        InlineKeyboardMarkup groupInlineKeyboard = InlineKeyboardMarkup.builder()
            .keyboardRow(Collections.singletonList(InlineKeyboardButton.builder().text(i18NService.get("GAME_MODE_DEMOCRACY"))
                .callbackData("game_change_mode__0").build()))
            .keyboardRow(Collections.singletonList(InlineKeyboardButton.builder().text(i18NService.get("GAME_MODE_CLASSIC"))
                .callbackData("game_change_mode__1").build()))
            .keyboardRow(Collections.singletonList(InlineKeyboardButton.builder().text(i18NService.get("GAME_MODE_DICTATORSHIP"))
                .callbackData("game_change_mode__2").build()))
            .keyboardRow(Collections.singletonList(InlineKeyboardButton.builder().text(i18NService.get("GO_BACK"))
                .callbackData("game_configure").build()))
            .build();

        cclhBotMessageService.editMessage(telegramGame.getGame().getRoom().getId(), telegramGame.getGroupMessageId(),
                getGameCreatedGroupMessage(telegramGame) + i18NService.get("GAME_SELECT_MODE"), groupInlineKeyboard);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
    public void gameSelectPunctuationModeQuery(long roomId, String callbackQueryId) {
        if (!CCLHSecurityUtils.isLoggedIn()) throw new UserDoesntExistsException();

        TelegramGame telegramGame = getGameByRoomIdAndCheckOwnership(roomId, callbackQueryId);

        InlineKeyboardMarkup groupInlineKeyboard = InlineKeyboardMarkup.builder()
            .keyboardRow(Collections.singletonList(InlineKeyboardButton.builder()
                .text(i18NService.get("GAME_TYPE_ROUNDS")).callbackData("game_sel_n_rounds").build()))
            .keyboardRow(Collections.singletonList(InlineKeyboardButton.builder()
                .text(i18NService.get("GAME_TYPE_POINTS")).callbackData("game_sel_n_points").build()))
            .keyboardRow(Collections.singletonList(InlineKeyboardButton.builder()
                .text(i18NService.get("GO_BACK")).callbackData("game_configure").build()))
            .build();

        cclhBotMessageService.editMessage(telegramGame.getGame().getRoom().getId(), telegramGame.getGroupMessageId(),
                getGameCreatedGroupMessage(telegramGame) + i18NService.get("GAME_PUNCTUATION_MODE"),
                groupInlineKeyboard);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
    public void gameSelectNRoundsToEndQuery(long roomId, String callbackQueryId) {
        if (!CCLHSecurityUtils.isLoggedIn()) throw new UserDoesntExistsException();

        TelegramGame telegramGame = getGameByRoomIdAndCheckOwnership(roomId, callbackQueryId);

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
                InlineKeyboardButton.builder().text(i18NService.get("GO_BACK")).callbackData("game_configure").build()))
            .build();

        cclhBotMessageService.editMessage(telegramGame.getGame().getRoom().getId(), telegramGame.getGroupMessageId(),
                getGameCreatedGroupMessage(telegramGame) + i18NService.get("GAME_TYPE_ROUNDS_SELECT"),
                groupInlineKeyboard);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
    public void gameSelectNPointsToWinQuery(long roomId, String callbackQueryId) {
        if (!CCLHSecurityUtils.isLoggedIn()) throw new UserDoesntExistsException();

        TelegramGame telegramGame = getGameByRoomIdAndCheckOwnership(roomId, callbackQueryId);

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
                InlineKeyboardButton.builder().text(i18NService.get("GO_BACK")).callbackData("game_configure").build()))
            .build();

        cclhBotMessageService.editMessage(telegramGame.getGame().getRoom().getId(), telegramGame.getGroupMessageId(),
                getGameCreatedGroupMessage(telegramGame) + i18NService.get("GAME_TYPE_POINTS_SELECT"),
                groupInlineKeyboard);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
    public void gameSelectDictionaryQuery(long roomId, String callbackQueryId, String data) {
        if (!CCLHSecurityUtils.isLoggedIn()) throw new UserDoesntExistsException();

        TelegramGame telegramGame = getGameByRoomIdAndCheckOwnership(roomId, callbackQueryId);

        int pageNumber = Integer.parseInt(data);
        int dictionariesPerPage = getGameDictionariesPerPage();
        long totalDictionaries = dictionaryService.getDictionaryCount(userService.getById(CCLHSecurityUtils.getId()));
        int firstResult = (pageNumber - 1) * dictionariesPerPage;
        List<Dictionary> dictionaryList = dictionaryService.getDictionariesPaginated(userService.getById(CCLHSecurityUtils.getId()), firstResult, dictionariesPerPage);

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
                .text(i18NService.get("GO_BACK")).callbackData("game_configure").build()));

        cclhBotMessageService.editMessage(telegramGame.getGame().getRoom().getId(), telegramGame.getGroupMessageId(),
                getGameCreatedGroupMessage(telegramGame) + i18NService.get("GAME_DICTIONARY_SELECT"), groupInlineKeyboard.build());
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
    public void gameSelectMaxPlayersQuery(long roomId, String callbackQueryId) {
        if (!CCLHSecurityUtils.isLoggedIn()) throw new UserDoesntExistsException();

        TelegramGame telegramGame = getGameByRoomIdAndCheckOwnership(roomId, callbackQueryId);

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
                InlineKeyboardButton.builder().text(i18NService.get("GO_BACK")).callbackData("game_configure").build()))
            .build();

        cclhBotMessageService.editMessage(telegramGame.getGame().getRoom().getId(), telegramGame.getGroupMessageId(),
                getGameCreatedGroupMessage(telegramGame) + i18NService.get("GAME_SELECT_MAX_PLAYERS"),
                groupInlineKeyboard);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
    public void gameChangeMode(long roomId, String callbackQueryId, String data) {
        if (!CCLHSecurityUtils.isLoggedIn()) throw new UserDoesntExistsException();

        TelegramGame telegramGame = getGameByRoomIdAndCheckOwnership(roomId, callbackQueryId);

        try {
            telegramGameService.setType(telegramGame, GameTypeEnum.getEnum(Integer.parseInt(data)));

            sendConfigMenu(telegramGame);
        } catch (GameAlreadyStartedException e) {
            logger.error("La partida de la sala {} ya estaba iniciada cuando se intentó cambiar el modo", roomId);

            cclhBotMessageService.answerCallbackQuery(callbackQueryId, i18NService.get("GAME_ALREADY_STARTED"));

            throw e;
        } catch (ApplicationException e) {
            cclhBotMessageService.answerCallbackQuery(callbackQueryId, e.getMessage());

            throw e;
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
    public void gameChangeDictionary(long roomId, String callbackQueryId, String data) {
        if (!CCLHSecurityUtils.isLoggedIn()) throw new UserDoesntExistsException();

        TelegramGame telegramGame = getGameByRoomIdAndCheckOwnership(roomId, callbackQueryId);

        try {
            telegramGameService.setDictionary(telegramGame, Integer.parseInt(data));

            sendConfigMenu(telegramGame);
        } catch (GameAlreadyStartedException e) {
            logger.error("La partida de la sala {} ya estaba iniciada cuando se intentó cambiar el modo", roomId);

            cclhBotMessageService.answerCallbackQuery(callbackQueryId, i18NService.get("GAME_ALREADY_STARTED"));

            throw e;
        } catch (DictionaryDoesntExistsException e) {
            logger.error("El diccionario {} no existe para la sala {}", data, roomId);

            cclhBotMessageService.answerCallbackQuery(callbackQueryId, i18NService.get("UNKNOWN_ERROR"));

            throw e;
        } catch (ApplicationException e) {
            cclhBotMessageService.answerCallbackQuery(callbackQueryId, e.getMessage());

            throw e;
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
    public void gameChangeMaxPlayers(long roomId, String callbackQueryId, String data) {
        if (!CCLHSecurityUtils.isLoggedIn()) throw new UserDoesntExistsException();

        TelegramGame telegramGame = getGameByRoomIdAndCheckOwnership(roomId, callbackQueryId);

        try {
            telegramGameService.setMaxNumberOfPlayers(telegramGame, Integer.parseInt(data));

            sendConfigMenu(telegramGame);
        } catch (GameAlreadyStartedException e) {
            logger.error("La partida de la sala {} ya estaba iniciada cuando se intentó cambiar el modo", roomId);

            cclhBotMessageService.answerCallbackQuery(callbackQueryId, i18NService.get("GAME_ALREADY_STARTED"));

            throw e;
        } catch (GameAlreadyFilledException e) {
            cclhBotMessageService.answerCallbackQuery(callbackQueryId, i18NService.get("GAME_ALREADY_FILLED"));

            throw e;
        } catch (ApplicationException e) {
            cclhBotMessageService.answerCallbackQuery(callbackQueryId, e.getMessage());

            throw e;
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
    public void gameChangeNRoundsToEnd(long roomId, String callbackQueryId, String data) {
        if (!CCLHSecurityUtils.isLoggedIn()) throw new UserDoesntExistsException();

        TelegramGame telegramGame = getGameByRoomIdAndCheckOwnership(roomId, callbackQueryId);

        try {
            telegramGameService.setNumberOfRoundsToEnd(telegramGame, Integer.parseInt(data));

            sendConfigMenu(telegramGame);
        } catch (GameAlreadyStartedException e) {
            logger.error("La partida de la sala {} ya estaba iniciada cuando se intentó cambiar el modo", roomId);

            cclhBotMessageService.answerCallbackQuery(callbackQueryId, i18NService.get("GAME_ALREADY_STARTED"));

            throw e;
        } catch (ApplicationException e) {
            cclhBotMessageService.answerCallbackQuery(callbackQueryId, e.getMessage());

            throw e;
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
    public void gameChangeNCardsToWin(long roomId, String callbackQueryId, String data) {
        if (!CCLHSecurityUtils.isLoggedIn()) throw new UserDoesntExistsException();

        TelegramGame telegramGame = getGameByRoomIdAndCheckOwnership(roomId, callbackQueryId);

        try {
            telegramGameService.setNumberOfCardsToWin(telegramGame, Integer.parseInt(data));

            sendConfigMenu(telegramGame);
        } catch (GameAlreadyStartedException e) {
            logger.error("La partida de la sala {} ya estaba iniciada cuando se intentó cambiar el modo", roomId);

            cclhBotMessageService.answerCallbackQuery(callbackQueryId, i18NService.get("GAME_ALREADY_STARTED"));

            throw e;
        } catch (ApplicationException e) {
            cclhBotMessageService.answerCallbackQuery(callbackQueryId, e.getMessage());

            throw e;
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
    public void gameDeleteGroupQuery(long roomId, String callbackQueryId) {
        if (!CCLHSecurityUtils.isLoggedIn()) throw new UserDoesntExistsException();

        try {
            TelegramGame telegramGame = getGameByRoomIdAndCheckOwnership(roomId, callbackQueryId);

            List<TelegramPlayer> telegramPlayerList = telegramPlayerService.deletePlayers(telegramGame);

            telegramGameService.deleteGame(telegramGame);

            sendDeleteMessages(telegramGame, telegramPlayerList);
        } catch (GameNotYourException e) {
            TelegramGame telegramGame = getGameByRoomId(roomId, callbackQueryId);

            if (telegramGame.getGame().getStatus().equals(GameStatusEnum.STARTED)) {
                telegramGameService.voteForDeletion(telegramGame, CCLHSecurityUtils.getId());

                cclhBotMessageService.answerCallbackQuery(callbackQueryId, i18NService.get("PLAYER_VOTED_DELETION"));

                if (telegramGame.getGame().getStatus().equals(GameStatusEnum.DELETING)) {
                    List<TelegramPlayer> telegramPlayerList = telegramPlayerService.deletePlayers(telegramGame);

                    telegramGameService.deleteGame(telegramGame);

                    sendDeleteMessages(telegramGame, telegramPlayerList);
                } else {
                    sendMainMenu(telegramGame);
                }
            } else {
                cclhBotMessageService.answerCallbackQuery(callbackQueryId, i18NService.get("GAME_ONLY_CREATOR_CAN_DELETE"));
            }
        } catch (GameNotStartedException e) {
            cclhBotMessageService.answerCallbackQuery(callbackQueryId, i18NService.get("GAME_DOESNT_EXISTS"));

            throw e;
        } catch (PlayerAlreadyVotedDeleteException e) {
            cclhBotMessageService.answerCallbackQuery(callbackQueryId, i18NService.get("PLAYER_ALREADY_VOTED_DELETION"));

            throw e;
        } catch (ApplicationException e) {
            cclhBotMessageService.answerCallbackQuery(callbackQueryId, e.getMessage());

            throw e;
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
    public void gameDeletePrivateQuery(String callbackQueryId) {
        if (!CCLHSecurityUtils.isLoggedIn()) throw new UserDoesntExistsException();

        TelegramGame telegramGame = getGameByCreatorId(callbackQueryId);

        try {
            List<TelegramPlayer> telegramPlayerList = telegramPlayerService.deletePlayers(telegramGame);

            telegramGameService.deleteGame(telegramGame);

            sendDeleteMessages(telegramGame, telegramPlayerList);
        } catch (ApplicationException e) {
            cclhBotMessageService.answerCallbackQuery(callbackQueryId, e.getMessage());

            throw e;
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
    public void gameJoinQuery(long roomId, String callbackQueryId) {
        if (!CCLHSecurityUtils.isLoggedIn()) throw new UserDoesntExistsException();
        CCLHUserDetails userDetails = CCLHSecurityUtils.getUserDetails();

        TelegramGame telegramGame = getGameByRoomId(roomId, callbackQueryId);

        if (!Objects.equals(CCLHSecurityUtils.getId(), telegramGame.getGame().getCreator().getId())) {
            cclhBotMessageService.sendMessageAsync(CCLHSecurityUtils.getId(), i18NService.get("PLAYER_JOINING"), new CCLHBotMessageService.Callback() {
                @Override
                public void success(BotApiMethod<Message> method, Message response) {
                    try {
                        CCLHSecurityUtils.setUserDetails(userDetails);

                        cclhBotService.joinGame(
                                roomId,
                                BotMessageUtils.getUsername(response.getChat()),
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
            cclhBotMessageService.answerCallbackQuery(callbackQueryId, i18NService.get("PLAYER_ALREADY_JOINED"));
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
    public void joinGame(long roomId, String username, int playerMessageId, String callbackQueryId) {
        if (!CCLHSecurityUtils.isLoggedIn()) throw new UserDoesntExistsException();

        TelegramGame telegramGame = getGameByRoomId(roomId, callbackQueryId);

        try {
            TelegramPlayer telegramPlayer = telegramPlayerService.createPlayer(telegramGame, CCLHSecurityUtils.getId(), username, playerMessageId);

            telegramGameService.addPlayer(telegramGame, telegramPlayer);

            InlineKeyboardMarkup privateInlineKeyboard = InlineKeyboardMarkup.builder()
                    .keyboardRow(Collections.singletonList(InlineKeyboardButton.builder().text(i18NService.get("GAME_LEAVE"))
                            .callbackData("game_leave").build()))
                    .build();

            cclhBotMessageService.editMessage(CCLHSecurityUtils.getId(), playerMessageId, i18NService.get("PLAYER_JOINED"), privateInlineKeyboard);

            sendMainMenu(telegramGame);
        } catch (UserDoesntExistsException e) {
            cclhBotMessageService.answerCallbackQuery(callbackQueryId, i18NService.get("GAME_USER_DOESNT_EXISTS"));

            throw e;
        } catch (PlayerAlreadyExistsException e) {
            cclhBotMessageService.answerCallbackQuery(callbackQueryId, i18NService.get("PLAYER_ALREADY_JOINED"));

            throw e;
        } catch (GameAlreadyStartedException e) {
            cclhBotMessageService.answerCallbackQuery(callbackQueryId, i18NService.get("GAME_ALREADY_STARTED"));

            throw e;
        } catch (GameAlreadyFilledException e) {
            cclhBotMessageService.answerCallbackQuery(callbackQueryId, i18NService.get("GAME_ALREADY_FILLED"));

            throw e;
        } catch (ApplicationException e) {
            cclhBotMessageService.answerCallbackQuery(callbackQueryId, e.getMessage());

            throw e;
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
    public void leaveGame(String callbackQueryId) {
        if (!CCLHSecurityUtils.isLoggedIn()) throw new UserDoesntExistsException();

        TelegramGame telegramGame = getGameByPlayerId(callbackQueryId);

        try {
            TelegramPlayer telegramPlayer = telegramPlayerService.getByUser(CCLHSecurityUtils.getId());

            telegramGameService.removePlayer(telegramGame, telegramPlayer);
            telegramPlayerService.deletePlayer(telegramPlayer);

            cclhBotMessageService.answerCallbackQuery(callbackQueryId, i18NService.get("GAME_LEFT"));
            cclhBotMessageService.deleteMessage(telegramPlayer.getPlayer().getUser().getId(), telegramPlayer.getMessageId());

            sendMainMenu(telegramGame);
        } catch (ApplicationException e) {
            cclhBotMessageService.answerCallbackQuery(callbackQueryId, e.getMessage());

            throw e;
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
    public void gameStartQuery(long roomId, String callbackQueryId) {
        if (!CCLHSecurityUtils.isLoggedIn()) throw new UserDoesntExistsException();

        TelegramGame telegramGame = getGameByRoomIdAndCheckOwnership(roomId, callbackQueryId);

        try {
            telegramGameService.startGame(telegramGame);

            if (telegramGame.getGame().getStatus().equals(GameStatusEnum.STARTED)) {
                startRound(telegramGame);
            } else {
                logger.error("La partida no se ha iniciado correctamente");

                cclhBotMessageService.answerCallbackQuery(callbackQueryId, i18NService.get("UNKNOWN_ERROR"));
            }
        } catch (GameAlreadyStartedException e) {
            cclhBotMessageService.answerCallbackQuery(callbackQueryId, i18NService.get("GAME_ALREADY_STARTED"));

            throw e;
        } catch (GameNotFilledException e) {
            cclhBotMessageService.answerCallbackQuery(callbackQueryId, i18NService.get("GAME_NOT_FILLED"));

            throw e;
        } catch (GameNotStartedException e) {
            cclhBotMessageService.answerCallbackQuery(callbackQueryId, i18NService.get("GAME_NOT_STARTED"));

            throw e;
        } catch (ApplicationException e) {
            cclhBotMessageService.answerCallbackQuery(callbackQueryId, e.getMessage());

            throw e;
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class, isolation = Isolation.READ_UNCOMMITTED)
    public void playerPlayCardQuery(String callbackQueryId, String data) {
        if (!CCLHSecurityUtils.isLoggedIn()) throw new UserDoesntExistsException();

        TelegramGame telegramGame = getGameByPlayerId(callbackQueryId);

        try {
            telegramGameService.playCard(telegramGame, CCLHSecurityUtils.getId(), Integer.parseInt(data));

            if (telegramGame.getGame().getTable().getStatus().equals(TableStatusEnum.PLAYING)) {
                if (telegramGameService.checkIfEveryoneHavePlayedACard(telegramGame)) {
                    logger.error("Juego en estado incorrecto: {}", telegramGame.getGame().getId());

                    cclhBotMessageService.answerCallbackQuery(callbackQueryId, i18NService.get("UNKNOWN_ERROR"));

                    throw new ApplicationException(i18NService.get("UNKNOWN_ERROR"));
                }

                TelegramPlayer telegramPlayer = telegramPlayerService.getByUser(CCLHSecurityUtils.getId());

                PlayedCard playedCard = getPlayedCardByPlayer(telegramGame, telegramPlayer);

                if (playedCard != null) {
                    cclhBotMessageService.editMessage(telegramPlayer.getPlayer().getUser().getId(),
                            telegramPlayer.getMessageId(),
                            StringUtils.formatMessage(i18NService.get("PLAYER_SELECTED_CARD"),
                                    telegramGame.getGame().getTable().getCurrentRoundNumber(),
                                    telegramGame.getGame().getTable().getCurrentBlackCard().getText(),
                                    playedCard.getCard().getText()));
                } else {
                    logger.error("No se ha encontrado el jugador o la carta jugada");

                    cclhBotMessageService.answerCallbackQuery(callbackQueryId, i18NService.get("PLAYER_DOES_NOT_EXISTS"));
                }
            } else if (telegramGame.getGame().getTable().getStatus().equals(TableStatusEnum.VOTING)) {
                if (telegramGame.getGame().getType().equals(GameTypeEnum.DEMOCRACY)) {
                    cclhBotMessageService.editMessage(
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
            cclhBotMessageService.answerCallbackQuery(callbackQueryId, i18NService.get("PLAYER_ALREADY_PLAYED_CARD"));

            throw e;
        } catch (ApplicationException e) {
            cclhBotMessageService.answerCallbackQuery(callbackQueryId, e.getMessage());

            throw e;
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class, isolation = Isolation.READ_UNCOMMITTED)
    public void playerVoteCardQuery(String callbackQueryId, String data) {
        if (!CCLHSecurityUtils.isLoggedIn()) throw new UserDoesntExistsException();

        TelegramGame telegramGame = getGameByPlayerId(callbackQueryId);

        try {
            telegramGameService.voteCard(telegramGame, CCLHSecurityUtils.getId(), Integer.parseInt(data));

            if (telegramGame.getGame().getTable().getStatus().equals(TableStatusEnum.VOTING)) {
                if (telegramGameService.checkIfEveryoneHaveVotedACard(telegramGame)) {
                    logger.error("Juego en estado incorrecto: {}", telegramGame.getGame().getId());

                    cclhBotMessageService.answerCallbackQuery(callbackQueryId, i18NService.get("UNKNOWN_ERROR"));

                    throw new ApplicationException(i18NService.get("UNKNOWN_ERROR"));
                }

                sendMessageToVoter(telegramGame, callbackQueryId);
            } else if (telegramGame.getGame().getTable().getStatus().equals(TableStatusEnum.ENDING)) {
                sendMessageToVoter(telegramGame, callbackQueryId);

                PlayedCard mostVotedCard = telegramGameService.getMostVotedCard(telegramGame);

                if (mostVotedCard == null) throw new ApplicationException(i18NService.get("ERROR_CANT_FIND_WINNER_CARD"));

                telegramPlayerService.incrementPoints(mostVotedCard.getPlayer());

                cclhBotMessageService.editMessage(
                        telegramGame.getGame().getRoom().getId(),
                        telegramGame.getBlackCardMessageId(),
                        getGameEndRoundMessage(telegramGame, mostVotedCard));

                telegramGameService.endRound(telegramGame);

                if (telegramGame.getGame().getStatus().equals(GameStatusEnum.STARTED)) {
                    startRound(telegramGame);
                } else if (telegramGame.getGame().getStatus().equals(GameStatusEnum.ENDED)) {
                    Player winner = getWinnerPlayer(telegramGame);

                    if (winner != null) {
                        cclhBotMessageService.sendMessage(telegramGame.getGame().getRoom().getId(), getGameEndGameMessage(winner));

                        List<TelegramPlayer> telegramPlayerList = telegramPlayerService.deletePlayers(telegramGame);

                        telegramGameService.deleteGame(telegramGame);

                        sendEndMessages(telegramGame, telegramPlayerList);
                    } else {
                        logger.error("Juego en estado incorrecto: {}", telegramGame.getGame().getId());

                        cclhBotMessageService.answerCallbackQuery(callbackQueryId, i18NService.get("UNKNOWN_ERROR"));

                        throw new ApplicationException(i18NService.get("UNKNOWN_ERROR"));
                    }
                }
            } else {
                logger.error("Juego en estado incorrecto: {}", telegramGame.getGame().getId());

                cclhBotMessageService.answerCallbackQuery(callbackQueryId, i18NService.get("UNKNOWN_ERROR"));

                throw new ApplicationException(i18NService.get("UNKNOWN_ERROR"));
            }
        } catch (PlayerAlreadyVotedCardException e) {
            cclhBotMessageService.answerCallbackQuery(callbackQueryId, i18NService.get("PLAYER_ALREADY_VOTED_CARD"));

            throw e;
        } catch (ApplicationException e) {
            cclhBotMessageService.answerCallbackQuery(callbackQueryId, e.getMessage());

            throw e;
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
    public void deleteMyGames() {
        if (!CCLHSecurityUtils.isLoggedIn()) throw new UserDoesntExistsException();

        TelegramGame telegramGame = getGameByCreatorId();

        try {
            List<TelegramPlayer> telegramPlayerList = telegramPlayerService.deletePlayers(telegramGame);

            telegramGameService.deleteGame(telegramGame);

            sendDeleteMessages(telegramGame, telegramPlayerList);
        } catch (ApplicationException e) {
            cclhBotMessageService.sendMessage(CCLHSecurityUtils.getId(), e.getMessage());

            throw e;
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
    public void deleteGameByCreatorUsername(String username) {
        if (!CCLHSecurityUtils.isLoggedIn() || !CCLHSecurityUtils.isAdmin())
            throw new UserDoesntExistsException();

        TelegramGame telegramGame = telegramGameService.getGameByCreatorUsername(username);

        if (telegramGame == null) {
            logger.error("No existe juego asociado al usuario {}", username);

            return;
        }

        try {
            List<TelegramPlayer> telegramPlayerList = telegramPlayerService.deletePlayers(telegramGame);

            telegramGameService.deleteGame(telegramGame);

            sendDeleteMessages(telegramGame, telegramPlayerList);

            cclhBotMessageService.sendMessage(getBotCreatorId(), MessageFormat.format(i18NService.get("GAME_DELETION_USER"), username));
        } catch (ApplicationException e) {
            cclhBotMessageService.sendMessage(telegramGame.getGame().getCreator().getId(), e.getMessage());

            throw e;
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
    public void deleteAllGames() {
        if (!CCLHSecurityUtils.isLoggedIn() || !CCLHSecurityUtils.isAdmin())
            throw new UserDoesntExistsException();

        List<TelegramGame> telegramGameList = telegramGameService.getGameList();

        for (TelegramGame telegramGame : telegramGameList) {
            try {
                List<TelegramPlayer> telegramPlayerList = telegramPlayerService.deletePlayers(telegramGame);

                telegramGameService.deleteGame(telegramGame);

                sendDeleteMessages(telegramGame, telegramPlayerList);

                cclhBotMessageService.sendMessage(telegramGame.getGame().getCreator().getId(),
                        i18NService.get("GAME_DELETION_FORCED"));

                cclhBotMessageService.sendMessage(getBotCreatorId(), i18NService.get("GAME_DELETION_ALL"));
            } catch (ApplicationException e) {
                cclhBotMessageService.sendMessage(telegramGame.getGame().getCreator().getId(), e.getMessage());

                throw e;
            }
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
    public void sendMessageToEveryone(String msg) {
        if (!CCLHSecurityUtils.isLoggedIn()) throw new UserDoesntExistsException();

        List<User> userList = userService.getAllUsers();

        for (User user : userList) {
            logger.info("Enviando mensaje a {}", user.getName());

            if (Boolean.TRUE.equals(canSendGlobalMessages)) {
                cclhBotMessageService.sendMessageAsync(user.getId(), msg, new CCLHBotMessageService.Callback() {
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
            logger.info("Enviando mensaje a {}", room.getName());

            if (Boolean.TRUE.equals(canSendGlobalMessages)) {
                cclhBotMessageService.sendMessageAsync(room.getId(), msg, new CCLHBotMessageService.Callback() {
                    @Override
                    public void success(BotApiMethod<Message> method, Message response) {
                        roomService.setActive(room, true);
                    }

                    @Override
                    public void failure(BotApiMethod<Message> method, Exception e) {
                        logger.error("La sala {}({}) ha sido bloqueada por el error {}", room.getName(), room.getId(), e.getMessage(), e);

                        roomService.setActive(room, false);

                        cclhBotMessageService.sendMessage(getBotCreatorId(), "Desactivando la sala " + room.getName());
                    }
                });
            }
        }
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS, rollbackFor = ApplicationException.class)
    public void toggleGlobalMessages() {
        if (!CCLHSecurityUtils.isLoggedIn() || !CCLHSecurityUtils.isAdmin())
            throw new UserDoesntExistsException();

        canSendGlobalMessages = !canSendGlobalMessages;

        if (Boolean.TRUE.equals(canSendGlobalMessages)) {
            cclhBotMessageService.sendMessage(getBotCreatorId(), "Activados mensajes globales");
        } else {
            cclhBotMessageService.sendMessage(getBotCreatorId(), "Desctivados mensajes globales");
        }
    }

    @Override
    public void sendHelpMessage(long roomId) {
        cclhBotMessageService.sendMessage(roomId, getHelpMessage());
    }

    private TelegramGame getGameByRoomId(long roomId, String callbackQueryId) {
        TelegramGame telegramGame = telegramGameService.getGame(roomId);

        if (telegramGame == null) {
            logger.error("No hay partida activa en la sala {}", roomId);

            cclhBotMessageService.answerCallbackQuery(callbackQueryId, i18NService.get("GAME_DOESNT_EXISTS"));

            throw new GameDoesntExistsException();
        }

        return telegramGame;
    }

    private TelegramGame getGameByRoomIdAndCheckOwnership(long roomId, String callbackQueryId) {
        TelegramGame telegramGame = getGameByRoomId(roomId, callbackQueryId);

        if (!Objects.equals(CCLHSecurityUtils.getId(), telegramGame.getGame().getCreator().getId())) {
            cclhBotMessageService.answerCallbackQuery(callbackQueryId, i18NService.get("GAME_ONLY_CREATOR_CAN_CONFIGURE"));

            throw new GameNotYourException();
        }

        return telegramGame;
    }

    private TelegramGame getGameByCreatorId(String callbackQueryId) {
        TelegramGame telegramGame = telegramGameService.getGameByCreatorId(CCLHSecurityUtils.getId());

        if (telegramGame == null) {
            logger.error("No hay partida activa del creador {}", CCLHSecurityUtils.getId());

            cclhBotMessageService.answerCallbackQuery(callbackQueryId, i18NService.get("GAME_DOESNT_EXISTS"));

            throw new GameDoesntExistsException();
        }

        return telegramGame;
    }

    private TelegramGame getGameByCreatorId() {
        TelegramGame telegramGame = telegramGameService.getGameByCreatorId(CCLHSecurityUtils.getId());

        if (telegramGame == null) {
            logger.error("No existe juego asociado al usuario {}", CCLHSecurityUtils.getId());

            cclhBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("PLAYER_NO_GAMES"));

            throw new GameDoesntExistsException();
        }

        return telegramGame;
    }

    private TelegramGame getGameByPlayerId(String callbackQueryId) {
        TelegramGame telegramGame = telegramGameService.getByPlayerId(CCLHSecurityUtils.getId());

        if (telegramGame == null) {
            logger.error("No hay partida activa del jugador {}", CCLHSecurityUtils.getId());

            cclhBotMessageService.answerCallbackQuery(callbackQueryId, i18NService.get("GAME_DOESNT_EXISTS"));

            throw new GameDoesntExistsException();
        }

        return telegramGame;
    }

    private void sendMainMenu(TelegramGame telegramGame) {
        InlineKeyboardMarkup.InlineKeyboardMarkupBuilder groupInlineKeyboard = InlineKeyboardMarkup.builder();

        if (telegramGame.getGame().getStatus().equals(GameStatusEnum.CREATED)) {
            if (telegramGame.getGame().getPlayers().size() < telegramGame.getGame().getMaxNumberOfPlayers()) {
                groupInlineKeyboard.keyboardRow(Collections.singletonList(InlineKeyboardButton.builder()
                        .text(i18NService.get("GAME_JOIN_BUTTON")).callbackData("game_join").build()));
            }


            groupInlineKeyboard.keyboardRow(Collections.singletonList(InlineKeyboardButton.builder()
                    .text(i18NService.get("GAME_CONFIGURE_BUTTON")).callbackData("game_configure").build()));

            if (telegramGame.getGame().getPlayers().size() >= getGameMinNumberOfPlayers()) {
                groupInlineKeyboard.keyboardRow(Collections.singletonList(InlineKeyboardButton.builder()
                        .text(i18NService.get("GAME_START_BUTTON")).callbackData("game_start").build()));
            }
        }

        groupInlineKeyboard.keyboardRow(Collections.singletonList(InlineKeyboardButton.builder()
                .text(i18NService.get("GAME_DELETE_BUTTON")).callbackData("game_delete_group").build()));

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

        cclhBotMessageService.editMessage(telegramGame.getGame().getRoom().getId(),
                telegramGame.getGroupMessageId(), msg, groupInlineKeyboard.build());
    }

    private void sendCreatorPrivateMenu(TelegramGame telegramGame) {
        InlineKeyboardMarkup privateInlineKeyboard = InlineKeyboardMarkup.builder()
                .keyboardRow(Collections.singletonList(InlineKeyboardButton.builder()
                        .text(i18NService.get("GAME_DELETE_BUTTON")).callbackData("game_delete_private").build()))
                .build();

        cclhBotMessageService.editMessage(telegramGame.getGame().getCreator().getId(),
                telegramGame.getPrivateMessageId(), i18NService.get("PLAYER_CREATED_GAME"), privateInlineKeyboard);
    }

    private void sendConfigMenu(TelegramGame telegramGame) {
        InlineKeyboardMarkup groupInlineKeyboard = InlineKeyboardMarkup.builder()
                .keyboardRow(Collections.singletonList(InlineKeyboardButton.builder()
                        .text(i18NService.get("GAME_CHANGE_GAME_MODE")).callbackData("game_sel_mode").build()))
                .keyboardRow(Collections.singletonList(InlineKeyboardButton.builder()
                        .text(i18NService.get("GAME_CHANGE_PUNCTUATION_MODE")).callbackData("game_sel_point_type").build()))
                .keyboardRow(Collections.singletonList(InlineKeyboardButton.builder()
                        .text(i18NService.get("GAME_CHANGE_DICTIONARY")).callbackData("game_sel_dictionary__1").build()))
                .keyboardRow(Collections.singletonList(InlineKeyboardButton.builder()
                        .text(i18NService.get("GAME_CHANGE_MAX_N_PLAYERS")).callbackData("game_sel_max_players").build()))
                .keyboardRow(Collections.singletonList(InlineKeyboardButton.builder()
                        .text(i18NService.get("GO_BACK")).callbackData("game_menu").build()))
                .build();

        cclhBotMessageService.editMessage(telegramGame.getGame().getRoom().getId(), telegramGame.getGroupMessageId(),
                getGameCreatedGroupMessage(telegramGame), groupInlineKeyboard);
    }

    private void sendDeleteMessages(TelegramGame telegramGame, List<TelegramPlayer> telegramPlayerList) {
        // Delete game messages
        for (TelegramPlayer telegramPlayer : telegramPlayerList) {
            cclhBotMessageService.deleteMessage(telegramPlayer.getPlayer().getUser().getId(),
                    telegramPlayer.getMessageId());
        }
        if (telegramGame.getGame().getStatus().equals(GameStatusEnum.STARTED)) {
            cclhBotMessageService.deleteMessage(telegramGame.getGame().getRoom().getId(),
                    telegramGame.getBlackCardMessageId());
        }

        // Edit game messages
        cclhBotMessageService.editMessage(telegramGame.getGame().getRoom().getId(),
                telegramGame.getGroupMessageId(),
                i18NService.get("GAME_DELETED"));
        cclhBotMessageService.editMessage(telegramGame.getGame().getCreator().getId(),
                telegramGame.getPrivateMessageId(),
                i18NService.get("GAME_DELETED"));
    }

    private void sendEndMessages(TelegramGame telegramGame, List<TelegramPlayer> telegramPlayerList) {
        // Delete game messages
        for (TelegramPlayer telegramPlayer : telegramPlayerList) {
            cclhBotMessageService.deleteMessage(telegramPlayer.getPlayer().getUser().getId(), telegramPlayer.getMessageId());
        }

        // Edit game messages
        cclhBotMessageService.deleteMessage(telegramGame.getGame().getRoom().getId(), telegramGame.getGroupMessageId());
        cclhBotMessageService.deleteMessage(telegramGame.getGame().getCreator().getId(), telegramGame.getPrivateMessageId());
    }

    private void startRound(TelegramGame telegramGame) {
        CCLHUserDetails userDetails = CCLHSecurityUtils.getUserDetails();

        if (telegramGame.getGame().getTable().getStatus().equals(TableStatusEnum.STARTING)) {
            telegramGameService.startRound(telegramGame);

            sendMainMenu(telegramGame);

            if (telegramGame.getGame().getTable().getStatus().equals(TableStatusEnum.PLAYING)) {
                String msg = StringUtils.formatMessage(i18NService.get("GAME_SELECT_CARD"),
                        telegramGame.getGame().getTable().getCurrentRoundNumber(),
                        telegramGame.getGame().getTable().getCurrentBlackCard().getText());

                cclhBotMessageService.sendMessageAsync(telegramGame.getGame().getRoom().getId(), msg, new CCLHBotMessageService.Callback() {
                    @Override
                    public void success(BotApiMethod<Message> method, Message response) {
                        CCLHSecurityUtils.setUserDetails(userDetails);
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

                    cclhBotMessageService.editMessage(telegramPlayer.getPlayer().getUser().getId(),
                            telegramPlayer.getMessageId(),
                            StringUtils.formatMessage(i18NService.get("PLAYER_SELECT_CARD"),
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

            cclhBotMessageService.editMessage(tgPlayer.getPlayer().getUser().getId(),
                    tgPlayer.getMessageId(), getPlayerVoteCardMessage(telegramGame, playedCard),
                    cardInlineKeyboard.build());
        } else {
            logger.error("No se ha encontrado la carta del jugador");
        }
    }

    private void sendMessageToVoter(TelegramGame telegramGame, String callbackQueryId) {
        TelegramPlayer telegramPlayer = telegramPlayerService.getByUser(CCLHSecurityUtils.getId());

        PlayedCard playedCard = getPlayedCardByPlayer(telegramGame, telegramPlayer);
        VotedCard votedCard = getVotedCardByPlayer(telegramGame, telegramPlayer);

        if (playedCard != null && votedCard != null) {
            cclhBotMessageService.editMessage(telegramPlayer.getPlayer().getUser().getId(),
                    telegramPlayer.getMessageId(), getPlayerVotedCardMessage(telegramGame, playedCard, votedCard));
        } else {
            logger.error("No se ha encontrado el jugador o la carta jugada");

            cclhBotMessageService.answerCallbackQuery(callbackQueryId, i18NService.get("PLAYER_DOES_NOT_EXISTS"));
        }
    }

    private String getGameCreatedGroupMessage(TelegramGame telegramGame) {
        String msg = i18NService.get("GAME_CREATED_GROUP");

        msg += "\n";

        msg += MessageFormat.format(i18NService.get("GAME_SELECTED_MODE"),
                getGameTypeName(telegramGame.getGame().getType()));

        msg += "\n";

        msg += MessageFormat.format(i18NService.get("GAME_SELECTED_DICTIONARY"),
                telegramGame.getGame().getDictionary().getName());

        msg += "\n";

        if (telegramGame.getGame().getPunctuationType() == GamePunctuationTypeEnum.POINTS) {
            msg += MessageFormat.format(i18NService.get("GAME_SELECTED_POINTS_TO_WIN"),
                    telegramGame.getGame().getNumberOfCardsToWin());
        } else if (telegramGame.getGame().getPunctuationType() == GamePunctuationTypeEnum.ROUNDS) {
            msg += MessageFormat.format(i18NService.get("GAME_SELECTED_ROUNDS_TO_END"),
                    telegramGame.getGame().getNumberOfRounds());
        }

        msg += "\n";

        msg += MessageFormat.format(i18NService.get("GAME_SELECTED_MAX_PLAYER_NUMBER"),
                telegramGame.getGame().getMaxNumberOfPlayers());

        return msg;
    }

    private String getGameCreatedCurrentPlayerNumberMessage(TelegramGame telegramGame) {
        StringBuilder players = new StringBuilder();
        players.append(i18NService.get("GAME_CREATED_CURRENT_PLAYER_NUMBER")).append("\n");
        for (Player player : telegramGame.getGame().getPlayers()) {
            players.append(player.getUser().getName()).append("\n");
        }

        return MessageFormat.format(players.toString(),
                telegramGame.getGame().getPlayers().size());
    }

    private String getGameCreatedCurrentVoteDeletionNumberMessage(TelegramGame telegramGame) {
        return MessageFormat.format(i18NService.get("GAME_CREATED_CURRENT_VOTE_DELETION_NUMBER"),
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

        return MessageFormat.format(i18NService.get("GAME_VOTE_CARD"),
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

        return MessageFormat.format(i18NService.get("GAME_END_ROUND"),
                mostVotedCard.getPlayer().getUser().getName(),
                mostVotedCard.getCard().getText(),
                telegramGame.getGame().getTable().getCurrentRoundNumber(),
                telegramGame.getGame().getTable().getCurrentBlackCard().getText(),
                playedCardsSB,
                playerPointsSB);
    }

    private String getPlayerVoteCardMessage(TelegramGame telegramGame, PlayedCard playedCard) {
        return MessageFormat.format(i18NService.get("PLAYER_VOTE_CARD"),
                telegramGame.getGame().getTable().getCurrentRoundNumber(),
                telegramGame.getGame().getTable().getCurrentBlackCard().getText(),
                playedCard.getCard().getText());
    }

    private String getPlayerVotedCardMessage(TelegramGame telegramGame, PlayedCard playedCard, VotedCard votedCard) {
        return MessageFormat.format(i18NService.get("PLAYER_VOTED_CARD"),
                telegramGame.getGame().getTable().getCurrentRoundNumber(),
                telegramGame.getGame().getTable().getCurrentBlackCard().getText(),
                playedCard.getCard().getText(),
                votedCard.getCard().getText());
    }

    public String getGameTypeName(GameTypeEnum gameTypeEnum) {
        if (gameTypeEnum == GameTypeEnum.DEMOCRACY) {
            return i18NService.get("GAME_MODE_DEMOCRACY");
        } else if (gameTypeEnum == GameTypeEnum.CLASSIC) {
            return i18NService.get("GAME_MODE_CLASSIC");
        } else if (gameTypeEnum == GameTypeEnum.DICTATORSHIP) {
            return i18NService.get("GAME_MODE_DICTATORSHIP");
        } else {
            return null;
        }
    }

    private String getGameEndGameMessage(Player winner) {
        return MessageFormat.format(i18NService.get("GAME_END_GAME"), winner.getUser().getName());
    }

    private String getHelpMessage() {
        return MessageFormat.format(i18NService.get("HELP"), getBotName(), getBotVersion(), getBotHelpURL(), getBotCreatorName());
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

    private Long getBotCreatorId() {
        return Long.parseLong(configurationService.getConfiguration("cclh_bot_owner_id"));
    }

    private String getBotCreatorName() {
        return configurationService.getConfiguration("cclh_bot_owner_alias");
    }

    private String getBotHelpURL() {
        return configurationService.getConfiguration("cclh_bot_help_url");
    }

    @Autowired
    public void setCCLHBotMessageService(CCLHBotMessageService cclhBotMessageService) {
        this.cclhBotMessageService = cclhBotMessageService;
    }

    @Autowired
    public void setCCLHBotService(CCLHBotService cclhBotService) {
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
    public void setI18NService(I18NService i18NService) {
        this.i18NService = i18NService;
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
