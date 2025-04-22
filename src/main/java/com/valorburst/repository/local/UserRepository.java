package com.valorburst.repository.local;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.valorburst.model.local.User;

public interface UserRepository extends JpaRepository<User, Integer>, UserRepositoryCustom {
    
    @Query(value = """
        SELECT user_id FROM user
        """, nativeQuery = true)
    List<Integer> fetchAllUserIds();
}
