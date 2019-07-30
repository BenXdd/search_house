package com.benx.service;

import com.benx.entity.User;

/**
 * 用户服务
 */
public interface IUserService {

    User findUserByName(String userName);
}
