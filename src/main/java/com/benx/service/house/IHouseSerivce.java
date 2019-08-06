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
    /**
     * 新增
     * @param houseForm
     * @return
     */
    ServiceResult<HouseDTO> save(HouseForm houseForm);

    /**
     * 编辑  不需要返回结果
     * @param houseForm
     * @return
     */
    ServiceResult update(HouseForm houseForm);

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

    /**
     * 移除图片接口
     * @param id
     * @return
     */
    ServiceResult removePhoto(Long id);

    /**
     * 修改封面接口
     * @param coverId
     * @param targetId
     * @return
     */
    ServiceResult updateCover(Long coverId, Long targetId);

    /**
     * 增加标签接口
     * @param houseId
     * @param tag
     * @return
     */
    ServiceResult addTag(Long houseId, String tag);

    /**
     * 移除标签接口
     * @param houseId
     * @param tag
     * @return
     */
    ServiceResult removeTag(Long houseId, String tag);
}
