package org.themarioga.cclh.bot.model;

import com.pengrad.telegrambot.model.Message;

public interface CommandHandler {

    void callback(Message message);

}
