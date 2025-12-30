package com.vending.service;

import com.vending.model.Drink;

public class DiscountService {

  /**
   * 計算折扣後的價格
   * @param drink 飲料物件
   * @param quantity 購買數量
   * @return 折扣後的總金額
   */
  public int getDiscountedPrice(Drink drink, int quantity) {
    // 取得單價 (假設 Drink 有 getPrice 方法)
    int price = drink.getPrice();

    // 計算原始總價
    int total = price * quantity;

    // --- 折扣規則 ---
    // 例如：買 2 瓶以上，打 9 折
    if (quantity >= 2) {
      return (int) (total * 0.9);
    }

    // 沒達到折扣門檻，回傳原價
    return total;
  }
}