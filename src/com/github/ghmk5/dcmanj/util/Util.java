package com.github.ghmk5.dcmanj.util;

import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import com.github.ghmk5.dcmanj.info.AppInfo;

public class Util {

  static public DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss Z");

  /**
   * <p>
   * [概 要] 文字列の重複チェック
   * </p>
   * <p>
   * [詳 細] パラメータのリストで重複があればtrue、なければfalseを返します。
   * </p>
   * <p>
   * [備 考]
   * </p>
   *
   * @param checkList 重複チェック用のリスト
   * @return 重複有り：true、重複無し：false
   */
  public static Boolean checkOverlap(List<String> checkList) {
    Boolean result = false;
    Set<String> checkHash = new HashSet<>();
    for (String str : checkList) {
      if (checkHash.contains(str)) {
        // 重複があればtrueをセットし終了
        result = true;
        break;
      } else {
        // 重複しなければハッシュセットへ追加
        checkHash.add(str);
      }
    }
    return result;
  }

  /**
   * <p>
   * [概 要] 文字列の重複チェック
   * </p>
   * <p>
   * [詳 細] パラメータのリストで重複があればtrue、なければfalseを返します。
   * </p>
   * <p>
   * [備 考]
   * </p>
   *
   * @param checkArray 重複チェック用の配列
   * @return 重複有り：true、重複無し：false
   */
  public static Boolean checkOverlap(String[] checkArray) {
    List<String> checkList = Arrays.asList(checkArray);
    return checkOverlap(checkList);
  }

  /**
   * 実行ファイルのパスを返す
   *
   * @param cls 実行されているクラス
   * @return パスの文字列表現
   * @throws URISyntaxException
   */
  public static Path getApplicationPath(Class<?> cls) throws URISyntaxException {
    ProtectionDomain pd = cls.getProtectionDomain();
    CodeSource cs = pd.getCodeSource();
    URL location = cs.getLocation();
    URI uri = location.toURI();
    Path path = Paths.get(uri);
    return path;
  }

  /**
   * JDialogでESCキーを"ダイアログを閉じる"にマップする
   *
   * @param jDialog
   */
  public static void mapESCtoCancel(JDialog jDialog) {
    // ダイアログを閉じるアクション
    Action act = new AbstractAction("Cancel") {
      @Override
      public void actionPerformed(ActionEvent e) {
        jDialog.dispose();
      }
    };

    // 「ダイアログを閉じる」アクションをESCにマップ
    InputMap imap =
        jDialog.getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close-it");
    jDialog.getRootPane().getActionMap().put("close-it", act);
  }

  /**
   * Stringのリストを別途指定されたマップで定義されるソート順に従って並べ替えて返す
   *
   * @param list Stringのリスト 全ての要素は後述のマップのキーに含まれていなければならない
   * @param map Stringをキー、並び順を示すIntegerを値とするマップ
   * @return 並び替えられたリスト
   */
  public static ArrayList<String> sortByMap(ArrayList<String> list, HashMap<String, Integer> map) {
    List<Entry<String, Integer>> list_entries = new ArrayList<>(map.entrySet());
    Collections.sort(list_entries, Entry.comparingByValue());
    ArrayList<String> result = new ArrayList<>();
    for (Entry<String, Integer> entry : list_entries) {
      String key = entry.getKey();
      if (list.contains(key))
        result.add(key);
    }
    return result;
  }

  /**
   * 指定の{@link Container}内に配置された{@link Component}の中から、{@link Component#setName(String)}
   * で名前を与えられたものを検索し、名前とインスタンスを紐づけたマップを返す<BR>
   * {@link Container}内のネストは再帰的に検索される
   *
   * @param container 検索対象の{@link Container}
   * @return {@link HashMap}
   */
  public static HashMap<String, Component> createComponentMap(Container container) {
    HashMap<String, Component> componentMap = new HashMap<>();
    componentMap = createComponentMap(container, componentMap);
    return componentMap;
  }

  // 上のpublicメソッドから呼び出して再帰的に使用するためのメソッド -- マップの初期化を外部で行わないと再帰的に使用できないため
  private static HashMap<String, Component> createComponentMap(Container container,
      HashMap<String, Component> componentMap) {
    for (Component c : container.getComponents()) {
      if (Objects.nonNull(c.getName()))
        componentMap.put(c.getName(), c);
      try {
        createComponentMap((Container) c, componentMap);
      } catch (Exception eE) {
        eE.printStackTrace();
      }
    }
    return componentMap;
  }

  /**
   * 指定の{@link Container}内に配置された{@link Component}の中から、所与の名前を持つものを検索して返す<BR>
   * 動的に再帰検索するので重いはず。遅すぎるようなら{@link #getComponentByName(String, Container, HashMap)}の使用を考慮のこと
   *
   * @param name 検索する名前 -- 予め {@link Component#setName(String)} で与えておく
   * @param container 検索対象の{@link Container} ネストは再帰的に検索される
   * @return {@link Component} -- 見つからなかった場合はnull
   */
  public static Component getComponentByName(String name, Container container) {
    HashMap<String, Component> componentMap = new HashMap<>();
    componentMap = createComponentMap(container, componentMap);
    if (componentMap.containsKey(name)) {
      return (Component) componentMap.get(name);
    } else
      return null;
  }

  /**
   * 所与の名前を持つインスタンスを、名前とインスタンスを紐づけた所与のマップを用いて検索して返す<BR>
   *
   * @param name 検索する名前 -- 予め {@link Component#setName(String)} で与えておく
   * @param componentMap {@link Component}とその名前を紐づけたマップ --
   *        {@link #createComponentMap(Container)}での生成を想定
   * @return {@link Component} -- 見つからなかった場合はnull
   */
  public static Component getComponentByName(String name, HashMap<String, Component> componentMap) {
    if (componentMap.containsKey(name)) {
      return (Component) componentMap.get(name);
    } else
      return null;
  }

  /**
   * Beanファイルを読み込む ファイルが存在しない場合は null が戻る
   *
   * @param fileName Beanファイル
   * @return Bean (ファイルが存在しない場合 null)
   */
  public static Object readBean(File file) {
    Object bean = null;
    try {
      XMLDecoder decoder = new XMLDecoder(new BufferedInputStream(new FileInputStream(file)));
      bean = decoder.readObject();
      decoder.close();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }

    return bean;
  }

  /**
   * Beanファイルを書き込む
   *
   * @param fileName Beanファイル
   * @param bean 書き込むBean
   */
  public static void writeBean(File file, Object bean) {
    try {

      // 指定のフォルダがない場合は作る (親フォルダがルートの場合は無視する)
      File parentDir = file.getParentFile();
      if (parentDir != null && !parentDir.exists())
        parentDir.mkdirs();

      XMLEncoder encoder = new XMLEncoder(new BufferedOutputStream(new FileOutputStream(file)));
      encoder.writeObject(bean);
      encoder.close();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
  }

  /**
   * Windowの位置とサイズを変更する -- 画面からはみ出す場合は調整される
   *
   * @param window
   * @param givenRect
   */
  public static void setRect(Window window, Rectangle givenRect) {
    window.pack();
    Rectangle desktopBounds =
        GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
    Rectangle currentRect = window.getBounds();

    if (givenRect.isEmpty()) {
      givenRect.setSize(currentRect.getSize());
    }

    Rectangle rectToSet = new Rectangle();

    // pack()後のサイズを最小とし、最小値以下にはしない
    if (currentRect.width <= givenRect.width) {
      rectToSet.width = givenRect.width;
    } else {
      rectToSet.width = currentRect.width;
    }

    if (currentRect.height <= givenRect.height) {
      rectToSet.height = givenRect.height;
    } else {
      rectToSet.height = currentRect.height;
    }

    // ウィンドウが一部でも画面に入っている場合のみ指定位置に移動させる
    // 完全に画面の外に出てしまっている場合はデフォルト位置に戻す
    if (desktopBounds.intersects(givenRect)) {
      rectToSet.setLocation(givenRect.getLocation());
    } else {
      rectToSet.setLocation(10, 10);
    }

    window.setBounds(rectToSet);
  }

  public static String quoteForLike(String string) {
    String result;
    result = string.replaceAll("\'", "\'\'");
    result = result.replaceAll("_", "^_");
    result = result.replaceAll("%", "^%");
    result = "\'%" + result + "%\'";
    return result;
  }

  public static void openWithViewer(AppInfo appInfo, com.github.ghmk5.dcmanj.info.Entry entry) {
    String viewerPath = appInfo.getViewerPath();
    String entryPath = entry.getPath().toString();
    if (!(new File(entryPath).exists())) {
      JOptionPane.showMessageDialog(null, entryPath + " にアクセスできません");
      return;
    }
    if (Objects.nonNull(viewerPath) && new File(viewerPath).canExecute()) {
      try {
        entryPath = entryPath.replaceAll("\\^", "^^").replaceAll("&", "^&").replaceAll("%", "^%")
            .replaceAll("\\(", "^(").replaceAll("\\)", "^)");
        String[] command = {"cmd", "/c", "\"" + viewerPath + "\" " + entryPath};
        Runtime.getRuntime().exec(command);
      } catch (IOException e1) {
        // TODO 自動生成された catch ブロック
        e1.printStackTrace();
      }
    } else {
      JOptionPane.showMessageDialog(null, "ビューワアプリケーションが設定されていません");
    }
  }

}
