package org.themarioga.cclh.bot.game.service.intf;

public interface CCLHBotService {

    void registerUser(long userId, String username, String lang);

    void loginUser(long userId);

    void changeUserLanguageMessage();

    void changeUserLanguage(int messageId, String lang);

    void deleteMyGames();

    void deleteGameByCreatorUsername(String user);

    void deleteAllGames();

    void startCreatingGame(long roomId, String roomTitle);

    void createGame(long roomId, String roomTitle, long creatorId, String creatorName, int privateMessageId, int groupMessageId, int playerMessageId);

    void gameMenuQuery(long roomId, String callbackQueryId);

    void gameSelectMaxPlayersQuery(long roomId, String callbackQueryId);

    void gameConfigureQuery(long roomId, String callbackQueryId);

    void gameSelectModeQuery(long roomId, String callbackQueryId);

    void gameSelectPunctuationModeQuery(long roomId, String callbackQueryId);

    void gameSelectNRoundsToEndQuery(long roomId, String callbackQueryId);

    void gameSelectNPointsToWinQuery(long roomId, String callbackQueryId);

    void gameSelectDictionaryQuery(long roomId, String callbackQueryId, String data);

    void gameChangeMode(long roomId, String callbackQueryId, String data);

    void gameChangeDictionary(long roomId, String callbackQueryId, String data);

    void gameChangeMaxPlayers(long roomId, String callbackQueryId, String data);

    void gameChangeNRoundsToEnd(long roomId, String callbackQueryId, String data);

    void gameChangeNCardsToWin(long roomId, String callbackQueryId, String data);

    void gameDeleteGroupQuery(long roomId, String callbackQueryId);

    void gameDeletePrivateQuery(String callbackQueryId);

    void gameJoinQuery(long roomId, String callbackQueryId);

    void joinGame(long roomId, String username, int playerMessageId, String callbackQueryId);

    void leaveGame(String callbackQueryId);

    void gameStartQuery(long roomId, String callbackQueryId);

    void playerPlayCardQuery(String callbackQueryId, String data);

    void playerVoteCardQuery(String callbackQueryId, String data);

	void toggleGlobalMessages();

	void sendHelpMessage(long roomId);

    void sendMessageToEveryone(String msg);
}
