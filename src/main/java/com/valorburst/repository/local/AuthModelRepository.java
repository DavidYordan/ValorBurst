package com.valorburst.repository.local;

import org.springframework.data.jpa.repository.JpaRepository;

import com.valorburst.model.local.AuthModel;

public interface AuthModelRepository extends JpaRepository<AuthModel, Integer> {

    AuthModel findByUsername(String username);
    AuthModel findByMachineCode(String machineCode);
}
