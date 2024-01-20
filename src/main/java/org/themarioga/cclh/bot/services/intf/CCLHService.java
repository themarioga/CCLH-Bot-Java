package org.themarioga.cclh.bot.services.intf;

import org.themarioga.cclh.bot.dto.TgGameDTO;
import org.themarioga.cclh.bot.model.TelegramGame;

public interface CCLHService {

    boolean registerUser(Long userId, String username);

    TelegramGame createGame(long roomId, String roomName, long creatorId, int groupMessageId, int privateMessageId);

    TgGameDTO deleteGame(long roomId);

    TgGameDTO deleteGameByUserId(long creatorId);
}
