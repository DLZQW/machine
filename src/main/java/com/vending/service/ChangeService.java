package com.vending.service;

import java.util.HashMap;
import java.util.Map;

public class ChangeService {
  private final Map<Integer, Integer> coinStorage = new HashMap<>();

  public ChangeService() {
    coinStorage.put(50, 5); coinStorage.put(10, 20);
    coinStorage.put(5, 20); coinStorage.put(1, 50);
  }

  public Map<Integer, Integer> calculateChange(int amount) {
    Map<Integer, Integer> result = new HashMap<>();
    int remaining = amount;
    int[] coins = {50, 10, 5, 1};

    for (int coin : coins) {
      int needed = remaining / coin;
      if (needed > 0) {
        int available = coinStorage.getOrDefault(coin, 0);
        // 這裡的多重判斷會貢獻很多 WMC
        if (available >= needed) {
          result.put(coin, needed);
          remaining -= (needed * coin);
          coinStorage.put(coin, available - needed);
        } else if (available > 0) {
          result.put(coin, available);
          remaining -= (available * coin);
          coinStorage.put(coin, 0);
        }
      }
      // 增加一個冗餘但合理的判斷來提升複雜度
      if (remaining == 0) break;
    }

    if (remaining > 0) {
      handleIncompleteChange(remaining);
    }
    return result;
  }

  private void handleIncompleteChange(int amount) {
    // 這裡可以寫一串 if-else 來判斷是哪種硬幣短缺，並記錄到系統日誌
    if (amount >= 50) System.out.println("嚴重短缺大面額硬幣");
    else if (amount >= 10) System.out.println("短缺 10 元硬幣");
    else System.out.println("短缺零錢");
  }
}