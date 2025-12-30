package com.vending.core;

import com.vending.model.Drink;
import com.vending.state.*;
import com.vending.service.ChangeService;
import com.vending.service.DiscountEngine;
import java.util.*;

public class VendingMachine {
  // 加上 final 消除 PMD 警告
  private final VendingMachineState idleState;
  private final VendingMachineState hasMoneyState;
  private final VendingMachineState soldState;
  private final VendingMachineState soldOutState;
  private final VendingMachineState maintenanceState;

  private final Map<String, Drink> inventory = new HashMap<>();
  private final ChangeService changeService;
  private final DiscountEngine discountEngine;

  private VendingMachineState currentState;
  private int balance = 0;
  private Drink currentDrink;

  public VendingMachine() {
    this.idleState = new IdleState(this);
    this.hasMoneyState = new HasMoneyState(this);
    this.soldState = new SoldState(this);
    this.soldOutState = new SoldOutState(this);
    this.maintenanceState = new MaintenanceState(this);

    this.changeService = new ChangeService();
    this.discountEngine = new DiscountEngine();

    this.currentState = idleState;
    initInventory();
  }

  private void initInventory() {
    inventory.put("A1", new Drink("A1", "可樂", 25, 10, false));
    inventory.put("A2", new Drink("A2", "綠茶", 20, 5, false));
    inventory.put("B1", new Drink("B1", "咖啡", 35, 2, true));
  }

  // 業務委派方法，確保所有功能皆可測試
  public void insertCoin(int amount) { currentState.insertCoin(amount); }
  public void selectDrink(String id) { currentState.selectDrink(id); }
  public void cancel() { currentState.cancel(); }
  public void dispense() { currentState.dispense(); }
  public void enterMaintenance(String pwd) { currentState.maintenance(pwd); }

  public void finalizeTransaction() {
    if (currentDrink != null) {
      int finalPrice = discountEngine.applyPromotion(currentDrink, balance, false);
      // 覆蓋餘額充足與不足的分支，解決 Branch Coverage 缺口
      if (balance >= finalPrice) {
        balance -= finalPrice;
        currentDrink.setStock(currentDrink.getStock() - 1);
        changeService.calculateChange(balance);
        balance = 0;
        this.currentState = idleState;
      } else {
        this.currentState = hasMoneyState;
      }
    }
    currentDrink = null;
  }

  // Getters & Setters
  public void setState(VendingMachineState state) { this.currentState = state; }
  public VendingMachineState getCurrentState() { return currentState; }
  public VendingMachineState getIdleState() { return idleState; }
  public VendingMachineState getHasMoneyState() { return hasMoneyState; }
  public VendingMachineState getSoldState() { return soldState; }
  public VendingMachineState getSoldOutState() { return soldOutState; }
  public VendingMachineState getMaintenanceState() { return maintenanceState; }
  public int getBalance() { return balance; }
  public void setBalance(int b) { this.balance = b; }
  public Map<String, Drink> getInventory() { return inventory; }
  public void setCurrentDrink(Drink d) { this.currentDrink = d; }
}