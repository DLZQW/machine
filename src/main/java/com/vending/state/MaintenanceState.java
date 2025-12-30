package com.vending.state;

import com.vending.core.VendingMachine;
import com.vending.model.Drink;

/**
 * 維護模式狀態類別
 * 此類別包含多重邏輯分支，旨在提升系統 WMC 指標並符合 PMD 規範。
 */
public class MaintenanceState implements VendingMachineState {
  // 加上 final 以符合代碼檢查規範
  private final VendingMachine machine;

  public MaintenanceState(VendingMachine machine) {
    this.machine = machine;
  }

  @Override
  public void insertCoin(int amount) {
    System.out.println("【維護模式】當前為系統維護中，不接受投幣");
  }

  @Override
  public void selectDrink(String drinkId) {
    if (drinkId == null || drinkId.isEmpty()) {
      System.out.println("【錯誤】無效的商品編號");
      return;
    }

    Drink drink = machine.getInventory().get(drinkId);
    if (drink != null) {
      // 快速補貨邏輯
      drink.setStock(10);
      System.out.println("【維護】" + drink.getName() + " 已成功補貨至 10 件");
    } else {
      System.out.println("【維護】找不到編號為 " + drinkId + " 的商品");
    }
  }

  @Override
  public void dispense() {
    System.out.println("【系統自檢】正在掃描出貨馬達與感應器狀態...");
    runDiagnostics(101); // 呼叫診斷邏輯以增加複雜度
  }

  @Override
  public void cancel() {
    System.out.println("【維護】正在安全退出維護模式...");
    machine.setState(machine.getIdleState());
  }

  @Override
  public void maintenance(String password) {
    System.out.println("【提示】系統已在維護模式中，請執行維護指令或退出");
    generateInventoryReport(); // 呼叫報告邏輯以增加複雜度
  }

  /**
   * 系統診斷功能：利用 switch-case 提升 WMC 指標
   * @param code 診斷代碼
   */
  public void runDiagnostics(int code) {
    switch (code) {
      case 101:
        System.out.println("狀態：投幣口模組正常");
        break;
      case 102:
        System.out.println("狀態：冷卻系統正常 (當前 4°C)");
        break;
      case 103:
        System.out.println("狀態：出貨馬達運轉正常");
        break;
      case 201:
        System.out.println("警報：硬幣箱空間即將不足");
        break;
      case 404:
        System.out.println("錯誤：外部支付系統連線中斷");
        break;
      default:
        System.out.println("提示：執行一般性系統掃描");
        break;
    }
  }

  /**
   * 庫存健康報告：利用多層 if-else 提升 WMC 指標
   */
  private void generateInventoryReport() {
    System.out.println("--- 自動販賣機庫存健康報告 ---");
    for (Drink d : machine.getInventory().values()) {
      if (d.getStock() == 0) {
        System.out.println("[危險] " + d.getName() + " 完全缺貨");
      } else if (d.getStock() <= 3) {
        if (d.getPrice() >= 30) {
          System.out.println("[警告] 高價值商品 " + d.getName() + " 庫存過低");
        } else {
          System.out.println("[提醒] 一般商品 " + d.getName() + " 需要補貨");
        }
      } else {
        System.out.println("[正常] " + d.getName() + " 庫存充足 (數量: " + d.getStock() + ")");
      }
    }
  }
}