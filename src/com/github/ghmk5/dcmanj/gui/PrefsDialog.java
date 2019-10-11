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
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;
import com.github.ghmk5.dcmanj.info.AppInfo;
import com.github.ghmk5.dcmanj.util.Util;

public class PrefsDialog extends JDialog {

  BrowserWindow owner;
  AppInfo appInfo;

  public PrefsDialog(BrowserWindow owner) {
    super(owner);
    this.owner = owner;
    this.appInfo = this.owner.main.appInfo;
    Util.mapESCtoCancel(this);
    initialize();
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
    JTextField saveDirField = new JTextField(24);
    saveDirField.setText(appInfo.getSaveDir());
    box.add(saveDirField);
    JButton selectSaveDirButton = new JButton("Select...");
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
    box.setBorder(new TitledBorder("保存先ディレクトリ"));

    box = Box.createHorizontalBox();
    panel.add(box);
    JTextField viewerPathField = new JTextField(24);
    viewerPathField.setText(appInfo.getViewerPath());
    box.add(viewerPathField);
    JButton selectViewerExecutableButton = new JButton("Select...");
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
    JTextField evRegExField = new JTextField(24);
    evRegExField.setBorder(new TitledBorder("イベントタグの正規表現リスト"));
    panel.add(evRegExField);

    JTextField noteRegExField = new JTextField(24);
    noteRegExField.setBorder(new TitledBorder("備考項目の正規表現リスト"));
    panel.add(noteRegExField);

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
    applyButton.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        appInfo.setSaveDir(saveDirField.getText());
        appInfo.setViewerPath(viewerPathField.getText());

        dispose();
      }
    });
    panel.add(applyButton);

    pack();
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
