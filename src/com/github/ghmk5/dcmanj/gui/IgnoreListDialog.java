package com.github.ghmk5.dcmanj.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Objects;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import com.github.ghmk5.dcmanj.info.AppInfo;
import com.github.ghmk5.dcmanj.util.Util;

public class IgnoreListDialog extends JDialog {

  Window owner;
  AppInfo appInfo;
  String[] namesToBeIgnored;


  public IgnoreListDialog(Window owner, AppInfo appInfo) {
    super(owner, "無視するファイル名リストを編集");
    this.owner = owner;
    this.appInfo = appInfo;

    getContentPane().setLayout(new BorderLayout());
    JTextArea textArea = new JTextArea();
    textArea.setMargin(new Insets(4, 4, 4, 4));
    textArea.setEditable(true);
    textArea.setToolTipText("インポート元ディレクトリ内にあっても無視されるファイル/ディレクトリ名のリスト(拡張子を含み完全一致)");
    JScrollPane scrollPane = new JScrollPane(textArea);
    getContentPane().add(scrollPane, BorderLayout.CENTER);
    Box box = Box.createHorizontalBox();
    getContentPane().add(box, BorderLayout.SOUTH);
    box.add(Box.createHorizontalGlue());
    JButton cancelButton = new JButton("Cancel");
    cancelButton.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        dispose();
      }
    });
    box.add(cancelButton);
    JButton applyButton = new JButton("Apply");
    applyButton.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        namesToBeIgnored = textArea.getText().split("\n");
        appInfo.setNamesToBeIgnored(namesToBeIgnored);
        Util.writeAppInfo(appInfo);
        dispose();
      }
    });
    box.add(applyButton);

    namesToBeIgnored = appInfo.getNamesToBeIgnored();
    if (Objects.nonNull(namesToBeIgnored) && namesToBeIgnored.length > 0) {
      textArea.append(String.join("\n", namesToBeIgnored));
    }

    setMinimumSize(new Dimension(250, 160));
  }
}
