package com.vending.service;

import com.vending.model.Drink;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class DiscountServiceTest {

  @Test
  public void testGetDiscountedPrice() {
    DiscountService discountService = new DiscountService();

    // --- 修正這裡 ---
    // 錯誤訊息說需要: String, String, int, int, boolean
    // 這裡填入假資料: 名稱="Coke", 描述="Cola", 價格=100, 庫存/數值=10, 布林值=true
    Drink drink = new Drink("Coke", "Cola", 100, 10, true);

    // 測試買 1 瓶 (不打折) -> 預期 100 元
    int price1 = discountService.getDiscountedPrice(drink, 1);
    assertEquals(100, price1);

    // 測試買 2 瓶 (打 9 折) -> 200 * 0.9 = 180 元
    int price2 = discountService.getDiscountedPrice(drink, 2);
    assertEquals(180, price2);
  }
}