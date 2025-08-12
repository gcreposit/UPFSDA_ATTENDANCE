package com.example.demo.binlog;

import com.example.demo.entity.WffLocationTracking;
import com.example.demo.service.AttendanceService;
import com.example.demo.serviceimpl.AttendanceServiceImpl;
import com.github.shyiko.mysql.binlog.BinaryLogClient;
import com.github.shyiko.mysql.binlog.event.EventData;
import com.github.shyiko.mysql.binlog.event.UpdateRowsEventData;
import com.github.shyiko.mysql.binlog.event.WriteRowsEventData;
import com.github.shyiko.mysql.binlog.event.deserialization.EventDeserializer;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

@Component
public class MySQLBinlogListener {

    @Autowired
    private AttendanceServiceImpl attendanceService;

    @PostConstruct
    public void start() {
        try {
            // Change DB connection here
            BinaryLogClient client = new BinaryLogClient("localhost", 3306, "root", "password");

            EventDeserializer deserializer = new EventDeserializer();
            deserializer.setCompatibilityMode(EventDeserializer.CompatibilityMode.DATE_AND_TIME_AS_LONG);
            client.setEventDeserializer(deserializer);

            client.registerEventListener(event -> {
                EventData data = event.getData();

                if (data instanceof WriteRowsEventData writeData) {
                    processWriteEvent(writeData);
                } else if (data instanceof UpdateRowsEventData updateData) {
                    processUpdateEvent(updateData);
                }
            });

            new Thread(() -> {
                try {
                    client.connect();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void processWriteEvent(WriteRowsEventData data) {
        for (Serializable[] row : data.getRows()) {
            WffLocationTracking location = mapRowToEntity(row);
            if (location != null) {
                attendanceService.notifyClients(location.getUserName(), location);
            }
        }
    }

    private void processUpdateEvent(UpdateRowsEventData data) {
        for (Map.Entry<Serializable[], Serializable[]> entry : data.getRows()) {
            Serializable[] row = entry.getValue(); // updated data
            WffLocationTracking location = mapRowToEntity(row);
            if (location != null) {
                attendanceService.notifyClients(location.getUserName(), location);
            }
        }
    }

    private WffLocationTracking mapRowToEntity(Serializable[] row) {
        try {
            return WffLocationTracking.builder()
                    .id(((Number) row[0]).longValue())
                    .userName(row[1].toString())
                    .lat(row[2].toString())
                    .lon(row[3].toString())
                    .timestamp(LocalDateTime.parse(row[4].toString()))
                    .build();
        } catch (Exception e) {
            return null;
        }
    }
}
