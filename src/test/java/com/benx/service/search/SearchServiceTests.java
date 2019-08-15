package com.benx.service.search;

import com.benx.SearchHouseApplicationTests;
import com.benx.service.user.ServiceMultiResult;
import com.benx.web.form.RentSearch;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class SearchServiceTests extends SearchHouseApplicationTests {

    @Autowired
    private ISearchService searchService;

    @Test
    public void testIndex(){
        Long houseId = 15L;
        searchService.index(houseId);
    }

    @Test
    public void testRemove(){
        Long houseId = 15L;
        searchService.remove(houseId);
    }

    @Test
    public void testQuery(){
        RentSearch rentSearch = new RentSearch();
        rentSearch.setCityEnName("bj");
        rentSearch.setStart(0);
        rentSearch.setSize(10);
        //rentSearch.setPriceBlock("*-1000");   //ok
        //rentSearch.setKeywords("测试"); //error
        rentSearch.setAreaBlock("*-30");//OK
        ServiceMultiResult<Long> serviceResult = searchService.query(rentSearch);
        System.out.println(serviceResult.getResult().toString());
        Assert.assertEquals(2,serviceResult.getTotal());
    }

    //cityEnName=bj&
    // priceBlock=*-1000&
    // areaBlock=*&room=0&
    // direction=0&regionEnName=*&
    // rentWay=-1&
    // orderBy=lastUpdateTime&orderDirection=desc
    // &start=0&size=5
    @Test
    public void  queryhouse(){

    }
}
