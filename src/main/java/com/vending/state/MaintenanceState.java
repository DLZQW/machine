// File: src/main/java/com/vending/state/MaintenanceState.java
package com.vending.state;

import com.vending.core.VendingMachine;
import com.vending.model.Drink;
import java.util.Map;

public class MaintenanceState implements VendingMachineState {
  private final VendingMachine machine;

  // Reflection targets
  private boolean wifi = true;
  private boolean sim4g = true;
  private int currentVoltage = 110;
  private int currentTemp = 4;
  private int coinMechCleanliness = 98;

  public MaintenanceState(VendingMachine machine) {
    this.machine = machine;
  }

  @Override
  public void insertCoin(int amount) {
    System.out.println("【維護中】系統鎖定，退還硬幣: " + amount);
  }

  @Override
  public void selectDrink(String drinkId) {
    Drink drink = machine.getInventory().get(drinkId);
    if (drink != null) {
      System.out.println("【手動補貨】" + drink.getName());
      drink.setStock(10);
    } else {
      System.out.println("【錯誤】查無此 ID: " + drinkId);
    }
  }

  @Override
  public void dispense() {
    System.out.println("【系統自檢】啟動深度硬體掃描...");
    performSubsystemCheck("POWER_UNIT");
    performSubsystemCheck("COOLING_SYSTEM");
    performSubsystemCheck("COIN_MECH");
    performSubsystemCheck("DISPENSER_MOTOR");
    performSubsystemCheck("CONNECTIVITY");
    testAllSlots();
  }

  @Override
  public void cancel() {
    System.out.println("【系統】維護完成。");
    machine.setState(machine.getIdleState());
  }

  @Override
  public void maintenance(String password) {
    System.out.println("已在維護模式中。");
    analyzeInventoryHealth();
    int estimatedCost = estimateMaintenanceCost();
    System.out.println("維修成本: $" + estimatedCost);
  }

  private void performSubsystemCheck(String systemCode) {
    switch (systemCode) {
      case "POWER_UNIT":
        if (checkVoltage(this.currentVoltage)) System.out.println("電壓穩定");
        else System.out.println("警報：電壓異常");
        break;
      case "COOLING_SYSTEM":
        if (this.currentTemp > 10) System.out.println("警報：溫度過高");
        else if (this.currentTemp < 0) System.out.println("警報：結霜風險");
        else System.out.println("冷藏功能正常");
        break;
      case "COIN_MECH":
        System.out.println("清潔度: " + coinMechCleanliness + "%");
        break;
      case "DISPENSER_MOTOR":
        System.out.println("馬達: OK");
        break;
      case "CONNECTIVITY":
        if (wifi && sim4g) System.out.println("雙網路正常");
        else if (wifi) System.out.println("僅 Wi-Fi");
        else if (sim4g) System.out.println("僅 4G");
        else System.out.println("警報：離線");
        break;
      default:
        System.out.println("未知子系統");
    }
  }

  private boolean checkVoltage(int v) {
    return v >= 100 && v <= 120;
  }

  private void testAllSlots() {
    for (Map.Entry<String, Drink> entry : machine.getInventory().entrySet()) {
      if (entry.getValue().getStock() > 0) System.out.println("正常");
      else System.out.println("略過");
    }
  }

  private void analyzeInventoryHealth() {
    for (Drink d : machine.getInventory().values()) {
      double rps = calculateRPS(d);
      if (rps > 100) System.out.println("緊急補貨");
      else if (rps > 50) System.out.println("需關注");
    }
  }

  private double calculateRPS(Drink d) {
    int missing = 10 - d.getStock();
    double score = missing * 10.0;
    if (d.getPrice() >= 30) score *= 1.5;
    if (d.isHot()) score += 20;
    if (d.getStock() == 0) score += 50;
    return score;
  }

  private int estimateMaintenanceCost() {
    int cost = 500;
    long emptySlots = machine.getInventory().values().stream().filter(d -> d.getStock() == 0).count();
    if (emptySlots > 3) cost += 300;
    else if (emptySlots > 0) cost += 100;
    if (machine.getBalance() < 100) cost += 200;
    return cost;
  }
}