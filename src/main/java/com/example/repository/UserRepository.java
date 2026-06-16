package com.example.repository;

import com.example.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    // pring Data JPA 把這個方法轉化為 SQL 指令
    // 這樣在註冊時，就能先檢查帳號是否已經被別人註冊過了，防止資料庫持久化時噴出 Unique 衝突錯誤
    Optional<User> findByUsername(String username);
}