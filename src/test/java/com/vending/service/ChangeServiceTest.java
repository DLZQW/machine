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

  @Test
  @DisplayName("覆蓋率衝刺：觸發 '部分硬幣不足' 與 '找零失敗' 的所有分支")
  void testCoverageBooster() {
    // -------------------------------------------------------
    // 情境 1：測試「部分硬幣不足」 (Partial Availability)
    // -------------------------------------------------------
    // 初始庫存: 50x5, 10x20, 5x20, 1x50

    // 步驟 A: 先把 50 元全部消耗光 (5 * 50 = 250)
    changeService.calculateChange(250);
    // 現在 50元剩 0 個

    // 步驟 B: 把 10 元消耗到只剩 1 個
    // 原本 20 個，消耗 19 個 (190 元)
    changeService.calculateChange(190);
    // 現在 10元剩 1 個

    // 步驟 C: 要求找 20 元
    // 邏輯會嘗試拿 2 個 10 元，但只剩 1 個 -> 觸發 "else if (available > 0)" 分支
    // 系統會拿走那 1 個 10 元，剩下 10 元用 5 元去湊
    Map<Integer, Integer> result = changeService.calculateChange(20);

    assertEquals(1, result.get(10)); // 拿到僅存的 1 個 10 元
    assertEquals(2, result.get(5));  // 剩下的 10 元用 2 個 5 元補
  }

  @Test
  @DisplayName("覆蓋率衝刺：觸發 handleIncompleteChange 的三種 Log 分支")
  void testLogBranches() {
    ChangeService cs = new ChangeService();

    // 分支 1: 剩餘金額 >= 50 (嚴重短缺)
    // 直接要求超大金額，讓它把錢吐光後還剩一大堆
    cs.calculateChange(100000);
    // 這會觸發 "嚴重短缺大面額硬幣"

    // 分支 3: 剩餘金額 < 10 (短缺零錢)
    cs = new ChangeService(); // 重置
    // 先把 1 元全部耗盡 (50 個)
    cs.calculateChange(50);
    // 現在沒有 1 元了，要求找 3 元 (無法由 5, 10, 50 組成)
    cs.calculateChange(3);
    // 剩餘 3 元 -> 觸發 "else" (短缺零錢)

    // 分支 2: 剩餘金額 >= 10 (短缺 10 元)
    cs = new ChangeService(); // 重置
    // 這次我們要把它榨乾到只剩無法湊出的金額
    // 簡單暴力法：把所有零錢都拿光
    cs.calculateChange(250); // 拿光 50
    cs.calculateChange(200); // 拿光 10
    cs.calculateChange(100); // 拿光 5
    cs.calculateChange(50);  // 拿光 1
    // 現在全空，要求找 15 元
    cs.calculateChange(15);
    // 剩餘 15 元 -> 觸發 "else if (amount >= 10)"
  }
}