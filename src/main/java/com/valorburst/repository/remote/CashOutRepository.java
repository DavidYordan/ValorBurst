package com.valorburst.repository.remote;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import com.valorburst.model.remote.CashOut;

import java.util.List;

/**
 * 远程数据库的 CashOut Repository
 */
public interface CashOutRepository extends JpaRepository<CashOut, Long> {

    /**
     * 查询远程数据库中所有 createAt 在指定时间之后的记录
     *
     * @param createAt 指定的时间
     * @return 符合条件的 CashOut 列表
     */
    List<CashOut> findByCreateAtAfter(String createAt, PageRequest pageRequest);

    /**
     * 查询远程数据库中所有 outAt 在指定时间之后的记录
     *
     * @param outAt 指定的时间
     * @return 符合条件的 CashOut 列表
     */
    List<CashOut> findByOutAtAfter(String outAt, PageRequest pageRequest);
}
