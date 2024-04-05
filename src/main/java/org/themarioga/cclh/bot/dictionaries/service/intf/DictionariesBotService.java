package org.themarioga.cclh.bot.dictionaries.service.intf;

public interface DictionariesBotService {

	void registerUser(long userId, String username, String lang);

	void loginUser(long userId);

	void changeUserLanguageMessage();

	void changeUserLanguage(int messageId, String lang);

	void mainMenu();

	void mainMenu(int messageId);

	void listDictionaries(int messageId);

	void createDictionaryMessage(int messageId);

	void createDictionary(String name);

	void renameDictionaryMessage(int messageId);

	void selectDictionaryToRename(int messageId, long dictionaryId);

	void renameDictionary(long dictionaryId, String newName);

	void changeDictionaryLangMessage(int messageId);

	void selectDictionaryToChangeLang(int messageId, long dictionaryId);

	void changeDictionaryLang(long dictionaryId, String language);

	void deleteDictionaryMessage(int messageId);

	void selectDictionaryToDelete(int messageId, long dictionaryId);

	void deleteDictionary(long dictionaryId, String text);

	void toggleDictionaryMessage(int messageId);

	void toggleDictionary(int messageId, long dictionaryId);

	void shareDictionaryMessage(int messageId);

	void requestShareDictionary(int messageId, long dictionaryId);

	void acceptShareDictionary(int messageId, long dictionaryId);

	void rejectShareDictionary(int messageId, long dictionaryId);

	void manageCardsMessage(int messageId);

	void selectDictionaryToManageCards(Integer messageId, long dictionaryId);

	void listWhiteCardsMessage(int messageId, long dictionaryId);

	void addWhiteCardsMessage(int messageId, long dictionaryId);

	void addWhiteCard(long dictionaryId, String text);

	void editWhiteCardsMessage(int messageId, long dictionaryId);

	void editWhiteCardSelect(long dictionaryId, long cardId);

	void editWhiteCard(long cardId, String newText);

	void deleteWhiteCardsMessage(int messageId, long dictionaryId);

	void deleteWhiteCard(long dictionaryId, long cardId);

	void listBlackCardsMessage(int messageId, long dictionaryId);

	void addBlackCardsMessage(int messageId, long dictionaryId);

	void addBlackCard(long dictionaryId, String text);

	void editBlackCardsMessage(int messageId, long dictionaryId);

	void editBlackCardSelect(long dictionaryId, long cardId);

	void editBlackCard(long cardId, String newText);

	void deleteBlackCardsMessage(int messageId, long dictionaryId);

	void deleteBlackCard(long dictionaryId, long cardId);

	void manageCollaboratorsMessage(int messageId);

	void selectDictionaryToManageCollaborators(Integer messageId, long dictionaryId);

	void listCollaboratorsMessage(int messageId, long dictionaryId);

	void addCollaboratorsMessage(int messageId, long dictionaryId);

	void addCollaborator(long dictionaryId, String nameOrId);

	void acceptCollaborator(int messageId, long dictionaryId);

	void rejectCollaborator(int messageId, long dictionaryId);

	void removeCollaboratorsMessage(int messageId, long dictionaryId);

	void deleteCollaborator(long dictionaryId, String nameOrId);

	void toggleCollaboratorsMessage(int messageId, long dictionaryId);

	void toggleCollaborator(long dictionaryId, String nameOrId);

	void sendHelpMessage(long chatId);
}
