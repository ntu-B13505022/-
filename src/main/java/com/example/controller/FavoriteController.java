package com.example.controller;

import com.example.entity.Favorite;
import com.example.entity.User;
import com.example.repository.FavoriteRepository;
import com.example.repository.ProductRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class FavoriteController {

    @Autowired
    private FavoriteRepository favoriteRepository;

    @Autowired
    private ProductRepository productRepository;

    // 顯示收藏清單頁面
    @GetMapping("/favorites")
    public String showFavorites(HttpSession session, Model model) {
        User user = (User) session.getAttribute("currentUser");
        if (user == null) return "redirect:/login"; // 沒登入就踢去登入

        List<Favorite> favorites = favoriteRepository.findByUserId(user.getId());
        model.addAttribute("favorites", favorites);
        return "favorites"; // 對應 templates/favorites.html
    }

    // 加入收藏
    @PostMapping("/favorite/add")
    public String addFavorite(@RequestParam Long productId, HttpSession session) {
        User user = (User) session.getAttribute("currentUser");
        if (user == null) return "redirect:/login";

        // 防重複收藏機制
        if (!favoriteRepository.existsByUserIdAndProductId(user.getId(), productId)) {
            Favorite fav = new Favorite();
            fav.setUser(user);
            fav.setProduct(productRepository.findById(productId).orElse(null));
            favoriteRepository.save(fav);
        }
        return "redirect:/product/detail/" + productId;
    }

    // 取消收藏 (可從商品頁或收藏清單頁面觸發)
    @PostMapping("/favorite/remove")
    public String removeFavorite(@RequestParam Long productId, 
                                 @RequestParam(required = false) String from, 
                                 HttpSession session) {
        User user = (User) session.getAttribute("currentUser");
        if (user == null) return "redirect:/login";

        favoriteRepository.deleteByUserIdAndProductId(user.getId(), productId);

        // 如果是從收藏清單頁面點擊刪除的，就導向回收藏頁面；否則導向回商品明細頁
        if ("list".equals(from)) {
            return "redirect:/favorites";
        }
        return "redirect:/product/detail/" + productId;
    }
}