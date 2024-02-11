package org.themarioga.cclh.bot.app.impl;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.themarioga.cclh.bot.model.TelegramGame;
import org.themarioga.cclh.bot.app.intf.CCLHService;
import org.themarioga.cclh.bot.model.TelegramPlayer;
import org.themarioga.cclh.bot.services.intf.TelegramGameService;
import org.themarioga.cclh.bot.services.intf.TelegramPlayerService;
import org.themarioga.cclh.commons.enums.GameTypeEnum;
import org.themarioga.cclh.commons.exceptions.ApplicationException;
import org.themarioga.cclh.commons.models.Dictionary;
import org.themarioga.cclh.commons.models.VotedCard;
import org.themarioga.cclh.commons.services.intf.*;

import java.util.List;

@Service
public class CCLHServiceImpl implements CCLHService {

    private final UserService userService;
    private final DictionaryService dictionaryService;
    private final ConfigurationService configurationService;
    private final TelegramGameService telegramGameService;
    private final TelegramPlayerService telegramPlayerService;

    @Autowired
    public CCLHServiceImpl(UserService userService, DictionaryService dictionaryService, ConfigurationService configurationService, TelegramGameService telegramGameService, TelegramPlayerService telegramPlayerService) {
        this.userService = userService;
        this.dictionaryService = dictionaryService;
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
    public void setBlackCardMessage(TelegramGame tgGame, int blackCardMessageId) {
        telegramGameService.setBlackCardMessage(tgGame, blackCardMessageId);
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
    public void setDictionary(TelegramGame tgGame, long dictionaryId) {
        telegramGameService.setDictionary(tgGame, dictionaryId);
    }

    @Override
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ApplicationException.class)
    public void joinGame(TelegramGame tgGame, long userId, int messageId) {
        TelegramPlayer telegramPlayer = telegramPlayerService.createPlayer(tgGame, userId, messageId);

        telegramGameService.joinGame(tgGame, telegramPlayer);
    }

    @Override
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ApplicationException.class)
    public void startGame(TelegramGame tgGame) {
        telegramGameService.startGame(tgGame);
    }

    @Override
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ApplicationException.class)
    public void startRound(TelegramGame tgGame) {
        telegramGameService.startRound(tgGame);
    }

    @Override
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ApplicationException.class)
    public void endRound(TelegramGame tgGame) {
        telegramGameService.endRound(tgGame);
    }

    @Override
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ApplicationException.class)
    public void voteForDeletion(TelegramGame tgGame, long userId){
        telegramGameService.voteForDeletion(tgGame, userId);
    }

    @Override
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ApplicationException.class)
    public void playCard(TelegramGame tgGame, long userId, long cardId) {
        telegramGameService.playCard(tgGame, userId, cardId);
    }

    @Override
    @Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ApplicationException.class)
    public void voteCard(TelegramGame tgGame, long userId, long cardId) {
        telegramGameService.voteCard(tgGame, userId, cardId);
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
    @Transactional(value = Transactional.TxType.SUPPORTS)
    public TelegramGame getByPlayerUser(long userId) {
        return telegramGameService.getByPlayerUser(userId);
    }

    @Override
    @Transactional(value = Transactional.TxType.SUPPORTS)
    public TelegramPlayer getPlayer(long userId) {
        return telegramPlayerService.getByUser(userId);
    }

    @Override
    @Transactional(value = Transactional.TxType.SUPPORTS)
    public List<TelegramPlayer> getPlayers(TelegramGame game) {
        return telegramPlayerService.getPlayers(game);
    }

    @Override
    @Transactional(value = Transactional.TxType.SUPPORTS)
    public List<Dictionary> getDictionariesPaginated(long creatorId, int firstResult, int maxResults) {
        return dictionaryService.getDictionariesPaginated(userService.getById(creatorId), firstResult, maxResults);
    }

    @Override
    @Transactional(value = Transactional.TxType.SUPPORTS)
    public VotedCard getMostVotedCard(TelegramGame tgGame) {
        return telegramGameService.getMostVotedCard(tgGame);
    }

    @Override
    @Transactional(value = Transactional.TxType.SUPPORTS)
    public long getDictionaryCount(long creatorId) {
        return dictionaryService.getDictionaryCount(userService.getById(creatorId));
    }

    @Override
    @Transactional(value = Transactional.TxType.SUPPORTS)
    public int getDictionariesPerPage() {
        return Integer.parseInt(configurationService.getConfiguration("game_dictionaries_per_page"));
    }

    @Override
    @Transactional(value = Transactional.TxType.SUPPORTS, rollbackOn = ApplicationException.class)
    public int getMinNumberOfPlayers() {
        return Integer.parseInt(configurationService.getConfiguration("game_min_number_of_players"));
    }

}
