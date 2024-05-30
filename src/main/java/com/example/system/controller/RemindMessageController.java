package com.example.system.controller;

import com.example.system.result.CommonResult;
import com.example.system.service.RemindMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/message")
public class RemindMessageController {

    @Autowired
    private RemindMessageService remindMessageService;

    /**
     * 向今日未及时打卡的外勤人员发送消息提醒
     */
    @PostMapping("/sendMessagesByTaskId/{taskId}")
    public CommonResult<Void> sendMessagesByTaskId(@PathVariable("taskId") Long taskId) {
        remindMessageService.sendMessagesByTaskId(taskId);
        return CommonResult.success();
    }

//    @GetMapping("/getRemindMessage/")
//    public CommonResult<Void> test() {

}
