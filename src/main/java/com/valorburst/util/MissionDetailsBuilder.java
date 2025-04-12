package com.valorburst.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

import com.valorburst.dto.JdbcDto;
import com.valorburst.model.local.LocalCommonInfo;
import com.valorburst.model.local.Mission;
import com.valorburst.model.local.MissionDetails;
import com.valorburst.model.local.User;
import com.valorburst.model.remote.Invite;
import com.valorburst.model.remote.TbUser;
import com.valorburst.model.remote.UserMoneyDetails;
import com.valorburst.repository.local.LocalCommonInfoRepository;
import com.valorburst.repository.local.MissionRepository;
import com.valorburst.repository.local.UserRepository;
import com.valorburst.repository.remote.TbUserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class MissionDetailsBuilder {

    private final LocalCommonInfoRepository localCommonInfoRepository;
    private final MissionRepository missionRepository;
    private final UserRepository userRepository;
    private final TbUserRepository tbUserRepository;

    private static final BigDecimal BIGZERO = BigDecimal.ZERO;

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
        Integer userId = details.getUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在: " + userId));

        String email = null;
        String phone = null;
        if (Math.random() > 0.55) {
            email = EmailGenerator.generateEmail(details.getLanguageType());
        } else {
            phone = PhoneGenerator.generatePhone(details.getLanguageType());
        }
        String username = maskUsername(email != null ? email : phone);

        LocalDateTime executeTime = details.getExecuteTime();

        BigDecimal rate1 = parseBigDecimal(localCommonInfoRepository.findById(420));
        BigDecimal rate2 = parseBigDecimal(localCommonInfoRepository.findById(421));

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
        TbUser updateTbUser = TbUser.builder()
                .userId(inviteeId)
                .invitationCode(InvitationCodeUtil.toSerialCode(inviteeId))
                .build();

        Invite invite1 = Invite.builder()
                .userId(userId)
                .inviteeUserId(inviteeId)
                .state(1)
                .money(BIGZERO)
                .createTime(executeTime)
                .userType(1)
                .build();
                
        StringBuilder sql = new StringBuilder();

        sql.append(JpaSqlBuilder.buildUpdateSql(updateTbUser, List.of("userId")));
        sql.append(JpaSqlBuilder.buildInsertSql(invite1));

        TbUser inviter = tbUserRepository.findByInvitationCode(user.getInviterCode())
                .orElse(null);
        if (inviter != null) {
            sql.append(JpaSqlBuilder.buildInsertSql(Invite.builder()
                    .userId(inviter.getUserId())
                    .inviteeUserId(inviteeId)
                    .state(1)
                    .money(BIGZERO)
                    .createTime(executeTime)
                    .userType(2)
                    .build()));
        }

        return JdbcDto.builder()
                .sql(sql.toString())
                .userId(inviteeId)
                .username(username)
                .build();
    }

    private JdbcDto buildJdbcVip1(MissionDetails details, int vipType) {
        Integer userId = details.getUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在: " + userId));

        String email = null;
        String phone = null;
        if (Math.random() > 0.55) {
            email = EmailGenerator.generateEmail(details.getLanguageType());
        } else {
            phone = PhoneGenerator.generatePhone(details.getLanguageType());
        }
        String username = maskUsername(email != null ? email : phone);

        LocalDateTime executeTime = details.getExecuteTime();
        LocalDateTime orderTime = offsetDateTime(executeTime, -60, -2);
        LocalDateTime registerTime = offsetDateTime(orderTime, -360, -30);

        BigDecimal rate1 = parseBigDecimal(localCommonInfoRepository.findById(420));
        BigDecimal rate2 = parseBigDecimal(localCommonInfoRepository.findById(421));

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
        UserMoneyDetails userMoneyDetailsCht = UserMoneyDetails.builder()
                .userId(userId)
                .title("[一級邀請佣金]一級好友名稱：" + username)
                .classify(2)
                .type(1)
                .state(1)
                .money(reward)
                .content("新增金額:" + reward)
                .createTime(executeTime)
                .languageType("cht")
                .build();
        Invite invite1 = Invite.builder()
                .userId(userId)
                .inviteeUserId(inviteeId)
                .state(1)
                .money(reward)
                .createTime(executeTime)
                .userType(1)
                .build();

        StringBuilder sql = new StringBuilder();
        sql.append(JpaSqlBuilder.buildUpdateSql(updateTbUser, List.of("userId")));
        sql.append(JpaSqlBuilder.buildInsertSql(userMoneyDetailsEn));
        sql.append(JpaSqlBuilder.buildInsertSql(userMoneyDetailsZh));
        sql.append(JpaSqlBuilder.buildInsertSql(userMoneyDetailsCht));
        sql.append(JpaSqlBuilder.buildInsertSql(invite1));
           
        sql.append("INSERT INTO `invite_money` (user_id, money_sum, money) VALUES (")
           .append(userId).append(", ").append(reward).append(", ").append(reward)
           .append(") ON DUPLICATE KEY UPDATE money_sum = money_sum + ")
           .append(reward).append(", money = money + ").append(reward).append(";");
        

        TbUser inviter = tbUserRepository.findByInvitationCode(user.getInviterCode())
                .orElse(null);
        if (inviter != null) {
            BigDecimal reward2 = BIGZERO;
            BigDecimal inviter2Rate2 = inviter.getTwoRate();
            if (inviter2Rate2 != null && inviter2Rate2.compareTo(BIGZERO) > 0) {
                reward2 = reward.multiply(inviter2Rate2).setScale(2, RoundingMode.HALF_UP);
                if (reward2.compareTo(BIGZERO) > 0) {
                    sql.append(JpaSqlBuilder.buildInsertSql(UserMoneyDetails.builder()
                            .userId(inviter.getUserId())
                            .title("[Second level invitation commission] Second level friend name: " + username)
                            .classify(2)
                            .type(1)
                            .state(1)
                            .money(reward2)
                            .content("Increase amount: " + reward2)
                            .createTime(executeTime)
                            .languageType("en")
                            .build()));
                    sql.append(JpaSqlBuilder.buildInsertSql(UserMoneyDetails.builder()
                            .userId(inviter.getUserId())
                            .title("[二级邀请佣金]二级好友名称：" + username)
                            .classify(2)
                            .type(1)
                            .state(1)
                            .money(reward2)
                            .content("增加金额:" + reward2)
                            .createTime(executeTime)
                            .languageType("zh")
                            .build()));
                            
                    sql.append("INSERT INTO `invite_money` (user_id, money_sum, money) VALUES (")
                       .append(inviter.getUserId()).append(", ").append(reward2).append(", ").append(reward2)
                       .append(") ON DUPLICATE KEY UPDATE money_sum = money_sum + ")
                       .append(reward2).append(", money = money + ").append(reward2).append(";");
                }
            }
            
            sql.append(JpaSqlBuilder.buildInsertSql(Invite.builder()
                    .userId(inviter.getUserId())
                    .inviteeUserId(inviteeId)
                    .state(1)
                    .money(reward2)
                    .createTime(executeTime)
                    .userType(2)
                    .build()));
        }

        return JdbcDto.builder()
                .sql(sql.toString())
                .userId(inviteeId)
                .username(username)
                .build();
    }

    private BigDecimal parseBigDecimal(Optional<LocalCommonInfo> commonInfo) {
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
        Integer userId = details.getUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在: " + userId));
    
        TbUser firstLevelInvitee = tbUserRepository.findOneRandom(user.getInvitationCode())
                .orElseThrow(() -> new IllegalArgumentException("该用户没有满足条件的一级邀请用户"));
    
        String email = null;
        String phone = null;
        if (Math.random() > 0.55) {
            email = EmailGenerator.generateEmail(details.getLanguageType());
        } else {
            phone = PhoneGenerator.generatePhone(details.getLanguageType());
        }
        String username = maskUsername(email != null ? email : phone);
    
        LocalDateTime executeTime = details.getExecuteTime();
        LocalDateTime orderTime = offsetDateTime(executeTime, -60, -2);
        LocalDateTime registerTime = offsetDateTime(orderTime, -300, -30);
    
        BigDecimal rate1 = parseBigDecimal(localCommonInfoRepository.findById(420));
        BigDecimal rate2 = parseBigDecimal(localCommonInfoRepository.findById(421));
    
        TbUser tbUser = TbUser.builder()
                .userName(username)
                .phone(phone)
                .emailName(email)
                .password(generatePassword())
                .createTime(registerTime)
                .status(1)
                .platform("h5.")
                .inviterCode(firstLevelInvitee.getInvitationCode())
                .rate(rate1)
                .twoRate(rate2)
                .qdCode("8888")
                .build();
    
        TbUser saveTbUser = tbUserRepository.save(tbUser);
        Integer inviteeId = saveTbUser.getUserId();
    
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
        UserMoneyDetails userMoneyDetailsCht2 = UserMoneyDetails.builder()
                .userId(userId)
                .title("[二級邀請佣金]二級好友名稱：" + username)
                .classify(2)
                .type(1)
                .state(1)
                .money(reward2)
                .content("新增金額:" + reward2)
                .createTime(executeTime)
                .languageType("cht")
                .build();
    
        Invite invite2 = Invite.builder()
                .userId(userId)  // 当前用户是二级邀请者
                .inviteeUserId(inviteeId)
                .state(1)
                .money(reward2)
                .createTime(executeTime)
                .userType(2)
                .build();
    
        StringBuilder sql = new StringBuilder();
        sql.append(JpaSqlBuilder.buildUpdateSql(updateTbUser, List.of("userId")));
        sql.append(JpaSqlBuilder.buildInsertSql(userMoneyDetailsEn2));
        sql.append(JpaSqlBuilder.buildInsertSql(userMoneyDetailsZh2));
        sql.append(JpaSqlBuilder.buildInsertSql(userMoneyDetailsCht2));
        sql.append(JpaSqlBuilder.buildInsertSql(invite2));

        sql.append("INSERT INTO `invite_money` (user_id, money_sum, money) VALUES (")
           .append(userId).append(", ").append(reward2).append(", ").append(reward2)
           .append(") ON DUPLICATE KEY UPDATE money_sum = money_sum + ")
           .append(reward2).append(", money = money + ").append(reward2).append(";");

        return JdbcDto.builder()
                .sql(sql.toString())
                .userId(inviteeId)
                .username(username)
                .build();
    }

    private JdbcDto buildJdbcSingle1(MissionDetails details) {
        Integer userId = details.getUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在: " + userId));

        LocalDateTime executeTime = details.getExecuteTime();
        LocalDateTime orderTime = offsetDateTime(executeTime, -60, -2);
        LocalDateTime registerTime = offsetDateTime(orderTime, -360, -30);

        BigDecimal rate1 = parseBigDecimal(localCommonInfoRepository.findById(420));
        BigDecimal rate2 = parseBigDecimal(localCommonInfoRepository.findById(421));

        Integer inviteeId = details.getInviteeId();
        String username = details.getInviteeName();
        BigDecimal reward = details.getMoney();
        
        List<MissionDetails> missionDetailsList = new ArrayList<>();
        StringBuilder sql = new StringBuilder();
        
        if (inviteeId == null) {
            String email = null;
            String phone = null;
            if (Math.random() > 0.55) {
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
                .rate(rate1)
                .twoRate(rate2)
                .qdCode("8888")
                .build();

            TbUser saveTbUser = tbUserRepository.save(tbUser);
            inviteeId = saveTbUser.getUserId();

            TbUser updateTbUser = TbUser.builder()
                .userId(inviteeId)
                .invitationCode(InvitationCodeUtil.toSerialCode(inviteeId))
                .build();

            Mission mission = missionRepository.findById(details.getMissionId())
                    .orElseThrow(() -> new IllegalArgumentException("任务不存在: " + details.getMissionId()));

            Integer continuous = details.getContinuous();
            LocalDateTime tempStartTime = executeTime;
            for (int i = continuous; i >= 1; i--) {
                tempStartTime = tempStartTime.plusSeconds(ThreadLocalRandom.current().nextInt(120, 240));
                missionDetailsList.add(MissionDetails.builder()
                        .userId(userId)
                        .missionId(mission.getMissionId())
                        .type(11)
                        .cost(details.getCost())
                        .rate(details.getRate())
                        .money(reward)
                        .inviteeId(inviteeId)
                        .inviteeName(username)
                        .continuous(0)
                        .executeTime(tempStartTime)
                        .languageType(details.getLanguageType())
                        .build());
            }

            sql.append(JpaSqlBuilder.buildUpdateSql(updateTbUser, List.of("userId")));
        }

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
        UserMoneyDetails userMoneyDetailsCht = UserMoneyDetails.builder()
                .userId(userId)
                .title("[一級邀請佣金]一級好友名稱：" + username)
                .classify(2)
                .type(1)
                .state(1)
                .money(reward)
                .content("新增金額:" + reward)
                .createTime(executeTime)
                .languageType("cht")
                .build();
        Invite invite1 = Invite.builder()
                .userId(userId)
                .inviteeUserId(inviteeId)
                .state(1)
                .money(reward)
                .createTime(executeTime)
                .userType(1)
                .build();

        sql.append(JpaSqlBuilder.buildInsertSql(userMoneyDetailsEn));
        sql.append(JpaSqlBuilder.buildInsertSql(userMoneyDetailsZh));
        sql.append(JpaSqlBuilder.buildInsertSql(userMoneyDetailsCht));
        sql.append(JpaSqlBuilder.buildInsertSql(invite1));
           
        sql.append("INSERT INTO `invite_money` (user_id, money_sum, money) VALUES (")
           .append(userId).append(", ").append(reward).append(", ").append(reward)
           .append(") ON DUPLICATE KEY UPDATE money_sum = money_sum + ")
           .append(reward).append(", money = money + ").append(reward).append(";");

        TbUser inviter = tbUserRepository.findByInvitationCode(user.getInviterCode())
                .orElse(null);
        if (inviter != null) {
            BigDecimal reward2 = BIGZERO;
            BigDecimal inviter2Rate2 = inviter.getTwoRate();
            if (inviter2Rate2 != null && inviter2Rate2.compareTo(BIGZERO) > 0) {
                reward2 = reward.multiply(inviter2Rate2).setScale(2, RoundingMode.HALF_UP);
                if (reward2.compareTo(BIGZERO) > 0) {
                    sql.append(JpaSqlBuilder.buildInsertSql(UserMoneyDetails.builder()
                            .userId(inviter.getUserId())
                            .title("[Second level invitation commission] Second level friend name: " + username)
                            .classify(2)
                            .type(1)
                            .state(1)
                            .money(reward2)
                            .content("Increase amount: " + reward2)
                            .createTime(executeTime)
                            .languageType("en")
                            .build()));
                    sql.append(JpaSqlBuilder.buildInsertSql(UserMoneyDetails.builder()
                            .userId(inviter.getUserId())
                            .title("[二级邀请佣金]二级好友名称：" + username)
                            .classify(2)
                            .type(1)
                            .state(1)
                            .money(reward2)
                            .content("增加金额:" + reward2)
                            .createTime(executeTime)
                            .languageType("zh")
                            .build()));
                            
                    sql.append("INSERT INTO `invite_money` (user_id, money_sum, money) VALUES (")
                       .append(inviter.getUserId()).append(", ").append(reward2).append(", ").append(reward2)
                       .append(") ON DUPLICATE KEY UPDATE money_sum = money_sum + ")
                       .append(reward2).append(", money = money + ").append(reward2).append(";");
                }
            }
            
            sql.append(JpaSqlBuilder.buildInsertSql(Invite.builder()
                    .userId(inviter.getUserId())
                    .inviteeUserId(inviteeId)
                    .state(1)
                    .money(reward2)
                    .createTime(executeTime)
                    .userType(2)
                    .build()));
        }

        if (missionDetailsList.isEmpty()) {
            return JdbcDto.builder()
                    .sql(sql.toString())
                    .userId(inviteeId)
                    .username(username)
                    .build();
        } else {
            return JdbcDto.builder()
                    .sql(sql.toString())
                    .userId(inviteeId)
                    .username(username)
                    .missionDetailsList(missionDetailsList)
                    .build();
        }
    }

    private JdbcDto buildJdbcSingle2(MissionDetails details) {
        Integer userId = details.getUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在: " + userId));
    
        TbUser firstLevelInvitee = tbUserRepository.findOneRandom(user.getInvitationCode())
                .orElseThrow(() -> new IllegalArgumentException("该用户没有满足条件的一级邀请用户"));
    
        LocalDateTime executeTime = details.getExecuteTime();
        LocalDateTime orderTime = offsetDateTime(executeTime, -60, -2);
        LocalDateTime registerTime = offsetDateTime(orderTime, -360, -30);

        BigDecimal rate1 = parseBigDecimal(localCommonInfoRepository.findById(420));
        BigDecimal rate2 = parseBigDecimal(localCommonInfoRepository.findById(421));

        Integer inviteeId = details.getInviteeId();
        String username = details.getInviteeName();
        BigDecimal reward2 = details.getMoney();
        
        List<MissionDetails> missionDetailsList = new ArrayList<>();
        StringBuilder sql = new StringBuilder();
        
        if (inviteeId == null) {
            String email = null;
            String phone = null;
            if (Math.random() > 0.55) {
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
                .inviterCode(firstLevelInvitee.getInvitationCode())
                .rate(rate1)
                .twoRate(rate2)
                .qdCode("8888")
                .build();

            TbUser saveTbUser = tbUserRepository.save(tbUser);
            inviteeId = saveTbUser.getUserId();

            TbUser updateTbUser = TbUser.builder()
                .userId(inviteeId)
                .invitationCode(InvitationCodeUtil.toSerialCode(inviteeId))
                .build();

            Mission mission = missionRepository.findById(details.getMissionId())
                    .orElseThrow(() -> new IllegalArgumentException("任务不存在: " + details.getMissionId()));

            Integer continuous = details.getContinuous();
            LocalDateTime tempStartTime = executeTime;
            for (int i = continuous; i >= 1; i--) {
                    tempStartTime = tempStartTime.plusSeconds(ThreadLocalRandom.current().nextInt(120, 240));
                    missionDetailsList.add(MissionDetails.builder()
                        .userId(userId)
                        .missionId(mission.getMissionId())
                        .type(12)
                        .cost(details.getCost())
                        .rate(details.getRate())
                        .money(reward2)
                        .inviteeId(inviteeId)
                        .inviteeName(username)
                        .continuous(0)
                        .executeTime(tempStartTime)
                        .languageType(details.getLanguageType())
                        .build());
            }

            sql.append(JpaSqlBuilder.buildUpdateSql(updateTbUser, List.of("userId")));
        }
    
        // 构建二级邀请奖励数据
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
        UserMoneyDetails userMoneyDetailsCht2 = UserMoneyDetails.builder()
                .userId(userId)
                .title("[二級邀請佣金]二級好友名稱：" + username)
                .classify(2)
                .type(1)
                .state(1)
                .money(reward2)
                .content("新增金額:" + reward2)
                .createTime(executeTime)
                .languageType("cht")
                .build();
    
        Invite invite2 = Invite.builder()
                .userId(userId)  // 当前用户是二级邀请者
                .inviteeUserId(inviteeId)
                .state(1)
                .money(reward2)
                .createTime(executeTime)
                .userType(2)
                .build();
    
        sql.append(JpaSqlBuilder.buildInsertSql(userMoneyDetailsEn2));
        sql.append(JpaSqlBuilder.buildInsertSql(userMoneyDetailsZh2));
        sql.append(JpaSqlBuilder.buildInsertSql(userMoneyDetailsCht2));
        sql.append(JpaSqlBuilder.buildInsertSql(invite2));

        sql.append("INSERT INTO `invite_money` (user_id, money_sum, money) VALUES (")
           .append(userId).append(", ").append(reward2).append(", ").append(reward2)
           .append(") ON DUPLICATE KEY UPDATE money_sum = money_sum + ")
           .append(reward2).append(", money = money + ").append(reward2).append(";");

        if (missionDetailsList.isEmpty()) {
            return JdbcDto.builder()
                    .sql(sql.toString())
                    .userId(inviteeId)
                    .username(username)
                    .build();
        } else {
            return JdbcDto.builder()
                    .sql(sql.toString())
                    .userId(inviteeId)
                    .username(username)
                    .missionDetailsList(missionDetailsList)
                    .build();
        }
    }

    private JdbcDto buildJdbcAll1(MissionDetails details) {
        Integer userId = details.getUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在: " + userId));

        String email = null;
        String phone = null;
        if (Math.random() > 0.55) {
            email = EmailGenerator.generateEmail(details.getLanguageType());
        } else {
            phone = PhoneGenerator.generatePhone(details.getLanguageType());
        }
        String username = maskUsername(email != null ? email : phone);

        LocalDateTime executeTime = details.getExecuteTime();
        LocalDateTime orderTime = offsetDateTime(executeTime, -60, -2);
        LocalDateTime registerTime = offsetDateTime(orderTime, -360, -30);

        BigDecimal rate1 = parseBigDecimal(localCommonInfoRepository.findById(420));
        BigDecimal rate2 = parseBigDecimal(localCommonInfoRepository.findById(421));

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
        UserMoneyDetails userMoneyDetailsCht = UserMoneyDetails.builder()
                .userId(userId)
                .title("[一級邀請佣金]一級好友名稱：" + username)
                .classify(2)
                .type(1)
                .state(1)
                .money(reward)
                .content("新增金額:" + reward)
                .createTime(executeTime)
                .languageType("cht")
                .build();
        Invite invite1 = Invite.builder()
                .userId(userId)
                .inviteeUserId(inviteeId)
                .state(1)
                .money(reward)
                .createTime(executeTime)
                .userType(1)
                .build();

        StringBuilder sql = new StringBuilder();
        sql.append(JpaSqlBuilder.buildUpdateSql(updateTbUser, List.of("userId")));
        sql.append(JpaSqlBuilder.buildInsertSql(userMoneyDetailsEn));
        sql.append(JpaSqlBuilder.buildInsertSql(userMoneyDetailsZh));
        sql.append(JpaSqlBuilder.buildInsertSql(userMoneyDetailsCht));
        sql.append(JpaSqlBuilder.buildInsertSql(invite1));
           
        sql.append("INSERT INTO `invite_money` (user_id, money_sum, money) VALUES (")
           .append(userId).append(", ").append(reward).append(", ").append(reward)
           .append(") ON DUPLICATE KEY UPDATE money_sum = money_sum + ")
           .append(reward).append(", money = money + ").append(reward).append(";");
        

        TbUser inviter = tbUserRepository.findByInvitationCode(user.getInviterCode())
                .orElse(null);
        if (inviter != null) {
            BigDecimal reward2 = BIGZERO;
            BigDecimal inviter2Rate2 = inviter.getTwoRate();
            if (inviter2Rate2 != null && inviter2Rate2.compareTo(BIGZERO) > 0) {
                reward2 = reward.multiply(inviter2Rate2).setScale(2, RoundingMode.HALF_UP);
                if (reward2.compareTo(BIGZERO) > 0) {
                    sql.append(JpaSqlBuilder.buildInsertSql(UserMoneyDetails.builder()
                            .userId(inviter.getUserId())
                            .title("[Second level invitation commission] Second level friend name: " + username)
                            .classify(2)
                            .type(1)
                            .state(1)
                            .money(reward2)
                            .content("Increase amount: " + reward2)
                            .createTime(executeTime)
                            .languageType("en")
                            .build()));
                    sql.append(JpaSqlBuilder.buildInsertSql(UserMoneyDetails.builder()
                            .userId(inviter.getUserId())
                            .title("[二级邀请佣金]二级好友名称：" + username)
                            .classify(2)
                            .type(1)
                            .state(1)
                            .money(reward2)
                            .content("增加金额:" + reward2)
                            .createTime(executeTime)
                            .languageType("zh")
                            .build()));
                            
                    sql.append("INSERT INTO `invite_money` (user_id, money_sum, money) VALUES (")
                       .append(inviter.getUserId()).append(", ").append(reward2).append(", ").append(reward2)
                       .append(") ON DUPLICATE KEY UPDATE money_sum = money_sum + ")
                       .append(reward2).append(", money = money + ").append(reward2).append(";");
                }
            }
            
            sql.append(JpaSqlBuilder.buildInsertSql(Invite.builder()
                    .userId(inviter.getUserId())
                    .inviteeUserId(inviteeId)
                    .state(1)
                    .money(reward2)
                    .createTime(executeTime)
                    .userType(2)
                    .build()));
        }

        return JdbcDto.builder()
                .sql(sql.toString())
                .userId(inviteeId)
                .username(username)
                .build();
    }

    private JdbcDto buildJdbcAll2(MissionDetails details) {
        Integer userId = details.getUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在: " + userId));
    
        TbUser firstLevelInvitee = tbUserRepository.findOneRandom(user.getInvitationCode())
                .orElseThrow(() -> new IllegalArgumentException("该用户没有满足条件的一级邀请用户"));
    
        String email = null;
        String phone = null;
        if (Math.random() > 0.55) {
            email = EmailGenerator.generateEmail(details.getLanguageType());
        } else {
            phone = PhoneGenerator.generatePhone(details.getLanguageType());
        }
        String username = maskUsername(email != null ? email : phone);
    
        LocalDateTime executeTime = details.getExecuteTime();
        LocalDateTime orderTime = offsetDateTime(executeTime, -60, -2);
        LocalDateTime registerTime = offsetDateTime(orderTime, -300, -30);
    
        BigDecimal rate1 = parseBigDecimal(localCommonInfoRepository.findById(420));
        BigDecimal rate2 = parseBigDecimal(localCommonInfoRepository.findById(421));
    
        TbUser tbUser = TbUser.builder()
                .userName(username)
                .phone(phone)
                .emailName(email)
                .password(generatePassword())
                .createTime(registerTime)
                .status(1)
                .platform("h5.")
                .inviterCode(firstLevelInvitee.getInvitationCode())
                .rate(rate1)
                .twoRate(rate2)
                .qdCode("8888")
                .build();
    
        TbUser saveTbUser = tbUserRepository.save(tbUser);
        Integer inviteeId = saveTbUser.getUserId();
    
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
        UserMoneyDetails userMoneyDetailsCht2 = UserMoneyDetails.builder()
                .userId(userId)
                .title("[二級邀請佣金]二級好友名稱：" + username)
                .classify(2)
                .type(1)
                .state(1)
                .money(reward2)
                .content("新增金額:" + reward2)
                .createTime(executeTime)
                .languageType("cht")
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
        sql.append(JpaSqlBuilder.buildInsertSql(userMoneyDetailsCht2));
        sql.append(JpaSqlBuilder.buildInsertSql(invite2));

        sql.append("INSERT INTO `invite_money` (user_id, money_sum, money) VALUES (")
           .append(userId).append(", ").append(reward2).append(", ").append(reward2)
           .append(") ON DUPLICATE KEY UPDATE money_sum = money_sum + ")
           .append(reward2).append(", money = money + ").append(reward2).append(";");

        return JdbcDto.builder()
                .sql(sql.toString())
                .userId(inviteeId)
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

    private LocalDateTime offsetDateTime(LocalDateTime dateTime, int minSeconds, int maxSeconds) {
        int offsetSeconds = ThreadLocalRandom.current().nextInt(minSeconds, maxSeconds);
        return dateTime.plusSeconds(offsetSeconds);
    }
}
