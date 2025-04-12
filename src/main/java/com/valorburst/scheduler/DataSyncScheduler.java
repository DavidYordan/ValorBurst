package com.valorburst.scheduler;

import com.valorburst.model.local.LocalCourse;
import com.valorburst.model.local.LocalCourseDetails;
import com.valorburst.model.local.LocalCommonInfo;
import com.valorburst.model.local.LocalVipDetails;
import com.valorburst.model.remote.RemoteCourse;
import com.valorburst.model.remote.RemoteCourseDetails;
import com.valorburst.model.remote.RemoteCommonInfo;
import com.valorburst.model.remote.RemoteVipDetails;
import com.valorburst.repository.local.LocalCourseDetailsRepository;
import com.valorburst.repository.local.LocalCourseRepository;
import com.valorburst.repository.local.LocalCommonInfoRepository;
import com.valorburst.repository.local.LocalVipDetailsRepository;
import com.valorburst.repository.remote.RemoteCommonInfoRepository;
import com.valorburst.repository.remote.RemoteCourseDetailsRepository;
import com.valorburst.repository.remote.RemoteCourseRepository;
import com.valorburst.repository.remote.RemoteVipDetailsRepository;
import com.valorburst.service.UserService;
import com.valorburst.util.DedupingExecutor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSyncScheduler {

    private static final int BATCHSIZE = 1000; // 每次查询的批量大小

    private final LocalCommonInfoRepository localCommonInfoRepository;
    private final LocalCourseRepository localCourseRepository;
    private final LocalCourseDetailsRepository localCourseDetailsRepository;
    private final LocalVipDetailsRepository localVipDetailsRepository;
    private final RemoteCourseRepository remoteCourseRepository;
    private final RemoteCourseDetailsRepository courseDetailsRepository;
    private final RemoteCommonInfoRepository remoteCommonInfoRepository;
    private final RemoteVipDetailsRepository remoteVipDetailsRepository;

    private final DedupingExecutor dedupingExecutor;
    private final UserService usersService;

    /**
     * 每隔 30 分钟执行一次同步任务
     */
    @Scheduled(fixedRate = 30 * 60 * 1000)
    @Transactional(transactionManager = "localTransactionManager")
    public void syncAllData() {
        log.info("开始同步所有数据...");
        try {
            dedupingExecutor.execute(this::syncCommonInfo, "commonInfo");
            dedupingExecutor.execute(this::syncCourse, "course");
            dedupingExecutor.execute(this::syncCourseDetails, "courseDetails");
            dedupingExecutor.execute(this::syncVipDetails, "vipDetails");
        } catch (Exception e) {
            log.error("数据同步过程中发生异常: ", e);
        }
    }

    /**
     * 每隔 5 分钟执行一次用户数据同步任务
     */
    @Scheduled(fixedRate = 5 * 60 * 1000)
    @Transactional(transactionManager = "localTransactionManager")
    public void syncUser() {
        log.info("开始同步用户数据...");
        try {
            dedupingExecutor.execute(usersService::syncAllUsers, "user");
        } catch (Exception e) {
            log.error("用户数据同步过程中发生异常: ", e);
        }
    }

    private void syncCommonInfo() {
        log.info("开始同步 CommonInfo 数据...");

        try {
            // 只查询[420, 421]
            List<Integer> remoteCommonInfoIds = Arrays.asList(420, 421);

            List<RemoteCommonInfo> remoteCommonInfos = remoteCommonInfoRepository.findByIdIn(remoteCommonInfoIds);

            if (!remoteCommonInfos.isEmpty()) {
                localCommonInfoRepository.saveAll(remoteCommonInfos.stream()
                        .map(LocalCommonInfo::fromRemote)
                        .collect(Collectors.toList()));

                log.info("同步 CommonInfo 数量: {}", remoteCommonInfos.size());
            }

        } catch (Exception e) {
            log.error("同步 CommonInfo 数据时发生异常: ", e);
        }
    }

    private void syncCourse() {
        log.info("开始同步 Course 数据...");
    
        try {
            LocalDateTime lastUpdateTime = localCourseRepository.findMaxUpdateTime();
            if (lastUpdateTime == null) {
                lastUpdateTime = LocalDateTime.of(1970, 1, 1, 0, 0, 0);
                log.debug("Course 本地数据库的最新更新时间为空，设置为默认值: {}", lastUpdateTime);
            } else {
                log.debug("Course 本地数据库的最新更新时间: {}", lastUpdateTime);
            }
    
            int page = 0;
            int total = 0;
    
            while (true) {
                Page<RemoteCourse> pageResult = remoteCourseRepository.findByUpdateTimeAfter(
                        lastUpdateTime,
                        PageRequest.of(page, BATCHSIZE, Sort.by("updateTime").ascending().and(Sort.by("courseId").ascending()))
                );
    
                List<RemoteCourse> content = pageResult.getContent();
                if (content.isEmpty()) break;
    
                localCourseRepository.saveAll(content.stream()
                        .map(LocalCourse::fromRemote)
                        .collect(Collectors.toList()));
    
                log.info("已保存第 {} 页 {} 条 Course 更新记录到本地数据库", page + 1, content.size());
                total += content.size();
    
                if (!pageResult.hasNext()) break;
                page++;
            }
    
            log.info("同步 Course 总数量: {}", total);
        } catch (Exception e) {
            log.error("同步 Course 数据时发生异常: ", e);
        }
    }

    private void syncCourseDetails() {
        log.info("开始同步 CourseDetails 数据...");
    
        try {
            LocalDateTime lastUpdateTime = localCourseDetailsRepository.findMaxUpdateTime();
            if (lastUpdateTime == null) {
                lastUpdateTime = LocalDateTime.of(1970, 1, 1, 0, 0, 0);
                log.debug("CourseDetails 本地数据库的最新更新时间为空，设置为默认值: {}", lastUpdateTime);
            } else {
                log.debug("CourseDetails 本地数据库的最新更新时间: {}", lastUpdateTime);
            }
    
            int page = 0;
            int total = 0;
    
            while (true) {
                Page<RemoteCourseDetails> pageResult = courseDetailsRepository.findByUpdateTimeAfter(
                        lastUpdateTime,
                        PageRequest.of(page, BATCHSIZE, Sort.by("updateTime").ascending().and(Sort.by("courseDetailsId").ascending()))
                );
    
                List<RemoteCourseDetails> content = pageResult.getContent();
                if (content.isEmpty()) break;
    
                localCourseDetailsRepository.saveAll(content.stream()
                        .map(LocalCourseDetails::fromRemote)
                        .collect(Collectors.toList()));
    
                log.info("已保存第 {} 页 {} 条 CourseDetails 更新记录到本地数据库", page + 1, content.size());
                total += content.size();
    
                if (!pageResult.hasNext()) break;
                page++;
            }
    
            log.info("同步 CourseDetails 总数量: {}", total);
        } catch (Exception e) {
            log.error("同步 CourseDetails 数据时发生异常: ", e);
        }
    }

    private void syncVipDetails() {
        log.info("开始同步 VipDetails 数据...");

        try {
            List<RemoteVipDetails> remoteVipDetails = remoteVipDetailsRepository.findAll();
            if (!remoteVipDetails.isEmpty()) {

                localVipDetailsRepository.saveAll(remoteVipDetails.stream()
                        .map(LocalVipDetails::fromRemote)
                        .collect(Collectors.toList()));

                log.info("同步 VipDetails 数量: {}", remoteVipDetails.size());
            }

        } catch (Exception e) {
            log.error("同步 VipDetails 数据时发生异常: ", e);
        }
    }
}
