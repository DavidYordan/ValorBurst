package com.valorburst.repository.local;

import com.valorburst.model.local.Mission;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MissionRepository extends JpaRepository<Mission, Integer> {

    @Query(value = """
            select *
            from mission
            where status = 1
            """, nativeQuery = true)
    List<Mission> findMissions();

    @Query(value = """
            select *
            from mission
            where user_id = :userId
            """, nativeQuery = true)
    List<Mission> findAllByUserId(@Param("userId") Integer userId);

    @Query(value = """
        select *
        from mission
        where status = 1 and user_id in (:userIds)
        """, nativeQuery = true)
    List<Mission> findAllActiveByUserIds(@Param("userIds") List<Integer> userIds);
}
