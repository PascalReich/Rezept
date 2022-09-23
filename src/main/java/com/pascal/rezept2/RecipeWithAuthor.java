package com.pascal.rezept2;

import java.sql.ResultSet;
import java.sql.SQLException;

public class RecipeWithAuthor implements RecipeInterface, UserInterface {
  private final Recipe recipe;
  private final User author;


  public RecipeWithAuthor(Recipe recipe, User author) {

    this.recipe = recipe;
    this.author = author;
  }

  public static RecipeWithAuthor from(ResultSet rs) throws SQLException {
    return new RecipeWithAuthor(Recipe.from(rs), User.from(rs));
  }

  @Override
  public int getUserID() {
    return author.getUserID();
  }

  /**
   * @return Name of the Recipe
   */
  @Override
  public String getRecipeName() {
    return recipe.getRecipeName();
  }

  /**
   * @param name Name to give to the recipe
   */
  @Override
  public void setRecipeName(String name) {
    recipe.setRecipeName(name);
  }

  /**
   * @return
   */
  @Override
  public int getRecipeID() {
    return recipe.getRecipeID();
  }

  /**
   * @return
   */
  @Override
  public String toSQLQuery() {
    return author.toSQLQuery();
  }

  /**
   * @return
   */
  @Override
  public String getUsername() {
    return author.getUsername();
  }

  /**
   * @param username
   */
  @Override
  public void setUsername(String username) {
    author.setUsername(username);
  }

  /**
   * @return
   */
  @Override
  public String getFirstName() {
    return author.getFirstName();
  }

  /**
   * @param firstName
   */
  @Override
  public void setFirstName(String firstName) {
    author.setFirstName(firstName);
  }

  /**
   * @return lastName of Author
   */
  @Override
  public String getLastName() {
    return author.getLastName();
  }

  /**
   * @param lastName setter
   */
  @Override
  public void setLastName(String lastName) {
    author.setLastName(lastName);
  }

  /**
   * @return
   */
  @Override
  public String getAccountAge() {
    return author.getAccountAge();
  }
}
