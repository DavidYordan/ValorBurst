package com.valorburst.service;

import java.util.List;
import java.util.Optional;

import com.valorburst.model.local.User;

public interface UsersService {
    List<User> syncAllUsers();
    Optional<User> updateUserByInput(String input);
}
