package org.themarioga.cclh.bot.game.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.themarioga.bot.model.CallbackQueryHandler;
import org.themarioga.bot.model.CommandHandler;
import org.themarioga.bot.service.intf.ApplicationService;
import org.themarioga.bot.util.BotMessageUtils;
import org.themarioga.cclh.bot.game.service.intf.CCLHBotMessageService;
import org.themarioga.cclh.bot.game.service.intf.CCLHBotService;
import org.themarioga.cclh.bot.service.intf.I18NService;

import java.util.HashMap;
import java.util.Map;

@Service("cclhBotApplicationService")
public class ApplicationServiceImpl implements ApplicationService {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationServiceImpl.class);

    private CCLHBotMessageService cclhBotMessageService;
    private CCLHBotService cclhBotService;
    private I18NService i18NService;

    @Override
    public Map<String, CommandHandler> getBotCommands() {
        Map<String, CommandHandler> commands = new HashMap<>();

        commands.put("/start", (message, data) -> {
            if (!BotMessageUtils.isMessagePrivate(message)) {
                logger.error("Comando /start enviado en lugar incorrecto por {}",
                        BotMessageUtils.getUserInfo(message.getFrom()));

                cclhBotMessageService.sendMessage(message.getChat().getId(),
                        i18NService.get("COMMAND_SHOULD_BE_ON_PRIVATE", message.getFrom().getLanguageCode()));

                return;
            }

            try {
                cclhBotService.registerUser(message.getFrom().getId(), BotMessageUtils.getUsername(message.getFrom()),
                        message.getFrom().getLanguageCode());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        });

        commands.put("/lang", (message, data) -> {
            if (!BotMessageUtils.isMessagePrivate(message)) {
                logger.error("Comando /lang enviado en lugar incorrecto por {}",
                        BotMessageUtils.getUserInfo(message.getFrom()));

                cclhBotMessageService.sendMessage(message.getChat().getId(),
                        i18NService.get("COMMAND_SHOULD_BE_ON_PRIVATE", message.getFrom().getLanguageCode()));

                return;
            }

            try {
                cclhBotService.loginUser(message.getFrom().getId());

                cclhBotService.changeUserLanguageMessage();
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        });

        commands.put("/create", (message, data) -> {
            if (BotMessageUtils.isMessagePrivate(message)) {
                logger.error("Comando /create enviado en lugar incorrecto por {}",
                        BotMessageUtils.getUserInfo(message.getFrom()));

                cclhBotMessageService.sendMessage(message.getChat().getId(),
                        i18NService.get("COMMAND_SHOULD_BE_ON_GROUP", message.getFrom().getLanguageCode()));

                return;
            }

            try {
                cclhBotService.loginUser(message.getFrom().getId());

                cclhBotService.startCreatingGame(message.getChat().getId(), message.getChat().getTitle());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        });

        commands.put("/deletemygames", (message, data) -> {
            if (!BotMessageUtils.isMessagePrivate(message)) {
                logger.error("Comando /deletemygames enviado en lugar incorrecto por {}",
                        BotMessageUtils.getUserInfo(message.getFrom()));

                cclhBotMessageService.sendMessage(message.getChat().getId(),
                        i18NService.get("COMMAND_SHOULD_BE_ON_PRIVATE", message.getFrom().getLanguageCode()));

                return;
            }

            try {
                cclhBotService.loginUser(message.getFrom().getId());

                cclhBotService.deleteMyGames();
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        });

        commands.put("/deletegamebyusername", (message, data) -> {
            if (!BotMessageUtils.isMessagePrivate(message)) {
                logger.error("Comando /deletegamebyusername enviado en lugar incorrecto por {}",
                        BotMessageUtils.getUserInfo(message.getFrom()));

                cclhBotMessageService.sendMessage(message.getChat().getId(),
                        i18NService.get("COMMAND_SHOULD_BE_ON_PRIVATE", message.getFrom().getLanguageCode()));

                return;
            }

            try {
                cclhBotService.loginUser(message.getFrom().getId());

                cclhBotService.deleteGameByCreatorUsername(
                        cclhBotMessageService.sanitizeTextFromCommand("/deletegamebyusername", message.getText()));
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        });

        commands.put("/deleteallgames", (message, data) -> {
            if (!BotMessageUtils.isMessagePrivate(message)) {
                logger.error("Comando /deleteallgames enviado en lugar incorrecto por {}",
                        BotMessageUtils.getUserInfo(message.getFrom()));

                cclhBotMessageService.sendMessage(message.getChat().getId(),
                        i18NService.get("COMMAND_SHOULD_BE_ON_PRIVATE", message.getFrom().getLanguageCode()));

                return;
            }

            try {
                cclhBotService.loginUser(message.getFrom().getId());

                cclhBotService.deleteAllGames();
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        });

        commands.put("/sendmessagetoeveryone", (message, data) -> {
            if (!BotMessageUtils.isMessagePrivate(message)) {
                logger.error("Comando /sendMessage enviado en lugar incorrecto por {}",
                        BotMessageUtils.getUserInfo(message.getFrom()));

                cclhBotMessageService.sendMessage(message.getChat().getId(),
                        i18NService.get("COMMAND_SHOULD_BE_ON_PRIVATE", message.getFrom().getLanguageCode()));

                return;
            }

            try {
                cclhBotService.loginUser(message.getFrom().getId());

                cclhBotService.sendMessageToEveryone(
                        cclhBotMessageService.sanitizeTextFromCommand("/sendmessagetoeveryone", message.getText()));

                cclhBotMessageService.sendMessage(message.getChatId(), i18NService.get("ALL_MESSAGES_SENT"));
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        });

        commands.put("/toggleglobalmessages", (message, data) -> {
            if (!BotMessageUtils.isMessagePrivate(message)) {
                logger.error("Comando /sendMessage enviado en lugar incorrecto por {}",
                        BotMessageUtils.getUserInfo(message.getFrom()));

                cclhBotMessageService.sendMessage(message.getChat().getId(),
                        i18NService.get("COMMAND_SHOULD_BE_ON_PRIVATE", message.getFrom().getLanguageCode()));

                return;
            }

            try {
                cclhBotService.loginUser(message.getFrom().getId());

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

        callbackQueryHandlerMap.put("change_user_lang", (callbackQuery, data) -> {
            try {
                cclhBotService.loginUser(callbackQuery.getFrom().getId());

                cclhBotService.changeUserLanguage(callbackQuery.getMessage().getMessageId(),
                        data != null && !data.isBlank() ? data : callbackQuery.getFrom().getLanguageCode());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            cclhBotMessageService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("game_menu", (callbackQuery, data) -> {
            try {
                cclhBotService.loginUser(callbackQuery.getFrom().getId());

                cclhBotService.gameMenuQuery(callbackQuery.getMessage().getChatId(),
                    callbackQuery.getId());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            cclhBotMessageService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("game_configure", (callbackQuery, data) -> {
            try {
                cclhBotService.loginUser(callbackQuery.getFrom().getId());

                cclhBotService.gameConfigureQuery(callbackQuery.getMessage().getChatId(),
                    callbackQuery.getId());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            cclhBotMessageService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("game_sel_mode", (callbackQuery, data) -> {
            try {
                cclhBotService.loginUser(callbackQuery.getFrom().getId());

                cclhBotService.gameSelectModeQuery(callbackQuery.getMessage().getChatId(),
                    callbackQuery.getId());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            cclhBotMessageService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("game_sel_point_type", (callbackQuery, data) -> {
            try {
                cclhBotService.loginUser(callbackQuery.getFrom().getId());

                cclhBotService.gameSelectPunctuationModeQuery(callbackQuery.getMessage().getChatId(),
                    callbackQuery.getId());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            cclhBotMessageService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("game_sel_dictionary", (callbackQuery, data) -> {
            try {
                cclhBotService.loginUser(callbackQuery.getFrom().getId());

                cclhBotService.gameSelectDictionaryQuery(callbackQuery.getMessage().getChatId(),
                    callbackQuery.getId(), data);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            cclhBotMessageService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("game_sel_max_players", (callbackQuery, data) -> {
            try {
                cclhBotService.loginUser(callbackQuery.getFrom().getId());

                cclhBotService.gameSelectMaxPlayersQuery(callbackQuery.getMessage().getChatId(),
                    callbackQuery.getId());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            cclhBotMessageService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("game_sel_n_rounds", (callbackQuery, data) -> {
            try {
                cclhBotService.loginUser(callbackQuery.getFrom().getId());

                cclhBotService.gameSelectNRoundsToEndQuery(callbackQuery.getMessage().getChatId(),
                    callbackQuery.getId());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            cclhBotMessageService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("game_sel_n_points", (callbackQuery, data) -> {
            try {
                cclhBotService.loginUser(callbackQuery.getFrom().getId());

                cclhBotService.gameSelectNPointsToWinQuery(callbackQuery.getMessage().getChatId(),
                    callbackQuery.getId());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            cclhBotMessageService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("game_change_mode", (callbackQuery, data) -> {
            try {
                cclhBotService.loginUser(callbackQuery.getFrom().getId());

                cclhBotService.gameChangeMode(callbackQuery.getMessage().getChatId(),
                    callbackQuery.getId(), data);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            cclhBotMessageService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("game_change_dictionary", (callbackQuery, data) -> {
            try {
                cclhBotService.loginUser(callbackQuery.getFrom().getId());

                cclhBotService.gameChangeDictionary(callbackQuery.getMessage().getChatId(),
                    callbackQuery.getId(), data);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            cclhBotMessageService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("game_change_max_players", (callbackQuery, data) -> {
            try {
                cclhBotService.loginUser(callbackQuery.getFrom().getId());

                cclhBotService.gameChangeMaxPlayers(callbackQuery.getMessage().getChatId(),
                    callbackQuery.getId(), data);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            cclhBotMessageService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("game_change_max_rounds", (callbackQuery, data) -> {
            try {
                cclhBotService.loginUser(callbackQuery.getFrom().getId());

                cclhBotService.gameChangeNRoundsToEnd(callbackQuery.getMessage().getChatId(),
                    callbackQuery.getId(), data);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            cclhBotMessageService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("game_change_max_points", (callbackQuery, data) -> {
            try {
                cclhBotService.loginUser(callbackQuery.getFrom().getId());

                cclhBotService.gameChangeNCardsToWin(callbackQuery.getMessage().getChatId(),
                    callbackQuery.getId(), data);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            cclhBotMessageService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("game_join", (callbackQuery, data) -> {
            try {
                cclhBotService.loginUser(callbackQuery.getFrom().getId());

                cclhBotService.gameJoinQuery(callbackQuery.getMessage().getChatId(),
                    callbackQuery.getId());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            cclhBotMessageService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("game_leave", (callbackQuery, data) -> {
            try {
                cclhBotService.loginUser(callbackQuery.getFrom().getId());

                cclhBotService.leaveGame(callbackQuery.getId());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            cclhBotMessageService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("game_start", (callbackQuery, data) -> {
            try {
                cclhBotService.loginUser(callbackQuery.getFrom().getId());

                cclhBotService.gameStartQuery(callbackQuery.getMessage().getChatId(),
                    callbackQuery.getId());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            cclhBotMessageService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("play_card", (callbackQuery, data) -> {
            try {
                cclhBotService.loginUser(callbackQuery.getFrom().getId());

                cclhBotService.playerPlayCardQuery(callbackQuery.getId(), data);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            cclhBotMessageService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("vote_card", (callbackQuery, data) -> {
            try {
                cclhBotService.loginUser(callbackQuery.getFrom().getId());

                cclhBotService.playerVoteCardQuery(callbackQuery.getId(), data);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            cclhBotMessageService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("game_delete_group", (callbackQuery, data) -> {
            try {
                cclhBotService.loginUser(callbackQuery.getFrom().getId());

                cclhBotService.gameDeleteGroupQuery(callbackQuery.getMessage().getChatId(),
                    callbackQuery.getId());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            cclhBotMessageService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("game_delete_private", (callbackQuery, data) -> {
            try {
                cclhBotService.loginUser(callbackQuery.getFrom().getId());

                cclhBotService.gameDeletePrivateQuery(callbackQuery.getId());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            cclhBotMessageService.answerCallbackQuery(callbackQuery.getId());
        });

        return callbackQueryHandlerMap;
    }

    @Autowired
    public void setBotService(CCLHBotMessageService cclhBotMessageService) {
        this.cclhBotMessageService = cclhBotMessageService;
    }

    @Autowired
    public void setCCLHService(CCLHBotService cclhBotService) {
        this.cclhBotService = cclhBotService;
    }

    @Autowired
    public void setI18NService(I18NService i18NService) {
        this.i18NService = i18NService;
    }
}
