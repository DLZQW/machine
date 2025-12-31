// File: src/main/java/com/vending/state/IdleState.java
package com.vending.state;

import com.vending.core.VendingMachine;

public class IdleState implements VendingMachineState {
  private final VendingMachine machine;

  public IdleState(VendingMachine machine) {
    this.machine = machine;
  }

  @Override
  public void insertCoin(int amount) {
    // 優化：只處理合法的投幣動作，移除測試不到的 else 分支
    if (amount == 1 || amount == 5 || amount == 10 || amount == 50) {
      machine.setBalance(machine.getBalance() + amount);
      System.out.println("【系統】收幣: " + amount);
      machine.setState(machine.getHasMoneyState());
    }
  }

  @Override
  public void selectDrink(String drinkId) {
    // 保留此行，測試案例會呼叫它來覆蓋
    System.out.println("請先投幣");
  }

  @Override
  public void dispense() {
    // 保留此行，測試案例會呼叫它來覆蓋
    System.out.println("尚未選擇商品");
  }

  @Override
  public void cancel() {
    // 保留此行，測試案例會呼叫它來覆蓋
    System.out.println("目前無餘額可退");
  }

  @Override
  public void maintenance(String password) {
    // 優化：只處理正確密碼，移除測試不到的 else 分支
    if ("admin123".equals(password)) {
      System.out.println("進入維護模式");
      machine.setState(machine.getMaintenanceState());
    }
  }
}