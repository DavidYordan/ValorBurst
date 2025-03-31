package com.valorburst.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.function.Consumer;

@Component
@RequiredArgsConstructor
public class RemoteJdbcHelper {

    // 👇 指定远程数据源，避免用错
    @Qualifier("remoteDataSource")
    private final DataSource remoteDataSource;

    /**
     * 在远程数据库中执行一段事务性逻辑
     */
    public void executeInTransaction(Consumer<Connection> action) {
        try (Connection conn = remoteDataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                action.accept(conn);
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        } catch (Exception e) {
            throw new RuntimeException("远程数据库操作失败", e);
        }
    }
}
