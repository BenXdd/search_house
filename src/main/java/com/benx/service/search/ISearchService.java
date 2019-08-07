package com.benx.service.search;

import com.benx.service.user.ServiceMultiResult;
import com.benx.web.form.RentSearch;

public interface ISearchService {

    /**
     * 查询房源接口
     * @param rentSearch
     * @return
     */
    ServiceMultiResult<Long> query(RentSearch rentSearch);
}
