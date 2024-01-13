package org.themarioga.cclh.bot.services.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.themarioga.cclh.bot.services.intf.CCLHService;
import org.themarioga.cclh.commons.services.intf.UserService;

@Service
public class CCLHServiceImpl implements CCLHService {

    private static Logger logger = LoggerFactory.getLogger(CCLHServiceImpl.class);

    private UserService userService;

    @Autowired
    public CCLHServiceImpl(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void registerUser(Long userId, String username) {
        userService.createOrReactivate(userId, username);
    }

}
