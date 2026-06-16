package com.example.controller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.entity.CartItem;
import com.example.entity.Order;
import com.example.entity.OrderDetail;
import com.example.entity.Product;
import com.example.entity.Review;
import com.example.entity.User;
import com.example.repository.FavoriteRepository;
import com.example.repository.OrderDetailRepository;
import com.example.repository.OrderRepository;
import com.example.repository.ProductRepository;
import com.example.repository.ReviewRepository;

import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;

@Controller
public class ProductController {

    @Autowired private ProductRepository productRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private OrderDetailRepository orderDetailRepository;
    @Autowired private ReviewRepository reviewRepository;
    

    // --- 1. 首頁 ---
 // 原本首頁的 Mapping
    @GetMapping("/")
    public String index(@RequestParam(value = "keyword", required = false) String keyword, Model model) {
        List<Product> products;
        
        if (keyword != null && !keyword.isEmpty()) {
            // 🌟 只搜尋上架中的商品
            products = productRepository.findByActiveTrueAndNameContaining(keyword);
        } else {
            // 🌟 沒有關鍵字時，也只撈出上架中的商品
            products = productRepository.findByActiveTrue();
        }
        
        model.addAttribute("products", products);
        model.addAttribute("isIndex", true);
        return "index";
    }
    // --- 2. 商品細節 ---
 // 記得要在 ProductController 最上方注入 FavoriteRepository 哦！
    @Autowired
    private FavoriteRepository favoriteRepository; 

    @GetMapping("/product/detail/{id}")
    public String detail(@PathVariable Long id, Model model, HttpSession session) {
        Product p = productRepository.findById(id).orElse(null);
        model.addAttribute("product", p);
        model.addAttribute("reviews", reviewRepository.findByProductId(id));
        
        // 取得當前使用者
        User user = (User) session.getAttribute("currentUser");
        
        // 1. 收藏狀態檢查
        boolean isFavorited = false;
        if (user != null) {
            isFavorited = favoriteRepository.existsByUserIdAndProductId(user.getId(), id);
        }
        model.addAttribute("isFavorited", isFavorited); 
        
        // 2. 評論狀態檢查
        boolean hasReviewed = false;
        if (user != null) {
            hasReviewed = reviewRepository.existsByUserIdAndProductId(user.getId(), id);
        }
        model.addAttribute("hasReviewed", hasReviewed);
        
        // 3. 🌟 新增：購買狀態檢查 (確保你在類別上方有注入 orderRepository)
        boolean hasPurchased = false;
        if (user != null) {
            hasPurchased = orderRepository.existsByUserIdAndProductId(user.getId(), id);
        }
        model.addAttribute("hasPurchased", hasPurchased);
        
        return "detail"; // 確保你的頁面檔名是 detail.html
    }
    // --- 3. 購物車查看 ---
    @GetMapping("/cart")
    public String viewCart(HttpSession session, Model model) {
        Object sessionCart = session.getAttribute("cart");
        List<CartItem> cart = (sessionCart instanceof List<?>) ? (List<CartItem>) sessionCart : new ArrayList<>();
        model.addAttribute("cartItems", cart);
        return "cart";
    }
    
 // 🌟 把這段直接加進你原本的 ProductController.java 裡面
    @PostMapping("/cart/remove")
    public String removeFromCart(@RequestParam("productId") Long productId, HttpSession session) {
        List<com.example.entity.CartItem> cart = (List<com.example.entity.CartItem>) session.getAttribute("cart");
        
        if (cart != null) {
            // 根據 ID 移除商品
            cart.removeIf(item -> item.getProduct().getId().equals(productId));
            session.setAttribute("cart", cart);
        }
        
        return "redirect:/cart";
    }

 // --- 4. 加入購物車 (完全對齊 AJAX) ---
    @PostMapping("/cart/add")
    @ResponseBody
    public String addToCart(@RequestParam Long productId, HttpSession session) {
        if (session.getAttribute("currentUser") == null) {
            return "NEED_LOGIN"; 
        }

        Product product = productRepository.findById(productId).orElse(null);
        if (product != null && product.getStock() > 0) {
            
            // 💡 【防呆機制】：安全地讀取購物車
            List<CartItem> cart = new ArrayList<>();
            Object sessionCart = session.getAttribute("cart");
            
            if (sessionCart instanceof List) {
                List<?> tempList = (List<?>) sessionCart;
                // 檢查購物車是不是空的，以及裡面裝的是不是正確的 CartItem
                if (!tempList.isEmpty() && tempList.get(0) instanceof CartItem) {
                    cart = (List<CartItem>) tempList;
                } else if (!tempList.isEmpty()) {
                    System.out.println("⚠️ 偵測到格式不符的舊版購物車，已自動清除重置！");
                    // 如果裝的是舊資料，就會直接使用上面 new 出來的空 ArrayList
                }
            }

            // 尋找是否已有相同商品
            boolean found = false;
            for (CartItem item : cart) {
                if (item.getProduct().getId().equals(product.getId())) {
                    item.setQuantity(item.getQuantity() + 1);
                    found = true;
                    break;
                }
            }
            
            // 如果沒有，就新增一筆
            if (!found) {
                cart.add(new CartItem(product, 1));
            }
            
            // 把更新後的購物車存回 Session
            session.setAttribute("cart", cart);
            
            // 回傳成功訊息與最新數量
         // ❌ 請替換原本的 return "SUCCESS:" + cart.size();
         // ✅ 改成下方這段：計算購物車內所有物品的數量總和
         int totalQuantity = cart.stream().mapToInt(CartItem::getQuantity).sum();
         return "SUCCESS:" + totalQuantity; 
        }
        
        return "OUT_OF_STOCK";
    }
    // --- 5. 結帳功能 ---
    @PostMapping("/checkout")
    @Transactional
    public String checkout(HttpSession session, RedirectAttributes redirectAttributes,
                           @RequestParam String receiverName,
                           @RequestParam String receiverPhone,
                           @RequestParam String receiverAddress) {

        User user = (User) session.getAttribute("currentUser");
        if (user == null) {
            redirectAttributes.addFlashAttribute("error", "請先登入才能結帳！");
            return "redirect:/login";
        }

        Object sessionCart = session.getAttribute("cart");
        List<CartItem> cart = (sessionCart instanceof List<?>) ? (List<CartItem>) sessionCart : new ArrayList<>();

        if (cart.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "購物車是空的！");
            return "redirect:/cart";
        }

        Order order = new Order();
        order.setOrderDate(LocalDateTime.now());
        order.setUser(user);
        order.setReceiverName(receiverName);
        order.setReceiverPhone(receiverPhone);
        order.setReceiverAddress(receiverAddress);
        order.setTotalPrice(cart.stream().mapToDouble(i -> i.getProduct().getPrice() * i.getQuantity()).sum());
        
        Order savedOrder = orderRepository.save(order);

     // 4. 處理訂單明細
        for (CartItem item : cart) {
            Product p = item.getProduct();
            int qty = item.getQuantity();

            System.out.println(">>> 正在檢查商品: " + p.getName() + ", 現有庫存: " + p.getStock() + ", 購買數量: " + qty);

            if (p.getStock() >= qty) {
                p.setStock(p.getStock() - qty);
                productRepository.save(p);
                
                OrderDetail detail = new OrderDetail();
                detail.setOrder(savedOrder);
                detail.setProduct(p);
                detail.setQuantity(qty);
                orderDetailRepository.save(detail);
                
                System.out.println(">>> 明細已寫入資料庫！");
            } else {
                System.out.println(">>> 庫存不足，跳過此商品。");
            }
        }
        session.removeAttribute("cart");
        redirectAttributes.addFlashAttribute("message", "結帳成功！");
        return "redirect:/";
    }

 // --- 6. 評價功能 (已整合「購買後才能評論」防護) ---
    @PostMapping("/product/review")
    public String addReview(HttpSession session, 
                            @RequestParam Long productId, 
                            @RequestParam String content, 
                            @RequestParam Integer rating,
                            @RequestParam(defaultValue = "false") Boolean anonymous) { // 🌟 接收前端的匿名勾選狀態
        
        User user = (User) session.getAttribute("currentUser");
        if (user == null) return "redirect:/login";
        
        // 🔒 關卡 1：核心安全防護 —— 檢查買家是否真的有購買過該商品
        boolean hasPurchased = orderRepository.existsByUserIdAndProductId(user.getId(), productId);
        if (!hasPurchased) {
            // 沒買過就無情彈回，並帶上錯誤標記
            return "redirect:/product/detail/" + productId + "?error=not_purchased";
        }
        
        // 🌟 關卡 2：後端防安全漏洞 —— 如果已經評過分，直接彈回，不給重複寫入
        if (reviewRepository.existsByUserIdAndProductId(user.getId(), productId)) {
            return "redirect:/product/detail/" + productId;
        }
        
        Review r = new Review();
        r.setContent(content);
        r.setRating(rating);
        r.setUser(user);
        r.setProduct(productRepository.findById(productId).orElse(null));
        r.setAnonymous(anonymous); // 🌟 存入是否匿名
        
        reviewRepository.save(r);
        return "redirect:/product/detail/" + productId;
    }
    
 // 📦 1. 顯示「上架商品」的網頁表單
    @GetMapping("/product/add")
    public String showAddProductForm(Model model) {
        // 產生一個空的 Product 實體，準備讓前端表單綁定資料
        model.addAttribute("product", new Product());
        return "add-product"; 
    }

    // 🚀 2. 接收表單送出的資料，並存入資料庫
    @PostMapping("/product/add")
    public String addProduct(Product product, HttpSession session) {
        // 從 Session 取得目前登入的會員物件
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login"; // 沒登入就導回登入頁
        }
        
        // 🌟 綁定賣家 ID 與預設上架狀態
        product.setSellerId(currentUser.getId());
        product.setActive(true); 
        
        productRepository.save(product);
        return "redirect:/product/manage";
    }
    /**
     * 1. 進入商品管理頁面
     */
    
    @GetMapping("/product/manage")
    public String manageProducts(HttpSession session, Model model) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }
        
        // 🌟 改用 findBySellerId，只撈出自己的商品
        List<Product> myProducts = productRepository.findBySellerId(currentUser.getId());
        model.addAttribute("products", myProducts);
        
        return "manage"; // 導向你的商品管理 HTML
    }
    @PostMapping("/product/toggle-active/{id}")
    public String toggleProductActive(@PathVariable Long id, HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        Product product = productRepository.findById(id).orElse(null);
        
        // 安全檢查：只有當商品存在、使用者已登入，且該商品確實屬於目前登入的會員時，才能修改狀態
        if (product != null && currentUser != null && product.getSellerId().equals(currentUser.getId())) {
            // 🌟 切換狀態：如果是 true 就變 false (下架)，如果是 false 就變 true (上架)
            product.setActive(!product.isActive());
            productRepository.save(product);
        }
        
        return "redirect:/product/manage"; // 執行完後重新整理管理頁面
    }
    /**
     * 2. 處理補貨的 AJAX 請求
     */
    @PostMapping("/product/restock")
    @ResponseBody
    public String restockProduct(@RequestParam("productId") Long productId, // 🌟 改成 Long 才能對應你的實體
                                 @RequestParam("addQuantity") Integer addQuantity,
                                 HttpSession session) {
        
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "NEED_LOGIN";
        }

        try {
            // 🌟 改用 productRepository 透過 ID 找到商品
            Product product = productRepository.findById(productId).orElse(null);
            
            if (product != null) {
                // 將原有庫存加上補貨數量
                int newStock = product.getStock() + addQuantity;
                product.setStock(newStock);
                
                // 🌟 改用 productRepository 儲存進資料庫
                productRepository.save(product); 
                
                return "SUCCESS:" + newStock;
            } else {
                return "FAIL:找不到商品";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR";
        }
    }
 // 1. 顯示修改商品的畫面
    @GetMapping("/product/edit/{id}")
    public String showEditForm(@PathVariable("id") Long id, org.springframework.ui.Model model, jakarta.servlet.http.HttpSession session) {
        // 【安全檢查】沒登入的人不能進來
        var currentUser = session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }

        // 撈出該商品資料
        Product product = productRepository.findById(id).orElse(null);
        if (product == null) {
            return "redirect:/"; // 商品不存在就導回首頁
        }

        // model.addAttribute 傳遞給前端 edit.html 渲染
        model.addAttribute("product", product);
        return "edit"; // 這會去開啟 src/main/resources/templates/edit.html
    }

    // 2. 接收修改後的表單資料並儲存
    @PostMapping("/product/edit/{id}")
    public String processEdit(@PathVariable("id") Long id, @ModelAttribute("product") Product updatedProduct, jakarta.servlet.http.HttpSession session) {
        // 【安全檢查】確認有登入
        var currentUser = session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }

        // 找出資料庫原有的舊資料
        Product existingProduct = productRepository.findById(id).orElse(null);
        if (existingProduct == null) {
            return "redirect:/";
        }

        // 將前端傳過來的新資料，覆蓋掉舊資料
        existingProduct.setName(updatedProduct.getName());
        existingProduct.setDescription(updatedProduct.getDescription());
        existingProduct.setPrice(updatedProduct.getPrice());
        existingProduct.setStock(updatedProduct.getStock());
        existingProduct.setImageUrl(updatedProduct.getImageUrl());
        existingProduct.setActive(updatedProduct.isActive()); // 這邊連帶可以控制要上架還是下架！

        // 儲存回資料庫
        productRepository.save(existingProduct);

        // 修改完成後，導回該商品的詳情頁（⚠️ 請根據你原本詳情頁的網址去改，如果是 /product/{id} 就改 /product/）
        return "redirect:/product/detail/" + id; 
    }
}