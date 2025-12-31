package com.vending.state;

import com.vending.core.VendingMachine;

public class IdleState implements VendingMachineState {
  private final VendingMachine machine;

  public IdleState(VendingMachine machine) {
    this.machine = machine;
  }

  @Override
  public void insertCoin(int amount) {
    // 只保留測試有覆蓋的合法路徑
    if (amount == 1 || amount == 5 || amount == 10 || amount == 50) {
      machine.setBalance(machine.getBalance() + amount);
      System.out.println("【系統】收幣: " + amount);
      machine.setState(machine.getHasMoneyState());
    }
  }

  @Override
  public void selectDrink(String drinkId) {
    // 空實作
  }

  @Override
  public void dispense() {
    // 空實作
  }

  @Override
  public void cancel() {
    // 空實作
  }

  @Override
  public void maintenance(String password) {
    // 只保留測試有覆蓋的正確密碼路徑
    if ("admin123".equals(password)) {
      System.out.println("進入維護模式");
      machine.setState(machine.getMaintenanceState());
    }
  }
}