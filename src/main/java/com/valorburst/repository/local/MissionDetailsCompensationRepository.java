package com.valorburst.repository.local;

import com.valorburst.model.local.MissionDetailsCompensation;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MissionDetailsCompensationRepository extends JpaRepository<MissionDetailsCompensation, Integer> {

    Integer countByUserId(Integer userId);
}
