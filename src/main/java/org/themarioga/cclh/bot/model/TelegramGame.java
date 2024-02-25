package org.themarioga.cclh.bot.model;

import jakarta.persistence.*;
import org.themarioga.cclh.commons.models.Game;

import java.io.Serializable;
import java.util.Objects;

@Entity
@jakarta.persistence.Table(name = "t_telegram_game", uniqueConstraints = {@UniqueConstraint(columnNames = {"game_id"})})
public class TelegramGame implements Serializable {

	@Id
	@ManyToOne(cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
	@JoinColumn(name = "game_id", referencedColumnName = "id", nullable = false)
	private Game game;
	@Column(name = "group_message_id", nullable = false)
	private Integer groupMessageId;
	@Column(name = "private_message_id", nullable = false)
	private Integer privateMessageId;
	@Column(name = "blackcard_message_id")
	private Integer blackCardMessageId;

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

	public Integer getBlackCardMessageId() {
		return blackCardMessageId;
	}

	public void setBlackCardMessageId(Integer blackCardMessageId) {
		this.blackCardMessageId = blackCardMessageId;
	}

	public Game getGame() {
		return game;
	}

	public void setGame(Game game) {
		this.game = game;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		TelegramGame that = (TelegramGame) o;
		return Objects.equals(game, that.game) && Objects.equals(groupMessageId, that.groupMessageId) && Objects.equals(privateMessageId, that.privateMessageId) && Objects.equals(blackCardMessageId, that.blackCardMessageId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(game, groupMessageId, privateMessageId, blackCardMessageId);
	}

	@Override
	public String toString() {
		return "TelegramGame{" +
				"id=" + game.getId() +
				", groupMessageId=" + groupMessageId +
				", privateMessageId=" + privateMessageId +
				", blackCardMessageId=" + blackCardMessageId +
				'}';
	}
}
