package com.benx.web.controller.admin;

import com.benx.base.ApiResponse;
import com.benx.service.house.IQiNiuService;
import com.benx.web.dto.QiNiuPutRet;
import com.google.gson.Gson;
import com.qiniu.common.QiniuException;
import com.qiniu.common.Zone;
import com.qiniu.http.Response;
import com.qiniu.storage.UploadManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

@Controller
public class AdminController {

    @Autowired
    private IQiNiuService qiNiuService;

    @Autowired
    private Gson gson;

    /**
     * 后台管理中心
     */
    @GetMapping("/admin/center")
    public String adminCenterPage(){
        return "admin/center";
    }

    /**
     * 欢迎页
     * @return
     */
    @GetMapping("/admin/welcome")
    public String adminWelcomePage(){
        return "admin/welcome";
    }

    @GetMapping("/admin/login")
    public String adminLoginPage(){
        return "admin/login";
    }

    @GetMapping("/admin/add/house")
    public String addHousePage(){
        return "admin/house-add";
    }

    @PostMapping(value = "/admin/upload/photo",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    public ApiResponse uploadPhoto(@RequestParam("file") MultipartFile file){

        if (file.isEmpty()){
            return ApiResponse.ofStatus(ApiResponse.Status.NOT_VALID_PARAM);
        }

        //获取filename
        String fileName = file.getOriginalFilename();
        try {
            InputStream inputStream = file.getInputStream();
            Response response = qiNiuService.uploadFile(inputStream);
            if (response.isOK()){
                //使用gson解析返回结果 使用定义的dto格式
              QiNiuPutRet ret = gson.fromJson(response.bodyString(), QiNiuPutRet.class);
                return ApiResponse.ofSuccess(ret);
            }
            else {
                return ApiResponse.ofMessage(response.statusCode,response.getInfo());
            }
        } catch (QiniuException e1){
            e1.printStackTrace();
            return ApiResponse.ofStatus(ApiResponse.Status.INTERNAL_SERVER_ERROR);
        }catch (IOException e) {
            return ApiResponse.ofStatus(ApiResponse.Status.INTERNAL_SERVER_ERROR);
        }

        /**
         * 上传图片保存到本地
         *
         *
         //保存起来,存放在我们的tmo目录下
         File target = new File("/Users/benx/IdeaProjects/search_house/tmp/"+fileName);

         try {
         file.transferTo(target);
         } catch (IOException e) {
         e.printStackTrace();
         return ApiResponse.ofStatus(ApiResponse.Status.INTERNAL_SERVER_ERROR);
         }
         return ApiResponse.ofSuccess(null);
         *
         */

    }

    /**
     * 华东机房
     *
     *  华东	Zone.zone0()
     *  华北	Zone.zone1()
     *  华南	Zone.zone2()
     *  北美	Zone.zoneNa0()
     *  东南亚	Zone.zoneAs0()
     *  refer https://developer.qiniu.com/kodo/sdk/1239/java
     */
    @Bean
    public com.qiniu.storage.Configuration qiniuConfig(){
        return new com.qiniu.storage.Configuration(Zone.zone0());
    }

    /**
     * 构建七牛上传工具实例
     */
    @Bean
    public UploadManager uploadManager(){
        return new UploadManager(qiniuConfig());
    }

}
