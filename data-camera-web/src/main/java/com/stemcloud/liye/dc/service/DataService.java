package com.stemcloud.liye.dc.service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.stemcloud.liye.dc.common.SensorType;
import com.stemcloud.liye.dc.dao.base.SensorRepository;
import com.stemcloud.liye.dc.dao.data.*;
import com.stemcloud.liye.dc.domain.base.SensorInfo;
import com.stemcloud.liye.dc.domain.data.*;
import com.stemcloud.liye.dc.domain.view.ChartDefine;
import com.stemcloud.liye.dc.domain.view.ChartEvent;
import com.stemcloud.liye.dc.domain.view.ChartTimeSeries;
import com.stemcloud.liye.dc.domain.view.Video;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Belongs to data-camera-web
 * Description:
 *  service of sensor data
 * @author liye on 2017/11/16
 */
@Service
public class DataService {
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private final SensorRepository sensorRepository;
    private final ValueDataRepository valueDataRepository;
    private final RecorderRepository recorderRepository;
    private final VideoDataRepository videoDataRepository;
    private final ContentRepository contentRepository;
    private final UserDefineChartRepository userDefineChartRepository;

    @Autowired
    public DataService(SensorRepository sensorRepository, ValueDataRepository valueDataRepository, RecorderRepository recorderRepository, VideoDataRepository videoDataRepository, ContentRepository contentRepository, UserDefineChartRepository userDefineChartRepository) {
        this.sensorRepository = sensorRepository;
        this.valueDataRepository = valueDataRepository;
        this.recorderRepository = recorderRepository;
        this.videoDataRepository = videoDataRepository;
        this.contentRepository = contentRepository;
        this.userDefineChartRepository = userDefineChartRepository;
    }

    /**
     * 获取实验最新的传感器数据
     *
     * @param expId 给定的实验id
     * @param timestamp 时间戳下限
     * @return sensor_id, (data_key, List<data_value>)
     */
    public Map<Long, Map<String, List<ChartTimeSeries>>> getRecentDataOfBoundSensors(long expId, long timestamp){
        List<SensorInfo> boundSensors = sensorRepository.findByExpIdAndIsDeleted(expId, 0);
        Set<Long> boundSensorIds = new HashSet<Long>();
        for (SensorInfo bs: boundSensors){
            if (bs.getSensorConfig().getType() == 1) {
                boundSensorIds.add(bs.getId());
            }
        }
        Date time = new Date(timestamp);
        List<ValueData> data = valueDataRepository.findByCreateTimeGreaterThanAndSensorIdInOrderByCreateTime(time, boundSensorIds);
        Map<Long, Map<String, List<ChartTimeSeries>>> map = transferChartData(data);
        logger.info("--> Get data of exp {}, sensor size is {}, data size is {}", expId, boundSensorIds.size(), data.size());
        return map;
    }

    /**
     * 获取数据片段的数据
     * @param recorderId
     * @return MAP
     *  key: SensorType
     *  value: sensor-id, data
     */
    public Map getRecorderData(long recorderId) throws ParseException {
        RecorderInfo recorder = recorderRepository.findOne(recorderId);
        List<RecorderDevices> devices = new Gson().fromJson(recorder.getDevices(), new TypeToken<ArrayList<RecorderDevices>>(){}.getType());
        Date startTime = recorder.getStartTime();
        Date endTime = recorder.getEndTime();

        // -- video data
        List<VideoData> videos = videoDataRepository.findByRecorderInfo(recorder);
        Map<Long, Video> videoMap = transferVideoData(videos);

        // -- value data for chart
        List<ValueData> chartValues = new ArrayList<ValueData>();
        List<Long> sensorIds = new ArrayList<Long>();
        for (RecorderDevices device: devices){
            sensorIds.add(device.getSensor());
        }
        List<ValueData> dataList = valueDataRepository.findBySensorIdInAndCreateTimeGreaterThanEqualAndCreateTimeLessThanEqualOrderByCreateTime(
                sensorIds, startTime, endTime
        );
        chartValues.addAll(dataList);
        Map<Long, Map<String, List<ChartTimeSeries>>> chartMap = transferChartData(chartValues);

        // -- user define chart
        List<ChartDefine> userDefineChart = new ArrayList<ChartDefine>();
        List<UserDefineChart> userCharts = userDefineChartRepository.findByRecorderId(recorderId);
        for (UserDefineChart uc : userCharts) {
            ChartDefine chartDefine = new ChartDefine();

            Map<String, List<ChartTimeSeries>> cd = chartMap.get(uc.getSensorId());
            List<ChartTimeSeries> x = cd.get(uc.getX());
            List<ChartTimeSeries> y = cd.get(uc.getY());
            chartDefine.setData(x, y);
            chartDefine.setInfo(uc);
            userDefineChart.add(chartDefine);
        }

        // -- event data
        List<ChartEvent> events = new ArrayList<ChartEvent>();
        events.add(new ChartEvent(startTime.getTime(), "START", "硬件"));
        for (ValueData data : chartValues) {
            if (data.getMark() != null) {
                events.add(new ChartEvent(data.getCreateTime().getTime(), data.getMark(), data.getKey() + "-" + data.getSensorId()));
            }
        }
        events.add(new ChartEvent(endTime.getTime(), "END", "硬件"));

        // -- 将不同数据段的数据对齐
        long maxDataTime = endTime.getTime();
        long minDataTime = startTime.getTime();
        SimpleDateFormat s = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        for (Map.Entry<Long, Map<String, List<ChartTimeSeries>>> entry : chartMap.entrySet()){
            Map<String, List<ChartTimeSeries>> map = entry.getValue();
            for (Map.Entry<String, List<ChartTimeSeries>> subEntry : map.entrySet()){
                List<ChartTimeSeries> list = subEntry.getValue();
                Date sDate = s.parse(String.valueOf(list.get(0).getValue().get(0)));
                Date eDate = s.parse(String.valueOf(list.get(list.size() - 1).getValue().get(0)));
                if (sDate.getTime() > minDataTime){
                    list.add(0, new ChartTimeSeries(new Date(minDataTime)));
                }
                if (eDate.getTime() < maxDataTime) {
                    list.add(new ChartTimeSeries(new Date(maxDataTime)));
                }
                map.put(subEntry.getKey(), list);
            }
            chartMap.put(entry.getKey(), map);
        }

        // -- 维护一个有序map作为返回，保证数据维度顺序一致
        Map<Long, LinkedHashMap<String, List<ChartTimeSeries>>> resultChartMap = new HashMap<Long, LinkedHashMap<String, List<ChartTimeSeries>>>();
        for (RecorderDevices device : devices) {
            long sensorId = device.getSensor();
            List<String> legends = device.getLegends();
            LinkedHashMap<String, List<ChartTimeSeries>> dataMap = new LinkedHashMap<String, List<ChartTimeSeries>>();
            for (String legend : legends) {
                if (chartMap.containsKey(sensorId) && chartMap.get(sensorId).containsKey(legend)) {
                    dataMap.put(legend, chartMap.get(sensorId).get(legend));
                }
            }
            if (!dataMap.isEmpty()) {
                resultChartMap.put(sensorId, dataMap);
            }
        }

        Map<String, Object> map = new HashMap<String, Object>(2);
        map.put(SensorType.CHART.toString(), resultChartMap);
        map.put(SensorType.VIDEO.toString(), videoMap);
        map.put("EVENT", events);
        map.put("DEFINE", userDefineChart);
        map.put("MIN", minDataTime);
        map.put("MAX", maxDataTime);

        return map;
    }

    /**
     * 新生成一条用户自定义的实验片段
     *
     * @param recorderId 片段id
     * @param start 数据截取起点
     * @param end 数据截取终点
     */
    public Long generateUserContent(long recorderId, String start, String end, String name, String desc) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        RecorderInfo recorder = recorderRepository.findOne(recorderId);
        List<RecorderDevices> devices = new Gson().fromJson(recorder.getDevices(), new TypeToken<ArrayList<RecorderDevices>>(){}.getType());

        // 保存新的实验记录
        RecorderInfo newRecorder = new RecorderInfo();
        newRecorder.setName(name);
        newRecorder.setDescription(desc);
        newRecorder.setStartTime(sdf.parse(start));
        newRecorder.setEndTime(sdf.parse(end));
        newRecorder.setExpId(recorder.getExpId());
        newRecorder.setAppId(recorder.getAppId());
        newRecorder.setIsRecorder(0);
        newRecorder.setIsUserGen(1);
        newRecorder.setParentId(recorderId);
        newRecorder.setDevices(new Gson().toJson(devices));
        newRecorder.setStartSeconds( (sdf.parse(start).getTime() - recorder.getStartTime().getTime())/1000 + recorder.getStartSeconds() );
        long newR = recorderRepository.save(newRecorder).getId();

        // 若有视频，则在video表中生成记录
        for (RecorderDevices device : devices) {
            if (device.getLegends().size() == 1 && "视频".equals(device.getLegends().get(0))) {
                List<VideoData> videos = videoDataRepository.findByRecorderInfo(recorder);
                List<VideoData> newVideos = new ArrayList<VideoData>();
                for (VideoData video : videos) {
                    VideoData newVideo = new VideoData(video.getTrackId(), video.getSensorId(),
                            newRecorder, video.getVideoPath(),video.getVideoPost());
                    newVideos.add(newVideo);
                }
                videoDataRepository.save(newVideos);
                break;
            }
        }

        return newR;
    }

    /**
     * 将valueData转换成为echart需要的时间数据格式
     * @param vd
     * @return sensor_id, (data_key, List<data_value>)
     */
    private Map<Long, Map<String, List<ChartTimeSeries>>> transferChartData(List<ValueData> vd){
        Map<Long, Map<String, List<ChartTimeSeries>>> result = new HashMap<Long, Map<String, List<ChartTimeSeries>>>(16);

        for (ValueData d : vd){
            long sensorId = d.getSensorId();
            String key = d.getKey();

            if (!result.containsKey(sensorId)){
                Map<String, List<ChartTimeSeries>> map = new HashMap<String, List<ChartTimeSeries>>();
                List<ChartTimeSeries> list = new ArrayList<ChartTimeSeries>();
                list.add(new ChartTimeSeries(d));
                map.put(key, list);
                result.put(sensorId, map);
            } else {
                Map<String, List<ChartTimeSeries>> map = result.get(sensorId);
                List<ChartTimeSeries> list = new ArrayList<ChartTimeSeries>();
                if (map.containsKey(key)){
                    list = map.get(key);
                    list.add(new ChartTimeSeries(d));
                } else {
                    list.add(new ChartTimeSeries(d));
                }
                map.put(key, list);
                result.put(sensorId, map);
            }
        }
        return result;
    }

    /**
     * 将videoData转换成为video.js需要的数据格式
     * @param videos
     * @return sensor_id, Video
     */
    private Map<Long, Video> transferVideoData(List<VideoData> videos){
        Map<Long, Video> map = new HashMap<Long, Video>();
        for (VideoData v : videos){
            long sensorId = v.getSensorId();
            Video video = new Video();
            video.setOption(v.getVideoPost(), v.getVideoPath());
            video.setRecorderId(v.getRecorderInfo().getId());
            map.put(sensorId, video);
        }
        return map;
    }

    /**
     * 更新数据标注
     * @param id
     * @param mark
     */
    public int updateDataMarker(long id, String mark){
        return valueDataRepository.updateMarker(id, mark);
    }

    public List<ContentInfo> getSearchContent(String search) {
        List<ContentInfo> content = new ArrayList<ContentInfo>();
        if (search.trim().isEmpty()) {
            return content;
        }
        return contentRepository.findByIsSharedAndIsDeletedAndTitleLikeOrderByLikeDesc(1,0,'%' + search + '%');
    }

    public Map<String, String> userNewChart(long sensorId, long recorderId, String x, String y, String name, String desc) {
        UserDefineChart chart = new UserDefineChart();
        chart.setRecorderId(recorderId);
        chart.setSensorId(sensorId);
        chart.setX(x);
        chart.setY(y);
        chart.setName(name);
        chart.setDesc(desc);

        UserDefineChart newChart = userDefineChartRepository.save(chart);
        Map<String, String> result = new HashMap<String, String>();
        result.put("id", String.valueOf(newChart.getId()));
        result.put("recorder", String.valueOf(newChart.getRecorderId()));

        return result;
    }
}
