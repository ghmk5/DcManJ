package com.github.ghmk5.dcmanj.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import com.github.ghmk5.dcmanj.info.Entry;
import com.github.ghmk5.dcmanj.util.Util;

public class ImportDialog extends JDialog {
  BrowserWindow browserWindow;
  ExtendedTable table;
  String dirPath;
  HashMap<String, Entry> entryMap;

  public ImportDialog(BrowserWindow browserWindow, String dirPath) throws IllegalArgumentException {
    this.browserWindow = browserWindow;
    this.dirPath = dirPath;

    setTitle("新規エントリ追加");
    setLayout(new BorderLayout());
    JPanel panel = new JPanel(new FlowLayout());
    getContentPane().add(panel, BorderLayout.NORTH);
    JButton reloadButton = new JButton("Reload");
    reloadButton.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        readEntries();

      }
    });
    panel.add(reloadButton);
    JButton changeDirButton = new JButton("Change Dir");
    changeDirButton.addActionListener(new ChangeDirAction(this));
    panel.add(changeDirButton);
    ButtonGroup storeFormBG = new ButtonGroup();
    JRadioButton zipRB = new JRadioButton("zipして保管");
    panel.add(zipRB);
    storeFormBG.add(zipRB);
    JRadioButton bareRB = new JRadioButton("フォルダのまま保管");
    panel.add(bareRB);
    storeFormBG.add(bareRB);
    table = new ExtendedTable();
    table.addMouseListener(new MouseAdapter() {
      public void mouseClicked(MouseEvent me) {
        if (me.getClickCount() == 2) {
          // TODO Entry設定ダイアログを開く処理--コンテキストメニューから呼んだときと共通なので、メソッド化する
        }
      }
    });

    JScrollPane scrollPane = new JScrollPane(table);
    getContentPane().add(scrollPane, BorderLayout.CENTER);
    panel = new JPanel();
    getContentPane().add(panel, BorderLayout.SOUTH);
    panel.setLayout(new FlowLayout(FlowLayout.TRAILING));
    JButton cancelButton = new JButton("Cancel");
    cancelButton.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        dispose();

      }
    });
    panel.add(cancelButton);
    JButton applyButton = new JButton("Apply");
    applyButton.addActionListener(new AbstractAction() {

      @Override
      public void actionPerformed(ActionEvent e) {
        ImportDialog dialog = (ImportDialog) SwingUtilities.getAncestorOfClass(ImportDialog.class,
            (JButton) e.getSource());
        dialog.dispose();

      }
    });
    panel.add(applyButton);

    addWindowListener(new ImportDialogListner());

    readEntries();

    // pack();

  }

  private void readEntries() {
    if (Objects.isNull(dirPath)) {
      setTitle(null);
      return;
    }
    setTitle(dirPath + " から新しいエントリをインポート");
    File impDir = new File(dirPath);
    if (!impDir.isDirectory()) {
      String errString = "ImportDialogクラスのコンストラクタに非ディレクトリなPathが渡された";
      JOptionPane.showMessageDialog(browserWindow, errString, "エラー", JOptionPane.ERROR_MESSAGE);
      throw new IllegalArgumentException(errString);
    }
    entryMap = new HashMap<String, Entry>();
    ArrayList<String[]> dataList = new ArrayList<String[]>();
    String[] outAry;
    for (File entry : impDir.listFiles()) {
      outAry = new String[3];
      if (entry.isFile()) {
        outAry[0] = "file";
      } else if (entry.isDirectory()) {
        outAry[0] = "directory";
      } else {
        outAry[0] = "unknown (simlink?)";
      }
      outAry[1] = entry.getName();
      entryMap.put(outAry[1], new Entry(outAry[1]));
      outAry[2] = "";
      dataList.add(outAry);
    }

    String[] columnNames = {"type", "Current Name", "Name to Store"};
    String[][] data = dataList.toArray(new String[3][dataList.size()]);
    DefaultTableModel model = new DefaultTableModel(data, columnNames);
    table.setModel(model);
    try {
      HashMap<String, Integer> columnWidthMap = browserWindow.main.appInfo.getColumnWidthImpt();
      if (Objects.nonNull(columnWidthMap)) {
        table.setColumnWidth(columnWidthMap);
      }
    } catch (Exception e) {

    }
    table.setRowSorter(new TableRowSorter<>((DefaultTableModel) table.getModel()));
  }

  private class ChangeDirAction extends AbstractAction {

    ImportDialog importDialog;

    public ChangeDirAction(ImportDialog importDialog) {
      this.importDialog = importDialog;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      JFileChooser fileChooser = new JFileChooser();
      fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
      fileChooser.setApproveButtonText("選択");
      int selected = fileChooser.showOpenDialog(importDialog);
      if (selected == JFileChooser.APPROVE_OPTION) {
        importDialog.dirPath = fileChooser.getSelectedFile().toString();
        importDialog.browserWindow.main.appInfo.setImptDir(dirPath);
        Util.writeBean(importDialog.browserWindow.main.prefFile, dirPath);
        readEntries();
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
        browserWindow.main.appInfo.setRectImpt(getBounds());
        HashMap<String, Integer> columnWidthMap = table.getColumnWidth();
        browserWindow.main.appInfo.setColumnWidthImpt(columnWidthMap);
        Util.writeBean(browserWindow.main.prefFile, browserWindow.main.appInfo);
      } catch (Exception e) {

      }
    }
  }

}
