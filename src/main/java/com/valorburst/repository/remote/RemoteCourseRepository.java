package com.valorburst.repository.remote;

import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import com.valorburst.model.remote.RemoteCourse;

public interface RemoteCourseRepository extends JpaRepository<RemoteCourse, Integer> {

    Page<RemoteCourse> findByUpdateTimeAfter(Instant updateTime, PageRequest pageRequest);
}
