package com.benx.service.house;

import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;
import com.qiniu.storage.BucketManager;
import com.qiniu.storage.UploadManager;
import com.qiniu.util.Auth;
import com.qiniu.util.StringMap;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.InputStream;


@Service
public class QiNiuServiceImpl implements IQiNiuService, InitializingBean {

    @Autowired
    private UploadManager uploadManager;  //上传实例
    @Autowired
    private BucketManager bucketManager;  //空间管理工具
    @Autowired
    private Auth auth; //认证信息
    @Value("${qiniu.Bucket}")
    private String bucket; //bucke的名字

    private StringMap putPolicy; //返回结果   实现InitializingBean  封装结果

    @Override
    public Response uploadFile(File file) throws QiniuException {
        Response response = this.uploadManager.put(file,null,getUploadToken()); //key置位null ,新增生成token的方法
        int retry = 0; //重传次数
        while (response.needRetry()&& retry<3){
            response = this.uploadManager.put(file,null,getUploadToken());
            retry++;
        }
        return response;
    }

    @Override
    public Response uploadFile(InputStream inputStream) throws QiniuException {
        Response response = this.uploadManager.put(inputStream,null,getUploadToken(),null,null); //key置位null ,新增生成token的方法
        int retry = 0; //重传次数
        while (response.needRetry()&& retry<3){
            response = this.uploadManager.put(inputStream,null,getUploadToken(),null,null);
            retry++;
        }
        return response;
    }

    @Override
    public Response delete(String key) throws QiniuException {
        Response response = bucketManager.delete(this.bucket,key);
        int retry = 0;
        if (response.needRetry() && retry < 3){
            response = bucketManager.delete(this.bucket,key);
            retry++;
        }
        return response;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.putPolicy = new StringMap();
        //新增width 和height
        putPolicy.put("returnBody", "{\"key\":\"$(key)\",\"hash\":\"$(etag)\"," +
                "\"bucket\":\"$(bucket)\",\"width\":$(imageInfo.width),\"height\":$(imageInfo.height)}");
    }

    /**
     * 获得上传凭证
     * @return
     */
    public String getUploadToken(){
        return this.auth.uploadToken(bucket,null,3600,putPolicy);
    }
}
