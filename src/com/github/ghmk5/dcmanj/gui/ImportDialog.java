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
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.zip.ZipException;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.ProgressMonitor;
import javax.swing.RowSorter.SortKey;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;
import org.apache.commons.io.FileUtils;
import com.github.ghmk5.dcmanj.info.AppInfo;
import com.github.ghmk5.dcmanj.info.Entry;
import com.github.ghmk5.dcmanj.util.MagZip;
import com.github.ghmk5.dcmanj.util.Util;

public class ImportDialog extends JDialog {
  BrowserWindow browserWindow;
  AppInfo appInfo;
  ExtendedTable table;
  String dirPath;
  File imptDir;
  HashMap<String, Entry> entryMap;
  ProgressMonitor progressMonitor;

  public ImportDialog(BrowserWindow browserWindow)
      throws IllegalArgumentException, ZipException, IOException {
    this.browserWindow = browserWindow;
    this.appInfo = browserWindow.main.appInfo;
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

    setTitle(dirPath + " から新規エントリを読み込み");
    setLayout(new BorderLayout());
    JPanel panel = new JPanel(new FlowLayout());
    getContentPane().add(panel, BorderLayout.NORTH);
    JButton reloadButton = new JButton("Reload");
    reloadButton.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        readEntries();
        updateTable();
      }
    });
    panel.add(reloadButton);
    JButton changeDirButton = new JButton("Change Dir");
    changeDirButton.addActionListener(new ChangeDirAction());
    panel.add(changeDirButton);

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
        String filename = (String) getValueAt(row, 1);
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
    table.getTableHeader().setFont(browserWindow.main.tableFont);
    table.setFont(browserWindow.main.tableFont);
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
    for (File srcFile : imptDir.listFiles()) {
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
  private void updateTable() {
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
    for (String fileName : entryMap.keySet()) {
      entry = entryMap.get(fileName);
      srcFile = entry.getPath().toFile();
      rowData = new String[4];
      if (srcFile.isFile()) {
        rowData[0] = "file";
      } else if (srcFile.isDirectory()) {
        rowData[0] = "directory";
      } else {
        rowData[0] = "unknown (simlink?)";
      }
      rowData[1] = srcFile.getName();
      rowData[2] = entry.getOriginal();
      rowData[3] = entry.generateNameToSave();
      dataList.add(rowData);
    }

    // データモデルを生成し、テーブルに適用
    String[] columnNames = {"type", "Current Name", "original", "Name to Store"};
    String[][] data = dataList.toArray(new String[4][dataList.size()]);
    DefaultTableModel model = new DefaultTableModel(data, columnNames);
    model.setColumnIdentifiers(columnNames);
    table.setModel(model);
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
   * 選択された行の保存名を更新する
   */
  private void refreshSelectedRows() {
    DefaultTableColumnModel columnModel = (DefaultTableColumnModel) table.getColumnModel();
    String fileName;
    Entry entry;
    for (int tableRowIdx : table.getSelectedRows()) {
      tableRowIdx = table.convertRowIndexToModel(tableRowIdx);
      fileName = (String) table.getModel().getValueAt(tableRowIdx,
          columnModel.getColumnIndex("Current Name"));
      entry = entryMap.get(fileName);
      table.getModel().setValueAt(entry.getOriginal(), tableRowIdx,
          columnModel.getColumnIndex("original"));
      table.getModel().setValueAt(entry.generateNameToSave(), tableRowIdx,
          columnModel.getColumnIndex("Name to Store"));
    }
  }

  // AttrDialogを開く。applyされたらentryMapを更新し、更にtableを更新
  private void openAttrDialog() {
    ArrayList<Entry> entryList = new ArrayList<Entry>();
    for (Object currentFileName : table.getSelectedColumnValues("Current Name")) {
      entryList.add(entryMap.get((String) currentFileName));
    }
    AttrDialog attrDialog = new AttrDialog(this, entryList);
    Util.setRect(attrDialog, appInfo.getRectAttr());
    attrDialog.setModal(true);
    attrDialog.setVisible(true);

    // AttrDialogから制御が戻ってきたら、戻り値のentryListの中身をentryMapに反映
    for (Entry entry : entryList) {
      entryMap.put(entry.getPath().toFile().getName(), entry);
    }

    // 選択された行の内容を更新する(updateTable()だと行選択が解除されてしまうので)
    refreshSelectedRows();
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
    for (int tableRowIdx : table.getSelectedRows()) {
      tableRowIdx = table.convertRowIndexToModel(tableRowIdx);
      fileName = (String) table.getModel().getValueAt(tableRowIdx,
          columnModel.getColumnIndex("Current Name"));
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

    SwingWorker<Object, Object[]> importWorker = new ImportWorker(entryList, appInfo);

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
   * 所与のエントリの情報を新規レコードとしてデータベースに挿入する
   *
   * @param entry
   * @throws SQLException
   */
  public static void putNewRecord(Entry entry, File dbFile) throws SQLException {

    Object[] values = {entry.getType(), entry.getAdult(), entry.getCircle(), entry.getAuthor(),
        entry.getTitle(), entry.getSubtitle(), entry.getVolume(), entry.getIssue(), entry.getNote(),
        entry.getPages(), entry.getSize(), entry.getPath().toString(),
        entry.getDate().format(Util.DTF), entry.getOriginal(), entry.getRelease()};
    String[] valueStrings = new String[values.length];
    for (int i = 0; i < values.length; i++) {
      if (values[i] instanceof String) {
        valueStrings[i] = "\'" + (String) values[i] + "\'";
      } else if (values[i] instanceof Boolean) {
        valueStrings[i] = "\'" + String.valueOf(values[i]) + "\'";
      } else {
        valueStrings[i] = String.valueOf(values[i]);
      }
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

  private class ImportWorker extends SwingWorker<Object, Object[]> {

    AppInfo appInfo;
    ArrayList<Entry> entryList;
    int max = 100;
    int progressPart;
    int totalProgress = 0;
    int partMax = 0;
    int processed = 0;
    MagZip zipper;

    public ImportWorker(ArrayList<Entry> entryList, AppInfo appInfo) {
      super();
      this.entryList = entryList;
      this.appInfo = appInfo;
      progressPart = max / entryList.size();
    }

    void setFProgress(float f) {
      setProgress(totalProgress + (int) ((float) progressPart * f));
    }

    @Override
    protected Object doInBackground() throws IOException, SQLException {
      for (Entry entry : entryList) {

        if (progressMonitor.isCanceled()) {
          progressMonitor.close();
          break;
        }

        processed++;
        partMax = totalProgress + progressPart;

        File srcFile = entry.getPath().toFile();

        publish(new Object[] {entry.getPath().toFile().getName(), processed, totalProgress});

        // entryをzipして保管するオプションが選択されている && 元がディレクトリの場合は一時ファイルにzipし、移動元を一時ファイルに切り替える
        if (appInfo.getZipToStore() && entry.getPath().toFile().isDirectory()) {
          // 一時ファイルとしてzip
          zipper = new MagZip();
          zipper.addPropertyChangeListener(new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
              if ("progress".equals(evt.getPropertyName())) {
                if (evt.getNewValue() instanceof Float) {
                  setFProgress((float) evt.getNewValue());
                }
              }
            }
          });
          srcFile = zipper.zipToTmp(srcFile, "DcManJTMP", ".zip");
        } else if (!srcFile.isFile()) {
          throw new IllegalArgumentException("Entry.pathの示す内容がファイルでもディレクトリでもない(シンボリックリンク?)");
        }

        // 保存先ディレクトリを設定
        File saveDir = Util.prepSaveDir(appInfo, srcFile);

        // 保存先のFileインスタンスを生成
        File newFile = new File(saveDir, entry.generateNameToSave() + ".zip");

        // 同名のエントリが既に存在する場合、後置付随詞群に"再"を付け加えて再生成
        if (newFile.exists()) {
          String note = entry.getNote();
          if (Objects.nonNull(note)) {
            ArrayList<String> noteAsList =
                new ArrayList<String>(Arrays.asList(entry.getNote().split(",")));
            noteAsList.add("再");
            note = String.join(",", noteAsList.toArray(new String[noteAsList.size()]));
          } else {
            note = "再";
          }
          entry.setNote(note);
          newFile = new File(saveDir, entry.generateNameToSave() + ".zip");
        }

        // 移動実行
        if (srcFile.isFile()) {
          FileUtils.moveFile(srcFile, newFile);
        } else {
          FileUtils.moveDirectory(srcFile, newFile);
        }

        // 元がディレクトリでzipして保存した場合は元のディレクトリを削除
        if (appInfo.getZipToStore() && entry.getPath().toFile().isDirectory()) {
          FileUtils.deleteDirectory(entry.getPath().toFile());
        }

        // entryMapからインポート済みのエントリを除去
        entryMap.remove(entry.getPath().toFile().getName());

        // Entryのサイズとパスを書き換え
        Double newSize;
        if (newFile.isFile()) {
          newSize = newFile.length() / 1024d / 1024d;
        } else {
          newSize = FileUtils.sizeOfDirectory(newFile) / 1024d / 1024d;
        }
        entry.setSize(newSize);
        entry.setPath(newFile.toPath());
        entry.setDate(OffsetDateTime.now(ZoneId.systemDefault()));

        // Entryの内容をデータベースに挿入 updateの場合は別途処理を作る
        putNewRecord(entry, browserWindow.main.dbFile);
        setProgress(partMax);
        totalProgress = partMax;

        publish(new Object[] {entry.getPath().toFile().getName(), processed, totalProgress});

      }
      return null;
    }

    @Override
    protected void process(List<Object[]> chunks) {
      String filename;
      int processed;
      for (Object[] chunk : chunks) {
        filename = (String) chunk[0];
        processed = (int) chunk[1];
        progressMonitor.setNote("(" + String.valueOf(processed) + "/"
            + String.valueOf(entryList.size()) + "): " + filename);
      }
    }

    @Override
    protected void done() {
      updateTable();
      setProgress(max);
    }
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
                columnModel.getColumnIndex("Current Name"));
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
