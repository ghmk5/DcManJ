package com.github.ghmk5.dcmanj.gui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import com.github.ghmk5.dcmanj.info.Entry;
import com.github.ghmk5.dcmanj.util.Util;

public class PredicateBox extends Box {

  BrowserWindow owner;
  JComboBox<String> columnNameBox;
  JComboBox<String> operatorBox;
  JTextField expressionField;

  static HashMap<String, String[]> OperatorsMap;
  static {
    OperatorsMap = new HashMap<String, String[]>();
    OperatorsMap.put("String", new String[] {"IS", "IS NOT", "LIKE", "NOT LIKE", "GLOB"});
    OperatorsMap.put("Integer", new String[] {"=", "!=", ">", "=>", "<", "=<"});
    OperatorsMap.put("Boolean", new String[] {"IS", "IS NOT"});
    OperatorsMap.put("OffsetDateTime", new String[] {"=", "!=", ">", "=>", "<", "=<"});
    OperatorsMap.put("Path", new String[] {"IS", "IS NOT", "LIKE", "NOT LIKE", "GLOB"});
  }

  public PredicateBox(BrowserWindow owner) {
    super(BoxLayout.Y_AXIS);
    this.owner = owner;
    columnNameBox = new JComboBox<String>(Entry.TAG_NAMES);
    columnNameBox.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        String tagName = (String) columnNameBox.getSelectedItem();
        String className = Entry.dataClassNameOf(tagName);
        String[] operators = OperatorsMap.get(className);
        ComboBoxModel<String> aModel = new DefaultComboBoxModel<String>(operators);
        operatorBox.setModel(aModel);
      }
    });
    add(columnNameBox);
    int w = columnNameBox.getPreferredSize().width;
    operatorBox = new JComboBox<String>();
    String tagName = (String) columnNameBox.getSelectedItem();
    String className = Entry.dataClassNameOf(tagName);
    String[] operators = OperatorsMap.get(className);
    ComboBoxModel<String> aModel = new DefaultComboBoxModel<String>(operators);
    operatorBox.setModel(aModel);
    add(operatorBox);
    expressionField = new JTextField(8);
    expressionField.addActionListener(new ExecuteQueryAction(owner));
    add(expressionField);
    setMaximumSize(new Dimension(w, getPreferredSize().height));
  }

  private class ExecuteQueryAction extends AbstractAction {

    BrowserWindow owner;

    public ExecuteQueryAction(BrowserWindow owner) {
      this.owner = owner;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      StringBuilder statementBuilder = new StringBuilder("SELECT ROWID, * FROM magdb WHERE ");
      Component[] components = owner.advancedSearchPanel.getComponents();
      Component component;
      PredicateBox predicateBox;
      String predicate;
      JComboBox<String> andOrBox;
      for (int i = 0; i < components.length; i++) {
        component = components[i];
        try {
          if (component instanceof PredicateBox) {
            predicateBox = (PredicateBox) component;
            predicate = predicateBox.getPredicate();
            statementBuilder.append("(");
            statementBuilder.append(predicate);
            statementBuilder.append(")");
          } else if (component instanceof JComboBox<?>) {
            andOrBox = (JComboBox<String>) component;
            statementBuilder.append(" ");
            statementBuilder.append((String) andOrBox.getSelectedItem());
            statementBuilder.append(" ");
          }
        } catch (IllegalArgumentException ex) {
          System.out.println(ex.toString());
          System.out.println("predicateが生成できないのでスキップします");
          i++;
        }
      }
      statementBuilder.append(";");
      String sql = statementBuilder.toString();

      if (sql.matches(".+[AO][NR]D? ;$")) {
        sql = sql.replaceAll(" [AO][NR]D? ", "");
      }
      System.out.println(sql);
      if (sql.matches(".+WHERE ;$")) {
        System.out.println("有効なpredicateがありません");
      } else {
        owner.refreshTable(sql);
      }
    }

  }

  public String getColumnName() {
    return (String) columnNameBox.getSelectedItem();
  }

  public String getOperator() {
    return (String) operatorBox.getSelectedItem();
  }

  public String getExpression() {
    return (String) expressionField.getText();
  }

  public String getPredicate() {
    String predicate = null;

    String tagName = (String) columnNameBox.getSelectedItem();
    String operator = (String) operatorBox.getSelectedItem();
    String expression = (String) expressionField.getText().trim();

    if (expression.equals("")) {
      throw new IllegalArgumentException("クエリフィールドが空");
    }

    String columnName = Entry.columnNameOf(tagName);
    String className = Entry.dataClassNameOf(tagName);

    if (operator.contains("LIKE")) {
      expression = Util.quoteForLike(expression);
      if (expression.matches(".+[\\^].+")) {
        expression += " ESCAPE \'^\'";
      }
    } else {
      switch (className) {
        case "String":
          expression = expression.replaceAll("'", "''");
          expression = "'" + expression + "'";
          break;
        case "Boolean":
          if (!expression.matches("[Tt][Rr][Uu][Ee]")
              && !expression.matches("[Ff][Aa][Ll][Ss][Ee]")) {
            throw new IllegalArgumentException("真偽値のクエリフィールドにtrue/falseとしてパースできない文字列が与えられた");
          }
          expression = expression.toLowerCase();
          expression = "'" + expression + "'";
          break;
        case "Integer":
          if (expression.matches(".+[^0-9].+")) {
            throw new IllegalArgumentException("整数値のクエリフィールドに半角数字以外の文字が与えられた");
          }
          break;
        case "Double":
          if (expression.matches(".+[^0-9\\.].+")) {
            throw new IllegalArgumentException("少数値のクエリフィールドに半角数字と小数点以外の文字が与えられた");
          }
          break;
        case "OffsetDateTime":
          try {
            // これだと 2007-12-03T10:15:30+01:00 の形式のテキストしか受け付けない
            // TODO もっと融通の効くパーサが必要 例えば、12/5 で 今年の12月5日午前零時、現在地のオフセットと解釈してくれるような
            OffsetDateTime offsetDateTime = OffsetDateTime.parse(expression);
            expression = "'" + offsetDateTime.format(Util.DTF) + "'";
          } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("日付のクエリフィールドに日付としてパースできない文字列が与えられた");
          }
      }
    }
    // GLOBのときはエスケープもユーザに任せる

    String[] strings = {columnName, operator, expression};
    predicate = String.join(" ", strings);

    return predicate;
  }
}
