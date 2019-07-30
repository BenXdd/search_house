package com.benx.entity;

import com.benx.SearchHouseApplicationTests;
import com.benx.repository.UserRepository;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class UserRepositoryTest extends SearchHouseApplicationTests{

    @Autowired
    private UserRepository userRepository;

    @Test
    public void testFindOne(){
        User user = userRepository.findOne(1L);
        Assert.assertEquals("waliwali",user.getName());
    }
}
