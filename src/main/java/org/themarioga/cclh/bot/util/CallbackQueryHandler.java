package org.themarioga.cclh.bot.util;

import com.pengrad.telegrambot.model.CallbackQuery;

public interface CallbackQueryHandler {

    void callback(CallbackQuery callbackQuery, String params);

}
