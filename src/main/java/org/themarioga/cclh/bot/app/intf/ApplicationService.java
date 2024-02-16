package org.themarioga.cclh.bot.app.intf;

import jakarta.transaction.Transactional;
import org.themarioga.cclh.commons.exceptions.ApplicationException;

public interface ApplicationService {

    void run();

	void registerUser(long userId, String username);

	void deleteMyGames(long userId);

	void startCreatingGame(long roomId, String roomTitle, long creatorId);

	void createGame(long roomId, String roomTitle, long creatorId, int groupMessageId, int privateMessageId, int playerMessageId);

	void gameCreatedQuery(long roomId, long userId, String callbackQueryId);

	void gameSelectMaxPlayersQuery(long roomId, long userId, String callbackQueryId);

	void gameConfigureQuery(long roomId, long userId, String callbackQueryId);

	void gameSelectModeQuery(long roomId, long userId, String callbackQueryId);

	void gameSelectDictionaryQuery(long roomId, long userId, String callbackQueryId, String data);

	void gameSelectNCardsToWinQuery(long roomId, long userId, String callbackQueryId);

	void gameChangeMode(long roomId, long userId, String callbackQueryId, String data);

	void gameChangeDictionary(long roomId, long userId, String callbackQueryId, String data);

	void gameChangeMaxPlayers(long roomId, long userId, String callbackQueryId, String data);

	void gameChangeNCardsToWin(long roomId, long userId, String callbackQueryId, String data);

	void gameDeleteGroupQuery(long roomId, long userId, String callbackQueryId);

	void gameDeletePrivateQuery(long userId, String callbackQueryId);

	void gameJoinQuery(long roomId, long userId, String callbackQueryId);

	void joinGame(long roomId, long userId, String callbackQueryId, int playerMessageId);

	void gameStartQuery(long roomId, long userId, String callbackQueryId);

	void playerPlayCardQuery(long userId, String callbackQueryId, String data);

	@Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ApplicationException.class)
	void playerVoteCardQuery(long userId, String callbackQueryId, String data);
}
