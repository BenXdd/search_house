package com.benx.web.controller.admin;

import com.benx.base.ApiDataTableResponse;
import com.benx.base.ApiResponse;
import com.benx.entity.SupportAddress;
import com.benx.service.ServiceResult;
import com.benx.service.house.IAddressService;
import com.benx.service.house.IHouseSerivce;
import com.benx.service.house.IQiNiuService;
import com.benx.service.user.ServiceMultiResult;
import com.benx.web.dto.*;
import com.benx.web.form.DatatableSearch;
import com.benx.web.form.HouseForm;
import com.google.gson.Gson;
import com.qiniu.common.QiniuException;
import com.qiniu.common.Zone;
import com.qiniu.http.Response;
import com.qiniu.storage.UploadManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

@Controller
public class AdminController {

    @Autowired
    private IQiNiuService qiNiuService;

    @Autowired
    private IAddressService addressService;

    @Autowired
    private IHouseSerivce houseSerivce;

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

    /**
     * 房屋列表页
     * @return
     */
    @GetMapping("admin/house/list")
    public String houesListPage(){
        return "admin/house-list";
    }

    @PostMapping("admin/houses")
    @ResponseBody
    public ApiDataTableResponse houses(@ModelAttribute DatatableSearch searchBody) {
        ServiceMultiResult<HouseDTO> result = houseSerivce.adminQuery(searchBody);

        ApiDataTableResponse response = new ApiDataTableResponse(ApiResponse.Status.SUCCESS);
        response.setData(result.getResult());
        response.setRecordsFiltered(result.getTotal());
        response.setRecordsTotal(result.getTotal());

        response.setDraw(searchBody.getDraw());
        return response;
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
     * 新增房源
     *  @ModelAttribute : 表单绑定作用
     *  @Valid :表单验证
     *  BindingResult :绑定返回结果
     * @param houseForm
     * @param bindingResult
     * @return
     */
    @PostMapping("admin/add/house")
    @ResponseBody
    public ApiResponse addHouse(@Valid @ModelAttribute("form-house-add") HouseForm houseForm, BindingResult bindingResult){
        if (bindingResult.hasErrors()){
            return new ApiResponse(HttpStatus.BAD_REQUEST.value(),bindingResult.getAllErrors().get(0).getDefaultMessage()
            ,null);
        }
        //上传图片失败 或者没有封面
        if (houseForm.getPhotos() == null || houseForm.getCover() == null){
            return ApiResponse.ofMessage(HttpStatus.BAD_REQUEST.value(), "必须上传图片");
        }

        //数据检验 地址信息防御检测
        Map<SupportAddress.Level,SupportAddressDTO> addressMap =  addressService.findCityAndRegion(houseForm.getCityEnName(),houseForm.getRegionEnName());
        //并没有一个城市 或者一个地区  city 或者region没有
        if (addressMap.keySet().size() != 2){
            return ApiResponse.ofStatus(ApiResponse.Status.NOT_VALID_PARAM);
        }
        ServiceResult<HouseDTO> result = houseSerivce.save(houseForm);
        if (result.isSuccess()){
            return ApiResponse.ofSuccess(result.getResult());
        }
        return ApiResponse.ofSuccess(ApiResponse.Status.NOT_VALID_PARAM);
    }


    /**
     * 房源信息编辑页面
     * @return
     */
    @GetMapping("admin/house/edit")
    public String houseEditPage(@RequestParam("id")Long id , Model model){

        if (id == null || id < 0){
            return "404";
        }
        ServiceResult<HouseDTO> serviceResult = houseSerivce.findCompleteOne(id);
        if (!serviceResult.isSuccess()){
            return "404";
        }
        HouseDTO houseDTO = serviceResult.getResult();
        //house信息
        model.addAttribute("house",houseDTO);
        //地点 城市 站点信息
        Map<SupportAddress.Level,SupportAddressDTO> addressMap = addressService.findCityAndRegion(houseDTO.getCityEnName(), houseDTO.getRegionEnName());
        model.addAttribute("city",addressMap.get(SupportAddress.Level.CITY));
        model.addAttribute("region",addressMap.get(SupportAddress.Level.REGION));

        HouseDetailDTO detailDTO = houseDTO.getHouseDetail();
        ServiceResult<SubwayDTO> subwayServiceResult = addressService.findSubway(detailDTO.getSubwayLineId());
        if (subwayServiceResult.isSuccess()){
            model.addAttribute("subway",subwayServiceResult.getResult());
        }
        ServiceResult<SubwayStationDTO> subwayStationServiceResult = addressService.findSubwayStation(detailDTO.getSubwayStationId());
        if (subwayStationServiceResult.isSuccess()){
            model.addAttribute("station",subwayStationServiceResult.getResult());
        }
        return "admin/house-edit";
    }

}
