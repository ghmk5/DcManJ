package com.github.ghmk5.dcmanj.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;
import com.github.ghmk5.dcmanj.info.AppInfo;
import com.github.ghmk5.dcmanj.info.Entry;
import com.github.ghmk5.dcmanj.util.Util;

public class AttrDialog extends JDialog {

  Window owner;
  ArrayList<Entry> entryList;
  Entry entry;
  AppInfo appInfo;
  File prefFile;
  String[] types = {"", "comic", "comic_s", "doujinshi", "magazine", "novel"};
  // Path dirToSave = Path.of("D:\\mag\\store"); // 保存先パス 最終的には環境設定で指定できるようにする

  JComboBox<String> typeComboBox;
  JCheckBox adultCheckBox;
  JTextField circleField;
  JTextField authorField;
  JTextField titleField;
  JTextField subTitleField;
  JTextField volumeField;
  JTextField issueField;
  JTextField pagesField;
  JTextField sizeField;
  JTextField noteField;
  JTextField originalField;
  JTextField releaseField;
  JTextField dateField;
  JTextField pathField;
  JLabel generatedFileNameLabel;

  public AttrDialog(Window owner, ArrayList<Entry> entryList) throws IllegalArgumentException {
    super(owner);
    this.owner = owner;
    this.entryList = entryList;

    if (owner instanceof ImportDialog) {
      appInfo = ((ImportDialog) owner).browserWindow.main.appInfo;
      prefFile = ((ImportDialog) owner).browserWindow.main.prefFile;
    } else if (owner instanceof BrowserWindow) {
      appInfo = ((BrowserWindow) owner).main.appInfo;
      prefFile = ((BrowserWindow) owner).main.prefFile;
    } else {
      throw new IllegalArgumentException("AttrDialogに想定されていない親ウィンドウが指定された");
    }

    createGUI();
    loadValues(entryList);
  }

  private void createGUI() {
    setTitle("属性値設定");
    getContentPane().setLayout(new BorderLayout());
    JPanel panel = new JPanel();
    getContentPane().add(panel, BorderLayout.CENTER);
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

    Box box = Box.createHorizontalBox();
    panel.add(box);
    typeComboBox = new JComboBox<String>(types);
    typeComboBox.setBorder(new TitledBorder("種別"));
    typeComboBox.setMaximumSize(new Dimension(128, 52));
    box.add(typeComboBox);
    adultCheckBox = new JCheckBox("成人向");
    box.add(adultCheckBox);

    box = Box.createHorizontalBox();
    panel.add(box);
    circleField = new JTextField(18);
    circleField.setBorder(new TitledBorder("サークル"));
    box.add(circleField);
    authorField = new JTextField(18);
    authorField.setBorder(new TitledBorder("著者"));
    box.add(authorField);

    box = Box.createHorizontalBox();
    panel.add(box);
    titleField = new JTextField(36);
    titleField.setBorder(new TitledBorder("タイトル"));
    box.add(titleField);

    box = Box.createHorizontalBox();
    panel.add(box);
    subTitleField = new JTextField(36);
    subTitleField.setBorder(new TitledBorder("サブタイトル"));
    box.add(subTitleField);

    box = Box.createHorizontalBox();
    panel.add(box);
    volumeField = new JTextField(9);
    volumeField.setBorder(new TitledBorder("巻号"));
    box.add(volumeField);
    issueField = new JTextField(9);
    issueField.setBorder(new TitledBorder("issue"));
    box.add(issueField);
    pagesField = new JTextField(9);
    pagesField.setBorder(new TitledBorder("頁数"));
    box.add(pagesField);
    sizeField = new JTextField(9);
    sizeField.setBorder(new TitledBorder("サイズ(MB)"));
    box.add(sizeField);

    box = Box.createHorizontalBox();
    panel.add(box);
    noteField = new JTextField(36);
    noteField.setBorder(new TitledBorder("備考"));
    noteField.setComponentPopupMenu(new NoteFieldPopupMenu());
    box.add(noteField);

    box = Box.createHorizontalBox();
    panel.add(box);
    originalField = new JTextField(18);
    originalField.setBorder(new TitledBorder("元ネタ"));
    box.add(originalField);
    releaseField = new JTextField(18);
    releaseField.setBorder(new TitledBorder("配布イベント"));
    box.add(releaseField);

    box = Box.createHorizontalBox();
    panel.add(box);
    dateField = new JTextField(18);
    dateField.setBorder(new TitledBorder("日付"));
    box.add(dateField);
    pathField = new JTextField(18);
    pathField.setBorder(new TitledBorder("保存パス"));
    box.add(pathField);

    box = Box.createHorizontalBox();
    panel.add(box);
    generatedFileNameLabel = new JLabel(" ");
    generatedFileNameLabel.setMaximumSize(new Dimension(Short.MAX_VALUE, Short.MAX_VALUE));
    generatedFileNameLabel.setBorder(new TitledBorder("保存ファイル/ディレクトリ名"));
    box.add(generatedFileNameLabel);



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
    applyButton.addActionListener(new ApplyAction(this));
    panel.add(applyButton);

    addWindowListener(new AttrDialogListner());

    pack();
    setResizable(false);
  }

  private void loadValues(ArrayList<Entry> entryList) {
    for (Entry entry : entryList) {
      // 引数として与えられたリスト内Entryの値をコンテナに反映させる 値未入力コンテナの内容はnullではなく""になるので注意

      // AttrDialogのコンストラクタに引数として与えたArrayList<Entry> の中身のEntryインスタンスがFileを引数にしたコンストラクタで作られている場合
      // (今の所そうなるように作ってある)、ここで使われるEntryインスタンスのpathフィールドには元ファイルのPathインスタンスが入っている

      if (typeComboBox.getSelectedItem().equals("") && Objects.nonNull(entry.getType())) {
        typeComboBox.setSelectedItem(entry.getType());
      }
      setTFValue(circleField, entry.getCircle());
      setTFValue(authorField, entry.getAuthor());
      setTFValue(titleField, entry.getTitle());
      setTFValue(subTitleField, entry.getSubtitle());
      setTFValue(volumeField, entry.getVolume());
      setTFValue(issueField, entry.getIssue());
      setTFValue(pagesField, String.valueOf(entry.getPages()));
      setTFValue(sizeField, String.format("%.2f", entry.getSize()));
      setTFValue(noteField, entry.getNote());
      setTFValue(originalField, entry.getOriginal());
      setTFValue(releaseField, entry.getRelease());
      setTFValue(pathField, entry.getPath().getParent().toString());

      pagesField.setEditable(false);
      sizeField.setEditable(false);
      pathField.setEditable(false);

    }
  }

  private void clearFields() {
    typeComboBox.setSelectedIndex(0);
    adultCheckBox.setSelected(false);
    circleField.setText("");
    authorField.setText("");
    titleField.setText("");
    subTitleField.setText("");
    volumeField.setText("");
    issueField.setText("");
    pagesField.setText("");
    sizeField.setText("");
    noteField.setText("");
    originalField.setText("");
    releaseField.setText("");
    pathField.setText("");
  }

  private void setTFValue(JTextField textField, String string) {
    if (Objects.nonNull(string)) {
      if (textField.getText().equals("")) {
        textField.setText(string);
      } else if (!textField.getText().equals(string)) {
        textField.setText("--ununified values--");
      }
    }
  }

  private class NoteFieldPopupMenu extends JPopupMenu {
    public NoteFieldPopupMenu() {

      JMenuItem moveLastNoteToOriginal = new JMenuItem("最後の要素を元ネタに移動");
      moveLastNoteToOriginal.addActionListener(new AbstractAction() {

        @Override
        public void actionPerformed(ActionEvent e) {
          ArrayList<String> noteList =
              new ArrayList<String>(Arrays.asList(noteField.getText().split(",")));
          String lastElement = noteList.get(noteList.size() - 1);
          noteList.remove(lastElement);
          String noteString = String.join(",", noteList.toArray(new String[noteList.size()]));
          originalField.setText(lastElement);
          noteField.setText(noteString);

        }
      });

      add(moveLastNoteToOriginal);
    }
  }

  private class ApplyAction extends AbstractAction {
    AttrDialog attrDialog;

    public ApplyAction(AttrDialog attrDialog) {
      this.attrDialog = attrDialog;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      String string;
      for (Entry entry : entryList) {
        string = (String) typeComboBox.getSelectedItem();
        if ((string.equals(""))) {
          entry.setType(null);
        } else if (!string.equals("--ununified values--")) {
          entry.setType(string);
        }
        entry.setAdult(adultCheckBox.isSelected());
        string = circleField.getText();
        if (string.equals("")) {
          entry.setCircle(null);
        } else if (!string.equals("--ununified values--")) {
          entry.setCircle(string);
        }
        string = authorField.getText();
        if (string.equals("")) {
          entry.setAuthor(null);
        } else if (!string.equals("--ununified values--")) {
          entry.setAuthor(string);
        }
        string = titleField.getText();
        if (string.equals("")) {
          entry.setTitle(null);
        } else if (!string.equals("--ununified values--")) {
          entry.setTitle(string);
        }
        string = subTitleField.getText();
        if (string.equals("")) {
          entry.setSubtitle(null);
        } else if (!string.equals("--ununified values--")) {
          entry.setSubtitle(string);
        }
        string = volumeField.getText();
        if (string.equals("")) {
          entry.setVolume(null);
        } else if (!string.equals("--ununified values--")) {
          entry.setVolume(string);
        }
        string = issueField.getText();
        if (string.equals("")) {
          entry.setIssue(null);
        } else if (!string.equals("--ununified values--")) {
          entry.setIssue(string);
        }
        string = noteField.getText();
        if (string.equals("")) {
          entry.setNote(null);
        } else if (!string.equals("--ununified values--")) {
          entry.setNote(string);
        }
        string = originalField.getText();
        if (string.equals("")) {
          entry.setOriginal(null);
        } else if (!string.equals("--ununified values--")) {
          entry.setOriginal(string);
        }
        string = releaseField.getText();
        if (string.equals("")) {
          entry.setRelease(null);
        } else if (!string.equals("--ununified values--")) {
          entry.setRelease(string);
        }
        attrDialog.dispose();
      }
    }
  }

  private class AttrDialogListner extends WindowAdapter {
    public void windowClosing(WindowEvent e) {
      // タイトルバーのクローズボックスクリックで閉じられたときに呼ばれる
      saveInfo((Window) e.getSource());
    }

    public void windowClosed(WindowEvent e) {
      // dispose()されたときに呼ばれる
      saveInfo((Window) e.getSource());
    }

    private void saveInfo(Window window) {
      try {
        appInfo.setRectAttr(window.getBounds());
        Util.writeBean(prefFile, appInfo);
      } catch (Exception e) {
        throw e;
      }
    }
  }

}
