package com.valorburst.repository.remote;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import com.valorburst.model.remote.Invite;

import java.util.List;

/**
 * 远程数据库的 Invite Repository
 */
public interface InviteRepository extends JpaRepository<Invite, Integer> {

    /**
     * 查询远程数据库中所有 id 大于指定值的记录
     *
     * @param id 指定的最小 id
     * @return 符合条件的 Invite 列表
     */
    List<Invite> findByIdGreaterThan(Integer id, PageRequest pageRequest);
}
