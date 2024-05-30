package com.example.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.system.entity.RemindMessageEntity;

public interface RemindMessageService extends IService<RemindMessageEntity> {

    void sendMessagesByTaskId(Long taskId);
}
