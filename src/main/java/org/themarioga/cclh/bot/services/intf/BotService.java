package org.themarioga.cclh.bot.services.intf;

import org.themarioga.cclh.bot.model.CommandHandler;

import java.util.Map;

public interface BotService {

    void registerCallbacks(Map<String, CommandHandler> commands);

    void startBot();

    void sendTextResponse(Long chatId, String text);
}
