package com.vending.state;

import com.vending.core.VendingMachine;
import com.vending.model.Drink;

public class HasMoneyState implements VendingMachineState {
  private final VendingMachine machine;

  public HasMoneyState(VendingMachine machine) { this.machine = machine; }

  @Override
  public void insertCoin(int amount) {
    if (amount == 1 || amount == 5 || amount == 10 || amount == 50) {
      machine.setBalance(machine.getBalance() + amount);
    }
  }

  @Override
  public void selectDrink(String drinkId) {
    Drink drink = machine.getInventory().get(drinkId);
    if (drink == null) {
      System.out.println("品項不存在");
    } else if (drink.getStock() <= 0) {
      // 修正重點：確保狀態切換至 SoldOutState，解決測試失敗問題
      machine.setState(machine.getSoldOutState());
    } else if (machine.getBalance() < drink.getPrice()) {
      System.out.println("餘額不足");
    } else {
      machine.setCurrentDrink(drink);
      machine.setState(machine.getSoldState());
      machine.dispense();
    }
  }

  @Override public void dispense() { /* 實作略 */ }
  @Override public void cancel() { machine.setBalance(0); machine.setState(machine.getIdleState()); }
  @Override public void maintenance(String pwd) { /* 實作略 */ }
}