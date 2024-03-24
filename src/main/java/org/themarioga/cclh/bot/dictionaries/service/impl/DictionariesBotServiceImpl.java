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
import org.themarioga.cclh.commons.exceptions.card.CardAlreadyExistsException;
import org.themarioga.cclh.commons.exceptions.card.CardDoesntExistsException;
import org.themarioga.cclh.commons.exceptions.card.CardTextExcededLength;
import org.themarioga.cclh.commons.exceptions.dictionary.*;
import org.themarioga.cclh.commons.exceptions.user.UserAlreadyExistsException;
import org.themarioga.cclh.commons.exceptions.user.UserDoesntExistsException;
import org.themarioga.cclh.commons.models.Card;
import org.themarioga.cclh.commons.models.Dictionary;
import org.themarioga.cclh.commons.models.DictionaryCollaborator;
import org.themarioga.cclh.commons.models.User;
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

			dictionariesBotMessageService.sendMessage(userId,  DictionariesBotResponseMessageI18n.PLAYER_WELCOME);

			throw e;
		} catch (ApplicationException e) {
			dictionariesBotMessageService.sendMessage(userId, e.getMessage());

			throw e;
		}
	}

	@Override
	@Transactional(propagation = Propagation.SUPPORTS, rollbackFor = ApplicationException.class)
	public void mainMenu(long userId) {
		InlineKeyboardMarkup privateInlineKeyboard = getMainMenuKeyboard();

		dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseMessageI18n.MAIN_MENU,
				privateInlineKeyboard);
	}

	@Override
	@Transactional(propagation = Propagation.SUPPORTS, rollbackFor = ApplicationException.class)
	public void mainMenu(long userId, int messageId) {
		InlineKeyboardMarkup privateInlineKeyboard = getMainMenuKeyboard();

		dictionariesBotMessageService.editMessage(userId, messageId, DictionariesBotResponseMessageI18n.MAIN_MENU,
				privateInlineKeyboard);
	}

	@Override
	@Transactional(propagation = Propagation.SUPPORTS, rollbackFor = ApplicationException.class)
	public void listDictionaries(long userId, int messageId) {
		List<Dictionary> dictionaryList =
				dictionaryService.getDictionariesByCreatorOrCollaborator(userService.getById(userId));

		InlineKeyboardMarkup privateInlineKeyboard = InlineKeyboardMarkup.builder()
				.keyboardRow(Collections.singletonList(InlineKeyboardButton.builder()
						.text(DictionariesBotResponseMessageI18n.VOLVER).callbackData("menu").build()))
				.build();
		dictionariesBotMessageService.editMessage(userId, messageId, getDictionaryListMessage(userId, dictionaryList),
				privateInlineKeyboard);
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
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseMessageI18n.MAIN_MENU,
					getMainMenuKeyboard());
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
		try {
			Dictionary dictionary = getDictionaryAndCheckCreator(userId, dictionaryId);

			dictionariesBotMessageService.deleteMessage(userId, messageId);
			dictionariesBotMessageService.setPendingReply(userId, "/rename__" + dictionary.getId());
			dictionariesBotMessageService.sendMessageWithForceReply(userId,
					DictionariesBotResponseMessageI18n.DICTIONARY_RENAME);
		} catch (DictionaryDoesntExistsException e) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_NOT_FOUND);
		} catch (DictionaryNotYoursException e) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_NOT_YOURS);
		}
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
	public void renameDictionary(long userId, long dictionaryId, String newName) {
		try {
			Dictionary dictionary = getDictionaryAndCheckCreator(userId, dictionaryId);

			dictionaryService.setName(dictionary, newName);

			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseMessageI18n.DICTIONARY_RENAMED);
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseMessageI18n.MAIN_MENU,
					getMainMenuKeyboard());
		} catch (DictionaryDoesntExistsException e) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_NOT_FOUND);
		} catch (DictionaryNotYoursException e) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_NOT_YOURS);
		}
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
		try {
			Dictionary dictionary = getDictionaryAndCheckCreator(userId, dictionaryId);

			if (Boolean.TRUE.equals(dictionary.getShared())) {
				dictionariesBotMessageService.sendMessage(userId, getDeleteSharedErrorMessage());

				return;
			}

			if (Boolean.TRUE.equals(dictionary.getPublished())) {
				dictionariesBotMessageService.deleteMessage(userId, messageId);
				dictionariesBotMessageService.setPendingReply(userId, "/delete__" + dictionaryId);
				dictionariesBotMessageService.sendMessageWithForceReply(userId,
						DictionariesBotResponseMessageI18n.DICTIONARY_DELETE);
			} else {
				dictionaryService.delete(dictionary);

				dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseMessageI18n.DICTIONARY_DELETED);
				dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseMessageI18n.MAIN_MENU,
						getMainMenuKeyboard());
			}
		} catch (DictionaryDoesntExistsException e) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_NOT_FOUND);
		} catch (DictionaryNotYoursException e) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_NOT_YOURS);
		}
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
	public void deleteDictionary(long userId, long dictionaryId, String text) {
		try {
			Dictionary dictionary = getDictionaryAndCheckCreator(userId, dictionaryId);

			if (Boolean.TRUE.equals(dictionary.getShared())) {
				dictionariesBotMessageService.sendMessage(userId, getDeleteSharedErrorMessage());

				return;
			}

			if (text == null || text.isBlank() || !text.equals("SI")) {
				dictionariesBotMessageService.sendMessage(userId,
						DictionariesBotResponseMessageI18n.DICTIONARY_DELETE_CANCELLED);
			} else {
				dictionaryService.delete(dictionary);

				dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseMessageI18n.DICTIONARY_DELETED);
				dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseMessageI18n.MAIN_MENU,
						getMainMenuKeyboard());
			}
		} catch (DictionaryDoesntExistsException e) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_NOT_FOUND);
		} catch (DictionaryNotYoursException e) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_NOT_YOURS);
		}
	}

	@Override
	@Transactional(propagation = Propagation.SUPPORTS, rollbackFor = ApplicationException.class)
	public void toggleDictionaryMessage(long userId, int messageId) {
		List<Dictionary> dictionaryList = dictionaryService.getDictionariesByCreator(userService.getById(userId));

		dictionariesBotMessageService.deleteMessage(userId, messageId);
		dictionariesBotMessageService.setPendingReply(userId, "/toggle_select");
		dictionariesBotMessageService.sendMessageWithForceReply(userId, getDictionaryRenameMessage(dictionaryList));
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
	public void toggleDictionary(long userId, int messageId, long dictionaryId) {
		try {
			Dictionary dictionary = getDictionaryAndCheckCreator(userId, dictionaryId);

			if (Boolean.TRUE.equals(dictionary.getShared())) {
				dictionariesBotMessageService.sendMessage(userId, getDeleteSharedErrorMessage());

				return;
			}

			dictionaryService.togglePublished(dictionary);

			dictionariesBotMessageService.sendMessage(userId, getDictionaryPublishedMessage(dictionary));
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseMessageI18n.MAIN_MENU,
					getMainMenuKeyboard());
		} catch (DictionaryDoesntExistsException e) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_NOT_FOUND);
		} catch (DictionaryNotYoursException e) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_NOT_YOURS);
		} catch (DictionaryAlreadySharedException e) {
			dictionariesBotMessageService.sendMessage(userId, getDictionaryAlreadySharedError());
		} catch (DictionaryNotCompletedException e) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_NOT_FILLED);
		}
	}

	@Override
	@Transactional(propagation = Propagation.SUPPORTS, rollbackFor = ApplicationException.class)
	public void manageCardsMessage(long userId, int messageId) {
		List<Dictionary> dictionaryList =
				dictionaryService.getDictionariesByCreatorOrCollaborator(userService.getById(userId));

		dictionariesBotMessageService.deleteMessage(userId, messageId);
		dictionariesBotMessageService.setPendingReply(userId, "/manage_cards_select");
		dictionariesBotMessageService.sendMessageWithForceReply(userId, getManageCardsMessage(userId, dictionaryList));
	}

	@Override
	@Transactional(propagation = Propagation.SUPPORTS, rollbackFor = ApplicationException.class)
	public void selectDictionaryToManageCards(long userId, Integer messageId, long dictionaryId) {
		try {
			Dictionary dictionary = getDictionaryAndCheckActiveCollaborator(userId, dictionaryId);

			checkDictionaryPublishedAndShared(dictionary);

			if (messageId == null) {
				dictionariesBotMessageService.sendMessage(userId, getCardMenuMessage(dictionary),
						getCardKeyboardMenu(dictionaryId));
			} else {
				dictionariesBotMessageService.editMessage(userId, messageId, getCardMenuMessage(dictionary),
						getCardKeyboardMenu(dictionaryId));
			}
		} catch (DictionaryDoesntExistsException e) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_NOT_FOUND);
		} catch (DictionaryNotYoursException e) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_NOT_YOURS);
		} catch (DictionaryAlreadyPublishedException e) {
			dictionariesBotMessageService.sendMessage(userId, getDeleteSharedErrorMessage());
		}
	}

	@Override
	@Transactional(propagation = Propagation.SUPPORTS, rollbackFor = ApplicationException.class)
	public void listWhiteCardsMessage(long userId, int messageId, long dictionaryId) {
		try {
			Dictionary dictionary = getDictionaryAndCheckActiveCollaborator(userId, dictionaryId);

			checkDictionaryPublishedAndShared(dictionary);

			dictionariesBotMessageService.editMessage(userId, messageId,
					DictionariesBotResponseMessageI18n.CARDS_WHITE_LIST);

			sendCardList(userId, dictionary, CardTypeEnum.WHITE);

			InlineKeyboardMarkup privateInlineKeyboard = InlineKeyboardMarkup.builder()
					.keyboardRow(Collections.singletonList(InlineKeyboardButton.builder()
							.text(DictionariesBotResponseMessageI18n.VOLVER)
							.callbackData("manage_cards_select__" + dictionaryId).build()))
					.build();

			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseMessageI18n.CARDS_WHITE_LIST_END,
					privateInlineKeyboard);
		} catch (DictionaryDoesntExistsException e) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_NOT_FOUND);
		} catch (DictionaryNotYoursException e) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_NOT_YOURS);
		} catch (DictionaryAlreadyPublishedException e) {
			dictionariesBotMessageService.sendMessage(userId, getDeleteSharedErrorMessage());
		}
	}

	@Override
	@Transactional(propagation = Propagation.SUPPORTS, rollbackFor = ApplicationException.class)
	public void addWhiteCardsMessage(long userId, int messageId, long dictionaryId) {
		try {
			Dictionary dictionary = getDictionaryAndCheckActiveCollaborator(userId, dictionaryId);

			checkDictionaryPublishedAndShared(dictionary);

			dictionariesBotMessageService.deleteMessage(userId, messageId);
			dictionariesBotMessageService.setPendingReply(userId, "/add_white_card__" + dictionaryId);
			dictionariesBotMessageService.sendMessageWithForceReply(userId,
					DictionariesBotResponseMessageI18n.CARDS_WHITE_CARD_ADD);
		} catch (DictionaryDoesntExistsException e) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_NOT_FOUND);
		} catch (DictionaryNotYoursException e) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_NOT_YOURS);
		} catch (DictionaryAlreadyPublishedException e) {
			dictionariesBotMessageService.sendMessage(userId, getDeleteSharedErrorMessage());
		}
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
	public void addWhiteCard(long userId, long dictionaryId, String text) {
		try {
			Dictionary dictionary = getDictionaryAndCheckActiveCollaborator(userId, dictionaryId);

			checkDictionaryPublishedAndShared(dictionary);

			if (!text.equals(":cancel:")) {
				cardService.create(dictionary, CardTypeEnum.WHITE, text);

				dictionariesBotMessageService.sendMessage(userId, getWhiteCardAddedMessage(dictionary));

				dictionariesBotMessageService.setPendingReply(userId, "/add_white_card__" + dictionaryId);
				dictionariesBotMessageService.sendMessageWithForceReply(userId,
						DictionariesBotResponseMessageI18n.CARDS_WHITE_CARD_ADD_ANOTHER);
			} else {
				dictionariesBotMessageService.sendMessage(userId, getCardMenuMessage(dictionary),
						getCardKeyboardMenu(dictionaryId));
			}
		} catch (DictionaryDoesntExistsException e) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_NOT_FOUND);
		} catch (DictionaryNotYoursException e) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_NOT_YOURS);
		} catch (DictionaryAlreadyPublishedException e) {
			dictionariesBotMessageService.sendMessage(userId, getDeleteSharedErrorMessage());
		} catch (CardAlreadyExistsException e) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.CARD_ALREADY_EXISTS);
		} catch (CardTextExcededLength e) {
			dictionariesBotMessageService.sendMessage(userId, getAddWhiteCardLengthExceededErrorMessage());
		} catch (DictionaryAlreadyFilledException e) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_ALREADY_FILLED);
		}
	}

	@Override
	@Transactional(propagation = Propagation.SUPPORTS, rollbackFor = ApplicationException.class)
	public void editWhiteCardsMessage(long userId, int messageId, long dictionaryId) {
		try {
			Dictionary dictionary = getDictionaryAndCheckActiveCollaborator(userId, dictionaryId);

			checkDictionaryPublishedAndShared(dictionary);

			dictionariesBotMessageService.editMessage(userId, messageId,
					DictionariesBotResponseMessageI18n.CARDS_WHITE_LIST);

			sendCardList(userId, dictionary, CardTypeEnum.WHITE);

			dictionariesBotMessageService.setPendingReply(userId, "/edit_white_card_sel__" + dictionaryId);
			dictionariesBotMessageService.sendMessageWithForceReply(userId,
					DictionariesBotResponseMessageI18n.CARDS_WHITE_CARD_EDIT);
		} catch (DictionaryDoesntExistsException e) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_NOT_FOUND);
		} catch (DictionaryNotYoursException e) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_NOT_YOURS);
		} catch (DictionaryAlreadyPublishedException e) {
			dictionariesBotMessageService.sendMessage(userId, getDeleteSharedErrorMessage());
		}
	}

	@Override
	@Transactional(propagation = Propagation.SUPPORTS, rollbackFor = ApplicationException.class)
	public void editWhiteCardSelect(long userId, long dictionaryId, long cardId) {
		try {
			Dictionary dictionary = getDictionaryAndCheckActiveCollaborator(userId, dictionaryId);

			checkDictionaryPublishedAndShared(dictionary);

			dictionariesBotMessageService.setPendingReply(userId, "/edit_white_card__" + cardId);
			dictionariesBotMessageService.sendMessageWithForceReply(userId,
					DictionariesBotResponseMessageI18n.CARDS_WHITE_CARD_EDIT_NEW_TEXT);
		} catch (DictionaryDoesntExistsException e) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_NOT_FOUND);
		} catch (DictionaryNotYoursException e) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_NOT_YOURS);
		} catch (DictionaryAlreadyPublishedException e) {
			dictionariesBotMessageService.sendMessage(userId, getDeleteSharedErrorMessage());
		}
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
	public void editWhiteCard(long userId, long cardId, String newText) {
		try {
			Card card = cardService.getCardById(cardId);

			if (card.getId() == null)
				throw new CardDoesntExistsException();

			if (!dictionaryService.isDictionaryActiveCollaborator(card.getDictionary(), userService.getById(userId)))
				throw new DictionaryNotYoursException();

			if (Boolean.TRUE.equals(card.getDictionary().getShared()))
				throw new DictionaryAlreadySharedException();

			cardService.changeText(card, newText);

			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseMessageI18n.CARDS_WHITE_CARD_EDITED);
			dictionariesBotMessageService.sendMessage(userId, getCardMenuMessage(card.getDictionary()), getCardKeyboardMenu(card.getDictionary().getId()));
		} catch (CardDoesntExistsException e) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.CARD_DOESNT_EXISTS);
		} catch (CardAlreadyExistsException e) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.CARD_ALREADY_EXISTS);
		} catch (DictionaryNotYoursException e) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.CARD_NOT_YOURS);
		} catch (DictionaryAlreadySharedException e) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_SHARED);
		} catch (CardTextExcededLength e) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.CARD_EXCEEDED_LENGTH);
		}
	}

	@Override
	@Transactional(propagation = Propagation.SUPPORTS, rollbackFor = ApplicationException.class)
	public void deleteWhiteCardsMessage(long userId, int messageId, long dictionaryId) {
		try {
			Dictionary dictionary = getDictionaryAndCheckActiveCollaborator(userId, dictionaryId);

			checkDictionaryPublishedAndShared(dictionary);

			dictionariesBotMessageService.editMessage(userId, messageId,
					DictionariesBotResponseMessageI18n.CARDS_WHITE_LIST);

			sendCardList(userId, dictionary, CardTypeEnum.WHITE);

			dictionariesBotMessageService.setPendingReply(userId, "/delete_white_card__" + dictionaryId);
			dictionariesBotMessageService.sendMessageWithForceReply(userId,
					DictionariesBotResponseMessageI18n.CARDS_WHITE_CARD_DELETE);
		} catch (DictionaryDoesntExistsException e) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_NOT_FOUND);
		} catch (DictionaryNotYoursException e) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_NOT_YOURS);
		} catch (DictionaryAlreadyPublishedException e) {
			dictionariesBotMessageService.sendMessage(userId, getDeleteSharedErrorMessage());
		}
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
	public void deleteWhiteCard(long userId, long dictionaryId, long cardId) {
		try {
			Dictionary dictionary = getDictionaryAndCheckActiveCollaborator(userId, dictionaryId);

			checkDictionaryPublishedAndShared(dictionary);

			Card card = cardService.getCardById(cardId);

			if (!card.getDictionary().getId().equals(dictionary.getId()))
				throw new CardDoesntExistsException();

			cardService.delete(card);

			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseMessageI18n.CARDS_WHITE_CARD_DELETED);

			dictionariesBotMessageService.sendMessage(userId, getCardMenuMessage(dictionary),
					getCardKeyboardMenu(dictionaryId));
		} catch (DictionaryDoesntExistsException e) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_NOT_FOUND);
		} catch (DictionaryNotYoursException e) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_NOT_YOURS);
		} catch (DictionaryAlreadyPublishedException e) {
			dictionariesBotMessageService.sendMessage(userId, getDeleteSharedErrorMessage());
		} catch (CardDoesntExistsException e) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.CARD_NOT_YOURS);
		}
	}

	@Override
	@Transactional(propagation = Propagation.SUPPORTS, rollbackFor = ApplicationException.class)
	public void listBlackCardsMessage(long userId, int messageId, long dictionaryId) {
		try {
			Dictionary dictionary = getDictionaryAndCheckActiveCollaborator(userId, dictionaryId);

			checkDictionaryPublishedAndShared(dictionary);

			dictionariesBotMessageService.editMessage(userId, messageId,
					DictionariesBotResponseMessageI18n.CARDS_BLACK_LIST);

			sendCardList(userId, dictionary, CardTypeEnum.BLACK);

			InlineKeyboardMarkup privateInlineKeyboard = InlineKeyboardMarkup.builder()
					.keyboardRow(Collections.singletonList(InlineKeyboardButton.builder()
							.text(DictionariesBotResponseMessageI18n.VOLVER)
							.callbackData("manage_cards_select__" + dictionaryId).build()))
					.build();

			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseMessageI18n.CARDS_BLACK_LIST_END,
					privateInlineKeyboard);
		} catch (DictionaryDoesntExistsException e) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_NOT_FOUND);
		} catch (DictionaryNotYoursException e) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_NOT_YOURS);
		} catch (DictionaryAlreadyPublishedException e) {
			dictionariesBotMessageService.sendMessage(userId, getDeleteSharedErrorMessage());
		}
	}

	@Override
	@Transactional(propagation = Propagation.SUPPORTS, rollbackFor = ApplicationException.class)
	public void addBlackCardsMessage(long userId, int messageId, long dictionaryId) {
		try {
			Dictionary dictionary = getDictionaryAndCheckActiveCollaborator(userId, dictionaryId);

			checkDictionaryPublishedAndShared(dictionary);

			dictionariesBotMessageService.deleteMessage(userId, messageId);
			dictionariesBotMessageService.setPendingReply(userId, "/add_black_card__" + dictionaryId);
			dictionariesBotMessageService.sendMessageWithForceReply(userId,
					DictionariesBotResponseMessageI18n.CARDS_BLACK_CARD_ADD);
		} catch (DictionaryDoesntExistsException e) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_NOT_FOUND);
		} catch (DictionaryNotYoursException e) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_NOT_YOURS);
		} catch (DictionaryAlreadyPublishedException e) {
			dictionariesBotMessageService.sendMessage(userId, getDeleteSharedErrorMessage());
		}
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
	public void addBlackCard(long userId, long dictionaryId, String text) {
		try {
			Dictionary dictionary = getDictionaryAndCheckActiveCollaborator(userId, dictionaryId);

			checkDictionaryPublishedAndShared(dictionary);

			if (!text.equals(":cancel:")) {
				cardService.create(dictionary, CardTypeEnum.BLACK, text);

				dictionariesBotMessageService.sendMessage(userId, getBlackCardAddedMessage(dictionary));

				dictionariesBotMessageService.setPendingReply(userId, "/add_black_card__" + dictionaryId);
				dictionariesBotMessageService.sendMessageWithForceReply(userId,
						DictionariesBotResponseMessageI18n.CARDS_BLACK_CARD_ADD_ANOTHER);
			} else {
				dictionariesBotMessageService.sendMessage(userId, getCardMenuMessage(dictionary),
						getCardKeyboardMenu(dictionaryId));
			}
		} catch (DictionaryDoesntExistsException e) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_NOT_FOUND);
		} catch (DictionaryNotYoursException e) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_NOT_YOURS);
		} catch (DictionaryAlreadyPublishedException e) {
			dictionariesBotMessageService.sendMessage(userId, getDeleteSharedErrorMessage());
		} catch (CardAlreadyExistsException e) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.CARD_ALREADY_EXISTS);
		} catch (CardTextExcededLength e) {
			dictionariesBotMessageService.sendMessage(userId, getAddBlackCardLengthExceededErrorMessage());
		}
	}

	@Override
	@Transactional(propagation = Propagation.SUPPORTS, rollbackFor = ApplicationException.class)
	public void editBlackCardsMessage(long userId, int messageId, long dictionaryId) {
		try {
			Dictionary dictionary = getDictionaryAndCheckActiveCollaborator(userId, dictionaryId);

			checkDictionaryPublishedAndShared(dictionary);

			dictionariesBotMessageService.editMessage(userId, messageId,
					DictionariesBotResponseMessageI18n.CARDS_BLACK_LIST);

			sendCardList(userId, dictionary, CardTypeEnum.BLACK);

			dictionariesBotMessageService.setPendingReply(userId, "/edit_black_card_sel__" + dictionaryId);
			dictionariesBotMessageService.sendMessageWithForceReply(userId,
					DictionariesBotResponseMessageI18n.CARDS_BLACK_CARD_EDIT);
		} catch (DictionaryDoesntExistsException e) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_NOT_FOUND);
		} catch (DictionaryNotYoursException e) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_NOT_YOURS);
		} catch (DictionaryAlreadyPublishedException e) {
			dictionariesBotMessageService.sendMessage(userId, getDeleteSharedErrorMessage());
		}
	}

	@Override
	@Transactional(propagation = Propagation.SUPPORTS, rollbackFor = ApplicationException.class)
	public void editBlackCardSelect(long userId, long dictionaryId, long cardId) {
		try {
			Dictionary dictionary = getDictionaryAndCheckActiveCollaborator(userId, dictionaryId);

			if (dictionary.getPublished() || dictionary.getShared()) {
				throw new DictionaryAlreadyPublishedException();
			}

			dictionariesBotMessageService.setPendingReply(userId, "/edit_black_card__" + cardId);
			dictionariesBotMessageService.sendMessageWithForceReply(userId,
					DictionariesBotResponseMessageI18n.CARDS_BLACK_CARD_EDIT_NEW_TEXT);
		} catch (DictionaryDoesntExistsException e) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_NOT_FOUND);
		} catch (DictionaryNotYoursException e) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_NOT_YOURS);
		} catch (DictionaryAlreadyPublishedException e) {
			dictionariesBotMessageService.sendMessage(userId,
					DictionariesBotResponseErrorI18n.DICTIONARY_ALREADY_PUBLISHED);
		}
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
	public void editBlackCard(long userId, long cardId, String newText) {
		try {
			Card card = cardService.getCardById(cardId);

			if (card.getId() == null)
				throw new CardDoesntExistsException();

			if (!dictionaryService.isDictionaryActiveCollaborator(card.getDictionary(), userService.getById(userId)))
				throw new DictionaryNotYoursException();

			if (Boolean.TRUE.equals(card.getDictionary().getShared()))
				throw new DictionaryAlreadySharedException();

			cardService.changeText(card, newText);

			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseMessageI18n.CARDS_BLACK_CARD_EDITED);
			dictionariesBotMessageService.sendMessage(userId, getCardMenuMessage(card.getDictionary()), getCardKeyboardMenu(card.getDictionary().getId()));
		} catch (CardDoesntExistsException e) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.CARD_DOESNT_EXISTS);
		} catch (CardAlreadyExistsException e) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.CARD_ALREADY_EXISTS);
		} catch (DictionaryNotYoursException e) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.CARD_NOT_YOURS);
		} catch (DictionaryAlreadySharedException e) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_SHARED);
		} catch (CardTextExcededLength e) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.CARD_EXCEEDED_LENGTH);
		}
	}

	@Override
	@Transactional(propagation = Propagation.SUPPORTS, rollbackFor = ApplicationException.class)
	public void deleteBlackCardsMessage(long userId, int messageId, long dictionaryId) {
		try {
			Dictionary dictionary = getDictionaryAndCheckActiveCollaborator(userId, dictionaryId);

			checkDictionaryPublishedAndShared(dictionary);

			dictionariesBotMessageService.editMessage(userId, messageId,
					DictionariesBotResponseMessageI18n.CARDS_BLACK_LIST);

			sendCardList(userId, dictionary, CardTypeEnum.BLACK);

			dictionariesBotMessageService.setPendingReply(userId, "/delete_black_card__" + dictionaryId);
			dictionariesBotMessageService.sendMessageWithForceReply(userId,
					DictionariesBotResponseMessageI18n.CARDS_BLACK_CARD_DELETE);
		} catch (DictionaryDoesntExistsException e) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_NOT_FOUND);
		} catch (DictionaryNotYoursException e) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_NOT_YOURS);
		} catch (DictionaryAlreadyPublishedException e) {
			dictionariesBotMessageService.sendMessage(userId, getDeleteSharedErrorMessage());
		}
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
	public void deleteBlackCard(long userId, long dictionaryId, long cardId) {
		try {
			Dictionary dictionary = getDictionaryAndCheckActiveCollaborator(userId, dictionaryId);

			checkDictionaryPublishedAndShared(dictionary);

			Card card = cardService.getCardById(cardId);

			if (!card.getDictionary().getId().equals(dictionary.getId()))
				throw new CardDoesntExistsException();

			cardService.delete(card);

			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseMessageI18n.CARDS_BLACK_CARD_DELETED);

			dictionariesBotMessageService.sendMessage(userId, getCardMenuMessage(dictionary),
					getCardKeyboardMenu(dictionaryId));
		} catch (DictionaryDoesntExistsException e) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_NOT_FOUND);
		} catch (DictionaryNotYoursException e) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_NOT_YOURS);
		} catch (DictionaryAlreadyPublishedException e) {
			dictionariesBotMessageService.sendMessage(userId, getDeleteSharedErrorMessage());
		} catch (CardDoesntExistsException e) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.CARD_NOT_YOURS);
		}
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
		try {
			Dictionary dictionary = getDictionaryAndCheckCreator(userId, dictionaryId);

			InlineKeyboardMarkup privateInlineKeyboard = getDictionaryCollaboratorMenu(dictionaryId);

			if (messageId == null) {
				dictionariesBotMessageService.sendMessage(userId,
						getCollaboratorMenuMessage(dictionary), privateInlineKeyboard);
			} else {
				dictionariesBotMessageService.editMessage(userId, messageId,
						getCollaboratorMenuMessage(dictionary), privateInlineKeyboard);
			}
		} catch (DictionaryDoesntExistsException e) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_NOT_FOUND);
		} catch (DictionaryNotYoursException e) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_NOT_YOURS);
		}
	}

	@Override
	@Transactional(propagation = Propagation.SUPPORTS, rollbackFor = ApplicationException.class)
	public void listCollaboratorsMessage(long userId, int messageId, long dictionaryId) {
		try {
			Dictionary dictionary = getDictionaryAndCheckCreator(userId, dictionaryId);

			InlineKeyboardMarkup privateInlineKeyboard = InlineKeyboardMarkup.builder()
					.keyboardRow(Collections.singletonList(InlineKeyboardButton.builder()
							.text(DictionariesBotResponseMessageI18n.VOLVER)
							.callbackData("manage_collabs_select__" + dictionaryId).build()))
					.build();
			dictionariesBotMessageService.editMessage(userId, messageId,
					getCollaboratorListMessage(userId, dictionary.getCollaborators()), privateInlineKeyboard);
		} catch (DictionaryDoesntExistsException e) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_NOT_FOUND);
		} catch (DictionaryNotYoursException e) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_NOT_YOURS);
		}
	}

	@Override
	@Transactional(propagation = Propagation.SUPPORTS, rollbackFor = ApplicationException.class)
	public void addCollaboratorsMessage(long userId, int messageId, long dictionaryId) {
		try {
			Dictionary dictionary = getDictionaryAndCheckCreator(userId, dictionaryId);

			dictionariesBotMessageService.deleteMessage(userId, messageId);
			dictionariesBotMessageService.setPendingReply(userId, "/add_collab__" + dictionary.getId());
			dictionariesBotMessageService.sendMessageWithForceReply(userId,
					DictionariesBotResponseMessageI18n.COLLABORATORS_ADD);
		} catch (DictionaryDoesntExistsException e) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_NOT_FOUND);
		} catch (DictionaryNotYoursException e) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_NOT_YOURS);
		}
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
	public void addCollaborator(long userId, long dictionaryId, String nameOrId) {
		try {
			Dictionary dictionary = getDictionaryAndCheckCreator(userId, dictionaryId);

			User user = getUserByNameOrId(nameOrId);

			DictionaryCollaborator dictionaryCollaborator = dictionaryService.addCollaborator(dictionary, user);

			dictionariesBotMessageService.sendMessage(userId,
					getAddedCollaboratorMessage(dictionaryCollaborator));

			InlineKeyboardMarkup collaboratorInlineKeyboard = InlineKeyboardMarkup.builder()
					.keyboardRow(Collections.singletonList(InlineKeyboardButton.builder().text("Aceptar")
							.callbackData("collaborator_accept__" + dictionaryId).build()))
					.keyboardRow(Collections.singletonList(InlineKeyboardButton.builder().text("Cancelar")
							.callbackData("collaborator_decline__" + dictionaryId).build()))
					.build();
			dictionariesBotMessageService.sendMessage(dictionaryCollaborator.getUser().getId(),
					getAddCollaboratorAcceptMessage(dictionaryCollaborator), collaboratorInlineKeyboard);

			InlineKeyboardMarkup privateInlineKeyboard = getDictionaryCollaboratorMenu(dictionaryId);

			dictionariesBotMessageService.sendMessage(userId,
					getCollaboratorMenuMessage(dictionary), privateInlineKeyboard);
		} catch (DictionaryDoesntExistsException e) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_NOT_FOUND);
		} catch (DictionaryNotYoursException e) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_NOT_YOURS);
		} catch (UserDoesntExistsException e) {
			dictionariesBotMessageService.sendMessage(userId,
					DictionariesBotResponseErrorI18n.COLLABORATOR_ADD_USER_DOESNT_EXISTS);
		} catch (DictionaryCollaboratorAlreadyExists e) {
			dictionariesBotMessageService.sendMessage(userId,
					DictionariesBotResponseErrorI18n.COLLABORATOR_ADD_ALREADY_EXISTS);
		} catch (DictionaryMaxCollaboratorsReached e) {
			dictionariesBotMessageService.sendMessage(userId,
					DictionariesBotResponseErrorI18n.COLLABORATOR_ADD_MAX_REACHED);
		}
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
	public void acceptCollaborator(long userId, int messageId, long dictionaryId) {
		try {
			Dictionary dictionary = getDictionaryAndCheckCollaborator(userId, dictionaryId);

			User user = userService.getById(userId);

			dictionaryService.acceptCollaborator(dictionary, user);

			dictionariesBotMessageService.editMessage(userId, messageId, DictionariesBotResponseMessageI18n.COLLABORATORS_ACCEPTED_MESSAGE);

			dictionariesBotMessageService.sendMessage(dictionary.getCreator().getId(), getCollaboratorAcceptedMessage(user.getName()));
		} catch (DictionaryDoesntExistsException e) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_NOT_FOUND);
		} catch (DictionaryNotYoursException e) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_NOT_YOURS);
		}
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
	public void rejectCollaborator(long userId, int messageId, long dictionaryId) {
		try {
			Dictionary dictionary = getDictionaryAndCheckCollaborator(userId, dictionaryId);

			User user = userService.getById(userId);

			dictionaryService.removeCollaborator(dictionary, user);

			dictionariesBotMessageService.editMessage(userId, messageId, DictionariesBotResponseMessageI18n.COLLABORATORS_REJECTED_MESSAGE);

			dictionariesBotMessageService.sendMessage(dictionary.getCreator().getId(), getCollaboratorRejectedMessage(user.getName()));
		} catch (DictionaryDoesntExistsException e) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_NOT_FOUND);
		} catch (DictionaryNotYoursException e) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_NOT_YOURS);
		}
	}

	@Override
	@Transactional(propagation = Propagation.SUPPORTS, rollbackFor = ApplicationException.class)
	public void removeCollaboratorsMessage(long userId, int messageId, long dictionaryId) {
		try {
			Dictionary dictionary = getDictionaryAndCheckCreator(userId, dictionaryId);

			dictionariesBotMessageService.deleteMessage(userId, messageId);
			dictionariesBotMessageService.setPendingReply(userId, "/delete_collab__" + dictionary.getId());
			dictionariesBotMessageService.sendMessageWithForceReply(userId,
					DictionariesBotResponseMessageI18n.COLLABORATORS_DELETE);
		} catch (DictionaryDoesntExistsException e) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_NOT_FOUND);
		} catch (DictionaryNotYoursException e) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_NOT_YOURS);
		}
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
	public void deleteCollaborator(long userId, long dictionaryId, String nameOrId) {
		try {
			Dictionary dictionary = getDictionaryAndCheckCreator(userId, dictionaryId);

			User user = getUserByNameOrId(nameOrId);

			dictionaryService.removeCollaborator(dictionary, user);

			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseMessageI18n.COLLABORATORS_DELETED);

			dictionariesBotMessageService.sendMessage(user.getId(), getDeleteCollaboratorInfoMessage(dictionary));

			InlineKeyboardMarkup privateInlineKeyboard = getDictionaryCollaboratorMenu(dictionaryId);

			dictionariesBotMessageService.sendMessage(userId,
					getCollaboratorMenuMessage(dictionary), privateInlineKeyboard);
		} catch (DictionaryDoesntExistsException e) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_NOT_FOUND);
		} catch (DictionaryNotYoursException e) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_NOT_YOURS);
		} catch (DictionaryCollaboratorDoesntExists e) {
			dictionariesBotMessageService.sendMessage(userId,
					DictionariesBotResponseErrorI18n.COLLABORATOR_REMOVE_USER_DOESNT_EXISTS);
		}
	}

	@Override
	public void toggleCollaboratorsMessage(long userId, int messageId, long dictionaryId) {
		try {
			Dictionary dictionary = getDictionaryAndCheckCreator(userId, dictionaryId);

			dictionariesBotMessageService.deleteMessage(userId, messageId);
			dictionariesBotMessageService.setPendingReply(userId, "/toggle_collab__" + dictionary.getId());
			dictionariesBotMessageService.sendMessageWithForceReply(userId,
					DictionariesBotResponseMessageI18n.COLLABORATORS_TOGGLE);
		} catch (DictionaryDoesntExistsException e) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_NOT_FOUND);
		} catch (DictionaryNotYoursException e) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_NOT_YOURS);
		}
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
	public void toggleCollaborator(long userId, long dictionaryId, String nameOrId) {
		try {
			Dictionary dictionary = getDictionaryAndCheckCreator(userId, dictionaryId);

			User user = getUserByNameOrId(nameOrId);

			DictionaryCollaborator dictionaryCollaborator = dictionaryService.toggleCollaborator(dictionary, user);

			dictionariesBotMessageService.sendMessage(userId,
					getToggledCollaboratorMessage(dictionaryCollaborator));
		} catch (DictionaryDoesntExistsException e) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_NOT_FOUND);
		} catch (DictionaryNotYoursException e) {
			dictionariesBotMessageService.sendMessage(userId, DictionariesBotResponseErrorI18n.DICTIONARY_NOT_YOURS);
		}
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
						.text("Publicar/despublicar diccionario").callbackData("dictionary_toggle").build()))
				.keyboardRow(Arrays.asList(
						InlineKeyboardButton.builder()
							.text("Gestionar cartas").callbackData("dictionary_manage_cards").build(),
						InlineKeyboardButton.builder()
							.text("Gestionar colaboradores").callbackData("dictionary_manage_collabs").build()))
				.build();
	}

	private static InlineKeyboardMarkup getDictionaryCollaboratorMenu(long dictionaryId) {
		return InlineKeyboardMarkup.builder()
				.keyboardRow(Collections.singletonList(InlineKeyboardButton.builder()
						.text("Listar colaboradores").callbackData("list_collabs__" + dictionaryId).build()))
				.keyboardRow(Collections.singletonList(InlineKeyboardButton.builder()
						.text("Añadir colaborador").callbackData("add_collabs__" + dictionaryId).build()))
				.keyboardRow(Collections.singletonList(InlineKeyboardButton.builder()
						.text("Quitar colaborador").callbackData("delete_collabs__" + dictionaryId).build()))
				.keyboardRow(Collections.singletonList(InlineKeyboardButton.builder()
						.text("Activar/Desactivar colaborador").callbackData("toggle_collabs__" + dictionaryId).build()))
				.keyboardRow(Collections.singletonList(InlineKeyboardButton.builder()
						.text(DictionariesBotResponseMessageI18n.VOLVER).callbackData("menu").build()))
				.build();
	}

	private static InlineKeyboardMarkup getCardKeyboardMenu(long dictionaryId) {
		return InlineKeyboardMarkup.builder()
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
						.text(DictionariesBotResponseMessageI18n.VOLVER).callbackData("menu").build()))
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

		return MessageFormat.format(DictionariesBotResponseMessageI18n.DICTIONARIES_MANAGE_CARDS_LIST,
				stringBuilder.toString());
	}

	private String getManageCollaboratorsMessage(List<Dictionary> dictionaryList) {
		StringBuilder stringBuilder = new StringBuilder();
		for (Dictionary dictionary : dictionaryList) {
			stringBuilder.append(dictionary.getId()).append(" - ").append(dictionary.getName()).append(" (")
					.append("publicado: ").append(StringUtils.booleanToSpanish(dictionary.getPublished()))
					.append(", compartido: ").append(StringUtils.booleanToSpanish(dictionary.getShared()))
					.append(")\n");
		}

		return MessageFormat.format(DictionariesBotResponseMessageI18n.DICTIONARIES_MANAGE_COLLABORATORS_LIST,
				stringBuilder.toString());
	}

	private String getCollaboratorListMessage(long userId, List<DictionaryCollaborator> collaborators) {
		StringBuilder stringBuilder = new StringBuilder();
		if (collaborators.size() > 1) {
			for (DictionaryCollaborator dictionaryCollaborator : collaborators) {
				if (dictionaryCollaborator.getUser().getId() != userId) {
					stringBuilder.append(dictionaryCollaborator.getUser().getName()).append(" (")
							.append("aceptado: ").append(StringUtils.booleanToSpanish(dictionaryCollaborator.getAccepted()))
							.append(", activo: ").append(StringUtils.booleanToSpanish(dictionaryCollaborator.getCanEdit()))
							.append(")\n");
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

	private static String getAddCollaboratorAcceptMessage(DictionaryCollaborator dictionaryCollaborator) {
		return MessageFormat.format(DictionariesBotResponseMessageI18n.COLLABORATORS_ACCEPT_MESSAGE,
				dictionaryCollaborator.getDictionary().getCreator().getName(),
				dictionaryCollaborator.getDictionary().getName());
	}

	private static String getDeleteCollaboratorInfoMessage(Dictionary dictionary) {
		return MessageFormat.format(DictionariesBotResponseMessageI18n.COLLABORATORS_DELETED_MESSAGE,
				dictionary.getName());
	}

	private String getCardMenuMessage(Dictionary dictionary) {
		return MessageFormat.format(DictionariesBotResponseMessageI18n.CARDS_MENU, dictionary.getName());
	}

	private String getWhiteCardAddedMessage(Dictionary dictionary) {
		int whiteCardCount = cardService.countCardsByDictionaryIdAndType(dictionary, CardTypeEnum.WHITE);
		int maxWhiteCards = (whiteCardCount < cardService.getDictionaryMinWhiteCards() ?
				cardService.getDictionaryMinWhiteCards() : cardService.getDictionaryMaxWhiteCards());
		return MessageFormat.format(DictionariesBotResponseMessageI18n.CARDS_WHITE_CARD_ADDED, whiteCardCount, maxWhiteCards);
	}

	private String getBlackCardAddedMessage(Dictionary dictionary) {
		int blackCardCount = cardService.countCardsByDictionaryIdAndType(dictionary, CardTypeEnum.BLACK);
		int maxBlackCards = (blackCardCount < cardService.getDictionaryMinBlackCards() ?
				cardService.getDictionaryMinBlackCards() : cardService.getDictionaryMaxBlackCards());
		return MessageFormat.format(DictionariesBotResponseMessageI18n.CARDS_BLACK_CARD_ADDED, blackCardCount, maxBlackCards);
	}

	private String getCollaboratorMenuMessage(Dictionary dictionary) {
		return MessageFormat.format(DictionariesBotResponseMessageI18n.COLLABORATORS_MENU, dictionary.getName());
	}

	private String getAddedCollaboratorMessage(DictionaryCollaborator collaborator) {
		return MessageFormat.format(DictionariesBotResponseMessageI18n.COLLABORATORS_ADDED, collaborator.getUser().getName());
	}

	private String getCollaboratorAcceptedMessage(String name) {
		return MessageFormat.format(DictionariesBotResponseMessageI18n.COLLABORATORS_ACCEPTED_CREATOR_MESSAGE, name);
	}

	private String getCollaboratorRejectedMessage(String name) {
		return MessageFormat.format(DictionariesBotResponseMessageI18n.COLLABORATORS_REJECTED_CREATOR_MESSAGE, name);
	}

	private String getToggledCollaboratorMessage(DictionaryCollaborator collaborator) {
		if (Boolean.TRUE.equals(collaborator.getCanEdit()))
			return MessageFormat.format(DictionariesBotResponseMessageI18n.COLLABORATORS_TOGGLED_ON, collaborator.getUser().getName());
		else
			return MessageFormat.format(DictionariesBotResponseMessageI18n.COLLABORATORS_TOGGLED_OFF, collaborator.getUser().getName());
	}

	private String getDeleteSharedErrorMessage() {
		return MessageFormat.format(DictionariesBotResponseErrorI18n.DICTIONARY_SHARED, getBotCreatorName());
	}

	private String getAddWhiteCardLengthExceededErrorMessage() {
		return MessageFormat.format(DictionariesBotResponseErrorI18n.CARD_EXCEEDED_LENGTH, cardService.getDictionaryWhiteCardMaxLength());
	}

	private String getAddBlackCardLengthExceededErrorMessage() {
		return MessageFormat.format(DictionariesBotResponseErrorI18n.CARD_EXCEEDED_LENGTH, cardService.getDictionaryBlackCardMaxLength());
	}

	private static String getDictionaryPublishedMessage(Dictionary dictionary) {
		String msg;
		if (Boolean.TRUE.equals(dictionary.getPublished())) {
			msg = MessageFormat.format(DictionariesBotResponseMessageI18n.DICTIONARY_TOGGLED_ON, dictionary.getName());
		} else {
			msg = MessageFormat.format(DictionariesBotResponseMessageI18n.DICTIONARY_TOGGLED_OFF, dictionary.getName());
		}
		return msg;
	}

	private String getDictionaryAlreadySharedError() {
		return MessageFormat.format(DictionariesBotResponseErrorI18n.DICTIONARY_ALREADY_SHARED, getBotCreatorName());
	}

	private String getHelpMessage() {
		return MessageFormat.format(DictionariesBotResponseMessageI18n.HELP, getBotName(), getBotVersion(),
				getBotHelpURL(), getBotCreatorName());
	}

	private Dictionary getDictionaryAndCheckCreator(long userId, long dictionaryId) {
		Dictionary dictionary = dictionaryService.getDictionaryById(dictionaryId);

		if (dictionary == null) {
			throw new DictionaryDoesntExistsException();
		}

		if (dictionary.getCreator().getId() != userId) {
			throw new DictionaryNotYoursException();
		}

		return dictionary;
	}

	private Dictionary getDictionaryAndCheckCollaborator(long userId, long dictionaryId) {
		Dictionary dictionary = dictionaryService.getDictionaryById(dictionaryId);

		if (dictionary == null) {
			throw new DictionaryDoesntExistsException();
		}

		if (!dictionaryService.isDictionaryCollaborator(dictionary, userService.getById(userId))) {
			throw new DictionaryNotYoursException();
		}

		return dictionary;
	}

	private Dictionary getDictionaryAndCheckActiveCollaborator(long userId, long dictionaryId) {
		Dictionary dictionary = dictionaryService.getDictionaryById(dictionaryId);

		if (dictionary == null) {
			throw new DictionaryDoesntExistsException();
		}

		if (!dictionaryService.isDictionaryActiveCollaborator(dictionary, userService.getById(userId))) {
			throw new DictionaryNotYoursException();
		}

		return dictionary;
	}

	private void checkDictionaryPublishedAndShared(Dictionary dictionary) {
		if (dictionary.getPublished() || dictionary.getShared()) {
			throw new DictionaryAlreadyPublishedException();
		}
	}

	private User getUserByNameOrId(String nameOrId) {
		User user;
		if (StringUtils.isNumeric(nameOrId)) {
			user = userService.getById(Long.parseLong(nameOrId));
		} else {
			user = userService.getByUsername(nameOrId);
		}

		if (user == null)
			throw new UserDoesntExistsException();

		return user;
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
