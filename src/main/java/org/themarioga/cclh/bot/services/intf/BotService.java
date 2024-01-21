package org.themarioga.cclh.bot.services.intf;

import com.pengrad.telegrambot.Callback;
import com.pengrad.telegrambot.Cancellable;
import com.pengrad.telegrambot.request.BaseRequest;
import com.pengrad.telegrambot.response.BaseResponse;
import org.themarioga.cclh.bot.model.CallbackQueryHandler;
import org.themarioga.cclh.bot.model.CommandHandler;

import java.util.Map;

public interface BotService {

    void registerCallbacks(Map<String, CommandHandler> commands, Map<String, CallbackQueryHandler> callbackQueries);

    void startBot();

    <T extends BaseRequest<T, R>, R extends BaseResponse> Cancellable sendMessage(T request);

    <T extends BaseRequest<T, R>, R extends BaseResponse> R sendMessageSync(BaseRequest<T, R> request);

    <T extends BaseRequest<T, R>, R extends BaseResponse> Cancellable sendMessageAsync(T request, Callback<T, R> callback);
}
