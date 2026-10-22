package com.milaavo.service;

import com.milaavo.dto.LoginRequest;
import com.milaavo.dto.LoginResponse;
import com.milaavo.dto.RegisterRequest;
import com.milaavo.dto.RegisterResponse;

public interface UserService {

    RegisterResponse register(RegisterRequest request);

    LoginResponse login(LoginRequest request);
}
