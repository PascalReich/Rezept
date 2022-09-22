package com.pascal.rezept2;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;

public interface  DataWrapper {

  public static String getTableName() {
    return "";
  }


  public static DataWrapper from(ResultSet resultSet) {
    return null;
  }
}
