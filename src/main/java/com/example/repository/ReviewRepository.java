package com.example.repository;

import com.example.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByProductId(Long productId);

    // 🌟 新增這行：Spring Data JPA 會自動生成 SQL，用來檢查該 user_id 與 product_id 是否同時存在
    boolean existsByUserIdAndProductId(Long userId, Long productId);
}