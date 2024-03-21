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

	void selectDictionaryToManageCards(long userId, Integer messageId, long dictionaryId);

	void listWhiteCardsMessage(long userId, int messageId, long dictionaryId);

	void addWhiteCardsMessage(long userId, int messageId, long dictionaryId);

	void editWhiteCardsMessage(long userId, int messageId, long dictionaryId);

	void deleteWhiteCardsMessage(long userId, int messageId, long dictionaryId);

	void listBlackCardsMessage(long userId, int messageId, long dictionaryId);

	void addBlackCardsMessage(long userId, int messageId, long dictionaryId);

	void editBlackCardsMessage(long userId, int messageId, long dictionaryId);

	void deleteBlackCardsMessage(long userId, int messageId, long dictionaryId);

	void manageCollaboratorsMessage(long userId, int messageId);

	void selectDictionaryToManageCollaborators(long userId, Integer messageId, long dictionaryId);

	void listCollaboratorsMessage(long userId, int messageId, long dictionaryId);

	void addCollaboratorsMessage(long userId, int messageId, long dictionaryId);

	void removeCollaboratorsMessage(long userId, int messageId, long dictionaryId);

	void toggleCollaboratorsMessage(long userId, int messageId, long dictionaryId);

	void sendHelpMessage(long chatId);
}
