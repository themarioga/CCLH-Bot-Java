package org.themarioga.cclh.bot.game.service.intf;

public interface CCLHBotService {

    void registerUser(long userId, String username);

    void deleteMyGames(long userId);

    void deleteGameByCreatorUsername(String user);

    void deleteAllGames();

    void startCreatingGame(long roomId, String roomTitle, long creatorId);

    void createGame(long roomId, String roomTitle, long creatorId, String creatorName, int privateMessageId, int groupMessageId, int playerMessageId);

    void gameCreatedQuery(long roomId, long userId, String callbackQueryId);

    void gameSelectMaxPlayersQuery(long roomId, long userId, String callbackQueryId);

    void gameConfigureQuery(long roomId, long userId, String callbackQueryId);

    void gameSelectModeQuery(long roomId, long userId, String callbackQueryId);

    void gameSelectPunctuationModeQuery(long roomId, long userId, String callbackQueryId);

    void gameSelectNRoundsToEndQuery(long roomId, long userId, String callbackQueryId);

    void gameSelectNPointsToWinQuery(long roomId, long userId, String callbackQueryId);

    void gameSelectDictionaryQuery(long roomId, long userId, String callbackQueryId, String data);

    void gameChangeMode(long roomId, long userId, String callbackQueryId, String data);

    void gameChangeDictionary(long roomId, long userId, String callbackQueryId, String data);

    void gameChangeMaxPlayers(long roomId, long userId, String callbackQueryId, String data);

    void gameChangeNRoundsToEnd(long roomId, long userId, String callbackQueryId, String data);

    void gameChangeNCardsToWin(long roomId, long userId, String callbackQueryId, String data);

    void gameDeleteGroupQuery(long roomId, long userId, String callbackQueryId);

    void gameDeletePrivateQuery(long userId, String callbackQueryId);

    void gameJoinQuery(long roomId, long userId, String callbackQueryId);

    void joinGame(long roomId, long userId, String username, int playerMessageId, String callbackQueryId);

    void leaveGame(long userId, String callbackQueryId);

    void gameStartQuery(long roomId, long userId, String callbackQueryId);

    void playerPlayCardQuery(long userId, String callbackQueryId, String data);

    void playerVoteCardQuery(long userId, String callbackQueryId, String data);

	void toggleGlobalMessages();

	void sendHelpMessage(long roomId);

    void sendMessageToEveryone(String msg);

    Long getBotCreatorId();
}
