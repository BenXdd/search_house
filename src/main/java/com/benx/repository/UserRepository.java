package com.benx.repository;

import com.benx.entity.User;
import org.springframework.data.repository.CrudRepository;

//泛型: 1.user实体类  2.主键的泛型
public interface UserRepository extends CrudRepository<User, Long>{

    User findByName(String userName);
}
