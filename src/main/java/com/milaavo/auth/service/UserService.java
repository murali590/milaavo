package com.milaavo.auth.service;

import com.milaavo.auth.dto.LoginRequest;
import com.milaavo.auth.dto.LoginResponse;
import com.milaavo.auth.dto.RegisterRequest;
import com.milaavo.auth.dto.RegisterResponse;

public interface UserService {

    RegisterResponse register(RegisterRequest request);

    LoginResponse login(LoginRequest request);
}
