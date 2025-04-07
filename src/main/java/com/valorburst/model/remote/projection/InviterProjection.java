package com.valorburst.model.remote.projection;

import java.math.BigDecimal;

public interface InviterProjection {
    Integer getUserId1();
    BigDecimal getRate1();
    BigDecimal getTwoRate1();
    Integer getUserId2();
    BigDecimal getRate2();
    BigDecimal getTwoRate2();
}
