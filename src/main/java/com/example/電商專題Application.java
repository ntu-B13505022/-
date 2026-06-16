package com.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class 電商專題Application {

    public static void main(String[] args) {
        // 🌟 清除未使用的變數宣告，直接執行
        SpringApplication.run(電商專題Application.class, args);
        openHomePage();
    }

    private static void openHomePage() {
        if (java.awt.Desktop.isDesktopSupported()) {
            try {
                // 🚀 自動幫你導向由 Java 渲染的主網址
                java.awt.Desktop.getDesktop().browse(new java.net.URI("http://localhost:8081/"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}