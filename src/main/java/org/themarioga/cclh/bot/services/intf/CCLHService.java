package org.themarioga.cclh.bot.services.intf;

import org.themarioga.cclh.bot.model.TelegramGame;
import org.themarioga.cclh.commons.enums.GameTypeEnum;

public interface CCLHService {

    boolean registerUser(Long userId, String username);

    TelegramGame createGame(long roomId, String roomName, long creatorId, int groupMessageId, int privateMessageId);

    TelegramGame deleteGame(TelegramGame game);

    TelegramGame setType(TelegramGame game, GameTypeEnum type);

    TelegramGame setNumberOfCardsToWin(TelegramGame game, int numberOfCardsToWin);

    TelegramGame setMaxNumberOfPlayers(TelegramGame game, int maxNumberOfPlayers);

    TelegramGame setDictionary(TelegramGame game, long dictionaryId);

    TelegramGame getGame(long roomId);

    TelegramGame getGameByCreatorId(long creatorId);
}
