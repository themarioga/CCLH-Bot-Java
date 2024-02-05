package org.themarioga.cclh.bot.services.intf;

import org.themarioga.cclh.bot.model.TelegramGame;
import org.themarioga.cclh.bot.model.TelegramPlayer;
import org.themarioga.cclh.commons.enums.GameTypeEnum;

public interface TelegramGameService {

	TelegramGame createGame(long roomId, String roomName, long creatorId, int groupMessageId, int privateMessageId);

	void deleteGame(TelegramGame telegramGame);

	void setType(TelegramGame game, GameTypeEnum type);

	void setNumberOfCardsToWin(TelegramGame game, int numberOfCardsToWin);

	void setMaxNumberOfPlayers(TelegramGame game, int maxNumberOfPlayers);

	void setDeck(TelegramGame game, long deckId);

	void joinGame(TelegramGame game, TelegramPlayer player);

	void startGame(TelegramGame tgGame);

	void voteForDeletion(TelegramGame tgGame, long userId);

	void playCard(TelegramGame tgGame, long userId, long cardId);

	TelegramGame getGame(long roomId);

	TelegramGame getGameByCreatorId(long creatorId);

}
