package com.vending.service;

import com.vending.model.Drink;

public class DiscountEngine {
  public int applyPromotion(Drink drink, int currentBalance, boolean isVip) {
    int price = drink.getPrice();

    // 條件一：VIP 且金額超過 50 打八折
    if (isVip && price >= 50) {
      return (int)(price * 0.8);
    }

    // 條件二：餘額過高（大於100）且購買高價品，折 5 元
    if (currentBalance > 100 || price > 40) {
      if (!isVip) return price - 5;
      else return price - 10;
    }

    // 條件三：特定商品折扣
    if (drink.getName().contains("咖啡") || drink.getName().contains("Tea")) {
      return price - 2;
    }

    return price;
  }
}