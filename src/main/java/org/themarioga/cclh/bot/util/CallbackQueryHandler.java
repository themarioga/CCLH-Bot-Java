package org.themarioga.cclh.bot.util;

import org.telegram.telegrambots.meta.api.objects.CallbackQuery;

public interface CallbackQueryHandler {

    void callback(CallbackQuery callbackQuery, String params);

}
