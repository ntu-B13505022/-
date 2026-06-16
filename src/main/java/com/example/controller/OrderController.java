package com.example.controller;

import com.example.entity.CartItem;
import com.example.entity.Order;
import com.example.entity.OrderDetail; // 確保引入明細
import com.example.entity.Product;
import com.example.entity.User;
import com.example.repository.OrderRepository;
import com.example.repository.ProductRepository;
import com.example.repository.OrderDetailRepository; // 確保引入明細 Repo
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;

@Controller
public class OrderController {

    @Autowired private OrderRepository orderRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private OrderDetailRepository orderDetailRepository; // 注入明細工具

    // 1. 處理結帳下單（同一筆消費綁在同個訂單裡）
    @PostMapping("/order/checkout")
    public String checkout(HttpSession session,
                           @RequestParam("receiverName") String receiverName,
                           @RequestParam("receiverPhone") String receiverPhone,
                           @RequestParam("receiverAddress") String receiverAddress,
                           Model model) {

        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) return "redirect:/login";

        List<CartItem> cart = (List<CartItem>) session.getAttribute("cart");
        if (cart == null || cart.isEmpty()) return "redirect:/cart";

        try {
            // 建立一張主訂單，用來記錄收件資訊
            Order order = new Order();
            order.setUser(currentUser);
            order.setReceiverName(receiverName);
            order.setReceiverPhone(receiverPhone);
            order.setReceiverAddress(receiverAddress);
            order.setTotalPrice(0.0); // 先設為 0，等一下算完商品再填入
            
            // 先儲存主訂單，讓資料庫生成這個訂單的唯一 ID
            order = orderRepository.save(order);

            double totalOrderPrice = 0.0;

            // 跑迴圈把購物車的每件商品，變成這張主訂單底下的明細
            for (CartItem item : cart) {
                Product p = item.getProduct();
                int buyQuantity = item.getQuantity();

                Product dbProduct = productRepository.findById(p.getId())
                        .orElseThrow(() -> new RuntimeException("找不到商品"));
                
                if (dbProduct.getStock() < buyQuantity) {
                    model.addAttribute("error", "【" + dbProduct.getName() + "】庫存不足！");
                    return "cart";
                }

                // 扣減庫存
                dbProduct.setStock(dbProduct.getStock() - buyQuantity);
                productRepository.save(dbProduct); 

                // 建立明細並綁定剛剛存好的主訂單
                OrderDetail detail = new OrderDetail();
                detail.setOrder(order); // ⭐ 關鍵：將明細指向同一個主訂單
                detail.setProduct(dbProduct);
                detail.setQuantity(buyQuantity);
                
                orderDetailRepository.save(detail); // 寫入明細表

                // 累加這筆大訂單的總金額
                totalOrderPrice += dbProduct.getPrice() * buyQuantity;
            }

            // 將計算出來的最終總金額更新回主訂單
            order.setTotalPrice(totalOrderPrice);
            orderRepository.save(order); 

            session.setAttribute("cart", new ArrayList<CartItem>()); 
            return "redirect:/orders";

        } catch (Exception e) {
            model.addAttribute("error", "結帳失敗：" + e.getMessage());
            return "cart";
        }
    }

    //  查看歷史訂單（撈取主訂單，前端再撈明細）
    @GetMapping("/orders")
    public String viewOrderHistory(HttpSession session, Model model) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) return "redirect:/login";

        // 撈出該會員的所有主訂單
        List<Order> myOrders = orderRepository.findByUser(currentUser);

        // 計算歷史累積總消費
        double totalSpend = myOrders.stream()
                .mapToDouble(Order::getTotalPrice)
                .sum();

        model.addAttribute("orders", myOrders); // 傳遞主訂單列表
        model.addAttribute("totalSpend", totalSpend);
        
        return "orders"; 
    }
    
    @GetMapping("/seller/orders")
    public String showSellerOrders(jakarta.servlet.http.HttpSession session, org.springframework.ui.Model model) {
        // 1. 安全檢查，必須登入才能看
        var currentUser = (User) session.getAttribute("currentUser"); 
        if (currentUser == null) {
            return "redirect:/login";
        }

        // 2. 根據目前登入的賣家 ID，撈出屬於他的買家訂單
        List<Order> sellerOrders = orderRepository.findOrdersBySellerId(currentUser.getId());

        // 3. 傳送到前端頁面
        model.addAttribute("orders", sellerOrders);
        
        return "seller_orders"; // 這會去開啟 templates/seller_orders.html
    }
}