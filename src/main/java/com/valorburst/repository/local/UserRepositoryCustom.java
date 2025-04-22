package com.valorburst.repository.local;

import java.util.List;

import com.valorburst.dto.MainDataDto;

public interface UserRepositoryCustom {

    List<MainDataDto> findAllMainData();
}
