package org.themarioga.cclh.bot.services.impl;

import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.themarioga.cclh.bot.dao.intf.TelegramGameDao;
import org.themarioga.cclh.bot.dto.TgGameDTO;
import org.themarioga.cclh.bot.model.TelegramGame;
import org.themarioga.cclh.bot.services.intf.CCLHService;
import org.themarioga.cclh.commons.enums.ErrorEnum;
import org.themarioga.cclh.commons.exceptions.ApplicationException;
import org.themarioga.cclh.commons.exceptions.game.GameAlreadyExistsException;
import org.themarioga.cclh.commons.exceptions.user.UserAlreadyExistsException;
import org.themarioga.cclh.commons.models.Game;
import org.themarioga.cclh.commons.services.intf.GameService;
import org.themarioga.cclh.commons.services.intf.RoomService;
import org.themarioga.cclh.commons.services.intf.UserService;

@Service
public class CCLHServiceImpl implements CCLHService {

    private static final Logger logger = LoggerFactory.getLogger(CCLHServiceImpl.class);

    private final UserService userService;
    private final RoomService roomService;
    private final GameService gameService;
    private final TelegramGameDao telegramGameDao;

    @Autowired
    public CCLHServiceImpl(UserService userService, RoomService roomService, GameService gameService, TelegramGameDao telegramGameDao) {
        this.userService = userService;
        this.roomService = roomService;
        this.gameService = gameService;
        this.telegramGameDao = telegramGameDao;
    }

    @Override
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ApplicationException.class)
    public boolean registerUser(Long userId, String username) {
        try {
            userService.createOrReactivate(userId, username);

            return true;
        } catch (UserAlreadyExistsException e) {
            logger.error("El usuario {} ({}) esta intentando registrarse de nuevo.", userId, username);

            return false;
        }
    }

    @Override
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ApplicationException.class)
    public TelegramGame createGame(long roomId, String roomName, long creatorId, int groupMessageId, int privateMessageId) {
        try {
            Game game = gameService.create(roomId, roomName, creatorId);
            if (game == null) throw new ApplicationException(ErrorEnum.GAME_NOT_FOUND);

            TelegramGame telegramGame = new TelegramGame();
            telegramGame.setGame(game);
            telegramGame.setGroupMessageId(groupMessageId);
            telegramGame.setPrivateMessageId(privateMessageId);
            return telegramGameDao.create(telegramGame);
        } catch (GameAlreadyExistsException e) {
            logger.error("Ya existe una partida para al sala {} ({}) o creado por {}.", roomId, roomName, creatorId);

            return null;
        }
    }

    @Override
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ApplicationException.class)
    public TgGameDTO deleteGame(long roomId) {
        TelegramGame telegramGame = telegramGameDao.getByRoom(roomService.getById(roomId));
        TgGameDTO dto = new TgGameDTO(telegramGame);
        telegramGameDao.delete(telegramGame);
        gameService.delete(roomId);
        return dto;
    }

    @Override
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ApplicationException.class)
    public TgGameDTO deleteGameByUserId(long creatorId) {
        TelegramGame telegramGame = telegramGameDao.getByCreator(userService.getById(creatorId));
        TgGameDTO dto = new TgGameDTO(telegramGame);
        telegramGameDao.delete(telegramGame);
        gameService.deleteByCreatorId(creatorId);
        return dto;
    }

}
