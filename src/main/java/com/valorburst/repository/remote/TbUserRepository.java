package com.valorburst.repository.remote;

import com.valorburst.model.remote.TbUser;
import com.valorburst.model.remote.projection.InviterProjection;
import com.valorburst.model.remote.projection.UserRemoteProjection;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TbUserRepository extends JpaRepository<TbUser, Integer> {

    // 通过invitationCode查询用户
    Optional<TbUser> findByInvitationCode(String invitationCode);

    // 通过inviterCode查询用户
    Optional<TbUser> findByInviterCode(String inviterCode);

    // 通过inviterCode及1-15天内取1个随机用户
        @Query(value = """
        SELECT * FROM tb_user
        WHERE inviter_code = :inviterCode
        AND platform = 'h5.'
        AND create_time <= NOW() - INTERVAL 24 HOUR
        AND create_time >= NOW() - INTERVAL 15 DAY
        ORDER BY RAND()
        LIMIT 1
        """, nativeQuery = true)
    Optional<TbUser> findOneRandom(@Param("inviterCode") String inviterCode);

    // 通过userId查询用户
    @Query(value = """
        SELECT 
            u.user_id AS userId,
            u.user_name AS userName,
            u.phone AS phone,
            u.email_name AS emailName,
            u.platform AS platform,
            u.invitation_code AS invitationCode,
            u.inviter_code AS inviterCode,
            u.rate AS rate,
            u.two_rate AS twoRate,
            im.money_sum AS moneySum,
            im.money AS money,
            IFNULL((
                SELECT SUM(CAST(c.money AS DECIMAL)) FROM cash_out c 
                WHERE c.user_id = u.user_id AND c.state = 1
            ), 0) AS cashOut,
            IFNULL((
                SELECT SUM(CAST(c.money AS DECIMAL)) FROM cash_out c 
                WHERE c.user_id = u.user_id AND c.state = 0
            ), 0) AS cashOutStay,
            um.money AS moneyWallet
        FROM tb_user u
        LEFT JOIN invite_money im ON u.user_id = im.user_id
        LEFT JOIN user_money um ON u.user_id = um.user_id
        WHERE u.user_id = :userId
        """, nativeQuery = true)
    Optional<UserRemoteProjection> findProjectedById(@Param("userId") Integer userId);

    // 通过userIds查询用户列表
    @Query(value = """
        SELECT 
            u.user_id AS userId,
            u.user_name AS userName,
            u.phone AS phone,
            u.email_name AS emailName,
            u.platform AS platform,
            u.invitation_code AS invitationCode,
            u.inviter_code AS inviterCode,
            u.rate AS rate,
            u.two_rate AS twoRate,
            im.money_sum AS moneySum,
            im.money AS money,
            IFNULL((
                SELECT SUM(CAST(c.money AS DECIMAL)) FROM cash_out c 
                WHERE c.user_id = u.user_id AND c.state = 1
            ), 0) AS cashOut,
            IFNULL((
                SELECT SUM(CAST(c.money AS DECIMAL)) FROM cash_out c 
                WHERE c.user_id = u.user_id AND c.state = 0
            ), 0) AS cashOutStay,
            um.money AS moneyWallet
        FROM tb_user u
        LEFT JOIN invite_money im ON u.user_id = im.user_id
        LEFT JOIN user_money um ON u.user_id = um.user_id
        WHERE u.user_id IN :userIds
        """, nativeQuery = true)
    List<UserRemoteProjection> findProjectedByUserIds(@Param("userIds") List<Integer> userIds);

    // 通过phone查询用户
    @Query(value = """
        SELECT 
            u.user_id AS userId,
            u.user_name AS userName,
            u.phone AS phone,
            u.email_name AS emailName,
            u.platform AS platform,
            u.invitation_code AS invitationCode,
            u.inviter_code AS inviterCode,
            u.rate AS rate,
            u.two_rate AS twoRate,
            im.money_sum AS moneySum,
            im.money AS money,
            IFNULL((
                SELECT SUM(CAST(c.money AS DECIMAL)) FROM cash_out c 
                WHERE c.user_id = u.user_id AND c.state = 1
            ), 0) AS cashOut,
            IFNULL((
                SELECT SUM(CAST(c.money AS DECIMAL)) FROM cash_out c 
                WHERE c.user_id = u.user_id AND c.state = 0
            ), 0) AS cashOutStay,
            um.money AS moneyWallet
        FROM tb_user u
        LEFT JOIN invite_money im ON u.user_id = im.user_id
        LEFT JOIN user_money um ON u.user_id = um.user_id
        WHERE u.phone = :phone
        """, nativeQuery = true)
    Optional<UserRemoteProjection> findProjectedByPhone(@Param("phone") String phone);

    // 通过email查询用户
    @Query(value = """
        SELECT 
            u.user_id AS userId,
            u.user_name AS userName,
            u.phone AS phone,
            u.email_name AS emailName,
            u.platform AS platform,
            u.invitation_code AS invitationCode,
            u.inviter_code AS inviterCode,
            u.rate AS rate,
            u.two_rate AS twoRate,
            im.money_sum AS moneySum,
            im.money AS money,
            IFNULL((
                SELECT SUM(CAST(c.money AS DECIMAL)) FROM cash_out c 
                WHERE c.user_id = u.user_id AND c.state = 1
            ), 0) AS cashOut,
            IFNULL((
                SELECT SUM(CAST(c.money AS DECIMAL)) FROM cash_out c 
                WHERE c.user_id = u.user_id AND c.state = 0
            ), 0) AS cashOutStay,
            um.money AS moneyWallet
        FROM tb_user u
        LEFT JOIN invite_money im ON u.user_id = im.user_id
        LEFT JOIN user_money um ON u.user_id = um.user_id
        WHERE u.email_name = :email
        """, nativeQuery = true)
    Optional<UserRemoteProjection> findProjectedByEmail(@Param("email") String email);

    // 查询二级邀请人
    @Query(value = """
        SELECT
            parent.user_id as userId1,
            parent.rate as rate1,
            parent.two_rate as twoRate1,
            u.user_id as userId2,
            u.rate as rate2,
            u.two_rate as twoRate2
        FROM tb_user u
        JOIN tb_user parent ON u.invitation_code = parent.inviter_code
        WHERE parent.invitation_code = :invitationCode
        """, nativeQuery = true)
    Optional<InviterProjection> fetchInviter(String invitationCode);
}
