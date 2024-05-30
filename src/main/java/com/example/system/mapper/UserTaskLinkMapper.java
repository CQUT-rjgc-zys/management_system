package com.example.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.system.entity.DepartmentEntity;
import com.example.system.entity.UserTaskLinkEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserTaskLinkMapper extends BaseMapper<UserTaskLinkEntity> {

}
