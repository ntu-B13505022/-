package com.example.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "product")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private Double price;
    private Integer stock;
    private String description;
    private String imageUrl; // 注意：Java習慣用駝峰命名，MySQL是 image_url

    // 🌟 若欄位名稱對不上，請加上 @Column 指定
    @Column(name = "image_url") 
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    // 其他標準 Getter 和 Setter...
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
 // 1. 記錄是哪位會員上架的（假設會員的主鍵是 Long id）
    private Long sellerId; 

    // 2. 記錄商品狀態：true 代表上架中，false 代表已下架
    private boolean active = true;

    public Long getSellerId() {
        return sellerId;
    }

    public void setSellerId(Long sellerId) {
        this.sellerId = sellerId;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
    
	    
}