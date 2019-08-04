package com.benx.repository;

import com.benx.entity.HouseTag;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface HouseTagRepository extends CrudRepository<HouseTag,Long> {

    List<HouseTag> findAllByHouseId(Long id);
}
