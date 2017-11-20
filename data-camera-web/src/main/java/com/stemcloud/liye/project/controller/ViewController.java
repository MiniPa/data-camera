package com.stemcloud.liye.project.controller;

import com.stemcloud.liye.project.domain.base.AppInfo;
import com.stemcloud.liye.project.domain.base.ExperimentInfo;
import com.stemcloud.liye.project.domain.base.SensorInfo;
import com.stemcloud.liye.project.domain.base.TrackInfo;
import com.stemcloud.liye.project.service.BaseInfoService;
import com.stemcloud.liye.project.service.CommonService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

/**
 * Belongs to data-camera-web
 * Description:
 *  controller of view (*.html)
 * @author liye on 2017/11/6
 */
@Controller
public class ViewController {
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private final CommonService commonService;
    private final BaseInfoService baseInfoService;

    @Autowired
    public ViewController(CommonService commonService, BaseInfoService baseInfoService) {
        this.commonService = commonService;
        this.baseInfoService = baseInfoService;
    }

    @GetMapping("/index")
    public String hello(Model model) {
        logger.info("In index.html");
        model.addAttribute("inIndex", true);
        return "index";
    }

    @GetMapping("/app")
    public String app(@RequestParam(value = "id", required = false) Long id, Model model,
                      HttpServletRequest request, HttpServletResponse response) throws IOException {
        // --- login before visit
        String user = commonService.getCurrentLoginUser(request);
        if (user == null) {
            logger.warn("No login user");
            response.sendRedirect(request.getContextPath() + "/login");
            return "login";
        }

        // --- can't visit app that not belong the user
        if (id != null && !baseInfoService.isAppBelongUser(id, user)){
            logger.warn("The user: " + user + " has not the app " + id);
            response.sendRedirect(request.getContextPath() + "/login");
            return "login";
        }

        logger.info("In app.html");
        model.addAttribute("inApp", true);
        List<AppInfo> apps = baseInfoService.getOnlineApps(user);
        model.addAttribute("apps", apps);

        if (id == null){
            // --- id is null, in app management page
            logger.info("APP PAGE: IN APP MANAGE PAGE!");
        } else {
            // --- APP: app detail page
            logger.info("APP PAGE: IN APP DETAIL PAGE!");
            model.addAttribute("app", baseInfoService.getCurrentApp(id));

            // --- EXP: get experiments of the app, get bound sensors, get monitor, recorder sensors
            List<ExperimentInfo> experiments = baseInfoService.getOnlineExpOfApp(id);
            Map<Long, List<SensorInfo>> boundSensors = new HashMap<Long, List<SensorInfo>>(experiments.size());
            Map<Long, Integer> isExperimentMonitor = new HashMap<Long, Integer>(experiments.size());
            Map<Long, Integer> isExperimentRecorder = new HashMap<Long, Integer>(experiments.size());
            List<TrackInfo> tracks = new ArrayList<TrackInfo>();
            for (ExperimentInfo exp : experiments){
                Set<TrackInfo> newTracks = new HashSet<TrackInfo>();
                isExperimentMonitor.put(exp.getId(), 0);
                isExperimentRecorder.put(exp.getId(), 0);

                for (TrackInfo track: exp.getTrackInfoList()){
                    if (track.getIsDeleted() == 0){
                        tracks.add(track);
                        TrackInfo newTrack = new TrackInfo();
                        newTrack.setId(track.getId());
                        newTrack.setSensor(track.getSensor());
                        newTrack.setType(track.getType());
                        newTracks.add(newTrack);

                        // add bound sensors
                        if (track.getSensor() != null){
                            List<SensorInfo> expBoundSensors = new ArrayList<SensorInfo>();
                            if (boundSensors.containsKey(exp.getId())){
                                expBoundSensors = boundSensors.get(exp.getId());
                            }
                            expBoundSensors.add(track.getSensor());
                            boundSensors.put(exp.getId(), expBoundSensors);

                            if (track.getSensor().getIsMonitor() == 1){
                                isExperimentMonitor.put(exp.getId(), 1);
                            }
                            if (track.getSensor().getIsRecoder() == 1){
                                isExperimentRecorder.put(exp.getId(), 1);
                            }
                        }
                    }
                }
                exp.setTrackInfoList(newTracks);
            }
            model.addAttribute("experiments", experiments);
            model.addAttribute("isExperimentMonitor", isExperimentMonitor);
            model.addAttribute("isExperimentRecorder", isExperimentRecorder);

            // --- TRACK
            model.addAttribute("tracks", tracks);

            // --- SENSOR: get user's sensor of this app and available sensors
            List<SensorInfo> availableSensor = baseInfoService.getAvailableSensorOfCurrentUser(user);
            model.addAttribute("freeSensors", availableSensor);
            model.addAttribute("boundSensors", boundSensors);

            /*List<SensorInfo> sensors = baseInfoService.getSensorsOfCurrentApp(id);
            model.addAttribute("sensors", sensors);*/
        }

        return "app";
    }

    @GetMapping("/denied")
    public String denied(HttpServletRequest request) {
        logger.warn("denied: " + request.getContextPath());
        return "denied";
    }

    @GetMapping("/exception")
    public String exception(){
        logger.warn("exception");
        return "exception";
    }

    @GetMapping("/login")
    public String login(){
        logger.info("In login.html");
        return "login";
    }
}
