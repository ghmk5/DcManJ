package com.github.ghmk5.dcmanj.util;

import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.StringSelection;
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
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.OffsetDateTime;
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
  public static String PLATFORM;

  static {
    String osName = System.getProperty("os.name").toLowerCase();
    if (osName.startsWith("windows")) {
      PLATFORM = "win";
    } else if (osName.startsWith("mac")) {
      PLATFORM = "mac";
    } else {
      PLATFORM = "unknown";
    }
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
   * nullであり得る2つのObjectが一致するか否かを返す<BR>
   * 2つが共にnullの場合は真、片方のみがnullであれば偽を返し、2つが共にnonNullであればequals()の結果を返す
   *
   * @param subject
   * @param object
   * @return
   */
  public static Boolean isIdentical(Object subject, Object object) {
    boolean result;
    if (Objects.isNull(subject)) {
      if (Objects.isNull(object)) {
        result = true;
      } else {
        result = false;
      }
    } else {
      if (Objects.isNull(object)) {
        result = false;
      } else {
        result = subject.equals(object);
      }
    }
    return result;
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
      JOptionPane.showConfirmDialog(null, "指定された初期設定ファイルの読み込みに失敗しました", "Beanファイル読み込みエラー",
          JOptionPane.ERROR_MESSAGE);
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
      JOptionPane.showConfirmDialog(null, "指定された初期設定ファイルの書き込みに失敗しました", "Beanファイル書き込みエラー",
          JOptionPane.ERROR_MESSAGE);
      e.printStackTrace();
    }
  }

  /**
   * AppInfoをファイルに記録する
   *
   * @param appInfo
   */
  public static void writeAppInfo(AppInfo appInfo) {
    File beanFile = new File(appInfo.getPrefFilePath());
    writeBean(beanFile, appInfo);
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

  /**
   * SQL文のLIKEステートメントで扱うためにクエリワードをクォートして返す
   *
   * @param string クォートしたいクエリワード
   * @return エスケープする必要がある文字列をエスケープした上でシングルクォートで囲んだクエリワード
   */
  public static String quoteForLike(String string) {
    String result;
    result = string.replaceAll("\'", "\'\'");
    result = result.replaceAll("_", "^_");
    result = result.replaceAll("%", "^%");
    result = "\'%" + result + "%\'";
    return result;
  }

  /**
   * Entryのフィールド値のうち、データベースフィールドに適用するものを<BR>
   * SQLのINSERT文やUPDATE文で使えるように適切にエスケープ/クォートして返す<BR>
   * ※ LIKE queryや GLOB queryで使用することは想定していない また、エスケープが必要な文字<BR>
   * (ここではシングルクォートのみ)が混ざることが考えにくいオブジェクト型では処理を省略してある
   *
   * @param object Entryクラスのフィールド値
   * @return objectのクラスに応じて適切にエスケープ/クォートされたString値
   * @throws IllegalArgumentException 使用されないはずのオブジェクトクラスが与えられたときに発生
   */
  public static String quoteForSQL(Object object) throws IllegalArgumentException {
    String result;
    if (object instanceof String) {
      result = "'" + ((String) object).replaceAll("\'", "\'\'") + "'";
    } else if (object instanceof Boolean) {
      result = "'" + String.valueOf(object) + "'";
    } else if ((object instanceof Integer) || (object instanceof Double)) {
      result = String.valueOf(object);
    } else if (object instanceof Path) {
      result = "'" + ((Path) object).toFile().getAbsolutePath().replaceAll("\'", "\'\'") + "'";
    } else if (object instanceof OffsetDateTime) {
      result = "'" + ((OffsetDateTime) object).format(DTF) + "'";
    } else if (Objects.isNull(object)) {
      result = "null";
    } else {
      throw new IllegalArgumentException(
          "想定されていないクラスのインスタンスが与えられた: " + object.getClass().toString());
    }
    return result;
  }

  /**
   * 所与のエントリの情報を新規レコードとしてデータベースに挿入する
   *
   * @param entry
   * @throws SQLException
   */
  public static void putNewRecord(com.github.ghmk5.dcmanj.info.Entry entry, File dbFile)
      throws SQLException {

    Object[] values = {entry.getType(), entry.getAdult(), entry.getCircle(), entry.getAuthor(),
        entry.getTitle(), entry.getSubtitle(), entry.getVolume(), entry.getIssue(), entry.getNote(),
        entry.getPages(), entry.getSize(), entry.getPath().toString(),
        entry.getDate().format(Util.DTF), entry.getOriginal(), entry.getRelease()};
    String[] valueStrings = new String[values.length];
    for (int i = 0; i < values.length; i++) {
      valueStrings[i] = Util.quoteForSQL(values[i]);
    }
    StringBuilder stringBuilder = new StringBuilder("INSERT INTO magdb values(");
    stringBuilder.append(String.join(",", valueStrings));
    stringBuilder.append(");");
    String sql = stringBuilder.toString();

    String conArg = "jdbc:sqlite:" + dbFile.toPath();
    Connection connection = DriverManager.getConnection(conArg);
    Statement statement = connection.createStatement();
    statement.execute(sql);
    statement.close();
    connection.close();
  }

  /**
   * 所与のリストに含まれるEntryについてデータベースレコードとの相違を調べ、<BR>
   * 相違がある場合はレコードを更新する
   *
   * @param entryList
   * @param dbFile
   */
  public static void updateDB(ArrayList<com.github.ghmk5.dcmanj.info.Entry> entryList, File dbFile)
      throws SQLException {
    String sql;
    com.github.ghmk5.dcmanj.info.Entry entryInRecord;
    ArrayList<String> setPredicates;
    Connection connection;
    Statement statement;
    ResultSet resultSet;
    for (com.github.ghmk5.dcmanj.info.Entry entry : entryList) {
      sql = "select rowid, * from magdb where rowid is " + String.valueOf(entry.getId()) + ";";
      try {
        String conArg = "jdbc:sqlite:" + dbFile.toPath();
        connection = DriverManager.getConnection(conArg);
        statement = connection.createStatement();
        resultSet = statement.executeQuery(sql);
        entryInRecord = new com.github.ghmk5.dcmanj.info.Entry(resultSet);
        resultSet.close();
        statement.close();
        if (entry.isIdenticalTo(entryInRecord)) {
          continue;
        } else {
          setPredicates = new ArrayList<String>();
          HashMap<String, Object> updatedValueMap = entry.getUpdatedValueMap(entryInRecord);
          for (String columnName : updatedValueMap.keySet()) {
            setPredicates
                .add(columnName + " = " + Util.quoteForSQL(updatedValueMap.get(columnName)));
          }
          sql = "update magdb set "
              + String.join(", ", setPredicates.toArray(new String[setPredicates.size()]));
          sql += " where rowid = ";
          sql += String.valueOf(entry.getId());
          sql += ";";
          statement = connection.createStatement();
          statement.execute(sql);
          statement.close();
        }
        connection.close();
      } catch (SQLException e) {
        throw new SQLException(sql);
      }
    }

  }

  /**
   * Runtime.exec()で発生したIOException、またはSQLステートメント実行時に発生したSQLExceptionに対する<BR>
   * エラーメッセージを表示する
   *
   * @param owner メッセージダイアログを表示する親コンポーネント null可
   * @param e Exceptionのインスタンス
   * @param string runtimeに与えたコマンドもしくはデータベースに与えたSQL文
   */
  public static void showErrorMessage(Component owner, Exception e, String string) {
    String errorType;
    String firstHalfOfMessage;
    String secondHalfOfMessage;
    String message;
    if (e instanceof IOException) {
      // Runtime.getRuntime().exec(new String[] {})実行時のエラーのみを想定している
      // 他の原因でIOExceptionが発生した場合には適用できないので注意
      // また、Runtime.exec()から出たIOExceptionであっても、cmd.exeがエラーを返さない場合(コマンドが見つからないなど)は捕捉できない
      errorType = "IOエラー";
      firstHalfOfMessage = "下記コマンド実行時にIOExcepationが発生しました";
      secondHalfOfMessage = "コマンド文字列をクリップボードにコピーしました";
      message = String.join("\n\n",
          (new String[] {firstHalfOfMessage, "  " + string, secondHalfOfMessage}));
      StringSelection selection = new StringSelection(string);
      Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
    } else if (e instanceof SQLException) {
      errorType = "SQLエラー";
      firstHalfOfMessage = "下記のSQL文の実行時にエラー(" + e.getMessage() + ")が発生しました";
      secondHalfOfMessage = "SQL文をクリップボードにコピーしました";
      message = String.join("\n\n",
          (new String[] {firstHalfOfMessage, "  " + string, secondHalfOfMessage}));
      StringSelection selection = new StringSelection(string);
      Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
    } else {
      errorType = "不明なエラー";
      message = "想定外のエラーが発生しました";
    }
    JOptionPane.showMessageDialog(owner, message, errorType, JOptionPane.ERROR_MESSAGE);
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
   * 所与のEntryを登録されたビューワで開く
   *
   * @param appInfo
   * @param entry
   */
  public static void openWithViewer(AppInfo appInfo, com.github.ghmk5.dcmanj.info.Entry entry,
      boolean isAoText) {

    String viewerPath;
    if (isAoText) {
      viewerPath = appInfo.getAoViewerPath();
    } else {
      viewerPath = appInfo.getViewerPath();
    }

    String entryPath = entry.getPath().toString();
    if (!(new File(entryPath).exists())) {
      JOptionPane.showMessageDialog(null, entryPath + " にアクセスできません");
      return;
    }
    if (Objects.nonNull(viewerPath)) {
      String command[];
      if (Util.PLATFORM.equals("win")) {
        if (!entryPath.contains(" ")) {
          entryPath = escapeForCMD(entryPath);
        }
        command = new String[4];
        command[0] = "cmd";
        command[1] = "/c";
        command[2] = "\"" + viewerPath;
        command[3] = entryPath + "\"";
      } else if (Util.PLATFORM.equals("mac")) {
        entryPath = entryPath.replaceAll(" ", "\\ ");
        viewerPath = viewerPath.replaceAll(" ", "\\ ");
        command = new String[4];
        command[0] = "open";
        command[1] = "-a";
        command[2] = viewerPath;
        command[3] = entryPath;
      } else {
        command = new String[2];
        command[0] = viewerPath;
        command[1] = entryPath;
      }
      try {
        Runtime.getRuntime().exec(command);
      } catch (IOException e) {
        showErrorMessage(null, e, String.join(" ", command));
        e.printStackTrace();
      }
    } else {
      JOptionPane.showMessageDialog(null, "ビューワアプリケーションが設定されていません");
    }
  }

  /**
   * 所与のFileをOS標準のファイラ上に表示する<BR>
   * Fileがディレクトリの場合はその内部を、ファイルであれば親ディレクトリを開く
   *
   * @param file
   * @throws IOException
   */
  public static void showInFiler(Window owner, File file) {
    ArrayList<String> commandList = new ArrayList<String>();
    if (Util.PLATFORM.equals("win")) {
      commandList.add("cmd");
      commandList.add("/c");
      StringBuilder argBuilder = new StringBuilder("%windir%\\explorer ");
      if (file.isFile()) {
        argBuilder.append("/select,");
      }
      argBuilder.append("^\"" + escapeForCMD(file.getPath().toString()) + "^\"");
      commandList.add(argBuilder.toString());
    } else if (Util.PLATFORM.equals("mac")) {
      commandList.add("open");
      if (file.isDirectory()) {
        commandList.add(file.getPath().toString());
      } else {
        commandList.add(file.getParentFile().getPath().toString());
      }
    } else {
      JOptionPane.showMessageDialog(null,
          "使用中のOSに対応したコマンドが登録されていません(クラス:Util, メソッド:showInFiler(File file))", "アプリケーションエラー",
          JOptionPane.ERROR_MESSAGE);
      return;
    }
    Runtime runtime = Runtime.getRuntime();
    String[] commandArray = commandList.toArray(new String[commandList.size()]);
    try {
      runtime.exec(commandArray);
    } catch (IOException e) {
      String commandString = String.join(" ", commandArray);
      showErrorMessage(owner, e, commandString);
      e.printStackTrace();
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
   * @param isImport 新規エントリとしてインポートもしくはインポート扱いの移動時true 他false
   * @return 保存先ディレクトリのFileインスタンス
   */
  public static File prepSaveDir(AppInfo appInfo, File srcFile, Boolean isImport) {

    File parentDir;
    String saveDirPrefix;
    File saveDir;

    if (isImport) {
      parentDir = new File(appInfo.getSaveDir());
      saveDirPrefix = appInfo.getChildDirPrefix();
      saveDir = getDirToSave(parentDir, saveDirPrefix);
    } else if (appInfo.getSelectDestDirOnMove()) {
      parentDir = new File(appInfo.getMoveDestDir());
      saveDirPrefix = appInfo.getChildlDirPrefixOnMove();
      if (appInfo.getUseChildDirOnMove()) {
        saveDir = getDirToSave(parentDir, saveDirPrefix);
      } else {
        saveDir = parentDir;
      }
    } else {
      saveDir = srcFile.getParentFile();
    }

    long dirSizeLimit = appInfo.getChildDirSplitSize();
    dirSizeLimit = dirSizeLimit * 1024 * 1024;
    if ((isImport && appInfo.getSplitChildDirBySize())
        || (!isImport && appInfo.getSplitChildDirBySizeOnMove())) {
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
    } else if ((isImport && appInfo.getSplitChildDirByNumber())
        || (!isImport && appInfo.getSplitChildDirByNumberOnMove())) {
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

}
