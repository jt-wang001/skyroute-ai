package com.skyroute.ai.service;

import com.skyroute.ai.dto.LoginDTO;
import com.skyroute.ai.dto.RegisterDTO;
import com.skyroute.ai.vo.AuthVO;
import com.skyroute.ai.vo.UserProfileVO;

public interface AuthService {

    Long register(RegisterDTO registerDTO);

    AuthVO login(LoginDTO loginDTO);

    UserProfileVO getCurrentUserProfile();
}
