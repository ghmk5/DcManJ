package com.github.ghmk5.dcmanj.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Objects;
import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import com.github.ghmk5.dcmanj.info.AppInfo;
import com.github.ghmk5.dcmanj.info.Entry;
import com.github.ghmk5.dcmanj.util.Util;

public class AttrDialog extends JDialog {

  Window owner;
  ArrayList<Entry> entryList;
  Entry entry;
  AppInfo appInfo;
  String[] types =
      {"", "comic", "comic_s", "doujinshi", "magazine", "novel", "--ununified values--"};
  // Path dirToSave = Path.of("D:\\mag\\store"); // 保存先パス 最終的には環境設定で指定できるようにする

  JComboBox<String> typeComboBox;
  JRadioButton adultTrueRB;
  JRadioButton adultNullRB;
  JRadioButton adultFalseRB;
  JCheckBox adultCheckBox;
  ExtendedField circleField;
  ExtendedField authorField;
  ExtendedField titleField;
  ExtendedField subTitleField;
  ExtendedField volumeField;
  ExtendedField issueField;
  JTextField pagesField;
  JTextField sizeField;
  ExtendedField noteField;
  ExtendedField originalField;
  ExtendedField releaseField;
  JTextField dateField;
  JTextField pathField;
  JLabel generatedFileNameLabel;

  public AttrDialog(Window owner, ArrayList<Entry> entryList) throws IllegalArgumentException {
    super(owner);
    this.owner = owner;
    this.entryList = entryList;

    if (owner instanceof ImportDialog) {
      appInfo = ((ImportDialog) owner).browserWindow.main.appInfo;
    } else if (owner instanceof BrowserWindow) {
      appInfo = ((BrowserWindow) owner).main.appInfo;
    } else {
      throw new IllegalArgumentException("AttrDialogに想定されていない親ウィンドウが指定された");
    }

    createGUI();
    Util.mapESCtoCancel(this);
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

    Box innerBox = Box.createHorizontalBox();
    box.add(innerBox);
    ButtonGroup adultButtonGroup = new ButtonGroup();
    String adultRBToolTip =
        "trueもしくはfalseが選択されている場合はその選択が成人向けフラグにセットされる。nullが選択されているときは成人向けフラグは変更されない";
    adultTrueRB = new JRadioButton("true");
    adultTrueRB.setToolTipText(adultRBToolTip);
    adultButtonGroup.add(adultTrueRB);
    innerBox.add(adultTrueRB);
    adultNullRB = new JRadioButton("null");
    adultNullRB.setToolTipText(adultRBToolTip);
    adultButtonGroup.add(adultNullRB);
    innerBox.add(adultNullRB);
    adultFalseRB = new JRadioButton("false");
    adultFalseRB.setToolTipText(adultRBToolTip);
    adultButtonGroup.add(adultFalseRB);
    innerBox.add(adultFalseRB);
    innerBox.setBorder(new TitledBorder("成人向けフラグ"));

    box = Box.createHorizontalBox();
    panel.add(box);
    circleField = new ExtendedField(18);
    circleField.setBorder(new TitledBorder("サークル"));
    box.add(circleField);
    authorField = new ExtendedField(18);
    authorField.setBorder(new TitledBorder("著者"));
    box.add(authorField);

    box = Box.createHorizontalBox();
    panel.add(box);
    titleField = new ExtendedField(36);
    titleField.setBorder(new TitledBorder("タイトル"));
    box.add(titleField);

    box = Box.createHorizontalBox();
    panel.add(box);
    subTitleField = new ExtendedField(36);
    subTitleField.setBorder(new TitledBorder("サブタイトル"));
    box.add(subTitleField);

    box = Box.createHorizontalBox();
    panel.add(box);
    volumeField = new ExtendedField(9);
    volumeField.setBorder(new TitledBorder("巻号"));
    box.add(volumeField);
    issueField = new ExtendedField(9);
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
    noteField = new ExtendedField(36);
    noteField.setBorder(new TitledBorder("備考"));
    noteField.setComponentPopupMenu(new NoteFieldPopupMenu());
    box.add(noteField);

    box = Box.createHorizontalBox();
    panel.add(box);
    originalField = new ExtendedField(18);
    originalField.setBorder(new TitledBorder("元ネタ"));
    box.add(originalField);
    releaseField = new ExtendedField(18);
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
    applyButton.addActionListener(new ApplyAction());
    panel.add(applyButton);

    addWindowListener(new AttrDialogListner());

    pack();
    setResizable(false);
  }

  // 引数として与えられたリスト内Entryの値をコンテナに反映させる
  private void loadValues(ArrayList<Entry> entryList) {

    ArrayList<String> typeList = new ArrayList<String>();
    ArrayList<Boolean> adultList = new ArrayList<Boolean>();
    ArrayList<String> circleList = new ArrayList<String>();
    ArrayList<String> authorList = new ArrayList<String>();
    ArrayList<String> titleList = new ArrayList<String>();
    ArrayList<String> subTitleList = new ArrayList<String>();
    ArrayList<String> volumeList = new ArrayList<String>();
    ArrayList<String> issueList = new ArrayList<String>();
    ArrayList<Integer> pagesList = new ArrayList<Integer>();
    ArrayList<Double> sizeList = new ArrayList<Double>();
    ArrayList<String> noteList = new ArrayList<String>();
    ArrayList<String> originalList = new ArrayList<String>();
    ArrayList<String> releaseList = new ArrayList<String>();
    ArrayList<String> pathList = new ArrayList<String>();
    ArrayList<String> generatedFileNameList = new ArrayList<String>();

    // 所与のエントリのリストのフィールド値をリストに格納
    for (Entry entry : entryList) {
      // 値未入力コンテナの内容はnullではなく""になるので注意

      // AttrDialogのコンストラクタに引数として与えたArrayList<Entry> の中身のEntryインスタンスがFileを引数にしたコンストラクタで作られている場合
      // (今の所そうなるように作ってある)、ここで使われるEntryインスタンスのpathフィールドには元ファイルのPathインスタンスが入っている

      typeList.add(entry.getType());
      adultList.add(entry.getAdult());
      circleList.add(entry.getCircle());
      authorList.add(entry.getAuthor());
      titleList.add(entry.getTitle());
      subTitleList.add(entry.getSubtitle());
      volumeList.add(entry.getVolume());
      issueList.add(entry.getIssue());
      pagesList.add(entry.getPages());
      sizeList.add(entry.getSize());
      noteList.add(entry.getNote());
      originalList.add(entry.getOriginal());
      releaseList.add(entry.getRelease());
      pathList.add(entry.getPath().getParent().toString());
      generatedFileNameList.add(entry.generateNameToSave());
    }

    // フィールド値のリストの重複を除く
    typeList = new ArrayList<String>(new LinkedHashSet<>(typeList));
    adultList = new ArrayList<Boolean>(new LinkedHashSet<>(adultList));
    circleList = new ArrayList<String>(new LinkedHashSet<>(circleList));
    authorList = new ArrayList<String>(new LinkedHashSet<>(authorList));
    titleList = new ArrayList<String>(new LinkedHashSet<>(titleList));
    subTitleList = new ArrayList<String>(new LinkedHashSet<>(subTitleList));
    volumeList = new ArrayList<String>(new LinkedHashSet<>(volumeList));
    issueList = new ArrayList<String>(new LinkedHashSet<>(issueList));
    pagesList = new ArrayList<Integer>(new LinkedHashSet<>(pagesList));
    sizeList = new ArrayList<Double>(new LinkedHashSet<>(sizeList));
    noteList = new ArrayList<String>(new LinkedHashSet<>(noteList));
    originalList = new ArrayList<String>(new LinkedHashSet<>(originalList));
    releaseList = new ArrayList<String>(new LinkedHashSet<>(releaseList));
    pathList = new ArrayList<String>(new LinkedHashSet<>(pathList));
    generatedFileNameList = new ArrayList<String>(new LinkedHashSet<>(generatedFileNameList));

    // 残った値が一つだけならその値をコンポーネントにセットする 値が複数ならそれ用の値をセットする
    if (typeList.size() == 1) {
      typeComboBox.setSelectedItem(typeList.get(0));
    } else {
      typeComboBox.setSelectedItem("--ununified values--");
    }
    if (adultList.size() == 1) {
      Boolean selected = adultList.get(0);
      if (Objects.isNull(selected)) {
        adultNullRB.setSelected(true);
      } else if (selected) {
        adultTrueRB.setSelected(true);
      } else {
        adultFalseRB.setSelected(true);
      }
    } else {
      adultNullRB.setSelected(true);
    }
    setTFValue(circleList, circleField);
    setTFValue(authorList, authorField);
    setTFValue(titleList, titleField);
    setTFValue(subTitleList, subTitleField);
    setTFValue(volumeList, volumeField);
    setTFValue(issueList, issueField);
    if (pagesList.size() == 1) {
      pagesField.setText(String.valueOf(pagesList.get(0)));
    } else {
      pagesField.setText("--ununified values--");
    }
    if (sizeList.size() == 1) {
      sizeField.setText(String.format("%.2f", sizeList.get(0)));
    } else {
      sizeField.setText("--ununified values--");
    }
    setTFValue(noteList, noteField);
    setTFValue(originalList, originalField);
    setTFValue(releaseList, releaseField);
    setTFValue(pathList, pathField);
    if (generatedFileNameList.size() == 1) {
      generatedFileNameLabel.setText(generatedFileNameList.get(0));
    } else {
      generatedFileNameLabel.setText("--ununified values--");
    }

    // 以下のフィールドは変更不可(allpyしたときもEntryに反映させない)
    pagesField.setEnabled(false);
    sizeField.setEnabled(false);
    pathField.setEnabled(false);


  }

  private void clearFields() {
    typeComboBox.setSelectedIndex(0);
    adultNullRB.setSelected(true);
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

  private void setTFValue(ArrayList<String> valueList, JTextField textField) {
    if (valueList.size() == 1) {
      textField.setText(valueList.get(0));
    } else {
      textField.setText("--ununified values--");
    }
  }

  private class ExtendedField extends JTextField {

    public ExtendedField(int i) {
      super(i);
      addActionListener(new ApplyAction());
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

    public ApplyAction() {};

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
        if (adultTrueRB.isSelected()) {
          entry.setAdult(true);
        } else if (adultFalseRB.isSelected()) {
          entry.setAdult(false);
        }
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
        attrDialog = (AttrDialog) SwingUtilities.getAncestorOfClass(AttrDialog.class,
            (Component) e.getSource());
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
        Util.writeAppInfo(appInfo);
      } catch (Exception e) {
        throw e;
      }
    }
  }

}
