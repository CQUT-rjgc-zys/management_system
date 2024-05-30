package com.example.system.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.system.dto.UserDTO;
import com.example.system.entity.DepartmentEntity;
import com.example.system.entity.TaskAllocationEntity;
import com.example.system.entity.UserEntity;
import com.example.system.entity.UserTaskLinkEntity;
import com.example.system.mapper.DepartmentMapper;
import com.example.system.mapper.TaskAllocationMapper;
import com.example.system.mapper.UserMapper;
import com.example.system.mapper.UserTaskLinkMapper;
import com.example.system.service.DepartmentService;
import com.example.system.service.UserService;
import com.example.system.util.JwtTokenUtil;
import com.example.system.util.MD5;
import com.example.system.util.RedisUtil;
import com.example.system.util.UUIDUtil;
import org.apache.catalina.User;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserServiceImpl extends ServiceImpl<UserMapper, UserEntity> implements UserService {

    @Autowired
    private TaskAllocationMapper taskAllocationMapper;

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private DepartmentMapper departmentMapper;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserTaskLinkMapper userTaskLinkMapper;

    @Override
    public void addUser(byte gender, String name, Long departmentId) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("工号不能为空");
        }

        QueryWrapper<DepartmentEntity> departmentQueryWrapper = new QueryWrapper<>();
        departmentQueryWrapper.eq("id", departmentId);
        DepartmentEntity departmentEntity = departmentService.getOne(departmentQueryWrapper);
        if (departmentEntity == null) {
            throw new IllegalArgumentException("不存在id为：" + departmentId + "的部门信息");
        }
        UserEntity user = new UserEntity();
        user.setId(UUIDUtil.uuid());
        user.setIdentity((byte) 1);
        user.setGender(gender);
        user.setName(name);
        user.setJobNumber(generateJobNumber());
        user.setPassword(MD5.encrypt("0123456789"));
        user.setDepartment(departmentId);
        user.setTaskAmount(0);
        save(user);
    }

    @Override
    public String adminLogin(String username, String password) {
        if (username == null || username.isEmpty()) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("密码不能为空");
        }
        QueryWrapper<UserEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("name", username);
        UserEntity userEntity = this.getOne(queryWrapper);
        if (userEntity == null) {
            throw new IllegalArgumentException("不存在用户名为：" + username + "的管理员");
        }

        if (userEntity.getIdentity() == 1) {
            throw new IllegalArgumentException("请在员工端登录");
        }

//        if (!MD5.encrypt(password).equals(userEntity.getPassword())) {
//            throw new IllegalArgumentException("密码错误");
//        }
        String token = jwtTokenUtil.generateToken(userEntity.getJobNumber());
        String key = "token:" + userEntity.getJobNumber();
        redisUtil.setWithExpire(key, token, 7, TimeUnit.DAYS);
        return token;
    }

    @Override
    public Map<String, Object> employeeLogin(String jobNumber, String password) {
        if (jobNumber == null || jobNumber.isEmpty()){
            throw new IllegalArgumentException("工号不能为空");
        }
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("密码不能为空");
        }
        QueryWrapper<UserEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("job_number", jobNumber);
        UserEntity userEntity = this.getOne(queryWrapper);

        if (userEntity == null) {
            throw new IllegalArgumentException("不存在工号为：" + jobNumber + "的员工");
        }

        if (userEntity.getIdentity() == 0) {
            throw new IllegalArgumentException("请在管理端登录");
        }

//        if (!MD5.encrypt(password).equals(userEntity.getPassword())) {
//            throw new IllegalArgumentException("密码错误");
//        }
        Map<String, Object> map = new HashMap<>();
        UserDTO userDTO = new UserDTO();
        BeanUtils.copyProperties(userEntity, userDTO);
        Byte gender = userEntity.getGender();
        if (gender == 0) {
            userDTO.setGender("0");
        } else {
            userDTO.setGender("1");
        }
        map.put("user", userDTO);
        String token = jwtTokenUtil.generateToken(jobNumber);
        String key = "token:" + jobNumber;
        redisUtil.setWithExpire(key, token, 7, TimeUnit.DAYS);
        map.put("token", token);
        return map;
    }

    @Override
    public UserDTO getUserById(Long id) {
        UserEntity userEntity = this.getById(id);
        if (userEntity == null) {
            throw new IllegalArgumentException("不存在id为：" + id + "的员工");
        }
        UserDTO userDTO = new UserDTO();
        BeanUtils.copyProperties(userEntity, userDTO);
        return userDTO;
    }

    @Override
    public UserDTO getUserByJobNumber(String jobNumber) {
        QueryWrapper<UserEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("job_number", jobNumber);
        UserEntity userEntity = this.getOne(queryWrapper);
        if (userEntity == null) {
            return null;
        }
        UserDTO userDTO = new UserDTO();
        BeanUtils.copyProperties(userEntity, userDTO);
        return userDTO;
    }

    @Override
    public void importEmployee(MultipartFile file) {
        try {
            Workbook workbook = WorkbookFactory.create(file.getInputStream());
            Sheet sheet = workbook.getSheetAt(0); // Excel中数据在第一个sheet

            Iterator<Row> rowIterator = sheet.iterator();
            // 跳过表头行
            if (rowIterator.hasNext()) {
                rowIterator.next();
            }

            List<UserEntity> users = new ArrayList<>();

            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                UserEntity user = new UserEntity();
                user.setId(UUIDUtil.uuid());
                user.setIdentity((byte) 1); // 员工身份
                String gender = row.getCell(0).getStringCellValue();// 第一列为性别
                if (gender.equals("男")) {
                    user.setGender((byte) 1);
                } else {
                    user.setGender((byte) 0);
                }
                user.setName(row.getCell(1).getStringCellValue()); // 第二列为姓名
                user.setPassword(MD5.encrypt("0123456789"));
                user.setJobNumber(generateJobNumber());
                String departmentName = row.getCell(2).getStringCellValue();// 第三列为部门名称
                Long departmentId = departmentMapper.selectByName(departmentName); // 查询部门id
                if (departmentId == null) {
                    throw new IllegalArgumentException("不存在部门名称为：" + departmentName + "的部门");
                }
                user.setDepartment(departmentId);
                user.setTaskAmount(0); // 任务数量初始化为0
                users.add(user);
            }

            // 保存员工信息到数据库
            saveBatch(users);

            workbook.close();
        } catch (IOException e) {
            e.printStackTrace();
            throw new IllegalArgumentException("导入失败");
        }
    }

    @Override
    public List<UserDTO> getAllEmployee() {
        List<UserEntity> userEntities = this.list(new QueryWrapper<UserEntity>().eq("identity", (byte) 1));
        List<UserDTO> userDTOS = new ArrayList<>();
        for (UserEntity userEntity : userEntities) {
            UserDTO userDTO = new UserDTO();
            BeanUtils.copyProperties(userEntity, userDTO);
            if (userEntity.getGender() == 1) {
                userDTO.setGender("男");
            } else {
                userDTO.setGender("女");
            }
            userDTO.setDepartment(userEntity.getDepartment());
            userDTOS.add(userDTO);
        }
        return userDTOS;
    }

    @Override
    public void updateEmployee(Long employeeId, String gender, Long department) {
        UserEntity byId = getById(employeeId);
        if ("男".equals(gender)) {
            byId.setGender((byte) 1);
        } else {
            byId.setGender((byte) 0);
        }
        byId.setDepartment(department);
        updateById(byId);
    }

    @Override
    public Map<String, List<UserDTO>> getUsersByFiledTaskId(Long taskId) {
        QueryWrapper<TaskAllocationEntity> queryWrapper = new QueryWrapper<>();

        Map<String, List<UserDTO>> map= new HashMap<>();

        queryWrapper.eq("task_id", taskId);
        queryWrapper.eq("status", 0);
        List<TaskAllocationEntity> taskAllocationEntities0 = taskAllocationMapper.selectList(queryWrapper);
        List<Long> userIds0 = taskAllocationEntities0.stream().map(TaskAllocationEntity::getUserId).collect(Collectors.toList());
        if (userIds0.size() != 0) {
            List<UserEntity> userEntities = listByIds(userIds0);
            List<UserDTO> status0 =  userEntities.stream().map(entity -> {
                UserDTO user = new UserDTO();
                BeanUtils.copyProperties(entity, user);
                if (entity.getGender() == 1) {
                    user.setGender("男");
                } else {
                    user.setGender("女");
                }
                return user;
            }).collect(Collectors.toList());
            map.put("0", status0);
        } else {
            map.put("0", null);
        }
        queryWrapper.clear();

        QueryWrapper<UserTaskLinkEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("task_id", taskId);
        List<UserTaskLinkEntity> userTaskLinkEntities = userTaskLinkMapper.selectList(wrapper);
        List<Long> userIds1 = userTaskLinkEntities.stream().map(UserTaskLinkEntity::getUserId).collect(Collectors.toList());

//        queryWrapper.eq("task_id", taskId);
//        queryWrapper.eq("status", 1);
//        List<TaskAllocationEntity> taskAllocationEntities1 = taskAllocationMapper.selectList(queryWrapper);
//        List<Long> userIds1 = taskAllocationEntities1.stream().map(TaskAllocationEntity::getUserId).collect(Collectors.toList());
        if (userIds1.size() != 0) {
            List<UserEntity> userEntities = listByIds(userIds1);
            List<UserDTO> status1 =  userEntities.stream().map(entity -> {
                UserDTO user = new UserDTO();
                BeanUtils.copyProperties(entity, user);
                if (entity.getGender() == 1) {
                    user.setGender("男");
                } else {
                    user.setGender("女");
                }
                return user;
            }).collect(Collectors.toList());
            map.put("1", status1);
        } else {
            map.put("1", null);
        }
        queryWrapper.clear();

        queryWrapper.eq("task_id", taskId);
        queryWrapper.eq("status", 2);
        List<TaskAllocationEntity> taskAllocationEntities2 = taskAllocationMapper.selectList(queryWrapper);
        List<Long> userIds2 = taskAllocationEntities2.stream().map(TaskAllocationEntity::getUserId).collect(Collectors.toList());
        if (userIds2.size() != 0) {
            List<UserEntity> userEntities = listByIds(userIds2);
            List<UserDTO> status2 =  userEntities.stream().map(entity -> {
                UserDTO user = new UserDTO();
                BeanUtils.copyProperties(entity, user);
                if (entity.getGender() == 1) {
                    user.setGender("男");
                } else {
                    user.setGender("女");
                }
                return user;
            }).collect(Collectors.toList());
            map.put("2", status2);
        } else {
            map.put("2", null);
        }

        return map;

    }

    @Override
    public void deleteOneById(Long id) {
        removeById(id);
    }

    @Override
    public void deleteListByIds(List<String> ids) {
//        List<Long> idList = JSON.parseArray(ids, Long.class);
        removeByIds(ids);
    }

    @Override
    public void updatePassword(Long id, String password) {
        UserEntity user = getById(id);
        user.setPassword(MD5.encrypt(password));
        updateById(user);
    }

    private String generateJobNumber() {
        Random random = new Random();
        StringBuilder jobNumber = new StringBuilder();
        int count;
        do {
            for (int i = 0; i < 8; i++) {
                jobNumber.append(random.nextInt(10)); // 生成0到9之间的随机数
            }
            count = userMapper.selectByJobNumber(jobNumber.toString()); // 查询数据库中是否存在该工号
        } while (count > 0); // 如果存在，则重新生成

        return jobNumber.toString();
    }

}
