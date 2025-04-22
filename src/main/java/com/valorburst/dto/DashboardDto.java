package com.valorburst.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardDto {
    
    // —— 用户统计
    private Integer totalUserCount;               // 总用户数
    private Long activeUserCount;              // 有正在执行任务的用户数
    private Long inactiveUserCount;            // 无正在执行任务的用户数

    // —— 任务收益汇总
    private Integer archiveMissionCount;          // 已归档的任务数
    private Integer totalMissionCount;             // 总任务数
    private Long activeMissionCount;            // 进行中的任务数
    private Long pauseMissionCount;             // 暂停中的任务数
    private BigDecimal totalMoney;             // 所有任务已执行总金额
    private BigDecimal totalTodayMoney;        // 所有任务今日已执行总金额

    // —— 任务分布
    private Map<Integer, Long> missionsByType;      // key=type,     value=任务数量

    // —— 补偿任务数
    private Integer compensationCount;

    // —— 预警数据
    private Long overdueMissionCount;              // executeTime < now 的任务数
    private Long usersOverTodayThreshold;          // 单用户今日收益 > 阀值 的用户数
    private Long usersOverExpectDailyThreshold;    // 单用户「日均预期收益」> 阀值 的用户数
}
