package org.themarioga.cclh.bot.util;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.User;
import com.pengrad.telegrambot.request.SetWebhook;
import com.pengrad.telegrambot.response.BaseResponse;
import org.springframework.util.StringUtils;

import java.io.File;

public class BotUtils {

    public static void registerWebhook(TelegramBot bot, String url, String certPath) {
        SetWebhook request = new SetWebhook()
                .url(url)
                .certificate(new File(certPath)); // or file
        BaseResponse response = bot.execute(request);

        if (!response.isOk()) throw new RuntimeException("Cannot register webhook");
    }

    public static String getUserInfo(User user) {
        String output = String.valueOf(user.id());
        if (StringUtils.hasText(user.firstName()) || StringUtils.hasText(user.lastName()) || StringUtils.hasText(user.username())) {
            output += " [" + getUsername(user) + "]";
        }

        return output;
    }

    public static String getUsername(User user) {
        String output = "";
        if (StringUtils.hasText(user.firstName())) output += user.firstName();
        if (StringUtils.hasText(user.lastName())) output += user.lastName();
        if (StringUtils.hasText(user.username())) output += "(" + user.username() + ")";
        return output;
    }

}
