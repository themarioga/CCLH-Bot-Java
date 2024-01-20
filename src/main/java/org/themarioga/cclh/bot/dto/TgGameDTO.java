package org.themarioga.cclh.bot.dto;


import org.themarioga.cclh.bot.model.TelegramGame;

public class TgGameDTO {

	private Long roomId;
	private Long creatorId;
	private Integer groupMessageId;
	private Integer privateMessageId;

	public TgGameDTO(TelegramGame telegramGame) {
		roomId = telegramGame.getGame().getRoom().getId();
		creatorId = telegramGame.getGame().getCreator().getId();
		groupMessageId = telegramGame.getGroupMessageId();
		privateMessageId = telegramGame.getPrivateMessageId();
	}

	public Long getRoomId() {
		return roomId;
	}

	public void setRoomId(Long roomId) {
		this.roomId = roomId;
	}

	public Long getCreatorId() {
		return creatorId;
	}

	public void setCreatorId(Long creatorId) {
		this.creatorId = creatorId;
	}

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
}
