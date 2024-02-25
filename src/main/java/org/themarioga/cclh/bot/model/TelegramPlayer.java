package org.themarioga.cclh.bot.model;

import jakarta.persistence.*;
import org.themarioga.cclh.commons.models.Player;

import java.io.Serializable;
import java.util.Objects;

@Entity
@jakarta.persistence.Table(name = "t_telegram_player", uniqueConstraints = {@UniqueConstraint(columnNames = {"player_id"})})
public class TelegramPlayer implements Serializable {

	@Id
	@ManyToOne(cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
	@JoinColumn(name = "player_id", referencedColumnName = "id", nullable = false)
	private Player player;
	@Column(name = "message_id", nullable = false)
	private Integer messageId;

	public Player getPlayer() {
		return player;
	}

	public void setPlayer(Player player) {
		this.player = player;
	}

	public Integer getMessageId() {
		return messageId;
	}

	public void setMessageId(Integer messageId) {
		this.messageId = messageId;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		TelegramPlayer that = (TelegramPlayer) o;
		return Objects.equals(player, that.player) && Objects.equals(messageId, that.messageId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(player, messageId);
	}

	@Override
	public String toString() {
		return "TelegramPlayer{" +
				"player=" + player +
				", messageId=" + messageId +
				'}';
	}
}
