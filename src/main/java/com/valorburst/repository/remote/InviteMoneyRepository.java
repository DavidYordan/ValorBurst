package com.valorburst.repository.remote;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import com.valorburst.model.remote.InviteMoney;

import java.util.List;

public interface InviteMoneyRepository extends JpaRepository<InviteMoney, Long> {
    
    /**
     * 根据指定的 userIds 批量查询 InviteMoney 记录
     * @param userIds 用户ID列表
     * @return 满足条件的 InviteMoney 列表
     */
    List<InviteMoney> findByUserIdIn(List<Integer> userIds, PageRequest pageRequest);
}
