package org.themarioga.cclh.bot.dao.intf;

import org.themarioga.cclh.bot.model.TelegramGame;
import org.themarioga.cclh.commons.dao.InterfaceHibernateDao;
import org.themarioga.cclh.commons.models.Room;
import org.themarioga.cclh.commons.models.User;

public interface TelegramGameDao extends InterfaceHibernateDao<TelegramGame> {
	TelegramGame getByRoom(Room room);

	TelegramGame getByCreator(User creator);
}