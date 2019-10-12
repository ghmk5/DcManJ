package com.github.ghmk5.dcmanj.info;

import java.awt.Rectangle;
import java.util.HashMap;
import java.util.Objects;

public class AppInfo {

  Rectangle rectMain;
  Rectangle rectPref;
  Rectangle rectImpt;
  Rectangle rectAttr;
  HashMap<String, Integer> columnWidthMain;
  HashMap<String, Integer> columnWidthImpt;
  String imptDir;
  String saveDir;
  Boolean useChildDir;
  String childDirPrefix;
  Boolean splitChildDirBySize;
  Integer childDirSplitSize;
  Boolean splitChildDirByNumber;
  Integer childDirSplitNumber;
  String viewerPath;
  String[] evRexExStrings;
  String[] defaultEvRexExStrings = {"^C\\d{2,3}$", "サンクリ", "酒保", "コミティア", "COMITIA", "紅楼夢", "例大祭",
      "神戸かわさき", "砲雷", "夜戦に突入す", "もう何も恐くない", "みみけっと", "ふたけっと", "ぱんっあ☆ふぉー", "とら祭り", "こみトレ",
      "^SC\\d{2}", "Cレヴォ", "C[Oo][Mm][Ii][Cc]1☆"};
  String[] noteRegExStrings;
  String[] defaultNoteRegExStrings = {"DL版", "CG集", "別スキャン", "^+", "補完", "雑誌寄せ集め", "修正版"};

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
    this.rectAttr = (Rectangle) archRectangle.clone();

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

  public HashMap<String, Integer> getColumnWidthMain() {
    return columnWidthMain;
  }

  public void setColumnWidthMain(HashMap<String, Integer> columnWidthMap) {
    this.columnWidthMain = columnWidthMap;
  }

  public HashMap<String, Integer> getColumnWidthImpt() {
    return columnWidthImpt;
  }

  public void setColumnWidthImpt(HashMap<String, Integer> columnWidthIMap) {
    this.columnWidthImpt = columnWidthIMap;
  }

  public String getImptDir() {
    return imptDir;
  }

  public void setImptDir(String imptDir) {
    this.imptDir = imptDir;
  }

  public Rectangle getRectAttr() {
    return rectAttr;
  }

  public void setRectAttr(Rectangle rectAttr) {
    this.rectAttr = rectAttr;
  }

  public String getSaveDir() {
    if (Objects.isNull(saveDir)) {
      return System.getProperty("user.home");
    } else {
      return saveDir;
    }
  }

  public void setSaveDir(String saveDir) {
    this.saveDir = saveDir;
  }

  public String getViewerPath() {
    return viewerPath;
  }

  public void setViewerPath(String viewerPath) {
    this.viewerPath = viewerPath;
  }

  public String[] getEvRexExStrings() {
    if (Objects.nonNull(evRexExStrings)) {
      return evRexExStrings;
    } else {
      return defaultEvRexExStrings;
    }
  }

  public void setEvRexExStrings(String[] evRexExStrings) {
    this.evRexExStrings = evRexExStrings;
  }

  public String[] getNoteRegExStrings() {
    if (Objects.nonNull(noteRegExStrings)) {
      return noteRegExStrings;
    } else {
      return defaultNoteRegExStrings;
    }
  }

  public void setNoteRegExStrings(String[] noteRegExStrings) {
    this.noteRegExStrings = noteRegExStrings;
  }

  public Boolean getUseChildDir() {
    if (Objects.isNull(useChildDir)) {
      return true;
    } else {
      return useChildDir;
    }
  }

  public void setUseChildDir(Boolean useChildDir) {
    this.useChildDir = useChildDir;
  }

  public String getChildDirPrefix() {
    return childDirPrefix;
  }

  public void setChildDirPrefix(String childDirPrefix) {
    this.childDirPrefix = childDirPrefix;
  }

  public Boolean getSplitChildDirBySize() {
    if (Objects.isNull(splitChildDirBySize)) {
      return true;
    } else {
      return splitChildDirBySize;
    }
  }

  public void setSplitChildDirBySize(Boolean splitChildDirBySize) {
    this.splitChildDirBySize = splitChildDirBySize;
  }

  public Integer getChildDirSplitSize() {
    if (Objects.isNull(childDirSplitSize)) {
      return 22649;
    } else {
      return childDirSplitSize;
    }
  }

  public void setChildDirSplitSize(Integer childDirSplitSize) {
    this.childDirSplitSize = childDirSplitSize;
  }

  public Boolean getSplitChildDirByNumber() {
    if (Objects.isNull(splitChildDirByNumber)) {
      return false;
    } else {
      return splitChildDirByNumber;
    }
  }

  public void setSplitChildDirByNumber(Boolean splitChildDirByNumber) {
    this.splitChildDirByNumber = splitChildDirByNumber;
  }

  public Integer getChildDirSplitNumber() {
    if (Objects.isNull(childDirSplitNumber)) {
      return 128;
    } else {
      return childDirSplitNumber;
    }
  }

  public void setChildDirSplitNumber(Integer childDirSplitNumber) {
    this.childDirSplitNumber = childDirSplitNumber;
  }


}
