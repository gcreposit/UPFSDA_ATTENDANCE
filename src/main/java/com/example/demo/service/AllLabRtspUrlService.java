package com.example.demo.service;

import com.example.demo.entity.AllLabRtspUrl;

import java.util.List;


public interface AllLabRtspUrlService extends BaseService<AllLabRtspUrl, Integer> {
    List<AllLabRtspUrl> getAllLabRtspUrlData();
}
