package com.pascal.rezept2;

public interface RecipeInterface {
  /**
   * @return Name of the Recipe
   */
  public String getRecipeName();

  /**
   * @param name Name to give to the recipe
   */
  public void setRecipeName(String name);

  public int getRecipeID();
}
