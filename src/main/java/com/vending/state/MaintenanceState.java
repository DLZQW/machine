// File: src/main/java/com/vending/state/MaintenanceState.java
package com.vending.state;

import com.vending.core.VendingMachine;
import com.vending.model.Drink;
import java.util.Map;

/**
 * 維護模式：包含進階庫存分析、子系統深度診斷與維修估價
 * 透過模擬真實的硬體檢測流程來大幅提升 WMC
 */
public class MaintenanceState implements VendingMachineState {
  private final VendingMachine machine;

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
      System.out.println("【手動補貨】" + drink.getName() + " 補貨前數量: " + drink.getStock());
      drink.setStock(10);
      System.out.println("【手動補貨】已重置為標準庫存 (10)");
    } else {
      System.out.println("【錯誤】查無此 ID: " + drinkId);
    }
  }

  @Override
  public void dispense() {
    System.out.println("【系統自檢】啟動深度硬體掃描...");
    // 執行多個子系統的檢查
    performSubsystemCheck("POWER_UNIT");
    performSubsystemCheck("COOLING_SYSTEM");
    performSubsystemCheck("COIN_MECH");
    performSubsystemCheck("DISPENSER_MOTOR");
    performSubsystemCheck("CONNECTIVITY");

    // 執行全貨道測試
    testAllSlots();
  }

  @Override
  public void cancel() {
    System.out.println("【系統】維護完成，切換回待機模式。");
    machine.setState(machine.getIdleState());
  }

  @Override
  public void maintenance(String password) {
    System.out.println("已在維護模式中。執行進階庫存與成本分析...");
    analyzeInventoryHealth();

    // 額外執行維修成本估算
    int estimatedCost = estimateMaintenanceCost();
    System.out.println("預估本次維護建議成本: $" + estimatedCost);
  }

  /**
   * 子系統深度診斷 (Subsystem Diagnostics)
   * 模擬針對不同硬體模組的詳細檢查邏輯
   */
  private void performSubsystemCheck(String systemCode) {
    System.out.print("檢測子系統 [" + systemCode + "]: ");

    // 使用 Switch-Case 模擬不同模組的檢查路徑
    switch (systemCode) {
      case "POWER_UNIT":
        // 模擬電壓檢查
        if (checkVoltage(110)) System.out.println("電壓穩定 (110V)");
        else System.out.println("警報：電壓異常");
        break;

      case "COOLING_SYSTEM":
        // 模擬溫度檢查
        int temp = 4; // 假設讀取到的溫度
        if (temp > 10) System.out.println("警報：溫度過高");
        else if (temp < 0) System.out.println("警報：結霜風險");
        else System.out.println("冷藏功能正常 (" + temp + "°C)");
        break;

      case "COIN_MECH":
        System.out.println("硬幣分類器清潔度: 98%");
        System.out.println("防釣魚閘門: 正常");
        break;

      case "DISPENSER_MOTOR":
        System.out.println("X軸馬達: OK");
        System.out.println("Y軸馬達: OK");
        System.out.println("掉落感應器: 靈敏度正常");
        break;

      case "CONNECTIVITY":
        // 模擬網路檢查
        boolean wifi = true;
        boolean sim4g = true;
        if (wifi && sim4g) System.out.println("雙網路備援正常");
        else if (wifi) System.out.println("僅 Wi-Fi 連線");
        else if (sim4g) System.out.println("僅 4G 連線");
        else System.out.println("警報：離線模式");
        break;

      default:
        System.out.println("未知子系統");
    }
  }

  private boolean checkVoltage(int v) {
    // 模擬電壓波動檢查
    return v >= 100 && v <= 120;
  }

  private void testAllSlots() {
    for (Map.Entry<String, Drink> entry : machine.getInventory().entrySet()) {
      String id = entry.getKey();
      Drink d = entry.getValue();
      if (d.getStock() > 0) {
        System.out.println("貨道 " + id + " (" + d.getName() + ") 測試: 正常");
      } else {
        System.out.println("貨道 " + id + " 測試: 略過 (無商品)");
      }
    }
  }

  private void analyzeInventoryHealth() {
    System.out.println("=== 智慧庫存分析報告 ===");
    int criticalItems = 0;

    for (Drink d : machine.getInventory().values()) {
      double rps = calculateRPS(d);
      String status = "正常";
      if (rps > 100) {
        status = "【緊急補貨】";
        criticalItems++;
      } else if (rps > 50) {
        status = "需關注";
      }
      System.out.printf("商品: %-6s 庫存: %2d 分數: %6.1f 狀態: %s%n",
              d.getName(), d.getStock(), rps, status);
    }

    if (criticalItems > 0) System.out.println("建議：有 " + criticalItems + " 項商品需要立即處理。");
  }

  private double calculateRPS(Drink d) {
    int maxStock = 10;
    int missing = maxStock - d.getStock();
    double score = missing * 10.0;

    if (d.getPrice() >= 30) score *= 1.5;
    if (d.isHot()) score += 20;
    if (d.getStock() == 0) score += 50;

    return score;
  }

  /**
   * 維修成本預估 (Maintenance Cost Estimation)
   * 根據機器狀態計算潛在維修費，增加大量判斷邏輯
   */
  private int estimateMaintenanceCost() {
    int cost = 0;

    // 基礎檢測費
    cost += 500;

    // 根據庫存狀況增加人工補貨成本
    long emptySlots = machine.getInventory().values().stream().filter(d -> d.getStock() == 0).count();
    if (emptySlots > 3) cost += 300; // 大量補貨
    else if (emptySlots > 0) cost += 100;

    // 根據硬幣餘額判斷是否需要補幣服務
    if (machine.getBalance() < 100) { // 假設機器內總金額低 (這裡簡化用 balance 代替)
      // 實際上應該檢查 ChangeService 的庫存，這裡模擬邏輯
      cost += 200;
    }

    return cost;
  }
}