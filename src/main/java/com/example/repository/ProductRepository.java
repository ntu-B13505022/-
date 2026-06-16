package com.example.repository;

import com.example.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    
  
    List<Product> findByNameContainingIgnoreCase(String keyword);
    
    // 1. 用來在商品管理頁面，只撈出目前登入會員自己上架的商品
    List<Product> findBySellerId(Long sellerId);
    
    // 2. 用來在首頁，只顯示真正上架中的商品（排除已下架的）
    List<Product> findByActiveTrue();
    
    // 3. 如果首頁有搜尋關鍵字功能，也要過濾掉下架商品
    List<Product> findByActiveTrueAndNameContaining(String keyword);
}