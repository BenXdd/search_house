package com.benx.service.house;

import com.benx.entity.Subway;
import com.benx.entity.SubwayStation;
import com.benx.entity.SupportAddress;
import com.benx.repository.SubwayRepository;
import com.benx.repository.SubwayStationRepository;
import com.benx.repository.SupportAddressRepository;
import com.benx.service.ServiceResult;
import com.benx.service.user.ServiceMultiResult;
import com.benx.web.dto.SubwayDTO;
import com.benx.web.dto.SubwayStationDTO;
import com.benx.web.dto.SupportAddressDTO;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AddressServiceImpl implements IAddressService{
    @Autowired
    private SupportAddressRepository supportAddressRepository;

    @Autowired
    private SubwayRepository subwayRepository;

    @Autowired
    private SubwayStationRepository subwayStationRepository;



    @Autowired
    private ModelMapper modelMapper;
    @Override
    public ServiceMultiResult<SupportAddressDTO> findAllCities() {
        List<SupportAddress> address = supportAddressRepository.findAllByLevel(SupportAddress.Level.CITY.getValue());

        //SupportAddress ---> SupportAddressDTO
        List<SupportAddressDTO> addressDTOS = new ArrayList<>();
        //webMvc 加入
        for (SupportAddress supportAddress : address) {
            SupportAddressDTO target = modelMapper.map(supportAddress,SupportAddressDTO.class);
            addressDTOS.add(target);
        }
        return new ServiceMultiResult<>(addressDTOS.size(),addressDTOS);
    }

    @Override
    public Map<SupportAddress.Level, SupportAddressDTO> findCityAndRegion(String cityEnName, String regionEnName) {
        Map<SupportAddress.Level, SupportAddressDTO> result = new HashMap<>();
        SupportAddress city = supportAddressRepository.findByEnNameAndLevel(cityEnName,SupportAddress.Level.CITY.getValue());
        SupportAddress region = supportAddressRepository.findByEnNameAndBelongTo(regionEnName,city.getEnName());
        result.put(SupportAddress.Level.CITY, modelMapper.map(city, SupportAddressDTO.class));
        result.put(SupportAddress.Level.REGION, modelMapper.map(region, SupportAddressDTO.class));
        return result;
    }

    @Override
    public ServiceMultiResult findAllRegionByCityName(String cityEnName) {
        if (cityEnName == null) {
            return new ServiceMultiResult(0, null);
        }
        List<SupportAddressDTO> result = new ArrayList<>();

        List<SupportAddress> regions = supportAddressRepository.findAllByLevelAndBelongTo(SupportAddress.Level.REGION
                .getValue(), cityEnName);
        for (SupportAddress region : regions) {
            result.add(modelMapper.map(region,SupportAddressDTO.class));
        }
        return new ServiceMultiResult(regions.size(), result);
    }

    @Override
    public List<SubwayDTO> findAllSubwayByCity(String cityEnName) {
        List<SubwayDTO> result = new ArrayList<>();
        List<Subway> subways = subwayRepository.findAllByCityEnName(cityEnName);
        if (subways.isEmpty()){
            return result;
        }
        for (Subway subway : subways) {
            result.add(modelMapper.map(subway,SubwayDTO.class));
        }
        //subways.forEach(subway -> result.add(modelMapper.map(subway, SubwayDTO.class)));
        return result;
    }

    @Override
    public List<SubwayStationDTO> findAllStationBySubway(Long subwayId) {
        List<SubwayStationDTO> result = new ArrayList<>();
        List<SubwayStation> subwayStations = subwayStationRepository.findAllBySubwayId(subwayId);
       if (subwayStations.isEmpty()){
           return result;
       }

       subwayStations.forEach(subwayStation -> result.add(modelMapper.map(subwayStation,SubwayStationDTO.class)));
        return result;
    }

    @Override
    public ServiceResult<SubwayDTO> findSubway(Long subwayId) {
        if (subwayId == null){
            return ServiceResult.notFound();
        }
        Subway subway = subwayRepository.findOne(subwayId);
        if (subway == null) {
            return ServiceResult.notFound();
        }
        return ServiceResult.of(modelMapper.map(subway, SubwayDTO.class));

    }

    @Override
    public ServiceResult<SubwayStationDTO> findSubwayStation(Long stationId) {
        if (stationId == null){
            return ServiceResult.notFound();
        }
        SubwayStation subwayStation = subwayStationRepository.findOne(stationId);
        if (subwayStation == null){
            return ServiceResult.notFound();
        }

        return ServiceResult.of(modelMapper.map(subwayStation,SubwayStationDTO.class));
    }

    @Override
    public ServiceResult<SupportAddressDTO> findCity(String cityEnName) {
        if (cityEnName == null){
            return ServiceResult.notFound();
        }
        SupportAddress address = supportAddressRepository.findByEnNameAndLevel(cityEnName,SupportAddress.Level.CITY.getValue());
        if (address == null){
            return ServiceResult.notFound();
        }

        return ServiceResult.of(modelMapper.map(address,SupportAddressDTO.class));
    }
}
