package com.github.ghmk5.dcmanj.info;

import java.awt.Rectangle;
import java.util.HashMap;

public class AppInfo {

  Rectangle rectMain;
  Rectangle rectPref;
  Rectangle rectImpt;
  HashMap<String, Integer> columnWidthMap;

  /**
   * アプリケーション設定情報をBeanとして扱うためのクラス
   *
   */
  public AppInfo() {
    super();

    // Rectangleのデフォルト値を設定
    int posX = 10;
    int posY = 10;
    Rectangle archRectangle = new Rectangle(posX, posY, 0, 0);
    this.rectMain = (Rectangle) archRectangle.clone();
    this.rectPref = (Rectangle) archRectangle.clone();
    this.rectImpt = (Rectangle) archRectangle.clone();

  }

  public Rectangle getRectMain() {
    return rectMain;
  }

  public void setRectMain(Rectangle rectMain) {
    this.rectMain = rectMain;
  }

  public Rectangle getRectPref() {
    return rectPref;
  }

  public void setRectPref(Rectangle rectPref) {
    this.rectPref = rectPref;
  }

  public Rectangle getRectImpt() {
    return rectImpt;
  }

  public void setRectImpt(Rectangle rectImpt) {
    this.rectImpt = rectImpt;
  }

  public HashMap<String, Integer> getColumnWidthMap() {
    return columnWidthMap;
  }

  public void setColumnWidthMap(HashMap<String, Integer> columnWidthMap) {
    this.columnWidthMap = columnWidthMap;
  }


}
