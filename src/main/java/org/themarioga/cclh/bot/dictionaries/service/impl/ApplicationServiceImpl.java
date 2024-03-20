package org.themarioga.cclh.bot.dictionaries.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.themarioga.bot.constants.BotConstants;
import org.themarioga.bot.model.CallbackQueryHandler;
import org.themarioga.bot.model.CommandHandler;
import org.themarioga.bot.service.intf.ApplicationService;
import org.themarioga.cclh.bot.dictionaries.service.intf.DictionariesBotMessageService;
import org.themarioga.bot.util.BotMessageUtils;
import org.themarioga.cclh.bot.dictionaries.service.intf.DictionariesBotService;
import org.themarioga.cclh.bot.game.constants.CCLHBotResponseErrorI18n;

import java.util.HashMap;
import java.util.Map;

@Service("dictionariesBotApplicationService")
public class ApplicationServiceImpl implements ApplicationService {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationServiceImpl.class);

    private DictionariesBotMessageService dictionariesBotMessageService;
    private DictionariesBotService dictionariesBotService;

    @Override
    public Map<String, CommandHandler> getBotCommands() {
        Map<String, CommandHandler> commands = new HashMap<>();

        commands.put("/start", (message, data) -> {
            if (!message.getChat().getType().equals(BotConstants.TELEGRAM_MESSAGE_TYPE_PRIVATE)) {
                logger.error("Comando /start enviado en lugar incorrecto por {}", BotMessageUtils.getUserInfo(message.getFrom()));

                dictionariesBotMessageService.sendMessage(message.getChat().getId(), CCLHBotResponseErrorI18n.COMMAND_SHOULD_BE_ON_PRIVATE);

                return;
            }

            try {
                dictionariesBotService.registerUser(message.getFrom().getId(), BotMessageUtils.getUsername(message.getFrom()));
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        });

        commands.put("/menu", (message, data) -> {
            if (!message.getChat().getType().equals(BotConstants.TELEGRAM_MESSAGE_TYPE_PRIVATE)) {
                logger.error("Comando /menu enviado en lugar incorrecto por {}", BotMessageUtils.getUserInfo(message.getFrom()));

                dictionariesBotMessageService.sendMessage(message.getChat().getId(), CCLHBotResponseErrorI18n.COMMAND_SHOULD_BE_ON_PRIVATE);

                return;
            }

            try {
                dictionariesBotService.mainMenu(message.getFrom().getId());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        });

        commands.put("/create", (message, data) -> {
            if (!message.getChat().getType().equals(BotConstants.TELEGRAM_MESSAGE_TYPE_PRIVATE)) {
                logger.error("Comando /create enviado en lugar incorrecto por {}", BotMessageUtils.getUserInfo(message.getFrom()));

                dictionariesBotMessageService.sendMessage(message.getChat().getId(), CCLHBotResponseErrorI18n.COMMAND_SHOULD_BE_ON_PRIVATE);

                return;
            }

            try {
                dictionariesBotService.createDictionary(message.getChatId(), message.getText());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        });

        commands.put("/rename_select", (message, data) -> {
            if (!message.getChat().getType().equals(BotConstants.TELEGRAM_MESSAGE_TYPE_PRIVATE)) {
                logger.error("Comando /rename_select enviado en lugar incorrecto por {}", BotMessageUtils.getUserInfo(message.getFrom()));

                dictionariesBotMessageService.sendMessage(message.getChat().getId(), CCLHBotResponseErrorI18n.COMMAND_SHOULD_BE_ON_PRIVATE);

                return;
            }

            try {
                dictionariesBotService.selectDictionaryToRename(message.getChatId(), Long.parseLong(message.getText()));
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        });

        commands.put("/rename", (message, data) -> {
            if (!message.getChat().getType().equals(BotConstants.TELEGRAM_MESSAGE_TYPE_PRIVATE)) {
                logger.error("Comando /rename enviado en lugar incorrecto por {}", BotMessageUtils.getUserInfo(message.getFrom()));

                dictionariesBotMessageService.sendMessage(message.getChat().getId(), CCLHBotResponseErrorI18n.COMMAND_SHOULD_BE_ON_PRIVATE);

                return;
            }

            try {
                dictionariesBotService.renameDictionary(message.getChatId(), Long.parseLong(data), message.getText());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        });

        commands.put("/delete_select", (message, data) -> {
            if (!message.getChat().getType().equals(BotConstants.TELEGRAM_MESSAGE_TYPE_PRIVATE)) {
                logger.error("Comando /rename_select enviado en lugar incorrecto por {}", BotMessageUtils.getUserInfo(message.getFrom()));

                dictionariesBotMessageService.sendMessage(message.getChat().getId(), CCLHBotResponseErrorI18n.COMMAND_SHOULD_BE_ON_PRIVATE);

                return;
            }

            try {
                dictionariesBotService.selectDictionaryToDelete(message.getChatId(), Long.parseLong(message.getText()));
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        });

        commands.put("/delete", (message, data) -> {
            if (!message.getChat().getType().equals(BotConstants.TELEGRAM_MESSAGE_TYPE_PRIVATE)) {
                logger.error("Comando /rename enviado en lugar incorrecto por {}", BotMessageUtils.getUserInfo(message.getFrom()));

                dictionariesBotMessageService.sendMessage(message.getChat().getId(), CCLHBotResponseErrorI18n.COMMAND_SHOULD_BE_ON_PRIVATE);

                return;
            }

            try {
                dictionariesBotService.deleteDictionary(message.getChatId(), Long.parseLong(data), message.getText());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        });

        commands.put("/manage_cards_select", (message, data) -> {
            if (!message.getChat().getType().equals(BotConstants.TELEGRAM_MESSAGE_TYPE_PRIVATE)) {
                logger.error("Comando /manage_cards_select enviado en lugar incorrecto por {}", BotMessageUtils.getUserInfo(message.getFrom()));

                dictionariesBotMessageService.sendMessage(message.getChat().getId(), CCLHBotResponseErrorI18n.COMMAND_SHOULD_BE_ON_PRIVATE);

                return;
            }

            try {
                dictionariesBotService.selectDictionaryToManageCards(message.getChatId(), Long.parseLong(message.getText()));
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        });

        commands.put("/help", (message, data) -> dictionariesBotService.sendHelpMessage(message.getChatId()));

        return commands;
    }

    @Override
    public Map<String, CallbackQueryHandler> getCallbackQueries() {
        Map<String, CallbackQueryHandler> callbackQueryHandlerMap = new HashMap<>();

        callbackQueryHandlerMap.put("dictionary_list", (callbackQuery, data) -> {
            try {
                dictionariesBotService.listDictionaries(callbackQuery.getFrom().getId());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            dictionariesBotMessageService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("dictionary_create", (callbackQuery, data) -> {
            try {
                dictionariesBotService.createDictionaryMessage(callbackQuery.getFrom().getId());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            dictionariesBotMessageService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("dictionary_rename", (callbackQuery, data) -> {
            try {
                dictionariesBotService.renameDictionaryMessage(callbackQuery.getFrom().getId());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            dictionariesBotMessageService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("dictionary_delete", (callbackQuery, data) -> {
            try {
                dictionariesBotService.deleteDictionaryMessage(callbackQuery.getFrom().getId());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            dictionariesBotMessageService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("dictionary_manage_cards", (callbackQuery, data) -> {
            try {
                dictionariesBotService.manageCardsMessage(callbackQuery.getFrom().getId());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            dictionariesBotMessageService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("add_white_cards", (callbackQuery, data) -> {
            try {
                logger.debug(data);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            dictionariesBotMessageService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("edit_white_card", (callbackQuery, data) -> {
            try {
                logger.debug(data);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            dictionariesBotMessageService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("delete_white_card", (callbackQuery, data) -> {
            try {
                logger.debug(data);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            dictionariesBotMessageService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("add_black_cards", (callbackQuery, data) -> {
            try {
                logger.debug(data);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            dictionariesBotMessageService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("edit_black_card", (callbackQuery, data) -> {
            try {
                logger.debug(data);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            dictionariesBotMessageService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("delete_black_card", (callbackQuery, data) -> {
            try {
                logger.debug(data);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            dictionariesBotMessageService.answerCallbackQuery(callbackQuery.getId());
        });

        return callbackQueryHandlerMap;
    }

    @Autowired
    public void setBotMessageService(DictionariesBotMessageService dictionariesBotMessageService) {
        this.dictionariesBotMessageService = dictionariesBotMessageService;
    }

    @Autowired
    public void setDictionariesBotService(DictionariesBotService dictionariesBotService) {
        this.dictionariesBotService = dictionariesBotService;
    }

}
