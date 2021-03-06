package com.stemcloud.liye.dc.service;

import com.google.gson.Gson;
import com.stemcloud.liye.dc.dao.data.ContentRepository;
import com.stemcloud.liye.dc.dao.data.RecorderRepository;
import com.stemcloud.liye.dc.dao.data.ValueDataRepository;
import com.stemcloud.liye.dc.domain.data.ContentInfo;
import com.stemcloud.liye.dc.domain.data.RecorderInfo;
import com.stemcloud.liye.dc.util.LiveRecorderUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Belongs to data-camera-web
 * Description:
 *  后台定期监控，用于自动结束监控、录制等
 * @author liye on 2018/6/12
 */
@Component
public class ScheduleService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduleService.class);

    private final RecorderRepository recorderRepository;
    private final ActionService actionService;

    public ScheduleService(RecorderRepository recorderRepository, ActionService actionService) {
        this.recorderRepository = recorderRepository;
        this.actionService = actionService;
    }

    @Scheduled(initialDelay = 5000, fixedRate = 1000 * 60)
    public void checkRecorderStatus() {
        long currentTime = System.currentTimeMillis();
        List<RecorderInfo> recorders = recorderRepository.findRecordingRecorder();
        LOGGER.info("Check the recording recorders, find {} recorders", recorders.size());

        for (RecorderInfo r : recorders) {
            long expId = r.getExpId();
            long startTime = r.getStartTime().getTime();
            if (currentTime - startTime > 5 * 60 * 1000) {
                actionService.endRecord(expId, 0, "", "");
                LOGGER.info("[Schedule] End recorder, appId = {}, expId = {}, recorderId = {}", r.getAppId(), expId, r.getId());
            }
        }
    }

    @Scheduled(cron = "05 */5 * * * *")
    public void checkRecorderMap() {
        LOGGER.info("[Schedule] Current Recorder Map: {}", new Gson().toJson(LiveRecorderUtil.recorderStatusMap));
    }
}
