package org.themarioga.cclh.bot.dictionaries.service.intf;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.themarioga.cclh.commons.exceptions.ApplicationException;

public interface DictionariesBotService {

	void registerUser(long userId, String username);

	void mainMenu(long userId);

	void listDictionaries(long userId);

	void createDictionaryMessage(long userId);

	@Transactional(propagation = Propagation.SUPPORTS, rollbackFor = ApplicationException.class)
	void createDictionary(long userId, String name);

	void sendHelpMessage(long roomId);
}
