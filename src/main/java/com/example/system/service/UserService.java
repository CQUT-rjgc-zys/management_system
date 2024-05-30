package com.example.system.service;

import com.example.system.dto.UserDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface UserService {

    void addUser(byte gender, String name, Long departmentId);

    String adminLogin(String username, String password);

    Map<String, Object> employeeLogin(String jobNumber, String password);

    UserDTO getUserById(Long id);

    UserDTO getUserByJobNumber(String jobNumber);

    void importEmployee(MultipartFile file);

    List<UserDTO> getAllEmployee();

    void updateEmployee(Long employeeId, String gender, Long department);

    Map<String, List<UserDTO>> getUsersByFiledTaskId(Long taskId);

    void deleteOneById(Long id);

    void deleteListByIds(List<String> ids);

    void updatePassword(Long id, String password);
}
