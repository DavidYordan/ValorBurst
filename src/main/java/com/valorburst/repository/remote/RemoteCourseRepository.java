package com.valorburst.repository.remote;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import com.valorburst.model.remote.RemoteCourse;

public interface RemoteCourseRepository extends JpaRepository<RemoteCourse, Integer> {

    Page<RemoteCourse> findByUpdateTimeAfter(LocalDateTime updateTime, PageRequest pageRequest);
}
