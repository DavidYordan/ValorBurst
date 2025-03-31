package com.valorburst.scheduler;

import com.valorburst.model.local.CourseDetailsPrice;
import com.valorburst.model.local.CoursePrice;
import com.valorburst.model.local.LocalVipDetails;
import com.valorburst.model.remote.RemoteVipDetails;
import com.valorburst.repository.local.CourseDetailsPriceRepository;
import com.valorburst.repository.local.CoursePriceRepository;
import com.valorburst.repository.local.LocalVipDetailsRepository;
import com.valorburst.repository.remote.CourseDetailsRepository;
import com.valorburst.repository.remote.CourseRepository;
import com.valorburst.repository.remote.RemoteVipDetailsRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSyncScheduler {

    private final CoursePriceRepository coursePriceRepository;
    private final CourseDetailsPriceRepository courseDetailsPriceRepository;
    private final LocalVipDetailsRepository localVipDetailsRepository;

    private final CourseRepository remoteCourseRepository;
    private final CourseDetailsRepository courseDetailsRepository;
    private final RemoteVipDetailsRepository remoteVipDetailsRepository;

    private final Executor missionExecutor;

    /**
     * 每隔 30 分钟执行一次同步任务
     */
    @Scheduled(fixedRate = 30 * 60 * 1000)
    @Transactional(transactionManager = "localTransactionManager")
    public void syncAllData() {
        log.info("开始同步所有数据...");
        try {
            // syncVipDetails();
            // syncCoursePrices();
            // syncCourseDetailsPrices();
            missionExecutor.execute(this::syncVipDetails);
            missionExecutor.execute(this::syncCoursePrices);
            missionExecutor.execute(this::syncCourseDetailsPrices);
        } catch (Exception e) {
            log.error("数据同步过程中发生异常: ", e);
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

    private void syncCoursePrices() {
        log.info("开始同步 CoursePrices 数据...");

        try {
            List<Double> remoteCoursePrices = remoteCourseRepository.findDistinctValidPrices();

            List<CoursePrice> toSave = new ArrayList<>();
            for (int i = 0; i < remoteCoursePrices.size(); i++) {
                Double price = remoteCoursePrices.get(i);
                if (price == null) continue;

                toSave.add(CoursePrice.builder()
                        .id(i + 1)
                        .price(price)
                        .build());
            }

            coursePriceRepository.saveAll(toSave);

            int maxId = remoteCoursePrices.size();
            coursePriceRepository.deleteByIdGreaterThan(maxId);

            log.info("同步 CoursePrices 数量: {}", remoteCoursePrices.size());
        } catch (Exception e) {
            log.error("同步 CoursePrices 数据时发生异常: ", e);
        }
    }

    private void syncCourseDetailsPrices() {
        log.info("开始同步 CourseDetailsPrices 数据...");

        try {
            List<Double> remoteCourseDetailsPrices = courseDetailsRepository.findDistinctValidPrices();

            List<CourseDetailsPrice> toSave = new ArrayList<>();
            for (int i = 0; i < remoteCourseDetailsPrices.size(); i++) {
                Double price = remoteCourseDetailsPrices.get(i);
                if (price == null) continue;

                toSave.add(CourseDetailsPrice.builder()
                        .id(i + 1)
                        .price(price)
                        .build());
            }

            courseDetailsPriceRepository.saveAll(toSave);

            int maxId = remoteCourseDetailsPrices.size();
            courseDetailsPriceRepository.deleteByIdGreaterThan(maxId);

            log.info("同步 CourseDetailsPrices 数量: {}", remoteCourseDetailsPrices.size());
        } catch (Exception e) {
            log.error("同步 CourseDetailsPrices 数据时发生异常: ", e);
        }
    }
}
