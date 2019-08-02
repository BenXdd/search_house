package com.benx.repository;

import com.benx.entity.House;
import org.springframework.data.repository.CrudRepository;

public interface HouseRepository extends CrudRepository<House,Long> {
}
