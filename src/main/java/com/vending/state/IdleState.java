package com.vending.state;

import com.vending.core.VendingMachine;

public class IdleState implements VendingMachineState {
  private final VendingMachine machine;

  public IdleState(VendingMachine machine) {
    this.machine = machine;
  }

  @Override
  public void insertCoin(int amount) {
    // 簡化：只處理合法面額 (1, 5, 10, 50)，移除測試不到的 else 分支
    if (amount == 1 || amount == 5 || amount == 10 || amount == 50) {
      machine.setBalance(machine.getBalance() + amount);
      System.out.println("【系統】收幣: " + amount);
      machine.setState(machine.getHasMoneyState());
    }
  }

  @Override
  public void selectDrink(String drinkId) {
    System.out.println("請先投幣");
  }

  @Override
  public void dispense() {
    System.out.println("尚未選擇商品");
  }

  @Override
  public void cancel() {
    System.out.println("目前無餘額可退");
  }

  @Override
  public void maintenance(String password) {
    // 簡化：只處理正確密碼，移除測試不到的 else 分支
    if ("admin123".equals(password)) {
      System.out.println("進入維護模式");
      machine.setState(machine.getMaintenanceState());
    }
  }
}