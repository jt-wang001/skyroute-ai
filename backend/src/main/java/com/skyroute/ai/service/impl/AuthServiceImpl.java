package com.skyroute.ai.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.skyroute.ai.common.JwtUtil;
import com.skyroute.ai.common.UserContext;
import com.skyroute.ai.common.exception.BusinessException;
import com.skyroute.ai.dto.LoginDTO;
import com.skyroute.ai.dto.RegisterDTO;
import com.skyroute.ai.entity.UserEntity;
import com.skyroute.ai.mapper.UserMapper;
import com.skyroute.ai.service.AuthService;
import com.skyroute.ai.vo.AuthVO;
import com.skyroute.ai.vo.UserProfileVO;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthServiceImpl(UserMapper userMapper, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Override
    @Transactional
    public Long register(RegisterDTO registerDTO) {
        String username = registerDTO.username().trim();
        Long count = userMapper.selectCount(Wrappers.<UserEntity>lambdaQuery()
                .eq(UserEntity::getUsername, username));
        if (count > 0) {
            throw new BusinessException(409, "用户名已存在");
        }

        UserEntity user = new UserEntity();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(registerDTO.password()));
        user.setNickname(registerDTO.nickname().trim());

        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(409, "用户名已存在");
        }
        return user.getId();
    }

    @Override
    public AuthVO login(LoginDTO loginDTO) {
        UserEntity user = userMapper.selectOne(Wrappers.<UserEntity>lambdaQuery()
                .eq(UserEntity::getUsername, loginDTO.username().trim()));
        if (user == null || !passwordEncoder.matches(loginDTO.password(), user.getPassword())) {
            throw new BusinessException(401, "用户名或密码错误");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        return new AuthVO(token, user.getId(), user.getNickname());
    }

    @Override
    public UserProfileVO getCurrentUserProfile() {
        UserEntity user = userMapper.selectById(UserContext.requireUserId());
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        return new UserProfileVO(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getCreateTime());
    }
}
