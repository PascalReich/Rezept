package com.pascal.rezept2;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class User implements UserInterface{
  private String username;
  private String firstName;
  private String lastName;

  private String passwordHash;



  private String passwordSalt; //base 64 encoded
  private final String accountAge;
  private final int userID;

  private static final Map<Integer, User> USER_MAP = new HashMap<>();

  public User(String username, String firstName, String lastName,
              String accountAge, String passwordHash, String passwordSalt, int userID) {
    this.username = username;
    this.firstName = firstName;
    this.lastName = lastName;
    this.accountAge = accountAge;
    this.passwordHash = passwordHash;
    this.passwordSalt = passwordSalt;
    this.userID = userID;

    USER_MAP.put(this.userID, this);
  }

  public static User from(ResultSet rs) throws SQLException {
    int userID = rs.getInt("ID");

    if (USER_MAP.containsKey(userID)) {
      return USER_MAP.get(userID);
    }

    String username = rs.getString("USERNAME");
    String firstName = rs.getString("FIRSTNAME");
    String lastName = rs.getString("LASTNAME");
    String passwordHash = rs.getString("PASSWORD");
    String passwordSalt = rs.getString("PASSWORD_SALT");

    //String accountAge = rs.getString("ACCOUNTAGE");

    return new User(username, firstName, lastName,
      "NOT IMPLEMENTED", passwordHash, passwordSalt, userID);
  }

  public String toSQLQuery() {
    return String.format("UPDATE USERS SET USERNAME = '%s', FIRSTNAME = '%s', LASTNAME = '%s', PASSWORD_SALT = '%s', PASSWORD = '%s' WHERE ID = %o", username, firstName, lastName, passwordSalt, passwordHash, userID);
    //TODO make the salt a string
  }

  public void updatePassword(String passwordHash, String salt) {
    this.passwordHash = passwordHash;
    this.passwordSalt = salt;
  }

  public boolean comparePassword(String password) {
    return password.equals(this.passwordHash);
  }

  public boolean checkPassword(String pswd) {
    return true; // TODO implement
  }

  public String getPasswordSalt() {
    return passwordSalt;
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
