package com.valorburst.util;

import jakarta.persistence.Column;
import jakarta.persistence.Table;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class JpaSqlBuilder {

    /**
     * 构建 INSERT 语句（拼接完整 SQL）
     */
    public static String buildInsertSql(Object entity) {
        Class<?> clazz = entity.getClass();
        String tableName = getTableName(clazz);

        List<String> columns = new ArrayList<>();
        List<Object> values = new ArrayList<>();

        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            Column column = field.getAnnotation(Column.class);
            if (column == null) continue;

            try {
                Object value = field.get(entity);
                if (value == null) continue;
                columns.add(column.name());
                values.add(value);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        return buildInsertSqlFromParts(tableName, columns, values);
    }

    /**
     * 构建 UPDATE 语句（基于主键更新其他字段）
     */
    public static String buildUpdateSql(Object entity, List<String> whereFieldNames) {
        Class<?> clazz = entity.getClass();
        String tableName = getTableName(clazz);

        List<String> setClauses = new ArrayList<>();
        List<String> whereClauses = new ArrayList<>();

        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            Column column = field.getAnnotation(Column.class);
            if (column == null) continue;

            try {
                Object value = field.get(entity);
                if (value == null) continue;

                String columnName = column.name();

                if (whereFieldNames.contains(field.getName())) {
                    whereClauses.add(columnName + " = " + formatSqlValue(value));
                } else {
                    setClauses.add(columnName + " = " + formatSqlValue(value));
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        if (whereClauses.isEmpty()) {
            throw new RuntimeException("未提供有效 WHERE 条件字段或字段值为空");
        }
    
        if (setClauses.isEmpty()) {
            throw new RuntimeException("没有需要更新的字段（除 WHERE 条件外）");
        }
    
        return "UPDATE " + tableName +
                " SET " + String.join(", ", setClauses) +
                " WHERE " + String.join(" AND ", whereClauses) + ";";
    }

    /**
     * 构建 INSERT SQL 字符串
     */
    private static String buildInsertSqlFromParts(String tableName, List<String> columns, List<Object> values) {
        StringBuilder sb = new StringBuilder();
        sb.append("INSERT INTO ").append(tableName)
                .append(" (").append(String.join(", ", columns)).append(") VALUES (");

        for (int i = 0; i < values.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(formatSqlValue(values.get(i)));
        }

        sb.append(");");
        return sb.toString();
    }

    /**
     * 格式化值到 SQL 字面量
     */
    private static String formatSqlValue(Object val) {
        if (val == null) {
            return "NULL";
        } else if (val instanceof Number) {
            return val.toString();
        } else if (val instanceof Instant) {
            return "'" + ((Instant) val).toString() + "'";
        } else {
            return "'" + val.toString().replace("'", "''") + "'";
        }
    }

    /**
     * 获取表名
     */
    private static String getTableName(Class<?> clazz) {
        Table table = clazz.getAnnotation(Table.class);
        return (table != null) ? table.name() : clazz.getSimpleName();
    }
}
