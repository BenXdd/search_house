package com.benx.service.house;

import com.benx.SearchHouseApplicationTests;
import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;

public class QiNiuServiceTest extends SearchHouseApplicationTests {

    @Autowired
    private IQiNiuService qiNiuService;
    @Test
    public void testUploadFile(){
        String fileName = "/Users/benx/IdeaProjects/search_house/tmp/WechatIMG5.jpeg";
        File file = new File(fileName);

        Assert.assertTrue(file.exists());

        try {
            Response response = qiNiuService.uploadFile(file);
            Assert.assertTrue(response.isOK());
        } catch (QiniuException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testDeletFile(){
        String key = "FjYbB9-SD2yI3BKdjcWk3u7Ok6aU";  //上传成功时会自动生成一个key
        try {
            Response response = qiNiuService.delete(key);
            Assert.assertTrue(response.isOK());
        } catch (QiniuException e) {
            e.printStackTrace();
        }
    }
}
