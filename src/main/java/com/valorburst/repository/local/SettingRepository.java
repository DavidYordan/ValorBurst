package com.valorburst.repository.local;

import com.valorburst.model.local.Setting;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SettingRepository extends JpaRepository<Setting, Integer> {
    
    @Query("SELECT s.value FROM Setting s WHERE s.key = :key")
    String findValueByKey(@Param("key") String key);
}
