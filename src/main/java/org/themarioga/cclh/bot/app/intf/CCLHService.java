package org.themarioga.cclh.bot.app.intf;

import org.themarioga.cclh.bot.model.TelegramGame;
import org.themarioga.cclh.bot.model.TelegramPlayer;
import org.themarioga.cclh.commons.enums.GameTypeEnum;
import org.themarioga.cclh.commons.models.Dictionary;
import org.themarioga.cclh.commons.models.VotedCard;

import java.util.List;

public interface CCLHService {

    void registerUser(Long userId, String username);

    TelegramGame createGame(long roomId, String roomName, long creatorId, int groupMessageId, int privateMessageId, int playerMessageId);

    List<TelegramPlayer> deleteGame(TelegramGame telegramGame);

    void setBlackCardMessage(TelegramGame tgGame, int blackCardMessageId);

    void setType(TelegramGame game, GameTypeEnum type);

    void setNumberOfCardsToWin(TelegramGame game, int numberOfCardsToWin);

    void setMaxNumberOfPlayers(TelegramGame game, int maxNumberOfPlayers);

    void setDictionary(TelegramGame game, long dictionaryId);

    void joinGame(TelegramGame game, long userId, int messageId);

    void startGame(TelegramGame tgGame);

    void startRound(TelegramGame tgGame);

    void endRound(TelegramGame tgGame);

    void voteForDeletion(TelegramGame tgGame, long userId);

    void playCard(TelegramGame tgGame, long userId, long cardId);

    void voteCard(TelegramGame tgGame, long userId, long cardId);

    TelegramGame getGame(long roomId);

    TelegramGame getGameByCreatorId(long creatorId);

    TelegramGame getByPlayerUser(long userId);

    TelegramPlayer getPlayer(long userId);

    List<TelegramPlayer> getPlayers(TelegramGame game);

    List<Dictionary> getDictionariesPaginated(long creatorId, int firstResult, int maxResults);

    VotedCard getMostVotedCard(TelegramGame tgGame);

	long getDictionaryCount(long creatorId);

    int getDictionariesPerPage();

	int getMinNumberOfPlayers();

}
