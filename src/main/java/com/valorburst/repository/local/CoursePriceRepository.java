package com.valorburst.repository.local;

import com.valorburst.model.local.CoursePrice;
import org.springframework.data.jpa.repository.JpaRepository;


public interface CoursePriceRepository extends JpaRepository<CoursePrice, Integer> {

    void deleteByIdGreaterThan(Integer id);
}
