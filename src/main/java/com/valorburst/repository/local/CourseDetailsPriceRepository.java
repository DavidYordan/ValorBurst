package com.valorburst.repository.local;

import com.valorburst.model.local.CourseDetailsPrice;
import org.springframework.data.jpa.repository.JpaRepository;


public interface CourseDetailsPriceRepository extends JpaRepository<CourseDetailsPrice, Integer> {

    void deleteByIdGreaterThan(Integer id);
}
