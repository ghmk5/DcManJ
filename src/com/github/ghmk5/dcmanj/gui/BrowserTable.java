package com.github.ghmk5.dcmanj.gui;

import java.awt.Component;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;
import com.github.ghmk5.dcmanj.info.Entry;
import com.github.ghmk5.dcmanj.main.DcManJ;

public class BrowserTable extends ExtendedTable {
  DcManJ main;
  DefaultTableModel model;
  String[] tableHeaders = {"ID", "種別", "成", "サークル", "著者", "タイトル(素)", "副題", "巻号", "issue", "タイトル",
      "備考", "頁数", "容量", "パス", "日付", "元ネタ", "発刊"};
  String[] dbColumnNames =
      {"rowid", "type", "adult", "circle", "author", "title", "subtitle", "volume", "issue",
          "displayedTitle", "note", "pages", "size", "path", "date", "original", "release"};
  HashMap<String, String> dbColumnNameMap;
  private Class[] classesInRow =
      {Integer.class, String.class, Boolean.class, String.class, String.class, String.class,
          String.class, String.class, String.class, String.class, String.class, Integer.class,
          Double.class, String.class, String.class, String.class, String.class};

  {
    dbColumnNameMap = new HashMap<String, String>();
    for (int i = 0; i < tableHeaders.length; i++) {
      dbColumnNameMap.put(tableHeaders[i], dbColumnNames[i]);
    }
  }

  public BrowserTable(DcManJ main) {
    super();

    this.main = main;
    // model = new BrowserTableModel();
    model = new DefaultTableModel(tableHeaders, 0) {

      // 各列が持つデータのクラスを指定
      @Override
      public Class getColumnClass(int columnIndex) {
        switch (classesInRow[columnIndex].getSimpleName()) {
          case "Integer":
            return Integer.class;
          case "String":
            return String.class;
          case "Boolean":
            return Boolean.class;
          default:
            return super.getColumnClass(columnIndex);
        }
      }
    };

    // 各カラムのidentifierとしてデータベースのカラム名を設定("タイトル"/"displayedTitle"だけはデータベース側に存在しないので注意)
    model.setColumnIdentifiers(tableHeaders);

    this.setModel(model);
    this.setRowSorter(new TableRowSorter<>((DefaultTableModel) this.getModel()));

    // TableModelには入っているが表示しない列 -- 直接インデックスで指定すると、表示しない列の二つ目以降で番号がずれるので分かりにくくなるので注意
    String[] columnsToHide =
        {"サークル", "著者", "タイトル(素)", "副題", "巻号", "issue", "備考", "パス", "日付", "元ネタ", "発刊"};
    for (String columnName : columnsToHide) {
      this.removeColumn(
          this.getColumnModel().getColumn(this.getColumnModel().getColumnIndex(columnName)));
    }

    // コンテキストメニュー
    TableContextMenu contextMenu = new TableContextMenu(this);
    setComponentPopupMenu(contextMenu);

    // 行ダブルクリックでビューワを開く処理
    addMouseListener(new MouseAdapter() {
      public void mouseClicked(MouseEvent me) {
        if (me.getClickCount() == 2 && getSelectedRows().length == 1) {
          try {
            int rowid = getEntries().get(0).getId();
            String sql = "select rowid, * from magdb where rowid = " + String.valueOf(rowid) + ";";
            Runtime runtime = Runtime.getRuntime();
            Connection connection = DriverManager.getConnection(main.conArg);
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(sql);
            String path = resultSet.getString("path");
            resultSet.close();
            statement.close();
            connection.close();
            String filerExecutablePath = "C:/Program Files/Honeyview/Honeyview.exe";
            String commandString = "\"" + filerExecutablePath + "\" " + path;
            // TODO 初期設定の画像ビューワ指定部分ができたら上2行を編集
            // cmdに与える引数ではダブルクォートの扱いが特殊なので注意すること
            // 参考 https://www.pg-fl.jp/program/dos/doscmd/cmd.htm
            String[] command = {"cmd", "/c", commandString};
            // String[] command =
            // {"cmd", "/c", "\"C:/Program Files/Honeyview/Honeyview.exe\" " + path};
            runtime.exec(command);
          } catch (SQLException | IOException e) {
            // TODO 自動生成された catch ブロック
            e.printStackTrace();
          }

        }
      }
    });

    // setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

  }

  class TableContextMenu extends JPopupMenu {

    JMenu searchMenu;
    Action searchSameAuthor;
    Action searchSameAuthorNW;
    Action searchSameCircle;
    Action searchSameCircleNW;
    Action searchSameTitle;
    Action searchSameTitleNW;
    JMenu copyMenu;
    Action copyCircle;
    Action copyAuthor;
    Action copyTitle;
    Action copyPath;
    JMenu manageMenu;
    Action showInFiler;
    Action setValues;
    Action openFilerAction;

    public TableContextMenu(BrowserTable browserTable) {
      super();

      searchMenu = new JMenu("検索");
      add(searchMenu);
      searchSameAuthor = new SearchInThis(browserTable, "author", "同じ著者");
      searchMenu.add(searchSameAuthor);
      searchSameAuthorNW = new SearchInNew(browserTable, "author", "同じ著者:新しいウィンドウ");
      searchMenu.add(searchSameAuthorNW);
      searchSameCircle = new SearchInThis(browserTable, "circle", "同じサークル");
      searchMenu.add(searchSameCircle);
      searchSameCircleNW = new SearchInNew(browserTable, "circle", "同じサークル:新しいウィンドウ");
      searchMenu.add(searchSameCircleNW);
      searchSameTitle = new SearchInThis(browserTable, "title", "同じタイトル");
      searchMenu.add(searchSameTitle);
      searchSameTitleNW = new SearchInNew(browserTable, "title", "同じタイトル:新しいウィンドウ");
      searchMenu.add(searchSameTitleNW);

      copyMenu = new JMenu("クリップボードにコピー");
      add(copyMenu);
      copyAuthor = new copyValue(browserTable, "author", "著者名");
      copyMenu.add(copyAuthor);
      copyCircle = new copyValue(browserTable, "circle", "サークル名");
      copyMenu.add(copyCircle);
      copyTitle = new copyValue(browserTable, "title", "タイトル");
      copyMenu.add(copyTitle);
      copyPath = new copyValue(browserTable, "path", "ファイルのパス");
      copyMenu.add(copyPath);

      openFilerAction = new AbstractAction("ファイルを表示") {

        @Override
        public void actionPerformed(ActionEvent e) {
          try {
            Path entryPath = browserTable.getEntries().get(0).getPath();

            // TODO この部分 環境判定で分岐
            String filerCommandString = "%windir%\\explorer ";
            filerCommandString += "/select,";
            filerCommandString += entryPath.toString();

            String[] command = {"cmd", "/c", filerCommandString};
            Runtime runtime = Runtime.getRuntime();
            runtime.exec(command);

          } catch (SQLException | IOException e1) {
            // TODO 自動生成された catch ブロック
            e1.printStackTrace();
          }
        }
      };
      add(openFilerAction);
    }

    @Override
    public void show(Component c, int x, int y) {
      boolean isSelectedSingleRow = (((BrowserTable) c).getSelectedRows().length == 1);
      try {
        boolean isAuthorNull =
            (Objects.nonNull(((BrowserTable) c).getEntries().get(0).getAuthor()));
        boolean isCircleNull =
            (Objects.nonNull(((BrowserTable) c).getEntries().get(0).getCircle()));
        searchMenu.setEnabled(isSelectedSingleRow);
        searchSameAuthor.setEnabled(isAuthorNull);
        searchSameAuthorNW.setEnabled(isAuthorNull);
        searchSameCircle.setEnabled(isCircleNull);
        searchSameCircleNW.setEnabled(isCircleNull);
        searchSameTitle.setEnabled(true);
        searchSameTitleNW.setEnabled(true);
        copyMenu.setEnabled(isSelectedSingleRow);
        copyCircle.setEnabled(isCircleNull);
        copyAuthor.setEnabled(isAuthorNull);
        openFilerAction.setEnabled(isSelectedSingleRow);
        super.show(c, x, y);
      } catch (SQLException e) {
        // TODO 自動生成された catch ブロック
        e.printStackTrace();
      }
    }

  }

  class SearchInThis extends AbstractAction {

    BrowserTable browserTable;
    String fieldName;

    public SearchInThis(BrowserTable browserTable, String fieldName, String actionName) {
      super(actionName);
      this.browserTable = browserTable;
      this.fieldName = fieldName;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      String queryWord;
      try {
        switch (fieldName) {
          case "author":
            queryWord = browserTable.getEntries().get(0).getAuthor();
            break;
          case "circle":
            queryWord = browserTable.getEntries().get(0).getCircle();
            break;
          case "title":
            queryWord = browserTable.getEntries().get(0).getTitle();
            break;
          default:
            queryWord = "";
        }
        String sql =
            "select rowid, * from magdb where " + fieldName + " like \'%" + queryWord + "%\';";
        refresh(sql);
      } catch (SQLException e1) {
        // TODO 自動生成された catch ブロック
        e1.printStackTrace();
      }
    }

  }

  class SearchInNew extends AbstractAction {

    BrowserTable browserTable;
    String fieldName;

    public SearchInNew(BrowserTable browserTable, String fieldName, String actionName) {
      super(actionName);
      this.browserTable = browserTable;
      this.fieldName = fieldName;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      String queryWord;
      try {
        switch (fieldName) {
          case "author":
            queryWord = browserTable.getEntries().get(0).getAuthor();
            break;
          case "circle":
            queryWord = browserTable.getEntries().get(0).getCircle();
            break;
          case "title":
            queryWord = browserTable.getEntries().get(0).getTitle();
            break;
          default:
            queryWord = "";
        }
        String sql =
            "select rowid, * from magdb where " + fieldName + " like \'%" + queryWord + "%\';";
        BrowserWindow browserWindow =
            (BrowserWindow) SwingUtilities.getAncestorOfClass(BrowserWindow.class, browserTable);
        BrowserWindow newWindow = new BrowserWindow(main);
        main.listBrowserWindows.add(newWindow);
        newWindow.refreshTable(sql);
        newWindow.setLocationCascadeOn(browserWindow);
        newWindow.setVisible(true);
      } catch (Exception e1) {
        // TODO: handle exception
      }
    }
  }

  class copyValue extends AbstractAction {

    BrowserTable browserTable;
    String fieldName;

    public copyValue(BrowserTable browserTable, String fieldName, String actionName) {
      super(actionName);
      this.browserTable = browserTable;
      this.fieldName = fieldName;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      String stringToCopy;
      try {
        switch (fieldName) {
          case "author":
            stringToCopy = browserTable.getEntries().get(0).getAuthor();
            break;
          case "circle":
            stringToCopy = browserTable.getEntries().get(0).getCircle();
            break;
          case "title":
            stringToCopy = browserTable.getEntries().get(0).getTitle();
            break;
          case "path":
            stringToCopy = browserTable.getEntries().get(0).getPath().toString();
            break;
          default:
            stringToCopy = null;
        }
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        // 通常なら文字列がnullの場合はコピーしないという処理を入れるべきところだが、ここではnullの場合は
        // メニューアイテムが無効化されるのでそのまま
        clipboard.setContents(new StringSelection(stringToCopy), null);
      } catch (SQLException ex) {
        // TODO: handle exception
      }
    }

  }

  private ArrayList<Entry> getEntries() throws SQLException {
    ArrayList<Entry> listEntries = new ArrayList<Entry>();
    Entry entry;
    DefaultTableColumnModel columnModel = (DefaultTableColumnModel) getColumnModel();
    int tableColumnIdx = columnModel.getColumnIndex("ID");
    for (int tableRowIdx : getSelectedRows()) {
      tableRowIdx = convertRowIndexToModel(tableRowIdx);
      int dbRowID = (int) getModel().getValueAt(tableRowIdx, tableColumnIdx);
      String sql = "select rowid, * from magdb where rowid = " + String.valueOf(dbRowID) + ";";
      Connection connection = DriverManager.getConnection(main.conArg);
      Statement statement = connection.createStatement();
      ResultSet resultSet = statement.executeQuery(sql);
      while (resultSet.next()) {
        entry = new Entry(resultSet);
        listEntries.add(entry);
      }
      resultSet.close();
      statement.close();
      connection.close();
    }
    return listEntries;
  }

  // public void refresh(ResultSet resultSet) throws SQLException {
  public void refresh(String sql) throws SQLException {

    DefaultTableModel model = (DefaultTableModel) this.getModel();

    // 全ての行を消去
    // for (int idx = model.getRowCount() - 1; idx >= 0; idx--) {
    // model.removeRow(idx);
    // }
    model.setRowCount(0);

    ArrayList<Object[]> listOfArray = new ArrayList<Object[]>();
    Object[] row;

    Connection connection = DriverManager.getConnection(main.conArg);
    Statement statement = connection.createStatement();
    ResultSet resultSet = statement.executeQuery(sql);
    while (resultSet.next()) {
      Entry entry = new Entry(resultSet);
      row = entry.getRowData();
      listOfArray.add(row);
    }
    resultSet.close();
    statement.close();
    connection.close();

    for (Object[] data : listOfArray) {
      model.addRow(data);
    }

    // 各列が持つデータのクラスに応じたソーターを設定(これをやらないと、Integerをもつ列のソート結果が 1 10 9 の順(Stringとしてのソート)になる)
    TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
    this.setRowSorter(sorter);

  }

  public void setColumnWidth(HashMap<String, Integer> columnWidthMap) {
    DefaultTableColumnModel defaultTableColumnModel = (DefaultTableColumnModel) getColumnModel();
    TableColumn column = null;
    int columnIdx;

    for (String key : columnWidthMap.keySet()) {
      try {
        columnIdx = defaultTableColumnModel.getColumnIndex(key);
        column = defaultTableColumnModel.getColumn(columnIdx);
        if (Objects.nonNull(columnWidthMap.get(key))) {
          column.setPreferredWidth(columnWidthMap.get(key));
        }
      } catch (Exception e) {
        System.out.println(key + " seems removed");
      }
    }
  }

}
