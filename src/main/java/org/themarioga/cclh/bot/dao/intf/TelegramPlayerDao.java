package org.themarioga.cclh.bot.dao.intf;

import org.themarioga.cclh.bot.model.TelegramPlayer;
import org.themarioga.cclh.commons.dao.InterfaceHibernateDao;
import org.themarioga.cclh.commons.models.Game;
import org.themarioga.cclh.commons.models.User;

import java.util.List;

public interface TelegramPlayerDao extends InterfaceHibernateDao<TelegramPlayer> {

	TelegramPlayer getByUser(User User);
	List<TelegramPlayer> getByGame(Game game);

}
