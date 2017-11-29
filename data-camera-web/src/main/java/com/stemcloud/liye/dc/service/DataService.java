package com.stemcloud.liye.dc.service;

import com.google.gson.Gson;
import com.stemcloud.liye.dc.dao.base.SensorRepository;
import com.stemcloud.liye.dc.dao.data.RecorderRepository;
import com.stemcloud.liye.dc.dao.data.ValueDataRepository;
import com.stemcloud.liye.dc.domain.base.SensorInfo;
import com.stemcloud.liye.dc.domain.data.RecorderDevices;
import com.stemcloud.liye.dc.domain.data.RecorderInfo;
import com.stemcloud.liye.dc.domain.data.ValueData;
import com.stemcloud.liye.dc.domain.view.ChartTimeSeries;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Belongs to data-camera-web
 * Description:
 *  service of sensor data
 * @author liye on 2017/11/16
 */
@Service
public class DataService {
    private final SensorRepository sensorRepository;
    private final ValueDataRepository valueDataRepository;
    private final RecorderRepository recorderRepository;

    @Autowired
    public DataService(SensorRepository sensorRepository, ValueDataRepository valueDataRepository, RecorderRepository recorderRepository) {
        this.sensorRepository = sensorRepository;
        this.valueDataRepository = valueDataRepository;
        this.recorderRepository = recorderRepository;
    }

    public Map<Long, Map<String, List<ChartTimeSeries>>> getRecentDataOfBoundSensors(long expId, long timestamp){
        List<SensorInfo> boundSensors = sensorRepository.findByExpIdAndIsDeleted(expId, 0);
        Set<Long> boundSensorIds = new HashSet<Long>();
        for (SensorInfo bs: boundSensors){
            boundSensorIds.add(bs.getId());
        }
        return transferChartData(valueDataRepository.findByCreateTimeGreaterThanAndSensorIdInOrderByCreateTime(new Date(timestamp), boundSensorIds));
    }

    public Map<Long, Map<Long, Map<String, List<ChartTimeSeries>>>> getContentDataOfExperiment(final long expId){
        Map<Long, Map<Long, Map<String, List<ChartTimeSeries>>>> result = new HashMap<Long, Map<Long, Map<String, List<ChartTimeSeries>>>>();
        List<RecorderInfo> ris = recorderRepository.findByExperiments(new HashSet<Long>(){{
            add(expId);
        }});
        for (RecorderInfo r : ris){
            long id = r.getId();
            RecorderDevices devices = new Gson().fromJson(r.getDevices(), RecorderDevices.class);

            Date startTime = r.getStartTime();
            Date endTime = r.getEndTime();
            Set<Long> sids = new HashSet<Long>(devices.getSensors());
            Map<Long, Map<String, List<ChartTimeSeries>>> map
                    = transferChartData(valueDataRepository.findBySensorIdInAndCreateTimeGreaterThanEqualAndCreateTimeLessThanEqualOrderByCreateTime(sids, startTime, endTime));
            result.put(id, map);
        }

        return result;
    }

    /**
     * sensor_id, (data_key, List<data_value>)
     */
    private Map<Long, Map<String, List<ChartTimeSeries>>> transferChartData(List<ValueData> vd){
        Map<Long, Map<String, List<ChartTimeSeries>>> result = new HashMap<Long, Map<String, List<ChartTimeSeries>>>();

        for (ValueData d : vd){
            long sensorId = d.getSensorId();
            String key = d.getKey();
            Double value = d.getValue();
            Date time = d.getCreateTime();

            if (!result.containsKey(sensorId)){
                Map<String, List<ChartTimeSeries>> map = new HashMap<String, List<ChartTimeSeries>>();
                List<ChartTimeSeries> list = new ArrayList<ChartTimeSeries>();
                list.add(new ChartTimeSeries(time, value));
                map.put(key, list);
                result.put(sensorId, map);
            } else {
                Map<String, List<ChartTimeSeries>> map = result.get(sensorId);
                List<ChartTimeSeries> list = new ArrayList<ChartTimeSeries>();
                if (map.containsKey(key)){
                    list = map.get(key);
                    list.add(new ChartTimeSeries(time, value));
                } else {
                    list.add(new ChartTimeSeries(time, value));
                }
                map.put(key, list);
                result.put(sensorId, map);
            }
        }

        return result;
    }
}
