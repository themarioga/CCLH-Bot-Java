package org.themarioga.cclh.bot.app.impl;

import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.themarioga.cclh.bot.model.TelegramGame;
import org.themarioga.cclh.bot.app.intf.CCLHService;
import org.themarioga.cclh.bot.model.TelegramPlayer;
import org.themarioga.cclh.bot.services.intf.TelegramGameService;
import org.themarioga.cclh.bot.services.intf.TelegramPlayerService;
import org.themarioga.cclh.commons.enums.GameTypeEnum;
import org.themarioga.cclh.commons.exceptions.ApplicationException;
import org.themarioga.cclh.commons.models.Deck;
import org.themarioga.cclh.commons.services.intf.*;

import java.util.List;

@Service
public class CCLHServiceImpl implements CCLHService {

    private static final Logger logger = LoggerFactory.getLogger(CCLHServiceImpl.class);

    private final UserService userService;
    private final DeckService deckService;
    private final ConfigurationService configurationService;
    private final TelegramGameService telegramGameService;
    private final TelegramPlayerService telegramPlayerService;

    @Autowired
    public CCLHServiceImpl(UserService userService, DeckService deckService, ConfigurationService configurationService, TelegramGameService telegramGameService, TelegramPlayerService telegramPlayerService) {
        this.userService = userService;
        this.deckService = deckService;
        this.configurationService = configurationService;
        this.telegramGameService = telegramGameService;
        this.telegramPlayerService = telegramPlayerService;
    }

    @Override
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ApplicationException.class)
    public void registerUser(Long userId, String username) {
        userService.createOrReactivate(userId, username);
    }

    @Override
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ApplicationException.class)
    public TelegramGame createGame(long roomId, String roomName, long creatorId, int groupMessageId, int privateMessageId, int playerMessageId) {
        TelegramGame telegramGame = telegramGameService.createGame(roomId, roomName, creatorId, groupMessageId, privateMessageId);

        TelegramPlayer telegramPlayer = telegramPlayerService.createPlayer(telegramGame, creatorId, playerMessageId);

        telegramGameService.joinGame(telegramGame, telegramPlayer);

        return telegramGame;
    }

    @Override
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ApplicationException.class)
    public List<TelegramPlayer> deleteGame(TelegramGame tgGame) {
        List<TelegramPlayer> players = telegramPlayerService.deletePlayers(tgGame);

        telegramGameService.deleteGame(tgGame);

        return players;
    }

    @Override
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ApplicationException.class)
    public void setType(TelegramGame tgGame, GameTypeEnum type) {
        telegramGameService.setType(tgGame, type);
    }

    @Override
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ApplicationException.class)
    public void setNumberOfCardsToWin(TelegramGame tgGame, int numberOfCardsToWin) {
        telegramGameService.setNumberOfCardsToWin(tgGame, numberOfCardsToWin);
    }

    @Override
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ApplicationException.class)
    public void setMaxNumberOfPlayers(TelegramGame tgGame, int maxNumberOfPlayers) {
        telegramGameService.setMaxNumberOfPlayers(tgGame, maxNumberOfPlayers);
    }

    @Override
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ApplicationException.class)
    public void setDeck(TelegramGame tgGame, long deckId) {
        telegramGameService.setDeck(tgGame, deckId);
    }

    @Override
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ApplicationException.class)
    public TelegramPlayer joinGame(TelegramGame tgGame, long userId, int messageId) {
        TelegramPlayer telegramPlayer = telegramPlayerService.createPlayer(tgGame, userId, messageId);

        telegramGameService.joinGame(tgGame, telegramPlayer);

        return telegramPlayer;
    }

    @Override
    @Transactional(value = Transactional.TxType.SUPPORTS)
    public TelegramGame getGame(long roomId) {
        return telegramGameService.getGame(roomId);
    }

    @Override
    @Transactional(value = Transactional.TxType.SUPPORTS)
    public TelegramGame getGameByCreatorId(long creatorId) {
        return telegramGameService.getGameByCreatorId(creatorId);
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

    @Override
    @Transactional(value = Transactional.TxType.SUPPORTS, rollbackOn = ApplicationException.class)
    public int getMinNumberOfPlayers() {
        return Integer.parseInt(configurationService.getConfiguration("game_min_number_of_players"));
    }

}
