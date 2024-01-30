package org.themarioga.cclh.bot.model;

import jakarta.persistence.*;
import org.themarioga.cclh.commons.models.Player;

import java.io.Serializable;

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
	public String toString() {
		return "TelegramPlayer{" +
				"player=" + player +
				", messageId=" + messageId +
				'}';
	}
}
