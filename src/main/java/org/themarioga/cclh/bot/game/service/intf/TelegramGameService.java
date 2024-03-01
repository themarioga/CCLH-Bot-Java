package org.themarioga.cclh.bot.game.service.intf;

import org.themarioga.cclh.bot.game.model.TelegramGame;
import org.themarioga.cclh.bot.game.model.TelegramPlayer;
import org.themarioga.cclh.commons.enums.GameTypeEnum;
import org.themarioga.cclh.commons.models.PlayedCard;

public interface TelegramGameService {

	TelegramGame createGame(long roomId, String roomName, long creatorId, int groupMessageId, int privateMessageId);

	void deleteGame(TelegramGame telegramGame);

	void setBlackCardMessage(TelegramGame tgGame, int blackCardMessageId);

	void setType(TelegramGame game, GameTypeEnum type);

	void setNumberOfCardsToWin(TelegramGame game, int numberOfCardsToWin);

	void setNumberOfRoundsToEnd(TelegramGame game, int numberOfRoundsToEnd);

	void setMaxNumberOfPlayers(TelegramGame game, int maxNumberOfPlayers);

	void setDictionary(TelegramGame game, long dictionaryId);

	void addPlayer(TelegramGame game, TelegramPlayer player);

	void removePlayer(TelegramGame game, TelegramPlayer player);

	void startGame(TelegramGame tgGame);

	void startRound(TelegramGame tgGame);

	void endRound(TelegramGame tgGame);

	void voteForDeletion(TelegramGame tgGame, long userId);

	void playCard(TelegramGame tgGame, long userId, long cardId);

	void voteCard(TelegramGame tgGame, long userId, long cardId);

	TelegramGame getGame(long roomId);

	TelegramGame getGameByCreatorId(long creatorId);

	TelegramGame getByPlayerUser(long userId);

	PlayedCard getMostVotedCard(TelegramGame tgGame);

}
