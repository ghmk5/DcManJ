package com.github.ghmk5.dcmanj.info;

import java.awt.Rectangle;
import java.io.File;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Objects;
import javax.swing.JOptionPane;
import com.github.ghmk5.dcmanj.util.Util;

public class AppInfo {

  // 各種パス
  String parentPath;
  String prefFilePath;
  String dbFilePath;

  // ウィンドウの位置とサイズ、テーブルカラム幅
  Rectangle rectMain;
  Rectangle rectPref;
  Rectangle rectImpt;
  Rectangle rectAttr;
  HashMap<String, Integer> columnWidthMain;
  HashMap<String, Integer> columnWidthImpt;

  // インポート関連
  String imptDir;
  String[] namesToBeIgnored;
  String saveDir;
  Boolean zipToStore;
  Boolean useChildDir;
  String childDirPrefix;
  Boolean splitChildDirBySize;
  Integer childDirSplitSize;
  Boolean splitChildDirByNumber;
  Integer childDirSplitNumber;

  // 移動関連
  Boolean moveAsReImport;
  Boolean selectDestDirOnMove;
  String moveDestDir;
  Boolean zipOnMove;
  Boolean unzipOnMove;
  Boolean renOnMove;
  Boolean useChildDirOnMove;
  String childlDirPrefixOnMove;
  Boolean splitChildDirBySizeOnMove;
  Integer childDirSplitSizeOnMove;
  Boolean splitChildDirByNumberOnMove;
  Integer childDirSplitNumberOnMove;

  // 画像ビューワ
  String viewerPath;

  // 青空文庫テキストビューワ
  String aoViewerPath;

  // パーサ
  String[] evRegExStrings;
  String[] defaultEvRegExStrings = {"^C\\d{2,3}$", "サンクリ", "酒保", "コミティア", "COMITIA", "紅楼夢", "例大祭",
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

  // setter/getter


  // 各種パス

  public String getParentPath() {
    if (Objects.isNull(parentPath)) {
      try {
        if (Util.getApplicationPath(this.getClass()).toFile().isFile()) {
          parentPath = Util.getApplicationPath(this.getClass()).getParent().toString();
        } else {
          parentPath = Util.getApplicationPath(this.getClass()).toString();
        }
        return parentPath;
      } catch (URISyntaxException e) {
        JOptionPane.showMessageDialog(null, "実行ファイルのパスが決定できません\nプログラムを終了します", "致命的なエラー",
            JOptionPane.ERROR_MESSAGE);
        e.printStackTrace();
        System.exit(0);
        return null;
      }
    } else {
      return parentPath;
    }
  }

  public void setParentPath(String parentPath) {
    this.parentPath = parentPath;
  }

  public String getPrefFilePath() {
    if (Objects.isNull(prefFilePath)) {
      File prefFile = new File(getParentPath(), "DcManJPrefs.xml");
      return prefFile.getAbsolutePath().toString();
    } else {
      return prefFilePath;
    }
  }

  public void setPrefFilePath(String prefFilePath) {
    this.prefFilePath = prefFilePath;
  }

  public String getDbFilePath() {
    if (Objects.isNull(dbFilePath)) {
      File dbFile = new File(getParentPath(), "DcManJ.db");
      return dbFile.getAbsolutePath().toString();
    } else {
      return dbFilePath;
    }
  }

  public void setDbFilePath(String dbFilePath) {
    this.dbFilePath = dbFilePath;
  }


  // ウィンドウの位置とサイズ、テーブルカラム幅

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

  public Rectangle getRectAttr() {
    return rectAttr;
  }

  public void setRectAttr(Rectangle rectAttr) {
    this.rectAttr = rectAttr;
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


  // インポート関連

  public String getImptDir() {
    if (Objects.isNull(imptDir)) {
      return System.getProperty("user.home");
    } else {
      return imptDir;
    }
  }

  public void setImptDir(String imptDir) {
    this.imptDir = imptDir;
  }

  public String[] getNamesToBeIgnored() {
    return namesToBeIgnored;
  }

  public void setNamesToBeIgnored(String[] namesToBeIgnored) {
    this.namesToBeIgnored = namesToBeIgnored;
  }

  public String getSaveDir() {
    if (Objects.isNull(saveDir)) {
      saveDir = System.getProperty("user.home");
    }
    return saveDir;
  }

  public void setSaveDir(String saveDir) {
    this.saveDir = saveDir;
  }

  public Boolean getZipToStore() {
    if (Objects.isNull(zipToStore)) {
      zipToStore = true;
    }
    return zipToStore;
  }

  public void setZipToStore(Boolean zipToStore) {
    this.zipToStore = zipToStore;
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
    if (Objects.isNull(childDirPrefix)) {
      childDirPrefix = "";
    }
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


  // 移動関連
  // 対応する値が保存されていない場合はインポート用設定の値を参照して返す(インポート用の値はデフォルト値の設定がある)

  public Boolean getMoveAsReImport() {
    if (Objects.isNull(moveAsReImport)) {
      moveAsReImport = true;
    }
    return moveAsReImport;
  }

  public void setMoveAsReImport(Boolean sameAsImptOnMove) {
    this.moveAsReImport = sameAsImptOnMove;
  }

  public Boolean getSelectDestDirOnMove() {
    if (Objects.isNull(selectDestDirOnMove)) {
      selectDestDirOnMove = true;
    }
    return selectDestDirOnMove;
  }

  public void setSelectDestDirOnMove(Boolean selectDestDirOnMove) {
    this.selectDestDirOnMove = selectDestDirOnMove;
  }

  public String getMoveDestDir() {
    if (Objects.isNull(moveDestDir)) {
      moveDestDir = System.getProperty("user.home");
    }
    return moveDestDir;
  }

  public void setMoveDestDir(String moveDestDir) {
    this.moveDestDir = moveDestDir;
  }

  public Boolean getZipOnMove() {
    if (Objects.isNull(zipOnMove)) {
      zipOnMove = getZipToStore();
    }
    return zipOnMove;
  }

  public void setZipOnMove(Boolean zipToStoreOnMove) {
    this.zipOnMove = zipToStoreOnMove;
  }

  public Boolean getUnzipOnMove() {
    if (Objects.isNull(unzipOnMove)) {
      unzipOnMove = false;
    }
    return unzipOnMove;
  }

  public void setUnzipOnMove(Boolean unzipOnMove) {
    this.unzipOnMove = unzipOnMove;
  }

  public Boolean getRenOnMove() {
    if (Objects.isNull(renOnMove)) {
      renOnMove = true;
    }
    return renOnMove;
  }

  public void setRenOnMove(Boolean renOnMove) {
    this.renOnMove = renOnMove;
  }

  public Boolean getUseChildDirOnMove() {
    if (Objects.isNull(useChildDirOnMove)) {
      useChildDirOnMove = true;
    }
    return useChildDirOnMove;
  }

  public void setUseChildDirOnMove(Boolean useChildDirOnMove) {
    this.useChildDirOnMove = useChildDirOnMove;
  }

  public String getChildlDirPrefixOnMove() {
    if (Objects.isNull(childlDirPrefixOnMove)) {
      childlDirPrefixOnMove = getChildDirPrefix();
    }
    return childlDirPrefixOnMove;
  }

  public void setChildlDirPrefixOnMove(String chidlDirPrefixOnMove) {
    this.childlDirPrefixOnMove = chidlDirPrefixOnMove;
  }

  public Boolean getSplitChildDirBySizeOnMove() {
    if (Objects.isNull(splitChildDirBySizeOnMove)) {
      splitChildDirBySizeOnMove = true;
    }
    return splitChildDirBySizeOnMove;
  }

  public void setSplitChildDirBySizeOnMove(Boolean splitChildDirBySizeOnMove) {
    this.splitChildDirBySizeOnMove = splitChildDirBySizeOnMove;
  }

  public Integer getChildDirSplitSizeOnMove() {
    if (Objects.isNull(childDirSplitSizeOnMove)) {
      childDirSplitSizeOnMove = getChildDirSplitSize();
    }
    return childDirSplitSizeOnMove;
  }

  public void setChildDirSplitSizeOnMove(Integer childDirSplitSizeOnMove) {
    this.childDirSplitSizeOnMove = childDirSplitSizeOnMove;
  }

  public Boolean getSplitChildDirByNumberOnMove() {
    if (Objects.isNull(splitChildDirByNumberOnMove)) {
      splitChildDirByNumberOnMove = getSplitChildDirByNumber();
    }
    return splitChildDirByNumberOnMove;
  }

  public void setSplitChildDirByNumberOnMove(Boolean splitChildDirByNumberOnMove) {
    this.splitChildDirByNumberOnMove = splitChildDirByNumberOnMove;
  }

  public Integer getChildDirSplitNumberOnMove() {
    if (Objects.isNull(childDirSplitNumberOnMove)) {
      childDirSplitNumberOnMove = getChildDirSplitNumber();
    }
    return childDirSplitNumberOnMove;
  }

  public void setChildDirSplitNumberOnMove(Integer childDirSplitNumberOnMove) {
    this.childDirSplitNumberOnMove = childDirSplitNumberOnMove;
  }


  // ビューワ

  public String getViewerPath() {
    return viewerPath;
  }

  public void setViewerPath(String viewerPath) {
    this.viewerPath = viewerPath;
  }


  // 青空文庫テキストビューワ

  public String getAoViewerPath() {
    return aoViewerPath;
  }

  public void setAoViewerPath(String viewerPath) {
    this.aoViewerPath = viewerPath;
  }

  // パーサ

  public String[] getEvRegExStrings() {
    if (Objects.nonNull(evRegExStrings)) {
      return evRegExStrings;
    } else {
      return defaultEvRegExStrings;
    }
  }

  public void setEvRegExStrings(String[] evRexExStrings) {
    this.evRegExStrings = evRexExStrings;
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

  public String[] getDefaultEvRegExStrings() {
    return defaultEvRegExStrings;
  }

  public String[] getDefaultNoteRegExStrings() {
    return defaultNoteRegExStrings;
  }


}
