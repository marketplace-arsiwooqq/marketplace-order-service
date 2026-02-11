package com.arsiwooqq.orderservice.service;

import com.arsiwooqq.orderservice.dto.UserResponse;

public interface UserDataService {

    UserResponse fetchUserData(String userId);
}
