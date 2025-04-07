package com.valorburst.repository.remote;

import com.valorburst.model.remote.RemoteCommonInfo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RemoteCommonInfoRepository extends JpaRepository<RemoteCommonInfo, Integer> {
    
    List<RemoteCommonInfo> findByIdIn(List<Integer> ids);
}
