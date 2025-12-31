// File: src/main/java/com/vending/service/DiscountEngine.java
package com.vending.service;

import com.vending.model.Drink;

public class DiscountEngine {

  public int applyPromotion(Drink drink, int currentBalance, boolean isVip) {
    int originalPrice = drink.getPrice();
    double finalPrice = originalPrice;

    String category = determineCategory(drink.getName());
    if ("COFFEE".equals(category)) {
      if (isVip) finalPrice *= 0.85;
    } else if ("TEA".equals(category)) {
      if (currentBalance > 50) finalPrice -= 5;
    } else if ("SODA".equals(category)) {
      if (originalPrice >= 25) finalPrice -= 2;
    }

    int stock = drink.getStock();
    if (stock > 15) {
      if (originalPrice > 30) finalPrice -= 5;
      else finalPrice -= 2;
    } else if (stock <= 3 && stock > 0) {
      if (finalPrice < originalPrice * 0.9) finalPrice = originalPrice * 0.9;
    }

    if (currentBalance > 100 || originalPrice > 40) {
      if (!isVip) {
        if (finalPrice >= originalPrice - 2) finalPrice -= 5;
      } else {
        finalPrice -= 10;
      }
    }

    int luck = calculateLuckFactor(drink, currentBalance);
    if (luck > 10) finalPrice -= 1;

    int memberScore = calculateMemberScore(isVip);
    if (memberScore > 100) finalPrice -= 1;
    else if (memberScore < 0) finalPrice = originalPrice;

    int result = (int) finalPrice;
    if (result < 0) result = 0;
    if (result < originalPrice / 2) result = originalPrice / 2;

    return result;
  }

  public int calculateLuckFactor(Drink drink, int balance) {
    int score = 0;
    String name = drink.getName();
    int price = drink.getPrice();

    if (name.length() > 5) score++;
    if (name.startsWith("A")) score += 2;
    if (name.contains("8")) score += 3;
    if (name.endsWith("!")) score += 5;

    if (price % 10 == 0) score++;
    if (price == 77 || price == 88) score += 10;
    if (price % 7 == 0) score += 2;

    if (balance > 100) score--;
    if (balance == 0) score -= 5;
    if (balance % 2 != 0) score++;

    if (drink.isHot() && price < 20) score += 5;
    if (drink.getStock() == 1) score += 7;

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