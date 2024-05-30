package com.example.system.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.system.dto.TaskAllocationInfoDTO;
import com.example.system.entity.FieldTaskEntity;
import com.example.system.entity.TaskAllocationEntity;
import com.example.system.entity.UserEntity;
import com.example.system.entity.UserTaskLinkEntity;
import com.example.system.mapper.FieldTaskMapper;
import com.example.system.mapper.TaskAllocationMapper;
import com.example.system.mapper.UserMapper;
import com.example.system.mapper.UserTaskLinkMapper;
import com.example.system.service.TaskAllocationService;
import com.example.system.util.UUIDUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TaskAllocationServiceImpl extends ServiceImpl<TaskAllocationMapper, TaskAllocationEntity> implements TaskAllocationService {

    @Autowired
    private FieldTaskMapper fieldTaskMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserTaskLinkMapper userTaskLinkMapper;

    @Override
    public void addAllocationInfos(String param) {
        List<TaskAllocationInfoDTO> list = JSON.parseArray(param, TaskAllocationInfoDTO.class);
        // 校验参数
        Long taskId = list.get(0).getTaskId();
        List<Long> userIds = new ArrayList<>();
        for(TaskAllocationInfoDTO item : list) {
            userIds.add(item.getUserId());
        }
        QueryWrapper<FieldTaskEntity> taskQueryWrapper = new QueryWrapper<>();
        taskQueryWrapper.eq("id", taskId);
        FieldTaskEntity fieldTaskEntity = fieldTaskMapper.selectOne(taskQueryWrapper);
        if (fieldTaskEntity == null) {
            throw new IllegalArgumentException("不存在id为：" + taskId + "的任务信息");
        }
        // 校验用户信息是否存在
        QueryWrapper<UserEntity> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.in("id", userIds);
        List<UserEntity> userEntities = userMapper.selectList(userQueryWrapper);
        List<Long> existIds = userEntities.stream().map(UserEntity::getId).collect(Collectors.toList());
        userIds.removeAll(existIds);
        if (!userIds.isEmpty()) {
            throw new IllegalArgumentException("不存在以下id对应的用户信息：" + userIds);
        }

        // 保存分配信息
        existIds.forEach(userId -> {
            TaskAllocationEntity taskAllocationEntity = new TaskAllocationEntity();
            taskAllocationEntity.setId(UUIDUtil.uuid());
            taskAllocationEntity.setTaskId(taskId);
            taskAllocationEntity.setUserId(userId);
            taskAllocationEntity.setStatus(0);
            save(taskAllocationEntity);
        });
    }

    @Override
    public void deleteAllocationInfo(Long userId, Long taskId) {
        QueryWrapper<UserTaskLinkEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        queryWrapper.eq("task_id", taskId);
        userTaskLinkMapper.delete(queryWrapper);
    }

    @Override
    public void dealTaskAllocationInfo(Long id, Integer status) {
        // 校验参数
        QueryWrapper<TaskAllocationEntity> taskAllocationQueryWrapper = new QueryWrapper<>();
        taskAllocationQueryWrapper.eq("id", id);
        TaskAllocationEntity taskAllocationEntity = getOne(taskAllocationQueryWrapper);
        if (taskAllocationEntity == null) {
            throw new IllegalArgumentException("不存在id为：" + id + "的任务分配信息");
        }
        if (taskAllocationEntity.getStatus() != 0) {
            throw new IllegalArgumentException("任务：" + id + "已经被处理");
        }
        if (status!= 1 && status!= 2) {
            throw new IllegalArgumentException("状态只能为1或2");
        }
        // 更新任务分配信息
        taskAllocationEntity.setStatus(status);
        updateById(taskAllocationEntity);
        UserTaskLinkEntity userTaskLinkEntity = new UserTaskLinkEntity();
        userTaskLinkEntity.setTaskId(taskAllocationEntity.getTaskId());
        userTaskLinkEntity.setUserId(taskAllocationEntity.getUserId());
        userTaskLinkMapper.insert(userTaskLinkEntity);
    }

    @Override
    public List<TaskAllocationInfoDTO> getAllTaskAllocationInfo() {
        List<TaskAllocationEntity> taskAllocationEntities = list();
        return taskAllocationEntities.stream().map(taskAllocationEntity -> {
            TaskAllocationInfoDTO taskAllocationInfoDTO = new TaskAllocationInfoDTO();
            taskAllocationInfoDTO.setId(taskAllocationEntity.getId());
            taskAllocationInfoDTO.setTaskId(taskAllocationEntity.getTaskId());
            taskAllocationInfoDTO.setUserId(taskAllocationEntity.getUserId());
            taskAllocationInfoDTO.setStatus(taskAllocationEntity.getStatus());
            return taskAllocationInfoDTO;
        }).collect(Collectors.toList());
    }

    @Override
    public List<TaskAllocationInfoDTO> getAllTaskAllocationInfoByUserId(Long userId) {
        QueryWrapper<TaskAllocationEntity> taskAllocationQueryWrapper = new QueryWrapper<>();
        taskAllocationQueryWrapper.eq("user_id", userId);
        List<TaskAllocationEntity> taskAllocationEntities = list(taskAllocationQueryWrapper);
        return taskAllocationEntities.stream().map(taskAllocationEntity -> {
            TaskAllocationInfoDTO taskAllocationInfoDTO = new TaskAllocationInfoDTO();
            taskAllocationInfoDTO.setId(taskAllocationEntity.getId());
            taskAllocationInfoDTO.setTaskId(taskAllocationEntity.getTaskId());
            taskAllocationInfoDTO.setUserId(taskAllocationEntity.getUserId());
            taskAllocationInfoDTO.setStatus(taskAllocationEntity.getStatus());
            return taskAllocationInfoDTO;
        }).collect(Collectors.toList());
    }
}
