package com.benx.service.house;

import com.benx.service.ServiceResult;
import com.benx.web.dto.HouseDTO;

/**
 * 房屋管理服务接口
 */
public interface IHouseSerivce {
    ServiceResult<HouseDTO> save();
}
