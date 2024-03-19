package org.themarioga.cclh.bot.dictionaries.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.themarioga.cclh.bot.dictionaries.constants.DictionariesBotResponseErrorI18n;
import org.themarioga.cclh.bot.dictionaries.service.intf.DictionariesBotMessageService;
import org.themarioga.cclh.bot.dictionaries.constants.DictionariesBotResponseMessageI18n;
import org.themarioga.cclh.bot.dictionaries.service.intf.DictionariesBotService;
import org.themarioga.cclh.bot.util.StringUtils;
import org.themarioga.cclh.commons.exceptions.ApplicationException;
import org.themarioga.cclh.commons.exceptions.dictionary.DictionaryAlreadyExistsException;
import org.themarioga.cclh.commons.exceptions.user.UserAlreadyExistsException;
import org.themarioga.cclh.commons.models.Dictionary;
import org.themarioga.cclh.commons.services.intf.ConfigurationService;
import org.themarioga.cclh.commons.services.intf.DictionaryService;
import org.themarioga.cclh.commons.services.intf.UserService;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;

@Service
public class DictionariesBotServiceImpl implements DictionariesBotService {

	private final Logger logger = LoggerFactory.getLogger(DictionariesBotServiceImpl.class);

	private DictionariesBotMessageService dictionariesBotMessageService;
	private DictionariesBotService dictionariesBotService;
	private UserService userService;
	private DictionaryService dictionaryService;
	private ConfigurationService configurationService;

	@Override
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
	public void registerUser(long userId, String username) {
		try {
			userService.createOrReactivate(userId, username);

			dictionariesBotMessageService.sendMessage(userId,  DictionariesBotResponseMessageI18n.PLAYER_WELCOME);
		} catch (UserAlreadyExistsException e) {
			logger.warn("El usuario {} ({}) ya estaba registrado en el otro bot.", userId, username);

			throw e;
		} catch (ApplicationException e) {
			dictionariesBotMessageService.sendMessage(userId, e.getMessage());

			throw e;
		}
	}

	@Override
	@Transactional(propagation = Propagation.SUPPORTS, rollbackFor = ApplicationException.class)
	public void mainMenu(long userId) {
		InlineKeyboardMarkup groupInlineKeyboard = InlineKeyboardMarkup.builder()
				.keyboardRow(Collections.singletonList(InlineKeyboardButton.builder()
						.text("Listar mis diccionarios").callbackData("dictionary_list").build()))
				.keyboardRow(Collections.singletonList(InlineKeyboardButton.builder()
						.text("Crear diccionario").callbackData("dictionary_create").build()))
				.keyboardRow(Collections.singletonList(InlineKeyboardButton.builder()
						.text("Renombrar diccionario").callbackData("dictionary_rename").build()))
				.keyboardRow(Collections.singletonList(InlineKeyboardButton.builder()
						.text("Gestionar cartas").callbackData("dictionary_manage_cards").build()))
				.keyboardRow(Collections.singletonList(InlineKeyboardButton.builder()
						.text("Borrar diccionario").callbackData("dictionary_delete").build()))
				.build();

		dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseMessageI18n.MAIN_MENU, groupInlineKeyboard);
	}

	@Override
	@Transactional(propagation = Propagation.SUPPORTS, rollbackFor = ApplicationException.class)
	public void listDictionaries(long userId) {
		List<Dictionary> dictionaryList = dictionaryService.getDictionariesByCreatorOrCollaborator(userService.getById(userId));

		dictionariesBotMessageService.sendMessage(userId, getDictionaryListMessage(userId, dictionaryList));
	}

	@Override
	@Transactional(propagation = Propagation.SUPPORTS, rollbackFor = ApplicationException.class)
	public void createDictionaryMessage(long userId) {
		dictionariesBotMessageService.setPendingReply(userId, "/create");

		dictionariesBotMessageService.sendMessageWithForceReply(userId,
				DictionariesBotResponseMessageI18n.DICTIONARY_CREATE);
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
	public void createDictionary(long userId, String name) {
		try {
			dictionaryService.create(name, userService.getById(userId));

			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseMessageI18n.DICTIONARY_CREATED);
		} catch (DictionaryAlreadyExistsException e) {
			logger.error("Ya existe un diccionario con el nombre {}", name);

			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_ALREADY_EXISTS);

			throw e;
		} catch (ApplicationException e) {
			dictionariesBotMessageService.sendMessage(userId, e.getMessage());

			throw e;
		}
	}

	@Override
	@Transactional(propagation = Propagation.SUPPORTS, rollbackFor = ApplicationException.class)
	public void renameDictionaryMessage(long userId) {
		List<Dictionary> dictionaryList = dictionaryService.getDictionariesByCreator(userService.getById(userId));

		dictionariesBotMessageService.setPendingReply(userId, "/rename_select");
		dictionariesBotMessageService.sendMessageWithForceReply(userId, getDictionaryRenameMessage(dictionaryList));
	}

	@Override
	@Transactional(propagation = Propagation.SUPPORTS, rollbackFor = ApplicationException.class)
	public void selectDictionaryToRename(long userId, long dictionaryId) {
		Dictionary dictionary = dictionaryService.findOne(dictionaryId);

		if (dictionary.getCreator().getId() != userId) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_NOT_YOURS);

			return;
		}

		dictionariesBotMessageService.setPendingReply(userId, "/rename__" + dictionaryId);
		dictionariesBotMessageService.sendMessageWithForceReply(userId, DictionariesBotResponseMessageI18n.DICTIONARY_RENAME);
	}

	@Override
	public void renameDictionary(long userId, long dictionaryId, String newName) {
		Dictionary dictionary = dictionaryService.findOne(dictionaryId);

		if (dictionary.getCreator().getId() != userId) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_NOT_YOURS);

			return;
		}

		dictionaryService.setName(dictionary, newName);

		dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseMessageI18n.DICTIONARY_RENAMED);
	}

	@Override
	@Transactional(propagation = Propagation.SUPPORTS, rollbackFor = ApplicationException.class)
	public void sendHelpMessage(long chatId) {
		dictionariesBotMessageService.sendMessage(chatId, getHelpMessage());
	}

	private String getDictionaryListMessage(long userId, List<Dictionary> dictionaryList) {
		StringBuilder stringBuilder = new StringBuilder();
		for (Dictionary dictionary : dictionaryList) {
			stringBuilder.append(dictionary.getId()).append(" - ").append(dictionary.getName()).append(" (")
					.append("tuyo: ").append(StringUtils.booleanToSpanish(dictionary.getCreator().getId() == userId))
					.append(", publicado: ").append(StringUtils.booleanToSpanish(dictionary.getPublished()))
					.append(", compartido: ").append(StringUtils.booleanToSpanish(dictionary.getShared()))
					.append(")\n");
		}

		return MessageFormat.format(DictionariesBotResponseMessageI18n.DICTIONARIES_LIST, stringBuilder.toString());
	}

	private String getDictionaryRenameMessage(List<Dictionary> dictionaryList) {
		StringBuilder stringBuilder = new StringBuilder();
		for (Dictionary dictionary : dictionaryList) {
			stringBuilder.append(dictionary.getId()).append(" - ").append(dictionary.getName()).append("\n");
		}

		return MessageFormat.format(DictionariesBotResponseMessageI18n.DICTIONARIES_EDIT_LIST, stringBuilder.toString());
	}

	private String getHelpMessage() {
		return MessageFormat.format(DictionariesBotResponseMessageI18n.HELP, getBotName(), getBotVersion(), getBotHelpURL(), getBotCreatorName());
	}

	private String getBotName() {
		return configurationService.getConfiguration("dictionaries_bot_name") +
				" (" + configurationService.getConfiguration("dictionaries_bot_alias") + ")";
	}

	private String getBotVersion() {
		return configurationService.getConfiguration("dictionaries_bot_version");
	}

	private Long getBotCreatorId() {
		return Long.parseLong(configurationService.getConfiguration("dictionaries_bot_owner_id"));
	}

	private String getBotCreatorName() {
		return configurationService.getConfiguration("dictionaries_bot_owner_alias");
	}

	private String getBotHelpURL() {
		return configurationService.getConfiguration("dictionaries_bot_help_url");
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
	public void setUserService(UserService userService) {
		this.userService = userService;
	}

	@Autowired
	public void setDictionaryService(DictionaryService dictionaryService) {
		this.dictionaryService = dictionaryService;
	}

	@Autowired
	public void setConfigurationService(ConfigurationService configurationService) {
		this.configurationService = configurationService;
	}

}
