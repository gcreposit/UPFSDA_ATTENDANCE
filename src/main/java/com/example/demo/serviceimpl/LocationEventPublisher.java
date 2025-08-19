package com.example.demo.serviceimpl;

import com.example.demo.entity.WffLocationTracking;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LocationEventPublisher {

    private final SimpMessagingTemplate messaging;

    public void publish(WffLocationTracking w) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", w.getId());
        payload.put("userName", w.getUserName());
        payload.put("lat", w.getLat());
        payload.put("lon", w.getLon());
        payload.put("timestamp", w.getTimestamp());

        Map<String, Object> location = new HashMap<>();
        location.put("lat", w.getLat());
        location.put("lng", w.getLon());

        payload.put("location", location);

        // broadcast to all dashboards
        messaging.convertAndSend("/topic/location.latest", payload);

        // per-user stream (optional)
        messaging.convertAndSend("/topic/location.user." + w.getUserName(), payload);
    }
}
