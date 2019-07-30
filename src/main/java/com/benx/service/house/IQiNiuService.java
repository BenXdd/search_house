package com.benx.service.house;

import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;

import java.io.File;
import java.io.InputStream;

/**
 * 七牛云服务
 */
public interface IQiNiuService {
    //文件
    Response uploadFile(File file)throws QiniuException;
    //文件流
    Response uploadFile(InputStream inputStream) throws QiniuException;
    //删除
    Response delete(String key) throws QiniuException;

}
