package org.themarioga.cclh.bot.dictionaries.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.themarioga.cclh.bot.dictionaries.service.intf.DictionariesBotMessageService;
import org.themarioga.cclh.bot.dictionaries.service.intf.DictionariesBotService;
import org.themarioga.cclh.bot.security.CCLHSecurityUtils;
import org.themarioga.cclh.bot.security.CCLHUserRole;
import org.themarioga.cclh.bot.service.intf.I18NService;
import org.themarioga.cclh.bot.util.StringUtils;
import org.themarioga.cclh.commons.enums.CardTypeEnum;
import org.themarioga.cclh.commons.exceptions.ApplicationException;
import org.themarioga.cclh.commons.exceptions.card.CardAlreadyExistsException;
import org.themarioga.cclh.commons.exceptions.card.CardDoesntExistsException;
import org.themarioga.cclh.commons.exceptions.card.CardTextExcededLength;
import org.themarioga.cclh.commons.exceptions.dictionary.*;
import org.themarioga.cclh.commons.exceptions.user.UserAlreadyExistsException;
import org.themarioga.cclh.commons.exceptions.user.UserDoesntExistsException;
import org.themarioga.cclh.commons.models.*;
import org.themarioga.cclh.commons.services.intf.*;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Service
public class DictionariesBotServiceImpl implements DictionariesBotService {

	private final Logger logger = LoggerFactory.getLogger(DictionariesBotServiceImpl.class);

	private DictionariesBotMessageService dictionariesBotMessageService;
	private UserService userService;
	private CardService cardService;
	private DictionaryService dictionaryService;
	private I18NService i18NService;
	private ConfigurationService configurationService;

	@Override
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
	public void registerUser(long userId, String username, String lang) {
		try {
			userService.createOrReactivate(userId, username, i18NService.getLanguage(lang));

			dictionariesBotMessageService.sendMessage(userId,  i18NService.get("PLAYER_WELCOME"));
		} catch (UserAlreadyExistsException e) {
			logger.warn("El usuario {} ({}) ya estaba registrado en el otro bot.", userId, username);

			dictionariesBotMessageService.sendMessage(userId,  i18NService.get("PLAYER_WELCOME"));

			throw e;
		} catch (ApplicationException e) {
			dictionariesBotMessageService.sendMessage(userId, e.getMessage());

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

		dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("USER_LANG_CHANGE"),
				keyboardBuilder.build());
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
	public void changeUserLanguage(int messageId, String lang) {
		if (!CCLHSecurityUtils.isLoggedIn()) throw new UserDoesntExistsException();

		CCLHSecurityUtils.setUserDetails(
				userService.setLanguage(userService.getById(CCLHSecurityUtils.getId()), i18NService.getLanguage(lang)),
				CCLHSecurityUtils.getId() == getBotCreatorId() ? CCLHUserRole.ADMIN : CCLHUserRole.USER);

		dictionariesBotMessageService.deleteMessage(CCLHSecurityUtils.getId(), messageId);
		dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("USER_LANG_CHANGED"));
	}

	@Override
	@Transactional(propagation = Propagation.SUPPORTS, rollbackFor = ApplicationException.class)
	public void mainMenu() {
		if (!CCLHSecurityUtils.isLoggedIn()) throw new UserDoesntExistsException();

		InlineKeyboardMarkup privateInlineKeyboard = getMainMenuKeyboard();

		dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("DICTIONARIES_MAIN_MENU"),
				privateInlineKeyboard);
	}

	@Override
	@Transactional(propagation = Propagation.SUPPORTS, rollbackFor = ApplicationException.class)
	public void mainMenu(int messageId) {
		if (!CCLHSecurityUtils.isLoggedIn()) throw new UserDoesntExistsException();

		InlineKeyboardMarkup privateInlineKeyboard = getMainMenuKeyboard();

		dictionariesBotMessageService.editMessage(CCLHSecurityUtils.getId(), messageId, i18NService.get("DICTIONARIES_MAIN_MENU"),
				privateInlineKeyboard);
	}

	@Override
	@Transactional(propagation = Propagation.SUPPORTS, rollbackFor = ApplicationException.class)
	public void listDictionaries(int messageId) {
		if (!CCLHSecurityUtils.isLoggedIn()) throw new UserDoesntExistsException();

		List<Dictionary> dictionaryList =
				dictionaryService.getDictionariesByCreatorOrCollaborator(userService.getById(CCLHSecurityUtils.getId()));

		InlineKeyboardMarkup privateInlineKeyboard = InlineKeyboardMarkup.builder()
				.keyboardRow(Collections.singletonList(InlineKeyboardButton.builder()
						.text(i18NService.get("GO_BACK")).callbackData("menu").build()))
				.build();
		dictionariesBotMessageService.editMessage(CCLHSecurityUtils.getId(), messageId, getDictionaryListMessage(dictionaryList),
				privateInlineKeyboard);
	}

	@Override
	@Transactional(propagation = Propagation.SUPPORTS, rollbackFor = ApplicationException.class)
	public void createDictionaryMessage(int messageId) {
		if (!CCLHSecurityUtils.isLoggedIn()) throw new UserDoesntExistsException();

		dictionariesBotMessageService.deleteMessage(CCLHSecurityUtils.getId(), messageId);
		dictionariesBotMessageService.setPendingReply(CCLHSecurityUtils.getId(), "/create");
		dictionariesBotMessageService.sendMessageWithForceReply(CCLHSecurityUtils.getId(),
				i18NService.get("DICTIONARY_CREATE"));
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
	public void createDictionary(String name) {
		if (!CCLHSecurityUtils.isLoggedIn()) throw new UserDoesntExistsException();

		try {
			dictionaryService.create(name, userService.getById(CCLHSecurityUtils.getId()));

			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("DICTIONARY_CREATED"));
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("DICTIONARIES_MAIN_MENU"),
					getMainMenuKeyboard());
		} catch (DictionaryAlreadyExistsException e) {
			logger.error("Ya existe un diccionario con el nombre {}", name);

			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("ERROR_DICTIONARY_ALREADY_EXISTS"));

			throw e;
		} catch (ApplicationException e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), e.getMessage());

			throw e;
		}
	}

	@Override
	@Transactional(propagation = Propagation.SUPPORTS, rollbackFor = ApplicationException.class)
	public void renameDictionaryMessage(int messageId) {
		if (!CCLHSecurityUtils.isLoggedIn()) throw new UserDoesntExistsException();

		List<Dictionary> dictionaryList = dictionaryService.getDictionariesByCreator(userService.getById(CCLHSecurityUtils.getId()));

		dictionariesBotMessageService.deleteMessage(CCLHSecurityUtils.getId(), messageId);
		dictionariesBotMessageService.setPendingReply(CCLHSecurityUtils.getId(), "/rename_select");
		dictionariesBotMessageService.sendMessageWithForceReply(CCLHSecurityUtils.getId(), getDictionaryRenameMessage(dictionaryList));
	}

	@Override
	@Transactional(propagation = Propagation.SUPPORTS, rollbackFor = ApplicationException.class)
	public void selectDictionaryToRename(int messageId, long dictionaryId) {
		if (!CCLHSecurityUtils.isLoggedIn()) throw new UserDoesntExistsException();

		try {
			Dictionary dictionary = getDictionaryAndCheckCreator(dictionaryId);

			if (Boolean.TRUE.equals(dictionary.getShared()))
				throw new DictionaryAlreadySharedException();

			dictionariesBotMessageService.deleteMessage(CCLHSecurityUtils.getId(), messageId);
			dictionariesBotMessageService.setPendingReply(CCLHSecurityUtils.getId(), "/rename__" + dictionary.getId());
			dictionariesBotMessageService.sendMessageWithForceReply(CCLHSecurityUtils.getId(),
					i18NService.get("DICTIONARY_RENAME"));
		} catch (DictionaryDoesntExistsException e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("ERROR_DICTIONARY_NOT_FOUND"));
		} catch (DictionaryNotYoursException e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("ERROR_DICTIONARY_NOT_YOURS"));
		} catch (DictionaryAlreadySharedException e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), getDictionaryAlreadySharedError());
		}
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
	public void renameDictionary(long dictionaryId, String newName) {
		if (!CCLHSecurityUtils.isLoggedIn()) throw new UserDoesntExistsException();

		try {
			Dictionary dictionary = getDictionaryAndCheckCreator(dictionaryId);

			dictionaryService.setName(dictionary, newName);

			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("DICTIONARY_RENAMED"));
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("DICTIONARIES_MAIN_MENU"),
					getMainMenuKeyboard());
		} catch (DictionaryDoesntExistsException e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("ERROR_DICTIONARY_NOT_FOUND"));
		} catch (DictionaryNotYoursException e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("ERROR_DICTIONARY_NOT_YOURS"));
		}
	}

	@Override
	@Transactional(propagation = Propagation.SUPPORTS, rollbackFor = ApplicationException.class)
	public void changeDictionaryLangMessage(int messageId) {
		if (!CCLHSecurityUtils.isLoggedIn()) throw new UserDoesntExistsException();

		List<Dictionary> dictionaryList = dictionaryService.getDictionariesByCreator(userService.getById(CCLHSecurityUtils.getId()));

		dictionariesBotMessageService.deleteMessage(CCLHSecurityUtils.getId(), messageId);
		dictionariesBotMessageService.setPendingReply(CCLHSecurityUtils.getId(), "/change_lang_select");
		dictionariesBotMessageService.sendMessageWithForceReply(CCLHSecurityUtils.getId(), getChangeDictionaryLangMessage(dictionaryList));
	}

	@Override
	public void selectDictionaryToChangeLang(int messageId, long dictionaryId) {
		if (!CCLHSecurityUtils.isLoggedIn()) throw new UserDoesntExistsException();

		try {
			Dictionary dictionary = getDictionaryAndCheckCreator(dictionaryId);

			if (Boolean.TRUE.equals(dictionary.getShared()))
				throw new DictionaryAlreadySharedException();

			dictionariesBotMessageService.deleteMessage(CCLHSecurityUtils.getId(), messageId);
			dictionariesBotMessageService.setPendingReply(CCLHSecurityUtils.getId(),
					"/change_lang__" + dictionary.getId());

			dictionariesBotMessageService.sendMessageWithForceReply(CCLHSecurityUtils.getId(), changeDictionariesLanguageListMessage());
		} catch (DictionaryDoesntExistsException e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("ERROR_DICTIONARY_NOT_FOUND"));
		} catch (DictionaryNotYoursException e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("ERROR_DICTIONARY_NOT_YOURS"));
		} catch (DictionaryAlreadySharedException e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), getDictionaryAlreadySharedError());
		}
	}

	@Override
	public void changeDictionaryLang(long dictionaryId, String language) {
		if (!CCLHSecurityUtils.isLoggedIn()) throw new UserDoesntExistsException();

		try {
			Dictionary dictionary = getDictionaryAndCheckCreator(dictionaryId);

			dictionaryService.setLanguage(dictionary, i18NService.getLanguage(language));

			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("DICTIONARY_LANG_CHANGED"));
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("DICTIONARIES_MAIN_MENU"),
					getMainMenuKeyboard());
		} catch (DictionaryDoesntExistsException e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("ERROR_DICTIONARY_NOT_FOUND"));
		} catch (DictionaryNotYoursException e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("ERROR_DICTIONARY_NOT_YOURS"));
		}
	}

	@Override
	@Transactional(propagation = Propagation.SUPPORTS, rollbackFor = ApplicationException.class)
	public void deleteDictionaryMessage(int messageId) {
		if (!CCLHSecurityUtils.isLoggedIn()) throw new UserDoesntExistsException();

		List<Dictionary> dictionaryList = dictionaryService.getDictionariesByCreator(userService.getById(CCLHSecurityUtils.getId()));

		dictionariesBotMessageService.deleteMessage(CCLHSecurityUtils.getId(), messageId);
		dictionariesBotMessageService.setPendingReply(CCLHSecurityUtils.getId(), "/delete_select");
		dictionariesBotMessageService.sendMessageWithForceReply(CCLHSecurityUtils.getId(), getDictionaryDeleteMessage(dictionaryList));
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
	public void selectDictionaryToDelete(int messageId, long dictionaryId) {
		if (!CCLHSecurityUtils.isLoggedIn()) throw new UserDoesntExistsException();

		try {
			Dictionary dictionary = getDictionaryAndCheckCreator(dictionaryId);

			if (Boolean.TRUE.equals(dictionary.getShared())) {
				dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), getDeleteSharedErrorMessage());

				return;
			}

			if (Boolean.TRUE.equals(dictionary.getPublished())) {
				dictionariesBotMessageService.deleteMessage(CCLHSecurityUtils.getId(), messageId);
				dictionariesBotMessageService.setPendingReply(CCLHSecurityUtils.getId(), "/delete__" + dictionaryId);
				dictionariesBotMessageService.sendMessageWithForceReply(CCLHSecurityUtils.getId(),
						i18NService.get("DICTIONARY_DELETE"));
			} else {
				dictionaryService.delete(dictionary);

				dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("DICTIONARY_DELETED"));
				dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("DICTIONARIES_MAIN_MENU"),
						getMainMenuKeyboard());
			}
		} catch (DictionaryDoesntExistsException e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("ERROR_DICTIONARY_NOT_FOUND"));
		} catch (DictionaryNotYoursException e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("ERROR_DICTIONARY_NOT_YOURS"));
		}
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
	public void deleteDictionary(long dictionaryId, String text) {
		if (!CCLHSecurityUtils.isLoggedIn()) throw new UserDoesntExistsException();

		try {
			Dictionary dictionary = getDictionaryAndCheckCreator(dictionaryId);

			if (Boolean.TRUE.equals(dictionary.getShared())) {
				dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), getDeleteSharedErrorMessage());

				return;
			}

			if (text == null || text.isBlank() || !text.equals("SI")) {
				dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(),
						i18NService.get("DICTIONARY_DELETE_CANCELLED"));
			} else {
				dictionaryService.delete(dictionary);

				dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("DICTIONARY_DELETED"));
				dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("DICTIONARIES_MAIN_MENU"),
						getMainMenuKeyboard());
			}
		} catch (DictionaryDoesntExistsException e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("ERROR_DICTIONARY_NOT_FOUND"));
		} catch (DictionaryNotYoursException e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("ERROR_DICTIONARY_NOT_YOURS"));
		}
	}

	@Override
	@Transactional(propagation = Propagation.SUPPORTS, rollbackFor = ApplicationException.class)
	public void toggleDictionaryMessage(int messageId) {
		if (!CCLHSecurityUtils.isLoggedIn()) throw new UserDoesntExistsException();

		List<Dictionary> dictionaryList = dictionaryService.getDictionariesByCreator(userService.getById(CCLHSecurityUtils.getId()));

		dictionariesBotMessageService.deleteMessage(CCLHSecurityUtils.getId(), messageId);
		dictionariesBotMessageService.setPendingReply(CCLHSecurityUtils.getId(), "/toggle_select");
		dictionariesBotMessageService.sendMessageWithForceReply(CCLHSecurityUtils.getId(), getDictionaryToggleMessage(dictionaryList));
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
	public void toggleDictionary(int messageId, long dictionaryId) {
		if (!CCLHSecurityUtils.isLoggedIn()) throw new UserDoesntExistsException();

		try {
			Dictionary dictionary = getDictionaryAndCheckCreator(dictionaryId);

			if (Boolean.TRUE.equals(dictionary.getShared())) {
				dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), getDeleteSharedErrorMessage());

				return;
			}

			dictionaryService.togglePublished(dictionary);

			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), getDictionaryPublishedMessage(dictionary));
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("DICTIONARIES_MAIN_MENU"),
					getMainMenuKeyboard());
		} catch (DictionaryDoesntExistsException e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("ERROR_DICTIONARY_NOT_FOUND"));
		} catch (DictionaryNotYoursException e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("ERROR_DICTIONARY_NOT_YOURS"));
		} catch (DictionaryAlreadySharedException e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), getDictionaryAlreadySharedError());
		} catch (DictionaryNotCompletedException e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("ERROR_DICTIONARY_NOT_FILLED"));
		}
	}

	@Override
	@Transactional(propagation = Propagation.SUPPORTS, rollbackFor = ApplicationException.class)
	public void shareDictionaryMessage(int messageId) {
		if (!CCLHSecurityUtils.isLoggedIn()) throw new UserDoesntExistsException();

		List<Dictionary> dictionaryList = dictionaryService.getDictionariesByCreator(userService.getById(CCLHSecurityUtils.getId()));

		dictionariesBotMessageService.deleteMessage(CCLHSecurityUtils.getId(), messageId);
		dictionariesBotMessageService.setPendingReply(CCLHSecurityUtils.getId(), "/share_select");
		dictionariesBotMessageService.sendMessageWithForceReply(CCLHSecurityUtils.getId(), getDictionaryRequestShareMessage(dictionaryList));
	}

	@Override
	@Transactional(propagation = Propagation.SUPPORTS, rollbackFor = ApplicationException.class)
	public void requestShareDictionary(int messageId, long dictionaryId) {
		if (!CCLHSecurityUtils.isLoggedIn()) throw new UserDoesntExistsException();

		try {
			Dictionary dictionary = getDictionaryAndCheckCreator(dictionaryId);

			if (Boolean.FALSE.equals(dictionary.getPublished())) {
				dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(),
						i18NService.get("DICTIONARY_NOT_PUBLISHED"));

				return;
			}

			sendCardList(dictionary, CardTypeEnum.BLACK);
			sendCardList(dictionary, CardTypeEnum.WHITE);

			InlineKeyboardMarkup shareInlineKeyboard = InlineKeyboardMarkup.builder()
					.keyboardRow(Collections.singletonList(InlineKeyboardButton.builder().text(i18NService.get("ACCEPT"))
							.callbackData("share_accept__" + dictionaryId).build()))
					.keyboardRow(Collections.singletonList(InlineKeyboardButton.builder().text(i18NService.get("CANCEL"))
							.callbackData("share_decline__" + dictionaryId).build()))
					.build();
			dictionariesBotMessageService.sendMessage(getBotCreatorId(), getDictionaryRequestShareMessage(dictionary),
					shareInlineKeyboard);

			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), getDictionaryRequestShareMessage(dictionary));
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("DICTIONARIES_MAIN_MENU"),
					getMainMenuKeyboard());
		} catch (DictionaryDoesntExistsException e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("ERROR_DICTIONARY_NOT_FOUND"));
		} catch (DictionaryNotYoursException e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("ERROR_DICTIONARY_NOT_YOURS"));
		} catch (DictionaryAlreadySharedException e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), getDictionaryAlreadySharedError());
		} catch (DictionaryNotCompletedException e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("ERROR_DICTIONARY_NOT_FILLED"));
		}
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
	public void acceptShareDictionary(int messageId, long dictionaryId) {
		if (!CCLHSecurityUtils.isLoggedIn()) throw new UserDoesntExistsException();

		try {
			Dictionary dictionary = getDictionaryAndCheckBotCreator(dictionaryId);

			dictionaryService.toggleShared(dictionary);

			dictionariesBotMessageService.editMessage(CCLHSecurityUtils.getId(), messageId, getDictionaryShareMessage(dictionary));

			dictionariesBotMessageService.sendMessage(dictionary.getCreator().getId(), getDictionaryShareMessage(dictionary));
		} catch (DictionaryDoesntExistsException e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("ERROR_DICTIONARY_NOT_FOUND"));
		} catch (DictionaryNotYoursException e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("ERROR_DICTIONARY_NOT_YOURS"));
		}
	}

	@Override
	@Transactional(propagation = Propagation.SUPPORTS, rollbackFor = ApplicationException.class)
	public void rejectShareDictionary(int messageId, long dictionaryId) {
		if (!CCLHSecurityUtils.isLoggedIn()) throw new UserDoesntExistsException();

		try {
			Dictionary dictionary = getDictionaryAndCheckBotCreator(dictionaryId);

			dictionariesBotMessageService.editMessage(CCLHSecurityUtils.getId(), messageId, getShareDictionaryRejectedMessage(dictionary));

			dictionariesBotMessageService.sendMessage(dictionary.getCreator().getId(), getShareDictionaryRejectedMessage(dictionary));
		} catch (DictionaryDoesntExistsException e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("ERROR_DICTIONARY_NOT_FOUND"));
		} catch (DictionaryNotYoursException e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("ERROR_DICTIONARY_NOT_YOURS"));
		}
	}

	@Override
	@Transactional(propagation = Propagation.SUPPORTS, rollbackFor = ApplicationException.class)
	public void manageCardsMessage(int messageId) {
		if (!CCLHSecurityUtils.isLoggedIn()) throw new UserDoesntExistsException();

		List<Dictionary> dictionaryList =
				dictionaryService.getDictionariesByCreatorOrCollaborator(userService.getById(CCLHSecurityUtils.getId()));

		dictionariesBotMessageService.deleteMessage(CCLHSecurityUtils.getId(), messageId);
		dictionariesBotMessageService.setPendingReply(CCLHSecurityUtils.getId(), "/manage_cards_select");
		dictionariesBotMessageService.sendMessageWithForceReply(CCLHSecurityUtils.getId(), getManageCardsMessage(dictionaryList));
	}

	@Override
	@Transactional(propagation = Propagation.SUPPORTS, rollbackFor = ApplicationException.class)
	public void selectDictionaryToManageCards(Integer messageId, long dictionaryId) {
		if (!CCLHSecurityUtils.isLoggedIn()) throw new UserDoesntExistsException();

		try {
			Dictionary dictionary = getDictionaryAndCheckActiveCollaborator(dictionaryId);

			checkDictionaryPublishedAndShared(dictionary);

			if (messageId == null) {
				dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), getCardMenuMessage(dictionary),
						getCardKeyboardMenu(dictionaryId));
			} else {
				dictionariesBotMessageService.editMessage(CCLHSecurityUtils.getId(), messageId, getCardMenuMessage(dictionary),
						getCardKeyboardMenu(dictionaryId));
			}
		} catch (DictionaryDoesntExistsException e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("ERROR_DICTIONARY_NOT_FOUND"));
		} catch (DictionaryNotYoursException e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("ERROR_DICTIONARY_NOT_YOURS"));
		} catch (DictionaryAlreadyPublishedException e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), getDeleteSharedErrorMessage());
		}
	}

	@Override
	@Transactional(propagation = Propagation.SUPPORTS, rollbackFor = ApplicationException.class)
	public void listWhiteCardsMessage(int messageId, long dictionaryId) {
		if (!CCLHSecurityUtils.isLoggedIn()) throw new UserDoesntExistsException();

		try {
			Dictionary dictionary = getDictionaryAndCheckActiveCollaborator(dictionaryId);

			checkDictionaryPublishedAndShared(dictionary);

			dictionariesBotMessageService.editMessage(CCLHSecurityUtils.getId(), messageId,
					i18NService.get("CARDS_WHITE_LIST"));

			sendCardList(dictionary, CardTypeEnum.WHITE);

			InlineKeyboardMarkup privateInlineKeyboard = InlineKeyboardMarkup.builder()
					.keyboardRow(Collections.singletonList(InlineKeyboardButton.builder()
							.text(i18NService.get("GO_BACK"))
							.callbackData("manage_cards_select__" + dictionaryId).build()))
					.build();

			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("CARDS_WHITE_LIST_END"),
					privateInlineKeyboard);
		} catch (DictionaryDoesntExistsException e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("ERROR_DICTIONARY_NOT_FOUND"));
		} catch (DictionaryNotYoursException e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("ERROR_DICTIONARY_NOT_YOURS"));
		} catch (DictionaryAlreadyPublishedException e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), getDeleteSharedErrorMessage());
		}
	}

	@Override
	@Transactional(propagation = Propagation.SUPPORTS, rollbackFor = ApplicationException.class)
	public void addWhiteCardsMessage(int messageId, long dictionaryId) {
		if (!CCLHSecurityUtils.isLoggedIn()) throw new UserDoesntExistsException();

		try {
			Dictionary dictionary = getDictionaryAndCheckActiveCollaborator(dictionaryId);

			checkDictionaryPublishedAndShared(dictionary);

			dictionariesBotMessageService.deleteMessage(CCLHSecurityUtils.getId(), messageId);
			dictionariesBotMessageService.setPendingReply(CCLHSecurityUtils.getId(), "/add_white_card__" + dictionaryId);
			dictionariesBotMessageService.sendMessageWithForceReply(CCLHSecurityUtils.getId(),
					i18NService.get("CARDS_WHITE_CARD_ADD"));
		} catch (DictionaryDoesntExistsException e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("ERROR_DICTIONARY_NOT_FOUND"));
		} catch (DictionaryNotYoursException e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("ERROR_DICTIONARY_NOT_YOURS"));
		} catch (DictionaryAlreadyPublishedException e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), getDeleteSharedErrorMessage());
		}
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
	public void addWhiteCard(long dictionaryId, String text) {
		if (!CCLHSecurityUtils.isLoggedIn()) throw new UserDoesntExistsException();

		try {
			Dictionary dictionary = getDictionaryAndCheckActiveCollaborator(dictionaryId);

			checkDictionaryPublishedAndShared(dictionary);

			if (!text.equals(":cancel:")) {
				cardService.create(dictionary, CardTypeEnum.WHITE, text);

				dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), getWhiteCardAddedMessage(dictionary));

				dictionariesBotMessageService.setPendingReply(CCLHSecurityUtils.getId(), "/add_white_card__" + dictionaryId);
				dictionariesBotMessageService.sendMessageWithForceReply(CCLHSecurityUtils.getId(),
						i18NService.get("CARDS_WHITE_CARD_ADD_ANOTHER"));
			} else {
				dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), getCardMenuMessage(dictionary),
						getCardKeyboardMenu(dictionaryId));
			}
		} catch (DictionaryDoesntExistsException e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("ERROR_DICTIONARY_NOT_FOUND"));
		} catch (DictionaryNotYoursException e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("ERROR_DICTIONARY_NOT_YOURS"));
		} catch (DictionaryAlreadyPublishedException e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), getDeleteSharedErrorMessage());
		} catch (CardAlreadyExistsException e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("ERROR_CARD_ALREADY_EXISTS"));
		} catch (CardTextExcededLength e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), getAddWhiteCardLengthExceededErrorMessage());
		} catch (DictionaryAlreadyFilledException e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("ERROR_DICTIONARY_ALREADY_FILLED"));
		}
	}

	@Override
	@Transactional(propagation = Propagation.SUPPORTS, rollbackFor = ApplicationException.class)
	public void editWhiteCardsMessage(int messageId, long dictionaryId) {
		if (!CCLHSecurityUtils.isLoggedIn()) throw new UserDoesntExistsException();

		try {
			Dictionary dictionary = getDictionaryAndCheckActiveCollaborator(dictionaryId);

			checkDictionaryPublishedAndShared(dictionary);

			dictionariesBotMessageService.editMessage(CCLHSecurityUtils.getId(), messageId,
					i18NService.get("CARDS_WHITE_LIST"));

			sendCardList(dictionary, CardTypeEnum.WHITE);

			dictionariesBotMessageService.setPendingReply(CCLHSecurityUtils.getId(), "/edit_white_card_sel__" + dictionaryId);
			dictionariesBotMessageService.sendMessageWithForceReply(CCLHSecurityUtils.getId(),
					i18NService.get("CARDS_WHITE_CARD_EDIT"));
		} catch (DictionaryDoesntExistsException e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("ERROR_DICTIONARY_NOT_FOUND"));
		} catch (DictionaryNotYoursException e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("ERROR_DICTIONARY_NOT_YOURS"));
		} catch (DictionaryAlreadyPublishedException e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), getDeleteSharedErrorMessage());
		}
	}

	@Override
	@Transactional(propagation = Propagation.SUPPORTS, rollbackFor = ApplicationException.class)
	public void editWhiteCardSelect(long dictionaryId, long cardId) {
		if (!CCLHSecurityUtils.isLoggedIn()) throw new UserDoesntExistsException();

		try {
			Dictionary dictionary = getDictionaryAndCheckActiveCollaborator(dictionaryId);

			checkDictionaryPublishedAndShared(dictionary);

			dictionariesBotMessageService.setPendingReply(CCLHSecurityUtils.getId(), "/edit_white_card__" + cardId);
			dictionariesBotMessageService.sendMessageWithForceReply(CCLHSecurityUtils.getId(),
					i18NService.get("CARDS_WHITE_CARD_EDIT_NEW_TEXT"));
		} catch (DictionaryDoesntExistsException e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("ERROR_DICTIONARY_NOT_FOUND"));
		} catch (DictionaryNotYoursException e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("ERROR_DICTIONARY_NOT_YOURS"));
		} catch (DictionaryAlreadyPublishedException e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), getDeleteSharedErrorMessage());
		}
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
	public void editWhiteCard(long cardId, String newText) {
		if (!CCLHSecurityUtils.isLoggedIn()) throw new UserDoesntExistsException();

		try {
			Card card = cardService.getCardById(cardId);

			if (card.getId() == null)
				throw new CardDoesntExistsException();

			if (!dictionaryService.isDictionaryActiveCollaborator(card.getDictionary(), userService.getById(CCLHSecurityUtils.getId())))
				throw new DictionaryNotYoursException();

			if (Boolean.TRUE.equals(card.getDictionary().getShared()))
				throw new DictionaryAlreadySharedException();

			cardService.changeText(card, newText);

			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("CARDS_WHITE_CARD_EDITED"));
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), getCardMenuMessage(card.getDictionary()), getCardKeyboardMenu(card.getDictionary().getId()));
		} catch (CardDoesntExistsException e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("ERROR_CARD_DOESNT_EXISTS"));
		} catch (CardAlreadyExistsException e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("ERROR_CARD_ALREADY_EXISTS"));
		} catch (DictionaryNotYoursException e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("ERROR_CARD_NOT_YOURS"));
		} catch (DictionaryAlreadySharedException e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("ERROR_DICTIONARY_SHARED"));
		} catch (CardTextExcededLength e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("ERROR_CARD_EXCEEDED_LENGTH"));
		}
	}

	@Override
	@Transactional(propagation = Propagation.SUPPORTS, rollbackFor = ApplicationException.class)
	public void deleteWhiteCardsMessage(int messageId, long dictionaryId) {
		if (!CCLHSecurityUtils.isLoggedIn()) throw new UserDoesntExistsException();

		try {
			Dictionary dictionary = getDictionaryAndCheckActiveCollaborator(dictionaryId);

			checkDictionaryPublishedAndShared(dictionary);

			dictionariesBotMessageService.editMessage(CCLHSecurityUtils.getId(), messageId,
					i18NService.get("CARDS_WHITE_LIST"));

			sendCardList(dictionary, CardTypeEnum.WHITE);

			dictionariesBotMessageService.setPendingReply(CCLHSecurityUtils.getId(), "/delete_white_card__" + dictionaryId);
			dictionariesBotMessageService.sendMessageWithForceReply(CCLHSecurityUtils.getId(),
					i18NService.get("CARDS_WHITE_CARD_DELETE"));
		} catch (DictionaryDoesntExistsException e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("ERROR_DICTIONARY_NOT_FOUND"));
		} catch (DictionaryNotYoursException e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("ERROR_DICTIONARY_NOT_YOURS"));
		} catch (DictionaryAlreadyPublishedException e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), getDeleteSharedErrorMessage());
		}
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
	public void deleteWhiteCard(long dictionaryId, long cardId) {
		if (!CCLHSecurityUtils.isLoggedIn()) throw new UserDoesntExistsException();

		try {
			Dictionary dictionary = getDictionaryAndCheckActiveCollaborator(dictionaryId);

			checkDictionaryPublishedAndShared(dictionary);

			Card card = cardService.getCardById(cardId);

			if (!card.getDictionary().getId().equals(dictionary.getId()))
				throw new CardDoesntExistsException();

			cardService.delete(card);

			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("CARDS_WHITE_CARD_DELETED"));

			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), getCardMenuMessage(dictionary),
					getCardKeyboardMenu(dictionaryId));
		} catch (DictionaryDoesntExistsException e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("ERROR_DICTIONARY_NOT_FOUND"));
		} catch (DictionaryNotYoursException e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("ERROR_DICTIONARY_NOT_YOURS"));
		} catch (DictionaryAlreadyPublishedException e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), getDeleteSharedErrorMessage());
		} catch (CardDoesntExistsException e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("ERROR_CARD_NOT_YOURS"));
		}
	}

	@Override
	@Transactional(propagation = Propagation.SUPPORTS, rollbackFor = ApplicationException.class)
	public void listBlackCardsMessage(int messageId, long dictionaryId) {
		if (!CCLHSecurityUtils.isLoggedIn()) throw new UserDoesntExistsException();

		try {
			Dictionary dictionary = getDictionaryAndCheckActiveCollaborator(dictionaryId);

			checkDictionaryPublishedAndShared(dictionary);

			dictionariesBotMessageService.editMessage(CCLHSecurityUtils.getId(), messageId,
					i18NService.get("CARDS_BLACK_LIST"));

			sendCardList(dictionary, CardTypeEnum.BLACK);

			InlineKeyboardMarkup privateInlineKeyboard = InlineKeyboardMarkup.builder()
					.keyboardRow(Collections.singletonList(InlineKeyboardButton.builder()
							.text(i18NService.get("GO_BACK"))
							.callbackData("manage_cards_select__" + dictionaryId).build()))
					.build();

			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("CARDS_BLACK_LIST_END"),
					privateInlineKeyboard);
		} catch (DictionaryDoesntExistsException e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("ERROR_DICTIONARY_NOT_FOUND"));
		} catch (DictionaryNotYoursException e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("ERROR_DICTIONARY_NOT_YOURS"));
		} catch (DictionaryAlreadyPublishedException e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), getDeleteSharedErrorMessage());
		}
	}

	@Override
	@Transactional(propagation = Propagation.SUPPORTS, rollbackFor = ApplicationException.class)
	public void addBlackCardsMessage(int messageId, long dictionaryId) {
		if (!CCLHSecurityUtils.isLoggedIn()) throw new UserDoesntExistsException();

		try {
			Dictionary dictionary = getDictionaryAndCheckActiveCollaborator(dictionaryId);

			checkDictionaryPublishedAndShared(dictionary);

			dictionariesBotMessageService.deleteMessage(CCLHSecurityUtils.getId(), messageId);
			dictionariesBotMessageService.setPendingReply(CCLHSecurityUtils.getId(), "/add_black_card__" + dictionaryId);
			dictionariesBotMessageService.sendMessageWithForceReply(CCLHSecurityUtils.getId(),
					i18NService.get("CARDS_BLACK_CARD_ADD"));
		} catch (DictionaryDoesntExistsException e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("ERROR_DICTIONARY_NOT_FOUND"));
		} catch (DictionaryNotYoursException e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("ERROR_DICTIONARY_NOT_YOURS"));
		} catch (DictionaryAlreadyPublishedException e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), getDeleteSharedErrorMessage());
		}
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
	public void addBlackCard(long dictionaryId, String text) {
		if (!CCLHSecurityUtils.isLoggedIn()) throw new UserDoesntExistsException();

		try {
			Dictionary dictionary = getDictionaryAndCheckActiveCollaborator(dictionaryId);

			checkDictionaryPublishedAndShared(dictionary);

			if (!text.equals(":cancel:")) {
				cardService.create(dictionary, CardTypeEnum.BLACK, text);

				dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), getBlackCardAddedMessage(dictionary));

				dictionariesBotMessageService.setPendingReply(CCLHSecurityUtils.getId(), "/add_black_card__" + dictionaryId);
				dictionariesBotMessageService.sendMessageWithForceReply(CCLHSecurityUtils.getId(),
						i18NService.get("CARDS_BLACK_CARD_ADD_ANOTHER"));
			} else {
				dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), getCardMenuMessage(dictionary),
						getCardKeyboardMenu(dictionaryId));
			}
		} catch (DictionaryDoesntExistsException e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("ERROR_DICTIONARY_NOT_FOUND"));
		} catch (DictionaryNotYoursException e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("ERROR_DICTIONARY_NOT_YOURS"));
		} catch (DictionaryAlreadyPublishedException e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), getDeleteSharedErrorMessage());
		} catch (CardAlreadyExistsException e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("ERROR_CARD_ALREADY_EXISTS"));
		} catch (CardTextExcededLength e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), getAddBlackCardLengthExceededErrorMessage());
		}
	}

	@Override
	@Transactional(propagation = Propagation.SUPPORTS, rollbackFor = ApplicationException.class)
	public void editBlackCardsMessage(int messageId, long dictionaryId) {
		if (!CCLHSecurityUtils.isLoggedIn()) throw new UserDoesntExistsException();

		try {
			Dictionary dictionary = getDictionaryAndCheckActiveCollaborator(dictionaryId);

			checkDictionaryPublishedAndShared(dictionary);

			dictionariesBotMessageService.editMessage(CCLHSecurityUtils.getId(), messageId,
					i18NService.get("CARDS_BLACK_LIST"));

			sendCardList(dictionary, CardTypeEnum.BLACK);

			dictionariesBotMessageService.setPendingReply(CCLHSecurityUtils.getId(), "/edit_black_card_sel__" + dictionaryId);
			dictionariesBotMessageService.sendMessageWithForceReply(CCLHSecurityUtils.getId(),
					i18NService.get("CARDS_BLACK_CARD_EDIT"));
		} catch (DictionaryDoesntExistsException e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("ERROR_DICTIONARY_NOT_FOUND"));
		} catch (DictionaryNotYoursException e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("ERROR_DICTIONARY_NOT_YOURS"));
		} catch (DictionaryAlreadyPublishedException e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), getDeleteSharedErrorMessage());
		}
	}

	@Override
	@Transactional(propagation = Propagation.SUPPORTS, rollbackFor = ApplicationException.class)
	public void editBlackCardSelect(long dictionaryId, long cardId) {
		if (!CCLHSecurityUtils.isLoggedIn()) throw new UserDoesntExistsException();

		try {
			Dictionary dictionary = getDictionaryAndCheckActiveCollaborator(dictionaryId);

			if (dictionary.getPublished() || dictionary.getShared()) {
				throw new DictionaryAlreadyPublishedException();
			}

			dictionariesBotMessageService.setPendingReply(CCLHSecurityUtils.getId(), "/edit_black_card__" + cardId);
			dictionariesBotMessageService.sendMessageWithForceReply(CCLHSecurityUtils.getId(),
					i18NService.get("CARDS_BLACK_CARD_EDIT_NEW_TEXT"));
		} catch (DictionaryDoesntExistsException e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("ERROR_DICTIONARY_NOT_FOUND"));
		} catch (DictionaryNotYoursException e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("ERROR_DICTIONARY_NOT_YOURS"));
		} catch (DictionaryAlreadyPublishedException e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("ERROR_DICTIONARY_ALREADY_PUBLISHED"));
		}
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
	public void editBlackCard(long cardId, String newText) {
		if (!CCLHSecurityUtils.isLoggedIn()) throw new UserDoesntExistsException();

		try {
			Card card = cardService.getCardById(cardId);

			if (card.getId() == null)
				throw new CardDoesntExistsException();

			if (!dictionaryService.isDictionaryActiveCollaborator(card.getDictionary(), userService.getById(CCLHSecurityUtils.getId())))
				throw new DictionaryNotYoursException();

			if (Boolean.TRUE.equals(card.getDictionary().getShared()))
				throw new DictionaryAlreadySharedException();

			cardService.changeText(card, newText);

			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("CARDS_BLACK_CARD_EDITED"));
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), getCardMenuMessage(card.getDictionary()), getCardKeyboardMenu(card.getDictionary().getId()));
		} catch (CardDoesntExistsException e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("ERROR_CARD_DOESNT_EXISTS"));
		} catch (CardAlreadyExistsException e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("ERROR_CARD_ALREADY_EXISTS"));
		} catch (DictionaryNotYoursException e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("ERROR_CARD_NOT_YOURS"));
		} catch (DictionaryAlreadySharedException e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("ERROR_DICTIONARY_SHARED"));
		} catch (CardTextExcededLength e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("ERROR_CARD_EXCEEDED_LENGTH"));
		}
	}

	@Override
	@Transactional(propagation = Propagation.SUPPORTS, rollbackFor = ApplicationException.class)
	public void deleteBlackCardsMessage(int messageId, long dictionaryId) {
		if (!CCLHSecurityUtils.isLoggedIn()) throw new UserDoesntExistsException();

		try {
			Dictionary dictionary = getDictionaryAndCheckActiveCollaborator(dictionaryId);

			checkDictionaryPublishedAndShared(dictionary);

			dictionariesBotMessageService.editMessage(CCLHSecurityUtils.getId(), messageId,
					i18NService.get("CARDS_BLACK_LIST"));

			sendCardList(dictionary, CardTypeEnum.BLACK);

			dictionariesBotMessageService.setPendingReply(CCLHSecurityUtils.getId(), "/delete_black_card__" + dictionaryId);
			dictionariesBotMessageService.sendMessageWithForceReply(CCLHSecurityUtils.getId(),
					i18NService.get("CARDS_BLACK_CARD_DELETE"));
		} catch (DictionaryDoesntExistsException e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("ERROR_DICTIONARY_NOT_FOUND"));
		} catch (DictionaryNotYoursException e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("ERROR_DICTIONARY_NOT_YOURS"));
		} catch (DictionaryAlreadyPublishedException e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), getDeleteSharedErrorMessage());
		}
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
	public void deleteBlackCard(long dictionaryId, long cardId) {
		if (!CCLHSecurityUtils.isLoggedIn()) throw new UserDoesntExistsException();

		try {
			Dictionary dictionary = getDictionaryAndCheckActiveCollaborator(dictionaryId);

			checkDictionaryPublishedAndShared(dictionary);

			Card card = cardService.getCardById(cardId);

			if (!card.getDictionary().getId().equals(dictionary.getId()))
				throw new CardDoesntExistsException();

			cardService.delete(card);

			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("CARDS_BLACK_CARD_DELETED"));

			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), getCardMenuMessage(dictionary),
					getCardKeyboardMenu(dictionaryId));
		} catch (DictionaryDoesntExistsException e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("ERROR_DICTIONARY_NOT_FOUND"));
		} catch (DictionaryNotYoursException e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("ERROR_DICTIONARY_NOT_YOURS"));
		} catch (DictionaryAlreadyPublishedException e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), getDeleteSharedErrorMessage());
		} catch (CardDoesntExistsException e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("ERROR_CARD_NOT_YOURS"));
		}
	}

	@Override
	@Transactional(propagation = Propagation.SUPPORTS, rollbackFor = ApplicationException.class)
	public void manageCollaboratorsMessage(int messageId) {
		if (!CCLHSecurityUtils.isLoggedIn()) throw new UserDoesntExistsException();

		List<Dictionary> dictionaryList = dictionaryService.getDictionariesByCreator(userService.getById(CCLHSecurityUtils.getId()));

		dictionariesBotMessageService.deleteMessage(CCLHSecurityUtils.getId(), messageId);
		dictionariesBotMessageService.setPendingReply(CCLHSecurityUtils.getId(), "/manage_collabs_select");
		dictionariesBotMessageService.sendMessageWithForceReply(CCLHSecurityUtils.getId(), getManageCollaboratorsMessage(dictionaryList));
	}

	@Override
	@Transactional(propagation = Propagation.SUPPORTS, rollbackFor = ApplicationException.class)
	public void selectDictionaryToManageCollaborators(Integer messageId, long dictionaryId) {
		if (!CCLHSecurityUtils.isLoggedIn()) throw new UserDoesntExistsException();

		try {
			Dictionary dictionary = getDictionaryAndCheckCreator(dictionaryId);

			InlineKeyboardMarkup privateInlineKeyboard = getDictionaryCollaboratorMenu(dictionaryId);

			if (messageId == null) {
				dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(),
						getCollaboratorMenuMessage(dictionary), privateInlineKeyboard);
			} else {
				dictionariesBotMessageService.editMessage(CCLHSecurityUtils.getId(), messageId,
						getCollaboratorMenuMessage(dictionary), privateInlineKeyboard);
			}
		} catch (DictionaryDoesntExistsException e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("ERROR_DICTIONARY_NOT_FOUND"));
		} catch (DictionaryNotYoursException e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("ERROR_DICTIONARY_NOT_YOURS"));
		}
	}

	@Override
	@Transactional(propagation = Propagation.SUPPORTS, rollbackFor = ApplicationException.class)
	public void listCollaboratorsMessage(int messageId, long dictionaryId) {
		if (!CCLHSecurityUtils.isLoggedIn()) throw new UserDoesntExistsException();

		try {
			Dictionary dictionary = getDictionaryAndCheckCreator(dictionaryId);

			InlineKeyboardMarkup privateInlineKeyboard = InlineKeyboardMarkup.builder()
					.keyboardRow(Collections.singletonList(InlineKeyboardButton.builder()
							.text(i18NService.get("GO_BACK"))
							.callbackData("manage_collabs_select__" + dictionaryId).build()))
					.build();
			dictionariesBotMessageService.editMessage(CCLHSecurityUtils.getId(), messageId,
					getCollaboratorListMessage(dictionary.getCollaborators()), privateInlineKeyboard);
		} catch (DictionaryDoesntExistsException e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("ERROR_DICTIONARY_NOT_FOUND"));
		} catch (DictionaryNotYoursException e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("ERROR_DICTIONARY_NOT_YOURS"));
		}
	}

	@Override
	@Transactional(propagation = Propagation.SUPPORTS, rollbackFor = ApplicationException.class)
	public void addCollaboratorsMessage(int messageId, long dictionaryId) {
		if (!CCLHSecurityUtils.isLoggedIn()) throw new UserDoesntExistsException();

		try {
			Dictionary dictionary = getDictionaryAndCheckCreator(dictionaryId);

			dictionariesBotMessageService.deleteMessage(CCLHSecurityUtils.getId(), messageId);
			dictionariesBotMessageService.setPendingReply(CCLHSecurityUtils.getId(), "/add_collab__" + dictionary.getId());
			dictionariesBotMessageService.sendMessageWithForceReply(CCLHSecurityUtils.getId(),
					i18NService.get("COLLABORATORS_ADD"));
		} catch (DictionaryDoesntExistsException e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("ERROR_DICTIONARY_NOT_FOUND"));
		} catch (DictionaryNotYoursException e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("ERROR_DICTIONARY_NOT_YOURS"));
		}
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
	public void addCollaborator(long dictionaryId, String nameOrId) {
		if (!CCLHSecurityUtils.isLoggedIn()) throw new UserDoesntExistsException();

		try {
			Dictionary dictionary = getDictionaryAndCheckCreator(dictionaryId);

			User user = getUserByNameOrId(nameOrId);

			DictionaryCollaborator dictionaryCollaborator = dictionaryService.addCollaborator(dictionary, user);

			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(),
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

			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(),
					getCollaboratorMenuMessage(dictionary), privateInlineKeyboard);
		} catch (DictionaryDoesntExistsException e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("ERROR_DICTIONARY_NOT_FOUND"));
		} catch (DictionaryNotYoursException e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("ERROR_DICTIONARY_NOT_YOURS"));
		} catch (UserDoesntExistsException e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(),
					i18NService.get("COLLABORATOR_ADD_USER_DOESNT_EXISTS"));
		} catch (DictionaryCollaboratorAlreadyExists e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(),
					i18NService.get("COLLABORATOR_ADD_ALREADY_EXISTS"));
		} catch (DictionaryMaxCollaboratorsReached e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(),
					i18NService.get("COLLABORATOR_ADD_MAX_REACHED"));
		}
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
	public void acceptCollaborator(int messageId, long dictionaryId) {
		if (!CCLHSecurityUtils.isLoggedIn()) throw new UserDoesntExistsException();

		try {
			Dictionary dictionary = getDictionaryAndCheckCollaborator(dictionaryId);

			User user = userService.getById(CCLHSecurityUtils.getId());

			dictionaryService.acceptCollaborator(dictionary, user);

			dictionariesBotMessageService.editMessage(CCLHSecurityUtils.getId(), messageId, i18NService.get("COLLABORATORS_ACCEPTED_MESSAGE"));

			dictionariesBotMessageService.sendMessage(dictionary.getCreator().getId(), getCollaboratorAcceptedMessage(user.getName()));
		} catch (DictionaryDoesntExistsException e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("ERROR_DICTIONARY_NOT_FOUND"));
		} catch (DictionaryNotYoursException e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("ERROR_DICTIONARY_NOT_YOURS"));
		}
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
	public void rejectCollaborator(int messageId, long dictionaryId) {
		if (!CCLHSecurityUtils.isLoggedIn()) throw new UserDoesntExistsException();

		try {
			Dictionary dictionary = getDictionaryAndCheckCollaborator(dictionaryId);

			User user = userService.getById(CCLHSecurityUtils.getId());

			dictionaryService.removeCollaborator(dictionary, user);

			dictionariesBotMessageService.editMessage(CCLHSecurityUtils.getId(), messageId, i18NService.get("COLLABORATORS_REJECTED_MESSAGE"));

			dictionariesBotMessageService.sendMessage(dictionary.getCreator().getId(), getCollaboratorRejectedMessage(user.getName()));
		} catch (DictionaryDoesntExistsException e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("ERROR_DICTIONARY_NOT_FOUND"));
		} catch (DictionaryNotYoursException e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("ERROR_DICTIONARY_NOT_YOURS"));
		}
	}

	@Override
	@Transactional(propagation = Propagation.SUPPORTS, rollbackFor = ApplicationException.class)
	public void removeCollaboratorsMessage(int messageId, long dictionaryId) {
		if (!CCLHSecurityUtils.isLoggedIn()) throw new UserDoesntExistsException();

		try {
			Dictionary dictionary = getDictionaryAndCheckCreator(dictionaryId);

			dictionariesBotMessageService.deleteMessage(CCLHSecurityUtils.getId(), messageId);
			dictionariesBotMessageService.setPendingReply(CCLHSecurityUtils.getId(), "/delete_collab__" + dictionary.getId());
			dictionariesBotMessageService.sendMessageWithForceReply(CCLHSecurityUtils.getId(), i18NService.get("COLLABORATORS_DELETE"));
		} catch (DictionaryDoesntExistsException e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("ERROR_DICTIONARY_NOT_FOUND"));
		} catch (DictionaryNotYoursException e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("ERROR_DICTIONARY_NOT_YOURS"));
		}
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
	public void deleteCollaborator(long dictionaryId, String nameOrId) {
		if (!CCLHSecurityUtils.isLoggedIn()) throw new UserDoesntExistsException();

		try {
			Dictionary dictionary = getDictionaryAndCheckCreator(dictionaryId);

			User user = getUserByNameOrId(nameOrId);

			dictionaryService.removeCollaborator(dictionary, user);

			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("COLLABORATORS_DELETED"));

			dictionariesBotMessageService.sendMessage(user.getId(), getDeleteCollaboratorInfoMessage(dictionary));

			InlineKeyboardMarkup privateInlineKeyboard = getDictionaryCollaboratorMenu(dictionaryId);

			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(),
					getCollaboratorMenuMessage(dictionary), privateInlineKeyboard);
		} catch (DictionaryDoesntExistsException e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("ERROR_DICTIONARY_NOT_FOUND"));
		} catch (DictionaryNotYoursException e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("ERROR_DICTIONARY_NOT_YOURS"));
		} catch (DictionaryCollaboratorDoesntExists e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("ERROR_COLLABORATOR_REMOVE_USER_DOESNT_EXISTS"));
		}
	}

	@Override
	public void toggleCollaboratorsMessage(int messageId, long dictionaryId) {
		if (!CCLHSecurityUtils.isLoggedIn()) throw new UserDoesntExistsException();

		try {
			Dictionary dictionary = getDictionaryAndCheckCreator(dictionaryId);

			dictionariesBotMessageService.deleteMessage(CCLHSecurityUtils.getId(), messageId);
			dictionariesBotMessageService.setPendingReply(CCLHSecurityUtils.getId(), "/toggle_collab__" + dictionary.getId());
			dictionariesBotMessageService.sendMessageWithForceReply(CCLHSecurityUtils.getId(), i18NService.get("COLLABORATORS_TOGGLE"));
		} catch (DictionaryDoesntExistsException e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("ERROR_DICTIONARY_NOT_FOUND"));
		} catch (DictionaryNotYoursException e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("ERROR_DICTIONARY_NOT_YOURS"));
		}
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationException.class)
	public void toggleCollaborator(long dictionaryId, String nameOrId) {
		if (!CCLHSecurityUtils.isLoggedIn()) throw new UserDoesntExistsException();

		try {
			Dictionary dictionary = getDictionaryAndCheckCreator(dictionaryId);

			User user = getUserByNameOrId(nameOrId);

			DictionaryCollaborator dictionaryCollaborator = dictionaryService.toggleCollaborator(dictionary, user);

			dictionariesBotMessageService.sendMessage(dictionaryCollaborator.getUser().getId(),
					getToggledCollaboratorPrivateMessage(dictionaryCollaborator));
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(),
					getToggledCollaboratorMessage(dictionaryCollaborator));

			InlineKeyboardMarkup privateInlineKeyboard = getDictionaryCollaboratorMenu(dictionaryId);

			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(),
					getCollaboratorMenuMessage(dictionary), privateInlineKeyboard);
		} catch (DictionaryDoesntExistsException e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("ERROR_DICTIONARY_NOT_FOUND"));
		} catch (DictionaryNotYoursException e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("ERROR_DICTIONARY_NOT_YOURS"));
		} catch (DictionaryCollaboratorDoesntExists e) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), i18NService.get("ERROR_COLLABORATOR_DOESNT_EXISTS"));
		}
	}

	@Override
	@Transactional(propagation = Propagation.SUPPORTS, rollbackFor = ApplicationException.class)
	public void sendHelpMessage(long chatId) {
		dictionariesBotMessageService.sendMessage(chatId, getHelpMessage());
	}

	private InlineKeyboardMarkup getMainMenuKeyboard() {
		return InlineKeyboardMarkup.builder()
				.keyboardRow(Collections.singletonList(InlineKeyboardButton.builder()
					.text(i18NService.get("DICTIONARIES_LIST_BUTTON"))
					.callbackData("dictionary_list").build()))
				.keyboardRow(Collections.singletonList(InlineKeyboardButton.builder()
					.text(i18NService.get("DICTIONARIES_CREATE_BUTTON"))
					.callbackData("dictionary_create").build()))
				.keyboardRow(Arrays.asList(
					InlineKeyboardButton.builder()
						.text(i18NService.get("DICTIONARIES_RENAME_BUTTON"))
						.callbackData("dictionary_rename").build(),
					InlineKeyboardButton.builder()
						.text(i18NService.get("DICTIONARIES_LANG_BUTTON"))
						.callbackData("dictionary_lang").build()))
				.keyboardRow(Collections.singletonList(InlineKeyboardButton.builder()
					.text(i18NService.get("DICTIONARIES_DELETE_BUTTON"))
						.callbackData("dictionary_delete").build()))
				.keyboardRow(Arrays.asList(
					InlineKeyboardButton.builder()
						.text(i18NService.get("DICTIONARIES_TOGGLE_PUBLISH_BUTTON"))
						.callbackData("dictionary_toggle").build(),
					InlineKeyboardButton.builder()
						.text(i18NService.get("DICTIONARIES_TOGGLE_SHARE_BUTTON"))
						.callbackData("dictionary_share").build()
					))
				.keyboardRow(Arrays.asList(
					InlineKeyboardButton.builder()
						.text(i18NService.get("DICTIONARIES_MANAGE_CARDS_BUTTON"))
						.callbackData("dictionary_manage_cards").build(),
					InlineKeyboardButton.builder()
						.text(i18NService.get("DICTIONARIES_MANAGE_COLLABS_BUTTON"))
						.callbackData("dictionary_manage_collabs").build()))
				.build();
	}

	private InlineKeyboardMarkup getDictionaryCollaboratorMenu(long dictionaryId) {
		return InlineKeyboardMarkup.builder()
				.keyboardRow(Collections.singletonList(InlineKeyboardButton.builder()
						.text(i18NService.get("COLLABORATORS_LIST_BUTTON"))
						.callbackData("list_collabs__" + dictionaryId).build()))
				.keyboardRow(Collections.singletonList(InlineKeyboardButton.builder()
						.text(i18NService.get("COLLABORATORS_ADD_BUTTON"))
						.callbackData("add_collabs__" + dictionaryId).build()))
				.keyboardRow(Collections.singletonList(InlineKeyboardButton.builder()
						.text(i18NService.get("COLLABORATORS_DELETE_BUTTON"))
						.callbackData("delete_collabs__" + dictionaryId).build()))
				.keyboardRow(Collections.singletonList(InlineKeyboardButton.builder()
						.text(i18NService.get("COLLABORATORS_TOGGLE_BUTTON"))
						.callbackData("toggle_collabs__" + dictionaryId).build()))
				.keyboardRow(Collections.singletonList(InlineKeyboardButton.builder()
						.text(i18NService.get("GO_BACK")).callbackData("menu").build()))
				.build();
	}

	private InlineKeyboardMarkup getCardKeyboardMenu(long dictionaryId) {
		return InlineKeyboardMarkup.builder()
				.keyboardRow(Arrays.asList(
						InlineKeyboardButton.builder()
								.text(i18NService.get("CARDS_WHITE_LIST_BUTTON"))
								.callbackData("list_white_cards__" + dictionaryId).build(),
						InlineKeyboardButton.builder()
								.text(i18NService.get("CARDS_BLACK_LIST_BUTTON"))
								.callbackData("list_black_cards__" + dictionaryId).build()))
				.keyboardRow(Arrays.asList(
						InlineKeyboardButton.builder()
								.text(i18NService.get("CARDS_WHITE_ADD_BUTTON"))
								.callbackData("add_white_cards__" + dictionaryId).build(),
						InlineKeyboardButton.builder()
								.text(i18NService.get("CARDS_BLACK_ADD_BUTTON"))
								.callbackData("add_black_cards__" + dictionaryId).build()))
				.keyboardRow(Arrays.asList(
						InlineKeyboardButton.builder()
								.text(i18NService.get("CARDS_WHITE_EDIT_BUTTON"))
								.callbackData("edit_white_cards__" + dictionaryId).build(),
						InlineKeyboardButton.builder()
								.text(i18NService.get("CARDS_BLACK_EDIT_BUTTON"))
								.callbackData("edit_black_cards__" + dictionaryId).build()))
				.keyboardRow(Arrays.asList(
						InlineKeyboardButton.builder()
								.text(i18NService.get("CARDS_WHITE_REMOVE_BUTTON"))
								.callbackData("delete_white_cards__" + dictionaryId).build(),
						InlineKeyboardButton.builder()
								.text(i18NService.get("CARDS_BLACK_REMOVE_BUTTON"))
								.callbackData("delete_black_cards__" + dictionaryId).build()))
				.keyboardRow(Collections.singletonList(InlineKeyboardButton.builder()
						.text(i18NService.get("GO_BACK")).callbackData("menu").build()))
				.build();
	}

	private String getDictionaryListMessage(List<Dictionary> dictionaryList) {
		return MessageFormat.format(i18NService.get("DICTIONARIES_LIST"), getDictionaryList(dictionaryList));
	}

	private String getDictionaryRenameMessage(List<Dictionary> dictionaryList) {
		return MessageFormat.format(i18NService.get("DICTIONARIES_RENAME_LIST"), getDictionaryList(dictionaryList));
	}

	private String getChangeDictionaryLangMessage(List<Dictionary> dictionaryList) {
		return MessageFormat.format(i18NService.get("DICTIONARIES_CHANGE_LANG_LIST"), getDictionaryList(dictionaryList));
	}

	private String getDictionaryToggleMessage(List<Dictionary> dictionaryList) {
		return MessageFormat.format(i18NService.get("DICTIONARIES_TOGGLE_LIST"), getDictionaryList(dictionaryList));
	}

	private String getDictionaryRequestShareMessage(List<Dictionary> dictionaryList) {
		return MessageFormat.format(i18NService.get("DICTIONARIES_SHARE_LIST"), getDictionaryList(dictionaryList));
	}

	private String getDictionaryDeleteMessage(List<Dictionary> dictionaryList) {
		return MessageFormat.format(i18NService.get("DICTIONARIES_DELETE_LIST"), getDictionaryList(dictionaryList));
	}

	private String getManageCardsMessage(List<Dictionary> dictionaryList) {
		return MessageFormat.format(i18NService.get("DICTIONARIES_MANAGE_CARDS_LIST"), getDictionaryList(dictionaryList));
	}

	private String getManageCollaboratorsMessage(List<Dictionary> dictionaryList) {
		return MessageFormat.format(i18NService.get("DICTIONARIES_MANAGE_COLLABORATORS_LIST"),
				getDictionaryList(dictionaryList));
	}

	private String getCollaboratorListMessage(List<DictionaryCollaborator> collaborators) {
		StringBuilder stringBuilder = new StringBuilder();
		if (collaborators.size() > 1) {
			for (DictionaryCollaborator dictionaryCollaborator : collaborators) {
				if (!Objects.equals(dictionaryCollaborator.getUser().getId(), CCLHSecurityUtils.getId())) {
					stringBuilder.append(dictionaryCollaborator.getUser().getName()).append(" (")
							.append("aceptado: ").append(StringUtils.booleanToSpanish(dictionaryCollaborator.getAccepted()))
							.append(", activo: ").append(StringUtils.booleanToSpanish(dictionaryCollaborator.getCanEdit()))
							.append(")\n");
				}
			}
		} else {
			stringBuilder.append("\n").append("No hay colaboradores.").append("\n");
		}

		return MessageFormat.format(i18NService.get("COLLABORATORS_LIST"), stringBuilder.toString());
	}

	private void sendCardList(Dictionary dictionary, CardTypeEnum cardTypeEnum) {
		StringBuilder stringBuilder = new StringBuilder();
		List<Card> whiteCards = cardService.findCardsByDictionaryIdAndType(dictionary, cardTypeEnum);
		for (Card whiteCard : whiteCards) {
			if (stringBuilder.length() > 4000) {
				dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), stringBuilder.toString());
				stringBuilder = new StringBuilder();
			}

			stringBuilder.append(whiteCard.getId()).append(" - ").append(whiteCard.getText()).append("\n");
		}

		if (!stringBuilder.isEmpty()) {
			dictionariesBotMessageService.sendMessage(CCLHSecurityUtils.getId(), stringBuilder.toString());
		}
	}

	private String getAddCollaboratorAcceptMessage(DictionaryCollaborator dictionaryCollaborator) {
		return MessageFormat.format(i18NService.get("COLLABORATORS_ACCEPT_MESSAGE"),
				dictionaryCollaborator.getDictionary().getCreator().getName(),
				dictionaryCollaborator.getDictionary().getName());
	}

	private String getDeleteCollaboratorInfoMessage(Dictionary dictionary) {
		return MessageFormat.format(i18NService.get("COLLABORATORS_DELETED_MESSAGE"),
				dictionary.getName());
	}

	private String getCardMenuMessage(Dictionary dictionary) {
		return MessageFormat.format(i18NService.get("CARDS_MENU"), dictionary.getName());
	}

	private String changeDictionariesLanguageListMessage() {
		List<Lang> languages = i18NService.getLanguages();
		StringBuilder langmsg = new StringBuilder();
		for (Lang lang : languages) {
			langmsg.append("<b>").append(lang.getId()).append("</b>").append(" - ").append(lang.getName()).append("\n");
		}
		return MessageFormat.format(i18NService.get("DICTIONARY_CHANGE_LANG"), langmsg.toString());
	}

	private String getWhiteCardAddedMessage(Dictionary dictionary) {
		int whiteCardCount = cardService.countCardsByDictionaryIdAndType(dictionary, CardTypeEnum.WHITE);
		int maxWhiteCards = (whiteCardCount < cardService.getDictionaryMinWhiteCards() ?
				cardService.getDictionaryMinWhiteCards() : cardService.getDictionaryMaxWhiteCards());
		return MessageFormat.format(i18NService.get("CARDS_WHITE_CARD_ADDED"), whiteCardCount, maxWhiteCards);
	}

	private String getBlackCardAddedMessage(Dictionary dictionary) {
		int blackCardCount = cardService.countCardsByDictionaryIdAndType(dictionary, CardTypeEnum.BLACK);
		int maxBlackCards = (blackCardCount < cardService.getDictionaryMinBlackCards() ?
				cardService.getDictionaryMinBlackCards() : cardService.getDictionaryMaxBlackCards());
		return MessageFormat.format(i18NService.get("CARDS_BLACK_CARD_ADDED"), blackCardCount, maxBlackCards);
	}

	private String getCollaboratorMenuMessage(Dictionary dictionary) {
		return MessageFormat.format(i18NService.get("COLLABORATORS_MENU"), dictionary.getName());
	}

	private String getAddedCollaboratorMessage(DictionaryCollaborator collaborator) {
		return MessageFormat.format(i18NService.get("COLLABORATORS_ADDED"), collaborator.getUser().getName());
	}

	private String getCollaboratorAcceptedMessage(String name) {
		return MessageFormat.format(i18NService.get("COLLABORATORS_ACCEPTED_CREATOR"), name);
	}

	private String getCollaboratorRejectedMessage(String name) {
		return MessageFormat.format(i18NService.get("COLLABORATORS_REJECTED_CREATOR"), name);
	}

	private String getShareDictionaryRejectedMessage(Dictionary dictionary) {
		return MessageFormat.format(i18NService.get("DICTIONARY_SHARED_REJECTED"), dictionary.getName());
	}

	private String getToggledCollaboratorMessage(DictionaryCollaborator collaborator) {
		if (Boolean.TRUE.equals(collaborator.getCanEdit()))
			return MessageFormat.format(i18NService.get("COLLABORATORS_TOGGLED_ON"), collaborator.getUser().getName());
		else
			return MessageFormat.format(i18NService.get("COLLABORATORS_TOGGLED_OFF"), collaborator.getUser().getName());
	}

	private String getToggledCollaboratorPrivateMessage(DictionaryCollaborator collaborator) {
		if (Boolean.TRUE.equals(collaborator.getCanEdit()))
			return MessageFormat.format(i18NService.get("COLLABORATORS_TOGGLED_ON_MESSAGE"), collaborator.getDictionary().getName());
		else
			return MessageFormat.format(i18NService.get("COLLABORATORS_TOGGLED_OFF_MESSAGE"), collaborator.getDictionary().getName());
	}

	private String getDictionaryPublishedMessage(Dictionary dictionary) {
		String msg;
		if (Boolean.TRUE.equals(dictionary.getPublished())) {
			msg = MessageFormat.format(i18NService.get("DICTIONARY_TOGGLED_ON"), dictionary.getName());
		} else {
			msg = MessageFormat.format(i18NService.get("DICTIONARY_TOGGLED_OFF"), dictionary.getName());
		}
		return msg;
	}

	private String getDictionaryRequestShareMessage(Dictionary dictionary) {
		String msg;
		if (Boolean.FALSE.equals(dictionary.getShared())) {
			msg = MessageFormat.format(i18NService.get("DICTIONARY_SHARED_ON_REQUEST"), dictionary.getName());
		} else {
			msg = MessageFormat.format(i18NService.get("DICTIONARY_SHARED_OFF_REQUEST"), dictionary.getName());
		}
		return msg;
	}

	private String getDictionaryShareMessage(Dictionary dictionary) {
		String msg;
		if (Boolean.TRUE.equals(dictionary.getShared())) {
			msg = MessageFormat.format(i18NService.get("DICTIONARY_SHARED_ON"), dictionary.getName());
		} else {
			msg = MessageFormat.format(i18NService.get("DICTIONARY_SHARED_OFF"), dictionary.getName());
		}
		return msg;
	}

	private String getDeleteSharedErrorMessage() {
		return MessageFormat.format(i18NService.get("ERROR_DICTIONARY_SHARED"), getBotCreatorName());
	}

	private String getAddWhiteCardLengthExceededErrorMessage() {
		return MessageFormat.format(i18NService.get("ERROR_CARD_EXCEEDED_LENGTH"), cardService.getDictionaryWhiteCardMaxLength());
	}

	private String getAddBlackCardLengthExceededErrorMessage() {
		return MessageFormat.format(i18NService.get("ERROR_CARD_EXCEEDED_LENGTH"), cardService.getDictionaryBlackCardMaxLength());
	}

	private String getDictionaryAlreadySharedError() {
		return MessageFormat.format(i18NService.get("ERROR_DICTIONARY_ALREADY_SHARED"), getBotCreatorName());
	}

	private String getHelpMessage() {
		return MessageFormat.format(i18NService.get("HELP"), getBotName(), getBotVersion(),
				getBotHelpURL(), getBotCreatorName());
	}

	private Dictionary getDictionaryAndCheckBotCreator(long dictionaryId) {
		Dictionary dictionary = dictionaryService.getDictionaryById(dictionaryId);

		if (dictionary == null) {
			throw new DictionaryDoesntExistsException();
		}

		if (!getBotCreatorId().equals(CCLHSecurityUtils.getId())) {
			throw new DictionaryNotYoursException();
		}

		return dictionary;
	}

	private Dictionary getDictionaryAndCheckCreator(long dictionaryId) {
		Dictionary dictionary = dictionaryService.getDictionaryById(dictionaryId);

		if (dictionary == null) {
			throw new DictionaryDoesntExistsException();
		}

		if (!Objects.equals(dictionary.getCreator().getId(), CCLHSecurityUtils.getId())) {
			throw new DictionaryNotYoursException();
		}

		return dictionary;
	}

	private Dictionary getDictionaryAndCheckCollaborator(long dictionaryId) {
		Dictionary dictionary = dictionaryService.getDictionaryById(dictionaryId);

		if (dictionary == null) {
			throw new DictionaryDoesntExistsException();
		}

		if (!dictionaryService.isDictionaryCollaborator(dictionary, userService.getById(CCLHSecurityUtils.getId()))) {
			throw new DictionaryNotYoursException();
		}

		return dictionary;
	}

	private Dictionary getDictionaryAndCheckActiveCollaborator(long dictionaryId) {
		Dictionary dictionary = dictionaryService.getDictionaryById(dictionaryId);

		if (dictionary == null) {
			throw new DictionaryDoesntExistsException();
		}

		if (!dictionaryService.isDictionaryActiveCollaborator(dictionary, userService.getById(CCLHSecurityUtils.getId()))) {
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

	private String getDictionaryList(List<Dictionary> dictionaryList) {
		StringBuilder stringBuilder = new StringBuilder();
		for (Dictionary dictionary : dictionaryList) {
			stringBuilder.append(getDictionaryInfo(dictionary));
		}
		return stringBuilder.toString();
	}

	private String getDictionaryInfo(Dictionary dictionary) {
		return dictionary.getId() + " - " + dictionary.getName() + " (" +
				"<b>tuyo:</b> " + StringUtils.booleanToSpanish(Objects.equals(dictionary.getCreator().getId(), CCLHSecurityUtils.getId())) +
				"; <b>publicado:</b> " + StringUtils.booleanToSpanish(dictionary.getPublished()) +
				"; <b>compartido:</b> " + StringUtils.booleanToSpanish(dictionary.getShared()) +
				"; <b>idioma:</b> " + dictionary.getLang().getName() +
				")\n";
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
	public void setI18NService(I18NService i18NService) {
		this.i18NService = i18NService;
	}

	@Autowired
	public void setConfigurationService(ConfigurationService configurationService) {
		this.configurationService = configurationService;
	}
}
