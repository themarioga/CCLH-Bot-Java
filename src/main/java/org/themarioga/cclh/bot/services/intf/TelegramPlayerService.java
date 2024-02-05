package org.themarioga.cclh.bot.services.intf;

import org.themarioga.cclh.bot.model.TelegramGame;
import org.themarioga.cclh.bot.model.TelegramPlayer;

import java.util.List;

public interface TelegramPlayerService {

	TelegramPlayer createPlayer(TelegramGame telegramGame, long userId, int messageId);

	List<TelegramPlayer> deletePlayers(TelegramGame tgGame);

	TelegramPlayer getByUser(long userId);

	List<TelegramPlayer> getPlayers(TelegramGame tgGame);

}
