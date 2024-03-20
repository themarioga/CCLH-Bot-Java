package org.themarioga.cclh.bot.dictionaries.service.intf;

public interface DictionariesBotService {

	void registerUser(long userId, String username);

	void mainMenu(long userId);

	void mainMenu(long userId, int messageId);

	void listDictionaries(long userId, int messageId);

	void createDictionaryMessage(long userId, int messageId);

	void createDictionary(long userId, String name);

	void renameDictionaryMessage(long userId, int messageId);

	void selectDictionaryToRename(long userId, int messageId, long dictionaryId);

	void renameDictionary(long userId, long dictionaryId, String newName);

	void deleteDictionaryMessage(long userId, int messageId);

	void selectDictionaryToDelete(long userId, int messageId, long dictionaryId);

	void deleteDictionary(long userId, long dictionaryId, String text);

	void manageCardsMessage(long userId, int messageId);

	void selectDictionaryToManageCards(long userId, long dictionaryId);

	void manageCollaboratorsMessage(long userId, int messageId);

	void selectDictionaryToManageCollaborators(long userId, long dictionaryId);

	void sendHelpMessage(long chatId);
}
