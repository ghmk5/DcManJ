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
import java.util.HashMap;
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
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;
import javax.swing.text.JTextComponent;
import com.github.ghmk5.dcmanj.info.AppInfo;
import com.github.ghmk5.dcmanj.util.Util;

public class PrefsDialog extends JDialog {

  BrowserWindow owner;
  AppInfo appInfo;

  // 保存先関連
  JTextField saveDirField;
  JButton selectSaveDirButton;
  JCheckBox useChildDirChkBx;
  JTextField childDirPrefixField;
  JRadioButton splitBySizeRB;
  JTextField splitSizeField;
  JRadioButton splitByNumberRB;
  JTextField splitNumberField;
  ButtonGroup splitStyleBG;

  // 移動関連
  JCheckBox sameAsNewEntriesChkBx;
  JCheckBox zipOnMoveChkBx;
  JCheckBox useChildDirOnMoveChkBx;
  ButtonGroup moveDestDirOptionBG;
  JTextField childDirPrefixOnMoveField;
  ButtonGroup splitStyleOnMoveBG;
  JRadioButton splitBySizeOnMoveRB;
  JTextField splitSizeOnMoveField;
  JRadioButton splitByNumberOnMoveRB;
  JTextField splitNumberOnMoveField;

  // ビューワパス
  JTextField viewerPathField;
  JButton selectViewerExecutableButton;

  // パーサ関連
  JTextArea evRegExArea;
  JButton loadDefaultEvRegExButton;
  JTextArea noteRegExArea;
  JButton loadDefaultNoteRegExButton;

  // 主ボタン
  JButton cancelButton;
  JButton applyButton;

  // 連動して有効化/無効化を切り替えるComponentを関連付けるマップ
  HashMap<Component, Component[]> relatedComponentsMap;

  public PrefsDialog() {
    super();
    initialize();
  }

  public PrefsDialog(BrowserWindow owner) {
    super(owner);
    this.owner = owner;
    this.appInfo = this.owner.main.appInfo;
    Util.mapESCtoCancel(this);
    createGUI();
    initialize();
    loadValues();
  }

  private void createGUI() {

    getContentPane().setLayout(new BorderLayout());
    JPanel panel = new JPanel();
    getContentPane().add(panel, BorderLayout.CENTER);
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

    // 保存先関連
    Box box = Box.createVerticalBox();
    box.setBorder(new TitledBorder("保存先"));
    panel.add(box);
    Box childBox = Box.createHorizontalBox();
    box.add(childBox);
    childBox.add(new JLabel("親ディレクトリ:"));
    saveDirField = new JTextField();
    saveDirField.setEditable(false);
    childBox.add(saveDirField);
    selectSaveDirButton = new JButton("Select...");
    childBox.add(selectSaveDirButton);

    childBox = Box.createHorizontalBox();
    box.add(childBox);
    useChildDirChkBx = new JCheckBox("子ディレクトリを使用する");
    childBox.add(useChildDirChkBx);
    childBox.add(new JLabel(" 接頭辞: "));
    childDirPrefixField = new JTextField(8);
    childDirPrefixField.setEditable(false);
    childDirPrefixField.setMaximumSize(getPreferredSize());
    childBox.add(childDirPrefixField);
    childBox.add(Box.createHorizontalGlue());
    box.add(childBox);

    childBox = Box.createHorizontalBox();
    box.add(childBox);
    splitBySizeRB = new JRadioButton("容量で分ける");
    childBox.add(splitBySizeRB);
    childBox.add(new JLabel("(容量(MB):"));
    splitSizeField = new JTextField(4);
    splitSizeField.setMaximumSize(getPreferredSize());
    childBox.add(splitSizeField);
    childBox.add(new JLabel(")"));
    splitByNumberRB = new JRadioButton("数で分ける");

    childBox.add(splitByNumberRB);
    childBox.add(new JLabel("(分割数:"));
    splitNumberField = new JTextField(4);
    splitNumberField.setMaximumSize(getPreferredSize());
    childBox.add(splitNumberField);
    childBox.add(new JLabel(")"));
    childBox.add(Box.createHorizontalGlue());
    splitStyleBG = new ButtonGroup();
    splitStyleBG.add(splitBySizeRB);
    splitStyleBG.add(splitByNumberRB);

    panel.add(Box.createVerticalStrut(8));

    // 移動関連
    box = Box.createVerticalBox();
    box.setBorder(new TitledBorder("移動時オプション"));
    panel.add(box);
    childBox = Box.createHorizontalBox();
    box.add(childBox);

    sameAsNewEntriesChkBx = new JCheckBox("新規エントリと同じ設定を使う");
    childBox.add(sameAsNewEntriesChkBx);
    zipOnMoveChkBx = new JCheckBox("未圧縮エントリはzipする");
    childBox.add(zipOnMoveChkBx);
    childBox.add(Box.createHorizontalGlue());

    childBox = Box.createHorizontalBox();
    box.add(childBox);
    useChildDirOnMoveChkBx = new JCheckBox("子ディレクトリを使用する");
    childBox.add(useChildDirOnMoveChkBx);
    childBox.add(new JLabel(" 接頭辞:"));
    childDirPrefixOnMoveField = new JTextField(8);
    childDirPrefixOnMoveField.setMaximumSize(getPreferredSize());
    childBox.add(childDirPrefixOnMoveField);
    childBox.add(Box.createHorizontalGlue());
    childBox = Box.createHorizontalBox();
    box.add(childBox);
    moveDestDirOptionBG = new ButtonGroup();
    splitBySizeOnMoveRB = new JRadioButton("容量で分ける");
    moveDestDirOptionBG.add(splitBySizeOnMoveRB);
    childBox.add(splitBySizeOnMoveRB);
    childBox.add(new JLabel("(容量(MB):"));
    splitSizeOnMoveField = new JTextField(4);
    splitSizeOnMoveField.setMaximumSize(getPreferredSize());
    childBox.add(splitSizeOnMoveField);
    childBox.add(new JLabel(")"));
    splitByNumberOnMoveRB = new JRadioButton("数で分ける");
    moveDestDirOptionBG.add(splitByNumberOnMoveRB);
    childBox.add(splitByNumberOnMoveRB);
    childBox.add(splitByNumberOnMoveRB);
    childBox.add(new JLabel("(分割数:"));
    splitNumberOnMoveField = new JTextField(4);
    splitNumberOnMoveField.setMaximumSize(getPreferredSize());
    childBox.add(splitNumberOnMoveField);
    childBox.add(new JLabel(")"));
    childBox.add(Box.createHorizontalGlue());
    splitStyleOnMoveBG = new ButtonGroup();
    splitStyleOnMoveBG.add(splitBySizeOnMoveRB);
    splitStyleOnMoveBG.add(splitByNumberOnMoveRB);
    childBox.add(Box.createHorizontalGlue());

    panel.add(Box.createVerticalStrut(8));

    // 画像ビューワ
    box = Box.createHorizontalBox();
    box.setBorder(new TitledBorder("画像ビューワ実行ファイルのパス"));
    panel.add(box);
    viewerPathField = new JTextField();
    viewerPathField.setEditable(false);
    box.add(viewerPathField);
    selectViewerExecutableButton = new JButton("Select...");
    box.add(selectViewerExecutableButton);
    panel.add(Box.createVerticalStrut(8));

    // パーサ関連
    box = Box.createHorizontalBox();
    box.setBorder(new TitledBorder("パーサで使用する正規表現"));
    box.setPreferredSize(new Dimension(getPreferredSize().width, 240));
    panel.add(box);
    childBox = Box.createVerticalBox();
    box.add(childBox);
    childBox.setBorder(new TitledBorder("配布イベント名"));
    evRegExArea = new JTextArea();
    JScrollPane scrollPane = new JScrollPane(evRegExArea);
    childBox.add(scrollPane);
    loadDefaultEvRegExButton = new JButton("デフォルト値に戻す");
    childBox.add(loadDefaultEvRegExButton);
    childBox = Box.createVerticalBox();
    box.add(childBox);
    childBox.setBorder(new TitledBorder("備考項目"));
    noteRegExArea = new JTextArea();
    scrollPane = new JScrollPane(noteRegExArea);
    childBox.add(scrollPane);
    loadDefaultNoteRegExButton = new JButton("デフォルト値に戻す");
    childBox.add(loadDefaultNoteRegExButton);
    evRegExArea.setPreferredSize(new Dimension(120, getPreferredSize().height));
    noteRegExArea.setPreferredSize(new Dimension(120, getPreferredSize().height));

    // 下部パネル
    panel = new JPanel();
    getContentPane().add(panel, BorderLayout.SOUTH);
    panel.setLayout(new FlowLayout(FlowLayout.TRAILING));
    cancelButton = new JButton("Cancel");
    panel.add(cancelButton);
    applyButton = new JButton("Apply");
    panel.add(applyButton);

    pack();
    setResizable(false);
  }

  private void initialize() {
    setTitle("初期設定");
    addWindowListener(new PrefsDialogListner());

    relatedComponentsMap = new HashMap<Component, Component[]>();
    relatedComponentsMap.put(useChildDirChkBx, new Component[] {childDirPrefixField, splitBySizeRB,
        splitSizeField, splitByNumberRB, splitNumberField});
    relatedComponentsMap.put(sameAsNewEntriesChkBx,
        new Component[] {useChildDirOnMoveChkBx, childDirPrefixOnMoveField, splitBySizeOnMoveRB,
            splitSizeOnMoveField, splitByNumberOnMoveRB, splitNumberOnMoveField});
    relatedComponentsMap.put(useChildDirOnMoveChkBx, new Component[] {childDirPrefixOnMoveField,
        splitBySizeOnMoveRB, splitSizeOnMoveField, splitByNumberOnMoveRB, splitNumberOnMoveField});

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
    useChildDirChkBx.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        Component c = (Component) e.getSource();
        Boolean selection = ((JCheckBox) c).isSelected();
        setEnabledRelatedComponents(selection, relatedComponentsMap.get(c));
      }
    });
    splitBySizeRB.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        Boolean selected = ((JRadioButton) e.getSource()).isSelected();
        toggleRelatedComponents(selected, new Component[] {splitSizeField},
            new Component[] {splitNumberField});
      }
    });
    splitByNumberRB.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        Boolean selected = ((JRadioButton) e.getSource()).isSelected();
        toggleRelatedComponents(selected, new Component[] {splitNumberField},
            new Component[] {splitSizeField});
      }
    });
    sameAsNewEntriesChkBx.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        Component c = (Component) e.getSource();
        Boolean selection = ((JCheckBox) c).isSelected();
        setEnabledRelatedComponents(!selection, relatedComponentsMap.get(c));
      }
    });
    zipOnMoveChkBx.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        // TODO 自動生成されたメソッド・スタブ

      }
    });
    useChildDirOnMoveChkBx.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        Component c = (Component) e.getSource();
        Boolean selection = ((JCheckBox) c).isSelected();
        setEnabledRelatedComponents(selection, relatedComponentsMap.get(c));
      }
    });
    splitBySizeOnMoveRB.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        Boolean selected = ((JRadioButton) e.getSource()).isSelected();
        toggleRelatedComponents(selected, new Component[] {splitSizeOnMoveField},
            new Component[] {splitNumberOnMoveField});

      }
    });
    splitByNumberOnMoveRB.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        Boolean selected = ((JRadioButton) e.getSource()).isSelected();
        toggleRelatedComponents(selected, new Component[] {splitNumberOnMoveField},
            new Component[] {splitSizeOnMoveField});

      }
    });
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
    cancelButton.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        dispose();
      }
    });
    applyButton.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        setValues();
        dispose();
      }
    });
  }

  private void loadValues() {

    // 保存関連
    saveDirField.setText(appInfo.getSaveDir());
    useChildDirChkBx.setSelected(appInfo.getUseChildDir());
    setEnabledRelatedComponents(appInfo.getUseChildDir(),
        relatedComponentsMap.get(useChildDirChkBx));
    childDirPrefixField.setEditable(useChildDirChkBx.isSelected());
    childDirPrefixField.setText(appInfo.getChildDirPrefix());
    splitBySizeRB.setSelected(appInfo.getSplitChildDirBySize());
    toggleRelatedComponents(appInfo.getSplitChildDirBySize(), new Component[] {splitSizeField},
        new Component[] {splitNumberField});
    splitSizeField.setText(String.valueOf(appInfo.getChildDirSplitSize()));
    splitByNumberRB.setSelected(appInfo.getSplitChildDirByNumber());
    toggleRelatedComponents(appInfo.getSplitChildDirByNumber(), new Component[] {splitNumberField},
        new Component[] {splitSizeField});
    splitNumberField.setText(String.valueOf(appInfo.getChildDirSplitNumber()));

    // 移動関連
    sameAsNewEntriesChkBx.setSelected(appInfo.getSameAsImptOnMove());
    zipOnMoveChkBx.setSelected(appInfo.getZipToStoreOnMove());
    useChildDirChkBx.setSelected(appInfo.getUseChildDirOnMove());
    splitBySizeOnMoveRB.setSelected(appInfo.getSplitChildDirBySizeOnMove());
    splitByNumberOnMoveRB.setSelected(appInfo.getSplitChildDirByNumberOnMove());
    toggleRelatedComponents(appInfo.getSplitChildDirByNumberOnMove(),
        new Component[] {splitNumberOnMoveField}, new Component[] {splitSizeOnMoveField});
    toggleRelatedComponents(appInfo.getSplitChildDirBySizeOnMove(),
        new Component[] {splitSizeOnMoveField}, new Component[] {splitNumberOnMoveField});
    setEnabledRelatedComponents(appInfo.getUseChildDirOnMove(),
        relatedComponentsMap.get(useChildDirChkBx));
    setEnabledRelatedComponents(!appInfo.getSameAsImptOnMove(),
        relatedComponentsMap.get(sameAsNewEntriesChkBx));

    // ビューワパス
    viewerPathField.setText(appInfo.getViewerPath());

    // 正規表現タグ
    for (String element : appInfo.getEvRegExStrings()) {
      evRegExArea.append(element + "\n");
    }
    for (String element : appInfo.getNoteRegExStrings()) {
      noteRegExArea.append(element + "\n");
    }
  }

  private void setValues() {

    // 保存先
    appInfo.setSaveDir(emptyToNull(saveDirField));
    appInfo.setUseChildDir(useChildDirChkBx.isSelected());
    appInfo.setChildDirPrefix(emptyToNull(childDirPrefixField));
    appInfo.setSplitChildDirBySize(splitBySizeRB.isSelected());
    appInfo.setChildDirSplitSize(getIntFromTF(splitSizeField));
    appInfo.setSplitChildDirByNumber(splitByNumberRB.isSelected());
    appInfo.setChildDirSplitNumber(getIntFromTF(splitNumberField));

    // 移動関連
    appInfo.setUseChildDirOnMove(useChildDirOnMoveChkBx.isSelected());
    appInfo.setZipToStoreOnMove(zipOnMoveChkBx.isSelected());
    appInfo.setUseChildDirOnMove(useChildDirOnMoveChkBx.isSelected());
    appInfo.setSplitChildDirBySizeOnMove(splitBySizeOnMoveRB.isSelected());
    appInfo.setChildDirSplitSizeOnMove(getIntFromTF(splitSizeOnMoveField));
    appInfo.setSplitChildDirByNumberOnMove(splitByNumberOnMoveRB.isSelected());
    appInfo.setChildDirSplitNumberOnMove(getIntFromTF(splitNumberOnMoveField));

    // ビューワパス
    appInfo.setViewerPath(emptyToNull(viewerPathField));

    // 正規表現タグ
    // TODO 正規表現の正当性検査 Test.java参照
    // エラーを出した文字列を保存しておいてまとめてやる？ 保存先の変数が必要
    // JTextAreaにドキュメントリスナを設定して、入力ごとにチェックする？

  }

  private void setEnabledRelatedComponents(Boolean b, Component[] components) {
    for (Component component : components) {
      if (component instanceof JTextComponent) {
        ((JTextComponent) component).setEditable(b);
      }
      component.setEnabled(b);
    }
  }

  private void toggleRelatedComponents(Boolean b, Component[] componentsToEnable,
      Component[] componentsToDisable) {
    for (Component component : componentsToEnable) {
      if (component instanceof JTextComponent) {
        ((JTextComponent) component).setEditable(b);
      }
      component.setEnabled(b);
    }
    for (Component component : componentsToDisable) {
      if (component instanceof JTextComponent) {
        ((JTextComponent) component).setEditable(!b);
      }
      component.setEnabled(!b);
    }
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

  private class PrefsDialogListner extends WindowAdapter {

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
        Util.writeAppInfo(appInfo);
      } catch (Exception e) {
        throw e;
      }
    }
  }

}
