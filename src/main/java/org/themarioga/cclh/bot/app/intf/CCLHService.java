package org.themarioga.cclh.bot.app.intf;

import org.themarioga.cclh.bot.model.TelegramGame;
import org.themarioga.cclh.bot.model.TelegramPlayer;
import org.themarioga.cclh.commons.enums.GameTypeEnum;
import org.themarioga.cclh.commons.models.Deck;

import java.util.List;

public interface CCLHService {

    void registerUser(Long userId, String username);

    TelegramGame createGame(long roomId, String roomName, long creatorId, int groupMessageId, int privateMessageId, int playerMessageId);

    List<TelegramPlayer> deleteGame(TelegramGame telegramGame);

    void setType(TelegramGame game, GameTypeEnum type);

    void setNumberOfCardsToWin(TelegramGame game, int numberOfCardsToWin);

    void setMaxNumberOfPlayers(TelegramGame game, int maxNumberOfPlayers);

    void setDeck(TelegramGame game, long deckId);

    void joinGame(TelegramGame game, long userId, int messageId);

    void startGame(TelegramGame tgGame);

    TelegramGame getGame(long roomId);

    TelegramGame getGameByCreatorId(long creatorId);

    List<TelegramPlayer> getPlayers(TelegramGame game);

    List<Deck> getDeckPaginated(long creatorId, int firstResult, int maxResults);

	long getDeckCount(long creatorId);

    int getDecksPerPage();

	int getMinNumberOfPlayers();

}
