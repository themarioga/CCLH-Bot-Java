package org.themarioga.cclh.bot.service.intf;

import org.themarioga.cclh.bot.util.CallbackQueryHandler;
import org.themarioga.cclh.bot.util.CommandHandler;

import java.util.Map;

public interface ApplicationService {

	Map<String, CommandHandler> getBotCommands();

	Map<String, CallbackQueryHandler> getCallbackQueries();

}
