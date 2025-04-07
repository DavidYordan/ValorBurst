package com.valorburst.repository.local;

import com.valorburst.model.local.MissionDetails;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MissionDetailsRepository extends JpaRepository<MissionDetails, Integer> {

    @Query(value = """
        SELECT md.* 
        FROM mission_details md
        JOIN mission m ON md.mission_id = m.mission_id
        WHERE md.execute_time < CURRENT_TIMESTAMP
        AND md.executing = false
        AND m.status = true
    """, nativeQuery = true)
    List<MissionDetails> findAllNeedExecute();

    @Query(value = """
        SELECT md.* 
        FROM mission_details md
        JOIN mission m ON md.mission_id = m.mission_id
        WHERE md.execute_time < :timeout
        AND m.status = true
    """, nativeQuery = true)
    List<MissionDetails> findAllTimeoutExecute(@Param("timeout") Instant timeout);

    @Modifying
    @Query(value = """
        UPDATE mission_details
        SET executing = true
        WHERE mission_details_id = :id AND executing = false
    """, nativeQuery = true)
    Integer tryMarkExecuting(@Param("id") Integer id);
        
}
