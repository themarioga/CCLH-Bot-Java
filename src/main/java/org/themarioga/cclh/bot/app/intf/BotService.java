package org.themarioga.cclh.bot.app.intf;

import com.pengrad.telegrambot.Callback;
import com.pengrad.telegrambot.Cancellable;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.BaseRequest;
import com.pengrad.telegrambot.response.BaseResponse;
import org.themarioga.cclh.bot.util.CallbackQueryHandler;
import org.themarioga.cclh.bot.util.CommandHandler;

import java.util.List;
import java.util.Map;

public interface BotService {

	void startBot(Map<String, CommandHandler> commands, Map<String, CallbackQueryHandler> callbackQueries);

	int handleUpdates(Map<String, CommandHandler> commands, Map<String, CallbackQueryHandler> callbackQueries, List<Update> updates);

    <T extends BaseRequest<T, R>, R extends BaseResponse> Cancellable sendMessage(T request);

    <T extends BaseRequest<T, R>, R extends BaseResponse> R sendMessageSync(BaseRequest<T, R> request);

    <T extends BaseRequest<T, R>, R extends BaseResponse> Cancellable sendMessageAsync(T request, Callback<T, R> callback);

}
