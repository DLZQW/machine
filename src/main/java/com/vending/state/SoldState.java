package com.vending.state;

import com.vending.core.VendingMachine;

public class SoldState implements VendingMachineState {
  private VendingMachine machine;

  public SoldState(VendingMachine machine) {
    this.machine = machine;
  }

  @Override
  public void insertCoin(int amount) {
    System.out.println("機器處理中，請稍後再投幣");
  }

  @Override
  public void selectDrink(String drinkId) {
    System.out.println("機器處理中，無法更改選擇");
  }

  @Override
  public void dispense() {
    machine.finalizeTransaction();
    // 交易完成後由 finalizeTransaction 負責切換回 IdleState
  }

  @Override
  public void cancel() {
    System.out.println("商品已售出，無法取消交易");
  }

  @Override
  public void maintenance(String password) {
    System.out.println("交易中不可維護");
  }
}