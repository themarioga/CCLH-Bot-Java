package org.themarioga.cclh.bot.app.intf;

import com.pengrad.telegrambot.model.CallbackQuery;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.response.SendResponse;
import jakarta.transaction.Transactional;
import org.themarioga.cclh.commons.exceptions.ApplicationException;

public interface ApplicationService {

    void run();

	void createGame(Message message, SendResponse groupResponse, SendResponse privateResponse, SendResponse playerResponse);

	@Transactional(value = Transactional.TxType.REQUIRED, rollbackOn = ApplicationException.class)
	void joinGame(CallbackQuery callbackQuery, SendResponse playerResponse);
}
