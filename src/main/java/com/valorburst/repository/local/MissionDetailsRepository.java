package com.valorburst.repository.local;

import com.valorburst.model.local.MissionDetails;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MissionDetailsRepository extends JpaRepository<MissionDetails, Integer> {

    @Query(value = """
        SELECT COUNT(*) 
        FROM mission_details
        WHERE mission_id = :missionId
    """, nativeQuery = true)
    Integer countByMissionId(@Param("missionId") Integer missionId);

    @Query(value = """
        SELECT COUNT(*) 
        FROM mission_details
        WHERE mission_id = :missionId
        AND type IN (11, 12)
        AND invitee_id IS NULL
    """, nativeQuery = true)
    Integer countSignalByMissionId(@Param("missionId") Integer missionId);

    @Query(value = """
        SELECT md.* 
        FROM mission_details md
        JOIN mission m ON md.mission_id = m.mission_id
        WHERE md.execute_time < CURRENT_TIMESTAMP
        AND m.status = 1
    """, nativeQuery = true)
    List<MissionDetails> findAllNeedExecute();

    @Query(value = """
        SELECT md.* 
        FROM mission_details md
        JOIN mission m ON md.mission_id = m.mission_id
        WHERE md.execute_time < :timeout
        AND m.status = 1
    """, nativeQuery = true)
    List<MissionDetails> findAllTimeoutExecute(@Param("timeout") LocalDateTime timeout);

    @Query(value = """
        SELECT SUM(money) 
        FROM mission_details
        WHERE mission_id = :missionId
    """, nativeQuery = true)
    BigDecimal sumMoneyByMissionId(@Param("missionId") Integer missionId);
}
