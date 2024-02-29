package org.themarioga.cclh.bot.game.dao.impl;

import org.springframework.stereotype.Repository;
import org.themarioga.cclh.bot.game.dao.intf.TelegramGameDao;
import org.themarioga.cclh.bot.game.model.TelegramGame;
import org.themarioga.cclh.commons.dao.AbstractHibernateDao;
import org.themarioga.cclh.commons.models.Game;
import org.themarioga.cclh.commons.models.Room;
import org.themarioga.cclh.commons.models.User;

@Repository
public class TelegramGameDaoImpl extends AbstractHibernateDao<TelegramGame> implements TelegramGameDao {

	public TelegramGameDaoImpl() {
		setClazz(TelegramGame.class);
	}

	@Override
	public TelegramGame getByGame(Game game) {
		return getCurrentSession().createQuery("SELECT t FROM TelegramGame t WHERE t.game=:game", TelegramGame.class).setParameter("game", game).getSingleResultOrNull();
	}

	@Override
	public TelegramGame getByRoom(Room room) {
		return getCurrentSession().createQuery("SELECT t FROM TelegramGame t WHERE t.game=(SELECT g FROM Game g WHERE g.room=:room)", TelegramGame.class).setParameter("room", room).getSingleResultOrNull();
	}

	@Override
	public TelegramGame getByCreator(User creator) {
		return getCurrentSession().createQuery("SELECT t FROM TelegramGame t WHERE t.game=(SELECT g FROM Game g WHERE g.creator=:creator)", TelegramGame.class).setParameter("creator", creator).getSingleResultOrNull();
	}

	@Override
	public TelegramGame getByPlayerUser(User user) {
		return getCurrentSession().createQuery("SELECT t FROM TelegramGame t WHERE t.game=(SELECT p.game FROM Player p WHERE p.user=:user)", TelegramGame.class).setParameter("user", user).getSingleResultOrNull();
	}

}
