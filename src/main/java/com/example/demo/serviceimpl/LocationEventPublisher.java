package com.example.demo.serviceimpl;

import com.example.demo.entity.WffLocationTracking;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class LocationEventPublisher {

    private final SimpMessagingTemplate messaging;

    public void publish(WffLocationTracking w) {
        var payload = Map.of(
                "id", w.getId(),
                "userName", w.getUserName(),
                "lat", w.getLat(),
                "lon", w.getLon(),
                "timestamp", w.getTimestamp(),
                "location", Map.of("lat", w.getLat(), "lng", w.getLon())
        );
        // broadcast to all dashboards
        messaging.convertAndSend("/topic/location.latest", payload);
        // per-user stream (optional)
        messaging.convertAndSend("/topic/location.user." + w.getUserName(), payload);
    }

}