package com.example.repository;

import com.example.entity.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    
    // 🔍 找出該使用者的所有收藏
    List<Favorite> findByUserId(Long userId);

    // 🔍 檢查某使用者是否已經收藏了某商品
    boolean existsByUserIdAndProductId(Long userId, Long productId);

    // ❌ 刪除某使用者的特定商品收藏
    @Transactional
    void deleteByUserIdAndProductId(Long userId, Long productId);
}