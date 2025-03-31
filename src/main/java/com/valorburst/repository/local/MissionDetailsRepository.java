package com.valorburst.repository.local;

import com.valorburst.model.local.MissionDetails;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MissionDetailsRepository extends JpaRepository<MissionDetails, Integer> {

    // findByExecuteTimeLessThan
    List<MissionDetails> findByExecuteTimeLessThan(LocalDateTime executeTime);

    // findByExecuteTimeLessThanEqual
    List<MissionDetails> findByExecuteTimeLessThanEqual(LocalDateTime executeTime);
}
