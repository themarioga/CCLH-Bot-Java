package org.themarioga.cclh.bot.game.service.intf;

import org.themarioga.cclh.bot.game.model.TelegramGame;
import org.themarioga.cclh.bot.game.model.TelegramPlayer;
import org.themarioga.cclh.commons.models.Player;

import java.util.List;

public interface TelegramPlayerService {

	TelegramPlayer createPlayer(TelegramGame telegramGame, long userId, String username, int messageId);

	void deletePlayer(TelegramPlayer telegramPlayer);

	List<TelegramPlayer> deletePlayers(TelegramGame tgGame);

	void incrementPoints(Player player);

	TelegramPlayer getByUser(long userId);

	List<TelegramPlayer> getPlayers(TelegramGame tgGame);

}
