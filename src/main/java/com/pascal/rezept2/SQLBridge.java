package com.pascal.rezept2;

import org.jetbrains.annotations.NotNull;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SQLBridge {

  private static SQLBridge instance;
  private Connection conn;

  private SQLBridge() {

  }

  /**
   * Gets gets the SQLBridge singleton instance
   * @return
   */
  public static SQLBridge getInstance() {
    if (instance == null) {
      instance = new SQLBridge();
    }
    return instance;
  }

  public void connectionToDerby() throws SQLException {
    String dbURL = "jdbc:derby:rezeptDB";
    conn = DriverManager.getConnection(dbURL);
  }

  /**
   * @throws SQLException
   *  Creates a new Table
   */
  public void createTable() throws SQLException {
    Statement stmt = conn.createStatement();

    //Create Table
    stmt.execute("Create Table Recipes (id int primary key, name varchar(30))");

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

  /**
   *
   * @return all recipes
   * @throws SQLException
   */
  public List<Recipe> getRecipes() throws SQLException {
    return getRecipes("*", "");
  }

  /**
   *
   * @param columns which columns to select from the table
   * @param conditions conditions given with "WHERE"
   * @return all recipes that match conditions
   * @throws SQLException
   */
  public List<Recipe> getRecipes(String columns, String conditions) throws SQLException {
    String query = "select " + columns + " from RECIPES " + conditions;
    System.out.println(query);
    Statement stmt = conn.createStatement();
    ResultSet rs = stmt.executeQuery(query);

    List<Recipe> resultValues = new ArrayList<>();

    while (rs.next()) {
      //System.out.println(rs);
      resultValues.add(Recipe.from(rs));
    }

    return resultValues;
  }

  /**
   * Get all recipes and their associated authors
   * @return List of RecipeWithAuthor instances
   * @throws SQLException
   */
  public List<RecipeWithAuthor> getRecipesAndAuthors() throws SQLException {
    String query = "SELECT * " +
      "FROM Recipes LEFT JOIN Users " +
      "ON Recipes.AuthorID = Users.ID";

    Statement stmt = conn.createStatement();
    ResultSet rs = stmt.executeQuery(query);

    List<RecipeWithAuthor> resultValues = new ArrayList<>();

    while (rs.next()) {
      resultValues.add(RecipeWithAuthor.from(rs));
    }

    return resultValues;
  }

  /**
   * Get all users
   * @return Returns all users
   * @throws SQLException
   */
  public List<User> getUsers() throws SQLException {
    return getUsers("*", "");
  }

  /**
   *
   * @param columns the columns of the table to fetch
   * @param conditions extra conditions with WHERE
   * @return users that meet the supplied conditions
   * @throws SQLException
   */
  public List<User> getUsers(String columns, String conditions) throws SQLException {
    String query = "select " + columns + " from USERS " + conditions;
    //System.out.println(query);
    Statement stmt = conn.createStatement();
    ResultSet rs = stmt.executeQuery(query);

    List<User> resultValues = new ArrayList<>();

    while (rs.next()) {
      resultValues.add(User.from(rs));
    }

    return resultValues;
  }

  public boolean comparePassword(String username, String hash) throws SQLException {

    String query = "SELECT PASSWORD FROM USERS WHERE USERS.USERNAME = '" + username+"'";

    Statement stmt = conn.createStatement();
    ResultSet rs = stmt.executeQuery(query);

    while (rs.next()) {
      // System.out.println(rs.getString("PASSWORD"));
      return rs.getString("PASSWORD").equals(hash);
    }

    return false;
  }

  /**
   * Update a user given its UserInterface instance
   * @param user a UserInterface instance with changed data.
   * @throws SQLException
   */
  public void updateUser(UserInterface user) throws SQLException {
    String query = user.toSQLQuery();
    Statement stmt = conn.createStatement();
    stmt.executeUpdate(query);
    //System.out.println("Updated Users");
  }

  public static void main(String[] args) throws SQLException {
    SQLBridge bridge = SQLBridge.getInstance();
    bridge.connectionToDerby();
    //bridge.createTable();
    List<Recipe> result = bridge.getRecipes();
    for (Recipe wrapper: result) {
      System.out.println(wrapper);
    }
  }
}
