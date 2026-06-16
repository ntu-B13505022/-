package com.example.repository;

import com.example.entity.Order;
import com.example.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    // 買家功能：查詢自己的訂單
    List<Order> findByUser(User user);
    
    // 加上 FETCH讓 Order 和 OrderDetail 一起被撈出來
    @Query("SELECT DISTINCT o FROM Order o JOIN FETCH o.orderDetails d WHERE d.product.sellerId = :sellerId ORDER BY o.id DESC")
    List<Order> findOrdersBySellerId(@Param("sellerId") Long sellerId);
    
    // 檢查買家是否購買過該商品
    @Query("SELECT COUNT(od) > 0 FROM OrderDetail od WHERE od.order.user.id = :userId AND od.product.id = :productId")
    boolean existsByUserIdAndProductId(@Param("userId") Long userId, @Param("productId") Long productId);
    
    
}