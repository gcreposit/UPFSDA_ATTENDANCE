package com.example.demo.serviceimpl;

import com.example.demo.entity.AllLabRtspUrl;
import com.example.demo.repository.AllAlertNotificationRepo;
import com.example.demo.repository.AllLabRtspUrlRepo;
import com.example.demo.repository.LabDetectionRepo;
import com.example.demo.service.AllLabRtspUrlService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional
public class AllLabRtspUrlServiceImpl extends BaseServiceImpl<AllLabRtspUrl, Integer> implements AllLabRtspUrlService {


    private final AllAlertNotificationRepo allAlertNotificationRepo;
    private final AllLabRtspUrlRepo allLabRtspUrlRepo;
    private final LabDetectionRepo labDetectionRepo;

    public AllLabRtspUrlServiceImpl(AllAlertNotificationRepo allAlertNotificationRepo,
                                    AllLabRtspUrlRepo allLabRtspUrlRepo,LabDetectionRepo labDetectionRepo) {
        super(allLabRtspUrlRepo);
        this.allAlertNotificationRepo = allAlertNotificationRepo;
        this.allLabRtspUrlRepo = allLabRtspUrlRepo;
        this.labDetectionRepo = labDetectionRepo;
    }

    @Override
    public List<AllLabRtspUrl> getAllLabRtspUrlData() {
        return allLabRtspUrlRepo.findAll();
    }


}
