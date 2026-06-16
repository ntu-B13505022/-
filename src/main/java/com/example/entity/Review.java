package com.example.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "review")
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String content; // 評價內容
    
    private Integer rating; // 評價星級 (1-5)
    private Boolean anonymous = false;
    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product; // 評價對應的商品
    
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user; // 評價的使用者
    
    // Getter 和 Setter
    public Long getId() { return id; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }
    
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    
    public Boolean getAnonymous() { return anonymous; }
    public void setAnonymous(Boolean anonymous) { this.anonymous = anonymous; }
}