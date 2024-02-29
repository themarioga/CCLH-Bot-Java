package org.themarioga.cclh.bot.game.dao.impl;

import org.springframework.stereotype.Repository;
import org.themarioga.cclh.bot.game.dao.intf.TelegramPlayerDao;
import org.themarioga.cclh.bot.game.model.TelegramPlayer;
import org.themarioga.cclh.commons.dao.AbstractHibernateDao;
import org.themarioga.cclh.commons.models.Game;
import org.themarioga.cclh.commons.models.User;

import java.util.List;

@Repository
public class TelegramPlayerDaoImpl extends AbstractHibernateDao<TelegramPlayer> implements TelegramPlayerDao {

	public TelegramPlayerDaoImpl() {
		setClazz(TelegramPlayer.class);
	}

	@Override
	public TelegramPlayer getByUser(User user) {
		return getCurrentSession().createQuery("SELECT t FROM TelegramPlayer t WHERE t.player=(SELECT p FROM Player p WHERE p.user=:user)", TelegramPlayer.class).setParameter("user", user).getSingleResultOrNull();
	}

	@Override
	public List<TelegramPlayer> getByGame(Game game) {
		return getCurrentSession().createQuery("SELECT t FROM TelegramPlayer t WHERE t.player IN (SELECT p FROM Player p WHERE p.game=:game)", TelegramPlayer.class).setParameter("game", game).getResultList();
	}
}
