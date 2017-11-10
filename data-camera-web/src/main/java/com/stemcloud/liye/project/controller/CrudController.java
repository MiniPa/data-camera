package com.stemcloud.liye.project.controller;

import com.stemcloud.liye.project.domain.base.AppInfo;
import com.stemcloud.liye.project.domain.base.ExperimentInfo;
import com.stemcloud.liye.project.domain.base.SensorInfo;
import com.stemcloud.liye.project.domain.base.TrackInfo;
import com.stemcloud.liye.project.service.CommonService;
import com.stemcloud.liye.project.service.CrudService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * Belongs to data-camera-web
 * Description:
 *  增加(Create)、读取查询(Retrieve)、更新(Update)、删除(Delete)
 *  for app, experiment, track and sensor
 * @author liye on 2017/11/6
 */
@RestController
@RequestMapping("/crud")
public class CrudController {
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private final CommonService commonService;
    private final CrudService crudService;

    @Autowired
    public CrudController(CommonService commonService, CrudService crudService) {
        this.commonService = commonService;
        this.crudService = crudService;
    }

    @PostMapping("/app/new")
    public Long newApp(@RequestParam Map<String, String> queryParams, HttpServletRequest request){
        AppInfo appInfo = new AppInfo();
        String user = commonService.getCurrentLoginUser(request);
        appInfo.setCreator(user);
        appInfo.setName(queryParams.get("app-name"));
        appInfo.setDescription(queryParams.get("app-desc"));

        Long id = crudService.newApp(appInfo);
        logger.info("USER " + user + " NEW APP " + id);
        return id;
    }

    @PostMapping("/app/update")
    public Integer updateApp(@RequestParam Map<String, String> queryParams, HttpServletRequest request){
        String user = commonService.getCurrentLoginUser(request);
        Long id = Long.parseLong(queryParams.get("app-id"));
        if (id <= 0){
            throw new IllegalArgumentException("ERROR PARAMETERS WHEN UPDATE APP");
        }

        AppInfo appInfo = crudService.findApp(id);
        appInfo.setId(id);
        logger.info("USER " + user + " UPDATE APP " + id);

        String appName = "app-name";
        if (queryParams.containsKey(appName) && !queryParams.get(appName).trim().isEmpty()) {
            appInfo.setName(queryParams.get(appName));
        }
        String appDesc = "app-desc";
        if (queryParams.containsKey(appDesc) && !queryParams.get(appDesc).trim().isEmpty()) {
            appInfo.setDescription(queryParams.get(appDesc));
        }

        return crudService.updateApp(appInfo);
    }

    @GetMapping("/app/delete")
    public Integer deleteApp(@RequestParam Map<String, String> queryParams, HttpServletRequest request){
        Long id = Long.parseLong(queryParams.get("app-id"));
        String user = commonService.getCurrentLoginUser(request);
        if (id <= 0){
            throw new IllegalArgumentException("ERROR PARAMETERS WHEN DELETE APP");
        }
        logger.info("USER " + user + " DELETE APP " + id);
        return crudService.deleteApp(id);
    }

    @PostMapping("/exp/new")
    public Long newExp(@RequestParam Map<String, String> queryParams, HttpServletRequest request){
        ExperimentInfo expInfo = new ExperimentInfo();
        String user = commonService.getCurrentLoginUser(request);
        expInfo.setName(queryParams.get("exp-name"));
        expInfo.setDescription(queryParams.get("exp-desc"));

        Long appId = Long.parseLong(queryParams.get("app-id"));
        expInfo.setApp(crudService.findApp(appId));

        Long id = crudService.newExp(expInfo);
        logger.info("USER " + user + " NEW EXP " + id);
        return id;
    }

    @PostMapping("/exp/update")
    public Integer updateExp(@RequestParam Map<String, String> queryParams, HttpServletRequest request){
        String user = commonService.getCurrentLoginUser(request);
        Long id = Long.parseLong(queryParams.get("exp-id"));
        if (id <= 0){
            throw new IllegalArgumentException("ERROR PARAMETERS WHEN UPDATE EXPERIMENT");
        }

        ExperimentInfo expInfo = crudService.findExp(id);
        expInfo.setId(id);
        logger.info("USER " + user + " UPDATE EXP " + id);

        String expName = "exp-name";
        if (queryParams.containsKey(expName) && !queryParams.get(expName).trim().isEmpty()) {
            expInfo.setName(queryParams.get(expName));
        }
        String expDesc = "exp-desc";
        if (queryParams.containsKey(expDesc) && !queryParams.get(expDesc).trim().isEmpty()) {
            expInfo.setDescription(queryParams.get(expDesc));
        }

        return crudService.updateExp(expInfo);
    }

    @GetMapping("/exp/delete")
    public Integer deleteExp(@RequestParam Map<String, String> queryParams, HttpServletRequest request){
        Long id = Long.parseLong(queryParams.get("exp-id"));
        String user = commonService.getCurrentLoginUser(request);
        if (id <= 0){
            throw new IllegalArgumentException("ERROR PARAMETERS WHEN DELETE EXPERIMENT");
        }
        logger.info("USER " + user + " DELETE EXP " + id);
        return crudService.deleteExp(id);
    }

    @GetMapping("/track/new")
    public Long newTrack(@RequestParam Map<String, String> queryParams, HttpServletRequest request){
        TrackInfo trackInfo = new TrackInfo();
        String user = commonService.getCurrentLoginUser(request);
        Long expId = Long.parseLong(queryParams.get("exp-id"));
        trackInfo.setExperiment(crudService.findExp(expId));

        Long id = crudService.newTrack(trackInfo);
        logger.info("USER " + user + " NEW TRACK " + id);
        return id;
    }

    @GetMapping("/track/bound")
    public Long boundTrack(@RequestParam Map<String, String> queryParams, HttpServletRequest request){
        String user = commonService.getCurrentLoginUser(request);
        Long trackId = Long.parseLong(queryParams.get("track-id"));
        Long sensorId = Long.parseLong(queryParams.get("sensor-id"));
        if (trackId <= 0 || sensorId <= 0){
            throw new IllegalArgumentException("ERROR PARAMETERS WHEN BOUND TRACK");
        }
        TrackInfo track = crudService.findTrack(trackId);
        track.setSensor(crudService.findSensor(sensorId));

        logger.info("USER " + user + " BOUND SENSOR " + sensorId + " ON TRACK " + trackId);
        return crudService.newTrack(track);
    }

    @GetMapping("/track/unbound")
    public Long unboundTrack(@RequestParam Map<String, String> queryParams, HttpServletRequest request){
        String user = commonService.getCurrentLoginUser(request);
        Long trackId = Long.parseLong(queryParams.get("track-id"));
        if (trackId <= 0){
            throw new IllegalArgumentException("ERROR PARAMETERS WHEN UNBOUND TRACK");
        }
        TrackInfo track = crudService.findTrack(trackId);
        track.setSensor(null);

        logger.info("USER " + user + " UNBOUND SENSOR ON TRACK " + trackId);
        return crudService.newTrack(track);
    }

    @GetMapping("/track/delete")
    public Integer deleteTrack(@RequestParam Map<String, String> queryParams, HttpServletRequest request){
        String user = commonService.getCurrentLoginUser(request);
        Long trackId = Long.parseLong(queryParams.get("track-id"));
        if (trackId <= 0){
            throw new IllegalArgumentException("ERROR PARAMETERS WHEN DELETE TRACK");
        }
        logger.info("USER " + user + " DELETE TRACK " + trackId);
        return crudService.deleteTrack(trackId);
    }

    @GetMapping("/sensor/new")
    public Long newSensor(@RequestParam Map<String, String> queryParams, HttpServletRequest request){
        SensorInfo sensor = new SensorInfo();
        String user = commonService.getCurrentLoginUser(request);

        String sensorName = "sensor-name";
        String sensorCode = "sensor-code";
        if (!queryParams.containsKey(sensorName) || !queryParams.containsKey(sensorCode)){
            throw new IllegalArgumentException("ERROR PARAMETERS WHEN NEW SENSOR");
        }

        sensor.setCreator(user);
        sensor.setName(queryParams.get(sensorName));
        sensor.setCode(queryParams.get(sensorCode));

        String city = "city";
        String latitude = "latitude";
        String longitude = "longitude";
        String description = "description";
        if (queryParams.containsKey(latitude)){
            sensor.setLatitude(Double.valueOf(queryParams.get(latitude)));
        }
        if (queryParams.containsKey(longitude)){
            sensor.setLatitude(Double.valueOf(queryParams.get(longitude)));
        }
        if (queryParams.containsKey(city)){
            sensor.setCity(queryParams.get(city));
        }
        if (queryParams.containsKey(description)){
            sensor.setDescription(description);
        }

        Long id = crudService.saveSensor(sensor);
        logger.info("USER " + user + " NEW SENSOR " + id);
        return id;
    }

    @GetMapping("/sensor/delete")
    public Integer deleteSensor(@RequestParam Map<String, String> queryParams, HttpServletRequest request){
        String user = commonService.getCurrentLoginUser(request);
        Long sensorId = Long.parseLong(queryParams.get("sensor-id"));
        if (sensorId <= 0){
            throw new IllegalArgumentException("ERROR PARAMETERS WHEN DELETE SENSOR");
        }
        logger.info("USER " + user + " DELETE TRACK " + sensorId);
        return crudService.deleteSensor(sensorId);
    }
}
