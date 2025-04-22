package com.valorburst.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.valorburst.dto.DashboardDto;
import com.valorburst.dto.MainDataDto;
import com.valorburst.model.local.User;

public interface UserService {

    Map<String, DashboardDto> getDashboardByRegion();
    void syncAllUsers();
    List<User> getAllUsers();
    List<MainDataDto> getAllMainData();
    Optional<User> updateUserByInput(String input);
}
