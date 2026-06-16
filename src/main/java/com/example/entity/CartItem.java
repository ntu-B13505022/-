package com.example.entity;

public class CartItem {
    private Product product;
    private int quantity;

    // 建構子 (Constructor)
    public CartItem(Product product, int quantity) {
        this.product = product;
        this.quantity = quantity;
    }

    // Getter & Setter
    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
    
 // 🌟 手動加上一個完全不帶參數的空建構子
    public CartItem() {
        // 裡面留空沒關係，純粹用來讓 new CartItem() 可以合法過關
    }
}