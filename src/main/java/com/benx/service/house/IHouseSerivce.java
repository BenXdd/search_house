package com.benx.service.house;

import com.benx.service.ServiceResult;
import com.benx.service.user.ServiceMultiResult;
import com.benx.web.dto.HouseDTO;
import com.benx.web.form.DatatableSearch;
import com.benx.web.form.HouseForm;

/**
 * 房屋管理服务接口
 */
public interface IHouseSerivce {
    ServiceResult<HouseDTO> save(HouseForm houseForm);

    /**
     * 查询房源信息
     * @param searchBody
     * @return
     */
    ServiceMultiResult<HouseDTO> adminQuery(DatatableSearch searchBody);

    /**
     * 查询完整房源信息
     * @param id
     * @return
     */
    ServiceResult<HouseDTO> findCompleteOne(Long id);

}
