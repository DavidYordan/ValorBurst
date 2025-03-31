package com.valorburst.util;

import com.valorburst.dto.RecordStatsDto;
import com.valorburst.model.local.MissionDetails;
import com.valorburst.dto.AgentDetailDto;
import com.valorburst.dto.DailyStatsSummaryDto;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 静态工具类：拼接 /details、每日汇总等指令的文本
 */
public class TelegramMessageBuilder {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final String[] RANK_EMOJIS = {
        "1️⃣", "2️⃣", "3️⃣", "4️⃣", "5️⃣",
        "6️⃣", "7️⃣", "8️⃣", "9️⃣", "🔟"
    };

    /**
     * 拼装 /details 的文本
     */
    public static String buildDetails(LocalDate start, LocalDate end, RecordStatsDto stats) {
        StringBuilder sb = new StringBuilder();

        if (start.equals(end)) {
            // 单日
            sb.append("📆 查询日期：").append(start.format(DATE_FMT)).append("\n\n");
        } else {
            // 区间
            sb.append("📆 查询区间：")
              .append(start.format(DATE_FMT))
              .append(" ~ ")
              .append(end.format(DATE_FMT))
              .append("\n\n");
        }

        sb.append("总视频数：").append(stats.getTotalVideos()).append("条 | ")
          .append(stats.getTotalAgents()).append("人\n");
        sb.append("有效视频数：").append(stats.getValidVideos()).append("条 | ")
          .append(stats.getValidAgents()).append("人\n");
        sb.append("结算视频数：").append(stats.getCandidatedVideos()).append("条 | ")
          .append(stats.getCandidatedAgents()).append("人 | ¥")
          .append(String.format("%.2f", stats.getCandidatedReward())).append("\n");
        sb.append("未结算视频数：").append(stats.getUnCandidateVideos()).append("条 | ")
          .append(stats.getUnCandidateAgents()).append("人 | ¥")
          .append(String.format("%.2f", stats.getUnCandidateReward())).append("\n\n");

        // 4) Top 10 用户
        if (stats.getAgentDetails() != null && !stats.getAgentDetails().isEmpty()) {
            sb.append("👥 用户收益排行（前十）：\n");
            int rank = 1;
            for (AgentDetailDto detail : stats.getAgentDetails()) {
                sb.append(RANK_EMOJIS[rank-1]).append(" ")
                  .append(detail.getAgentName()).append("（").append(detail.getMyBusinessId())
                  .append("）\n├─总视频数 ").append(detail.getTotalVideoCount()).append("条\n")
                  .append("├─有效视频数 ").append(detail.getValidVideoCount()).append("条\n")
                  .append("├─结算视频数 ").append(detail.getCandidatedVideoCount()).append("条 | ")
                  .append("¥").append(String.format("%.2f", detail.getCandidatedReward())).append("\n")
                  .append("├─未结算视频数 ").append(detail.getUnCandidateVideoCount()).append("条 | ")
                  .append("¥").append(String.format("%.2f", detail.getUnCandidateReward())).append("\n");
                rank++;
                if (rank > 10) {
                    break;
                }
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    /**
     * 发送每日汇总 /buildStatusReport 的拼装
     */
    public static String buildStatusReport(DailyStatsSummaryDto stats) {
        StringBuilder sb = new StringBuilder();
        sb.append("截止 ").append(LocalDate.now()).append(" 的发布概况：\n\n");

        sb.append("总视频数：").append(stats.getTotalVideos()).append("条 | ")
          .append(stats.getTotalAgents()).append("人\n");
        sb.append("有效视频数：").append(stats.getTotalValidVideos()).append("条 | ")
          .append(stats.getTotalValidAgents()).append("人\n");
        sb.append("结算视频数：").append(stats.getTotalCandidatedVideos()).append("条 | ")
          .append(stats.getTotalCandidatedAgents()).append("人 | ¥")
          .append(String.format("%.2f", stats.getTotalCandidatedReward())).append("\n");
        sb.append("未结算视频数：").append(stats.getTotalUnCandidateVideos()).append("条 | ")
          .append(stats.getTotalUnCandidateAgents()).append("人 | ¥")
          .append(String.format("%.2f", stats.getTotalUnCandidateReward())).append("\n\n");

        // 逐日输出
        for (RecordStatsDto ds : stats.getDailyStats()) {
            LocalDate d = ds.getFabuDate();

            sb.append(d).append("\n");
            sb.append("├─ 总视频数：").append(ds.getTotalVideos()).append("条 | ")
              .append(ds.getTotalAgents()).append("人\n");
            sb.append("├─ 有效视频数：").append(ds.getValidVideos()).append("条 | ")
              .append(ds.getValidAgents()).append("人\n");
            sb.append("├─ 结算视频数：").append(ds.getCandidatedVideos()).append("条 | ")
              .append(ds.getCandidatedAgents()).append("人 | ¥")
              .append(String.format("%.2f", ds.getCandidatedReward())).append(")\n");
            sb.append("├─ 未结算视频数：").append(ds.getUnCandidateVideos()).append("条 | ")
              .append(ds.getUnCandidateAgents()).append("人 | ¥")
              .append(String.format("%.2f", ds.getUnCandidateReward())).append("\n");
        }

        return sb.toString().trim();
    }

    /**
     * 构建超时警报
     */
    public static String buildTimeoutDetails(List<MissionDetails> details) {
        StringBuilder sb = new StringBuilder();
        sb.append("⚠️ 任务超时!（共").append(details.size()).append("条）\n\n");

        for (MissionDetails d : details) {
            sb.append(d.getMissionDetailsId()).append(" | ")
              .append(d.getType()).append(" | ")
              .append(d.getExecuteTime()).append("\n\n");
        }

        return sb.toString();
    }
}
