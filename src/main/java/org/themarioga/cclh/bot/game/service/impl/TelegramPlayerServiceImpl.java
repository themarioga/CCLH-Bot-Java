package org.themarioga.cclh.bot.game.service.impl;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.themarioga.cclh.bot.game.dao.intf.TelegramPlayerDao;
import org.themarioga.cclh.bot.game.model.TelegramGame;
import org.themarioga.cclh.bot.game.model.TelegramPlayer;
import org.themarioga.cclh.bot.game.service.intf.TelegramPlayerService;
import org.themarioga.cclh.commons.exceptions.ApplicationException;
import org.themarioga.cclh.commons.models.Player;
import org.themarioga.cclh.commons.services.intf.PlayerService;
import org.themarioga.cclh.commons.services.intf.UserService;

import java.util.List;

@Service
public class TelegramPlayerServiceImpl implements TelegramPlayerService {

	private final TelegramPlayerDao telegramPlayerDao;
	private final PlayerService playerService;
	private final UserService userService;

	@Autowired
	public TelegramPlayerServiceImpl(TelegramPlayerDao telegramPlayerDao, PlayerService playerService, UserService userService) {
		this.telegramPlayerDao = telegramPlayerDao;
		this.playerService = playerService;
		this.userService = userService;
	}

	@Override
	@Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ApplicationException.class)
	public TelegramPlayer createPlayer(TelegramGame telegramGame, long userId, String username, int messageId) {
		Player player = playerService.create(telegramGame.getGame(), userId);

		if (!player.getUser().getName().equals(username)) {
			userService.rename(player.getUser(), username);
		}

		TelegramPlayer telegramPlayer = new TelegramPlayer();
		telegramPlayer.setPlayer(player);
		telegramPlayer.setMessageId(messageId);
		return telegramPlayerDao.create(telegramPlayer);
	}

	@Override
	@Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ApplicationException.class)
	public void deletePlayer(TelegramPlayer telegramPlayer) {
		telegramPlayerDao.delete(telegramPlayer);
		playerService.delete(telegramPlayer.getPlayer());
	}

	@Override
	@Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ApplicationException.class)
	public List<TelegramPlayer> deletePlayers(TelegramGame tgGame) {
		List<TelegramPlayer> telegramPlayerList = telegramPlayerDao.getByGame(tgGame.getGame());

		for (TelegramPlayer telegramPlayer : telegramPlayerList) {
			telegramPlayerDao.delete(telegramPlayer);
		}

		return telegramPlayerList;
	}

	@Override
	public TelegramPlayer getByUser(long userId) {
		return telegramPlayerDao.getByUser(userService.getById(userId));
	}

	@Override
	@Transactional(value = Transactional.TxType.SUPPORTS)
	public List<TelegramPlayer> getPlayers(TelegramGame tgGame) {
		return telegramPlayerDao.getByGame(tgGame.getGame());
	}

}
