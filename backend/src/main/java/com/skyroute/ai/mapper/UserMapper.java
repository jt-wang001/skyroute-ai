package com.skyroute.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.skyroute.ai.entity.UserEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<UserEntity> {
}
