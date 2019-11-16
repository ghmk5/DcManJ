package com.github.ghmk5.dcmanj.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.zip.ZipException;
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
import javax.swing.ProgressMonitor;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import javax.swing.text.JTextComponent;
import com.github.ghmk5.dcmanj.info.AppInfo;
import com.github.ghmk5.dcmanj.info.Entry;
import com.github.ghmk5.dcmanj.util.Util;
import com.github.ghmk5.dcmanj.util.Worker;

public class MoveOptionDialog extends JDialog {

  Window owner;
  AppInfo appInfo;
  ArrayList<Entry> entryList;

  JCheckBox reImportChkBx;

  JCheckBox selectDestChkBx;

  JCheckBox zipOnMoveChkBx;
  JCheckBox unzipOnMoveChkBx;
  JCheckBox renOnMoveChkBx;

  JCheckBox useChildDirOnMoveChkBx;
  JTextField childDirPrefixOnMoveField;
  ButtonGroup splitStyleOnMoveBG;
  JRadioButton splitBySizeOnMoveRB;
  JTextField splitSizeOnMoveField;
  JRadioButton splitByNumberOnMoveRB;
  JTextField splitNumberOnMoveField;

  JButton cancelButton;
  JButton executeButton;

  Component[] componentsEnabledOnMove;
  Component[] componentsEnabledOnMoveToSpecifiedDest;
  Component[] componentsEnabledOnUseChildDir;

  ProgressMonitor progressMonitor;

  public static void main(String[] args) throws ZipException, IOException {
    ToolTipManager.sharedInstance().setDismissDelay(Integer.MAX_VALUE);
    AppInfo appInfo = new AppInfo();
    ArrayList<Entry> entryList = new ArrayList<Entry>();
    entryList.add(new Entry(new File("(同人誌)[サークル(著者)]タイトル(元ネタ)"), appInfo));
    MoveOptionDialog moveOptionDialog = new MoveOptionDialog(null, appInfo, entryList);
    Util.mapESCtoCancel(moveOptionDialog);
    moveOptionDialog.setVisible(true);

  }

  public MoveOptionDialog(Window owner, AppInfo appInfo, ArrayList<Entry> entryList) {
    super(owner);
    this.owner = owner;
    this.appInfo = appInfo;
    this.entryList = entryList;

    createGUI();

    componentsEnabledOnMove = new Component[10];
    componentsEnabledOnMove[0] = selectDestChkBx;
    componentsEnabledOnMove[1] = zipOnMoveChkBx;
    componentsEnabledOnMove[2] = unzipOnMoveChkBx;
    componentsEnabledOnMove[3] = renOnMoveChkBx;
    componentsEnabledOnMove[4] = useChildDirOnMoveChkBx;
    componentsEnabledOnMove[5] = childDirPrefixOnMoveField;
    componentsEnabledOnMove[6] = splitBySizeOnMoveRB;
    componentsEnabledOnMove[7] = splitSizeOnMoveField;
    componentsEnabledOnMove[8] = splitByNumberOnMoveRB;
    componentsEnabledOnMove[9] = splitNumberOnMoveField;

    componentsEnabledOnMoveToSpecifiedDest = new Component[6];
    componentsEnabledOnMoveToSpecifiedDest[0] = useChildDirOnMoveChkBx;
    componentsEnabledOnMoveToSpecifiedDest[1] = childDirPrefixOnMoveField;
    componentsEnabledOnMoveToSpecifiedDest[2] = splitBySizeOnMoveRB;
    componentsEnabledOnMoveToSpecifiedDest[3] = splitSizeOnMoveField;
    componentsEnabledOnMoveToSpecifiedDest[4] = splitByNumberOnMoveRB;
    componentsEnabledOnMoveToSpecifiedDest[5] = splitNumberOnMoveField;

    componentsEnabledOnUseChildDir = new Component[5];
    componentsEnabledOnUseChildDir[0] = childDirPrefixOnMoveField;
    componentsEnabledOnUseChildDir[1] = splitBySizeOnMoveRB;
    componentsEnabledOnUseChildDir[2] = splitSizeOnMoveField;
    componentsEnabledOnUseChildDir[3] = splitByNumberOnMoveRB;
    componentsEnabledOnUseChildDir[4] = splitNumberOnMoveField;


    setActions();
    loadValues();

    pack();
    setResizable(false);

  }

  private void createGUI() {

    getContentPane().setLayout(new BorderLayout());

    // 主パネル
    Box box = new Box(BoxLayout.Y_AXIS) {
      @Override
      public Insets getInsets() {
        return new Insets(0, 8, 0, 8);
      }
    };
    getContentPane().add(box, BorderLayout.CENTER);

    box.add(Box.createVerticalStrut(32));

    JLabel label = new JLabel(" 実行する処理を選択してください");
    label.setIcon(UIManager.getIcon("OptionPane.questionIcon"));
    Font font = label.getFont().deriveFont(label.getFont().getSize2D() + 2f);
    label.setFont(font);

    box.add(label);
    label.setAlignmentX(CENTER_ALIGNMENT);

    box.add(Box.createVerticalStrut(28));

    Box childBox = Box.createVerticalBox();

    childBox.setBorder(new TitledBorder("オプション"));
    box.add(childBox);

    Box grandChildBox = Box.createHorizontalBox();
    childBox.add(grandChildBox);
    grandChildBox.add(Box.createHorizontalStrut(8));
    reImportChkBx = new JCheckBox("再インポート");
    reImportChkBx.setToolTipText("新規エントリインポート時と同じ設定で現保存位置からリネーム / 移動し、データベースを更新します");
    grandChildBox.add(reImportChkBx);
    grandChildBox.add(Box.createHorizontalGlue());

    childBox.add(Box.createVerticalStrut(16));

    grandChildBox = Box.createHorizontalBox();
    childBox.add(grandChildBox);
    grandChildBox.add(Box.createHorizontalStrut(8));
    selectDestChkBx = new JCheckBox("移動先を指定する");
    selectDestChkBx.setToolTipText("実行ボタン押下時に移動先親ディレクトリを指定するダイアログを開きます");
    grandChildBox.add(selectDestChkBx);
    zipOnMoveChkBx = new JCheckBox("圧縮");
    zipOnMoveChkBx.setToolTipText("選択されたエントリが未圧縮の場合、zipします");
    grandChildBox.add(zipOnMoveChkBx);
    unzipOnMoveChkBx = new JCheckBox("展開");
    unzipOnMoveChkBx.setToolTipText("選択されたエントリがzipファイルの場合、展開します。zip書庫内に親ディレクトリが存在しない場合は生成されます");
    grandChildBox.add(unzipOnMoveChkBx);
    renOnMoveChkBx = new JCheckBox("ファイル名再生成");
    renOnMoveChkBx.setToolTipText("選択されたエントリのファイル名が記名法に反する場合、改名します");
    grandChildBox.add(renOnMoveChkBx);
    grandChildBox.add(Box.createHorizontalGlue());

    childBox.add(Box.createVerticalStrut(16));

    grandChildBox = Box.createHorizontalBox();
    childBox.add(grandChildBox);
    grandChildBox.add(Box.createHorizontalStrut(8));
    useChildDirOnMoveChkBx = new JCheckBox("子ディレクトリを使用する");
    grandChildBox.add(useChildDirOnMoveChkBx);
    grandChildBox.add(new JLabel(" 接頭辞:"));
    childDirPrefixOnMoveField = new JTextField(8);
    childDirPrefixOnMoveField.setMaximumSize(getPreferredSize());
    grandChildBox.add(childDirPrefixOnMoveField);
    grandChildBox.add(Box.createHorizontalGlue());
    grandChildBox = Box.createHorizontalBox();
    childBox.add(grandChildBox);
    grandChildBox.add(Box.createHorizontalStrut(8));
    splitBySizeOnMoveRB = new JRadioButton("容量で分ける");
    grandChildBox.add(splitBySizeOnMoveRB);
    grandChildBox.add(new JLabel("(容量(MB):"));
    splitSizeOnMoveField = new JTextField(4);
    splitSizeOnMoveField.setMaximumSize(getPreferredSize());
    grandChildBox.add(splitSizeOnMoveField);
    grandChildBox.add(new JLabel(")"));
    splitByNumberOnMoveRB = new JRadioButton("数で分ける");
    grandChildBox.add(splitByNumberOnMoveRB);
    grandChildBox.add(splitByNumberOnMoveRB);
    grandChildBox.add(new JLabel("(分割数:"));
    splitNumberOnMoveField = new JTextField(4);
    splitNumberOnMoveField.setMaximumSize(getPreferredSize());
    grandChildBox.add(splitNumberOnMoveField);
    grandChildBox.add(new JLabel(")"));
    grandChildBox.add(Box.createHorizontalGlue());
    splitStyleOnMoveBG = new ButtonGroup();
    splitStyleOnMoveBG.add(splitBySizeOnMoveRB);
    splitStyleOnMoveBG.add(splitByNumberOnMoveRB);
    grandChildBox.add(Box.createHorizontalGlue());
    childBox.add(Box.createVerticalStrut(8));

    box.add(Box.createVerticalStrut(16));

    // 下部パネル
    JPanel panel = new JPanel();
    getContentPane().add(panel, BorderLayout.SOUTH);
    panel.setLayout(new FlowLayout(FlowLayout.TRAILING));
    cancelButton = new JButton("Cancel");
    panel.add(cancelButton);
    executeButton = new JButton("Execute");
    panel.add(executeButton);
  }

  private void setActions() {

    setTitle("移動機能選択");

    reImportChkBx.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        Component c = (Component) e.getSource();
        Boolean selection = ((JCheckBox) c).isSelected();
        setEnabledRelatedComponents(!selection, componentsEnabledOnMove);
      }
    });

    selectDestChkBx.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        Component c = (Component) e.getSource();
        Boolean selection = ((JCheckBox) c).isSelected();
        setEnabledRelatedComponents(selection, componentsEnabledOnMoveToSpecifiedDest);
      }
    });

    useChildDirOnMoveChkBx.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        Component c = (Component) e.getSource();
        Boolean selection = ((JCheckBox) c).isSelected();
        setEnabledRelatedComponents(selection, componentsEnabledOnUseChildDir);
      }
    });

    splitBySizeOnMoveRB.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        Boolean selected = ((JRadioButton) e.getSource()).isSelected();
        splitSizeOnMoveField.setEnabled(selected);
        splitNumberOnMoveField.setEnabled(!selected);
      }
    });

    splitByNumberOnMoveRB.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        Boolean selected = ((JRadioButton) e.getSource()).isSelected();
        splitSizeOnMoveField.setEnabled(!selected);
        splitNumberOnMoveField.setEnabled(selected);
      }
    });


    cancelButton.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        dispose();
      }
    });

    executeButton.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {

        setValues();
        Util.writeAppInfo(appInfo);

        if (!appInfo.getMoveAsReImport() && appInfo.getSelectDestDirOnMove()) {
          JFileChooser fileChooser = new JFileChooser(appInfo.getMoveDestDir());
          fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
          fileChooser.setApproveButtonText("選択");
          int selected = fileChooser.showOpenDialog(owner);
          if (selected == JFileChooser.APPROVE_OPTION) {
            appInfo.setMoveDestDir(fileChooser.getSelectedFile().getAbsolutePath());
          } else {
            return;
          }
        }

        int min = 0;
        int max = 100;
        progressMonitor = new ProgressMonitor(owner, "移動中...", "開始中...", min, max);
        progressMonitor.setMillisToDecideToPopup(5);
        progressMonitor.setProgress(min);

        Worker moveWorker = new Worker(owner, progressMonitor, entryList, appInfo);

        moveWorker.addPropertyChangeListener(new PropertyChangeListener() {
          @Override
          public void propertyChange(PropertyChangeEvent evt) {
            if ("progress".equals(evt.getPropertyName())) {
              progressMonitor.setProgress((Integer) evt.getNewValue());
            }
          }
        });

        moveWorker.execute();

        dispose();
      }
    });

  }

  private void loadValues() {

    reImportChkBx.setSelected(appInfo.getMoveAsReImport());

    selectDestChkBx.setSelected(appInfo.getSelectDestDirOnMove());
    zipOnMoveChkBx.setSelected(appInfo.getZipOnMove());
    unzipOnMoveChkBx.setSelected(appInfo.getUnzipOnMove());
    renOnMoveChkBx.setSelected(appInfo.getRenOnMove());

    useChildDirOnMoveChkBx.setSelected(appInfo.getUseChildDirOnMove());
    setEnabledRelatedComponents(useChildDirOnMoveChkBx.isSelected(),
        componentsEnabledOnUseChildDir);
    childDirPrefixOnMoveField.setText(appInfo.getChildlDirPrefixOnMove());
    splitBySizeOnMoveRB.setSelected(appInfo.getSplitChildDirBySize());
    splitSizeOnMoveField.setText(String.valueOf(appInfo.getChildDirSplitSizeOnMove()));
    splitSizeOnMoveField.setEnabled(splitBySizeOnMoveRB.isSelected());
    splitByNumberOnMoveRB.setSelected(appInfo.getSplitChildDirByNumberOnMove());
    splitNumberOnMoveField.setText(String.valueOf(appInfo.getChildDirSplitNumberOnMove()));
    splitNumberOnMoveField.setEnabled(splitByNumberOnMoveRB.isSelected());

    setEnabledRelatedComponents(selectDestChkBx.isSelected(),
        componentsEnabledOnMoveToSpecifiedDest);
    setEnabledRelatedComponents(!reImportChkBx.isSelected(), componentsEnabledOnMove);

  }

  private void setValues() {

    appInfo.setMoveAsReImport(reImportChkBx.isSelected());
    appInfo.setSelectDestDirOnMove(selectDestChkBx.isSelected());
    appInfo.setZipOnMove(zipOnMoveChkBx.isSelected());
    appInfo.setUnzipOnMove(unzipOnMoveChkBx.isSelected());
    appInfo.setRenOnMove(renOnMoveChkBx.isSelected());
    appInfo.setUseChildDirOnMove(useChildDirOnMoveChkBx.isSelected());
    appInfo.setChildlDirPrefixOnMove(PrefsDialog.emptyToNull(childDirPrefixOnMoveField));
    appInfo.setSplitChildDirBySizeOnMove(splitBySizeOnMoveRB.isSelected());
    appInfo.setChildDirSplitSizeOnMove(PrefsDialog.getIntFromTF(splitSizeOnMoveField));
    appInfo.setSplitChildDirByNumberOnMove(splitByNumberOnMoveRB.isSelected());
    appInfo.setChildDirSplitNumberOnMove(PrefsDialog.getIntFromTF(splitNumberOnMoveField));

  }

  private void setEnabledRelatedComponents(Boolean b, Component[] components) {
    for (Component component : components) {
      if (component instanceof JTextComponent) {
        ((JTextComponent) component).setEditable(b);
      }
      component.setEnabled(b);
    }
  }

}
