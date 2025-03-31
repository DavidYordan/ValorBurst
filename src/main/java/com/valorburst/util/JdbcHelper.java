package com.valorburst.util;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

@Component
@RequiredArgsConstructor
public class JdbcHelper {

    private final DataSource remoteDataSource;

    public void executeInTransaction(String sql) {
        Connection conn = null;
        try {
            conn = remoteDataSource.getConnection();
            conn.setAutoCommit(false);
            conn.createStatement().execute(sql);
            conn.commit();
        } catch (Exception e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (Exception rollbackEx) {
                    e.addSuppressed(rollbackEx);
                }
            }
            throw new RuntimeException("远程数据库操作失败", e);
        } finally {
            try {
                if (conn != null) conn.close();
            } catch (Exception ignore) {}
        }
    }
}
