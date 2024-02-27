package org.themarioga.cclh.bot.util;

import org.springframework.util.StringUtils;
import org.telegram.telegrambots.meta.api.objects.User;

public class BotUtils {

    private BotUtils() {
        throw new UnsupportedOperationException();
    }

    public static String getUserInfo(User user) {
        String output = String.valueOf(user.getId());
        if (StringUtils.hasText(user.getFirstName()) || StringUtils.hasText(user.getLastName()) || StringUtils.hasText(user.getUserName())) {
            output += " [" + getUsername(user) + "]";
        }

        return output;
    }

    public static String getUsername(User user) {
        String output = "";
        if (StringUtils.hasText(user.getFirstName())) output += user.getFirstName();
        if (StringUtils.hasText(user.getLastName())) output += " " + user.getLastName();
        if (StringUtils.hasText(user.getUserName())) output += " (@" + user.getUserName() + ")";
        return output;
    }

}
