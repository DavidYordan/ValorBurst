package com.valorburst.util;

import com.valorburst.model.local.MissionDetails;

public class MissionDetailsBuilder {
    public static String buildJdbc(Integer type, MissionDetails details) {
        switch (type) {
            case 1:
                return buildJdbcVip1(details, 3);
            case 2:
                return buildJdbcVip1(details, 0);
            case 3:
                return buildJdbcVip1(details, 1);
            case 4:
                return buildJdbcVip1(details, 2);
            case 5:
                return buildJdbcVip2(details, 3);
            case 6:
                return buildJdbcVip2(details, 0);
            case 7:
                return buildJdbcVip2(details, 1);
            case 8:
                return buildJdbcVip2(details, 2);
            case 11:
                return buildJdbcSingle1(details);
            case 12:
                return buildJdbcSingle2(details);
            case 21:
                return buildJdbcAll1(details);
            case 22:
                return buildJdbcAll2(details);
            default:
                throw new IllegalArgumentException("不支持的任务类型: " + type);
        }
    }

    private static String buildJdbcVip1(MissionDetails details, int vipType) {
        // User user = User.builder()
        String sql1 = "INSERT INTO remote_table_1 (col1, col2) VALUES (?, ?)";
        String sql2 = "INSERT INTO remote_log (type, exec_time) VALUES (?, ?)";

        return sql1 + sql2;
    }

    private static String buildJdbcVip2(MissionDetails details, int vipType) {
        String sql1 = "INSERT INTO remote_table_1 (col1, col2) VALUES (?, ?)";
        String sql2 = "INSERT INTO remote_log (type, exec_time) VALUES (?, ?)";

        return sql1 + sql2;
    }

    private static String buildJdbcSingle1(MissionDetails details) {
        String sql1 = "INSERT INTO remote_table_1 (col1, col2) VALUES (?, ?)";
        String sql2 = "INSERT INTO remote_log (type, exec_time) VALUES (?, ?)";

        return sql1 + sql2;
    }

    private static String buildJdbcSingle2(MissionDetails details) {
        String sql1 = "INSERT INTO remote_table_1 (col1, col2) VALUES (?, ?)";
        String sql2 = "INSERT INTO remote_log (type, exec_time) VALUES (?, ?)";

        return sql1 + sql2;
    }

    private static String buildJdbcAll1(MissionDetails details) {
        String sql1 = "INSERT INTO remote_table_1 (col1, col2) VALUES (?, ?)";
        String sql2 = "INSERT INTO remote_log (type, exec_time) VALUES (?, ?)";

        return sql1 + sql2;
    }

    private static String buildJdbcAll2(MissionDetails details) {
        String sql1 = "INSERT INTO remote_table_1 (col1, col2) VALUES (?, ?)";
        String sql2 = "INSERT INTO remote_log (type, exec_time) VALUES (?, ?)";

        return sql1 + sql2;
    }
}
