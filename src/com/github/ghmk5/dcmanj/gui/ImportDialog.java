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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;
import java.util.zip.ZipException;
import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
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

  public ImportDialog(BrowserWindow browserWindow)
      throws IllegalArgumentException, ZipException, IOException {
    this.browserWindow = browserWindow;
    this.dirPath = browserWindow.main.appInfo.getImptDir();

    setTitle("新規エントリ追加");
    setLayout(new BorderLayout());
    JPanel panel = new JPanel(new FlowLayout());
    getContentPane().add(panel, BorderLayout.NORTH);
    JButton reloadButton = new JButton("Reload");
    reloadButton.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        try {
          readEntries();
        } catch (ZipException e1) {
          // TODO 自動生成された catch ブロック
          e1.printStackTrace();
        } catch (IOException e1) {
          // TODO 自動生成された catch ブロック
          e1.printStackTrace();
        }

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

  private void readEntries() throws ZipException, IOException {
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
    Entry entry;
    for (File file : impDir.listFiles()) {
      outAry = new String[3];
      if (file.isFile()) {
        outAry[0] = "file";
      } else if (file.isDirectory()) {
        outAry[0] = "directory";
      } else {
        outAry[0] = "unknown (simlink?)";
      }
      outAry[1] = file.getName();
      entry = new Entry(file);
      entryMap.put(outAry[1], entry);
      outAry[2] = entry.generateNameToSave();
      dataList.add(outAry);
    }

    String[] columnNames = {"type", "Current Name", "Name to Store"};
    String[][] data = dataList.toArray(new String[3][dataList.size()]);
    DefaultTableModel model = new DefaultTableModel(data, columnNames);
    model.setColumnIdentifiers(columnNames);
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

  private void openAttrDialog() {
    ArrayList<Entry> entryList = new ArrayList<Entry>();
    for (Object currentFileName : table.getSelectedColumnValues("Current Name")) {
      entryList.add(entryMap.get((String) currentFileName));
    }
    AttrDialog attrDialog = new AttrDialog(this, entryList);
    Util.setRect(attrDialog, browserWindow.main.appInfo.getRectAttr());
    attrDialog.setModal(true);
    attrDialog.setVisible(true);
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
        try {
          readEntries();
        } catch (IOException e1) {
          // TODO 自動生成された catch ブロック
          e1.printStackTrace();
        }
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
        throw e;
      }
    }
  }

  private class TableContextMenu extends JPopupMenu {

    public TableContextMenu() {

      JMenuItem setAttr = new JMenuItem("属性を設定...");
      setAttr.addActionListener(new AbstractAction() {

        @Override
        public void actionPerformed(ActionEvent e) {
          openAttrDialog();
        }
      });

      JMenuItem moveLastNoteToOriginal = new JMenuItem("備考の最後の要素を元ネタに移動");
      moveLastNoteToOriginal.addActionListener(new AbstractAction() {

        @Override
        public void actionPerformed(ActionEvent e) {
          for (Object fileName : table.getSelectedColumnValues("Current Name")) {
            Entry entry = entryMap.get((String) fileName);
            if (Objects.nonNull(entry.getNote())) {
              ArrayList<String> noteList =
                  new ArrayList<String>(Arrays.asList(entry.getNote().split(",")));
              String lastElement = noteList.get(noteList.size() - 1);
              entry.setOriginal(lastElement);
              noteList.remove(lastElement);
              entry.setNote(String.join(",", noteList.toArray(new String[noteList.size()])));
            }
          }

        }
      });

      add(setAttr);
      add(moveLastNoteToOriginal);
    }
  }


}
