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
                dictionariesBotService.createDictionary(message.getChatId(), message.getText().trim());
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
                dictionariesBotService.selectDictionaryToRename(message.getChatId(), message.getMessageId(), Long.parseLong(message.getText().trim()));
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
                dictionariesBotService.renameDictionary(message.getChatId(), Long.parseLong(data), message.getText().trim());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        });

        commands.put("/delete_select", (message, data) -> {
            if (!message.getChat().getType().equals(BotConstants.TELEGRAM_MESSAGE_TYPE_PRIVATE)) {
                logger.error("Comando /delete_select enviado en lugar incorrecto por {}", BotMessageUtils.getUserInfo(message.getFrom()));

                dictionariesBotMessageService.sendMessage(message.getChat().getId(), CCLHBotResponseErrorI18n.COMMAND_SHOULD_BE_ON_PRIVATE);

                return;
            }

            try {
                dictionariesBotService.selectDictionaryToDelete(message.getChatId(), message.getMessageId(), Long.parseLong(message.getText().trim()));
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        });

        commands.put("/delete", (message, data) -> {
            if (!message.getChat().getType().equals(BotConstants.TELEGRAM_MESSAGE_TYPE_PRIVATE)) {
                logger.error("Comando /delete enviado en lugar incorrecto por {}", BotMessageUtils.getUserInfo(message.getFrom()));

                dictionariesBotMessageService.sendMessage(message.getChat().getId(), CCLHBotResponseErrorI18n.COMMAND_SHOULD_BE_ON_PRIVATE);

                return;
            }

            try {
                dictionariesBotService.deleteDictionary(message.getChatId(), Long.parseLong(data), message.getText().trim());
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
                dictionariesBotService.selectDictionaryToManageCards(message.getChatId(), null, Long.parseLong(message.getText().trim()));
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        });

        commands.put("/add_white_card", (message, data) -> {
            if (!message.getChat().getType().equals(BotConstants.TELEGRAM_MESSAGE_TYPE_PRIVATE)) {
                logger.error("Comando /add_white_card enviado en lugar incorrecto por {}", BotMessageUtils.getUserInfo(message.getFrom()));

                dictionariesBotMessageService.sendMessage(message.getChat().getId(), CCLHBotResponseErrorI18n.COMMAND_SHOULD_BE_ON_PRIVATE);

                return;
            }

            try {
                dictionariesBotService.addWhiteCard(message.getChatId(), Long.parseLong(data), message.getText().trim());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        });

        commands.put("/edit_white_card", (message, data) -> {
            if (!message.getChat().getType().equals(BotConstants.TELEGRAM_MESSAGE_TYPE_PRIVATE)) {
                logger.error("Comando /edit_white_card enviado en lugar incorrecto por {}", BotMessageUtils.getUserInfo(message.getFrom()));

                dictionariesBotMessageService.sendMessage(message.getChat().getId(), CCLHBotResponseErrorI18n.COMMAND_SHOULD_BE_ON_PRIVATE);

                return;
            }

            try {
                dictionariesBotService.editWhiteCard(message.getChatId(), Long.parseLong(data), Long.parseLong(message.getText().trim()));
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        });

        commands.put("/delete_white_card", (message, data) -> {
            if (!message.getChat().getType().equals(BotConstants.TELEGRAM_MESSAGE_TYPE_PRIVATE)) {
                logger.error("Comando /delete_white_card enviado en lugar incorrecto por {}", BotMessageUtils.getUserInfo(message.getFrom()));

                dictionariesBotMessageService.sendMessage(message.getChat().getId(), CCLHBotResponseErrorI18n.COMMAND_SHOULD_BE_ON_PRIVATE);

                return;
            }

            try {
                dictionariesBotService.deleteWhiteCard(message.getChatId(), Long.parseLong(data), Long.parseLong(message.getText().trim()));
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        });

        commands.put("/add_black_card", (message, data) -> {
            if (!message.getChat().getType().equals(BotConstants.TELEGRAM_MESSAGE_TYPE_PRIVATE)) {
                logger.error("Comando /add_black_card enviado en lugar incorrecto por {}", BotMessageUtils.getUserInfo(message.getFrom()));

                dictionariesBotMessageService.sendMessage(message.getChat().getId(), CCLHBotResponseErrorI18n.COMMAND_SHOULD_BE_ON_PRIVATE);

                return;
            }

            try {
                dictionariesBotService.addBlackCard(message.getChatId(), Long.parseLong(data), message.getText().trim());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        });

        commands.put("/edit_black_card", (message, data) -> {
            if (!message.getChat().getType().equals(BotConstants.TELEGRAM_MESSAGE_TYPE_PRIVATE)) {
                logger.error("Comando /edit_black_card enviado en lugar incorrecto por {}", BotMessageUtils.getUserInfo(message.getFrom()));

                dictionariesBotMessageService.sendMessage(message.getChat().getId(), CCLHBotResponseErrorI18n.COMMAND_SHOULD_BE_ON_PRIVATE);

                return;
            }

            try {
                dictionariesBotService.editBlackCard(message.getChatId(), Long.parseLong(data), Long.parseLong(message.getText().trim()));
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        });

        commands.put("/delete_black_card", (message, data) -> {
            if (!message.getChat().getType().equals(BotConstants.TELEGRAM_MESSAGE_TYPE_PRIVATE)) {
                logger.error("Comando /delete_black_card enviado en lugar incorrecto por {}", BotMessageUtils.getUserInfo(message.getFrom()));

                dictionariesBotMessageService.sendMessage(message.getChat().getId(), CCLHBotResponseErrorI18n.COMMAND_SHOULD_BE_ON_PRIVATE);

                return;
            }

            try {
                dictionariesBotService.deleteBlackCard(message.getChatId(), Long.parseLong(data), Long.parseLong(message.getText().trim()));
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        });

        commands.put("/manage_collabs_select", (message, data) -> {
            if (!message.getChat().getType().equals(BotConstants.TELEGRAM_MESSAGE_TYPE_PRIVATE)) {
                logger.error("Comando /manage_collabs_select enviado en lugar incorrecto por {}", BotMessageUtils.getUserInfo(message.getFrom()));

                dictionariesBotMessageService.sendMessage(message.getChat().getId(), CCLHBotResponseErrorI18n.COMMAND_SHOULD_BE_ON_PRIVATE);

                return;
            }

            try {
                dictionariesBotService.selectDictionaryToManageCollaborators(message.getChatId(), null, Long.parseLong(message.getText().trim()));
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        });

        commands.put("/add_collab", (message, data) -> {
            if (!message.getChat().getType().equals(BotConstants.TELEGRAM_MESSAGE_TYPE_PRIVATE)) {
                logger.error("Comando /add_white_card enviado en lugar incorrecto por {}", BotMessageUtils.getUserInfo(message.getFrom()));

                dictionariesBotMessageService.sendMessage(message.getChat().getId(), CCLHBotResponseErrorI18n.COMMAND_SHOULD_BE_ON_PRIVATE);

                return;
            }

            try {
                dictionariesBotService.addCollaborator(message.getChatId(), Long.parseLong(data), message.getText().trim());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        });

        commands.put("/delete_collab", (message, data) -> {
            if (!message.getChat().getType().equals(BotConstants.TELEGRAM_MESSAGE_TYPE_PRIVATE)) {
                logger.error("Comando /add_white_card enviado en lugar incorrecto por {}", BotMessageUtils.getUserInfo(message.getFrom()));

                dictionariesBotMessageService.sendMessage(message.getChat().getId(), CCLHBotResponseErrorI18n.COMMAND_SHOULD_BE_ON_PRIVATE);

                return;
            }

            try {
                dictionariesBotService.deleteCollaborator(message.getChatId(), Long.parseLong(data), message.getText().trim());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        });

        commands.put("/toggle_collab", (message, data) -> {
            if (!message.getChat().getType().equals(BotConstants.TELEGRAM_MESSAGE_TYPE_PRIVATE)) {
                logger.error("Comando /add_white_card enviado en lugar incorrecto por {}", BotMessageUtils.getUserInfo(message.getFrom()));

                dictionariesBotMessageService.sendMessage(message.getChat().getId(), CCLHBotResponseErrorI18n.COMMAND_SHOULD_BE_ON_PRIVATE);

                return;
            }

            try {
                dictionariesBotService.toggleCollaborator(message.getChatId(), Long.parseLong(data), message.getText().trim());
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

        callbackQueryHandlerMap.put("menu", (callbackQuery, data) -> {
            try {
                dictionariesBotService.mainMenu(callbackQuery.getFrom().getId(), callbackQuery.getMessage().getMessageId());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            dictionariesBotMessageService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("dictionary_list", (callbackQuery, data) -> {
            try {
                dictionariesBotService.listDictionaries(callbackQuery.getFrom().getId(), callbackQuery.getMessage().getMessageId());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            dictionariesBotMessageService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("dictionary_create", (callbackQuery, data) -> {
            try {
                dictionariesBotService.createDictionaryMessage(callbackQuery.getFrom().getId(), callbackQuery.getMessage().getMessageId());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            dictionariesBotMessageService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("dictionary_rename", (callbackQuery, data) -> {
            try {
                dictionariesBotService.renameDictionaryMessage(callbackQuery.getFrom().getId(), callbackQuery.getMessage().getMessageId());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            dictionariesBotMessageService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("dictionary_delete", (callbackQuery, data) -> {
            try {
                dictionariesBotService.deleteDictionaryMessage(callbackQuery.getFrom().getId(), callbackQuery.getMessage().getMessageId());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            dictionariesBotMessageService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("dictionary_manage_cards", (callbackQuery, data) -> {
            try {
                dictionariesBotService.manageCardsMessage(callbackQuery.getFrom().getId(), callbackQuery.getMessage().getMessageId());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            dictionariesBotMessageService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("manage_cards_select", (callbackQuery, data) -> {
            try {
                dictionariesBotService.selectDictionaryToManageCards(callbackQuery.getFrom().getId(), callbackQuery.getMessage().getMessageId(), Long.parseLong(data));
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            dictionariesBotMessageService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("list_white_cards", (callbackQuery, data) -> {
            try {
                dictionariesBotService.listWhiteCardsMessage(callbackQuery.getFrom().getId(), callbackQuery.getMessage().getMessageId(), Long.parseLong(data));
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            dictionariesBotMessageService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("add_white_cards", (callbackQuery, data) -> {
            try {
                dictionariesBotService.addWhiteCardsMessage(callbackQuery.getFrom().getId(), callbackQuery.getMessage().getMessageId(), Long.parseLong(data));
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            dictionariesBotMessageService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("edit_white_cards", (callbackQuery, data) -> {
            try {
                dictionariesBotService.editWhiteCardsMessage(callbackQuery.getFrom().getId(), callbackQuery.getMessage().getMessageId(), Long.parseLong(data));
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            dictionariesBotMessageService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("delete_white_cards", (callbackQuery, data) -> {
            try {
                dictionariesBotService.deleteWhiteCardsMessage(callbackQuery.getFrom().getId(), callbackQuery.getMessage().getMessageId(), Long.parseLong(data));
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            dictionariesBotMessageService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("list_black_cards", (callbackQuery, data) -> {
            try {
                dictionariesBotService.listBlackCardsMessage(callbackQuery.getFrom().getId(), callbackQuery.getMessage().getMessageId(), Long.parseLong(data));
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            dictionariesBotMessageService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("add_black_cards", (callbackQuery, data) -> {
            try {
                dictionariesBotService.addBlackCardsMessage(callbackQuery.getFrom().getId(), callbackQuery.getMessage().getMessageId(), Long.parseLong(data));
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            dictionariesBotMessageService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("edit_black_cards", (callbackQuery, data) -> {
            try {
                dictionariesBotService.editBlackCardsMessage(callbackQuery.getFrom().getId(), callbackQuery.getMessage().getMessageId(), Long.parseLong(data));
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            dictionariesBotMessageService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("delete_black_cards", (callbackQuery, data) -> {
            try {
                dictionariesBotService.deleteBlackCardsMessage(callbackQuery.getFrom().getId(), callbackQuery.getMessage().getMessageId(), Long.parseLong(data));
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            dictionariesBotMessageService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("dictionary_manage_collabs", (callbackQuery, data) -> {
            try {
                dictionariesBotService.manageCollaboratorsMessage(callbackQuery.getFrom().getId(), callbackQuery.getMessage().getMessageId());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            dictionariesBotMessageService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("manage_collabs_select", (callbackQuery, data) -> {
            try {
                dictionariesBotService.selectDictionaryToManageCollaborators(callbackQuery.getFrom().getId(), callbackQuery.getMessage().getMessageId(), Long.parseLong(data));
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            dictionariesBotMessageService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("list_collabs", (callbackQuery, data) -> {
            try {
                dictionariesBotService.listCollaboratorsMessage(callbackQuery.getFrom().getId(), callbackQuery.getMessage().getMessageId(), Long.parseLong(data));
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            dictionariesBotMessageService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("add_collabs", (callbackQuery, data) -> {
            try {
                dictionariesBotService.addCollaboratorsMessage(callbackQuery.getFrom().getId(), callbackQuery.getMessage().getMessageId(), Long.parseLong(data));
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            dictionariesBotMessageService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("delete_collabs", (callbackQuery, data) -> {
            try {
                dictionariesBotService.removeCollaboratorsMessage(callbackQuery.getFrom().getId(), callbackQuery.getMessage().getMessageId(), Long.parseLong(data));
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            dictionariesBotMessageService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("toggle_collabs", (callbackQuery, data) -> {
            try {
                dictionariesBotService.toggleCollaboratorsMessage(callbackQuery.getFrom().getId(), callbackQuery.getMessage().getMessageId(), Long.parseLong(data));
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            dictionariesBotMessageService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("collaborator_accept", (callbackQuery, data) -> {
            try {
                dictionariesBotService.acceptCollaborator(callbackQuery.getFrom().getId(), callbackQuery.getMessage().getMessageId(), Long.parseLong(data));
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            dictionariesBotMessageService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("collaborator_decline", (callbackQuery, data) -> {
            try {
                dictionariesBotService.rejectCollaborator(callbackQuery.getFrom().getId(), callbackQuery.getMessage().getMessageId(), Long.parseLong(data));
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
