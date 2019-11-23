package com.github.ghmk5.dcmanj.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.zip.ZipException;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.ProgressMonitor;
import javax.swing.RowSorter.SortKey;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;
import com.github.ghmk5.dcmanj.info.AppInfo;
import com.github.ghmk5.dcmanj.info.Entry;
import com.github.ghmk5.dcmanj.main.DcManJ;
import com.github.ghmk5.dcmanj.util.Util;
import com.github.ghmk5.dcmanj.util.Worker;

public class ImportDialog extends JDialog {
  BrowserWindow browserWindow;
  AppInfo appInfo;
  ExtendedTable table;
  String[] tableHeaders = {"ID", "種別", "成", "サークル", "著者", "タイトル", "副題", "巻号", "issue", "元ファイル名",
      "保存ファイル名", "備考", "頁数", "容量", "パス", "日付", "元ネタ", "発刊"};
  // TableModelには入っているが表示しない列 -- 直接インデックスで指定すると、表示しない列の二つ目以降で番号がずれて分かりにくくなる
  String[] columnsToHide = {"ID", "成", "パス", "日付", "発刊"};
  private Class[] classesInRow =
      {Integer.class, String.class, Boolean.class, String.class, String.class, String.class,
          String.class, String.class, String.class, String.class, String.class, String.class,
          Integer.class, Double.class, String.class, String.class, String.class, String.class};
  String dirPath;
  File imptDir;
  HashMap<String, Entry> entryMap;
  ProgressMonitor progressMonitor;

  public ImportDialog(BrowserWindow browserWindow, AppInfo appInfo)
      throws IllegalArgumentException, ZipException, IOException {
    this.browserWindow = browserWindow;
    this.appInfo = appInfo;
    this.dirPath = appInfo.getImptDir();
    this.imptDir = new File(dirPath);
    if (!imptDir.exists() || !imptDir.isDirectory() || !imptDir.canRead()) {
      imptDir = chooseImptDir();
      if (Objects.isNull(imptDir) || !imptDir.exists() || !imptDir.isDirectory()
          || !imptDir.canRead()) {
        dispose();
      } else {
        dirPath = imptDir.getAbsolutePath().toString();
        appInfo.setImptDir(dirPath);
        Util.writeAppInfo(appInfo);
      }
    }

    // MacOSのVM(あるいはL&F)はTableだけ選択色と被選択時文字色がおかしいので修正
    if (Util.PLATFORM.equals("mac")) {
      UIManager.put("Table.selectionBackground",
          UIManager.getColor("EditorPane.selectionBackground"));
      SwingUtilities.updateComponentTreeUI(this);
    }

    setTitle(dirPath + " から新規エントリを読み込み");
    setLayout(new BorderLayout());
    JPanel panel = new JPanel(new FlowLayout());
    getContentPane().add(panel, BorderLayout.NORTH);
    JButton reloadButton = new JButton("再読込");
    reloadButton.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        readEntries();
        updateTable();
      }
    });
    panel.add(reloadButton);
    JButton changeDirButton = new JButton("ディレクトリ変更");
    changeDirButton.addActionListener(new ChangeDirAction());
    panel.add(changeDirButton);

    JButton editIgnoreListButton = new JButton("無視リストを編集");
    editIgnoreListButton.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        ImportDialog importDialog = (ImportDialog) SwingUtilities
            .getAncestorOfClass(ImportDialog.class, (Component) e.getSource());
        IgnoreListDialog ignoreListDialog = new IgnoreListDialog(importDialog, appInfo);
        ignoreListDialog.setLocationRelativeTo(importDialog);
        ignoreListDialog.setVisible(true);
      }
    });
    panel.add(editIgnoreListButton);

    JCheckBox checkBox = new JCheckBox("ディレクトリはzipして保管");
    checkBox.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        appInfo.setZipToStore(((JCheckBox) e.getSource()).isSelected());
        Util.writeAppInfo(appInfo);
      }
    });
    checkBox.setSelected(appInfo.getZipToStore());
    panel.add(checkBox);

    table = new ExtendedTable() {

      // entry.isReady()がfalseの行は赤文字にする
      @Override
      public Component prepareRenderer(TableCellRenderer tcr, int row, int column) {
        Component c = super.prepareRenderer(tcr, row, column);
        int viewColumnIdx = getColumnModel().getColumnIndex("元ファイル名");
        // viewColumnIdx = convertColumnIndexToModel(viewColumnIdx);
        String filename = (String) getValueAt(row, viewColumnIdx);
        Entry entry = entryMap.get(filename);
        Boolean ready = null;
        if (Objects.nonNull(entry)) {
          ready = entry.isReady();
          if (!ready) {
            c.setForeground(Color.RED);
          } else {
            c.setForeground(getForeground());
          }
        } else {
          c.setForeground(getForeground());
        }
        return c;
      }

    };
    table.getTableHeader().setFont(DcManJ.TABLEFONT);
    table.setFont(DcManJ.TABLEFONT);
    table.addMouseListener(new MouseAdapter() {
      public void mouseClicked(MouseEvent me) {
        if (me.getClickCount() == 2) {
          openAttrDialog();
        }
      }
    });

    // コンテキストメニュー
    TableContextMenu contextMenu = new TableContextMenu();
    table.setComponentPopupMenu(contextMenu);

    JScrollPane scrollPane = new JScrollPane(table);
    getContentPane().add(scrollPane, BorderLayout.CENTER);

    panel = new JPanel();
    getContentPane().add(panel, BorderLayout.SOUTH);
    panel.setLayout(new FlowLayout(FlowLayout.TRAILING));
    JButton cancelButton = new JButton("Done.");
    cancelButton.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        dispose();

      }
    });
    panel.add(cancelButton);

    addWindowListener(new ImportDialogListner());
    Util.mapESCtoCancel(this);

    readEntries();
    updateTable();

  }

  private File chooseImptDir() {
    JFileChooser fileChooser = new JFileChooser();
    fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    fileChooser.setApproveButtonText("選択");
    int selected = fileChooser.showOpenDialog(this);
    if (selected == JFileChooser.APPROVE_OPTION) {
      return fileChooser.getSelectedFile();
    } else {
      return null;
    }
  }

  // imptDirの内容を読み込んでentryMapを生成
  private void readEntries() {
    entryMap = new HashMap<String, Entry>();
    String fileName;
    Entry entry;
    ArrayList<String> fileNamesCouldntRead = new ArrayList<String>();
    ArrayList<String> namesToBeIgnoredList;
    String[] namesToBeIgnored = appInfo.getNamesToBeIgnored();
    if (Objects.nonNull(namesToBeIgnored) && namesToBeIgnored.length > 0) {
      namesToBeIgnoredList = new ArrayList<String>(Arrays.asList(namesToBeIgnored));
    } else {
      namesToBeIgnoredList = new ArrayList<String>();
    }
    for (File srcFile : imptDir.listFiles()) {
      if (srcFile.isHidden() || namesToBeIgnoredList.contains(srcFile.getName())) {
        continue;
      }
      fileName = srcFile.getName();
      try {
        entry = new Entry(srcFile, appInfo);
        entryMap.put(fileName, entry);
      } catch (IOException e) {
        fileNamesCouldntRead.add(fileName);
      }
    }
    if (fileNamesCouldntRead.size() > 0) {
      JOptionPane.showMessageDialog(null,
          "以下のファイルは読み込めなかったのでスキップされました\n  " + String.join("\n  ",
              fileNamesCouldntRead.toArray(new String[fileNamesCouldntRead.size()])),
          "読み込みエラー", JOptionPane.WARNING_MESSAGE);
    }
  }

  // entryMapの内容をテーブルに挿入
  public void updateTable() {
    // ソート順を保存しておく (テーブル内容を書き換えた後で再適用する)
    List<? extends SortKey> sortKeys = null;
    if (Objects.nonNull(table.getRowSorter())) {
      sortKeys = table.getRowSorter().getSortKeys();
    }

    // 全ての行を消去
    ((DefaultTableModel) table.getModel()).setRowCount(0);

    // entryListの内容からデータを生成
    Entry entry;
    File srcFile;

    ArrayList<String[]> dataList = new ArrayList<String[]>();
    String[] rowData;

    ArrayList<Object[]> rowList = new ArrayList<Object[]>();
    Object[] row;
    for (String fileName : entryMap.keySet()) {
      entry = entryMap.get(fileName);

      ArrayList<Object> list = new ArrayList<Object>(Arrays.asList(entry.getRowData()));
      list.set(9, entry.getPath().getFileName().toString());
      list.add(10, entry.generateNameToSave());

      row = list.toArray(new Object[list.size()]);
      rowList.add(row);

    }

    // データモデルを生成し、テーブルに適用
    DefaultTableModel model;

    Object[][] data = rowList.toArray(new Object[tableHeaders.length][rowList.size()]);
    model = new DefaultTableModel(data, tableHeaders) {

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

    model.setColumnIdentifiers(tableHeaders);
    table.setModel(model);

    for (String columnName : columnsToHide) {
      table.removeColumn(
          table.getColumnModel().getColumn(table.getColumnModel().getColumnIndex(columnName)));
    }

    // "容量"列に右詰めのレンダラを設定
    DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
    rightRenderer.setHorizontalAlignment(JLabel.RIGHT);
    table.getColumnModel().getColumn(table.getColumnModel().getColumnIndex("容量"))
        .setCellRenderer(rightRenderer);

    try {
      HashMap<String, Integer> columnWidthMap = appInfo.getColumnWidthImpt();
      if (Objects.nonNull(columnWidthMap)) {
        table.setColumnWidth(columnWidthMap);
      }
    } catch (Exception e) {

    }

    table.setRowSorter(new TableRowSorter<>((DefaultTableModel) table.getModel()));

    // ソート順を再適用
    table.getRowSorter().setSortKeys(sortKeys);
  }

  /**
   * entryMapから指定のキーを持つエントリを削除する(Worker内部で使用)
   */
  public void refreshMap(ArrayList<Object> processedKeys) {
    for (Object key : processedKeys) {
      entryMap.remove(key);
    }
  }

  /**
   * 選択された行をentryMapに記録されたentryの内容で更新する
   */
  private void refreshSelectedRows() {
    DefaultTableColumnModel columnModel = (DefaultTableColumnModel) table.getColumnModel();
    String fileName;
    Entry entry;
    int viewColumnIndex;
    ArrayList<Object> rowData;
    for (int viewRowIndex : table.getSelectedRows()) {
      viewRowIndex = table.convertRowIndexToModel(viewRowIndex);

      viewColumnIndex = table.convertColumnIndexToModel(columnModel.getColumnIndex("元ファイル名"));
      fileName = (String) table.getModel().getValueAt(viewRowIndex, viewColumnIndex);
      entry = entryMap.get(fileName);

      // 全カラム版ここから
      // tableHeaders = {"ID", "種別", "成", "サークル", "著者", "タイトル", "副題", "巻号", "issue", "元ファイル名",
      // "保存ファイル名", "備考", "頁数", "容量", "パス", "日付", "元ネタ", "発刊"};

      // row = {getId(), getType(), getAdult(), getCircle(), getAuthor(), getTitle(),
      // getSubtitle(), getVolume(), getIssue(), getEntryTitle(), getNote(), getPages(),
      // String.format("%.2f", getSize()), getPath().toString(), dateString, getOriginal(),
      // getRelease()};

      // TODO Entry.getRowData()の戻り値とtableHeadersの並び順を合わせるメソッドを作ればこことupdateTableの両方で使える

      rowData = new ArrayList<Object>(Arrays.asList(entry.getRowData()));
      rowData.set(9, entry.getPath().getFileName().toString());
      rowData.add(10, entry.generateNameToSave());
      for (int index = 0; index < tableHeaders.length; index++) {
        if (Arrays.asList(columnsToHide).contains(tableHeaders[index])) {
          continue;
        }
        viewColumnIndex = columnModel.getColumnIndex(tableHeaders[index]);
        viewColumnIndex = table.convertColumnIndexToModel(viewColumnIndex);
        table.getModel().setValueAt(rowData.get(index), viewRowIndex, viewColumnIndex);
      }
    }
  }

  // AttrDialogを開く。applyされたらentryMapを更新し、更にtableを更新
  private void openAttrDialog() {
    ArrayList<Entry> entryList = new ArrayList<Entry>();
    for (Object currentFileName : table.getSelectedColumnValues("元ファイル名")) {
      entryList.add(entryMap.get((String) currentFileName));
    }
    entryList.removeAll(Collections.singleton(null));
    if (entryList.size() < 1) {
      return;
    }
    AttrDialog attrDialog = new AttrDialog(this, entryList);
    attrDialog.setLocation(appInfo.getRectAttr().getLocation());
    attrDialog.setModal(true);
    attrDialog.setVisible(true);

    // AttrDialogから制御が戻ってきたら、戻り値のentryListの中身をentryMapに反映
    for (Entry entry : entryList) {
      entryMap.put(entry.getPath().toFile().getName(), entry);
    }

    // 選択された行の内容を更新する(updateTable()だと行選択が解除されてしまうので)
    refreshSelectedRows();
  }

  public HashMap<String, Entry> getEntryMap() {
    return entryMap;
  }

  public void setEntryMap(HashMap<String, Entry> entryMap) {
    this.entryMap = entryMap;
  }

  /**
   * テーブルで選択された行に対応するEntryを返す
   *
   * @return ArrayList<Entry>
   */
  private ArrayList<Entry> getSelectedEntries() {
    DefaultTableColumnModel columnModel = (DefaultTableColumnModel) table.getColumnModel();
    String fileName;
    ArrayList<Entry> entryList = new ArrayList<Entry>();
    int tableColumnIdx;
    for (int tableRowIdx : table.getSelectedRows()) {
      tableRowIdx = table.convertRowIndexToModel(tableRowIdx);
      tableColumnIdx = columnModel.getColumnIndex("元ファイル名");
      tableColumnIdx = table.convertColumnIndexToModel(tableColumnIdx);
      fileName = (String) table.getModel().getValueAt(tableRowIdx, tableColumnIdx);
      entryList.add(entryMap.get(fileName));
    }
    return entryList;
  }

  /**
   * 新しいエントリをインポートする
   *
   * @param entryList
   * @param appInfo
   */
  private void importEntry(ArrayList<Entry> entryList, AppInfo appInfo) {
    ArrayList<Entry> imcompletes = new ArrayList<Entry>();
    for (Entry entry : entryList) {
      // entryの情報が揃っているかをチェック
      if (!entry.isReady()) {
        imcompletes.add(entry);
      }
    }
    if (imcompletes.size() > 0) {
      ArrayList<String> fileNameList = new ArrayList<String>();
      for (Entry entry : imcompletes) {
        fileNameList.add(entry.getPath().toFile().getName());
      }
      String[] fileNames = fileNameList.toArray(new String[fileNameList.size()]);
      if (JOptionPane.YES_OPTION != JOptionPane.showConfirmDialog(null,
          "以下のエントリの情報が十分でない可能性があります。処理を続けますか？\n  " + String.join("\n  ", fileNames), "確認",
          JOptionPane.YES_NO_OPTION)) {
        return;
      }
    }

    int min = 0;
    int max = 100;
    progressMonitor = new ProgressMonitor(this, "インポート中...", "開始中...", min, max);
    progressMonitor.setMillisToDecideToPopup(5);
    progressMonitor.setProgress(min);

    Worker importWorker = new Worker(this, progressMonitor, entryList, appInfo);

    importWorker.addPropertyChangeListener(new PropertyChangeListener() {
      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        if ("progress".equals(evt.getPropertyName())) {
          progressMonitor.setProgress((Integer) evt.getNewValue());
        }
      }
    });

    importWorker.execute();

  }

  /**
   * インポート元のディレクトリを切り替える
   *
   */
  private class ChangeDirAction extends AbstractAction {

    @Override
    public void actionPerformed(ActionEvent e) {

      File newImptDir = chooseImptDir();
      if (Objects.nonNull(newImptDir) && newImptDir.exists() && newImptDir.isDirectory()
          || newImptDir.canRead()) {
        imptDir = newImptDir;
        dirPath = imptDir.getAbsolutePath().toString();
        appInfo.setImptDir(dirPath);
        Util.writeAppInfo(appInfo);

        readEntries();
        updateTable();

      }
    }

  }

  private class ImportDialogListner extends WindowAdapter {
    public void windowClosing(WindowEvent e) {
      // タイトルバーのクローズボックスクリックで閉じられてときに呼ばれる
      saveInfo();
    }

    public void windowClosed(WindowEvent e) {
      // dispose()されたときに呼ばれる
      saveInfo();
    }

    private void saveInfo() {
      try {
        appInfo.setRectImpt(getBounds());
        HashMap<String, Integer> columnWidthMap = table.getColumnWidth();
        appInfo.setColumnWidthImpt(columnWidthMap);
        Util.writeAppInfo(appInfo);
      } catch (Exception e) {
        throw e;
      }
    }
  }

  private class TableContextMenu extends JPopupMenu {

    ImportDialog importDialog;
    JMenuItem setAttr;
    JMenuItem moveLastNoteToOriginal;
    JMenuItem addToIgnoreList;
    JMenuItem openWithViewer;
    JMenuItem showFiles;
    JMenuItem importEntries;

    public TableContextMenu() {

      importDialog = (ImportDialog) SwingUtilities.getAncestorOfClass(ImportDialog.class, table);
      setAttr = new JMenuItem("属性を設定...");
      setAttr.addActionListener(new AbstractAction() {

        @Override
        public void actionPerformed(ActionEvent e) {
          openAttrDialog();
        }
      });
      add(setAttr);

      moveLastNoteToOriginal = new JMenuItem("備考の最後の要素を元ネタに移動");
      moveLastNoteToOriginal.addActionListener(new AbstractAction() {

        @Override
        public void actionPerformed(ActionEvent e) {

          DefaultTableColumnModel columnModel = (DefaultTableColumnModel) table.getColumnModel();
          String fileName;
          Entry entry;
          for (int tableRowIdx : table.getSelectedRows()) {
            tableRowIdx = table.convertRowIndexToModel(tableRowIdx);
            fileName = (String) table.getModel().getValueAt(tableRowIdx,
                table.convertColumnIndexToModel(columnModel.getColumnIndex("元ファイル名")));
            entry = entryMap.get(fileName);
            if (Objects.nonNull(entry.getNote())) {
              ArrayList<String> noteList =
                  new ArrayList<String>(Arrays.asList(entry.getNote().split(",")));
              String lastElement = noteList.get(noteList.size() - 1);
              entry.setOriginal(lastElement);
              noteList.remove(lastElement);
              if (noteList.size() > 0) {
                entry.setNote(String.join(",", noteList.toArray(new String[noteList.size()])));
              } else {
                entry.setNote(null);
              }
              entryMap.put(fileName, entry);
            }
          }
          refreshSelectedRows();
        }
      });
      add(moveLastNoteToOriginal);

      addToIgnoreList = new JMenuItem("無視リストに追加");
      addToIgnoreList.addActionListener(new AbstractAction() {

        @Override
        public void actionPerformed(ActionEvent e) {
          String fileName;
          ArrayList<String> namesToBeIgnoredList;
          String[] namesToBeIgnored = appInfo.getNamesToBeIgnored();
          if (Objects.nonNull(namesToBeIgnored)) {
            namesToBeIgnoredList = new ArrayList<String>(Arrays.asList(namesToBeIgnored));
          } else {
            namesToBeIgnoredList = new ArrayList<String>();
          }
          for (Entry entry : getSelectedEntries()) {
            if (Objects.isNull(entry)) {
              continue;
            }
            fileName = entry.getPath().toFile().getName();
            if (Objects.nonNull(fileName) && !fileName.equals("")) {
              namesToBeIgnoredList.add(fileName);
              entryMap.remove(fileName);
            }
          }
          if (namesToBeIgnoredList.size() > 0) {
            namesToBeIgnored =
                namesToBeIgnoredList.toArray(new String[namesToBeIgnoredList.size()]);
          }
          appInfo.setNamesToBeIgnored(namesToBeIgnored);
          Util.writeAppInfo(appInfo);
          updateTable();
        }
      });
      add(addToIgnoreList);

      addSeparator();

      openWithViewer = new JMenuItem("ビューワで開く");
      openWithViewer.addActionListener(new AbstractAction() {

        @Override
        public void actionPerformed(ActionEvent e) {
          Entry entry = getSelectedEntries().get(0);
          Util.openWithViewer(appInfo, entry);
        }
      });
      add(openWithViewer);

      showFiles = new JMenuItem("ファイラで表示");
      showFiles.addActionListener(new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent e) {
          Entry entry = getSelectedEntries().get(0);
          Util.showInFiler(importDialog, entry.getPath().toFile());
        }
      });
      add(showFiles);

      addSeparator();

      importEntries = new JMenuItem("選択されたエントリをインポート");
      importEntries.addActionListener(new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent e) {
          importEntry(getSelectedEntries(), appInfo);
        }
      });
      add(importEntries);
    }

    // 行が選択されているときだけコンテキストメニューを表示する & 選択行が一行のときだけ有効化するメニューの設定
    @Override
    public void show(Component c, int x, int y) {
      if ((((ExtendedTable) c).getSelectedRows().length > 0)) {
        boolean isSelectedSingleRow = (((ExtendedTable) c).getSelectedRows().length == 1);
        openWithViewer.setEnabled(isSelectedSingleRow);
        showFiles.setEnabled(isSelectedSingleRow);
        super.show(c, x, y);
      }
    }

  }


}
