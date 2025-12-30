package com.vending.service;

import com.vending.service.ChangeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ChangeServiceTest {
  private final ChangeService changeService = new ChangeService();

  @Test
  @DisplayName("找零測試：剛好找完")
  void testCalculateChangeExact() {
    Map<Integer, Integer> result = changeService.calculateChange(66);
    // 50x1, 10x1, 5x1, 1x1
    assertEquals(1, result.get(50));
    assertEquals(1, result.get(10));
    assertEquals(1, result.get(5));
    assertEquals(1, result.get(1));
  }

  @Test
  @DisplayName("找零測試：大面額不足時應由小面額替補")
  void testCalculateChangeWithLimitedCoins() {
    // 你可以修改 ChangeService 內的庫存來模擬這種情況
    // 這是達成 90% 覆蓋率的關鍵點
  }
}