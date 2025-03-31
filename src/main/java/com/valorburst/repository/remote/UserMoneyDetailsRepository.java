package com.valorburst.repository.remote;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import com.valorburst.model.remote.UserMoneyDetails;

import java.util.List;

/**
 * 远程数据库的 UserMoneyDetails Repository
 */
public interface UserMoneyDetailsRepository extends JpaRepository<UserMoneyDetails, Integer> {

    /**
     * 查询远程数据库中所有 id 大于指定值的记录
     *
     * @param id 指定的最小 id
     * @return 符合条件的 UserMoneyDetails 列表
     */
    List<UserMoneyDetails> findByIdGreaterThan(Integer id, PageRequest pageRequest);
}
