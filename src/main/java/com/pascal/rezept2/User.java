package com.pascal.rezept2;

import java.sql.ResultSet;
import java.sql.SQLException;

public class User implements UserInterface{
  private String username;
  private String firstName;
  private String lastName;
  private final String accountAge;
  private final int userID;

  public User(String username, String firstName, String lastName, String accountAge, int userID) {
    this.username = username;
    this.firstName = firstName;
    this.lastName = lastName;
    this.accountAge = accountAge;
    this.userID = userID;
  }

  public static User from(ResultSet rs) throws SQLException {
    int userID = rs.getInt("ID");
    String username = rs.getString("USERNAME");
    String firstName = rs.getString("FIRSTNAME");
    String lastName = rs.getString("LASTNAME");
    //String accountAge = rs.getString("ACCOUNTAGE");
    return new User(username, firstName, lastName, "NOT IMPLEMENTED", userID);
  }

  @Override
  public int getUserID() {
    return userID;
  }
  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getFirstName() {
    return firstName;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  public String getAccountAge() {
    return accountAge;
  }

  @Override
  public String toString() {
    return "User{" +
      "username='" + username + '\'' +
      ", firstName='" + firstName + '\'' +
      ", lastName='" + lastName + '\'' +
      ", accountAge='" + accountAge + '\'' +
      ", userID=" + userID +
      '}';
  }
}
