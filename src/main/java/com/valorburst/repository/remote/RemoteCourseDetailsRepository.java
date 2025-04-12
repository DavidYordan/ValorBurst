package com.valorburst.repository.remote;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import com.valorburst.model.remote.RemoteCourseDetails;

public interface RemoteCourseDetailsRepository extends JpaRepository<RemoteCourseDetails, Integer> {

    Page<RemoteCourseDetails> findByUpdateTimeAfter(LocalDateTime updateTime, PageRequest pageRequest);
}
