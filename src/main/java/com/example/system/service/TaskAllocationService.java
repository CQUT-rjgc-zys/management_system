package com.example.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.system.dto.TaskAllocationInfoDTO;
import com.example.system.entity.TaskAllocationEntity;

import java.util.List;

public interface TaskAllocationService extends IService<TaskAllocationEntity> {

    void addAllocationInfos(String param);

    void deleteAllocationInfo(Long userid, Long taskId);

    void dealTaskAllocationInfo(Long id, Integer status);

    List<TaskAllocationInfoDTO> getAllTaskAllocationInfo();

    List<TaskAllocationInfoDTO> getAllTaskAllocationInfoByUserId(Long userId);
}
