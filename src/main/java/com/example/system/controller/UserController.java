package com.example.system.controller;

import com.example.system.dto.UserDTO;
import com.example.system.result.CommonResult;
import com.example.system.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 单独新增员工信息
     */
    @PostMapping("/addUser")
    public CommonResult<Void> addUser(@RequestBody UserDTO user) {
        byte n = 0;
        if ("男".equals(user.getGender())) {
            n = 1;
        }
        userService.addUser(n, user.getName(), user.getDepartment());
        return CommonResult.success();
    }

    /**
     * 管理员登录
     */
    @PutMapping("/adminLogin")
    public CommonResult<String> adminLogin(@RequestParam("username") String username, @RequestParam("password") String password) {
        String token = userService.adminLogin(username, password);
        return CommonResult.success(token);
    }

    /**
     * 员工登录
     */
    @PutMapping("/employeeLogin")
    public CommonResult<Map<String, Object>> employeeLogin(@RequestParam("jobNumber") String jobNumber, @RequestParam("password") String password) {
        Map<String, Object> map = userService.employeeLogin(jobNumber, password);
        return CommonResult.success(map);
    }

    @DeleteMapping("/deleteOneById/{id}")
    public CommonResult<Void> deleteOneById(@PathVariable("id") Long id) {
        userService.deleteOneById(id);
        return CommonResult.success();
    }

    @DeleteMapping("/deleteListByIds")
    public CommonResult<Void> deleteListByIds(@RequestBody List<String> ids) {
        userService.deleteListByIds(ids);
        return CommonResult.success();
    }

    /**
     * 根据id获取员工信息
     */
    @GetMapping("/getUserById/{id}")
    public CommonResult<UserDTO> getUserById(@PathVariable("id") Long id) {
        UserDTO user = userService.getUserById(id);
        return CommonResult.success(user);
    }

    @GetMapping("/getAllEmployee")
    public CommonResult<List<UserDTO>> getAllEmployee() {
        List<UserDTO> userList = userService.getAllEmployee();
        return CommonResult.success(userList);
    }

    /**
     * 根据工号获取员工信息
     */
    @GetMapping("/getUserByJobNumber/{jobNumber}")
    public CommonResult<UserDTO> getUserByJobNumber(@PathVariable("jobNumber") String jobNumber) {
        UserDTO user = userService.getUserByJobNumber(jobNumber);
        return CommonResult.success(user);
    }

    @PutMapping("/updateEmployee")
    public CommonResult<Void> updateEmployee(@RequestBody UserDTO userDTO) {
        userService.updateEmployee(userDTO.getId(), userDTO.getGender(), userDTO.getDepartment());
        return CommonResult.success();
    }

    @PutMapping("/updatePassword")
    public CommonResult<Void> updatePassword(@RequestParam("id") Long id, @RequestParam("password") String password) {
        userService.updatePassword(id, password);
        return CommonResult.success();
    }

    /**
     * 上传excel文件批量导入员工信息
     */
    @PostMapping("/importEmployee")
    public CommonResult<Void> importEmployee(@RequestParam("file") MultipartFile file) {
        userService.importEmployee(file);
        return CommonResult.success();
    }

    /**
     * 根据任务id获取员工信息
     */
    @GetMapping("/getUsersByFiledTaskId/{taskId}")
    public CommonResult<Map<String, List<UserDTO>>> getUsersByFiledTaskId(@PathVariable("taskId") Long taskId) {
        Map<String, List<UserDTO>> users = userService.getUsersByFiledTaskId(taskId);
        return CommonResult.success(users);
    }
}
