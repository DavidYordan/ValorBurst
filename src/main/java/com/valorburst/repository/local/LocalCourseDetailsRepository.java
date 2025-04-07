package com.valorburst.repository.local;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.valorburst.model.local.LocalCourseDetails;

public interface LocalCourseDetailsRepository extends JpaRepository<LocalCourseDetails, Integer> {

    @Query("SELECT MAX(update_time) FROM course_details")
    Instant findMaxUpdateTime();

    @Query(value = """
        SELECT DISTINCT price FROM course_details
        WHERE is_price = 1
        """, nativeQuery = true)
    List<BigDecimal> findDistinctValidPrices();
}
