package com.example.repository;

import com.example.entity.OrderDetail;
import com.example.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrderDetailRepository extends JpaRepository<OrderDetail, Long> {
    // 透過訂單裡面的 User 來找明細
    List<OrderDetail> findByOrderUser(User user);
}