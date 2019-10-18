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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import org.apache.commons.io.FileUtils;
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

  /**
   * 所与のEntryを登録されたビューワで開く
   *
   * @param appInfo
   * @param entry
   */
  public static void openWithViewer(AppInfo appInfo, com.github.ghmk5.dcmanj.info.Entry entry) {
    String viewerPath = appInfo.getViewerPath();
    String entryPath = entry.getPath().toString();
    if (!(new File(entryPath).exists())) {
      JOptionPane.showMessageDialog(null, entryPath + " にアクセスできません");
      return;
    }
    if (Objects.nonNull(viewerPath) && new File(viewerPath).canExecute()) {
      if (!entryPath.contains(" ")) {
        entryPath = escapeForCMD(entryPath);
      }
      try {
        String[] command = {"cmd", "/c", "\"" + viewerPath, entryPath + "\""};
        Runtime.getRuntime().exec(command);
      } catch (IOException e1) {
        // TODO 自動生成された catch ブロック
        e1.printStackTrace();
      }
    } else {
      JOptionPane.showMessageDialog(null, "ビューワアプリケーションが設定されていません");
    }
  }

  /**
   * 保存先ディレクトリを決定して(必要なら作成した上で)返す<BR>
   * <BR>
   * AppInfoを参照してディレクトリ分割方法を決定し、現行保存先に既に保存されているエントリ数および容量と照らし合わせ、<BR>
   * 新規ディレクトリの作成が不要であれば現行保存先を、必要なら新しいディレクトリを作成して返す
   *
   * @param appInfo 分割条件やディレクトリ名接頭辞を読み出す
   * @param srcFile 保存するファイルまたはディレクトリ
   * @return 保存先ディレクトリのFileインスタンス
   */
  public static File prepSaveDir(AppInfo appInfo, File srcFile) {

    File parentDir = new File(appInfo.getSaveDir());
    String saveDirPrefix = appInfo.getChildDirPrefix();
    File saveDir = getDirToSave(parentDir, saveDirPrefix);

    long dirSizeLimit = appInfo.getChildDirSplitSize();
    dirSizeLimit = dirSizeLimit * 1024 * 1024;
    if (appInfo.getSplitChildDirBySize()) {
      long dirSize = FileUtils.sizeOfDirectory(saveDir);
      long sizeToMove;
      if (srcFile.isFile()) {
        sizeToMove = srcFile.length();
      } else {
        sizeToMove = FileUtils.sizeOfDirectory(srcFile);
      }
      if ((sizeToMove + dirSize) > dirSizeLimit) {
        saveDir = getNewSaveDir(saveDir);
      }
    } else {
      int numberSaved = saveDir.listFiles().length;
      if ((numberSaved + 1) > appInfo.getChildDirSplitNumber()) {
        saveDir = getNewSaveDir(saveDir);
      }
    }

    return saveDir;
  }

  /**
   * 指定の親ディレクトリ直下にある指定の接頭辞を持つ連番ディレクトリのうち最新のものを返す<BR>
   * 連番は10進数4桁<BR>
   * 存在しない場合は(親ディレクトリとも)作成する<BR>
   *
   * @param parentDir 親ディレクトリ
   * @param saveDirPrefix 子ディレクトリ接頭辞 空文字列やnullも可
   * @return 子ディレクトリのFileインスタンス
   */
  public static File getDirToSave(File parentDir, String saveDirPrefix) {

    if (!parentDir.exists()) {
      int answer =
          JOptionPane.showConfirmDialog(null, "指定の保存先 " + parentDir.toString() + " は存在しません。作成しますか？",
              "保存ディレクトリ作成の確認", JOptionPane.OK_CANCEL_OPTION);
      if (answer == JOptionPane.OK_OPTION) {
        if (!parentDir.mkdirs()) {
          JOptionPane.showMessageDialog(null, parentDir.toString() + " を作成できません。処理を中止します");
          return null;
        }
      } else {
        JOptionPane.showMessageDialog(null, "処理を中止します");
        return null;
      }
    }

    if (Objects.isNull(saveDirPrefix)) {
      saveDirPrefix = "";
    }
    File dirToSave;
    Integer childDirIdx = null;
    ArrayList<File> childDirList = new ArrayList<File>();
    Comparator<File> fileComparator = Comparator.comparing(File::getName).reversed();
    for (File file : parentDir.listFiles()) {
      if (file.getName().matches("^" + saveDirPrefix + "\\d{4}$")) {
        childDirList.add(file);
      }
    }
    childDirList =
        (ArrayList<File>) childDirList.stream().sorted(fileComparator).collect(Collectors.toList());
    if (childDirList.size() > 0) {
      Pattern pattern = Pattern.compile("^" + (saveDirPrefix) + "(\\d{4})$");
      Matcher matcher = pattern.matcher(childDirList.get(0).getName());
      if (matcher.find()) {
        childDirIdx = Integer.valueOf(matcher.group(1));
      }
    } else {
      childDirIdx = 1;
    }
    dirToSave = new File(parentDir, saveDirPrefix + String.format("%04d", childDirIdx));
    if (!dirToSave.exists()) {
      dirToSave.mkdir();
    }
    return dirToSave;
  }

  /**
   * 現行の保存ディレクトリと同じ階層にあたらしい保存ディレクトリを作って返す<BR>
   * 既に同名のディレクトリが存在する場合は新規作成はせず、既存のディレクトリをそのまま使用する
   *
   * @param currentSaveDir 現行の保存ディレクトリ
   * @return 新しい保存ディレクトリ
   * @throws IllegalArgumentException
   */
  public static File getNewSaveDir(File currentSaveDir) throws IllegalArgumentException {
    String currentDirName = currentSaveDir.getName();
    Pattern pattern = Pattern.compile("(\\d{4}$)");
    Matcher matcher = pattern.matcher(currentDirName);
    String prefix = null;
    Integer idx = null;
    if (matcher.find()) {
      idx = Integer.valueOf(matcher.group(1));
      prefix = currentDirName.replaceFirst("(\\d{4}$)", "");
    } else {
      throw new IllegalArgumentException(
          "メソッド getNewSaveDir に想定外の名 " + currentDirName + " を持つ File が渡された");
    }
    idx++;
    String newDirName = prefix + String.format("%04d", idx);
    File newSaveDir = new File(currentSaveDir.getParent(), newDirName);
    if (!newSaveDir.exists()) {
      newSaveDir.mkdir();
    }
    return newSaveDir;
  }

  /**
   * Windowsのcmd.exeに引数として渡すために特殊文字をエスケープして返す<BR>
   * エスケープされる特殊文字は & ( ) % ^<BR>
   * explorer.exeの引数に使う場合、このメソッドの戻り値を"^\""でくくる必要がある<BR>
   * (パス中の"="に対応するため)
   *
   * @param pathString File.getPath().toString()などして取得したパス文字列
   * @return cmd.exeで使用される特殊文字を"^"でエスケープした文字列
   */
  public static String escapeForCMD(String pathString) {
    return pathString.replaceAll("\\^", "^^").replaceAll("&", "^&").replaceAll("%", "^%")
        .replaceAll("\\(", "^(").replaceAll("\\)", "^)");
  }

  /**
   * 所与のFileをOS標準のファイラ上に表示する<BR>
   * Fileがディレクトリの場合はその内部を、ファイルであれば親ディレクトリを開く
   *
   * @param file
   * @throws IOException
   */
  public static void showInFiler(File file) throws IOException {
    String osName = System.getProperty("os.name").toLowerCase();
    ArrayList<String> commandList = new ArrayList<String>();
    if (osName.startsWith("windows")) {
      commandList.add("cmd");
      commandList.add("/c");
      StringBuilder argBuilder = new StringBuilder("%windir%\\explorer ");
      if (file.isFile()) {
        argBuilder.append("/select,");
      }
      argBuilder.append("^\"" + escapeForCMD(file.getPath().toString()) + "^\"");
      commandList.add(argBuilder.toString());
    } else if (osName.startsWith("mac")) {
      commandList.add("open");
      if (file.isDirectory()) {
        commandList.add(file.getPath().toString());
      } else {
        commandList.add(file.getParentFile().getPath().toString());
      }
    } else {
      JOptionPane.showMessageDialog(null,
          "使用中のOSに対応したコマンドが登録されていません(クラス:Util, メソッド:showInFiler(File file))", "エラー",
          JOptionPane.ERROR_MESSAGE);
      return;
    }
    Runtime runtime = Runtime.getRuntime();
    runtime.exec(commandList.toArray(new String[commandList.size()]));
  }

}
