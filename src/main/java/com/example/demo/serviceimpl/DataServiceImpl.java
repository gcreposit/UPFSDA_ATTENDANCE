package com.example.demo.serviceimpl;

import com.example.demo.repository.AllAlertNotificationRepo;
import com.example.demo.repository.AllLabRtspUrlRepo;
import com.example.demo.repository.LabDetectionRepo;
import com.example.demo.service.DataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@Transactional
public class DataServiceImpl implements DataService {


    private final AllAlertNotificationRepo allAlertNotificationRepo;
    private final AllLabRtspUrlRepo allLabRtspUrlRepo;
    private final LabDetectionRepo labDetectionRepo;

    public DataServiceImpl(AllAlertNotificationRepo allAlertNotificationRepo,
                           AllLabRtspUrlRepo allLabRtspUrlRepo, LabDetectionRepo labDetectionRepo) {
        this.allAlertNotificationRepo = allAlertNotificationRepo;
        this.allLabRtspUrlRepo = allLabRtspUrlRepo;
        this.labDetectionRepo = labDetectionRepo;
    }


    @Value("${profile.directory}")
    private String defaultProfilePath;

    @Override
    public List<Map<String, Object>> fetchLabDetectionData(String id) {

        List<Map<String, Object>> labDetectionData = labDetectionRepo.findLabDetectionById(Integer.valueOf(id));
        return labDetectionData;
    }



    @Override
    public byte[] getFileData(String filePath) throws IOException {
        String finalPath=defaultProfilePath + filePath;
        log.info("File path to read: {}", finalPath);
        // Create a File object based on the provided path
        File file = new File(finalPath);

        // Check if the file exists
        if (!file.exists()) {
            throw new IOException("File not found"); // Throw exception if file does not exist
        }
        // Read the file into a byte array
        return Files.readAllBytes(file.toPath());
    }

}
