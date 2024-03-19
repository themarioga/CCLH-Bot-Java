package org.themarioga.cclh.bot.game.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.themarioga.bot.constants.BotConstants;
import org.themarioga.bot.service.intf.ApplicationService;
import org.themarioga.bot.service.intf.BotService;
import org.themarioga.bot.util.BotMessageUtils;
import org.themarioga.bot.model.CallbackQueryHandler;
import org.themarioga.bot.model.CommandHandler;
import org.themarioga.cclh.bot.game.constants.CCLHBotResponseErrorI18n;
import org.themarioga.cclh.bot.game.constants.CCLHBotResponseMessageI18n;
import org.themarioga.cclh.bot.game.service.intf.CCLHBotService;

import java.util.*;

@Service("cclhBotApplicationService")
public class ApplicationServiceImpl implements ApplicationService {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationServiceImpl.class);

    private BotService botService;
    private CCLHBotService cclhBotService;

    @Override
    public Map<String, CommandHandler> getBotCommands() {
        Map<String, CommandHandler> commands = new HashMap<>();

        commands.put("/start", (message, data) -> {
            if (!message.getChat().getType().equals(BotConstants.TELEGRAM_MESSAGE_TYPE_PRIVATE)) {
                logger.error("Comando /start enviado en lugar incorrecto por {}", BotMessageUtils.getUserInfo(message.getFrom()));

                botService.sendMessage(message.getChat().getId(), CCLHBotResponseErrorI18n.COMMAND_SHOULD_BE_ON_PRIVATE);

                return;
            }

            try {
                cclhBotService.registerUser(message.getFrom().getId(), BotMessageUtils.getUsername(message.getFrom()));
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        });

        commands.put("/create", (message, data) -> {
            if (message.getChat().getType().equals(BotConstants.TELEGRAM_MESSAGE_TYPE_PRIVATE)) {
                logger.error("Comando /create enviado en lugar incorrecto por {}", BotMessageUtils.getUserInfo(message.getFrom()));

                botService.sendMessage(message.getChat().getId(), CCLHBotResponseErrorI18n.COMMAND_SHOULD_BE_ON_GROUP);

                return;
            }

            try {
                cclhBotService.startCreatingGame(message.getChat().getId(), message.getChat().getTitle(), message.getFrom().getId());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        });

        commands.put("/deletemygames", (message, data) -> {
            if (!message.getChat().getType().equals(BotConstants.TELEGRAM_MESSAGE_TYPE_PRIVATE)) {
                logger.error("Comando /deletemygames enviado en lugar incorrecto por {}", BotMessageUtils.getUserInfo(message.getFrom()));

                botService.sendMessage(message.getChat().getId(), CCLHBotResponseErrorI18n.COMMAND_SHOULD_BE_ON_PRIVATE);

                return;
            }

            try {
                cclhBotService.deleteMyGames(message.getFrom().getId());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        });

        commands.put("/deletegamebyusername", (message, data) -> {
            if (!message.getChat().getType().equals(BotConstants.TELEGRAM_MESSAGE_TYPE_PRIVATE)
                    || !message.getChat().getId().equals(cclhBotService.getBotCreatorId())
                    || data == null) {
                logger.error("Comando /deletegamebyusername enviado en lugar incorrecto por {}", BotMessageUtils.getUserInfo(message.getFrom()));

                botService.sendMessage(message.getChat().getId(), CCLHBotResponseErrorI18n.COMMAND_SHOULD_BE_ON_PRIVATE);

                return;
            }

            try {
                cclhBotService.deleteGameByCreatorUsername(data);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        });

        commands.put("/deleteallgames", (message, data) -> {
            if (!message.getChat().getType().equals(BotConstants.TELEGRAM_MESSAGE_TYPE_PRIVATE)
                    || !message.getChat().getId().equals(cclhBotService.getBotCreatorId())) {
                logger.error("Comando /deleteallgames enviado en lugar incorrecto por {}", BotMessageUtils.getUserInfo(message.getFrom()));

                botService.sendMessage(message.getChat().getId(), CCLHBotResponseErrorI18n.COMMAND_SHOULD_BE_ON_PRIVATE);

                return;
            }

            try {
                cclhBotService.deleteAllGames();
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        });

        commands.put("/sendmessagetoeveryone", (message, data) -> {
            if (!message.getChat().getType().equals(BotConstants.TELEGRAM_MESSAGE_TYPE_PRIVATE)
                    || !message.getChat().getId().equals(cclhBotService.getBotCreatorId())
                    || data == null) {
                logger.error("Comando /sendMessage enviado en lugar incorrecto por {}", BotMessageUtils.getUserInfo(message.getFrom()));

                botService.sendMessage(message.getChat().getId(), CCLHBotResponseErrorI18n.COMMAND_SHOULD_BE_ON_PRIVATE);

                return;
            }

            try {
                cclhBotService.sendMessageToEveryone(data);

                botService.sendMessage(message.getChatId(), CCLHBotResponseMessageI18n.ALL_MESSAGES_SENT);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        });

        commands.put("/toggleglobalmessages", (message, data) -> {
            if (!message.getChat().getType().equals(BotConstants.TELEGRAM_MESSAGE_TYPE_PRIVATE)
                    || !message.getChat().getId().equals(cclhBotService.getBotCreatorId())) {
                logger.error("Comando /sendMessage enviado en lugar incorrecto por {}", BotMessageUtils.getUserInfo(message.getFrom()));

                botService.sendMessage(message.getChat().getId(), CCLHBotResponseErrorI18n.COMMAND_SHOULD_BE_ON_PRIVATE);

                return;
            }

            try {
                cclhBotService.toggleGlobalMessages();
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        });

        commands.put("/help", (message, data) -> cclhBotService.sendHelpMessage(message.getChatId()));

        return commands;
    }

    @Override
    public Map<String, CallbackQueryHandler> getCallbackQueries() {
        Map<String, CallbackQueryHandler> callbackQueryHandlerMap = new HashMap<>();

        callbackQueryHandlerMap.put("game_created", (callbackQuery, data) -> {
            try {
                cclhBotService.gameCreatedQuery(callbackQuery.getMessage().getChatId(), callbackQuery.getFrom().getId(),
                    callbackQuery.getId());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            botService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("game_configure", (callbackQuery, data) -> {
            try {
                cclhBotService.gameConfigureQuery(callbackQuery.getMessage().getChatId(), callbackQuery.getFrom().getId(),
                    callbackQuery.getId());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            botService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("game_sel_mode", (callbackQuery, data) -> {
            try {
                cclhBotService.gameSelectModeQuery(callbackQuery.getMessage().getChatId(), callbackQuery.getFrom().getId(),
                    callbackQuery.getId());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            botService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("game_sel_point_type", (callbackQuery, data) -> {
            try {
                cclhBotService.gameSelectPunctuationModeQuery(callbackQuery.getMessage().getChatId(), callbackQuery.getFrom().getId(),
                    callbackQuery.getId());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            botService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("game_sel_dictionary", (callbackQuery, data) -> {
            try {
                cclhBotService.gameSelectDictionaryQuery(callbackQuery.getMessage().getChatId(), callbackQuery.getFrom().getId(),
                    callbackQuery.getId(), data);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            botService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("game_sel_max_players", (callbackQuery, data) -> {
            try {
                cclhBotService.gameSelectMaxPlayersQuery(callbackQuery.getMessage().getChatId(), callbackQuery.getFrom().getId(),
                    callbackQuery.getId());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            botService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("game_sel_n_rounds", (callbackQuery, data) -> {
            try {
                cclhBotService.gameSelectNRoundsToEndQuery(callbackQuery.getMessage().getChatId(), callbackQuery.getFrom().getId(),
                    callbackQuery.getId());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            botService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("game_sel_n_points", (callbackQuery, data) -> {
            try {
                cclhBotService.gameSelectNPointsToWinQuery(callbackQuery.getMessage().getChatId(), callbackQuery.getFrom().getId(),
                    callbackQuery.getId());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            botService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("game_change_mode", (callbackQuery, data) -> {
            try {
                cclhBotService.gameChangeMode(callbackQuery.getMessage().getChatId(), callbackQuery.getFrom().getId(),
                    callbackQuery.getId(), data);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            botService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("game_change_dictionary", (callbackQuery, data) -> {
            try {
                cclhBotService.gameChangeDictionary(callbackQuery.getMessage().getChatId(), callbackQuery.getFrom().getId(),
                    callbackQuery.getId(), data);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            botService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("game_change_max_players", (callbackQuery, data) -> {
            try {
                cclhBotService.gameChangeMaxPlayers(callbackQuery.getMessage().getChatId(), callbackQuery.getFrom().getId(),
                    callbackQuery.getId(), data);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            botService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("game_change_max_rounds", (callbackQuery, data) -> {
            try {
                cclhBotService.gameChangeNRoundsToEnd(callbackQuery.getMessage().getChatId(), callbackQuery.getFrom().getId(),
                    callbackQuery.getId(), data);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            botService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("game_change_max_points", (callbackQuery, data) -> {
            try {
                cclhBotService.gameChangeNCardsToWin(callbackQuery.getMessage().getChatId(), callbackQuery.getFrom().getId(),
                    callbackQuery.getId(), data);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            botService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("game_join", (callbackQuery, data) -> {
            try {
                cclhBotService.gameJoinQuery(callbackQuery.getMessage().getChatId(), callbackQuery.getFrom().getId(),
                    callbackQuery.getId());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            botService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("game_leave", (callbackQuery, data) -> {
            try {
                cclhBotService.leaveGame(callbackQuery.getFrom().getId(), callbackQuery.getId());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            botService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("game_start", (callbackQuery, data) -> {
            try {
                cclhBotService.gameStartQuery(callbackQuery.getMessage().getChatId(), callbackQuery.getFrom().getId(),
                    callbackQuery.getId());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            botService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("play_card", (callbackQuery, data) -> {
            try {
                cclhBotService.playerPlayCardQuery(callbackQuery.getFrom().getId(), callbackQuery.getId(), data);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            botService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("vote_card", (callbackQuery, data) -> {
            try {
                cclhBotService.playerVoteCardQuery(callbackQuery.getFrom().getId(), callbackQuery.getId(), data);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            botService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("game_delete_group", (callbackQuery, data) -> {
            try {
                cclhBotService.gameDeleteGroupQuery(callbackQuery.getMessage().getChatId(), callbackQuery.getFrom().getId(),
                    callbackQuery.getId());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            botService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("game_delete_private", (callbackQuery, data) -> {
            try {
                cclhBotService.gameDeletePrivateQuery(callbackQuery.getFrom().getId(), callbackQuery.getId());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            botService.answerCallbackQuery(callbackQuery.getId());
        });

        return callbackQueryHandlerMap;
    }

    @Autowired
    public void setBotService(BotService botService) {
        this.botService = botService;
    }

    @Autowired
    public void setCCLHService(CCLHBotService cclhBotService) {
        this.cclhBotService = cclhBotService;
    }

}
