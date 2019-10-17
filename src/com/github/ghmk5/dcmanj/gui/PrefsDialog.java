package com.github.ghmk5.dcmanj.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;
import com.github.ghmk5.dcmanj.info.AppInfo;
import com.github.ghmk5.dcmanj.util.Util;

public class PrefsDialog extends JDialog {

  BrowserWindow owner;
  AppInfo appInfo;

  JTextField saveDirField;
  JButton selectSaveDirButton;
  JCheckBox useChildDirChkBx;
  JTextField childDirPrefixField;
  JRadioButton splitBySizeRB;
  JTextField splitSizeField;
  JRadioButton splitByNumberRB;
  JTextField splitByNumberField;
  ButtonGroup splitStyleBG;
  JTextField viewerPathField;
  JButton selectViewerExecutableButton;
  JTextField evRegExField;
  JTextField noteRegExField;
  JButton cancelButton;
  JButton applyButton;

  public static void main(String[] args) {
    PrefsDialog prefsDialog = new PrefsDialog();
    prefsDialog.setVisible(true);
  }

  public PrefsDialog() {
    super();
    initialize();
  }

  public PrefsDialog(BrowserWindow owner) {
    super(owner);
    this.owner = owner;
    this.appInfo = this.owner.main.appInfo;
    Util.mapESCtoCancel(this);
    initialize();
    loadValues();
  }

  private void initialize() {
    setTitle("初期設定");
    addWindowListener(new PrefsDialogListner(owner));
    getContentPane().setLayout(new BorderLayout());
    JPanel panel = new JPanel();
    getContentPane().add(panel, BorderLayout.CENTER);
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

    Box box = Box.createHorizontalBox();
    panel.add(box);
    saveDirField = new JTextField(24);
    saveDirField.setEditable(false);
    box.add(saveDirField);
    selectSaveDirButton = new JButton("Select...");
    selectSaveDirButton.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setApproveButtonText("選択");
        int selected = fileChooser.showOpenDialog((Component) e.getSource());
        if (selected == JFileChooser.APPROVE_OPTION) {
          saveDirField.setText(fileChooser.getSelectedFile().toString());
        }
      }
    });
    box.add(selectSaveDirButton);
    box.setBorder(new TitledBorder("保存先親ディレクトリ"));

    box = Box.createVerticalBox();
    panel.add(box);
    Box childBox = Box.createHorizontalBox();
    box.add(childBox);
    useChildDirChkBx = new JCheckBox("子ディレクトリを使用する");
    useChildDirChkBx.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        if (((JCheckBox) e.getSource()).isSelected()) {
          childDirPrefixField.setEditable(true);
        } else {
          childDirPrefixField.setEditable(false);
        }

      }
    });
    childBox.add(useChildDirChkBx);
    childDirPrefixField = new JTextField(12);
    childDirPrefixField.setEditable(false);
    childBox.add(new JLabel("        接頭辞: "));
    childBox.add(childDirPrefixField);
    childBox = Box.createHorizontalBox();
    box.add(childBox);
    splitBySizeRB = new JRadioButton("容量で分ける (");
    splitBySizeRB.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        JRadioButton radioButton = (JRadioButton) e.getSource();
        splitSizeField.setEditable(radioButton.isSelected());
        splitByNumberField.setEditable(!radioButton.isSelected());
      }
    });
    childBox.add(splitBySizeRB);
    childBox.add(new JLabel("容量(MB): "));
    splitSizeField = new JTextField(4);
    childBox.add(splitSizeField);
    childBox.add(new JLabel(")  "));
    splitByNumberRB = new JRadioButton("数で分ける (");
    splitByNumberRB.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        JRadioButton radioButton = (JRadioButton) e.getSource();
        splitSizeField.setEditable(!radioButton.isSelected());
        splitByNumberField.setEditable(radioButton.isSelected());
      }
    });

    childBox.add(splitByNumberRB);
    childBox.add(new JLabel("分割数: "));
    splitByNumberField = new JTextField(4);
    childBox.add(splitByNumberField);
    childBox.add(new JLabel(")"));
    splitStyleBG = new ButtonGroup();
    splitStyleBG.add(splitBySizeRB);
    splitStyleBG.add(splitByNumberRB);
    box.setBorder(new TitledBorder("保存先子ディレクトリ"));

    box = Box.createHorizontalBox();
    panel.add(box);
    viewerPathField = new JTextField(24);
    viewerPathField.setEditable(false);
    box.add(viewerPathField);
    selectViewerExecutableButton = new JButton("Select...");
    selectViewerExecutableButton.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setApproveButtonText("選択");
        int selected = fileChooser.showOpenDialog((Component) e.getSource());
        if (selected == JFileChooser.APPROVE_OPTION) {
          viewerPathField.setText(fileChooser.getSelectedFile().toString());
        }
      }
    });
    box.add(selectViewerExecutableButton);
    box.setBorder(new TitledBorder("画像ビューワ実行ファイルのパス"));

    box = Box.createHorizontalBox();
    evRegExField = new JTextField(24);
    evRegExField.setEditable(false);
    evRegExField.setBorder(new TitledBorder("イベントタグの正規表現リスト"));
    panel.add(evRegExField);

    noteRegExField = new JTextField(24);
    noteRegExField.setEditable(false);
    noteRegExField.setBorder(new TitledBorder("備考項目の正規表現リスト"));
    panel.add(noteRegExField);

    panel = new JPanel();
    getContentPane().add(panel, BorderLayout.SOUTH);
    panel.setLayout(new FlowLayout(FlowLayout.TRAILING));
    cancelButton = new JButton("Cancel");
    cancelButton.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        dispose();
      }
    });
    panel.add(cancelButton);
    applyButton = new JButton("Apply");
    applyButton.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        setValues();
        dispose();
      }
    });
    panel.add(applyButton);

    pack();
  }

  private void loadValues() {
    saveDirField.setText(appInfo.getSaveDir());
    useChildDirChkBx.setSelected(appInfo.getUseChildDir());
    childDirPrefixField.setEditable(useChildDirChkBx.isSelected());
    childDirPrefixField.setText(appInfo.getChildDirPrefix());
    splitBySizeRB.setSelected(appInfo.getSplitChildDirBySize());
    splitSizeField.setEditable(appInfo.getSplitChildDirBySize());
    splitSizeField.setText(String.valueOf(appInfo.getChildDirSplitSize()));
    splitByNumberRB.setSelected(appInfo.getSplitChildDirByNumber());
    splitByNumberField.setEditable(appInfo.getSplitChildDirByNumber());
    splitByNumberField.setText(String.valueOf(appInfo.getChildDirSplitNumber()));
    viewerPathField.setText(appInfo.getViewerPath());
    evRegExField.setText(String.join(",", appInfo.getEvRegExStrings()));
    noteRegExField.setText(String.join(",", appInfo.getNoteRegExStrings()));
  }

  private String emptyToNull(JTextField textField) {
    String string = textField.getText();
    if (string.equals("")) {
      return null;
    } else {
      return string;
    }
  }

  private Integer getIntFromTF(JTextField textField) {
    String string = textField.getText();
    Integer integer;
    try {
      integer = Integer.valueOf(string);
    } catch (NumberFormatException e) {
      integer = null;
    }
    return integer;
  }

  private void setValues() {
    appInfo.setSaveDir(emptyToNull(saveDirField));
    appInfo.setUseChildDir(useChildDirChkBx.isSelected());
    appInfo.setChildDirPrefix(emptyToNull(childDirPrefixField));
    appInfo.setSplitChildDirBySize(splitBySizeRB.isSelected());
    appInfo.setChildDirSplitSize(getIntFromTF(splitSizeField));
    appInfo.setSplitChildDirByNumber(splitByNumberRB.isSelected());
    appInfo.setChildDirSplitNumber(getIntFromTF(splitByNumberField));
    appInfo.setViewerPath(emptyToNull(viewerPathField));
    // TODO イベントタグと備考の正規表現に関しては後日

  }

  private class PrefsDialogListner extends WindowAdapter {

    BrowserWindow owner;

    public PrefsDialogListner(BrowserWindow owner) {
      this.owner = owner;
    }

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
        appInfo.setRectPref(window.getBounds());
        Util.writeBean(owner.main.prefFile, appInfo);
      } catch (Exception e) {
        throw e;
      }
    }
  }

}
