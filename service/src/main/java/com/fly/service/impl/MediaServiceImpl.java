package com.fly.service.impl;

import com.alibaba.fastjson.JSON;
import com.fly.common.base.pojo.PageInfo;
import com.fly.common.base.pojo.ResultBean;
import com.fly.common.exception.QueryException;
import com.fly.common.query.entity.Column;
import com.fly.common.query.entity.Query;
import com.fly.common.query.entity.QueryCondition;
import com.fly.common.query.util.QueryUtil;
import com.fly.common.utils.StrUtil;
import com.fly.dao.MediaRepository;
import com.fly.entity.*;
import com.fly.service.MediaService;
import com.fly.service.SeriesService;
import com.fly.service.StarService;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Predicate;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.File;
import java.math.BigDecimal;
import java.util.*;

/**
 * @Author:xukangfeng
 * @Description
 * @Date : Create in 10:28 2019/8/22
 */
@Service
public class MediaServiceImpl implements MediaService {

    @Autowired
    MediaRepository mediaRepository;

    @Autowired
    StarService starService;

    @Autowired
    SeriesService seriesService;

    @PersistenceContext
    EntityManager entityManager;

    @Override
    public Map<String, Object> findAll(String reqObj,Boolean isAll) throws Exception {
        //用于接收返回数据(配置、分页、数据)
        Map<String, Object> map = new HashMap<>();
        QueryCondition queryCondition = JSON.parseObject(reqObj, QueryCondition.class);

        // 分页信息
        PageInfo pageInfo = QueryUtil.getPageInfo(queryCondition);
        //获取Query配置
        Query query = QueryUtil.getQuery(queryCondition);


        int pageNum = pageInfo.getPageNum();
        int pageSize = pageInfo.getPageSize();

        //排序信息
        String sortInfo = !StrUtil.isEmpty(queryCondition.getSortInfo()) ? queryCondition.getSortInfo() : query.getOrder();
        Sort sort = null;
        if (!StrUtil.isEmpty(sortInfo)) {
            //判断排序类型及排序字段
            String[] sortArray = sortInfo.split(" ");
            sort = "asc".equals(sortArray[1]) ? new Sort(Sort.Direction.ASC, sortArray[0]) : new Sort(Sort.Direction.DESC, sortArray[0]);
        }


        //4)dsl动态查询
        List<Map<String, Object>> conditions = queryCondition.getConditions();
        String nameChn = "";
        Short year = 9999;
        String  starId = "-1";
        if (!conditions.isEmpty()){
            for(int i = 0 ; i < conditions.size() ; i++) {
                System.out.println(conditions.get(i).get("key"));
                System.out.println(conditions.get(i).get("value"));
                if ("StarId".equals(conditions.get(i).get("key"))) {
                    starId =  (String) conditions.get(i).get("value");
                }
                if ("nameChn".equals(conditions.get(i).get("key"))) {
                    nameChn =  (String) conditions.get(i).get("value");
                    nameChn  = nameChn.trim();
                }
                if ("year".equals(conditions.get(i).get("key"))) {
                    if (!"".equals(conditions.get(i).get("value").toString())){
                        year = Short.parseShort ( conditions.get(i).get("value").toString() );
                    }
                }
            }
        }
        String starAsDirect="",starAsActor="",starAsWriter="";
        if (!"-1".equals(starId)){
            Star star = starService.findOne(Long.valueOf(starId));
            starAsDirect = star.getAsDirector();
            starAsActor= star.getAsActor();
            starAsWriter = star.getAsWriter();

            starAsDirect = starAsDirect == null ? "" : starAsDirect;
            starAsActor = starAsActor == null ? "" : starAsActor;
            starAsWriter = starAsWriter == null ? "" : starAsWriter;
        }

//        System.out.println(starAsDirect);
//        System.out.println(starAsActor);
//        System.out.println(starAsWriter);
//        System.out.println("-1".equals(starId));

        List<Long> b = new ArrayList<Long>();
        if (!"".equals(starAsDirect)){
            for (String retval: starAsDirect.split(",")){
                Long a = Long.valueOf(retval);
                b.add(a);
            }
        }
        if (!"".equals(starAsActor)){
            for (String retval: starAsActor.split(",")){
                Long a = Long.valueOf(retval);
                b.add(a);
            }
        }
        if (!"".equals(starAsWriter)){
            for (String retval: starAsWriter.split(",")){
                Long a = Long.valueOf(retval);
                b.add(a);
            }
        }
        Long[] c = new Long[b.size()];
        b.toArray(c);

        QMedia media = QMedia.media;
        //初始化组装条件(类似where 1=1)

        Predicate predicate = media.deleted.ne(1);
        if (false == isAll){
            predicate = ExpressionUtils.and(predicate,media.film.isNotNull());
        }
        //执行动态条件拼装
        predicate = "".equals(nameChn) ? predicate : ExpressionUtils.and(predicate,media.nameChn.like(nameChn));
        predicate = year == 9999 ? predicate : ExpressionUtils.and(predicate,media.year.eq(year));
        predicate = "-1".equals(starId) ? predicate : ExpressionUtils.and(predicate,media.id.in(c));

        Pageable pageable = new PageRequest(pageNum-1, pageSize, sort);
        Page<Media> pageCarrier = mediaRepository.findAll(predicate , pageable);
        List<Column> columnCarrier = query.getColumnList();

        map.put("pageCarrier", pageCarrier);
        map.put("columnCarrier", columnCarrier);

        return map;
    }


    /**
     * 去重
     * @param reqObj
     * @return
     */
    @Override
    public Map<String, Object> findDuplicate(String reqObj) throws Exception {

        //用于接收返回数据(配置、分页、数据)
        Map<String, Object> map = new HashMap<>();
        QueryCondition queryCondition = JSON.parseObject(reqObj, QueryCondition.class);

        // 分页信息
        PageInfo pageInfo = QueryUtil.getPageInfo(queryCondition);
        //获取Query配置
        Query query = QueryUtil.getQuery(queryCondition);


        int pageNum = pageInfo.getPageNum();
        int pageSize = pageInfo.getPageSize();

        //排序信息
        String sortInfo = "nameChn asc";
        Sort sort = null;
        if (!StrUtil.isEmpty(sortInfo)) {
            //判断排序类型及排序字段
            String[] sortArray = sortInfo.split(" ");
            sort = "asc".equals(sortArray[1]) ? new Sort(Sort.Direction.ASC, sortArray[0]) : new Sort(Sort.Direction.DESC, sortArray[0]);
        }

        Pageable pageable = new PageRequest(pageNum-1, pageSize, sort);

        QMedia media = QMedia.media;
        //查询语句动态准备
        List<Predicate> criteria = new ArrayList<>();
        criteria.add(media.deleted.ne(1));



        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
        List<String> listRepeat = queryFactory.selectFrom(media)
                .groupBy(media.nameChn,media.year)
                .select(media.nameChn)
                .where(criteria.toArray(new Predicate[criteria.size()]))
                .having(media.nameChn.count().gt(1))
                .fetch();
        //再次搜索：带分页
        Predicate predicate = media.nameChn.in(listRepeat).and(media.deleted.ne(1));

        Page<Media> pageCarrier = mediaRepository.findAll(predicate , pageable);
        List<Column> columnCarrier = query.getColumnList();

        map.put("pageCarrier", pageCarrier);
        map.put("columnCarrier", columnCarrier);

        return map;
    }



    @Override
    public Map<String, Object> findUnlink(String reqObj) throws Exception {
        //用于接收返回数据(配置、分页、数据)
        Map<String, Object> map = new HashMap<>();
        QueryCondition queryCondition = JSON.parseObject(reqObj, QueryCondition.class);

        // 分页信息
        PageInfo pageInfo = QueryUtil.getPageInfo(queryCondition);
        //获取Query配置
        Query query = QueryUtil.getQuery(queryCondition);


        int pageNum = pageInfo.getPageNum();
        int pageSize = pageInfo.getPageSize();

        //排序信息
        String sortInfo = !StrUtil.isEmpty(queryCondition.getSortInfo()) ? queryCondition.getSortInfo() : query.getOrder();
        //String sortInfo = "gatherDate desc";
        Sort sort = null;
        if (!StrUtil.isEmpty(sortInfo)) {
            //判断排序类型及排序字段
            String[] sortArray = sortInfo.split(" ");
            //System.out.println(sortArray);
            sort = "asc".equals(sortArray[1]) ? new Sort(Sort.Direction.ASC, sortArray[0]) : new Sort(Sort.Direction.DESC, sortArray[0]);
        }


        Pageable pageable = new PageRequest(pageNum-1, pageSize, sort);

        QMedia media = QMedia.media;
        //再次搜索：带分页
        Predicate predicate = media.film.isNull().and(media.deleted.ne(1));

        Page<Media> pageCarrier = mediaRepository.findAll(predicate , pageable);
        List<Column> columnCarrier = query.getColumnList();

        map.put("pageCarrier", pageCarrier);
        map.put("columnCarrier", columnCarrier);

        return map;
    }


    @Override
    public Map<String, Object> findAllOfSeriesUnselect(String reqObj) throws Exception {
        //用于接收返回数据(配置、分页、数据)
        Map<String, Object> map = new HashMap<>();
        QueryCondition queryCondition = JSON.parseObject(reqObj, QueryCondition.class);

        // 分页信息
        PageInfo pageInfo = QueryUtil.getPageInfo(queryCondition);
        //获取Query配置
        Query query = QueryUtil.getQuery(queryCondition);

        int pageNum = pageInfo.getPageNum();
        int pageSize = pageInfo.getPageSize();

        //排序信息
        String sortInfo = !StrUtil.isEmpty(queryCondition.getSortInfo()) ? queryCondition.getSortInfo() : query.getOrder();
        //String sortInfo = "gatherDate desc";
        Sort sort = null;
        if (!StrUtil.isEmpty(sortInfo)) {
            //判断排序类型及排序字段
            String[] sortArray = sortInfo.split(" ");
            //System.out.println(sortArray);
            sort = "asc".equals(sortArray[1]) ? new Sort(Sort.Direction.ASC, sortArray[0]) : new Sort(Sort.Direction.DESC, sortArray[0]);
        }

        Pageable pageable = new PageRequest(pageNum-1, pageSize, sort);
        //4)dsl动态查询
        List<Map<String, Object>> conditions = queryCondition.getConditions();
        String name = null ,seriesId = "0";
        if (!"".equals(conditions.get(0).get("value"))){
            seriesId = (String) conditions.get(0).get("value");
        }
        if (!"".equals(conditions.get(1).get("value"))){
            name =  (String) conditions.get(1).get("value");
        }

        Series series = seriesService.findOne(Long.valueOf(seriesId));

        QMedia media = QMedia.media;
        Predicate predicate = media.deleted.ne(1);
        String[] mediasArray = new String[]{};

        if (series == null){
            predicate = media.id.stringValue().eq("-1");
        }else{
            String medias = series.getAsMedias();
            System.out.println(medias);
            if (medias == null){
                predicate = media.deleted.ne(1);
            }else{
                mediasArray = medias.split(",");
                //再次搜索：带分页
                predicate = media.id.stringValue().notIn(Arrays.asList(mediasArray)).and(media.deleted.ne(1));
            }
        }
        predicate = name == null ? predicate : ExpressionUtils.and(predicate,media.nameChn.like(name).or(media.nameEng.like(name)));

        Page<Media> pageCarrier = mediaRepository.findAll(predicate , pageable);
        List<Column> columnCarrier = query.getColumnList();

        map.put("pageCarrier", pageCarrier);
        map.put("columnCarrier", columnCarrier);

        return map;
    }

    /**
     * 查找系列中的所有Media
     * @param reqObj
     * @return
     * @throws Exception
     */
    @Override
    public Map<String, Object> findAllOfSeries(String reqObj) throws Exception {
        //用于接收返回数据(配置、分页、数据)
        Map<String, Object> map = new HashMap<>();
        QueryCondition queryCondition = JSON.parseObject(reqObj, QueryCondition.class);

        // 分页信息
        PageInfo pageInfo = QueryUtil.getPageInfo(queryCondition);
        //获取Query配置
        Query query = QueryUtil.getQuery(queryCondition);


        int pageNum = pageInfo.getPageNum();
        int pageSize = pageInfo.getPageSize();

        //排序信息
        String sortInfo = !StrUtil.isEmpty(queryCondition.getSortInfo()) ? queryCondition.getSortInfo() : query.getOrder();
        //String sortInfo = "gatherDate desc";
        Sort sort = null;
        if (!StrUtil.isEmpty(sortInfo)) {
            //判断排序类型及排序字段
            String[] sortArray = sortInfo.split(" ");
            //System.out.println(sortArray);
            sort = "asc".equals(sortArray[1]) ? new Sort(Sort.Direction.ASC, sortArray[0]) : new Sort(Sort.Direction.DESC, sortArray[0]);
        }

        Pageable pageable = new PageRequest(pageNum-1, pageSize, sort);

        //4)dsl动态查询
        List<Map<String, Object>> conditions = queryCondition.getConditions();
        String name = null ,seriesId = "0";
        if (!"".equals(conditions.get(0).get("value"))){
            seriesId = (String) conditions.get(0).get("value");
        }
        if (!"".equals(conditions.get(1).get("value"))){
            name =  (String) conditions.get(1).get("value");
        }

        Series series = seriesService.findOne(Long.valueOf(seriesId));

        QMedia media = QMedia.media;
        Predicate predicate = media.deleted.ne(1);
        String[] mediasArray = new String[]{};

        if (series == null){
            predicate = media.id.stringValue().eq("-1");
        }else{

            String medias = series.getAsMedias();
            System.out.println(medias);
            if (medias == null){
                predicate = media.id.stringValue().eq("-1");
            }else{
                mediasArray = medias.split(",");
                //再次搜索：带分页
                predicate = media.id.stringValue().in(Arrays.asList(mediasArray));
            }

        }
        predicate = name == null ? predicate : ExpressionUtils.and(predicate,media.nameChn.like(name).or(media.nameEng.like(name)));


        Page<Media> pageCarrier = mediaRepository.findAll(predicate , pageable);
        List<Column> columnCarrier = query.getColumnList();

        map.put("pageCarrier", pageCarrier);
        map.put("columnCarrier", columnCarrier);

        return map;
    }

    @Override
    public Map<String, Object> findAllOfStar(String reqObj) throws Exception {
        //用于接收返回数据(配置、分页、数据)
        Map<String, Object> map = new HashMap<>();
        QueryCondition queryCondition = JSON.parseObject(reqObj, QueryCondition.class);

        // 分页信息
        PageInfo pageInfo = QueryUtil.getPageInfo(queryCondition);
        //获取Query配置
        Query query = QueryUtil.getQuery(queryCondition);


        int pageNum = pageInfo.getPageNum();
        int pageSize = pageInfo.getPageSize();


        //排序信息
        String sortInfo = !StrUtil.isEmpty(queryCondition.getSortInfo()) ? queryCondition.getSortInfo() : query.getOrder();
        //String sortInfo = "gatherDate desc";
        Sort sort = null;
        if (!StrUtil.isEmpty(sortInfo)) {
            //判断排序类型及排序字段
            String[] sortArray = sortInfo.split(" ");
            //System.out.println(sortArray);
            sort = "asc".equals(sortArray[1]) ? new Sort(Sort.Direction.ASC, sortArray[0]) : new Sort(Sort.Direction.DESC, sortArray[0]);
        }

        Pageable pageable = new PageRequest(pageNum-1, pageSize, sort);

        //4)dsl动态查询
        List<Map<String, Object>> conditions = queryCondition.getConditions();
        String name = null ,starId = "0", propName = null;
        if (!"".equals(conditions.get(0).get("value"))){
            starId = (String) conditions.get(0).get("value");
        }
        if (!"".equals(conditions.get(1).get("value"))){
            name =  (String) conditions.get(1).get("value");
        }
        if (!"".equals(conditions.get(2).get("value"))){
            propName =  (String) conditions.get(2).get("value");
        }

        Class starClass = Star.class;
        Star star = starService.findOne(Long.valueOf(starId));

        QMedia media = QMedia.media;
        Predicate predicate = media.isNotNull().or(media.isNull());
        String[] mediasArray = new String[]{};
        if (star == null){
        }else{

            String medias = (String) starClass.getDeclaredMethod(propName).invoke(star);
            System.out.println(medias);

            if (medias == null){
                predicate = media.id.stringValue().eq("-1");
            }else{
                mediasArray = medias.split(",");
                //再次搜索：带分页
                predicate = media.id.stringValue().in(Arrays.asList(mediasArray));
            }

        }
        predicate = name == null ? predicate : ExpressionUtils.and(predicate,media.nameChn.like(name).or(media.nameEng.like(name)));






        Page<Media> pageCarrier = mediaRepository.findAll(predicate , pageable);
        List<Column> columnCarrier = query.getColumnList();

        map.put("pageCarrier", pageCarrier);
        map.put("columnCarrier", columnCarrier);

        return map;
    }



    @Override
    public Map<String, Object> findByDeleted(String reqObj,Integer deleted) throws Exception {

        //用于接收返回数据(配置、分页、数据)
        Map<String, Object> map = new HashMap<>();
        QueryCondition queryCondition = JSON.parseObject(reqObj, QueryCondition.class);

        // 分页信息
        PageInfo pageInfo = QueryUtil.getPageInfo(queryCondition);
        //获取Query配置
        Query query = QueryUtil.getQuery(queryCondition);


        int pageNum = pageInfo.getPageNum();
        int pageSize = pageInfo.getPageSize();

        //排序信息
        String sortInfo = !StrUtil.isEmpty(queryCondition.getSortInfo()) ? queryCondition.getSortInfo() : query.getOrder();
        //String sortInfo = "gatherDate desc";
        Sort sort = null;
        if (!StrUtil.isEmpty(sortInfo)) {
            //判断排序类型及排序字段
            String[] sortArray = sortInfo.split(" ");
            //System.out.println(sortArray);
            sort = "asc".equals(sortArray[1]) ? new Sort(Sort.Direction.ASC, sortArray[0]) : new Sort(Sort.Direction.DESC, sortArray[0]);
        }


        Pageable pageable = new PageRequest(pageNum-1, pageSize, sort);

        QMedia media = QMedia.media;
        //再次搜索：带分页
        Predicate predicate = media.deleted.eq(deleted);

        Page<Media> pageCarrier = mediaRepository.findAll(predicate , pageable);
        List<Column> columnCarrier = query.getColumnList();

        map.put("pageCarrier", pageCarrier);
        map.put("columnCarrier", columnCarrier);

        return map;
    }

    @Override
    public Media findOne(Long id) {
        return mediaRepository.findOne(id);
    }


    @Override
    public void save(Media media){
        mediaRepository.save(media);
    }


    @Override
    public void delete(Media media){
        mediaRepository.delete(media);
    }

    /**
     * 删除media到回收站
     * @param id
     */
    @Override
    @Transactional
    public Boolean damage(Long id) {
        Media media = mediaRepository.findOne(id);
        media.setDeleted(1);
        media.setDeleteDate(new Date());
        media.setDeleteMemo("洗版或者重复下载");

        //去除Star表中As字段中当前MediaId
        starService.damageMedia(media);

        media.setWriter(null);
        media.setActor(null);
        media.setDirector(null);
        media.setFilm(null);
        mediaRepository.save(media);

        return true;

    }

    /**
     * 彻底销毁数据
     * @param id
     * @return
     */
    @Override
    @Transactional
    public Boolean burned(Long id) {
        Media media = mediaRepository.findOne(id);
        String fullPath = media.getFullPath();
        String diskNo = media.getDiskNo();
        File file = new File(diskNo+":"+fullPath);  //"D:\\xxx\\sss"
        if (file.exists()) { //用来测试此路径名表示的文件或目录是否存在
            return false;
        }
        return true;
    }

    @Override
    public Map<String, Object> findAllOfQuality(String reqObj) throws QueryException {
        //用于接收返回数据(配置、分页、数据)
        Map<String, Object> map = new HashMap<>();
        QueryCondition queryCondition = JSON.parseObject(reqObj, QueryCondition.class);

        // 分页信息
        PageInfo pageInfo = QueryUtil.getPageInfo(queryCondition);
        int pageNum = pageInfo.getPageNum();
        int pageSize = pageInfo.getPageSize();
        //获取Query配置
        Query query = QueryUtil.getQuery(queryCondition);


        //4)dsl动态查询
        List<Map<String, Object>> conditions = queryCondition.getConditions();
        int sizeCritical = 15;
        float filmDoubanRatingCritical  = 7.0f;
        String qualityType = "high";

        if (!conditions.isEmpty()){
            for(int i = 0 ; i < conditions.size() ; i++) {
                System.out.println(conditions.get(i).get("key")+" : "+conditions.get(i).get("value"));
                if ("qualityType".equals(conditions.get(i).get("key"))) {
                    qualityType =  (String) conditions.get(i).get("value");
                }
            }
            if ("low".equals(qualityType)){
                 sizeCritical = 10;
                 filmDoubanRatingCritical  = 8.0f;
            }
            for(int i = 0 ; i < conditions.size() ; i++) {
                if ("sizeCritical".equals(conditions.get(i).get("key"))) {
                    if(!"".equals(conditions.get(i).get("value"))){
                        sizeCritical =  Integer.parseInt((String) conditions.get(i).get("value"));
                    }
                }
                if ("ratingCritical".equals(conditions.get(i).get("key"))) {
                    if(!"".equals(conditions.get(i).get("value"))){
                        filmDoubanRatingCritical =  Float.parseFloat((String) conditions.get(i).get("value"));
                    }
                }
            }

        }

        //排序信息
        String sortInfo = !StrUtil.isEmpty(queryCondition.getSortInfo()) ? queryCondition.getSortInfo() : query.getOrder();
        //String sortInfo = "gatherDate desc";
        Sort sort = null;
        if (!StrUtil.isEmpty(sortInfo)) {
            //判断排序类型及排序字段
            String[] sortArray = sortInfo.split(" ");
            //System.out.println(sortArray);
            sort = "asc".equals(sortArray[1]) ? new Sort(Sort.Direction.ASC, sortArray[0]) : new Sort(Sort.Direction.DESC, sortArray[0]);
        }

        Pageable pageable = new PageRequest(pageNum-1, pageSize, sort);

        QMedia media = QMedia.media;
        //再次搜索：带分页
        Predicate predicate = media.deleted.eq(0).and(media.film.isNotNull());


        //Long mediaSizeCritical = 21474836480l;
        long mediaSizeCritical = (long) sizeCritical * 1024 * 1024 * 1024;
        if ("low".equals(qualityType)){
            System.out.println("查询条件：容量小于" + sizeCritical + "GB；豆分大于" + filmDoubanRatingCritical+"。");
            predicate  = ExpressionUtils.and(predicate,media.mediaSize.lt(mediaSizeCritical).and(media.film.doubanRating.gt(filmDoubanRatingCritical)));

        }else{
            System.out.println("查询条件：容量大于" + sizeCritical + "GB；豆分小于" + filmDoubanRatingCritical+"。");
            predicate  = ExpressionUtils.and(predicate,media.mediaSize.gt(mediaSizeCritical).and(media.film.doubanRating.lt(filmDoubanRatingCritical)));

        }

        Page<Media> pageCarrier = mediaRepository.findAll(predicate , pageable);
        List<Column> columnCarrier = query.getColumnList();

        map.put("pageCarrier", pageCarrier);
        map.put("columnCarrier", columnCarrier);

        return map;
    }




    public static boolean isNumeric(String str) {
        String bigStr;
        try {
            bigStr = new BigDecimal(str).toString();
        } catch (Exception e) {
            return false;//异常 说明包含非数字。
        }
        return true;
    }

}
