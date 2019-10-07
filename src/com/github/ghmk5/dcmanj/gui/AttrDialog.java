package com.github.ghmk5.dcmanj.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;
import com.github.ghmk5.dcmanj.info.Entry;

public class AttrDialog extends JDialog {

  Window owner;
  Entry entry;
  String[] types = {"comic", "doujinshi", "magazine", "novel"};

  public static void main (String[] args ) {
    AttrDialog attrDialog = new AttrDialog(null);
    attrDialog.setVisible(true);
  }

  //  public AttrDialog(Window owner, Entry entry) {
  public AttrDialog(Window owner) {
    super(owner);
    this.owner = owner;
//    this.entry = entry;

    setTitle("属性値設定");
    getContentPane().setLayout(new BorderLayout());
    JPanel panel = new JPanel();
    getContentPane().add(panel, BorderLayout.CENTER);
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

    Box box = Box.createHorizontalBox();
    panel.add(box);
    JComboBox<String> comboBox = new JComboBox<String>(types);
    comboBox.setBorder(new TitledBorder("種別"));
    comboBox.setMaximumSize(new Dimension(128, 52));
    box.add(comboBox);
    JCheckBox checkBox = new JCheckBox("成人向");
    box.add(checkBox);

    box = Box.createHorizontalBox();
    panel.add(box);
    JTextField textField = new JTextField(18);
    textField.setBorder(new TitledBorder("サークル"));
    box.add(textField);
    textField = new JTextField(18);
    textField.setBorder(new TitledBorder("著者"));
    box.add(textField);

    box = Box.createHorizontalBox();
    panel.add(box);
    textField = new JTextField(36);
    textField.setBorder(new TitledBorder("タイトル"));
    box.add(textField);

    box = Box.createHorizontalBox();
    panel.add(box);
    textField = new JTextField(36);
    textField.setBorder(new TitledBorder("サブタイトル"));
    box.add(textField);

    box = Box.createHorizontalBox();
    panel.add(box);
    textField = new JTextField(9);
    textField.setBorder(new TitledBorder("巻号"));
    box.add(textField);
    textField = new JTextField(9);
    textField.setBorder(new TitledBorder("issue"));
    box.add(textField);
    textField = new JTextField(9);
    textField.setBorder(new TitledBorder("頁数"));
    box.add(textField);
    textField = new JTextField(9);
    textField.setBorder(new TitledBorder("サイズ(MB)"));
    box.add(textField);

    box = Box.createHorizontalBox();
    panel.add(box);
    textField = new JTextField(36);
    textField.setBorder(new TitledBorder("備考"));
    box.add(textField);

    box = Box.createHorizontalBox();
    panel.add(box);
    textField = new JTextField(18);
    textField.setBorder(new TitledBorder("元ネタ"));
    box.add(textField);
    textField = new JTextField(18);
    textField.setBorder(new TitledBorder("配布イベント"));
    box.add(textField);

    box = Box.createHorizontalBox();
    panel.add(box);
    textField = new JTextField(18);
    textField.setBorder(new TitledBorder("日付"));
    box.add(textField);
    textField = new JTextField(18);
    textField.setBorder(new TitledBorder("保存パス"));
    box.add(textField);

    box = Box.createHorizontalBox();
    panel.add(box);
    JLabel label = new JLabel(" ");
    label.setMaximumSize(new Dimension(Short.MAX_VALUE, Short.MAX_VALUE));
    label.setBorder(new TitledBorder("保存ファイル/ディレクトリ名"));
    box.add(label);



    panel = new JPanel();
    getContentPane().add(panel, BorderLayout.SOUTH);
    panel.setLayout(new FlowLayout(FlowLayout.TRAILING));
    JButton cancelButton = new JButton("Cancel");
    panel.add(cancelButton);
    JButton applyButton = new JButton("Apply");
    panel.add(applyButton);

    pack();
    setResizable(false);
  }

}
