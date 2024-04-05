package org.themarioga.cclh.bot.dictionaries.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.themarioga.bot.model.CallbackQueryHandler;
import org.themarioga.bot.model.CommandHandler;
import org.themarioga.bot.service.intf.ApplicationService;
import org.themarioga.cclh.bot.dictionaries.service.intf.DictionariesBotMessageService;
import org.themarioga.bot.util.BotMessageUtils;
import org.themarioga.cclh.bot.dictionaries.service.intf.DictionariesBotService;
import org.themarioga.cclh.bot.service.intf.I18NService;

import java.util.HashMap;
import java.util.Map;

@Service("dictionariesBotApplicationService")
public class ApplicationServiceImpl implements ApplicationService {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationServiceImpl.class);

    private DictionariesBotMessageService dictionariesBotMessageService;
    private DictionariesBotService dictionariesBotService;
    private I18NService i18NService;

    @Override
    public Map<String, CommandHandler> getBotCommands() {
        Map<String, CommandHandler> commands = new HashMap<>();

        commands.put("/start", (message, data) -> {
            if (!BotMessageUtils.isMessagePrivate(message)) {
                logger.error("Comando /start enviado en lugar incorrecto por {}", BotMessageUtils.getUserInfo(message.getFrom()));

                dictionariesBotMessageService.sendMessage(message.getChat().getId(), i18NService.get("COMMAND_SHOULD_BE_ON_PRIVATE", message.getFrom().getLanguageCode()));

                return;
            }

            try {
                dictionariesBotService.registerUser(
                        message.getFrom().getId(),
                        BotMessageUtils.getUsername(message.getFrom()),
                        message.getFrom().getLanguageCode());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        });

        commands.put("/lang", (message, data) -> {
            if (!BotMessageUtils.isMessagePrivate(message)) {
                logger.error("Comando /lang enviado en lugar incorrecto por {}",
                        BotMessageUtils.getUserInfo(message.getFrom()));

                dictionariesBotMessageService.sendMessage(message.getChat().getId(),
                        i18NService.get("COMMAND_SHOULD_BE_ON_PRIVATE", message.getFrom().getLanguageCode()));

                return;
            }

            try {
                dictionariesBotService.loginUser(message.getFrom().getId());

                dictionariesBotService.changeUserLanguageMessage();
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        });

        commands.put("/menu", (message, data) -> {
            if (!BotMessageUtils.isMessagePrivate(message)) {
                logger.error("Comando /menu enviado en lugar incorrecto por {}", BotMessageUtils.getUserInfo(message.getFrom()));

                dictionariesBotMessageService.sendMessage(message.getChat().getId(), i18NService.get("COMMAND_SHOULD_BE_ON_PRIVATE", message.getFrom().getLanguageCode()));

                return;
            }

            try {
                dictionariesBotService.loginUser(message.getFrom().getId());

                dictionariesBotService.mainMenu();
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        });

        commands.put("/create", (message, data) -> {
            if (!BotMessageUtils.isMessagePrivate(message)) {
                logger.error("Comando /create enviado en lugar incorrecto por {}", BotMessageUtils.getUserInfo(message.getFrom()));

                dictionariesBotMessageService.sendMessage(message.getChat().getId(), i18NService.get("COMMAND_SHOULD_BE_ON_PRIVATE", message.getFrom().getLanguageCode()));

                return;
            }

            try {
                dictionariesBotService.loginUser(message.getFrom().getId());

                dictionariesBotService.createDictionary(message.getText().trim());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        });

        commands.put("/rename_select", (message, data) -> {
            if (!BotMessageUtils.isMessagePrivate(message)) {
                logger.error("Comando /rename_select enviado en lugar incorrecto por {}", BotMessageUtils.getUserInfo(message.getFrom()));

                dictionariesBotMessageService.sendMessage(message.getChat().getId(), i18NService.get("COMMAND_SHOULD_BE_ON_PRIVATE", message.getFrom().getLanguageCode()));

                return;
            }

            try {
                dictionariesBotService.loginUser(message.getFrom().getId());

                dictionariesBotService.selectDictionaryToRename(message.getMessageId(), Long.parseLong(message.getText().trim()));
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        });

        commands.put("/rename", (message, data) -> {
            if (!BotMessageUtils.isMessagePrivate(message)) {
                logger.error("Comando /rename enviado en lugar incorrecto por {}", BotMessageUtils.getUserInfo(message.getFrom()));

                dictionariesBotMessageService.sendMessage(message.getChat().getId(), i18NService.get("COMMAND_SHOULD_BE_ON_PRIVATE", message.getFrom().getLanguageCode()));

                return;
            }

            try {
                dictionariesBotService.loginUser(message.getFrom().getId());

                dictionariesBotService.renameDictionary(Long.parseLong(data), message.getText().trim());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        });

        commands.put("/change_lang_select", (message, data) -> {
            if (!BotMessageUtils.isMessagePrivate(message)) {
                logger.error("Comando /change_lang_select enviado en lugar incorrecto por {}", BotMessageUtils.getUserInfo(message.getFrom()));

                dictionariesBotMessageService.sendMessage(message.getChat().getId(), i18NService.get("COMMAND_SHOULD_BE_ON_PRIVATE", message.getFrom().getLanguageCode()));

                return;
            }

            try {
                dictionariesBotService.loginUser(message.getFrom().getId());

                dictionariesBotService.selectDictionaryToChangeLang(message.getMessageId(), Long.parseLong(message.getText().trim()));
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        });

        commands.put("/change_lang", (message, data) -> {
            if (!BotMessageUtils.isMessagePrivate(message)) {
                logger.error("Comando /change_lang enviado en lugar incorrecto por {}", BotMessageUtils.getUserInfo(message.getFrom()));

                dictionariesBotMessageService.sendMessage(message.getChat().getId(), i18NService.get("COMMAND_SHOULD_BE_ON_PRIVATE", message.getFrom().getLanguageCode()));

                return;
            }

            try {
                dictionariesBotService.loginUser(message.getFrom().getId());

                dictionariesBotService.changeDictionaryLang(Long.parseLong(data), message.getText().trim());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        });

        commands.put("/delete_select", (message, data) -> {
            if (!BotMessageUtils.isMessagePrivate(message)) {
                logger.error("Comando /delete_select enviado en lugar incorrecto por {}", BotMessageUtils.getUserInfo(message.getFrom()));

                dictionariesBotMessageService.sendMessage(message.getChat().getId(), i18NService.get("COMMAND_SHOULD_BE_ON_PRIVATE", message.getFrom().getLanguageCode()));

                return;
            }

            try {
                dictionariesBotService.loginUser(message.getFrom().getId());

                dictionariesBotService.selectDictionaryToDelete(message.getMessageId(), Long.parseLong(message.getText().trim()));
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        });

        commands.put("/delete", (message, data) -> {
            if (!BotMessageUtils.isMessagePrivate(message)) {
                logger.error("Comando /delete enviado en lugar incorrecto por {}", BotMessageUtils.getUserInfo(message.getFrom()));

                dictionariesBotMessageService.sendMessage(message.getChat().getId(), i18NService.get("COMMAND_SHOULD_BE_ON_PRIVATE", message.getFrom().getLanguageCode()));

                return;
            }

            try {
                dictionariesBotService.loginUser(message.getFrom().getId());

                dictionariesBotService.deleteDictionary(Long.parseLong(data), message.getText().trim());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        });

        commands.put("/toggle_select", (message, data) -> {
            if (!BotMessageUtils.isMessagePrivate(message)) {
                logger.error("Comando /delete_select enviado en lugar incorrecto por {}", BotMessageUtils.getUserInfo(message.getFrom()));

                dictionariesBotMessageService.sendMessage(message.getChat().getId(), i18NService.get("COMMAND_SHOULD_BE_ON_PRIVATE", message.getFrom().getLanguageCode()));

                return;
            }

            try {
                dictionariesBotService.loginUser(message.getFrom().getId());

                dictionariesBotService.toggleDictionary(message.getMessageId(), Long.parseLong(message.getText().trim()));
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        });

        commands.put("/share_select", (message, data) -> {
            if (!BotMessageUtils.isMessagePrivate(message)) {
                logger.error("Comando /delete_select enviado en lugar incorrecto por {}", BotMessageUtils.getUserInfo(message.getFrom()));

                dictionariesBotMessageService.sendMessage(message.getChat().getId(), i18NService.get("COMMAND_SHOULD_BE_ON_PRIVATE", message.getFrom().getLanguageCode()));

                return;
            }

            try {
                dictionariesBotService.loginUser(message.getFrom().getId());

                dictionariesBotService.requestShareDictionary(message.getMessageId(), Long.parseLong(message.getText().trim()));
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        });

        commands.put("/manage_cards_select", (message, data) -> {
            if (!BotMessageUtils.isMessagePrivate(message)) {
                logger.error("Comando /manage_cards_select enviado en lugar incorrecto por {}", BotMessageUtils.getUserInfo(message.getFrom()));

                dictionariesBotMessageService.sendMessage(message.getChat().getId(), i18NService.get("COMMAND_SHOULD_BE_ON_PRIVATE", message.getFrom().getLanguageCode()));

                return;
            }

            try {
                dictionariesBotService.loginUser(message.getFrom().getId());

                dictionariesBotService.selectDictionaryToManageCards(null, Long.parseLong(message.getText().trim()));
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        });

        commands.put("/add_white_card", (message, data) -> {
            if (!BotMessageUtils.isMessagePrivate(message)) {
                logger.error("Comando /add_white_card enviado en lugar incorrecto por {}", BotMessageUtils.getUserInfo(message.getFrom()));

                dictionariesBotMessageService.sendMessage(message.getChat().getId(), i18NService.get("COMMAND_SHOULD_BE_ON_PRIVATE", message.getFrom().getLanguageCode()));

                return;
            }

            try {
                dictionariesBotService.loginUser(message.getFrom().getId());

                dictionariesBotService.addWhiteCard(Long.parseLong(data), message.getText().trim());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        });

        commands.put("/edit_white_card_sel", (message, data) -> {
            if (!BotMessageUtils.isMessagePrivate(message)) {
                logger.error("Comando /edit_white_card_sel enviado en lugar incorrecto por {}", BotMessageUtils.getUserInfo(message.getFrom()));

                dictionariesBotMessageService.sendMessage(message.getChat().getId(), i18NService.get("COMMAND_SHOULD_BE_ON_PRIVATE", message.getFrom().getLanguageCode()));

                return;
            }

            try {
                dictionariesBotService.loginUser(message.getFrom().getId());

                dictionariesBotService.editWhiteCardSelect(Long.parseLong(data), Long.parseLong(message.getText().trim()));
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        });

        commands.put("/edit_white_card", (message, data) -> {
            if (!BotMessageUtils.isMessagePrivate(message)) {
                logger.error("Comando /edit_white_card enviado en lugar incorrecto por {}", BotMessageUtils.getUserInfo(message.getFrom()));

                dictionariesBotMessageService.sendMessage(message.getChat().getId(), i18NService.get("COMMAND_SHOULD_BE_ON_PRIVATE", message.getFrom().getLanguageCode()));

                return;
            }

            try {
                dictionariesBotService.loginUser(message.getFrom().getId());

                dictionariesBotService.editWhiteCard(Long.parseLong(data), message.getText().trim());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        });

        commands.put("/delete_white_card", (message, data) -> {
            if (!BotMessageUtils.isMessagePrivate(message)) {
                logger.error("Comando /delete_white_card enviado en lugar incorrecto por {}", BotMessageUtils.getUserInfo(message.getFrom()));

                dictionariesBotMessageService.sendMessage(message.getChat().getId(), i18NService.get("COMMAND_SHOULD_BE_ON_PRIVATE", message.getFrom().getLanguageCode()));

                return;
            }

            try {
                dictionariesBotService.loginUser(message.getFrom().getId());

                dictionariesBotService.deleteWhiteCard(Long.parseLong(data), Long.parseLong(message.getText().trim()));
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        });

        commands.put("/add_black_card", (message, data) -> {
            if (!BotMessageUtils.isMessagePrivate(message)) {
                logger.error("Comando /add_black_card enviado en lugar incorrecto por {}", BotMessageUtils.getUserInfo(message.getFrom()));

                dictionariesBotMessageService.sendMessage(message.getChat().getId(), i18NService.get("COMMAND_SHOULD_BE_ON_PRIVATE", message.getFrom().getLanguageCode()));

                return;
            }

            try {
                dictionariesBotService.loginUser(message.getFrom().getId());

                dictionariesBotService.addBlackCard(Long.parseLong(data), message.getText().trim());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        });

        commands.put("/edit_black_card_sel", (message, data) -> {
            if (!BotMessageUtils.isMessagePrivate(message)) {
                logger.error("Comando /edit_black_card_sel enviado en lugar incorrecto por {}", BotMessageUtils.getUserInfo(message.getFrom()));

                dictionariesBotMessageService.sendMessage(message.getChat().getId(), i18NService.get("COMMAND_SHOULD_BE_ON_PRIVATE", message.getFrom().getLanguageCode()));

                return;
            }

            try {
                dictionariesBotService.loginUser(message.getFrom().getId());

                dictionariesBotService.editBlackCardSelect(Long.parseLong(data), Long.parseLong(message.getText().trim()));
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        });

        commands.put("/edit_black_card", (message, data) -> {
            if (!BotMessageUtils.isMessagePrivate(message)) {
                logger.error("Comando /edit_black_card enviado en lugar incorrecto por {}", BotMessageUtils.getUserInfo(message.getFrom()));

                dictionariesBotMessageService.sendMessage(message.getChat().getId(), i18NService.get("COMMAND_SHOULD_BE_ON_PRIVATE", message.getFrom().getLanguageCode()));

                return;
            }

            try {
                dictionariesBotService.loginUser(message.getFrom().getId());

                dictionariesBotService.editBlackCard(Long.parseLong(data), message.getText().trim());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        });

        commands.put("/delete_black_card", (message, data) -> {
            if (!BotMessageUtils.isMessagePrivate(message)) {
                logger.error("Comando /delete_black_card enviado en lugar incorrecto por {}", BotMessageUtils.getUserInfo(message.getFrom()));

                dictionariesBotMessageService.sendMessage(message.getChat().getId(), i18NService.get("COMMAND_SHOULD_BE_ON_PRIVATE", message.getFrom().getLanguageCode()));

                return;
            }

            try {
                dictionariesBotService.loginUser(message.getFrom().getId());

                dictionariesBotService.deleteBlackCard(Long.parseLong(data), Long.parseLong(message.getText().trim()));
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        });

        commands.put("/manage_collabs_select", (message, data) -> {
            if (!BotMessageUtils.isMessagePrivate(message)) {
                logger.error("Comando /manage_collabs_select enviado en lugar incorrecto por {}", BotMessageUtils.getUserInfo(message.getFrom()));

                dictionariesBotMessageService.sendMessage(message.getChat().getId(), i18NService.get("COMMAND_SHOULD_BE_ON_PRIVATE", message.getFrom().getLanguageCode()));

                return;
            }

            try {
                dictionariesBotService.loginUser(message.getFrom().getId());

                dictionariesBotService.selectDictionaryToManageCollaborators(null, Long.parseLong(message.getText().trim()));
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        });

        commands.put("/add_collab", (message, data) -> {
            if (!BotMessageUtils.isMessagePrivate(message)) {
                logger.error("Comando /add_collab enviado en lugar incorrecto por {}", BotMessageUtils.getUserInfo(message.getFrom()));

                dictionariesBotMessageService.sendMessage(message.getChat().getId(), i18NService.get("COMMAND_SHOULD_BE_ON_PRIVATE", message.getFrom().getLanguageCode()));

                return;
            }

            try {
                dictionariesBotService.loginUser(message.getFrom().getId());

                dictionariesBotService.addCollaborator(Long.parseLong(data), message.getText().trim());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        });

        commands.put("/delete_collab", (message, data) -> {
            if (!BotMessageUtils.isMessagePrivate(message)) {
                logger.error("Comando /delete_collab enviado en lugar incorrecto por {}", BotMessageUtils.getUserInfo(message.getFrom()));

                dictionariesBotMessageService.sendMessage(message.getChat().getId(), i18NService.get("COMMAND_SHOULD_BE_ON_PRIVATE", message.getFrom().getLanguageCode()));

                return;
            }

            try {
                dictionariesBotService.loginUser(message.getFrom().getId());

                dictionariesBotService.deleteCollaborator(Long.parseLong(data), message.getText().trim());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        });

        commands.put("/toggle_collab", (message, data) -> {
            if (!BotMessageUtils.isMessagePrivate(message)) {
                logger.error("Comando /toggle_collab enviado en lugar incorrecto por {}", BotMessageUtils.getUserInfo(message.getFrom()));

                dictionariesBotMessageService.sendMessage(message.getChat().getId(), i18NService.get("COMMAND_SHOULD_BE_ON_PRIVATE", message.getFrom().getLanguageCode()));

                return;
            }

            try {
                dictionariesBotService.loginUser(message.getFrom().getId());

                dictionariesBotService.toggleCollaborator(Long.parseLong(data), message.getText().trim());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        });

        commands.put("/getmyid", (message, data) -> dictionariesBotMessageService.sendMessage(message.getFrom().getId(),
                "ID: " + message.getFrom().getId()));

        commands.put("/help", (message, data) -> dictionariesBotService.sendHelpMessage(message.getChatId()));

        return commands;
    }

    @Override
    public Map<String, CallbackQueryHandler> getCallbackQueries() {
        Map<String, CallbackQueryHandler> callbackQueryHandlerMap = new HashMap<>();

        callbackQueryHandlerMap.put("change_user_lang", (callbackQuery, data) -> {
            try {
                dictionariesBotService.loginUser(callbackQuery.getFrom().getId());

                dictionariesBotService.changeUserLanguage(callbackQuery.getMessage().getMessageId(),
                        data != null && !data.isBlank() ? data : callbackQuery.getFrom().getLanguageCode());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            dictionariesBotMessageService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("menu", (callbackQuery, data) -> {
            try {
                dictionariesBotService.loginUser(callbackQuery.getFrom().getId());

                dictionariesBotService.mainMenu(callbackQuery.getMessage().getMessageId());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            dictionariesBotMessageService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("dictionary_list", (callbackQuery, data) -> {
            try {
                dictionariesBotService.loginUser(callbackQuery.getFrom().getId());

                dictionariesBotService.listDictionaries(callbackQuery.getMessage().getMessageId());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            dictionariesBotMessageService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("dictionary_create", (callbackQuery, data) -> {
            try {
                dictionariesBotService.loginUser(callbackQuery.getFrom().getId());

                dictionariesBotService.createDictionaryMessage(callbackQuery.getMessage().getMessageId());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            dictionariesBotMessageService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("dictionary_rename", (callbackQuery, data) -> {
            try {
                dictionariesBotService.loginUser(callbackQuery.getFrom().getId());

                dictionariesBotService.renameDictionaryMessage(callbackQuery.getMessage().getMessageId());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            dictionariesBotMessageService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("dictionary_lang", (callbackQuery, data) -> {
            try {
                dictionariesBotService.loginUser(callbackQuery.getFrom().getId());

                dictionariesBotService.changeDictionaryLangMessage(callbackQuery.getMessage().getMessageId());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            dictionariesBotMessageService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("dictionary_delete", (callbackQuery, data) -> {
            try {
                dictionariesBotService.loginUser(callbackQuery.getFrom().getId());

                dictionariesBotService.deleteDictionaryMessage(callbackQuery.getMessage().getMessageId());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            dictionariesBotMessageService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("dictionary_toggle", (callbackQuery, data) -> {
            try {
                dictionariesBotService.loginUser(callbackQuery.getFrom().getId());

                dictionariesBotService.toggleDictionaryMessage(callbackQuery.getMessage().getMessageId());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            dictionariesBotMessageService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("dictionary_share", (callbackQuery, data) -> {
            try {
                dictionariesBotService.loginUser(callbackQuery.getFrom().getId());

                dictionariesBotService.shareDictionaryMessage(callbackQuery.getMessage().getMessageId());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            dictionariesBotMessageService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("share_accept", (callbackQuery, data) -> {
            try {
                dictionariesBotService.loginUser(callbackQuery.getFrom().getId());

                dictionariesBotService.acceptShareDictionary(callbackQuery.getMessage().getMessageId(), Long.parseLong(data));
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            dictionariesBotMessageService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("share_decline", (callbackQuery, data) -> {
            try {
                dictionariesBotService.loginUser(callbackQuery.getFrom().getId());

                dictionariesBotService.rejectShareDictionary(callbackQuery.getMessage().getMessageId(), Long.parseLong(data));
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            dictionariesBotMessageService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("dictionary_manage_cards", (callbackQuery, data) -> {
            try {
                dictionariesBotService.loginUser(callbackQuery.getFrom().getId());

                dictionariesBotService.manageCardsMessage(callbackQuery.getMessage().getMessageId());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            dictionariesBotMessageService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("manage_cards_select", (callbackQuery, data) -> {
            try {
                dictionariesBotService.loginUser(callbackQuery.getFrom().getId());

                dictionariesBotService.selectDictionaryToManageCards(callbackQuery.getMessage().getMessageId(), Long.parseLong(data));
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            dictionariesBotMessageService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("list_white_cards", (callbackQuery, data) -> {
            try {
                dictionariesBotService.loginUser(callbackQuery.getFrom().getId());

                dictionariesBotService.listWhiteCardsMessage(callbackQuery.getMessage().getMessageId(), Long.parseLong(data));
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            dictionariesBotMessageService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("add_white_cards", (callbackQuery, data) -> {
            try {
                dictionariesBotService.loginUser(callbackQuery.getFrom().getId());

                dictionariesBotService.addWhiteCardsMessage(callbackQuery.getMessage().getMessageId(), Long.parseLong(data));
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            dictionariesBotMessageService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("edit_white_cards", (callbackQuery, data) -> {
            try {
                dictionariesBotService.loginUser(callbackQuery.getFrom().getId());

                dictionariesBotService.editWhiteCardsMessage(callbackQuery.getMessage().getMessageId(), Long.parseLong(data));
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            dictionariesBotMessageService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("delete_white_cards", (callbackQuery, data) -> {
            try {
                dictionariesBotService.loginUser(callbackQuery.getFrom().getId());

                dictionariesBotService.deleteWhiteCardsMessage(callbackQuery.getMessage().getMessageId(), Long.parseLong(data));
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            dictionariesBotMessageService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("list_black_cards", (callbackQuery, data) -> {
            try {
                dictionariesBotService.loginUser(callbackQuery.getFrom().getId());

                dictionariesBotService.listBlackCardsMessage(callbackQuery.getMessage().getMessageId(), Long.parseLong(data));
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            dictionariesBotMessageService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("add_black_cards", (callbackQuery, data) -> {
            try {
                dictionariesBotService.loginUser(callbackQuery.getFrom().getId());

                dictionariesBotService.addBlackCardsMessage(callbackQuery.getMessage().getMessageId(), Long.parseLong(data));
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            dictionariesBotMessageService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("edit_black_cards", (callbackQuery, data) -> {
            try {
                dictionariesBotService.loginUser(callbackQuery.getFrom().getId());

                dictionariesBotService.editBlackCardsMessage(callbackQuery.getMessage().getMessageId(), Long.parseLong(data));
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            dictionariesBotMessageService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("delete_black_cards", (callbackQuery, data) -> {
            try {
                dictionariesBotService.loginUser(callbackQuery.getFrom().getId());

                dictionariesBotService.deleteBlackCardsMessage(callbackQuery.getMessage().getMessageId(), Long.parseLong(data));
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            dictionariesBotMessageService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("dictionary_manage_collabs", (callbackQuery, data) -> {
            try {
                dictionariesBotService.loginUser(callbackQuery.getFrom().getId());

                dictionariesBotService.manageCollaboratorsMessage(callbackQuery.getMessage().getMessageId());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            dictionariesBotMessageService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("manage_collabs_select", (callbackQuery, data) -> {
            try {
                dictionariesBotService.loginUser(callbackQuery.getFrom().getId());

                dictionariesBotService.selectDictionaryToManageCollaborators(callbackQuery.getMessage().getMessageId(), Long.parseLong(data));
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            dictionariesBotMessageService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("list_collabs", (callbackQuery, data) -> {
            try {
                dictionariesBotService.loginUser(callbackQuery.getFrom().getId());

                dictionariesBotService.listCollaboratorsMessage(callbackQuery.getMessage().getMessageId(), Long.parseLong(data));
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            dictionariesBotMessageService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("add_collabs", (callbackQuery, data) -> {
            try {
                dictionariesBotService.loginUser(callbackQuery.getFrom().getId());

                dictionariesBotService.addCollaboratorsMessage(callbackQuery.getMessage().getMessageId(), Long.parseLong(data));
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            dictionariesBotMessageService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("delete_collabs", (callbackQuery, data) -> {
            try {
                dictionariesBotService.loginUser(callbackQuery.getFrom().getId());

                dictionariesBotService.removeCollaboratorsMessage(callbackQuery.getMessage().getMessageId(), Long.parseLong(data));
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            dictionariesBotMessageService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("toggle_collabs", (callbackQuery, data) -> {
            try {
                dictionariesBotService.loginUser(callbackQuery.getFrom().getId());

                dictionariesBotService.toggleCollaboratorsMessage(callbackQuery.getMessage().getMessageId(), Long.parseLong(data));
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            dictionariesBotMessageService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("collaborator_accept", (callbackQuery, data) -> {
            try {
                dictionariesBotService.loginUser(callbackQuery.getFrom().getId());

                dictionariesBotService.acceptCollaborator(callbackQuery.getMessage().getMessageId(), Long.parseLong(data));
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            dictionariesBotMessageService.answerCallbackQuery(callbackQuery.getId());
        });

        callbackQueryHandlerMap.put("collaborator_decline", (callbackQuery, data) -> {
            try {
                dictionariesBotService.loginUser(callbackQuery.getFrom().getId());

                dictionariesBotService.rejectCollaborator(callbackQuery.getMessage().getMessageId(), Long.parseLong(data));
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

    @Autowired
    public void setI18NService(I18NService i18NService) {
        this.i18NService = i18NService;
    }

}
