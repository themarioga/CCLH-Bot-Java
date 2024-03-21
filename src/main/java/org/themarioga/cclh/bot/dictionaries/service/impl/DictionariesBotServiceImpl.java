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
import org.themarioga.cclh.commons.enums.CardTypeEnum;
import org.themarioga.cclh.commons.exceptions.ApplicationException;
import org.themarioga.cclh.commons.exceptions.dictionary.DictionaryAlreadyExistsException;
import org.themarioga.cclh.commons.exceptions.user.UserAlreadyExistsException;
import org.themarioga.cclh.commons.models.Card;
import org.themarioga.cclh.commons.models.Dictionary;
import org.themarioga.cclh.commons.models.DictionaryCollaborator;
import org.themarioga.cclh.commons.services.intf.CardService;
import org.themarioga.cclh.commons.services.intf.ConfigurationService;
import org.themarioga.cclh.commons.services.intf.DictionaryService;
import org.themarioga.cclh.commons.services.intf.UserService;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
public class DictionariesBotServiceImpl implements DictionariesBotService {

	private final Logger logger = LoggerFactory.getLogger(DictionariesBotServiceImpl.class);

	private DictionariesBotMessageService dictionariesBotMessageService;
	private UserService userService;
	private DictionaryService dictionaryService;
	private CardService cardService;
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
		InlineKeyboardMarkup groupInlineKeyboard = getMainMenuKeyboard();

		dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseMessageI18n.MAIN_MENU, groupInlineKeyboard);
	}

	@Override
	@Transactional(propagation = Propagation.SUPPORTS, rollbackFor = ApplicationException.class)
	public void mainMenu(long userId, int messageId) {
		InlineKeyboardMarkup groupInlineKeyboard = getMainMenuKeyboard();

		dictionariesBotMessageService.editMessage(userId, messageId, DictionariesBotResponseMessageI18n.MAIN_MENU, groupInlineKeyboard);
	}

	@Override
	@Transactional(propagation = Propagation.SUPPORTS, rollbackFor = ApplicationException.class)
	public void listDictionaries(long userId, int messageId) {
		List<Dictionary> dictionaryList = dictionaryService.getDictionariesByCreatorOrCollaborator(userService.getById(userId));

		InlineKeyboardMarkup groupInlineKeyboard = InlineKeyboardMarkup.builder()
				.keyboardRow(Collections.singletonList(InlineKeyboardButton.builder()
						.text("<- Volver").callbackData("menu").build()))
				.build();
		dictionariesBotMessageService.editMessage(userId, messageId, getDictionaryListMessage(userId, dictionaryList), groupInlineKeyboard);
	}

	@Override
	@Transactional(propagation = Propagation.SUPPORTS, rollbackFor = ApplicationException.class)
	public void createDictionaryMessage(long userId, int messageId) {
		dictionariesBotMessageService.deleteMessage(userId, messageId);
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
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseMessageI18n.MAIN_MENU, getMainMenuKeyboard());
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
	public void renameDictionaryMessage(long userId, int messageId) {
		List<Dictionary> dictionaryList = dictionaryService.getDictionariesByCreator(userService.getById(userId));

		dictionariesBotMessageService.deleteMessage(userId, messageId);
		dictionariesBotMessageService.setPendingReply(userId, "/rename_select");
		dictionariesBotMessageService.sendMessageWithForceReply(userId, getDictionaryRenameMessage(dictionaryList));
	}

	@Override
	@Transactional(propagation = Propagation.SUPPORTS, rollbackFor = ApplicationException.class)
	public void selectDictionaryToRename(long userId, int messageId, long dictionaryId) {
		Dictionary dictionary = dictionaryService.findOne(dictionaryId);

		if (dictionary == null) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_NOT_FOUND);

			return;
		}

		if (dictionary.getCreator().getId() != userId) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_NOT_YOURS);

			return;
		}

		dictionariesBotMessageService.deleteMessage(userId, messageId);
		dictionariesBotMessageService.setPendingReply(userId, "/rename__" + dictionaryId);
		dictionariesBotMessageService.sendMessageWithForceReply(userId, DictionariesBotResponseMessageI18n.DICTIONARY_RENAME);
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
	public void renameDictionary(long userId, long dictionaryId, String newName) {
		Dictionary dictionary = dictionaryService.findOne(dictionaryId);

		if (dictionary == null) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_NOT_FOUND);

			return;
		}

		if (dictionary.getCreator().getId() != userId) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_NOT_YOURS);

			return;
		}

		dictionaryService.setName(dictionary, newName);

		dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseMessageI18n.DICTIONARY_RENAMED);
		dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseMessageI18n.MAIN_MENU, getMainMenuKeyboard());
	}

	@Override
	@Transactional(propagation = Propagation.SUPPORTS, rollbackFor = ApplicationException.class)
	public void deleteDictionaryMessage(long userId, int messageId) {
		List<Dictionary> dictionaryList = dictionaryService.getDictionariesByCreator(userService.getById(userId));

		dictionariesBotMessageService.deleteMessage(userId, messageId);
		dictionariesBotMessageService.setPendingReply(userId, "/delete_select");
		dictionariesBotMessageService.sendMessageWithForceReply(userId, getDictionaryDeleteMessage(dictionaryList));
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
	public void selectDictionaryToDelete(long userId, int messageId, long dictionaryId) {
		Dictionary dictionary = dictionaryService.findOne(dictionaryId);

		if (dictionary == null) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_NOT_FOUND);

			return;
		}

		if (dictionary.getCreator().getId() != userId) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_NOT_YOURS);

			return;
		}

		if (Boolean.TRUE.equals(dictionary.getShared())) {
			dictionariesBotMessageService.sendMessage(userId, getDeleteSharedErrorMessage());

			return;
		}

		if (Boolean.TRUE.equals(dictionary.getPublished())) {
			dictionariesBotMessageService.deleteMessage(userId, messageId);
			dictionariesBotMessageService.setPendingReply(userId, "/delete__" + dictionaryId);
			dictionariesBotMessageService.sendMessageWithForceReply(userId, DictionariesBotResponseMessageI18n.DICTIONARY_DELETE);
		} else {
			dictionaryService.delete(dictionary);

			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseMessageI18n.DICTIONARY_DELETED);
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseMessageI18n.MAIN_MENU, getMainMenuKeyboard());
		}
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
	public void deleteDictionary(long userId, long dictionaryId, String text) {
		Dictionary dictionary = dictionaryService.findOne(dictionaryId);

		if (dictionary == null) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_NOT_FOUND);

			return;
		}

		if (dictionary.getCreator().getId() != userId) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_NOT_YOURS);

			return;
		}

		if (Boolean.TRUE.equals(dictionary.getShared())) {
			dictionariesBotMessageService.sendMessage(userId, getDeleteSharedErrorMessage());

			return;
		}

		if (text == null || text.isBlank() || !text.equals("SI")) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseMessageI18n.DICTIONARY_DELETE_CANCELLED);
		} else {
			dictionaryService.delete(dictionary);

			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseMessageI18n.DICTIONARY_DELETED);
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseMessageI18n.MAIN_MENU, getMainMenuKeyboard());
		}
	}

	@Override
	@Transactional(propagation = Propagation.SUPPORTS, rollbackFor = ApplicationException.class)
	public void manageCardsMessage(long userId, int messageId) {
		List<Dictionary> dictionaryList = dictionaryService.getDictionariesByCreatorOrCollaborator(userService.getById(userId));

		dictionariesBotMessageService.deleteMessage(userId, messageId);
		dictionariesBotMessageService.setPendingReply(userId, "/manage_cards_select");
		dictionariesBotMessageService.sendMessageWithForceReply(userId, getManageCardsMessage(userId, dictionaryList));
	}

	@Override
	@Transactional(propagation = Propagation.SUPPORTS, rollbackFor = ApplicationException.class)
	public void selectDictionaryToManageCards(long userId, Integer messageId, long dictionaryId) {
		Dictionary dictionary = dictionaryService.findOne(dictionaryId);

		if (dictionary == null) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_NOT_FOUND);

			return;
		}

		if (!dictionaryService.isDictionaryCollaborator(dictionary, userService.getById(userId))) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_NOT_YOURS);

			return;
		}

		if (dictionary.getPublished() || dictionary.getShared()) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_ALREADY_PUBLISHED);

			return;
		}

		InlineKeyboardMarkup groupInlineKeyboard = InlineKeyboardMarkup.builder()
				.keyboardRow(Arrays.asList(
						InlineKeyboardButton.builder()
								.text("Listar cartas blancas").callbackData("list_white_cards__" + dictionaryId).build(),
						InlineKeyboardButton.builder()
								.text("Listar cartas negras").callbackData("list_black_cards__" + dictionaryId).build()))
				.keyboardRow(Arrays.asList(
						InlineKeyboardButton.builder()
								.text("Añadir cartas blancas").callbackData("add_white_cards__" + dictionaryId).build(),
						InlineKeyboardButton.builder()
								.text("Añadir cartas negras").callbackData("add_black_cards__" + dictionaryId).build()))
				.keyboardRow(Arrays.asList(
						InlineKeyboardButton.builder()
								.text("Editar carta blanca").callbackData("edit_white_cards__" + dictionaryId).build(),
						InlineKeyboardButton.builder()
								.text("Editar carta negra").callbackData("edit_black_cards__" + dictionaryId).build()))
				.keyboardRow(Arrays.asList(
						InlineKeyboardButton.builder()
								.text("Borrar carta blanca").callbackData("delete_white_cards__" + dictionaryId).build(),
						InlineKeyboardButton.builder()
								.text("Borrar carta negra").callbackData("delete_black_cards__" + dictionaryId).build()))
				.keyboardRow(Collections.singletonList(InlineKeyboardButton.builder()
						.text("<- Volver").callbackData("menu").build()))
				.build();

		if (messageId == null) {
			dictionariesBotMessageService.sendMessage(userId, getCardMenuMessage(dictionary.getName()), groupInlineKeyboard);
		} else {
			dictionariesBotMessageService.editMessage(userId, messageId, getCardMenuMessage(dictionary.getName()), groupInlineKeyboard);
		}
	}

	@Override
	public void listWhiteCardsMessage(long userId, int messageId, long dictionaryId) {
		Dictionary dictionary = dictionaryService.findOne(dictionaryId);

		if (dictionary == null) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_NOT_FOUND);

			return;
		}

		if (!dictionaryService.isDictionaryCollaborator(dictionary, userService.getById(userId))) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_NOT_YOURS);

			return;
		}

		if (dictionary.getPublished() || dictionary.getShared()) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_ALREADY_PUBLISHED);

			return;
		}

		dictionariesBotMessageService.editMessage(userId, messageId, DictionariesBotResponseMessageI18n.CARDS_WHITE_LIST);

		sendCardList(userId, dictionary, CardTypeEnum.WHITE);

		InlineKeyboardMarkup groupInlineKeyboard = InlineKeyboardMarkup.builder()
				.keyboardRow(Collections.singletonList(InlineKeyboardButton.builder()
						.text("<- Volver").callbackData("manage_cards_select__" + dictionaryId).build()))
				.build();

		dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseMessageI18n.CARDS_WHITE_LIST_END, groupInlineKeyboard);
	}

	@Override
	@Transactional(propagation = Propagation.SUPPORTS, rollbackFor = ApplicationException.class)
	public void addWhiteCardsMessage(long userId, int messageId, long dictionaryId) {
		Dictionary dictionary = dictionaryService.findOne(dictionaryId);

		if (dictionary == null) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_NOT_FOUND);

			return;
		}

		if (!dictionaryService.isDictionaryCollaborator(dictionary, userService.getById(userId))) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_NOT_YOURS);

			return;
		}

		if (dictionary.getPublished() || dictionary.getShared()) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_ALREADY_PUBLISHED);

			return;
		}

		dictionariesBotMessageService.deleteMessage(userId, messageId);
		dictionariesBotMessageService.setPendingReply(userId, "/add_white_card__" + dictionaryId);
		dictionariesBotMessageService.sendMessageWithForceReply(userId, DictionariesBotResponseMessageI18n.CARDS_WHITE_CARD_ADD);
	}

	@Override
	public void addWhiteCard(long userId, long dictionaryId, String text) {
		Dictionary dictionary = dictionaryService.findOne(dictionaryId);

		if (dictionary == null) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_NOT_FOUND);

			return;
		}

		if (!dictionaryService.isDictionaryCollaborator(dictionary, userService.getById(userId))) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_NOT_YOURS);

			return;
		}

		if (dictionary.getPublished() || dictionary.getShared()) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_ALREADY_PUBLISHED);

			return;
		}

		// ToDo
	}

	@Override
	@Transactional(propagation = Propagation.SUPPORTS, rollbackFor = ApplicationException.class)
	public void editWhiteCardsMessage(long userId, int messageId, long dictionaryId) {
		Dictionary dictionary = dictionaryService.findOne(dictionaryId);

		if (dictionary == null) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_NOT_FOUND);

			return;
		}

		if (!dictionaryService.isDictionaryCollaborator(dictionary, userService.getById(userId))) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_NOT_YOURS);

			return;
		}

		if (dictionary.getPublished() || dictionary.getShared()) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_ALREADY_PUBLISHED);

			return;
		}

		// ToDo
	}

	@Override
	@Transactional(propagation = Propagation.SUPPORTS, rollbackFor = ApplicationException.class)
	public void deleteWhiteCardsMessage(long userId, int messageId, long dictionaryId) {
		Dictionary dictionary = dictionaryService.findOne(dictionaryId);

		if (dictionary == null) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_NOT_FOUND);

			return;
		}

		if (!dictionaryService.isDictionaryCollaborator(dictionary, userService.getById(userId))) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_NOT_YOURS);

			return;
		}

		if (dictionary.getPublished() || dictionary.getShared()) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_ALREADY_PUBLISHED);

			return;
		}

		// ToDo
	}

	@Override
	public void listBlackCardsMessage(long userId, int messageId, long dictionaryId) {
		Dictionary dictionary = dictionaryService.findOne(dictionaryId);

		if (dictionary == null) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_NOT_FOUND);

			return;
		}

		if (!dictionaryService.isDictionaryCollaborator(dictionary, userService.getById(userId))) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_NOT_YOURS);

			return;
		}

		if (dictionary.getPublished() || dictionary.getShared()) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_ALREADY_PUBLISHED);

			return;
		}

		dictionariesBotMessageService.editMessage(userId, messageId, DictionariesBotResponseMessageI18n.CARDS_BLACK_LIST);

		sendCardList(userId, dictionary, CardTypeEnum.BLACK);

		InlineKeyboardMarkup groupInlineKeyboard = InlineKeyboardMarkup.builder()
				.keyboardRow(Collections.singletonList(InlineKeyboardButton.builder()
						.text("<- Volver").callbackData("manage_cards_select__" + dictionaryId).build()))
				.build();

		dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseMessageI18n.CARDS_BLACK_LIST_END, groupInlineKeyboard);
	}

	@Override
	@Transactional(propagation = Propagation.SUPPORTS, rollbackFor = ApplicationException.class)
	public void addBlackCardsMessage(long userId, int messageId, long dictionaryId) {
		Dictionary dictionary = dictionaryService.findOne(dictionaryId);

		if (dictionary == null) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_NOT_FOUND);

			return;
		}

		if (!dictionaryService.isDictionaryCollaborator(dictionary, userService.getById(userId))) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_NOT_YOURS);

			return;
		}

		if (dictionary.getPublished() || dictionary.getShared()) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_ALREADY_PUBLISHED);

			return;
		}

		dictionariesBotMessageService.deleteMessage(userId, messageId);
		dictionariesBotMessageService.setPendingReply(userId, "/add_black_card__" + dictionaryId);
		dictionariesBotMessageService.sendMessageWithForceReply(userId, DictionariesBotResponseMessageI18n.CARDS_BLACK_CARD_ADD);
	}

	@Override
	public void addBlackCard(long userId, long dictionaryId, String text) {
		Dictionary dictionary = dictionaryService.findOne(dictionaryId);

		if (dictionary == null) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_NOT_FOUND);

			return;
		}

		if (!dictionaryService.isDictionaryCollaborator(dictionary, userService.getById(userId))) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_NOT_YOURS);

			return;
		}

		if (dictionary.getPublished() || dictionary.getShared()) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_ALREADY_PUBLISHED);

			return;
		}

		// ToDo
	}

	@Override
	@Transactional(propagation = Propagation.SUPPORTS, rollbackFor = ApplicationException.class)
	public void editBlackCardsMessage(long userId, int messageId, long dictionaryId) {
		Dictionary dictionary = dictionaryService.findOne(dictionaryId);

		if (dictionary == null) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_NOT_FOUND);

			return;
		}

		if (!dictionaryService.isDictionaryCollaborator(dictionary, userService.getById(userId))) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_NOT_YOURS);

			return;
		}

		if (dictionary.getPublished() || dictionary.getShared()) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_ALREADY_PUBLISHED);

			return;
		}

		// ToDo
	}

	@Override
	@Transactional(propagation = Propagation.SUPPORTS, rollbackFor = ApplicationException.class)
	public void deleteBlackCardsMessage(long userId, int messageId, long dictionaryId) {
		Dictionary dictionary = dictionaryService.findOne(dictionaryId);

		if (dictionary == null) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_NOT_FOUND);

			return;
		}

		if (!dictionaryService.isDictionaryCollaborator(dictionary, userService.getById(userId))) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_NOT_YOURS);

			return;
		}

		if (dictionary.getPublished() || dictionary.getShared()) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_ALREADY_PUBLISHED);

			return;
		}

		// ToDo
	}

	@Override
	@Transactional(propagation = Propagation.SUPPORTS, rollbackFor = ApplicationException.class)
	public void manageCollaboratorsMessage(long userId, int messageId) {
		List<Dictionary> dictionaryList = dictionaryService.getDictionariesByCreator(userService.getById(userId));

		dictionariesBotMessageService.deleteMessage(userId, messageId);
		dictionariesBotMessageService.setPendingReply(userId, "/manage_collabs_select");
		dictionariesBotMessageService.sendMessageWithForceReply(userId, getManageCollaboratorsMessage(dictionaryList));
	}

	@Override
	@Transactional(propagation = Propagation.SUPPORTS, rollbackFor = ApplicationException.class)
	public void selectDictionaryToManageCollaborators(long userId, Integer messageId, long dictionaryId) {
		Dictionary dictionary = dictionaryService.findOne(dictionaryId);

		if (dictionary == null) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_NOT_FOUND);

			return;
		}

		if (dictionary.getCreator().getId() != userId) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_NOT_YOURS);

			return;
		}

		InlineKeyboardMarkup groupInlineKeyboard = InlineKeyboardMarkup.builder()
				.keyboardRow(Collections.singletonList(InlineKeyboardButton.builder()
						.text("Listar colaboradores").callbackData("list_collabs__" + dictionaryId).build()))
				.keyboardRow(Collections.singletonList(InlineKeyboardButton.builder()
						.text("Añadir colaborador").callbackData("add_collabs__" + dictionaryId).build()))
				.keyboardRow(Collections.singletonList(InlineKeyboardButton.builder()
						.text("Quitar colaborador").callbackData("delete_collabs__" + dictionaryId).build()))
				.keyboardRow(Collections.singletonList(InlineKeyboardButton.builder()
						.text("Activar/Desactivar colaborador").callbackData("toggle_collabs__" + dictionaryId).build()))
				.keyboardRow(Collections.singletonList(InlineKeyboardButton.builder()
						.text("<- Volver").callbackData("menu").build()))
				.build();

		if (messageId == null) {
			dictionariesBotMessageService.sendMessage(userId, getCollaboratorMenuMessage(dictionary.getName()), groupInlineKeyboard);
		} else {
			dictionariesBotMessageService.editMessage(userId, messageId, getCollaboratorMenuMessage(dictionary.getName()), groupInlineKeyboard);
		}
	}

	@Override
	@Transactional(propagation = Propagation.SUPPORTS, rollbackFor = ApplicationException.class)
	public void listCollaboratorsMessage(long userId, int messageId, long dictionaryId) {
		Dictionary dictionary = dictionaryService.findOne(dictionaryId);

		if (dictionary == null) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_NOT_FOUND);

			return;
		}

		if (dictionary.getCreator().getId() != userId) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_NOT_YOURS);

			return;
		}

		InlineKeyboardMarkup groupInlineKeyboard = InlineKeyboardMarkup.builder()
				.keyboardRow(Collections.singletonList(InlineKeyboardButton.builder()
						.text("<- Volver").callbackData("manage_collabs_select__" + dictionaryId).build()))
				.build();
		dictionariesBotMessageService.editMessage(userId, messageId, getCollaboratorListMessage(userId, dictionary.getCollaborators()), groupInlineKeyboard);
	}

	@Override
	@Transactional(propagation = Propagation.SUPPORTS, rollbackFor = ApplicationException.class)
	public void addCollaboratorsMessage(long userId, int messageId, long dictionaryId) {
		Dictionary dictionary = dictionaryService.findOne(dictionaryId);

		if (dictionary == null) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_NOT_FOUND);

			return;
		}

		if (dictionary.getCreator().getId() != userId) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_NOT_YOURS);

			return;
		}

		dictionariesBotMessageService.deleteMessage(userId, messageId);
		dictionariesBotMessageService.setPendingReply(userId, "/add_collab__" + dictionaryId);
		dictionariesBotMessageService.sendMessageWithForceReply(userId, DictionariesBotResponseMessageI18n.DICTIONARY_ADD_COLLABORATOR);
	}

	@Override
	@Transactional(propagation = Propagation.SUPPORTS, rollbackFor = ApplicationException.class)
	public void removeCollaboratorsMessage(long userId, int messageId, long dictionaryId) {
		Dictionary dictionary = dictionaryService.findOne(dictionaryId);

		if (dictionary == null) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_NOT_FOUND);

			return;
		}

		if (dictionary.getCreator().getId() != userId) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_NOT_YOURS);

			return;
		}

		// ToDo
	}

	@Override
	public void toggleCollaboratorsMessage(long userId, int messageId, long dictionaryId) {
		Dictionary dictionary = dictionaryService.findOne(dictionaryId);

		if (dictionary == null) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_NOT_FOUND);

			return;
		}

		if (dictionary.getCreator().getId() != userId) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_NOT_YOURS);

			return;
		}

		// ToDo
	}

	@Override
	@Transactional(propagation = Propagation.SUPPORTS, rollbackFor = ApplicationException.class)
	public void sendHelpMessage(long chatId) {
		dictionariesBotMessageService.sendMessage(chatId, getHelpMessage());
	}

	private static InlineKeyboardMarkup getMainMenuKeyboard() {
		return InlineKeyboardMarkup.builder()
				.keyboardRow(Collections.singletonList(InlineKeyboardButton.builder()
						.text("Listar mis diccionarios").callbackData("dictionary_list").build()))
				.keyboardRow(Collections.singletonList(InlineKeyboardButton.builder()
						.text("Crear diccionario").callbackData("dictionary_create").build()))
				.keyboardRow(Collections.singletonList(InlineKeyboardButton.builder()
						.text("Renombrar diccionario").callbackData("dictionary_rename").build()))
				.keyboardRow(Collections.singletonList(InlineKeyboardButton.builder()
						.text("Borrar diccionario").callbackData("dictionary_delete").build()))
				.keyboardRow(Collections.singletonList(InlineKeyboardButton.builder()
						.text("Gestionar cartas").callbackData("dictionary_manage_cards").build()))
				.keyboardRow(Collections.singletonList(InlineKeyboardButton.builder()
						.text("Gestionar colaboradores").callbackData("dictionary_manage_collabs").build()))
				.build();
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

		return MessageFormat.format(DictionariesBotResponseMessageI18n.DICTIONARIES_RENAME_LIST, stringBuilder.toString());
	}

	private String getDictionaryDeleteMessage(List<Dictionary> dictionaryList) {
		StringBuilder stringBuilder = new StringBuilder();
		for (Dictionary dictionary : dictionaryList) {
			stringBuilder.append(dictionary.getId()).append(" - ").append(dictionary.getName()).append(" (")
					.append("publicado: ").append(StringUtils.booleanToSpanish(dictionary.getPublished()))
					.append(", compartido: ").append(StringUtils.booleanToSpanish(dictionary.getShared()))
					.append(")\n");
		}

		return MessageFormat.format(DictionariesBotResponseMessageI18n.DICTIONARIES_DELETE_LIST, stringBuilder.toString());
	}

	private String getManageCardsMessage(long userId, List<Dictionary> dictionaryList) {
		StringBuilder stringBuilder = new StringBuilder();
		for (Dictionary dictionary : dictionaryList) {
			stringBuilder.append(dictionary.getId()).append(" - ").append(dictionary.getName()).append(" (")
					.append("tuyo: ").append(StringUtils.booleanToSpanish(dictionary.getCreator().getId() == userId))
					.append(", publicado: ").append(StringUtils.booleanToSpanish(dictionary.getPublished()))
					.append(", compartido: ").append(StringUtils.booleanToSpanish(dictionary.getShared()))
					.append(")\n");
		}

		return MessageFormat.format(DictionariesBotResponseMessageI18n.DICTIONARIES_MANAGE_CARDS_LIST, stringBuilder.toString());
	}

	private String getManageCollaboratorsMessage(List<Dictionary> dictionaryList) {
		StringBuilder stringBuilder = new StringBuilder();
		for (Dictionary dictionary : dictionaryList) {
			stringBuilder.append(dictionary.getId()).append(" - ").append(dictionary.getName()).append(" (")
					.append("publicado: ").append(StringUtils.booleanToSpanish(dictionary.getPublished()))
					.append(", compartido: ").append(StringUtils.booleanToSpanish(dictionary.getShared()))
					.append(")\n");
		}

		return MessageFormat.format(DictionariesBotResponseMessageI18n.DICTIONARIES_MANAGE_COLLABORATORS_LIST, stringBuilder.toString());
	}

	private String getCollaboratorListMessage(long userId, List<DictionaryCollaborator> collaborators) {
		StringBuilder stringBuilder = new StringBuilder();
		if (collaborators.size() > 1) {
			for (DictionaryCollaborator dictionaryCollaborator : collaborators) {
				if (dictionaryCollaborator.getUser().getId() != userId) {
					stringBuilder.append(dictionaryCollaborator.getUser().getName()).append("\n");
				}
			}
		} else {
			stringBuilder.append("\n").append("No hay colaboradores.").append("\n");
		}

		return MessageFormat.format(DictionariesBotResponseMessageI18n.COLLABORATORS_LIST, stringBuilder.toString());
	}

	private void sendCardList(long userId, Dictionary dictionary, CardTypeEnum cardTypeEnum) {
		StringBuilder stringBuilder = new StringBuilder();
		List<Card> whiteCards = cardService.findCardsByDictionaryIdAndType(dictionary, cardTypeEnum);
		for (Card whiteCard : whiteCards) {
			if (stringBuilder.length() > 4000) {
				dictionariesBotMessageService.sendMessage(userId, stringBuilder.toString());
				stringBuilder = new StringBuilder();
			}

			stringBuilder.append(whiteCard.getId()).append(" - ").append(whiteCard.getText()).append("\n");
		}

		if (!stringBuilder.isEmpty()) {
			dictionariesBotMessageService.sendMessage(userId, stringBuilder.toString());
		}
	}

	private String getCardMenuMessage(String name) {
		return MessageFormat.format(DictionariesBotResponseMessageI18n.CARDS_MENU, name);
	}

	private String getCollaboratorMenuMessage(String name) {
		return MessageFormat.format(DictionariesBotResponseMessageI18n.COLLABORATORS_MENU, name);
	}

	private String getDeleteSharedErrorMessage() {
		return MessageFormat.format(DictionariesBotResponseErrorI18n.DICTIONARY_SHARED, getBotCreatorName());
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
	public void setCardService(CardService cardService) {
		this.cardService = cardService;
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
