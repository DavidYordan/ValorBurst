package com.valorburst.repository.local;

import com.valorburst.model.local.LocalVipDetails;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface LocalVipDetailsRepository extends JpaRepository<LocalVipDetails, Integer> {
    
    @Query("""
        select lvd.money
        from LocalVipDetails lvd
        where lvd.vipNameType = :vipNameType
        and lvd.languageType = :languageType
    """)
    BigDecimal findMoney(Integer vipNameType, String languageType);

    // 根据languageType查找
    @Query("""
        select lvd
        from LocalVipDetails lvd
        where lvd.languageType = :languageType
    """)
    List<LocalVipDetails> findByLanguageType(String languageType);
}
