package com.vending.state;

import com.vending.core.VendingMachine;

public class SoldOutState implements VendingMachineState {
  private VendingMachine machine;

  public SoldOutState(VendingMachine machine) {
    this.machine = machine;
  }

  @Override
  public void insertCoin(int amount) {
    System.out.println("商品已售罄，請按取消鍵退幣");
    machine.setBalance(machine.getBalance() + amount);
  }

  @Override
  public void selectDrink(String drinkId) {
    System.out.println("目前無貨");
  }

  @Override
  public void dispense() {
    System.out.println("無貨可供出貨");
  }

  @Override
  public void cancel() {
    int refund = machine.getBalance();
    machine.setBalance(0);
    System.out.println("退還全部金額: " + refund);
    machine.setState(machine.getIdleState());
  }

  @Override
  public void maintenance(String password) {
    if ("admin123".equals(password)) {
      machine.setState(machine.getMaintenanceState());
    }
  }
}