package com.example.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.system.dto.Coordinate;
import com.example.system.dto.FieldTaskDTO;
import com.example.system.entity.FieldTaskEntity;
import com.example.system.entity.TaskAllocationEntity;
import com.example.system.entity.UserTaskLinkEntity;
import com.example.system.mapper.FieldTaskMapper;
import com.example.system.mapper.TaskAllocationMapper;
import com.example.system.mapper.UserTaskLinkMapper;
import com.example.system.service.FieldTaskService;
import com.example.system.util.UUIDUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FieldTaskTaskServiceImpl extends ServiceImpl<FieldTaskMapper, FieldTaskEntity> implements FieldTaskService {

    @Autowired
    private FieldTaskMapper fieldTaskMapper;

    @Autowired
    private TaskAllocationMapper taskAllocationMapper;

    @Autowired
    private UserTaskLinkMapper userTaskLinkMapper;

    @Override
    public void addFieldTask(FieldTaskDTO fieldTask) {
        String taskName = fieldTask.getName();
        QueryWrapper<FieldTaskEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("name", taskName);
        long count = this.count(queryWrapper);
        if (count > 0) {
            throw new IllegalArgumentException("任务名称已存在");
        }

        String taskBrief = fieldTask.getTaskBrief();
        if (taskBrief == null || taskBrief.trim().isEmpty()) {
            throw new IllegalArgumentException("任务简介不能为空");
        }

        Timestamp startTime = fieldTask.getStartTime();
        if (startTime == null) {
            throw new IllegalArgumentException("开始时间不能为空");
        }
        if (startTime.before(new Timestamp(System.currentTimeMillis()))) {
            throw new IllegalArgumentException("开始时间不能早于当前时间");
        }
        Timestamp endTime = fieldTask.getEndTime();
        if (endTime == null) {
            throw new IllegalArgumentException("结束时间不能为空");
        }
        if (endTime.before(startTime)) {
            throw new IllegalArgumentException("结束时间不能早于开始时间");
        }

        FieldTaskEntity fieldTaskEntity = new FieldTaskEntity();
        fieldTaskEntity.setId(UUIDUtil.uuid());
        fieldTaskEntity.setName(taskName);
        fieldTaskEntity.setTaskBrief(taskBrief);
        fieldTaskEntity.setStartTime(startTime);
        fieldTaskEntity.setEndTime(endTime);
        fieldTaskEntity.setStatus(0);
        save(fieldTaskEntity);
    }

    @Override
    public void deleteFieldTask(Long id) {
        FieldTaskEntity taskEntity = getById(id);
        if (taskEntity == null) {
            throw new IllegalArgumentException("不存在id为：" + id + "的任务信息");
        }
        if (taskEntity.getStatus() == 1) {
            QueryWrapper<UserTaskLinkEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("task_id", id);
            List<UserTaskLinkEntity> userTaskLinks = userTaskLinkMapper.selectList(queryWrapper);
            if (!userTaskLinks.isEmpty()) {
                throw new IllegalArgumentException("已有外勤人员正在执行此任务，不能删除");
            }
        }

        this.removeById(id);
        // 删除文件
        String fileAddress = taskEntity.getFileAddress();
        if (fileAddress != null) {
            String fileDir = "E:/File Upload Directory/";
            File file = new File(fileDir + fileAddress);
            if (file.exists()) {
                file.delete();
            }
        }
    }

    @Override
    public void updateFieldTask(FieldTaskDTO fieldTask) {
        String taskName = fieldTask.getName();
        QueryWrapper<FieldTaskEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("name", taskName);
        queryWrapper.ne("id", fieldTask.getId());
        long count = this.count(queryWrapper);
        if (count > 0) {
            throw new IllegalArgumentException("任务名称已存在");
        }
        FieldTaskEntity taskEntity = getById(fieldTask.getId());
        if (taskEntity == null) {
            throw new IllegalArgumentException("不存在id为：" + fieldTask.getId() + "的任务信息");
        }

        Timestamp startTime = fieldTask.getStartTime();
        if (startTime == null) {
            throw new IllegalArgumentException("开始时间不能为空");
        }
        if (startTime.before(new Timestamp(System.currentTimeMillis()))) {
            throw new IllegalArgumentException("开始时间不能早于当前时间");
        }
        Timestamp endTime = fieldTask.getEndTime();
        if (endTime == null) {
            throw new IllegalArgumentException("结束时间不能为空");
        }
        if (endTime.before(startTime)) {
            throw new IllegalArgumentException("结束时间不能早于开始时间");
        }
        BeanUtils.copyProperties(fieldTask, taskEntity);
//        taskEntity.setTaskSpot(fieldTask.getTaskSpot().toString());


        int status = taskEntity.getStatus();
        if (status == 0 && taskEntity.getDescription() != null && taskEntity.getTaskSpot() != null) {
            taskEntity.setStatus(1);
        }
        updateById(taskEntity);

    }

    @Override
    public List<FieldTaskDTO> getFieldTasks() {
        List<FieldTaskEntity> list = fieldTaskMapper.selectList(null);
        List<FieldTaskDTO> results = new ArrayList<>();
        list.forEach(task -> {
            FieldTaskDTO fieldTaskDTO = new FieldTaskDTO();
            BeanUtils.copyProperties(task, fieldTaskDTO);
            String taskSpot = task.getTaskSpot();
            if (taskSpot == null) {
                fieldTaskDTO.setTaskSpot(null);
            } else {
                fieldTaskDTO.setTaskSpot(new Coordinate(taskSpot));
            }
            results.add(fieldTaskDTO);
        });
        return results;
    }

    @Override
    public FieldTaskDTO getFieldTaskById(Long id) {
        FieldTaskEntity taskEntity = getById(id);
        if (taskEntity == null) {
            throw new IllegalArgumentException("不存在id为：" + id + "的任务信息");
        }
        FieldTaskDTO fieldTaskDTO = new FieldTaskDTO();
        BeanUtils.copyProperties(taskEntity, fieldTaskDTO);
        String taskSpot = taskEntity.getTaskSpot();
        if (taskSpot == null) {
            fieldTaskDTO.setTaskSpot(null);
        } else {
            fieldTaskDTO.setTaskSpot(new Coordinate(taskSpot));
        }
        String fileAddress = taskEntity.getFileAddress();
        if (fileAddress != null) {
            fieldTaskDTO.setFileName(getFileName(fileAddress));
        } else {{
            fieldTaskDTO.setFileName(null);
        }}
        return fieldTaskDTO;
    }

    @Override
    public void uploadFile(Long id, MultipartFile file) {
        // 将文件上传到 E:/upload/ 目录下，文件名为 id_文件名
        String uploadDir = "E:/File Upload Directory/";

        // 确保目录存在
        File dir = new File(uploadDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // 构建目标文件路径
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new IllegalArgumentException("File name must not be null");
        }
        String targetFilename = id + "_" + originalFilename;
        File targetFile = new File(uploadDir + targetFilename);

        try {
            // 将文件传输到目标位置
            file.transferTo(targetFile);
            // 更新数据库记录
            FieldTaskEntity taskEntity = getById(id);
            taskEntity.setFileAddress(uploadDir + targetFilename);
            updateById(taskEntity);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("File upload failed", e);
        }
    }

    @Override
    public byte[] downloadFile(Long id) {
        FieldTaskEntity taskEntity = getById(id);
        String fileAddress = taskEntity.getFileAddress();
        if (fileAddress == null) {
            throw new IllegalArgumentException("当前没有文件");
        }
        String fileDir = "E:/File Upload Directory/";
        // 读取文件内容并返回
        File file = new File(fileAddress);
        try {
            return Files.readAllBytes(file.toPath());
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("文件下载失败");
        }
    }

    @Override
    public List<FieldTaskDTO> getAcceptedFieldTasksByUserId(Long userId) {
        QueryWrapper<UserTaskLinkEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        List<UserTaskLinkEntity> userTaskLinks = userTaskLinkMapper.selectList(queryWrapper);
        List<Long> taskIds = userTaskLinks.stream().map(UserTaskLinkEntity::getTaskId).collect(Collectors.toList());
        if (taskIds.isEmpty()) {
            return new ArrayList<>();
        }
        List<FieldTaskEntity> fieldTaskEntities = listByIds(taskIds);
        List<FieldTaskDTO> results = new ArrayList<>();
        fieldTaskEntities.forEach(task -> {
            FieldTaskDTO fieldTaskDTO = new FieldTaskDTO();
            BeanUtils.copyProperties(task, fieldTaskDTO);
            String taskSpot = task.getTaskSpot();
            if (taskSpot == null) {
                fieldTaskDTO.setTaskSpot(null);
            } else {
                fieldTaskDTO.setTaskSpot(new Coordinate(taskSpot));
            }
            results.add(fieldTaskDTO);
        });
        return results;
    }

    private String getFileName(String fileAddress) {
        String[] split = fileAddress.split("_");
        return split[1];
    }
}
