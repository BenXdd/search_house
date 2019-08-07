package com.benx.service;

import com.benx.entity.User;
import com.benx.web.dto.UserDTO;

/**
 * 用户服务
 */
public interface IUserService {

    User findUserByName(String userName);

    ServiceResult<UserDTO> findById(Long adminId);
}
