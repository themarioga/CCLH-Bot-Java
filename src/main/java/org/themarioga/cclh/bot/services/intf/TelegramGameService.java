package org.themarioga.cclh.bot.services.intf;

import jakarta.transaction.Transactional;
import org.themarioga.cclh.bot.model.TelegramGame;
import org.themarioga.cclh.bot.model.TelegramPlayer;
import org.themarioga.cclh.commons.enums.GameTypeEnum;
import org.themarioga.cclh.commons.exceptions.ApplicationException;

public interface TelegramGameService {

	TelegramGame createGame(long roomId, String roomName, long creatorId, int groupMessageId, int privateMessageId);

	void deleteGame(TelegramGame telegramGame);

	void setType(TelegramGame game, GameTypeEnum type);

	void setNumberOfCardsToWin(TelegramGame game, int numberOfCardsToWin);

	void setMaxNumberOfPlayers(TelegramGame game, int maxNumberOfPlayers);

	void setDeck(TelegramGame game, long deckId);

	void joinGame(TelegramGame game, TelegramPlayer player);

	@Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ApplicationException.class)
	void startGame(TelegramGame tgGame);

	TelegramGame getGame(long roomId);

	TelegramGame getGameByCreatorId(long creatorId);

}
