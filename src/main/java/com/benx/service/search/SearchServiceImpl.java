package com.benx.service.search;

import com.benx.base.HouseSort;
import com.benx.base.RentValueBlock;
import com.benx.entity.House;
import com.benx.entity.HouseDetail;
import com.benx.entity.HouseTag;
import com.benx.repository.HouseDetailRepository;
import com.benx.repository.HouseRepository;

import com.benx.repository.HouseTagRepository;
import com.benx.service.user.ServiceMultiResult;
import com.benx.web.form.RentSearch;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.primitives.Longs;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.DeleteByQueryAction;
import org.elasticsearch.index.reindex.DeleteByQueryRequestBuilder;
import org.elasticsearch.rest.RestStatus;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class SearchServiceImpl implements ISearchService {

    private static final Logger logger = LoggerFactory.getLogger(SearchServiceImpl.class);
    private static final String INDEX_NAME = "xunwu";   //事先定义的索引名称

    private static final String INDEX_TYPE = "house";   //索引类型(mappings下一级)

    private static final String INDEX_TOPIC = "house_build";

    @Autowired
    private HouseRepository houseRepository;

    @Autowired
    private HouseDetailRepository houseDetailRepository;

    @Autowired
    private HouseTagRepository houseTagRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private TransportClient esClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    /**
     * 通过houseId查到我们的目标数据然后去es构建
     *
     * @param houseId
     */
    @Override
    public void index(Long houseId) {
        this.index(houseId, 0);
    }

    private void index(Long houseId, int retry) {
        //超过重试最大次数
        if (retry > HouseIndexMessage.MAX_RETRY) {
            logger.error("Retry index times over 3 for house: " + houseId + "Please check it");
            return;
        }
        //构建消息体,用kafka发布到topic中
        HouseIndexMessage message = new HouseIndexMessage(houseId, HouseIndexMessage.INDEX, retry);
        try {
            kafkaTemplate.send(INDEX_TOPIC, objectMapper.writeValueAsString(message));
        } catch (JsonProcessingException e) {
            logger.error("Json encode error for " + message);
        }
    }


    private boolean create(HouseIndexTemplate houseIndexTemplate) {
        try {
            IndexResponse response = this.esClient.prepareIndex(INDEX_NAME, INDEX_TYPE)
                    .setSource(objectMapper.writeValueAsBytes(houseIndexTemplate), XContentType.JSON).get(); //需要构建的数据json
            logger.debug("create index with house:" + houseIndexTemplate.getHouseId());
            if (response.status() == RestStatus.CREATED) {
                return true;
            } else {
                return false;
            }
        } catch (JsonProcessingException e) {
            logger.error("Error to index house " + houseIndexTemplate.getHouseId(), e);
            return false;
        }
    }

    private boolean update(String esId, HouseIndexTemplate houseIndexTemplate) {
        try {
            UpdateResponse response = this.esClient.prepareUpdate(INDEX_NAME, INDEX_TYPE, esId)
                    .setDoc(objectMapper.writeValueAsBytes(houseIndexTemplate), XContentType.JSON).get(); //需要构建的数据json
            logger.debug("update index with house:" + houseIndexTemplate.getHouseId());
            if (response.status() == RestStatus.OK) {
                return true;
            } else {
                return false;
            }
        } catch (JsonProcessingException e) {
            logger.error("Error to update house " + houseIndexTemplate.getHouseId(), e);
            return false;
        }
    }

    private boolean deleteAndCreate(long totalHit, HouseIndexTemplate houseIndexTemplate) {
        DeleteByQueryRequestBuilder builder =
                DeleteByQueryAction.INSTANCE
                        .newRequestBuilder(esClient)
                        //删的条件  query出来的东西都要删掉
                        .filter(QueryBuilders.termQuery(HouseIndexKey.HOUSE_ID, houseIndexTemplate.getHouseId()))
                        .source(INDEX_NAME);
        //索引名

        logger.debug("Delete by query for house " + builder);
        BulkByScrollResponse response = builder.get();
        long deleted = response.getDeleted();
        //查到的和删除的数量不一致的情况下
        if (deleted != totalHit) {
            logger.warn("Need delete {}, but {} was deleted!", totalHit, deleted);
            return false;
        } else {
            return create(houseIndexTemplate);
        }
    }

    /**
     * 房屋被租出去了就不能被用户查到了
     *
     * @param houseId
     */
    @Override
    public void remove(Long houseId) {
        this.remove(houseId, 0);
    }

    private void remove(Long houseId, int retry) {
        if (retry > HouseIndexMessage.MAX_RETRY) {
            logger.error("Retry remove times over 3 for house: " + houseId + "please check it");
            return;
        }

        HouseIndexMessage message = new HouseIndexMessage(houseId, HouseIndexMessage.REMOVE, retry);
        try {
            this.kafkaTemplate.send(INDEX_TOPIC, objectMapper.writeValueAsString(message));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            logger.error("Cannot encode json for :" + message, e);
        }
    }


    /**
     * kafka监听器 监听topic
     * 实现一个消息 能代表index 又能代表remove  --->自定义消息结构体
     *
     * @param content
     */
    @KafkaListener(topics = INDEX_TOPIC)
    private void handleMessage(String content) {
        try {
            //把string 转换成 消息结构体类型
            HouseIndexMessage message = objectMapper.readValue(content, HouseIndexMessage.class);

            switch (message.getOperation()) {
                case HouseIndexMessage.INDEX:
                    this.createOrUpdate(message);
                    break;
                case HouseIndexMessage.REMOVE:
                    this.removeIndex(message);
                    break;
                default:
                    logger.warn("Not support message " + content);
                    break;

            }
        } catch (IOException e) {
            e.printStackTrace();
            logger.error("Cannot parse json for " + content);
        }
    }

    private void removeIndex(HouseIndexMessage message) {
        Long houseId = message.getHouseId();
        DeleteByQueryRequestBuilder builder =
                DeleteByQueryAction.INSTANCE
                        .newRequestBuilder(esClient)
                        //删的条件  query出来的东西都要删掉
                        .filter(QueryBuilders.termQuery(HouseIndexKey.HOUSE_ID, houseId))
                        .source(INDEX_NAME);
        //索引名

        logger.debug("Delete by query for house " + builder);
        BulkByScrollResponse response = builder.get();
        long deleted = response.getDeleted();
        //打印remove的条数
        logger.debug("Delete total" + deleted);

        if (deleted <= 0) {
            this.remove(houseId, message.getRetry() + 1);
        }
    }

    //create 形参是houseTemplate ,我们这里需要传message
    private void createOrUpdate(HouseIndexMessage message) {
        Long houseId = message.getHouseId();

        House house = houseRepository.findOne(houseId);
        if (house == null) {
            logger.error("index house {} dose not exist!", houseId);
            //构建索引未成功,重新构建
            this.index(houseId, message.getRetry() + 1);
            return;
        }
        //初始化索引bean
        HouseIndexTemplate indexTemplate = new HouseIndexTemplate();
        modelMapper.map(house, indexTemplate);

        //地铁等其他信息在别的bean中  -->houseDetail   houseTag
        HouseDetail houseDetail = houseDetailRepository.findByHouseId(houseId);
        if (houseDetail == null) {
            //TODO 异常情况
        }
        modelMapper.map(houseDetail, indexTemplate);

        //es中都是字符串 需要遍历  并将其放入indexTemplate中
        List<HouseTag> tags = houseTagRepository.findAllByHouseId(houseId);
        if (tags != null && !tags.isEmpty()) {
            List<String> tagStrings = new ArrayList<>();
            tags.forEach(houseTag -> tagStrings.add(houseTag.getName()));
            indexTemplate.setTags(tagStrings);
        }

        //查看数据是否在es中存在
        SearchRequestBuilder requestBuilder = this.esClient.prepareSearch(INDEX_NAME).setTypes(INDEX_TYPE)
                .setQuery(QueryBuilders.termQuery(HouseIndexKey.HOUSE_ID, houseId));
        logger.debug(requestBuilder.toString());
        SearchResponse searchResponse = requestBuilder.get();

        boolean success;
        long totalHit = searchResponse.getHits().getTotalHits();
        if (totalHit == 0) {
            //没有数据 需要创建
            success = create(indexTemplate);
        } else if (totalHit == 1) {
            //只有1个说明是需要更新的  原来有数据直接覆盖
            String esId = searchResponse.getHits().getAt(0).getId();
            success = update(esId, indexTemplate);
        } else {
            //不止一个 同样的数据存了很多个
            success = deleteAndCreate(totalHit, indexTemplate);
        }
        if (success) {
            logger.debug("Index success with house " + houseId);
        }

    }

    @Override
    public ServiceMultiResult<Long> query(RentSearch rentSearch) {

        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        //1. 城市名
        boolQuery.filter(
                QueryBuilders.termQuery(HouseIndexKey.CITY_EN_NAME, rentSearch.getCityEnName())
        );
        //2.区域名
        if (rentSearch.getRegionEnName() != null && !"*".equals(rentSearch.getRegionEnName())) {
            //不等于所有区域(*), region不为空
            boolQuery.filter(
                    QueryBuilders.termQuery(HouseIndexKey.REGION_EN_NAME, rentSearch.getRegionEnName()))
            ;
        }



        //3.区间类型数据查询  面积
        RentValueBlock area = RentValueBlock.matchArea(rentSearch.getAreaBlock());
        //范围不相等的话
        if (!RentValueBlock.ALL.equals(area)) {
            RangeQueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery(HouseIndexKey.AREA);
            if (area.getMax() > 0) {
                //小于最大值
                rangeQueryBuilder.lte(area.getMax());
            }
            if (area.getMin() > 0) {
                //大于最小值
                rangeQueryBuilder.gte(area.getMin());
            }
            boolQuery.filter(rangeQueryBuilder);
        }
        //4.价格
        RentValueBlock price = RentValueBlock.matchPrice(rentSearch.getPriceBlock());
        if (!RentValueBlock.ALL.equals(price)) {
            RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery(HouseIndexKey.PRICE);
            if (price.getMax() > 0) {
                rangeQuery.lte(price.getMax());
            }
            if (price.getMin() > 0) {
                rangeQuery.gte(price.getMin());
            }
            boolQuery.filter(rangeQuery);
        }
        //5.朝向  朝向存在的话
        if (rentSearch.getDirection() > 0) {
            boolQuery.filter(
                    QueryBuilders.termQuery(HouseIndexKey.DIRECTION, rentSearch.getDirection())
            );
        }
        //6.整租 or 合租
        if (rentSearch.getRentWay() > -1) {
            boolQuery.filter(
                    QueryBuilders.termQuery(HouseIndexKey.RENT_WAY, rentSearch.getRentWay())
            );
        }

        //7.关键词
        boolQuery.must(
                QueryBuilders.multiMatchQuery(rentSearch.getKeywords(),
                        HouseIndexKey.TITLE,
                        HouseIndexKey.TRAFFIC,
                        HouseIndexKey.DIRECTION,
                        HouseIndexKey.ROUND_SERVICE,
                        HouseIndexKey.SUBWAY_LINE_NAME,
                        HouseIndexKey.SUBWAY_STATION_NAME

                ));

        //通过查询条件查询
        SearchRequestBuilder requestBuilder = this.esClient.prepareSearch(INDEX_NAME)
                .setTypes(INDEX_TYPE)
                .setQuery(boolQuery)
                .addSort(
                        HouseSort.getSortKey(rentSearch.getOrderBy()),
                        SortOrder.fromString(rentSearch.getOrderDirection())
                )
                .setFrom(rentSearch.getStart())
                .setSize(rentSearch.getSize())
                .setFetchSource(HouseIndexKey.HOUSE_ID, null);

        logger.debug(requestBuilder.toString());

        //在es中只查id
        List<Long> houseIds = new ArrayList<>();
        SearchResponse response = requestBuilder.get();
        if (response.status() != RestStatus.OK){
            logger.error("Search status is not ok for"+ requestBuilder);
            return new ServiceMultiResult<>(0,houseIds);
        }
        for (SearchHit hit : response.getHits()) {
            //object -->string -->long
            houseIds.add(Longs.tryParse(String.valueOf(hit.getSource().get(HouseIndexKey.HOUSE_ID))));
        }

        return new ServiceMultiResult<>(response.getHits().totalHits,houseIds);
    }
}
