package com.example.system.controller;

import com.example.system.dto.TaskAllocationInfoDTO;
import com.example.system.result.CommonResult;
import com.example.system.service.TaskAllocationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/taskAllocation")
public class TaskAllocationController {

    @Autowired
    private TaskAllocationService taskAllocationService;

    /**
     * 新增任务分配信息
     */
    @PostMapping("/addAllocationInfos")
    public CommonResult<Void> addAllocationInfos(@RequestBody String param) {
        taskAllocationService.addAllocationInfos(param);
        return CommonResult.success();
    }

    /**
     * 删除任务分配信息
     */
    @DeleteMapping("/deleteAllocationInfo/{userId}/{taskId}")
    public CommonResult<Void> deleteAllocationInfo(@PathVariable("userId") Long userId, @PathVariable("taskId") Long taskId) {
        taskAllocationService.deleteAllocationInfo(userId, taskId);
        return CommonResult.success();
    }

    /**
     * 处理任务分配
     */
    @PutMapping("/dealTaskAllocationInfo")
    public CommonResult<Void> dealTaskAllocationInfo(@RequestParam("id") Long id, @RequestParam("status") Integer status) {
        taskAllocationService.dealTaskAllocationInfo(id, status);
        return CommonResult.success();
    }

    /**
     * 获取所有任务分配信息
     */
    @GetMapping("/getAllTaskAllocationInfo")
    public CommonResult<List<TaskAllocationInfoDTO>> getAllTaskAllocationInfo() {
        List<TaskAllocationInfoDTO> taskAllocationInfoDTOList = taskAllocationService.getAllTaskAllocationInfo();
        return CommonResult.success(taskAllocationInfoDTOList);
    }

    /**
     * 获取用户的任务分配信息
     */
    @GetMapping("/getAllTaskAllocationInfoByUserId/{userId}")
    public CommonResult<List<TaskAllocationInfoDTO>> getAllTaskAllocationInfoByUserId(@PathVariable("userId") Long userId) {
        List<TaskAllocationInfoDTO> taskAllocationInfoDTOList = taskAllocationService.getAllTaskAllocationInfoByUserId(userId);
        return CommonResult.success(taskAllocationInfoDTOList);
    }
}
