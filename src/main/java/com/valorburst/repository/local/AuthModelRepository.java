package com.valorburst.repository.local;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.valorburst.model.local.AuthModel;

public interface AuthModelRepository extends JpaRepository<AuthModel, Integer> {

    AuthModel findByUsername(String username);
    AuthModel findByMachineCode(String machineCode);

    @Query("SELECT a.tokenNeverExpire FROM AuthModel a WHERE a.username = ?1")
    Boolean findTokenNeverExpireByUsername(String username);
}
