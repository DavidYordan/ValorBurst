package com.valorburst.service.impl;

import com.valorburst.dto.PircesResponseDto;
import com.valorburst.model.local.LocalVipDetails;
import com.valorburst.repository.local.LocalCourseDetailsRepository;
import com.valorburst.repository.local.LocalCourseRepository;
import com.valorburst.repository.local.LocalVipDetailsRepository;
import com.valorburst.service.PircesService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PircesServiceImpl implements PircesService {

    private final LocalCourseRepository localCourseRepository;
    private final LocalCourseDetailsRepository localCourseDetailsRepository;
    private final LocalVipDetailsRepository localVipDetailsRepository;

    public PircesResponseDto getPrices() {
        List<LocalVipDetails> localVipDetails = localVipDetailsRepository.findByLanguageType("en");
        List<BigDecimal> typeCourse = localCourseRepository.findDistinctValidPrices();
        List<BigDecimal> typeCourseDetails = localCourseDetailsRepository.findDistinctValidPrices();

        BigDecimal type1 = null;
        BigDecimal type2 = null;
        BigDecimal type3 = null;
        BigDecimal type4 = null;

        for (LocalVipDetails vip : localVipDetails) {
            switch (vip.getVipNameType()) {
                case 0 -> type2 = vip.getMoney();
                case 1 -> type3 = vip.getMoney();
                case 2 -> type4 = vip.getMoney();
                case 3 -> type1 = vip.getMoney();
            }
        }

        return PircesResponseDto.builder()
                .type1(type1)
                .type2(type2)
                .type3(type3)
                .type4(type4)
                .type5(type1)
                .type6(type2)
                .type7(type3)
                .type8(type4)
                .type11(typeCourseDetails)
                .type12(typeCourseDetails)
                .type21(typeCourse)
                .type22(typeCourse)
                .build();
    }
}
