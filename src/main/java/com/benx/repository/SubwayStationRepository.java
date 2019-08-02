package com.benx.repository;

import com.benx.entity.SubwayStation;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface SubwayStationRepository extends CrudRepository<SubwayStation,Long> {
    List<SubwayStation> findAllBySubwayId(Long subwayId);
}
