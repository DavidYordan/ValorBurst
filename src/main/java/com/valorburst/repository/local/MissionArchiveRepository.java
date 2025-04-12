package com.valorburst.repository.local;

import com.valorburst.model.local.MissionArchive;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MissionArchiveRepository extends JpaRepository<MissionArchive, Integer> {

    @Query(value = """
            select *
            from mission_archive
            where user_id = :userId
            """, nativeQuery = true)
    List<MissionArchive> findAllByUserId(Integer userId);
}
