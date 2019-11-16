package com.github.ghmk5.dcmanj.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Objects;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import org.apache.commons.io.FileUtils;
import com.github.ghmk5.dcmanj.info.AppInfo;
import com.github.ghmk5.dcmanj.info.Entry;
import com.github.ghmk5.dcmanj.main.DcManJ;
import com.github.ghmk5.dcmanj.util.Util;

public class BrowserTable extends ExtendedTable {
  DcManJ main;
  BrowserWindow browserWindow;
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
  HashMap<Integer, Entry> entryMap;

  {
    dbColumnNameMap = new HashMap<String, String>();
    for (int i = 0; i < tableHeaders.length; i++) {
      dbColumnNameMap.put(tableHeaders[i], dbColumnNames[i]);
    }
  }

  // ファイルにアクセスできない場合は文字を灰色にする
  @Override
  public Component prepareRenderer(TableCellRenderer tcr, int row, int column) {
    Component c = super.prepareRenderer(tcr, row, column);
    Integer dbRowID = (Integer) getValueAt(row, 0);

    Entry entry = entryMap.get(dbRowID);
    Boolean ready = null;
    if (Objects.nonNull(entry)) {
      ready = entry.getPath().toFile().exists();
      if (!ready) {
        c.setForeground(Color.GRAY);
      } else {
        c.setForeground(getForeground());
      }
    } else {
      c.setForeground(getForeground());
    }
    return c;

  }


  public BrowserTable(DcManJ main, BrowserWindow browserWindow) {
    super();

    entryMap = new HashMap<Integer, Entry>();

    this.main = main;
    this.browserWindow = browserWindow;
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

    // TableModelには入っているが表示しない列 -- 直接インデックスで指定すると、表示しない列の二つ目以降で番号がずれて分かりにくくなるので注意
    String[] columnsToHide =
        {"サークル", "著者", "タイトル(素)", "副題", "巻号", "issue", "備考", "パス", "日付", "元ネタ", "発刊"};
    for (String columnName : columnsToHide) {
      this.removeColumn(
          this.getColumnModel().getColumn(this.getColumnModel().getColumnIndex(columnName)));
    }

    // "容量"列に右詰めのレンダラを設定
    DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
    rightRenderer.setHorizontalAlignment(JLabel.RIGHT);
    getColumnModel().getColumn(this.getColumnModel().getColumnIndex("容量"))
        .setCellRenderer(rightRenderer);


    // コンテキストメニュー
    TableContextMenu contextMenu = new TableContextMenu(this);
    setComponentPopupMenu(contextMenu);

    // 行ダブルクリックでビューワを開く処理
    addMouseListener(new MouseAdapter() {
      public void mouseClicked(MouseEvent me) {
        if (me.getClickCount() == 2 && getSelectedRows().length == 1) {
          Util.openWithViewer(main.appInfo, getEntries().get(0));
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
    Action openFilerAction;
    Action openAttrDialogAction;
    Action removeEntriesAction;
    Action moveEntriesAction;

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
      copyAuthor = new CopyValue(browserTable, "author", "著者名");
      copyMenu.add(copyAuthor);
      copyCircle = new CopyValue(browserTable, "circle", "サークル名");
      copyMenu.add(copyCircle);
      copyTitle = new CopyValue(browserTable, "title", "タイトル");
      copyMenu.add(copyTitle);
      copyPath = new CopyValue(browserTable, "path", "ファイルのパス");
      copyMenu.add(copyPath);

      manageMenu = new JMenu("エントリの管理");
      add(manageMenu);
      openFilerAction = new AbstractAction("ファイルを表示") {

        @Override
        public void actionPerformed(ActionEvent e) {
          Util.showInFiler(browserWindow, browserTable.getEntries().get(0).getPath().toFile());
        }
      };
      manageMenu.add(openFilerAction);
      openAttrDialogAction = new AbstractAction("属性を設定...") {

        @Override
        public void actionPerformed(ActionEvent e) {
          ArrayList<Entry> entryList = browserTable.getEntries();
          AttrDialog attrDialog = new AttrDialog(browserWindow, entryList);
          attrDialog.setLocation(main.appInfo.getRectAttr().getLocation());
          attrDialog.setModal(true);
          attrDialog.setVisible(true);
          try {
            Util.updateDB(entryList, new File(main.appInfo.getDbFilePath()));
          } catch (SQLException e1) {
            Util.showErrorMessage(null, e1, e1.getMessage());
            e1.printStackTrace();
          }
          for (Entry entry : entryList) {
            Entry entryInMap = entryMap.get(entry.getId());
            if (Objects.nonNull(entryInMap)) {
              entryMap.put(entry.getId(), entry);
            }
          }
          updateSelectedRows();
        }
      };
      manageMenu.add(openAttrDialogAction);
      removeEntriesAction = new AbstractAction("エントリを削除...") {

        @Override
        public void actionPerformed(ActionEvent e) {
          ArrayList<String> filesNotFound = new ArrayList<String>();
          // 確認ダイアログ表示
          int answer = JOptionPane.showConfirmDialog(browserWindow, "選択されたエントリを削除します。よろしいですか？",
              "削除の確認", JOptionPane.YES_NO_OPTION);
          if (answer == JOptionPane.YES_OPTION) {
            ArrayList<Entry> entryList = browserTable.getEntries();
            model = (DefaultTableModel) browserTable.getModel();
            File file;
            String sql = "";
            try {
              Connection connection = DriverManager.getConnection(main.conArg);
              Statement statement = connection.createStatement();
              for (Entry entry : entryList) {
                // ファイル削除
                try {
                  file = entry.getPath().toFile();
                  if (file.isFile()) {
                    System.out.println(file.delete());
                    // file.delete();
                  } else if (file.isDirectory()) {
                    FileUtils.deleteDirectory(file);
                  } else {
                    // 消すべきファイルが見つからなかった場合はここに来る
                    // ファイルが無いことを承知の上でレコードだけ消したい場合を考慮し、エラーメッセージは後でまとめて出す
                    // (ここで出すとループ1回毎にメッセージが出ることになるため)
                    filesNotFound.add(file.getPath().toString());
                  }
                } catch (IOException e1) {
                  // 消すべきファイルは見つかったけど何らかの理由で消せなかった場合はここに来る
                  String message = "以下のファイルが削除できません\n\n  ";
                  message += entry.getPath().toString();
                  message += "\n\n  ファイルのパスをクリップボードにコピーしました";
                  StringSelection selection = new StringSelection(entry.getPath().toString());
                  Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection,
                      selection);
                  JOptionPane.showMessageDialog(browserWindow, message, "ファイルの削除エラー",
                      JOptionPane.ERROR_MESSAGE);
                  e1.printStackTrace();
                }
                // データベースから削除
                sql = "DELETE FROM magdb WHERE ROWID = " + String.valueOf(entry.getId()) + ";";
                statement.execute(sql);
                // entryMapから削除
                entryMap.remove(entry.getId());
              }
              statement.close();
              connection.close();
            } catch (SQLException e2) {
              Util.showErrorMessage(browserWindow, e2, sql);
              e2.printStackTrace();
            }
            // テーブルモデルから削除
            // getSelectedRows()の戻り値は、選択した順番に関わりなく数値昇順
            // そのまま順に(=上から)削除していくと削除すべきIDがずれるので(例えば行ID2と3を削除したいとき、2を削除するとそれまで3だった行はID2になっている)、
            // 降順で(=下から)削除していく必要がある
            ArrayList<Integer> modelRowIDs = new ArrayList<Integer>();
            for (int rowID : getSelectedRows()) {
              modelRowIDs.add(convertRowIndexToModel(rowID));
            }
            modelRowIDs.sort(Comparator.reverseOrder());
            for (Integer modelRowID : modelRowIDs) {
              model.removeRow(modelRowID);
            }
            // 消すべきファイルが見つからないエントリがあった場合
            if (filesNotFound.size() > 0) {
              String message = "以下のファイルは見つからなかったため削除できませんでした\n\n  ";
              message +=
                  String.join("\n  ", filesNotFound.toArray(new String[filesNotFound.size()]));
              message += "\n\n  ファイルのパスをクリップボードにコピーしました";
              message += "\n\n  オフラインボリュームに保存されているなどの場合、必要に応じて手動で削除してください";
              StringSelection selection = new StringSelection(
                  String.join("\n", filesNotFound.toArray(new String[filesNotFound.size()])));
              Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
              JOptionPane.showMessageDialog(browserWindow, message, "ファイルの削除エラー",
                  JOptionPane.ERROR_MESSAGE);
            }
          }
        }
      };
      manageMenu.add(removeEntriesAction);
      moveEntriesAction = new MoveEntries(browserWindow, main.appInfo);
      manageMenu.add(moveEntriesAction);
    }

    @Override
    public void show(Component c, int x, int y) {
      if ((((BrowserTable) c).getSelectedRows().length > 0)) {
        boolean isSelectedSingleRow = (((BrowserTable) c).getSelectedRows().length == 1);
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
      BrowserWindow newWindow = new BrowserWindow(main);
      main.listBrowserWindows.add(newWindow);
      newWindow.refreshTable(sql);
      newWindow.setLocationCascadeOn(browserWindow);
      newWindow.setSize(new Dimension(browserWindow.getWidth(), newWindow.getHeight()));
      newWindow.setVisible(true);
    }
  }

  class CopyValue extends AbstractAction {

    BrowserTable browserTable;
    String fieldName;

    public CopyValue(BrowserTable browserTable, String fieldName, String actionName) {
      super(actionName);
      this.browserTable = browserTable;
      this.fieldName = fieldName;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      String stringToCopy;
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
    }

  }

  private class MoveEntries extends AbstractAction {

    Window caller;
    AppInfo appInfo;

    public MoveEntries(Window caller, AppInfo appInfo) {
      super("ファイルを移動...");
      this.caller = caller;
      this.appInfo = appInfo;
    }

    @Override
    public void actionPerformed(ActionEvent e) {

      ArrayList<Entry> entryList = getEntries();
      MoveOptionDialog moveOptionDialog = new MoveOptionDialog(caller, appInfo, entryList);
      Util.mapESCtoCancel(moveOptionDialog);
      moveOptionDialog.setLocationRelativeTo(caller);
      moveOptionDialog.setModal(true);
      moveOptionDialog.setVisible(true);

    }

  }

  /**
   * 選択された行に対応するEntryのリストを返す
   *
   * @return
   */
  private ArrayList<Entry> getEntries() {
    ArrayList<Entry> listEntries = new ArrayList<Entry>();
    Entry entry;

    for (Object object : getSelectedColumnValues("ID")) {
      Integer dbRowID = (Integer) object;
      String sql = "select rowid, * from magdb where rowid = " + String.valueOf(dbRowID) + ";";
      try {
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
      } catch (SQLException e) {
        Util.showErrorMessage(browserWindow, e, sql);
        e.printStackTrace();
        return null;
      }
    }
    return listEntries;

  }

  public void refreshMap(ArrayList<Entry> entryList) {
    Entry entryInMap;
    for (Entry entry : entryList) {
      entryInMap = entryMap.get(entry.getId());
      if (Objects.nonNull(entryInMap)) {
        entryMap.put(entry.getId(), entry);
      }
    }
  }

  public void updateSelectedRows() {
    DefaultTableColumnModel columnModel = (DefaultTableColumnModel) getColumnModel();
    Integer rowID;
    Entry entry;
    int columnIdx;
    TableModel model = getModel();

    for (int tableRowIdx : getSelectedRows()) {
      tableRowIdx = convertRowIndexToModel(tableRowIdx);
      rowID = (Integer) model.getValueAt(tableRowIdx, columnModel.getColumnIndex("ID"));
      entry = entryMap.get(rowID);

      // tableで表示されているカラムのindexと、データモデルのカラムindexは異なる(場合がある)
      // データモデルのカラム全てを表示しており、かつカラムの順番を入れ替えていない場合は一致するが、
      // ここでやっているようにカラムの一部を非表示にしていたり、カラムの動的な入れ替えを許可していたりすると
      // 食い違いが生じる
      // ColumnModel.getColumnIndex()で取得するインデックスはテーブルで表示中の列順を示すものであり、
      // TableModel.setValueAt()で指定すべきはデータモデルにおける列順であって、これを得るためには
      // JTable.convertColumnIndexToModelメソッドで変換してやる必要がある
      columnIdx = convertColumnIndexToModel(columnModel.getColumnIndex("種別"));
      model.setValueAt(entry.getType(), tableRowIdx, columnIdx);
      columnIdx = convertColumnIndexToModel(columnModel.getColumnIndex("成"));
      model.setValueAt(entry.getAdult(), tableRowIdx, columnIdx);
      columnIdx = convertColumnIndexToModel(columnModel.getColumnIndex("タイトル"));
      model.setValueAt(entry.getEntryTitle(), tableRowIdx, columnIdx);
      columnIdx = convertColumnIndexToModel(columnModel.getColumnIndex("頁数"));
      model.setValueAt(entry.getPages(), tableRowIdx, columnIdx);
      columnIdx = convertColumnIndexToModel(columnModel.getColumnIndex("容量"));
      model.setValueAt(String.format("%.2f", entry.getSize()), tableRowIdx, columnIdx);
    }

  }

  /**
   * 所与のSQL文でデータベースを検索し、その結果に基づいてテーブルを書き換える
   *
   *
   * @param sql SELECTステートメントであり、かつ要求するカラムが rowid, * でなければならない
   */
  public void refresh(String sql) {

    try {
      DefaultTableModel model = (DefaultTableModel) this.getModel();
      model.setRowCount(0);

      ArrayList<Object[]> listOfArray = new ArrayList<Object[]>();
      Object[] row;

      Connection connection = DriverManager.getConnection(main.conArg);
      Statement statement = connection.createStatement();
      ResultSet resultSet = statement.executeQuery(sql);
      while (resultSet.next()) {
        Entry entry = new Entry(resultSet);
        entryMap.put(entry.getId(), entry);
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
    } catch (SQLException e) {
      Util.showErrorMessage(browserWindow, e, sql);
      e.printStackTrace();
    }

  }

}
