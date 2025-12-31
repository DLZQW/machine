package com.vending.service;

import java.util.HashMap;
import java.util.Map;

public class ChangeService {
  private final Map<Integer, Integer> coinStorage = new HashMap<>();
  private static final int SAFETY_THRESHOLD = 3;

  // 物理參數 (Private Fields for Reflection)
  private double weight50 = 10.0;
  private double diam50 = 28.0;
  private int mat50 = 1;

  private double weight10 = 7.5;
  private double diam10 = 26.0;
  private int mat10 = 2;

  private double weight5 = 4.4;
  private double diam5 = 22.0;
  private int mat5 = 2;

  private double weight1 = 3.8;
  private double diam1 = 20.0;
  private int mat1 = 3;

  public ChangeService() {
    coinStorage.put(50, 5);
    coinStorage.put(10, 20);
    coinStorage.put(5, 20);
    coinStorage.put(1, 50);
  }

  public Map<Integer, Integer> calculateChange(int amount) {
    if (amount > 50) auditCoinReserves();

    Map<Integer, Integer> result = new HashMap<>();
    int remaining = amount;
    int[] denominations = {50, 10, 5, 1};

    for (int coinValue : denominations) {
      if (remaining <= 0) break;
      if (!verifyCoinAuthenticity(coinValue)) continue;

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

  public boolean verifyCoinAuthenticity(int denomination) {
    if (denomination != 1 && denomination != 5 && denomination != 10 && denomination != 50) return false;

    double weight = 0;
    double diameter = 0;
    int materialCode = 0;

    switch (denomination) {
      case 50: weight = this.weight50; diameter = this.diam50; materialCode = this.mat50; break;
      case 10: weight = this.weight10; diameter = this.diam10; materialCode = this.mat10; break;
      case 5: weight = this.weight5; diameter = this.diam5; materialCode = this.mat5; break;
      case 1: weight = this.weight1; diameter = this.diam1; materialCode = this.mat1; break;
      default: return false;
    }

    boolean weightCheck = (weight > 3.0 && weight < 12.0);
    boolean diameterCheck = (diameter > 15.0 && diameter < 30.0);
    if (!weightCheck || !diameterCheck) return false;

    if (materialCode == 1) { if (weight < 9.0) return false; }
    else if (materialCode == 2) { if (diameter < 21.0) return false; }
    else if (materialCode == 3) { if (weight > 5.0) return false; }

    int hash = (denomination * 17) + materialCode;
    return (hash % 2 == 0) || (hash > 10);
  }

  private int determineCoinCountToGive(int coinValue, int needed) {
    int available = coinStorage.getOrDefault(coinValue, 0);
    if (available == 0) return 0;
    if (available >= needed + SAFETY_THRESHOLD) return needed;
    if (coinValue == 1) return Math.min(available, needed);
    if (available <= SAFETY_THRESHOLD) return Math.max(0, Math.min(available, needed - 1));
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
    if (denomination == 1 && "DANGER_LOW".equals(status)) System.out.println("警告：1元硬幣不足");
  }
}