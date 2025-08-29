package com.example.demo.controller;

import com.example.demo.service.DataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/monitoring/api/data")
public class DataController {


    private final DataService dataService;

    public DataController(DataService dataService) {
        this.dataService = dataService;

    }

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @PostMapping("/alert")
    public void receiveAlert(@RequestBody String alertMessage) {
        log.info("Received alert: {}", alertMessage);
        messagingTemplate.convertAndSend("/topic/alerts", alertMessage);
    }


    @PostMapping("/fetchLabDetectionData")
    public List<Map<String, Object>> fetchLabDetectionData(@RequestParam(value = "id") String id) {

        return dataService.fetchLabDetectionData(id);
    }


    // Endpoint to get byte array of the image
    @PostMapping(value = "/getProfileImgData", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<byte[]> getImgData(@RequestParam String filePath) throws IOException {

        try {
            // Use the service to get the file data
            byte[] fileData = dataService.getFileData(filePath);

            // Return the byte array wrapped in a ResponseEntity
            return ResponseEntity.ok(fileData);

        } catch (IOException e) {
            // Return 404 if file does not exist or any other IOException occurs
            return ResponseEntity.status(404).body(null);
        }
    }

}
