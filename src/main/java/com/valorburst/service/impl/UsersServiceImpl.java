package com.valorburst.service.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.valorburst.model.local.User;
import com.valorburst.model.remote.projection.UserRemoteProjection;
import com.valorburst.repository.local.UserRepository;
import com.valorburst.repository.remote.TbUserRepository;
import com.valorburst.service.UsersService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UsersServiceImpl implements UsersService{

    private final UserRepository userRepository;
    private final TbUserRepository tbUserRepository;

    @Override
    public List<User> syncAllUsers() {
        List<Integer> userIds = userRepository.fetchAllUserIds();
        List<UserRemoteProjection> users = tbUserRepository.findProjectedByUserIds(userIds);
        List<User> localUsers = users.stream().map(this::mapToLocal).toList();
        userRepository.saveAll(localUsers);
        return localUsers;
    }

    @Override
    public Optional<User> updateUserByInput(String input) {
        Optional<UserRemoteProjection> userOpt = input.startsWith("+")
                ? tbUserRepository.findProjectedByPhone(input)
                : input.contains("@")
                    ? tbUserRepository.findProjectedByEmail(input)
                    : Optional.empty();

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
        return User.builder()
                .userId(p.getUserId())
                .userName(p.getUserName())
                .phone(p.getPhone())
                .emailName(p.getEmailName())
                .platform(p.getPlatform())
                .invitationCode(p.getInvitationCode())
                .inviterCode(p.getInviterCode())
                .rate(p.getRate() != null ? p.getRate() : 0.0)
                .twoRate(p.getTwoRate() != null ? p.getTwoRate() : 0.0)
                .moneySum(p.getMoneySum() != null ? p.getMoneySum() : 0.0)
                .money(p.getMoney() != null ? p.getMoney() : 0.0)
                .cashOut(p.getCashOut() != null ? p.getCashOut() : 0.0)
                .cashOutStay(p.getCashOutStay() != null ? p.getCashOutStay() : 0.0)
                .moneyWallet(p.getMoneyWallet() != null ? p.getMoneyWallet() : 0.0)
                .build();
    }
}
