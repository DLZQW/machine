// File: com/vending/model/Drink.java
package com.vending.model;

public class Drink {
  private String id;
  private String name;
  private int price;
  private int stock;
  private boolean isHot;

  public Drink(String id, String name, int price, int stock, boolean isHot) {
    this.id = id;
    this.name = name;
    this.price = price;
    this.stock = stock;
    this.isHot = isHot;
  }
  // Getters and Setters
  public String getName() { return name; }
  public int getPrice() { return price; }
  public int getStock() { return stock; }
  public void setStock(int stock) { this.stock = stock; }
}