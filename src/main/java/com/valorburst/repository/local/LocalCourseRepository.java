package com.valorburst.repository.local;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.valorburst.model.local.LocalCourse;

public interface LocalCourseRepository extends JpaRepository<LocalCourse, Integer> {

    @Query("SELECT MAX(update_time) FROM course")
    Instant findMaxUpdateTime();

    @Query(value = """
        SELECT DISTINCT price FROM course
        WHERE is_delete = 0 AND is_price = 1 AND status = 1
        """, nativeQuery = true)
    List<BigDecimal> findDistinctValidPrices();
}
