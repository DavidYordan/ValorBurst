package com.valorburst.repository.local;

import com.valorburst.model.local.Mission;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MissionRepository extends JpaRepository<Mission, Integer> {

    @Query("""
            select m from Mission m
            where m.executeTime is null
            and m.status = true
    """)
    List<Mission> findAllNeedInit();
}
