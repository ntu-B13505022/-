package com.example.controller;

import com.example.entity.User;
import com.example.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

@Controller
public class LoginController {

    @Autowired
    private UserRepository userRepository;

    // 1. 顯示登入網頁
    @GetMapping("/login")
    public String showLoginForm() {
        return "login"; 
    }

    // 2. 處理登入驗證
    @PostMapping("/login")
    public String loginUser(@RequestParam String username, 
                            @RequestParam String password, 
                            HttpSession session, 
                            Model model) {
        System.out.println("====== 🔑 收到登入請求： " + username + " ======");

        Optional<User> userOpt = userRepository.findByUsername(username);

        if (userOpt.isPresent() && userOpt.get().getPassword().equals(password)) {
            session.setAttribute("currentUser", userOpt.get());
            System.out.println("🎉 登入成功！當前登入者為：" + username);
            return "redirect:/"; 
        }

        System.out.println("❌ 登入失敗：帳號或密碼錯誤");
        model.addAttribute("loginError", "帳號或密碼輸入錯誤，請再試一次！");
        return "login";
    }

    // 3. 顯示註冊網頁
    @GetMapping("/register")
    public String showRegisterForm() {
        return "register"; 
    }

    // 4. 處理會員註冊（不分買賣家，統一註冊為 MEMBER）
    @PostMapping("/register")
    public String registerUser(@RequestParam String username, 
                               @RequestParam String password, 
                               @RequestParam String email, 
                               Model model) {
        System.out.println("====== 📝 收到全新註冊請求，帳號：" + username + " ======");

        //  這裡已經精準修正，沒有多餘的括號了！
        if (userRepository.findByUsername(username).isPresent()) {
            System.out.println("❌ 註冊失敗：帳號已存在");
            model.addAttribute("registerError", "該帳號已被佔用，請換一個名字！");
            return "register";
        }

        User newUser = new User();
        newUser.setUsername(username);
        newUser.setPassword(password);
        newUser.setEmail(email);
        newUser.setRole("MEMBER"); 

        userRepository.save(newUser);
        System.out.println("🎉 帳號 [" + username + "] 註冊成功，已持久化寫入資料庫！");

        return "redirect:/login";
    }

    // 5. 處理登出
    @GetMapping("/logout")
    public String logoutUser(HttpSession session) {
        session.invalidate();
        System.out.println("👋 使用者已安全登出");
        return "redirect:/";
    }
}