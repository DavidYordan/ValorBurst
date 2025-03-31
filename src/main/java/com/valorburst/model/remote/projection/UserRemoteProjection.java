package com.valorburst.model.remote.projection;

public interface UserRemoteProjection {
    Integer getUserId();
    String getUserName();
    String getPhone();
    String getEmailName();
    String getPlatform();
    String getInvitationCode();
    String getInviterCode();
    Double getRate();
    Double getTwoRate();
    Double getMoneySum();
    Double getMoney();
    Double getCashOut();
    Double getCashOutStay();
    Double getMoneyWallet();
}
