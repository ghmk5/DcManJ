package com.github.ghmk5.dcmanj.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
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

  // ビューワパス
  JTextField viewerPathField;
  JButton selectViewerExecutableButton;

  // パーサ関連
  JList<String> evRegExList;
  DefaultListModel<String> evRegExModel;
  JButton addEvRegExButton;
  JButton loadDefaultEvRegExButton;
  JList<String> noteRegExList;
  DefaultListModel<String> noteRegExModel;
  JButton addNoteRegExButton;
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
    panel.add(Box.createVerticalStrut(8));
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
    evRegExList = new JList<String>();
    RegExListPopupMenu contextMenu = new RegExListPopupMenu(evRegExList);
    evRegExList.setComponentPopupMenu(contextMenu);
    JScrollPane scrollPane = new JScrollPane(evRegExList);
    childBox.add(scrollPane);
    Box grandChildBox = Box.createHorizontalBox();
    childBox.add(grandChildBox);
    addEvRegExButton = new JButton("追加");
    grandChildBox.add(addEvRegExButton);
    loadDefaultEvRegExButton = new JButton("初期値に戻す");
    grandChildBox.add(loadDefaultEvRegExButton);
    childBox = Box.createVerticalBox();
    box.add(childBox);
    childBox.setBorder(new TitledBorder("備考項目"));
    noteRegExList = new JList<String>();
    contextMenu = new RegExListPopupMenu(noteRegExList);
    noteRegExList.setComponentPopupMenu(contextMenu);
    scrollPane = new JScrollPane(noteRegExList);
    childBox.add(scrollPane);
    grandChildBox = Box.createHorizontalBox();
    childBox.add(grandChildBox);
    addNoteRegExButton = new JButton("追加");
    grandChildBox.add(addNoteRegExButton);
    loadDefaultNoteRegExButton = new JButton("初期値に戻す");
    grandChildBox.add(loadDefaultNoteRegExButton);
    evRegExList.setPreferredSize(new Dimension(120, getPreferredSize().height));
    noteRegExList.setPreferredSize(new Dimension(120, getPreferredSize().height));

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
    evRegExList.addMouseListener(new ListItemEditorCaller(evRegExList));
    addEvRegExButton.addActionListener(new ListItemAdder(evRegExList));
    loadDefaultEvRegExButton
        .addActionListener(new DefaultListLoader(evRegExList, appInfo.getDefaultEvRegExStrings()));
    noteRegExList.addMouseListener(new ListItemEditorCaller(noteRegExList));
    addNoteRegExButton.addActionListener(new ListItemAdder(noteRegExList));
    loadDefaultNoteRegExButton.addActionListener(
        new DefaultListLoader(noteRegExList, appInfo.getDefaultNoteRegExStrings()));
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

    // ビューワパス
    viewerPathField.setText(appInfo.getViewerPath());

    // 正規表現タグ
    evRegExModel = new DefaultListModel<String>();
    for (String element : appInfo.getEvRegExStrings()) {
      evRegExModel.addElement(element);
    }
    evRegExList.setModel(evRegExModel);

    noteRegExModel = new DefaultListModel<String>();
    for (String element : appInfo.getNoteRegExStrings()) {
      noteRegExModel.addElement(element);
    }
    noteRegExList.setModel(noteRegExModel);

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

    // ビューワパス
    appInfo.setViewerPath(emptyToNull(viewerPathField));

    // 正規表現タグ
    Object[] objects = ((DefaultListModel<String>) evRegExList.getModel()).toArray();
    String[] strings = new String[objects.length];
    for (int i = 0; i < objects.length; i++) {
      strings[i] = (String) objects[i];
    }
    appInfo.setEvRegExStrings(strings);
    objects = ((DefaultListModel<String>) noteRegExList.getModel()).toArray();
    strings = new String[objects.length];
    for (int i = 0; i < objects.length; i++) {
      strings[i] = (String) objects[i];
    }
    appInfo.setNoteRegExStrings(strings);

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

  public static String emptyToNull(JTextField textField) {
    String string = textField.getText();
    if (string.equals("")) {
      return null;
    } else {
      return string;
    }
  }

  public static Integer getIntFromTF(JTextField textField) {
    String string = textField.getText();
    Integer integer;
    try {
      integer = Integer.valueOf(string);
    } catch (NumberFormatException e) {
      integer = null;
    }
    return integer;
  }

  /**
   * 正規表現を格納するJListのアイテムを変更/追加する
   *
   * @param list 操作対象のJList
   * @param idx 変更対象アイテムのインデックス <U> nullを指定すると新規アイテムの追加になる </U>
   * @param regex 変更後のまたは新規追加する正規表現
   */
  private void setRegExToList(JList<String> list, Integer idx, String regex) {
    try {
      Pattern.compile(regex);
    } catch (PatternSyntaxException e) {
      JOptionPane.showMessageDialog(null, regex + " は正規表現としてパースできません");
      return;
    }
    if (regex.equals("")) {
      return;
    }
    if (Objects.nonNull(idx)) {
      ((DefaultListModel<String>) list.getModel()).set(list.getSelectedIndex(), regex);
    } else {
      ((DefaultListModel<String>) list.getModel()).addElement(regex);
      int lastIdx = list.getModel().getSize() - 1;
      list.ensureIndexIsVisible(lastIdx);
      list.setSelectedIndex(lastIdx);
    }
  }

  private class RegExListPopupMenu extends JPopupMenu {
    JMenuItem edit;
    JMenuItem remove;

    public RegExListPopupMenu(JList<String> list) {
      edit = new JMenuItem("編集...");
      edit.addActionListener(new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent e) {
          String selectedString = list.getSelectedValue();
          String returned = JOptionPane.showInputDialog(rootPane, "正規表現を入力してください", selectedString);
          if (Objects.nonNull(returned)) {
            setRegExToList(list, list.getSelectedIndex(), returned);
          }
        }
      });
      add(edit);
      remove = new JMenuItem("消去...");
      remove.addActionListener(new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent e) {
          int returnedValue = JOptionPane.showConfirmDialog(rootPane, "選択された項目をリストから除去します。よろしいですか？",
              "確認", JOptionPane.YES_NO_OPTION);
          if (returnedValue == JOptionPane.YES_OPTION) {
            List<String> selectedItems = list.getSelectedValuesList();
            if (selectedItems.size() > 0) {
              for (String selectedItem : selectedItems) {
                list.setSelectedValue(selectedItem, false);
                ((DefaultListModel<String>) list.getModel()).remove(list.getSelectedIndex());
              }
            }
          }
        }
      });
      add(remove);
    }

    // 選択行が一行のときだけ有効化するメニューの設定
    @Override
    public void show(Component c, int x, int y) {
      @SuppressWarnings("unchecked")
      JList<String> list = (JList<String>) c;
      if (list.getSelectedIndices().length > 0) {
        boolean isSelectedSingleRow = (list.getSelectedIndices().length == 1);
        edit.setEnabled(isSelectedSingleRow);
        super.show(c, x, y);
      }
    }

  }

  private class ListItemEditorCaller extends MouseAdapter {
    JList<String> list;

    public ListItemEditorCaller(JList<String> list) {
      this.list = list;
    }

    public void mouseClicked(MouseEvent me) {
      if (me.getClickCount() == 2) {
        String selectedString = list.getSelectedValue();
        String returned = JOptionPane.showInputDialog(rootPane, "正規表現を入力してください", selectedString);
        if (Objects.nonNull(returned)) {
          setRegExToList(list, list.getSelectedIndex(), returned);
        }
      }
    }

  }

  private class ListItemAdder extends AbstractAction {
    JList<String> list;

    public ListItemAdder(JList<String> list) {
      this.list = list;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      String returned = JOptionPane.showInputDialog(rootPane, "正規表現を入力してください");
      if (Objects.nonNull(returned)) {
        setRegExToList(list, null, returned);
      }
    }
  }

  private class DefaultListLoader extends AbstractAction {
    JList<String> list;
    String[] items;

    public DefaultListLoader(JList<String> list, String[] items) {
      this.list = list;
      this.items = items;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      int answer = JOptionPane.showConfirmDialog(rootPane, "現在の正規表現リストを消去し、デフォルト値に置き換えます。続行しますか？",
          "確認", JOptionPane.OK_CANCEL_OPTION);
      if (answer == JOptionPane.OK_OPTION) {
        ((DefaultListModel<String>) list.getModel()).clear();
        ((DefaultListModel<String>) list.getModel()).addAll(Arrays.asList(items));
      }
    }

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
