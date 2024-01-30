package org.themarioga.cclh.bot.util;

import com.pengrad.telegrambot.model.Message;

public interface CommandHandler {

    void callback(Message message);

}
