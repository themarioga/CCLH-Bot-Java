package org.themarioga.cclh.bot.services.impl;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.themarioga.cclh.bot.dao.intf.TelegramPlayerDao;
import org.themarioga.cclh.bot.model.TelegramGame;
import org.themarioga.cclh.bot.model.TelegramPlayer;
import org.themarioga.cclh.bot.services.intf.TelegramPlayerService;
import org.themarioga.cclh.commons.exceptions.ApplicationException;
import org.themarioga.cclh.commons.models.Player;
import org.themarioga.cclh.commons.services.intf.PlayerService;

import java.util.List;

@Service
public class TelegramPlayerServiceImpl implements TelegramPlayerService {

	private final TelegramPlayerDao telegramPlayerDao;
	private final PlayerService playerService;

	@Autowired
	public TelegramPlayerServiceImpl(TelegramPlayerDao telegramPlayerDao, PlayerService playerService) {
		this.telegramPlayerDao = telegramPlayerDao;
		this.playerService = playerService;
	}

	@Override
	@Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ApplicationException.class)
	public TelegramPlayer createPlayer(TelegramGame telegramGame, long userId, int messageId) {
		Player player = playerService.create(telegramGame.getGame(), userId);
		TelegramPlayer telegramPlayer = new TelegramPlayer();
		telegramPlayer.setPlayer(player);
		telegramPlayer.setMessageId(messageId);
		return telegramPlayerDao.create(telegramPlayer);
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
	@Transactional(value = Transactional.TxType.SUPPORTS)
	public List<TelegramPlayer> getPlayers(TelegramGame tgGame) {
		return telegramPlayerDao.getByGame(tgGame.getGame());
	}

}
