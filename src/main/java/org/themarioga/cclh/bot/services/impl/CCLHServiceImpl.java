package org.themarioga.cclh.bot.services.impl;

import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.themarioga.cclh.bot.dao.intf.TelegramGameDao;
import org.themarioga.cclh.bot.model.TelegramGame;
import org.themarioga.cclh.bot.services.intf.CCLHService;
import org.themarioga.cclh.commons.enums.ErrorEnum;
import org.themarioga.cclh.commons.enums.GameTypeEnum;
import org.themarioga.cclh.commons.exceptions.ApplicationException;
import org.themarioga.cclh.commons.exceptions.game.GameAlreadyExistsException;
import org.themarioga.cclh.commons.exceptions.user.UserAlreadyExistsException;
import org.themarioga.cclh.commons.models.Deck;
import org.themarioga.cclh.commons.models.Game;
import org.themarioga.cclh.commons.services.intf.*;

import java.util.List;

@Service
public class CCLHServiceImpl implements CCLHService {

    private static final Logger logger = LoggerFactory.getLogger(CCLHServiceImpl.class);

    private final UserService userService;
    private final RoomService roomService;
    private final DeckService deckService;
    private final GameService gameService;
    private final ConfigurationService configurationService;
    private final TelegramGameDao telegramGameDao;

    @Autowired
    public CCLHServiceImpl(UserService userService, RoomService roomService, DeckService deckService, GameService gameService, ConfigurationService configurationService, TelegramGameDao telegramGameDao) {
        this.userService = userService;
        this.roomService = roomService;
        this.deckService = deckService;
        this.gameService = gameService;
        this.configurationService = configurationService;
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
    @Transactional(value = Transactional.TxType.SUPPORTS)
    public TelegramGame getGame(long roomId) {
        return telegramGameDao.getByRoom(roomService.getById(roomId));
    }

    @Override
    @Transactional(value = Transactional.TxType.SUPPORTS)
    public TelegramGame getGameByCreatorId(long creatorId) {
        return telegramGameDao.getByCreator(userService.getById(creatorId));
    }

    @Override
    public List<Deck> getDeckPaginated(long creatorId, int firstResult, int maxResults) {
        return deckService.getDeckPaginated(userService.getById(creatorId), firstResult, maxResults);
    }

    @Override
    @Transactional(value = Transactional.TxType.SUPPORTS)
    public long getDeckCount(long creatorId) {
        return deckService.getDeckCount(userService.getById(creatorId));
    }

    @Override
    @Transactional(value = Transactional.TxType.SUPPORTS)
    public int getDecksPerPage() {
        return Integer.parseInt(configurationService.getConfiguration("game_dictionaries_per_page"));
    }

}
