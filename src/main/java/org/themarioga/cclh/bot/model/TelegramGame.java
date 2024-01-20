package org.themarioga.cclh.bot.model;

import jakarta.persistence.*;
import org.themarioga.cclh.commons.models.Base;
import org.themarioga.cclh.commons.models.Game;

@Entity
@jakarta.persistence.Table(name = "t_telegram_game", uniqueConstraints = {@UniqueConstraint(columnNames = {"game_id"})})
public class TelegramGame extends Base {

	@Id
	@ManyToOne(cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
	@JoinColumn(name = "game_id", referencedColumnName = "id", nullable = false)
	private Game game;
	@Column(name = "group_message_id", nullable = false)
	private Integer groupMessageId;
	@Column(name = "private_message_id", nullable = false)
	private Integer privateMessageId;

	public Integer getGroupMessageId() {
		return groupMessageId;
	}

	public void setGroupMessageId(Integer groupMessageId) {
		this.groupMessageId = groupMessageId;
	}

	public Integer getPrivateMessageId() {
		return privateMessageId;
	}

	public void setPrivateMessageId(Integer privateMessageId) {
		this.privateMessageId = privateMessageId;
	}

	public Game getGame() {
		return game;
	}

	public void setGame(Game game) {
		this.game = game;
	}

	@Override
	public String toString() {
		return "TelegramGame{" +
				"id=" + game.getId() +
				", groupMessageId=" + groupMessageId +
				", privateMessageId=" + privateMessageId +
				'}';
	}
}
