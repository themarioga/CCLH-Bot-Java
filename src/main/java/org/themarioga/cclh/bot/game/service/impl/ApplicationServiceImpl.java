package org.themarioga.cclh.bot.game.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.themarioga.cclh.bot.game.service.intf.CCLHGameService;
import org.themarioga.cclh.bot.service.intf.ApplicationService;
import org.themarioga.cclh.bot.service.intf.BotService;
import org.themarioga.cclh.bot.game.constants.ResponseErrorI18n;
import org.themarioga.cclh.bot.util.BotUtils;
import org.themarioga.cclh.bot.util.CallbackQueryHandler;
import org.themarioga.cclh.bot.util.CommandHandler;

import java.util.*;

@Service
public class ApplicationServiceImpl implements ApplicationService {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationServiceImpl.class);

    private BotService botService;
    private CCLHGameService cclhGameService;

    @Override
    public Map<String, CommandHandler> getBotCommands() {
        Map<String, CommandHandler> commands = new HashMap<>();

        commands.put("/start", message -> {
            if (!message.getChat().getType().equals("private")) {
                logger.error("Comando /start enviado en lugar incorrecto por {}", BotUtils.getUserInfo(message.getFrom()));

                botService.sendMessage(message.getChat().getId(), ResponseErrorI18n.COMMAND_SHOULD_BE_ON_PRIVATE);

                return;
            }

            try {
                cclhGameService.registerUser(message.getFrom().getId(), BotUtils.getUsername(message.getFrom()));
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        });

        commands.put("/create", message -> {
            if (message.getChat().getType().equals("private")) {
                logger.error("Comando /start enviado en lugar incorrecto por {}", BotUtils.getUserInfo(message.getFrom()));

                botService.sendMessage(message.getChat().getId(), ResponseErrorI18n.COMMAND_SHOULD_BE_ON_GROUP);

                return;
            }

            try {
                cclhGameService.startCreatingGame(message.getChat().getId(), message.getChat().getTitle(), message.getFrom().getId());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        });

        commands.put("/deleteMyGames", message -> {
            if (!message.getChat().getType().equals("private")) {
                logger.error("Comando /start enviado en lugar incorrecto por {}", BotUtils.getUserInfo(message.getFrom()));

                botService.sendMessage(message.getChat().getId(), ResponseErrorI18n.COMMAND_SHOULD_BE_ON_PRIVATE);

                return;
            }

            try {
                cclhGameService.deleteMyGames(message.getFrom().getId());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        });

        commands.put("/help", message -> cclhGameService.sendHelpMessage(message.getChatId()));

        return commands;
    }

    @Override
    public Map<String, CallbackQueryHandler> getCallbackQueries() {
        Map<String, CallbackQueryHandler> callbackQueryHandlerMap = new HashMap<>();

        callbackQueryHandlerMap.put("game_created", (callbackQuery, data) -> {
            try {
                cclhGameService.gameCreatedQuery(callbackQuery.getMessage().getChatId(), callbackQuery.getFrom().getId(),
                    callbackQuery.getId());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            botService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("game_configure", (callbackQuery, data) -> {
            try {
                cclhGameService.gameConfigureQuery(callbackQuery.getMessage().getChatId(), callbackQuery.getFrom().getId(),
                    callbackQuery.getId());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            botService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("game_sel_mode", (callbackQuery, data) -> {
            try {
                cclhGameService.gameSelectModeQuery(callbackQuery.getMessage().getChatId(), callbackQuery.getFrom().getId(),
                    callbackQuery.getId());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            botService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("game_sel_point_type", (callbackQuery, data) -> {
            try {
                cclhGameService.gameSelectPunctuationModeQuery(callbackQuery.getMessage().getChatId(), callbackQuery.getFrom().getId(),
                    callbackQuery.getId());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            botService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("game_sel_dictionary", (callbackQuery, data) -> {
            try {
                cclhGameService.gameSelectDictionaryQuery(callbackQuery.getMessage().getChatId(), callbackQuery.getFrom().getId(),
                    callbackQuery.getId(), data);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            botService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("game_sel_max_players", (callbackQuery, data) -> {
            try {
                cclhGameService.gameSelectMaxPlayersQuery(callbackQuery.getMessage().getChatId(), callbackQuery.getFrom().getId(),
                    callbackQuery.getId());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            botService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("game_sel_n_rounds", (callbackQuery, data) -> {
            try {
                cclhGameService.gameSelectNRoundsToEndQuery(callbackQuery.getMessage().getChatId(), callbackQuery.getFrom().getId(),
                    callbackQuery.getId());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            botService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("game_sel_n_points", (callbackQuery, data) -> {
            try {
                cclhGameService.gameSelectNPointsToWinQuery(callbackQuery.getMessage().getChatId(), callbackQuery.getFrom().getId(),
                    callbackQuery.getId());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            botService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("game_change_mode", (callbackQuery, data) -> {
            try {
                cclhGameService.gameChangeMode(callbackQuery.getMessage().getChatId(), callbackQuery.getFrom().getId(),
                    callbackQuery.getId(), data);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            botService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("game_change_dictionary", (callbackQuery, data) -> {
            try {
                cclhGameService.gameChangeDictionary(callbackQuery.getMessage().getChatId(), callbackQuery.getFrom().getId(),
                    callbackQuery.getId(), data);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            botService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("game_change_max_players", (callbackQuery, data) -> {
            try {
                cclhGameService.gameChangeMaxPlayers(callbackQuery.getMessage().getChatId(), callbackQuery.getFrom().getId(),
                    callbackQuery.getId(), data);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            botService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("game_change_max_rounds", (callbackQuery, data) -> {
            try {
                cclhGameService.gameChangeNRoundsToEnd(callbackQuery.getMessage().getChatId(), callbackQuery.getFrom().getId(),
                    callbackQuery.getId(), data);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            botService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("game_change_max_points", (callbackQuery, data) -> {
            try {
                cclhGameService.gameChangeNCardsToWin(callbackQuery.getMessage().getChatId(), callbackQuery.getFrom().getId(),
                    callbackQuery.getId(), data);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            botService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("game_join", (callbackQuery, data) -> {
            try {
                cclhGameService.gameJoinQuery(callbackQuery.getMessage().getChatId(), callbackQuery.getFrom().getId(),
                    callbackQuery.getId());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            botService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("game_leave", (callbackQuery, data) -> {
            try {
                cclhGameService.leaveGame(callbackQuery.getFrom().getId(), callbackQuery.getId());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            botService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("game_start", (callbackQuery, data) -> {
            try {
                cclhGameService.gameStartQuery(callbackQuery.getMessage().getChatId(), callbackQuery.getFrom().getId(),
                    callbackQuery.getId());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            botService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("play_card", (callbackQuery, data) -> {
            try {
                cclhGameService.playerPlayCardQuery(callbackQuery.getFrom().getId(), callbackQuery.getId(), data);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            botService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("vote_card", (callbackQuery, data) -> {
            try {
                cclhGameService.playerVoteCardQuery(callbackQuery.getFrom().getId(), callbackQuery.getId(), data);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            botService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("game_delete_group", (callbackQuery, data) -> {
            try {
                cclhGameService.gameDeleteGroupQuery(callbackQuery.getMessage().getChatId(), callbackQuery.getFrom().getId(),
                    callbackQuery.getId());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            botService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("game_delete_private", (callbackQuery, data) -> {
            try {
                cclhGameService.gameDeletePrivateQuery(callbackQuery.getFrom().getId(), callbackQuery.getId());
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
    public void setCCLHService(CCLHGameService cclhGameService) {
        this.cclhGameService = cclhGameService;
    }

}
