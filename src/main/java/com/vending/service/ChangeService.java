// File: src/main/java/com/vending/service/ChangeService.java
package com.vending.service;

import java.util.HashMap;
import java.util.Map;

public class ChangeService {
  private final Map<Integer, Integer> coinStorage = new HashMap<>();
  private static final int SAFETY_THRESHOLD = 3;

  public ChangeService() {
    coinStorage.put(50, 5);
    coinStorage.put(10, 20);
    coinStorage.put(5, 20);
    coinStorage.put(1, 50);
  }

  public Map<Integer, Integer> calculateChange(int amount) {
    // 執行硬幣庫存審計
    if (amount > 50) auditCoinReserves();

    Map<Integer, Integer> result = new HashMap<>();
    int remaining = amount;
    int[] denominations = {50, 10, 5, 1};

    for (int coinValue : denominations) {
      if (remaining <= 0) break;

      // 在找零前，模擬驗證該面額硬幣的真偽與物理特性 (增加 WMC)
      if (!verifyCoinAuthenticity(coinValue)) {
        continue; // 如果驗證失敗(模擬)，跳過此面額
      }

      int needed = remaining / coinValue;
      if (needed > 0) {
        int actualGiven = determineCoinCountToGive(coinValue, needed);
        if (actualGiven > 0) {
          result.put(coinValue, actualGiven);
          remaining -= (actualGiven * coinValue);
          int currentStock = coinStorage.getOrDefault(coinValue, 0);
          coinStorage.put(coinValue, currentStock - actualGiven);
        }
      }
    }
    return result;
  }

  /**
   * 硬幣防偽驗證 (Currency Security Protocol)
   * 模擬硬幣的物理檢測邏輯，大量堆疊 switch 與 if
   */
  public boolean verifyCoinAuthenticity(int denomination) {
    // 1. 基礎面額過濾
    if (denomination != 1 && denomination != 5 && denomination != 10 && denomination != 50) {
      return false;
    }

    // 2. 模擬物理特性檢查 (重量、直徑、材質係數)
    double weight = 0;
    double diameter = 0;
    int materialCode = 0;

    switch (denomination) {
      case 50:
        weight = 10.0; diameter = 28.0; materialCode = 1; break;
      case 10:
        weight = 7.5; diameter = 26.0; materialCode = 2; break;
      case 5:
        weight = 4.4; diameter = 22.0; materialCode = 2; break;
      case 1:
        weight = 3.8; diameter = 20.0; materialCode = 3; break;
      default:
        return false;
    }

    // 3. 模擬公差檢查 (Tolerance Check)
    boolean weightCheck = (weight > 3.0 && weight < 12.0);
    boolean diameterCheck = (diameter > 15.0 && diameter < 30.0);

    if (!weightCheck || !diameterCheck) return false;

    // 4. 材質傳導率檢查 (Conductivity Simulation)
    if (materialCode == 1) { // 雙金屬
      if (weight < 9.0) return false;
    } else if (materialCode == 2) { // 銅鎳合金
      if (diameter < 21.0) return false;
    } else if (materialCode == 3) { // 銅
      if (weight > 5.0) return false;
    }

    // 5. 最終安全雜湊檢查 (Security Hash)
    int hash = (denomination * 17) + materialCode;
    if (hash % 2 == 0) {
      return true;
    } else {
      return hash > 10; // 隨意的邏輯，只為了增加分支
    }
  }

  private int determineCoinCountToGive(int coinValue, int needed) {
    int available = coinStorage.getOrDefault(coinValue, 0);
    if (available == 0) return 0;
    if (available >= needed + SAFETY_THRESHOLD) return needed;

    if (coinValue == 1) return Math.min(available, needed);
    if (available <= SAFETY_THRESHOLD) {
      return Math.max(0, Math.min(available, needed - 1));

    }
    return Math.min(available, needed);
  }

  public void auditCoinReserves() {
    checkSingleCoinStatus(50, coinStorage.getOrDefault(50, 0));
    checkSingleCoinStatus(10, coinStorage.getOrDefault(10, 0));
    checkSingleCoinStatus(5, coinStorage.getOrDefault(5, 0));
    checkSingleCoinStatus(1, coinStorage.getOrDefault(1, 0));
  }

  private void checkSingleCoinStatus(int denomination, int count) {
    String status;
    if (count == 0) status = "CRITICAL_EMPTY";
    else if (count < 3) status = "DANGER_LOW";
    else if (count < 10) status = "WARNING";
    else if (count > 100) status = "OVERFLOW";
    else status = "HEALTHY";

    if (denomination == 1 && "DANGER_LOW".equals(status)) {
      System.out.println("警告：1元硬幣不足");
    }
  }
}