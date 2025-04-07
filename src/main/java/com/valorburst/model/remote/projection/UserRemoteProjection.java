package com.valorburst.model.remote.projection;

import java.math.BigDecimal;

public interface UserRemoteProjection {
    Integer getUserId();
    String getUserName();
    String getPhone();
    String getEmailName();
    String getPlatform();
    String getInvitationCode();
    String getInviterCode();
    BigDecimal getRate();
    BigDecimal getTwoRate();
    BigDecimal getMoneySum();
    BigDecimal getMoney();
    BigDecimal getCashOut();
    BigDecimal getCashOutStay();
    BigDecimal getMoneyWallet();
}
