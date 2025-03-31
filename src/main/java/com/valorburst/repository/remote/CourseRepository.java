package com.valorburst.repository.remote;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.valorburst.model.remote.Course;

public interface CourseRepository extends JpaRepository<Course, Integer> {

    @Query(value = """
        SELECT DISTINCT price FROM course
        WHERE is_delete = 0 AND is_price = 1 AND status = 1
        """, nativeQuery = true)
    List<Double> findDistinctValidPrices();
}
