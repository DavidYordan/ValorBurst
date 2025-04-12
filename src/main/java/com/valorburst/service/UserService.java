package com.valorburst.service;

import java.util.List;
import java.util.Optional;

import com.valorburst.model.local.User;

public interface UserService {
    void syncAllUsers();
    List<User> getAllUsers();
    Optional<User> updateUserByInput(String input);
}
