package fr.eiffelbikecorp.bikeapi.service;

import fr.eiffelbikecorp.bikeapi.dto.UserLoginRequest;
import fr.eiffelbikecorp.bikeapi.dto.UserLoginResponse;
import fr.eiffelbikecorp.bikeapi.dto.UserRegisterRequest;
import fr.eiffelbikecorp.bikeapi.dto.UserResponse;

public interface UserService {
    UserResponse register(UserRegisterRequest request);

    UserLoginResponse login(UserLoginRequest request);
}
