package com.pascal.rezept2;

import java.sql.*;

public class SQLBridge {
  private Connection conn;

  public void connectionToDerby() throws SQLException {
    String dbURL = "jdbc:derby:rezeptDB;create=true";
    conn = DriverManager.getConnection(dbURL);
  }

  public void createTable() throws SQLException {
    Statement stmt = conn.createStatement();

    //Create Table
    stmt.execute("Create Table USERS (id int primary key, name varchar(30))");

  }

  public void getTable() throws SQLException {
    String query = "select * from Users";
    Statement stmt = conn.createStatement();
    ResultSet rs = stmt.executeQuery(query);
    while (rs.next()) {
      System.out.println(rs);
            /* String coffeeName = rs.getString("COF_NAME");
            int supplierID = rs.getInt("SUP_ID");
            float price = rs.getFloat("PRICE");
            int sales = rs.getInt("SALES");
            int total = rs.getInt("TOTAL");
            System.out.println(coffeeName + ", " + supplierID + ", " + price +
                    ", " + sales + ", " + total);*/
    }
  }

  public static void main(String[] args) throws SQLException {
    SQLBridge bridge = new SQLBridge();
    bridge.connectionToDerby();
    bridge.createTable();
  }
}
