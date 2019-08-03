package com.benx.service.house;

import com.benx.entity.SupportAddress;
import com.benx.service.ServiceResult;
import com.benx.service.user.ServiceMultiResult;
import com.benx.web.dto.SubwayDTO;
import com.benx.web.dto.SubwayStationDTO;
import com.benx.web.dto.SupportAddressDTO;

import java.util.List;
import java.util.Map;


/**
 * 地址服务接口
 */
public interface IAddressService {

    /**
     * 支持所有城市列表
     * @return
     */
    ServiceMultiResult<SupportAddressDTO> findAllCities();

    /**
     * 根据英文简写获取具体区域信息
     * @param cityEnName
     * @param regionEnName
     * @return
     */
    Map<SupportAddress.Level,SupportAddressDTO> findCityAndRegion(String cityEnName,String regionEnName);

    /**
     * 根据城市英文简写获取该市所有可用的区域信息
     * @param cityEnName
     * @return
     */
    ServiceMultiResult findAllRegionByCityName(String cityEnName);

    /**
     * 获取该城市所有地铁线路
     * @param cityEnName
     * @return
     */
    List<SubwayDTO> findAllSubwayByCity(String cityEnName);

    /**
     * 获取地铁线路所有站点
     * @param subwayId
     * @return
     */
    List<SubwayStationDTO> findAllStationBySubway(Long subwayId);

    /**
     * 获取地铁信息
     * @param subwayId
     * @return
     */
    ServiceResult<SubwayDTO> findSubway(Long subwayId);

    /**
     * 获取地铁站点信息
     * @param stationId
     * @return
     */
    ServiceResult<SubwayStationDTO> findSubwayStation(Long stationId);

    /**
     * 根据城市英文简写获取城市详细信息
     * @param cityEnName
     * @return
     */
    ServiceResult<SupportAddressDTO> findCity(String cityEnName);
}
