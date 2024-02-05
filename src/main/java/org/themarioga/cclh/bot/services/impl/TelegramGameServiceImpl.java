package org.themarioga.cclh.bot.services.impl;

import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.themarioga.cclh.bot.dao.intf.TelegramGameDao;
import org.themarioga.cclh.bot.model.TelegramGame;
import org.themarioga.cclh.bot.model.TelegramPlayer;
import org.themarioga.cclh.bot.services.intf.TelegramGameService;
import org.themarioga.cclh.commons.enums.ErrorEnum;
import org.themarioga.cclh.commons.enums.GameTypeEnum;
import org.themarioga.cclh.commons.enums.TableStatusEnum;
import org.themarioga.cclh.commons.exceptions.ApplicationException;
import org.themarioga.cclh.commons.models.Game;
import org.themarioga.cclh.commons.services.intf.GameService;
import org.themarioga.cclh.commons.services.intf.RoomService;
import org.themarioga.cclh.commons.services.intf.UserService;

@Service
public class TelegramGameServiceImpl implements TelegramGameService {

	private static final Logger logger = LoggerFactory.getLogger(TelegramGameServiceImpl.class);

	private final TelegramGameDao telegramGameDao;
	private final GameService gameService;
	private final RoomService roomService;
	private final UserService userService;

	@Autowired
	public TelegramGameServiceImpl(TelegramGameDao telegramGameDao, RoomService roomService, GameService gameService, UserService userService) {
		this.telegramGameDao = telegramGameDao;
		this.roomService = roomService;
		this.gameService = gameService;
		this.userService = userService;
	}

	@Override
	@Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ApplicationException.class)
	public TelegramGame createGame(long roomId, String roomName, long creatorId, int groupMessageId, int privateMessageId) {
		logger.trace("Creating game {}, {}, {}, {}, {}", roomId, roomName, creatorId, groupMessageId, privateMessageId);

		Game game = gameService.create(roomId, roomName, creatorId);
		if (game == null) throw new ApplicationException(ErrorEnum.GAME_NOT_FOUND);

		TelegramGame telegramGame = new TelegramGame();
		telegramGame.setGame(game);
		telegramGame.setGroupMessageId(groupMessageId);
		telegramGame.setPrivateMessageId(privateMessageId);
		return telegramGameDao.create(telegramGame);
	}

	@Override
	@Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ApplicationException.class)
	public void deleteGame(TelegramGame tgGame) {
		telegramGameDao.delete(tgGame);
		gameService.delete(tgGame.getGame());
	}

	@Override
	@Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ApplicationException.class)
	public void setType(TelegramGame tgGame, GameTypeEnum type) {
		tgGame.setGame(gameService.setType(tgGame.getGame(), type));
	}

	@Override
	@Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ApplicationException.class)
	public void setNumberOfCardsToWin(TelegramGame tgGame, int numberOfCardsToWin) {
		tgGame.setGame(gameService.setNumberOfCardsToWin(tgGame.getGame(), numberOfCardsToWin));
	}

	@Override
	@Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ApplicationException.class)
	public void setMaxNumberOfPlayers(TelegramGame tgGame, int maxNumberOfPlayers) {
		tgGame.setGame(gameService.setMaxNumberOfPlayers(tgGame.getGame(), maxNumberOfPlayers));
	}

	@Override
	@Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ApplicationException.class)
	public void setDeck(TelegramGame tgGame, long deckId) {
		tgGame.setGame(gameService.setDeck(tgGame.getGame(), deckId));
	}

	@Override
	@Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ApplicationException.class)
	public void joinGame(TelegramGame tgGame, TelegramPlayer tgPlayer) {
		gameService.addPlayer(tgGame.getGame(), tgPlayer.getPlayer());
	}

	@Override
	@Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ApplicationException.class)
	public void startGame(TelegramGame tgGame) {
		gameService.startGame(tgGame.getGame());

		if (tgGame.getGame().getTable().getStatus().equals(TableStatusEnum.STARTING)) {
			gameService.startRound(tgGame.getGame());
		} else {
			logger.error("La partida no se ha iniciado correctamente");
		}
	}

	@Override
	@Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ApplicationException.class)
	public void voteForDeletion(TelegramGame tgGame, long userId) {
		gameService.voteForDeletion(tgGame.getGame(), userId);
	}

	@Override
	@Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ApplicationException.class)
	public void playCard(TelegramGame tgGame, long userId, long cardId) {
		gameService.playCard(tgGame.getGame(), userId, cardId);
	}

	@Override
	@Transactional(value = Transactional.TxType.SUPPORTS)
	public TelegramGame getGame(long roomId) {
		return telegramGameDao.getByRoom(roomService.getById(roomId));
	}

	@Override
	@Transactional(value = Transactional.TxType.SUPPORTS)
	public TelegramGame getGameByCreatorId(long creatorId) {
		return telegramGameDao.getByCreator(userService.getById(creatorId));
	}

}
