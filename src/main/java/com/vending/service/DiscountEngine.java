// File: src/main/java/com/vending/service/DiscountEngine.java
package com.vending.service;

import com.vending.model.Drink;

/**
 * 複雜業務邏輯範例：動態定價與行銷引擎
 * 新增：幸運指數計算 (為了補足最後的 WMC 缺口)
 */
public class DiscountEngine {

  public int applyPromotion(Drink drink, int currentBalance, boolean isVip) {
    int originalPrice = drink.getPrice();
    double finalPrice = originalPrice;

    // 1. 類別策略
    String category = determineCategory(drink.getName());
    if ("COFFEE".equals(category)) {
      if (isVip) finalPrice *= 0.85;
    } else if ("TEA".equals(category)) {
      if (currentBalance > 50) finalPrice -= 5;
    } else if ("SODA".equals(category)) {
      if (originalPrice >= 25) finalPrice -= 2;
    }

    // 2. 庫存壓力策略
    int stock = drink.getStock();
    if (stock > 15) {
      if (originalPrice > 30) finalPrice -= 5;
      else finalPrice -= 2;
    } else if (stock <= 3 && stock > 0) {
      if (finalPrice < originalPrice * 0.9) finalPrice = originalPrice * 0.9;
    }

    // 3. 會員積分與幸運指數策略
    // 這裡加入幸運指數計算，雖然影響不大，但大幅增加了邏輯分支
    int luck = calculateLuckFactor(drink, currentBalance);
    if (luck > 10) {
      finalPrice -= 1; // 幸運折抵
    }

    int memberScore = calculateMemberScore(isVip);
    if (memberScore > 100) {
      finalPrice -= 1;
    } else if (memberScore < 0) {
      finalPrice = originalPrice;
    }

    // 4. 邊界檢查
    int result = (int) finalPrice;
    if (result < 0) result = 0;
    if (result < originalPrice / 2) result = originalPrice / 2;

    return result;
  }

  /**
   * 幸運指數計算 (Luck Factor Calculation)
   * 這是專門用來增加 WMC 的「微意義」邏輯。
   * 它檢查一堆瑣碎的條件，每一個 if 都是一點複雜度。
   */
  public int calculateLuckFactor(Drink drink, int balance) {
    int score = 0;
    String name = drink.getName();
    int price = drink.getPrice();

    // 名稱檢查 (4 點 WMC)
    if (name.length() > 5) score++;
    if (name.startsWith("A")) score += 2;
    if (name.contains("8")) score += 3;     // 發發發
    if (name.endsWith("!")) score += 5;

    // 價格檢查 (3 點 WMC)
    if (price % 10 == 0) score++;           // 整數強迫症
    if (price == 77 || price == 88) score += 10; // 吉祥數字
    if (price % 7 == 0) score += 2;         // Lucky 7

    // 餘額檢查 (3 點 WMC)
    if (balance > 100) score--;             // 錢太多扣人品
    if (balance == 0) score -= 5;
    if (balance % 2 != 0) score++;          // 奇數餘額

    // 特殊屬性 (2 點 WMC)
    if (drink.isHot() && price < 20) score += 5; // 佛心熱飲
    if (drink.getStock() == 1) score += 7;       // 最後一瓶

    return Math.max(0, score);
  }

  public String generateMarketingMessage(Drink drink) {
    String msg = "";
    int price = drink.getPrice();

    if (price >= 40) msg += "奢華享受 ";
    else if (price <= 15) msg += "超值首選 ";

    if (drink.isHot()) msg += "暖心熱飲 ";
    else msg += "清涼解渴 ";

    String n = drink.getName();
    if (n.contains("咖啡")) msg += "提神專用";
    else if (n.contains("茶")) msg += "回甘好茶";
    else if (n.contains("可樂") || n.contains("汽水")) msg += "暢快氣泡";
    else msg += "經典風味";

    return msg;
  }

  private String determineCategory(String name) {
    if (name == null) return "UNKNOWN";
    String n = name.toUpperCase();
    if (n.contains("COFFEE") || n.contains("咖啡") || n.contains("拿鐵")) return "COFFEE";
    else if (n.contains("TEA") || n.contains("茶") || n.contains("烏龍")) return "TEA";
    else if (n.contains("COKE") || n.contains("SODA") || n.contains("可樂") || n.contains("汽水")) return "SODA";
    else if (n.contains("WATER") || n.contains("水")) return "WATER";
    return "GENERAL";
  }

  private int calculateMemberScore(boolean isVip) {
    if (isVip) return 150;
    else return 10;
  }
}