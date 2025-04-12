package com.valorburst.service.impl;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.valorburst.model.local.User;
import com.valorburst.model.remote.projection.UserRemoteProjection;
import com.valorburst.repository.local.UserRepository;
import com.valorburst.repository.remote.TbUserRepository;
import com.valorburst.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService{

    private static final int BATCHSIZE = 100;

    private final UserRepository userRepository;
    private final TbUserRepository tbUserRepository;

    @Override
    public void syncAllUsers() {
        List<Integer> userIds = userRepository.fetchAllUserIds();
        for (int i = 0; i < userIds.size(); i += BATCHSIZE) {
            int end = Math.min(i + BATCHSIZE, userIds.size());
            List<Integer> batchUserIds = userIds.subList(i, end);
            List<User> users = tbUserRepository.findProjectedByUserIds(batchUserIds)
                    .stream()
                    .map(this::mapToLocal)
                    .toList();
            userRepository.saveAll(users);
        }
    }

    // 获取所有用户
    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public Optional<User> updateUserByInput(String input) {
        Optional<UserRemoteProjection> userOpt = Optional.empty();
        if (input.startsWith("+")) {
            userOpt = tbUserRepository.findProjectedByPhone(input);
        } else if (input.contains("@")) {
            userOpt = tbUserRepository.findProjectedByEmail(input);
        } else if (input.length() == 6) {
            userOpt = tbUserRepository.findProjectedByInvitationCode(input);
        }

        if (userOpt.isPresent()) {
            UserRemoteProjection user = userOpt.get();
            User localUser = mapToLocal(user);
            userRepository.save(localUser);
            return Optional.of(localUser);
        } else {
            return Optional.empty();
        }
    }

    private User mapToLocal(UserRemoteProjection p) {
        BigDecimal bigZero = BigDecimal.ZERO;
        return User.builder()
                .userId(p.getUserId())
                .userName(p.getUserName())
                .phone(p.getPhone())
                .emailName(p.getEmailName())
                .platform(p.getPlatform())
                .invitationCode(p.getInvitationCode())
                .inviterCode(p.getInviterCode())
                .rate(p.getRate() != null ? p.getRate() : bigZero)
                .twoRate(p.getTwoRate() != null ? p.getTwoRate() : bigZero)
                .moneySum(p.getMoneySum() != null ? p.getMoneySum() : bigZero)
                .money(p.getMoney() != null ? p.getMoney() : bigZero)
                .cashOut(p.getCashOut() != null ? p.getCashOut() : bigZero)
                .cashOutStay(p.getCashOutStay() != null ? p.getCashOutStay() : bigZero)
                .moneyWallet(p.getMoneyWallet() != null ? p.getMoneyWallet() : bigZero)
                .build();
    }
}
