package com.example.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.system.entity.FieldTaskEntity;
import com.example.system.entity.PunchCardEntity;
import com.example.system.entity.RemindMessageEntity;
import com.example.system.entity.UserTaskLinkEntity;
import com.example.system.mapper.PunchCardMapper;
import com.example.system.mapper.RemindMessageMapper;
import com.example.system.mapper.UserTaskLinkMapper;
import com.example.system.service.FieldTaskService;
import com.example.system.service.RemindMessageService;
import com.example.system.util.UUIDUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class RemindMessageServiceImpl extends ServiceImpl<RemindMessageMapper, RemindMessageEntity> implements RemindMessageService {

    @Autowired
    private UserTaskLinkMapper userTaskLinkMapper;

    @Autowired
    private PunchCardMapper punchCardMapper;

    @Autowired
    private FieldTaskService fileTaskService;

    @Override
    public void sendMessagesByTaskId(Long taskId) {
        FieldTaskEntity taskEntity = fileTaskService.getById(taskId);
        QueryWrapper<UserTaskLinkEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("task_id", taskId);
        List<UserTaskLinkEntity> userTaskLinkEntities = userTaskLinkMapper.selectList(queryWrapper);
        // 获取到所有参与任务人员的id信息
        List<Long> userIds = userTaskLinkEntities.stream().map(UserTaskLinkEntity::getUserId).collect(Collectors.toList());

        // 获取到今天已经打卡的人员的id信息
        QueryWrapper<PunchCardEntity> punchCardQueryWrapper = new QueryWrapper<>();
        punchCardQueryWrapper.eq("task_id", taskId);
        Date today = new Date();
        Date startOfDay = new Date(today.getTime() - today.getTime() % (24 * 60 * 60 * 1000));
        punchCardQueryWrapper.ge("time", startOfDay);
        List<PunchCardEntity> punchCardEntities = punchCardMapper.selectList(punchCardQueryWrapper);
        List<Long> punchedUserIds = punchCardEntities.stream().map(PunchCardEntity::getUserId).collect(Collectors.toList());

        // 筛选出今天还未打卡的人员的id信息
        List<Long> needPunchUserIds = userIds.stream().filter(userId -> !punchedUserIds.contains(userId)).collect(Collectors.toList());

        // 发送提醒信息
        List<RemindMessageEntity> messageEntities = new ArrayList<>();
        for (Long userId : needPunchUserIds) {
            RemindMessageEntity remindMessageEntity = new RemindMessageEntity();
            remindMessageEntity.setId(UUIDUtil.uuid());
            remindMessageEntity.setUserId(userId);
            remindMessageEntity.setTaskName(taskEntity.getName());
            remindMessageEntity.setReceive(false);
            messageEntities.add(remindMessageEntity);
        }
        this.saveBatch(messageEntities);
    }
}
