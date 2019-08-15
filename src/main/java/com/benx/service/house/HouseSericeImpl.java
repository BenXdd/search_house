package com.benx.service.house;

import com.benx.base.HouseSort;
import com.benx.base.HouseStatus;
import com.benx.base.LoginUserUtil;
import com.benx.entity.*;
import com.benx.repository.*;
import com.benx.service.ServiceResult;
import com.benx.service.search.ISearchService;
import com.benx.service.user.ServiceMultiResult;
import com.benx.web.dto.HouseDTO;
import com.benx.web.dto.HouseDetailDTO;
import com.benx.web.dto.HousePictureDTO;
import com.benx.web.form.DatatableSearch;
import com.benx.web.form.HouseForm;
import com.benx.web.form.PhotoForm;
import com.benx.web.form.RentSearch;
import com.google.common.collect.Maps;
import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.Predicate;
import java.util.*;

@Service
public class HouseSericeImpl implements IHouseSerivce {
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private HouseRepository houseRepository;
    @Autowired
    private HouseDetailRepository houseDetailRepository;
    @Autowired
    private HousePictureRepository housePictureRepository;
    @Autowired
    private HouseTagRepository houseTagRepository;
    @Autowired
    private SubwayRepository subwayRepository;
    @Autowired
    private SubwayStationRepository subwayStationRepository;
    //    @Autowired
//    private HouseSubscribeRespository subscribeRespository;
    @Autowired
    private ISearchService searchService;
    @Autowired
    private IQiNiuService qiNiuService;

    @Value("${qiniu.cdn.prefix}")
    private String cdnPrefix;

    @Override
    public ServiceResult<HouseDTO> save(HouseForm houseForm) {
        HouseDetail detail = new HouseDetail();
        ServiceResult<HouseDTO> subwayValidtionResult = wrapperDetailInfo(detail, houseForm);
        if (subwayValidtionResult != null) {
            return subwayValidtionResult;
        }

        House house = new House();
        modelMapper.map(houseForm, house);

        Date now = new Date();
        house.setCreateTime(now);
        house.setLastUpdateTime(now);
        house.setAdminId(LoginUserUtil.getLoginUserId());
        house = houseRepository.save(house);

        detail.setHouseId(house.getId());
        detail = houseDetailRepository.save(detail);

        List<HousePicture> pictures = generatePictures(houseForm, house.getId());
        Iterable<HousePicture> housePictures = housePictureRepository.save(pictures);

        HouseDTO houseDTO = modelMapper.map(house, HouseDTO.class);
        HouseDetailDTO houseDetailDTO = modelMapper.map(detail, HouseDetailDTO.class);

        houseDTO.setHouseDetail(houseDetailDTO);

        List<HousePictureDTO> pictureDTOS = new ArrayList<>();
        housePictures.forEach(housePicture -> pictureDTOS.add(modelMapper.map(housePicture, HousePictureDTO.class)));
        houseDTO.setPictures(pictureDTOS);
        houseDTO.setCover(this.cdnPrefix + houseDTO.getCover());

        List<String> tags = houseForm.getTags();
        if (tags != null && !tags.isEmpty()) {
            List<HouseTag> houseTags = new ArrayList<>();
            for (String tag : tags) {
                houseTags.add(new HouseTag(house.getId(), tag));
            }
            houseTagRepository.save(houseTags);
            houseDTO.setTags(tags);
        }
        return new ServiceResult<HouseDTO>(true, null, houseDTO);
    }


    /**
     * 图片对象列表信息填充
     *
     * @param form
     * @param houseId
     * @return
     */
    private List<HousePicture> generatePictures(HouseForm form, Long houseId) {
        List<HousePicture> pictures = new ArrayList<>();
        if (form.getPhotos() == null || form.getPhotos().isEmpty()) {
            return pictures;
        }

        for (PhotoForm photoForm : form.getPhotos()) {
            HousePicture picture = new HousePicture();
            picture.setHouseId(houseId);
            picture.setCdnPrefix(cdnPrefix);
            picture.setPath(photoForm.getPath());
            picture.setWidth(photoForm.getWidth());
            picture.setHeight(photoForm.getHeight());
            pictures.add(picture);
        }
        return pictures;
    }

    /**
     * 房源详细信息对象填充
     *
     * @param houseDetail
     * @param houseForm
     * @return
     */
    private ServiceResult<HouseDTO> wrapperDetailInfo(HouseDetail houseDetail, HouseForm houseForm) {
        Subway subway = subwayRepository.findOne(houseForm.getSubwayLineId());
        if (subway == null) {
            return new ServiceResult<>(false, "Not valid subway line!");
        }

        SubwayStation subwayStation = subwayStationRepository.findOne(houseForm.getSubwayStationId());
        if (subwayStation == null || subway.getId() != subwayStation.getSubwayId()) {
            return new ServiceResult<>(false, "Not valid subway station!");
        }

        houseDetail.setSubwayLineId(subway.getId());
        houseDetail.setSubwayLineName(subway.getName());

        houseDetail.setSubwayStationId(subwayStation.getId());
        houseDetail.setSubwayStationName(subwayStation.getName());

        houseDetail.setDescription(houseForm.getDescription());
        houseDetail.setDetailAddress(houseForm.getDetailAddress());
        houseDetail.setLayoutDesc(houseForm.getLayoutDesc());
        houseDetail.setRentWay(houseForm.getRentWay());
        houseDetail.setRoundService(houseForm.getRoundService());
        houseDetail.setTraffic(houseForm.getTraffic());
        return null;

    }

    @Override
    public ServiceMultiResult<HouseDTO> adminQuery(DatatableSearch searchBody) {
        List<HouseDTO> houseDTOS = new ArrayList<>();

        //1.排序 2. 字段名称
        Sort sort = new Sort(Sort.Direction.fromString(searchBody.getDirection()), searchBody.getOrderBy());
        // 从哪开始/ 每页长度
        //分页页数 (第几页)
        int page = searchBody.getStart() / searchBody.getLength();

        /**
         * searchBody.getStart()0
         * searchBody.getLength()3
         * page0
         *
         * searchBody.getStart()3
         * searchBody.getLength()3
         * page1
         */
        System.out.println("searchBody.getStart()" + searchBody.getStart());
        System.out.println("searchBody.getLength()" + searchBody.getLength());
        System.out.println("page" + page);

        //1.页数 2每页长度 3.排序方式
        Pageable pageable = new PageRequest(page, searchBody.getLength(), sort);

        //多条件查询
        Specification<House> specification = (root, query, cb) -> {
            //获取当前用户可以操作的房源    oot.get("adminId") -->对应house 里面的adminId
            Predicate predicate = cb.equal(root.get("adminId"), LoginUserUtil.getLoginUserId());
            //房源状态不是逻辑删除的
            predicate = cb.and(predicate, cb.notEqual(root.get("status"), HouseStatus.DELETED.getValue()));
            if (searchBody.getCity() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("cityEnName"), searchBody.getCity()));
            }

            if (searchBody.getStatus() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("status"), searchBody.getStatus()));
            }

            if (searchBody.getCreateTimeMin() != null) {
                predicate = cb.and(predicate, cb.greaterThanOrEqualTo(root.get("createTime"), searchBody.getCreateTimeMin()));
            }

            if (searchBody.getCreateTimeMax() != null) {
                predicate = cb.and(predicate, cb.lessThanOrEqualTo(root.get("createTime"), searchBody.getCreateTimeMax()));
            }

            if (searchBody.getTitle() != null) {
                predicate = cb.and(predicate, cb.like(root.get("title"), "%" + searchBody.getTitle() + "%"));
            }

            return predicate;
        };

        Page<House> houses = houseRepository.findAll(specification, pageable);

        houses.forEach(house -> {
            HouseDTO houseDTO = modelMapper.map(house, HouseDTO.class);
            houseDTO.setCover(this.cdnPrefix + house.getCover());
            houseDTOS.add(houseDTO);
        });

        return new ServiceMultiResult<>(houses.getTotalElements(), houseDTOS);
    }


    @Override
    public ServiceResult<HouseDTO> findCompleteOne(Long id) {

        House house = houseRepository.findOne(id);
        if (house == null) {
            return ServiceResult.notFound();
        }
        // houseDetail  houseTag  housePicture
        HouseDetail houseDetail = houseDetailRepository.findByHouseId(id);
        List<HousePicture> pictures = housePictureRepository.findAllByHouseId(id);
        List<HouseTag> tags = houseTagRepository.findAllByHouseId(id);  //option+回车

        //返回需要houseDTO  对应的detail picture  都需要是dto的  tag是List<String>
        HouseDetailDTO houseDetailDTO = modelMapper.map(houseDetail, HouseDetailDTO.class);
        List<HousePictureDTO> pictureDTOS = new ArrayList<>();
        for (HousePicture picture : pictures) {
            pictureDTOS.add(modelMapper.map(picture, HousePictureDTO.class));
        }
        List<String> tagList = new ArrayList<>();
        for (HouseTag tag : tags) {
            tagList.add(tag.getName());
        }

        HouseDTO result = modelMapper.map(house, HouseDTO.class);

        result.setHouseDetail(houseDetailDTO);
        result.setPictures(pictureDTOS);
        result.setTags(tagList);

        return ServiceResult.of(result);
    }


    @Override
    @Transactional
    public ServiceResult update(HouseForm houseForm) {
        House house = houseRepository.findOne(houseForm.getId());
        if (house == null) {
            return ServiceResult.notFound();
        }

        HouseDetail detail = houseDetailRepository.findByHouseId(house.getId());
        if (detail == null) {
            return ServiceResult.notFound();
        }

        ServiceResult wrapperResult = wrapperDetailInfo(detail, houseForm);

        if (wrapperResult != null) {
            return wrapperResult;
        }
        houseDetailRepository.save(detail);
        List<HousePicture> pictures = generatePictures(houseForm, houseForm.getId());

        housePictureRepository.save(pictures);

        //如果cover数据没有返回 让其使用原先的
        if (houseForm.getCover() == null) {
            houseForm.setCover(house.getCover());
        }
        //houseForm 存到house中
        modelMapper.map(houseForm, house);
        house.setLastUpdateTime(new Date());

        houseRepository.save(house);

        if (house.getStatus() == HouseStatus.PASSES.getValue()){
            searchService.index(house.getId());
        }

        return ServiceResult.success();
    }


    /**
     * 先删服务器端成功后  --> 删除本地端
     * @param id
     * @return
     */
    @Override
    public ServiceResult removePhoto(Long id) {
        HousePicture housePicture = housePictureRepository.findOne(id);
        if (housePicture == null){
            return ServiceResult.notFound();
        }

        try {
            //云删除
            Response response = this.qiNiuService.delete(housePicture.getPath());
            if (response.isOK()){
                housePictureRepository.delete(id);
                return ServiceResult.success();
            }else {
                return new ServiceResult(false,response.error);
            }
        } catch (QiniuException e) {
            e.printStackTrace();
            return new ServiceResult(false,e.getMessage());
        }
    }

    @Override
    @Transactional
    public ServiceResult updateCover(Long coverId, Long targetId) {
       HousePicture cover = housePictureRepository.findOne(coverId);
       if (cover == null){
           return ServiceResult.notFound();
       }
       houseRepository.updateCover(targetId,cover.getPath());
        return ServiceResult.success();
    }

    @Override
    @Transactional
    public ServiceResult addTag(Long houseId, String tag) {
        House house = houseRepository.findOne(houseId);
        if (house == null){
            return ServiceResult.notFound();
        }
        HouseTag houseTag = houseTagRepository.findByNameAndHouseId(tag,houseId);
        if (houseTag != null){
            return new ServiceResult(false,"标签已经存在");
        }
        houseTagRepository.save(new HouseTag(houseId,tag));
        return ServiceResult.success();
    }

    @Override
    @Transactional
    public ServiceResult removeTag(Long houseId, String tag) {
        House house = houseRepository.findOne(houseId);
        if (house == null){
            return ServiceResult.notFound();
        }
        HouseTag houseTag = houseTagRepository.findByNameAndHouseId(tag, houseId);
        if (houseTag == null){
            return new ServiceResult(false, "标签不存在");
        }
        houseTagRepository.delete(houseTag.getId());
        return ServiceResult.success();
    }

    @Override
    @Transactional
    public ServiceResult updateStatus(Long id, int status) {
        House house = this.houseRepository.findOne(id);
        if (house == null){
            return ServiceResult.notFound();
        }
        if (house.getStatus() == status){
            return new ServiceResult(false,"状态没有发生改变");
        }
        if (house.getStatus() == HouseStatus.RENTED.getValue()){
            return new ServiceResult(false,"已出租房屋不许修改状态");
        }
        if (house.getStatus() == HouseStatus.DELETED.getValue()){
            return new ServiceResult(false,"已删除资源不许修改操作");
        }
        houseRepository.updateStatus(id,status);
        //上架更新索引  其他情况都要删除索引

        if (status == HouseStatus.PASSES.getValue()){
            searchService.index(house.getId());
        }else {
            searchService.remove(house.getId());
        }
        return ServiceResult.success();
    }

    private List<HouseDTO> wrapperHouseResult(List<Long> houseIds){
        List<HouseDTO> res = new ArrayList<>();

        Map<Long, HouseDTO> idToHouseMap = new HashMap<>();
        Iterable<House> houses = houseRepository.findAll(houseIds);
        houses.forEach(house -> {
            HouseDTO houseDTO = modelMapper.map(house,HouseDTO.class);
            houseDTO.setCover(this.cdnPrefix+ house.getCover());
            idToHouseMap.put(house.getId(),houseDTO);
        });

        //渲染housetag houseDetail  映射到houseDto中
        wrapperHouseList(houseIds,idToHouseMap);

        //es查到的顺序和mysql查到的顺序不一定一致  矫正顺序 向es的顺序
        for (Long houseId : houseIds) {
            res.add(idToHouseMap.get(houseId));
        }
        return res;
    }

    @Override
    public ServiceMultiResult<HouseDTO> query(RentSearch rentSearch) {
        if (rentSearch.getKeywords() != null && !rentSearch.getKeywords().isEmpty()){
            ServiceMultiResult<Long> serviceResult = searchService.query(rentSearch);
            if (serviceResult.getTotal() == 0 ){
                return new ServiceMultiResult<>(0, new ArrayList<>());
            }
            //有结果的话用houseId来查 封装方法
            return new ServiceMultiResult<>(serviceResult.getTotal(),wrapperHouseResult(serviceResult.getResult()));
        }

        return simpleQuery(rentSearch);
    }

    //mysql 的query
    private ServiceMultiResult<HouseDTO> simpleQuery(RentSearch rentSearch){
        Sort sort = HouseSort.generateSort(rentSearch.getOrderBy(), rentSearch.getOrderDirection());
        int page = rentSearch.getStart() / rentSearch.getSize(); //第几页
        Pageable pageable = new PageRequest(page,rentSearch.getSize(),sort);
        //删除的数据不能查到
        Specification<House> specification= (root, query, cb) -> {
            //审核
            Predicate predicate = cb.equal(root.get("status"),HouseStatus.PASSES.getValue());
            //城市相同
            predicate = cb.and(predicate,cb.equal(root.get("cityEnName"),rentSearch.getCityEnName()));
            //地铁距离
            if (HouseSort.DISTANCE_TO_SUBWAY_KEY.equals(rentSearch.getOrderBy())) {
                predicate = cb.and(predicate, cb.gt(root.get(HouseSort.DISTANCE_TO_SUBWAY_KEY), -1));
            }
            return predicate;
        };

        Page<House> houses = houseRepository.findAll(specification,pageable);
        List<HouseDTO> houseDTOS = new ArrayList<>();
        List<Long> houseIds = new ArrayList<>();

        Map<Long, HouseDTO> idToHouseMap = Maps.newHashMap();
        houses.forEach(house -> {
            HouseDTO houseDTO = modelMapper.map(house,HouseDTO.class);
            houseDTO.setCover(this.cdnPrefix+house.getCover());
            houseDTOS.add(houseDTO);
            houseIds.add(house.getId());
            idToHouseMap.put(house.getId(),houseDTO);
        });

        wrapperHouseList(houseIds,idToHouseMap);
        return new ServiceMultiResult<>(houses.getTotalElements(),houseDTOS);
    }
    /**
     * 渲染详细信息 及 标签
     * @param houseIds
     * @param idToHouseMap
     */
    private void wrapperHouseList(List<Long> houseIds, Map<Long, HouseDTO> idToHouseMap) {
        List<HouseDetail> details = houseDetailRepository.findAllByHouseIdIn(houseIds);
        details.forEach(houseDetail -> {
            HouseDTO houseDTO = idToHouseMap.get(houseDetail.getHouseId());
            HouseDetailDTO detailDTO = modelMapper.map(houseDetail, HouseDetailDTO.class);
            houseDTO.setHouseDetail(detailDTO);
        });

        List<HouseTag> houseTags = houseTagRepository.findAllByHouseIdIn(houseIds);
        houseTags.forEach(houseTag -> {
            HouseDTO house = idToHouseMap.get(houseTag.getHouseId());
            house.getTags().add(houseTag.getName());
        });
    }
}
