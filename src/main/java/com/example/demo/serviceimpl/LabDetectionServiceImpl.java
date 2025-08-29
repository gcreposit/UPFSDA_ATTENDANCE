package com.example.demo.serviceimpl;

import com.example.demo.entity.LabDetection;
import com.example.demo.repository.LabDetectionRepo;
import com.example.demo.service.LabDetectionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
public class LabDetectionServiceImpl  extends BaseServiceImpl<LabDetection, Integer> implements LabDetectionService {

    private final LabDetectionRepo labDetectionRepo;

    public LabDetectionServiceImpl(LabDetectionRepo labDetectionRepo) {
        super(labDetectionRepo);
        this.labDetectionRepo = labDetectionRepo;
    }


    // Additional methods specific to LabDetection can be added here
}
