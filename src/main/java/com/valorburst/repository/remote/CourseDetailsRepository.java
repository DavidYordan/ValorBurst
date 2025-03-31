package com.valorburst.repository.remote;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.valorburst.model.remote.CourseDetails;

public interface CourseDetailsRepository extends JpaRepository<CourseDetails, Integer> {

    @Query(value = """
        SELECT DISTINCT price FROM course_details
        WHERE is_price = 1
        """, nativeQuery = true)
    List<Double> findDistinctValidPrices();
}
