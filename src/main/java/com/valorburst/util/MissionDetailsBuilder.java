package com.valorburst.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

import com.valorburst.dto.JdbcDto;
import com.valorburst.model.local.LocalCommonInfo;
import com.valorburst.model.local.MissionDetails;
import com.valorburst.model.local.User;
import com.valorburst.model.remote.Invite;
import com.valorburst.model.remote.TbUser;
import com.valorburst.model.remote.UserMoneyDetails;
import com.valorburst.repository.local.LocalCommonInfoRepository;
import com.valorburst.repository.local.UserRepository;
import com.valorburst.repository.remote.TbUserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class MissionDetailsBuilder {

    private final LocalCommonInfoRepository localCommonInfoRepository;
    private final UserRepository userRepository;
    private final TbUserRepository tbUserRepository;

    public JdbcDto buildJdbc(MissionDetails details) {
        Integer type = details.getType();
        switch (type) {
            case 0:
                return buildJdbcEmpty(details);
            case 1:
                return buildJdbcVip1(details, 3);
            case 2:
                return buildJdbcVip1(details, 0);
            case 3:
                return buildJdbcVip1(details, 1);
            case 4:
                return buildJdbcVip1(details, 2);
            case 5:
                return buildJdbcVip2(details, 3);
            case 6:
                return buildJdbcVip2(details, 0);
            case 7:
                return buildJdbcVip2(details, 1);
            case 8:
                return buildJdbcVip2(details, 2);
            case 11:
                return buildJdbcSingle1(details);
            case 12:
                return buildJdbcSingle2(details);
            case 21:
                return buildJdbcAll1(details);
            case 22:
                return buildJdbcAll2(details);
            default:
                throw new IllegalArgumentException("不支持的任务类型: " + type);
        }
    }

    private JdbcDto buildJdbcEmpty(MissionDetails details) {
        log.info("开始处理Empty任务: {}", details);

        Integer userId = details.getUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在: " + userId));

        Optional<TbUser> inviter = tbUserRepository.findByInvitationCode(user.getInviterCode());

        String email = null;
        String phone = null;
        if (Math.random() > 0.5) {
            email = EmailGenerator.generateEmail(details.getLanguageType());
        } else {
            phone = PhoneGenerator.generatePhone(details.getLanguageType());
        }
        String username = maskUsername(email != null ? email : phone);

        Instant executeTime = details.getExecuteTime();

        BigDecimal rate1 = parseBigDecimalById(localCommonInfoRepository.findById(420));
        BigDecimal rate2 = parseBigDecimalById(localCommonInfoRepository.findById(421));

        TbUser tbUser = TbUser.builder()
                .userName(username)
                .phone(phone)
                .emailName(email)
                .password(generatePassword())
                .createTime(executeTime)
                .status(1)
                .platform("h5.")
                .inviterCode(user.getInvitationCode())
                .rate(rate1)
                .twoRate(rate2)
                .qdCode("8888")
                .build();

        TbUser saveTbUser = tbUserRepository.save(tbUser);
        Integer inviteeId = saveTbUser.getUserId();
        log.info("生成新用户: {}", inviteeId);
        TbUser updateTbUser = TbUser.builder()
                .userId(inviteeId)
                .invitationCode(InvitationCodeUtil.toSerialCode(inviteeId))
                .build();

        Invite invite1 = Invite.builder()
                .userId(userId)
                .inviteeUserId(inviteeId)
                .state(1)
                .money(BigDecimal.ZERO)
                .createTime(executeTime)
                .userType(1)
                .build();

        Invite invite2 = null;
        if (inviter.isPresent()) {
            invite2 = Invite.builder()
                .userId(inviter.get().getUserId())
                .inviteeUserId(inviteeId)
                .state(1)
                .money(BigDecimal.ZERO)
                .createTime(executeTime)
                .userType(2)
                .build();
        }

        log.info("准备生成SQL");
        StringBuilder sql = new StringBuilder();
        sql.append(JpaSqlBuilder.buildUpdateSql(updateTbUser, List.of("userId")));
        sql.append(JpaSqlBuilder.buildInsertSql(invite1));
        if (invite2 != null) {
            sql.append(JpaSqlBuilder.buildInsertSql(invite2));
        }
        return JdbcDto.builder()
                .sql(sql.toString())
                .userId(userId)
                .username(username)
                .build();
    }

    private JdbcDto buildJdbcVip1(MissionDetails details, int vipType) {
        log.info("开始处理VIP1任务: {}", details);

        Integer userId = details.getUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在: " + userId));

        Optional<TbUser> inviter = tbUserRepository.findByInvitationCode(user.getInviterCode());

        String email = null;
        String phone = null;
        if (Math.random() > 0.5) {
            email = EmailGenerator.generateEmail(details.getLanguageType());
        } else {
            phone = PhoneGenerator.generatePhone(details.getLanguageType());
        }
        String username = maskUsername(email != null ? email : phone);

        Instant executeTime = details.getExecuteTime();
        Instant orderTime = offsetDateTime(executeTime, -60, -2);
        Instant registerTime = offsetDateTime(orderTime, -300, -30);

        BigDecimal rate1 = parseBigDecimalById(localCommonInfoRepository.findById(420));
        BigDecimal rate2 = parseBigDecimalById(localCommonInfoRepository.findById(421));

        // user_id, user_name, email_name, password, create_time, status, platform, invitation_code,
        // inviter_code, rate, two_rate, qd_code, ban_invitations, fake
        TbUser tbUser = TbUser.builder()
                .userName(username)
                .phone(phone)
                .emailName(email)
                .password(generatePassword())
                .createTime(registerTime)
                .status(1)
                .platform("h5.")
                .inviterCode(user.getInvitationCode())
                .rate(rate1)
                .twoRate(rate2)
                .qdCode("8888")
                .build();

        TbUser saveTbUser = tbUserRepository.save(tbUser);
        Integer inviteeId = saveTbUser.getUserId();
        log.info("生成新用户: {}", inviteeId);
        TbUser updateTbUser = TbUser.builder()
                .userId(inviteeId)
                .invitationCode(InvitationCodeUtil.toSerialCode(inviteeId))
                .build();

        BigDecimal reward = details.getMoney();
        // user_id, title, classify, type, state, money, content, create_time, language_type
        UserMoneyDetails userMoneyDetailsEn = UserMoneyDetails.builder()
                .userId(userId)
                .title("[First level invitation commission] First level friend name: " + username)
                .classify(2)
                .type(1)
                .state(1)
                .money(reward)
                .content("Increase amount: " + reward)
                .createTime(executeTime)
                .languageType("en")
                .build();
        UserMoneyDetails userMoneyDetailsZh = UserMoneyDetails.builder()
                .userId(userId)
                .title("[一级邀请佣金]一级好友名称：" + username)
                .classify(2)
                .type(1)
                .state(1)
                .money(reward)
                .content("增加金额:" + reward)
                .createTime(executeTime)
                .languageType("zh")
                .build();
        // user_id, invitee_user_id, state, money, create_time, user_type
        Invite invite1 = Invite.builder()
                .userId(userId)
                .inviteeUserId(inviteeId)
                .state(1)
                .money(reward)
                .createTime(executeTime)
                .userType(1)
                .build();

        UserMoneyDetails userMoneyDetailsEn2 = null;
        UserMoneyDetails userMoneyDetailsZh2 = null;
        Invite invite2 = null;
        BigDecimal reward2 = BigDecimal.ZERO;
        if (inviter.isPresent()) {
            TbUser inviterUser = inviter.get();
            BigDecimal inviter2Rate2 = inviterUser.getTwoRate();
            if (inviter2Rate2 != null && inviter2Rate2.compareTo(BigDecimal.ZERO) > 0) {
                reward2 = reward.multiply(inviter2Rate2).setScale(2, RoundingMode.HALF_UP);
                if (reward2.compareTo(BigDecimal.ZERO) > 0) {
                    userMoneyDetailsEn2 = UserMoneyDetails.builder()
                        .userId(inviterUser.getUserId())
                        .title("[Second level invitation commission] Second level friend name: " + username)
                        .classify(2)
                        .type(1)
                        .state(1)
                        .money(reward2)
                        .content("Increase amount: " + reward2)
                        .createTime(executeTime)
                        .languageType("en")
                        .build();
                    userMoneyDetailsZh2 = UserMoneyDetails.builder()
                        .userId(inviterUser.getUserId())
                        .title("[二级邀请佣金]二级好友名称：" + username)
                        .classify(2)
                        .type(1)
                        .state(1)
                        .money(reward2)
                        .content("增加金额:" + reward2)
                        .createTime(executeTime)
                        .languageType("zh")
                        .build();
                }
            }
            
            invite2 = Invite.builder()
                .userId(inviter.get().getUserId())
                .inviteeUserId(inviteeId)
                .state(1)
                .money(reward2)
                .createTime(executeTime)
                .userType(2)
                .build();
        }

        log.info("准备生成SQL");
        StringBuilder sql = new StringBuilder();
        sql.append(JpaSqlBuilder.buildUpdateSql(updateTbUser, List.of("userId")));
        sql.append(JpaSqlBuilder.buildInsertSql(userMoneyDetailsEn));
        sql.append(JpaSqlBuilder.buildInsertSql(userMoneyDetailsZh));
        sql.append(JpaSqlBuilder.buildInsertSql(invite1));
        if (invite2 != null) {
            sql.append(JpaSqlBuilder.buildInsertSql(invite2));
            if (userMoneyDetailsEn2 != null) {
                sql.append(JpaSqlBuilder.buildInsertSql(userMoneyDetailsEn2));
                sql.append(JpaSqlBuilder.buildInsertSql(userMoneyDetailsZh2));
            }
        }
        sql.append("UPDATE `invite_money` SET money_sum = money_sum + ")
            .append(reward).append(", money = money + ").append(reward)
            .append(" WHERE user_id=").append(userId).append(";");
        if (reward2.compareTo(BigDecimal.ZERO) > 0) {
            sql.append("UPDATE `invite_money` SET money_sum = money_sum + ")
                .append(reward2).append(", money = money + ").append(reward2)
                .append(" WHERE user_id=").append(inviter.get().getUserId()).append(";");
        }
        return JdbcDto.builder()
                .sql(sql.toString())
                .userId(userId)
                .username(username)
                .build();
    }

    private BigDecimal parseBigDecimalById(Optional<LocalCommonInfo> commonInfo) {
        if (commonInfo.isEmpty()) {
            throw new IllegalArgumentException("默认rate不存在");
        }

        String value = commonInfo.get().getValue();
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            throw new RuntimeException("值格式错误，无法转换为double: " + value, e);
        }
    }

    private JdbcDto buildJdbcVip2(MissionDetails details, int vipType) {
        log.info("开始处理VIP2任务: {}", details);
    
        Integer userId = details.getUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在: " + userId));
    
        // 获取当前用户的所有一级邀请（即 tb_user 表中 inviter_code = user.invitation_code）
        Optional<TbUser> firstLevelInvitee = tbUserRepository.findOneRandom(user.getInvitationCode());
    
        if (firstLevelInvitee.isEmpty()) {
            throw new IllegalArgumentException("该用户没有满足条件的一级邀请用户");
        }
    
        // 随机选择一个一级邀请的用户
        TbUser selectedInvitee = firstLevelInvitee.get();
        String selectedInviteeCode = selectedInvitee.getInvitationCode();
    
        // 构造一个新用户（被一级用户邀请 = 二级用户）
        String email = null;
        String phone = null;
        if (Math.random() > 0.5) {
            email = EmailGenerator.generateEmail(details.getLanguageType());
        } else {
            phone = PhoneGenerator.generatePhone(details.getLanguageType());
        }
        String username = maskUsername(email != null ? email : phone);
    
        Instant executeTime = details.getExecuteTime();
        Instant orderTime = offsetDateTime(executeTime, -60, -2);
        Instant registerTime = offsetDateTime(orderTime, -300, -30);
    
        BigDecimal rate1 = parseBigDecimalById(localCommonInfoRepository.findById(420));
        BigDecimal rate2 = parseBigDecimalById(localCommonInfoRepository.findById(421));
    
        TbUser tbUser = TbUser.builder()
                .userName(username)
                .phone(phone)
                .emailName(email)
                .password(generatePassword())
                .createTime(registerTime)
                .status(1)
                .platform("h5.")
                .inviterCode(selectedInviteeCode)  // 绑定一级邀请用户
                .rate(rate1)
                .twoRate(rate2)
                .qdCode("8888")
                .build();
    
        TbUser saveTbUser = tbUserRepository.save(tbUser);
        Integer inviteeId = saveTbUser.getUserId();
        log.info("生成新用户: {}", inviteeId);
    
        TbUser updateTbUser = TbUser.builder()
                .userId(inviteeId)
                .invitationCode(InvitationCodeUtil.toSerialCode(inviteeId))
                .build();
    
        // 构建二级邀请奖励数据
        BigDecimal reward2 = details.getMoney();
        UserMoneyDetails userMoneyDetailsEn2 = UserMoneyDetails.builder()
                .userId(userId)
                .title("[Second level invitation commission] Second level friend name: " + username)
                .classify(2)
                .type(1)
                .state(1)
                .money(reward2)
                .content("Increase amount: " + reward2)
                .createTime(executeTime)
                .languageType("en")
                .build();
        UserMoneyDetails userMoneyDetailsZh2 = UserMoneyDetails.builder()
                .userId(userId)
                .title("[二级邀请佣金]二级好友名称：" + username)
                .classify(2)
                .type(1)
                .state(1)
                .money(reward2)
                .content("增加金额:" + reward2)
                .createTime(executeTime)
                .languageType("zh")
                .build();
    
        Invite invite2 = Invite.builder()
                .userId(userId)  // 当前用户是二级邀请者
                .inviteeUserId(inviteeId)
                .state(1)
                .money(reward2)
                .createTime(executeTime)
                .userType(2)
                .build();
    
        // 构建 SQL
        StringBuilder sql = new StringBuilder();
        sql.append(JpaSqlBuilder.buildUpdateSql(updateTbUser, List.of("userId")));
        sql.append(JpaSqlBuilder.buildInsertSql(userMoneyDetailsEn2));
        sql.append(JpaSqlBuilder.buildInsertSql(userMoneyDetailsZh2));
        sql.append(JpaSqlBuilder.buildInsertSql(invite2));
        sql.append("UPDATE `invite_money` SET money_sum = money_sum + ")
                .append(reward2).append(", money = money + ").append(reward2)
                .append(" WHERE user_id=").append(userId).append(";");
        return JdbcDto.builder()
                .sql(sql.toString())
                .userId(userId)
                .username(username)
                .build();
    }

    private JdbcDto buildJdbcSingle1(MissionDetails details) {
        log.info("开始处理单集任务: {}", details);

        Integer userId = details.getUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在: " + userId));

        Instant executeTime = details.getExecuteTime();
        Instant orderTime = offsetDateTime(executeTime, -60, -2);
        Instant registerTime = offsetDateTime(orderTime, -300, -30);

        Integer inviteeId = details.getInviteeId();
        String username = details.getInviteeName();
        TbUser updateTbUser = null;

        if (inviteeId == null) {
            String email = null;
            String phone = null;
            if (Math.random() > 0.5) {
                email = EmailGenerator.generateEmail(details.getLanguageType());
            } else {
                phone = PhoneGenerator.generatePhone(details.getLanguageType());
            }
            username = maskUsername(email != null ? email : phone);

            TbUser tbUser = TbUser.builder()
                .userName(username)
                .phone(phone)
                .emailName(email)
                .password(generatePassword())
                .createTime(registerTime)
                .status(1)
                .platform("h5.")
                .inviterCode(user.getInvitationCode())
                .rate(parseBigDecimalById(localCommonInfoRepository.findById(420)))
                .twoRate(parseBigDecimalById(localCommonInfoRepository.findById(421)))
                .qdCode("8888")
                .build();
            TbUser saveTbUser = tbUserRepository.save(tbUser);
            inviteeId = saveTbUser.getUserId();
            log.info("生成新用户: {}", inviteeId);
            updateTbUser = TbUser.builder()
                    .userId(inviteeId)
                    .invitationCode(InvitationCodeUtil.toSerialCode(inviteeId))
                    .build();
        }

        BigDecimal reward = details.getMoney();
        UserMoneyDetails userMoneyDetailsEn = UserMoneyDetails.builder()
                .userId(userId)
                .title("[First level invitation commission] First level friend name: " + username)
                .classify(2)
                .type(1)
                .state(1)
                .money(reward)
                .content("Increase amount: " + reward)
                .createTime(executeTime)
                .languageType("en")
                .build();
        UserMoneyDetails userMoneyDetailsZh = UserMoneyDetails.builder()
                .userId(userId)
                .title("[一级邀请佣金]一级好友名称：" + username)
                .classify(2)
                .type(1)
                .state(1)
                .money(reward)
                .content("增加金额:" + reward)
                .createTime(executeTime)
                .languageType("zh")
                .build();
        Invite invite1 = Invite.builder()
                .userId(userId)
                .inviteeUserId(inviteeId)
                .state(1)
                .money(BigDecimal.ZERO)
                .createTime(executeTime)
                .userType(1)
                .build();

        Optional<TbUser> inviter = tbUserRepository.findByInvitationCode(user.getInviterCode());
        UserMoneyDetails userMoneyDetailsEn2 = null;
        UserMoneyDetails userMoneyDetailsZh2 = null;
        Invite invite2 = null;
        BigDecimal reward2 = BigDecimal.ZERO;
        if (inviter.isPresent()) {
            TbUser inviterUser = inviter.get();
            BigDecimal inviter2Rate2 = inviterUser.getTwoRate();
            if (inviter2Rate2 != null && inviter2Rate2.compareTo(BigDecimal.ZERO) > 0) {
                // reward2 = details.getCost() * inviterUser.getTwoRate();
                reward2 = reward.multiply(inviter2Rate2).setScale(2, RoundingMode.HALF_UP);
                if (reward2.compareTo(BigDecimal.ZERO) > 0) {
                    userMoneyDetailsEn2 = UserMoneyDetails.builder()
                        .userId(inviterUser.getUserId())
                        .title("[Second level invitation commission] Second level friend name: " + username)
                        .classify(2)
                        .type(1)
                        .state(1)
                        .money(reward2)
                        .content("Increase amount: " + reward2)
                        .createTime(executeTime)
                        .languageType("en")
                        .build();
                    userMoneyDetailsZh2 = UserMoneyDetails.builder()
                        .userId(inviterUser.getUserId())
                        .title("[二级邀请佣金]二级好友名称：" + username)
                        .classify(2)
                        .type(1)
                        .state(1)
                        .money(reward2)
                        .content("增加金额:" + reward2)
                        .createTime(executeTime)
                        .languageType("zh")
                        .build();
                }
                
            }
            
            invite2 = Invite.builder()
                .userId(inviter.get().getUserId())
                .inviteeUserId(inviteeId)
                .state(1)
                .money(reward2)
                .createTime(executeTime)
                .userType(2)
                .build();
        }


        log.info("准备生成SQL");
        StringBuilder sql = new StringBuilder();
        if (updateTbUser != null) {
            sql.append(JpaSqlBuilder.buildUpdateSql(updateTbUser, List.of("userId")));
        }
        sql.append(JpaSqlBuilder.buildInsertSql(userMoneyDetailsEn));
        sql.append(JpaSqlBuilder.buildInsertSql(userMoneyDetailsZh));
        sql.append(JpaSqlBuilder.buildInsertSql(invite1));
        if (userMoneyDetailsEn2 != null) {
            sql.append(JpaSqlBuilder.buildInsertSql(userMoneyDetailsEn2));
            sql.append(JpaSqlBuilder.buildInsertSql(userMoneyDetailsZh2));
        }
        if (invite2 != null) {
            sql.append(JpaSqlBuilder.buildInsertSql(invite2));
        }
        sql.append("UPDATE `invite_money` SET money_sum = money_sum + ")
            .append(reward).append(", money = money + ").append(reward)
            .append(" WHERE user_id=").append(userId).append(";");
        if (reward2.compareTo(BigDecimal.ZERO) > 0) {
            sql.append("UPDATE `invite_money` SET money_sum = money_sum + ")
                .append(reward2).append(", money = money + ").append(reward2)
                .append(" WHERE user_id=").append(inviter.get().getUserId()).append(";");
        }
        return JdbcDto.builder()
                .sql(sql.toString())
                .userId(userId)
                .username(username)
                .build();
    }

    private JdbcDto buildJdbcSingle2(MissionDetails details) {
        log.info("开始处理单集2任务: {}", details);
    
        Integer userId = details.getUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在: " + userId));
    
        Instant executeTime = details.getExecuteTime();
        Instant orderTime = offsetDateTime(executeTime, -60, -2);
        Instant registerTime = offsetDateTime(orderTime, -300, -30);

        Integer inviteeId = details.getInviteeId();
        String username = details.getInviteeName();
        TbUser updateTbUser = null;

        if (inviteeId == null) {
            // 获取当前用户的所有一级邀请（即 tb_user 表中 inviter_code = user.invitation_code）
            Optional<TbUser> firstLevelInvitee = tbUserRepository.findOneRandom(user.getInvitationCode());
        
            if (firstLevelInvitee.isEmpty()) {
                throw new IllegalArgumentException("该用户没有满足条件的一级邀请用户");
            }
        
            // 随机选择一个一级邀请的用户
            TbUser selectedInvitee = firstLevelInvitee.get();
            String selectedInviteeCode = selectedInvitee.getInvitationCode();
        
            // 构造一个新用户（被一级用户邀请 = 二级用户）
            String email = null;
            String phone = null;
            if (Math.random() > 0.5) {
                email = EmailGenerator.generateEmail(details.getLanguageType());
            } else {
                phone = PhoneGenerator.generatePhone(details.getLanguageType());
            }
            username = maskUsername(email != null ? email : phone);
        
            BigDecimal rate1 = parseBigDecimalById(localCommonInfoRepository.findById(420));
            BigDecimal rate2 = parseBigDecimalById(localCommonInfoRepository.findById(421));
        
            TbUser tbUser = TbUser.builder()
                    .userName(username)
                    .phone(phone)
                    .emailName(email)
                    .password(generatePassword())
                    .createTime(registerTime)
                    .status(1)
                    .platform("h5.")
                    .inviterCode(selectedInviteeCode)  // 绑定一级邀请用户
                    .rate(rate1)
                    .twoRate(rate2)
                    .qdCode("8888")
                    .build();
        
            TbUser saveTbUser = tbUserRepository.save(tbUser);
            inviteeId = saveTbUser.getUserId();
            log.info("生成新用户: {}", inviteeId);
        
            updateTbUser = TbUser.builder()
                    .userId(inviteeId)
                    .invitationCode(InvitationCodeUtil.toSerialCode(inviteeId))
                    .build();
        }
    
        // 构建二级邀请奖励数据
        BigDecimal reward2 = details.getMoney();
        UserMoneyDetails userMoneyDetailsEn2 = UserMoneyDetails.builder()
                .userId(userId)
                .title("[Second level invitation commission] Second level friend name: " + username)
                .classify(2)
                .type(1)
                .state(1)
                .money(reward2)
                .content("Increase amount: " + reward2)
                .createTime(executeTime)
                .languageType("en")
                .build();
        UserMoneyDetails userMoneyDetailsZh2 = UserMoneyDetails.builder()
                .userId(userId)
                .title("[二级邀请佣金]二级好友名称：" + username)
                .classify(2)
                .type(1)
                .state(1)
                .money(reward2)
                .content("增加金额:" + reward2)
                .createTime(executeTime)
                .languageType("zh")
                .build();
    
        Invite invite2 = Invite.builder()
                .userId(userId)  // 当前用户是二级邀请者
                .inviteeUserId(inviteeId)
                .state(1)
                .money(reward2)
                .createTime(executeTime)
                .userType(2)
                .build();
    
        // 构建 SQL
        StringBuilder sql = new StringBuilder();
        if (updateTbUser != null) {
            sql.append(JpaSqlBuilder.buildUpdateSql(updateTbUser, List.of("userId")));
        }
        sql.append(JpaSqlBuilder.buildInsertSql(userMoneyDetailsEn2));
        sql.append(JpaSqlBuilder.buildInsertSql(userMoneyDetailsZh2));
        sql.append(JpaSqlBuilder.buildInsertSql(invite2));
        sql.append("UPDATE `invite_money` SET money_sum = money_sum + ")
                .append(reward2).append(", money = money + ").append(reward2)
                .append(" WHERE user_id=").append(userId).append(";");
        return JdbcDto.builder()
                .sql(sql.toString())
                .userId(userId)
                .username(username)
                .build();
    }

    private JdbcDto buildJdbcAll1(MissionDetails details) {
        log.info("开始处理全集1任务: {}", details);

        Integer userId = details.getUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在: " + userId));

        String email = null;
        String phone = null;
        if (Math.random() > 0.5) {
            email = EmailGenerator.generateEmail(details.getLanguageType());
        } else {
            phone = PhoneGenerator.generatePhone(details.getLanguageType());
        }
        String username = maskUsername(email != null ? email : phone);
        Instant executeTime = details.getExecuteTime();
        Instant orderTime = offsetDateTime(executeTime, -60, -2);
        Instant registerTime = offsetDateTime(orderTime, -300, -30);

        BigDecimal rate1 = parseBigDecimalById(localCommonInfoRepository.findById(420));
        BigDecimal rate2 = parseBigDecimalById(localCommonInfoRepository.findById(421));

        Integer inviteeId = details.getInviteeId();

        TbUser tbUser = TbUser.builder()
            .userName(username)
            .phone(phone)
            .emailName(email)
            .password(generatePassword())
            .createTime(registerTime)
            .status(1)
            .platform("h5.")
            .inviterCode(user.getInvitationCode())
            .rate(rate1)
            .twoRate(rate2)
            .qdCode("8888")
            .build();
        TbUser saveTbUser = tbUserRepository.save(tbUser);
        inviteeId = saveTbUser.getUserId();
        log.info("生成新用户: {}", inviteeId);
        TbUser updateTbUser = TbUser.builder()
                .userId(inviteeId)
                .invitationCode(InvitationCodeUtil.toSerialCode(inviteeId))
                .build();

        BigDecimal reward = details.getMoney();
        UserMoneyDetails userMoneyDetailsEn = UserMoneyDetails.builder()
                .userId(userId)
                .title("[First level invitation commission] First level friend name: " + username)
                .classify(2)
                .type(1)
                .state(1)
                .money(reward)
                .content("Increase amount: " + reward)
                .createTime(executeTime)
                .languageType("en")
                .build();
        UserMoneyDetails userMoneyDetailsZh = UserMoneyDetails.builder()
                .userId(userId)
                .title("[一级邀请佣金]一级好友名称：" + username)
                .classify(2)
                .type(1)
                .state(1)
                .money(reward)
                .content("增加金额:" + reward)
                .createTime(executeTime)
                .languageType("zh")
                .build();
        Invite invite1 = Invite.builder()
                .userId(userId)
                .inviteeUserId(inviteeId)
                .state(1)
                .money(BigDecimal.ZERO)
                .createTime(executeTime)
                .userType(1)
                .build();
        Optional<TbUser> inviter = tbUserRepository.findByInvitationCode(user.getInviterCode());
        UserMoneyDetails userMoneyDetailsEn2 = null;
        UserMoneyDetails userMoneyDetailsZh2 = null;
        Invite invite2 = null;
        BigDecimal reward2 = BigDecimal.ZERO;
        if (inviter.isPresent()) {
            TbUser inviterUser = inviter.get();
            BigDecimal inviter2Rate2 = inviterUser.getTwoRate();
            if (inviter2Rate2 != null && inviter2Rate2.compareTo(BigDecimal.ZERO) > 0) {
                reward2 = reward.multiply(inviter2Rate2).setScale(2, RoundingMode.HALF_UP);
                if (reward2.compareTo(BigDecimal.ZERO) > 0) {
                    userMoneyDetailsEn2 = UserMoneyDetails.builder()
                        .userId(inviterUser.getUserId())
                        .title("[Second level invitation commission] Second level friend name: " + username)
                        .classify(2)
                        .type(1)
                        .state(1)
                        .money(reward2)
                        .content("Increase amount: " + reward2)
                        .createTime(executeTime)
                        .languageType("en")
                        .build();
                    userMoneyDetailsZh2 = UserMoneyDetails.builder()
                        .userId(inviterUser.getUserId())
                        .title("[二级邀请佣金]二级好友名称：" + username)
                        .classify(2)
                        .type(1)
                        .state(1)
                        .money(reward2)
                        .content("增加金额:" + reward2)
                        .createTime(executeTime)
                        .languageType("zh")
                        .build();
                }
                
            }
            
            invite2 = Invite.builder()
                .userId(inviter.get().getUserId())
                .inviteeUserId(inviteeId)
                .state(1)
                .money(reward2)
                .createTime(executeTime)
                .userType(2)
                .build();
        }
        log.info("准备生成SQL");
        StringBuilder sql = new StringBuilder();
        sql.append(JpaSqlBuilder.buildUpdateSql(updateTbUser, List.of("userId")));
        sql.append(JpaSqlBuilder.buildInsertSql(userMoneyDetailsEn));
        sql.append(JpaSqlBuilder.buildInsertSql(userMoneyDetailsZh));
        sql.append(JpaSqlBuilder.buildInsertSql(invite1));
        if (userMoneyDetailsEn2 != null) {
            sql.append(JpaSqlBuilder.buildInsertSql(userMoneyDetailsEn2));
            sql.append(JpaSqlBuilder.buildInsertSql(userMoneyDetailsZh2));
        }
        if (invite2 != null) {
            sql.append(JpaSqlBuilder.buildInsertSql(invite2));
        }
        sql.append("UPDATE `invite_money` SET money_sum = money_sum + ")
            .append(reward).append(", money = money + ").append(reward)
            .append(" WHERE user_id=").append(userId).append(";");
        if (reward2.compareTo(BigDecimal.ZERO) > 0) {
            sql.append("UPDATE `invite_money` SET money_sum = money_sum + ")
                .append(reward2).append(", money = money + ").append(reward2)
                .append(" WHERE user_id=").append(inviter.get().getUserId()).append(";");
        }
        return JdbcDto.builder()
                .sql(sql.toString())
                .userId(userId)
                .username(username)
                .build();
    }

    private JdbcDto buildJdbcAll2(MissionDetails details) {
        log.info("开始处理全集2任务: {}", details);
    
        Integer userId = details.getUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在: " + userId));
    
        // 获取当前用户的所有一级邀请（即 tb_user 表中 inviter_code = user.invitation_code）
        Optional<TbUser> firstLevelInvitee = tbUserRepository.findOneRandom(user.getInvitationCode());
    
        if (firstLevelInvitee.isEmpty()) {
            throw new IllegalArgumentException("该用户没有满足条件的一级邀请用户");
        }
    
        // 随机选择一个一级邀请的用户
        TbUser selectedInvitee = firstLevelInvitee.get();
        String selectedInviteeCode = selectedInvitee.getInvitationCode();
    
        // 构造一个新用户（被一级用户邀请 = 二级用户）
        String email = null;
        String phone = null;
        if (Math.random() > 0.5) {
            email = EmailGenerator.generateEmail(details.getLanguageType());
        } else {
            phone = PhoneGenerator.generatePhone(details.getLanguageType());
        }
        String username = maskUsername(email != null ? email : phone);
    
        Instant executeTime = details.getExecuteTime();
        Instant orderTime = offsetDateTime(executeTime, -60, -2);
        Instant registerTime = offsetDateTime(orderTime, -300, -30);
    
        BigDecimal rate1 = parseBigDecimalById(localCommonInfoRepository.findById(420));
        BigDecimal rate2 = parseBigDecimalById(localCommonInfoRepository.findById(421));
    
        TbUser tbUser = TbUser.builder()
                .userName(username)
                .phone(phone)
                .emailName(email)
                .password(generatePassword())
                .createTime(registerTime)
                .status(1)
                .platform("h5.")
                .inviterCode(selectedInviteeCode)  // 绑定一级邀请用户
                .rate(rate1)
                .twoRate(rate2)
                .qdCode("8888")
                .build();
    
        TbUser saveTbUser = tbUserRepository.save(tbUser);
        Integer inviteeId = saveTbUser.getUserId();
        log.info("生成新用户: {}", inviteeId);
    
        TbUser updateTbUser = TbUser.builder()
                .userId(inviteeId)
                .invitationCode(InvitationCodeUtil.toSerialCode(inviteeId))
                .build();
    
        // 构建二级邀请奖励数据
        BigDecimal reward2 = details.getMoney();
        UserMoneyDetails userMoneyDetailsEn2 = UserMoneyDetails.builder()
                .userId(userId)
                .title("[Second level invitation commission] Second level friend name: " + username)
                .classify(2)
                .type(1)
                .state(1)
                .money(reward2)
                .content("Increase amount: " + reward2)
                .createTime(executeTime)
                .languageType("en")
                .build();
        UserMoneyDetails userMoneyDetailsZh2 = UserMoneyDetails.builder()
                .userId(userId)
                .title("[二级邀请佣金]二级好友名称：" + username)
                .classify(2)
                .type(1)
                .state(1)
                .money(reward2)
                .content("增加金额:" + reward2)
                .createTime(executeTime)
                .languageType("zh")
                .build();
    
        Invite invite2 = Invite.builder()
                .userId(userId)  // 当前用户是二级邀请者
                .inviteeUserId(inviteeId)
                .state(1)
                .money(reward2)
                .createTime(executeTime)
                .userType(2)
                .build();
    
        // 构建 SQL
        StringBuilder sql = new StringBuilder();
        sql.append(JpaSqlBuilder.buildUpdateSql(updateTbUser, List.of("userId")));
        sql.append(JpaSqlBuilder.buildInsertSql(userMoneyDetailsEn2));
        sql.append(JpaSqlBuilder.buildInsertSql(userMoneyDetailsZh2));
        sql.append(JpaSqlBuilder.buildInsertSql(invite2));
        sql.append("UPDATE `invite_money` SET money_sum = money_sum + ")
                .append(reward2).append(", money = money + ").append(reward2)
                .append(" WHERE user_id=").append(userId).append(";");
        return JdbcDto.builder()
                .sql(sql.toString())
                .userId(userId)
                .username(username)
                .build();
    }

    private String maskUsername(String username) {
        int atIndex = username.indexOf('@');
        if (atIndex != -1) {
            return username.substring(0, 3) + "****" + username.substring(atIndex);
        } else {
            return username.substring(0, 4) + "****" + username.substring(username.length() - 4);
        }
    }

    private String generatePassword() {
        String password = UUID.randomUUID().toString().replace("-", "");
        return DigestUtils.md5DigestAsHex(password.getBytes());
    }

    private Instant offsetDateTime(Instant dateTime, int minSeconds, int maxSeconds) {
        int offsetSeconds = ThreadLocalRandom.current().nextInt(minSeconds, maxSeconds);
        return dateTime.plusSeconds(offsetSeconds);
    }
}
