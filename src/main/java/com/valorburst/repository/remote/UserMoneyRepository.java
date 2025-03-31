package com.valorburst.repository.remote;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import com.valorburst.model.remote.UserMoney;

import java.util.List;

public interface UserMoneyRepository extends JpaRepository<UserMoney, Long> {
    
    /**
     * 根据指定的 userIds 批量查询 UserMoney 记录
     * @param userIds 用户ID列表
     * @return 满足条件的 UserMoney 列表
     */
    List<UserMoney> findByUserIdIn(List<Integer> userIds, PageRequest pageRequest);
}
