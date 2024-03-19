package org.themarioga.cclh.bot.dictionaries.service.intf;

public interface DictionariesBotService {

	void registerUser(long userId, String username);

	void mainMenu(long userId);

	void listDictionaries(long userId);

	void createDictionaryMessage(long userId);

	void createDictionary(long userId, String name);

	void renameDictionaryMessage(long userId);

	void selectDictionaryToRename(long userId, long dictionaryId);

	void renameDictionary(long userId, long dictionaryId, String newName);

	void sendHelpMessage(long chatId);
}
