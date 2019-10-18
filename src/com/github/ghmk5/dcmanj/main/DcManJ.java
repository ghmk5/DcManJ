package com.github.ghmk5.dcmanj.main;

import java.awt.EventQueue;
import java.awt.Font;
import java.awt.FontFormatException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import javax.swing.ToolTipManager;
import com.github.ghmk5.dcmanj.gui.BrowserWindow;
import com.github.ghmk5.dcmanj.info.AppInfo;
import com.github.ghmk5.dcmanj.util.Util;

public class DcManJ {

  public File prefFile;
  public AppInfo appInfo;
  public ArrayList<BrowserWindow> listBrowserWindows;
  public File dbFile;
  // public Connection sqlConnection;
  // public Statement statement = null;
  public String conArg;
  public Font tableFont;


  public DcManJ() {}


  public static void main(String[] args) throws URISyntaxException, IOException, SQLException {

    // 明示的にアンチエイリアスをオンにする WindowsでL&Fに"Windows"または"WindowsClassic"を指定したとき以外は
    // デフォルトでONなので意味がないが、悪影響もないようなのでそのままで良い
    System.setProperty("awt.useSystemAAFontSettings", "on");
    System.setProperty("swing.aatext", "true");

    DcManJ main = new DcManJ();
    main.initialize();

    EventQueue.invokeLater(new Runnable() {
      public void run() {

        try {
          main.initialize();
        } catch (URISyntaxException | IOException | SQLException e) {
          // TODO 自動生成された catch ブロック
          e.printStackTrace();
        }

        BrowserWindow browserWindow = new BrowserWindow(main);
        main.listBrowserWindows.add(browserWindow);

        Util.setRect(browserWindow, main.appInfo.getRectMain());
        try {
          browserWindow.refreshTable("select rowid, * from magdb order by rowid desc;");
        } catch (SQLException e) {
          // TODO 自動生成された catch ブロック
          e.printStackTrace();
        }
        browserWindow.setVisible(true);
      }
    });
  }

  void initialize() throws URISyntaxException, IOException, SQLException {

    // 初期設定ファイルの検索 なければ作る
    String prefFileName = "DcManJPrefs.xml";
    Path parentPath;
    if (Util.getApplicationPath(this.getClass()).toFile().isFile()) {
      parentPath = Util.getApplicationPath(this.getClass()).getParent();
    } else {
      parentPath = Util.getApplicationPath(this.getClass());
    }
    prefFile = new File(parentPath.toFile(), prefFileName);
    if (prefFile.exists()) {
      appInfo = (AppInfo) Util.readBean(prefFile);
    } else {
      appInfo = new AppInfo();
      Util.writeBean(prefFile, appInfo);
    }

    // データベースファイルの検索 なければjar内リソースからコピーしてくる
    // 接続してstatementを用意する
    String dbFileName = "DcManJ.db";
    dbFile = new File(parentPath.toFile(), dbFileName);
    if (!dbFile.exists()) {
      InputStream inputStream = DcManJ.class.getResourceAsStream("/data/DcManJ.db");
      Files.copy(inputStream, dbFile.toPath());
    }
    conArg = "jdbc:sqlite:" + dbFile.toPath();
    // sqlConnection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.toPath());
    // statement = sqlConnection.createStatement();

    // テーブルの表示に使うフォントを生成
    try {
      tableFont = Font.createFont(Font.TRUETYPE_FONT,
          // this.getClass().getResourceAsStream("/data/BIZ-UDGOTHICB.TTC"));
          this.getClass().getResourceAsStream("/data/VL-PGothic-Regular.ttf"));
      // this.getClass().getResourceAsStream("/data/mplus-2m-bold.ttf"));
      // this.getClass().getResourceAsStream("/data/mplus-2m-medium.ttf"));
      tableFont = tableFont.deriveFont(Font.BOLD, 11.0f);
    } catch (FontFormatException e2) {
      System.out.println("jar同梱フォントファイルが不正です");
      e2.printStackTrace();
    } catch (IOException e2) {
      System.out.println("jar同梱フォントファイルが読み込めません");
      e2.printStackTrace();
    }

    // L&FをOSデフォルトに設定する(Windowsでは"Windows"、MacOSでは"MacOS"
    // これをやらないと、WindowsではMetalが使用される。MacOSでは無指定でMacOSが使用されるので関係ない
    // try {
    // UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    // } catch (ClassNotFoundException | InstantiationException | IllegalAccessException
    // | UnsupportedLookAndFeelException ex) {
    // ex.printStackTrace();
    // Toolkit.getDefaultToolkit().beep();
    // }

    listBrowserWindows = new ArrayList<BrowserWindow>();

    ToolTipManager.sharedInstance().setDismissDelay(Integer.MAX_VALUE);
  }

}
