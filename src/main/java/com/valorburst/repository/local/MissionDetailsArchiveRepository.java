package com.valorburst.repository.local;

import com.valorburst.model.local.MissionDetailsArchive;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MissionDetailsArchiveRepository extends JpaRepository<MissionDetailsArchive, Integer> {

    // 按mission_id查询money之和
    @Query(value = """
        SELECT SUM(money)
        FROM mission_details_archive
        WHERE mission_id = :missionId
        """, nativeQuery = true)
    BigDecimal sumMoneyByMissionId(Integer missionId);

    // 按mission_id及今日日期查询money之和
    @Query(value = """
        SELECT SUM(money)
        FROM mission_details_archive
        WHERE mission_id = :missionId
        AND execute_time >= CURDATE()
        """, nativeQuery = true)
    BigDecimal sumMoneyByMissionIdAndToday(Integer missionId);

    // 按mission_id统计type为0的数量
    @Query(value = """
        SELECT COUNT(*)
        FROM mission_details_archive
        WHERE mission_id = :missionId
        AND type = 0
        """, nativeQuery = true)
    Integer countTypeZeroByMissionId(Integer missionId);

    @Query(value = """
        SELECT MAX(execute_time)
        FROM mission_details_archive
        WHERE mission_id = :missionId
        """, nativeQuery = true)
    LocalDateTime findMaxExecuteTimeByMissionId(Integer missionId);

    @Query(value = """
        SELECT MAX(execute_time)
        FROM mission_details_archive
        WHERE mission_id = :missionId
        AND continuous > 0
        """, nativeQuery = true)
    LocalDateTime findMaxExecuteTimeByMissionIdAndContinuous(Integer missionId);
}
