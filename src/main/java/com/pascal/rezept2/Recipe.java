package com.pascal.rezept2;

import java.sql.ResultSet;
import java.sql.SQLException;

public class Recipe implements RecipeInterface {

  private String RecipeName;
  private final String dateCreated;



  private final int RecipeID;

  public static final String tableName = "RECIPES";

  public Recipe() {
    this("Unnamed Recipe", "Now", -1);
  }

  public Recipe (String RecipeName, String dateCreated, int RecipeID) {
    this.RecipeName = RecipeName;
    this.dateCreated = dateCreated;
    this.RecipeID = RecipeID;
  }

  static Recipe from(ResultSet resultSet) throws SQLException {
    String name = resultSet.getString("NAME");
    int id = resultSet.getInt("ID");
    return new Recipe(name, "Not Implemented", id);
  }

  public static String getTableName() {
    return Recipe.tableName;
  }

  /**
   * @return Name of the Recipe
   */
  public String getRecipeName() {
    return RecipeName;
  }

  /**
   * @param name Name to give to the recipe
   */
  public void setRecipeName(String name) {
    RecipeName = name;
  }

  public int getRecipeID() {
    return RecipeID;
  }

  @Override
  public String toString() {
    return "Recipe{" +
      "Name='" + RecipeName + '\'' +
      ", dateCreated='" + dateCreated + '\'' +
      ", ID=" + RecipeID +
      '}';
  }
}
