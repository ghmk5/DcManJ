package com.github.ghmk5.dcmanj.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.JTextComponent;
import com.github.ghmk5.dcmanj.info.Entry;
import com.github.ghmk5.dcmanj.main.DcManJ;
import com.github.ghmk5.dcmanj.util.Util;

public class BrowserWindow extends JFrame {

  DcManJ main;
  static int CascadingOffSetX = 20;
  static int CascadingOffSetY = 20;
  JTextField queryField;
  JComboBox<String> typeComboBox;
  JRadioButton adultTrueRB;
  JRadioButton adultNullRB;
  JRadioButton adultFalseRB;
  BrowserTable table;
  DefaultTableModel model;
  JPanel advancedSearchPanel;
  ArrayList<PredicateBox> predicateList;

  public BrowserWindow(DcManJ main) {
    super();
    this.main = main;
    this.addWindowListener(new BrowserWindowListner());

    // MacOSのVM(あるいはL&F)はTableだけ選択色と被選択時文字色がおかしいので修正
    if (Util.PLATFORM.equals("mac")) {
      UIManager.put("Table.selectionBackground",
          UIManager.getColor("EditorPane.selectionBackground"));
      SwingUtilities.updateComponentTreeUI(this);
    }

    this.setTitle("DcManJ");
    // this.setTitle("DcManJ(Dev)");

    this.getContentPane().setLayout(new BorderLayout());

    JMenuBar menuBar = new JMenuBar();
    this.getContentPane().add(menuBar, BorderLayout.NORTH);
    JMenu menu = new JMenu("File");
    menuBar.add(menu);
    JMenuItem mntm = new JMenuItem("New Window");
    mntm.addActionListener(new openNewWindow(this));
    menu.add(mntm);
    mntm = new JMenuItem("Close Window");
    mntm.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        dispose();
      }
    });
    mntm.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.CTRL_DOWN_MASK));
    menu.add(mntm);
    menu.addSeparator();
    mntm = new JMenuItem("add new Entries...");
    mntm.addActionListener(new OpenImptDlgListner(this));
    menu.add(mntm);
    menu.addSeparator();
    mntm = new JMenuItem("設定");
    mntm.addActionListener(new OpenPrefsDlgListner(this));
    menu.add(mntm);

    JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    this.getContentPane().add(panel, BorderLayout.CENTER);

    JTabbedPane tabbedPane = new JTabbedPane();
    panel.add(tabbedPane);

    JPanel tabPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
    tabbedPane.addTab("通常検索", tabPanel);

    queryField = new JTextField(22);
    queryField.setToolTipText("タイトル、著者名、サークル名、副題のいずれかに所与の文字列を含むレコードを検索する");
    queryField.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        String query = queryField.getText();
        if (!query.equals("")) {
          String sql = buildSQL(true);
          refreshTable(sql);
        }

      }
    });
    TextFieldContextMenu textFieldContextMenu = new TextFieldContextMenu();
    queryField.setComponentPopupMenu(textFieldContextMenu);
    queryField.setBorder(new TitledBorder("クエリワード(LIKE検索)"));
    tabPanel.add(queryField);

    typeComboBox = new JComboBox<String>(new String[] {"無指定", "コミック", "同人誌", "雑誌", "小説"});
    typeComboBox.setBorder(new TitledBorder("種別指定"));
    tabPanel.add(typeComboBox);

    Box box = Box.createHorizontalBox();
    tabPanel.add(box);
    ButtonGroup adultFlagBG = new ButtonGroup();
    adultTrueRB = new JRadioButton("true");
    adultFlagBG.add(adultTrueRB);
    box.add(adultTrueRB);
    adultNullRB = new JRadioButton("none");
    adultFlagBG.add(adultNullRB);
    box.add(adultNullRB);
    adultFalseRB = new JRadioButton("false");
    adultFlagBG.add(adultFalseRB);
    box.add(adultFalseRB);
    box.setBorder(new TitledBorder("成人向けフラグ"));
    adultNullRB.setSelected(true);



    JButton button = new JButton("全表示");
    button.setFont(DcManJ.TABLEFONT);
    button.setToolTipText("種別と成人向けフラグのみを指定して検索");
    button.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        refreshTable(buildSQL(false));
      }
    });
    tabPanel.add(button);

    advancedSearchPanel = new JPanel();
    advancedSearchPanel.setLayout(new BoxLayout(advancedSearchPanel, BoxLayout.X_AXIS));
    tabbedPane.addTab("詳細検索", advancedSearchPanel);

    predicateList = new ArrayList<PredicateBox>();

    PredicateBox predicateBox = new PredicateBox(this);
    predicateList.add(predicateBox);
    advancedSearchPanel.add(predicateBox);

    AddTakePredBox addTakePredBox = new AddTakePredBox(this);
    advancedSearchPanel.add(addTakePredBox);

    advancedSearchPanel.add(Box.createHorizontalGlue());

    JPanel lowerPanel = new JPanel(new BorderLayout());
    panel.add(lowerPanel);

    table = new BrowserTable(main, this);
    table.getTableHeader().setFont(DcManJ.TABLEFONT);
    table.setFont(DcManJ.TABLEFONT);

    HashMap<String, Integer> columnWidthMap = main.appInfo.getColumnWidthMain();
    if (Objects.nonNull(columnWidthMap)) {
      table.setColumnWidth(columnWidthMap);
    }

    JScrollPane scrollPane = new JScrollPane(table);
    lowerPanel.add(scrollPane, BorderLayout.CENTER);

    this.pack();
  }

  private String buildSQL(Boolean useQueryWords) {
    ArrayList<String> criteria = new ArrayList<String>();
    StringBuilder outerSqlBuilder = new StringBuilder("select rowid, * from magdb");
    if (useQueryWords && !queryField.getText().equals("")) {
      StringBuilder innerSqlBuilder = new StringBuilder();
      String queryWord = Util.quoteForLike(queryField.getText());
      innerSqlBuilder.append("(title like " + queryWord + " escape \'^\'");
      innerSqlBuilder.append(" or circle like " + queryWord + " escape \'^\'");
      innerSqlBuilder.append(" or author like " + queryWord + " escape \'^\'");
      innerSqlBuilder.append(" or subtitle like " + queryWord + " escape \'^\')");
      criteria.add(innerSqlBuilder.toString());
    }
    String specifiedType = (String) typeComboBox.getSelectedItem();
    switch (specifiedType) {
      case "コミック":
        criteria.add("type is \'comic\'");
        break;
      case "同人誌":
        criteria.add("type is \'doujinshi\'");
        break;
      case "雑誌":
        criteria.add("type is \'magazine\'");
        break;
      case "小説":
        criteria.add("type is \'novel\'");
        break;
    }
    if (adultTrueRB.isSelected()) {
      criteria.add("adult is \'true\'");
    } else if (adultFalseRB.isSelected()) {
      criteria.add("adult is \'false\'");
    }
    if (criteria.size() > 0) {
      outerSqlBuilder.append(" where ");
      outerSqlBuilder.append(String.join(" and ", criteria.toArray(new String[criteria.size()])));
    }
    outerSqlBuilder.append(" order by rowid desc;");
    return outerSqlBuilder.toString();
  }

  /**
   * 指定のWindowに対するカスケード位置に移動する オフセットはクラス定数CascadingOffSet[XY]で定義される
   *
   * @param w 位置の基準にするWindowインスタンス
   */
  public void setLocationCascadeOn(Window w) {
    int x = w.getLocation().x + CascadingOffSetX;
    int y = w.getLocation().y + CascadingOffSetY;
    this.setLocation(x, y);
  }

  private static class openNewWindow implements ActionListener {
    BrowserWindow browserWindow;

    public openNewWindow(BrowserWindow browserWindow) {
      this.browserWindow = browserWindow;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      BrowserWindow newWindow = new BrowserWindow(browserWindow.main);
      newWindow.setLocationCascadeOn(browserWindow);
      browserWindow.main.listBrowserWindows.add(newWindow);
      newWindow.setSize(new Dimension(browserWindow.getWidth(), newWindow.getHeight()));
      newWindow.setVisible(true);
    }
  }

  private static class OpenImptDlgListner implements ActionListener {
    BrowserWindow browserWindow;

    public OpenImptDlgListner(BrowserWindow browserWindow) {
      this.browserWindow = browserWindow;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      ImportDialog importDialog;
      try {
        importDialog = new ImportDialog(browserWindow, browserWindow.main.appInfo);
        Util.setRect(importDialog, browserWindow.main.appInfo.getRectImpt());
        importDialog.setVisible(true);
      } catch (IllegalArgumentException | IOException e1) {
        // TODO 自動生成された catch ブロック
        e1.printStackTrace();
      }
    }

  }

  private static class OpenPrefsDlgListner implements ActionListener {
    BrowserWindow browserWindow;

    public OpenPrefsDlgListner(BrowserWindow browserWindow) {
      this.browserWindow = browserWindow;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      PrefsDialog prefsDialog = new PrefsDialog(browserWindow);
      Util.setRect(prefsDialog, browserWindow.main.appInfo.getRectPref());
      prefsDialog.setVisible(true);
    }

  }

  @SuppressWarnings("unused")
  private static class mntmPackListner implements ActionListener {
    BrowserWindow browserWindow;

    public mntmPackListner(BrowserWindow browserWindow) {
      this.browserWindow = browserWindow;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      browserWindow.pack();
    }
  }

  private class BrowserWindowListner extends WindowAdapter {
    public void windowClosing(WindowEvent e) {
      // タイトルバーのクローズボックスクリックで閉じられてときに呼ばれる
      saveInfo((Window) e.getSource());
    }

    public void windowClosed(WindowEvent e) {
      // dispose()されたときに呼ばれる
      saveInfo((Window) e.getSource());
    }

    private void saveInfo(Window window) {
      try {
        main.listBrowserWindows.remove((BrowserWindow) window);
        if (main.listBrowserWindows.size() == 0) {
          main.appInfo.setRectMain(window.getBounds());
          HashMap<String, Integer> columnWidthMap = table.getColumnWidth();
          main.appInfo.setColumnWidthMain(columnWidthMap);
          Util.writeAppInfo(main.appInfo);
          ((BrowserWindow) window).setDefaultCloseOperation(EXIT_ON_CLOSE);
        }
      } catch (Exception e) {
        throw e;
      }
    }
  }

  public static int getCascadingOffSetX() {
    return CascadingOffSetX;
  }

  public static void setCascadingOffSetX(int cascadingOffSetX) {
    BrowserWindow.CascadingOffSetX = cascadingOffSetX;
  }

  public static int getCascadingOffSetY() {
    return CascadingOffSetY;
  }

  public static void setCascadingOffSetY(int cascadingOffSetY) {
    CascadingOffSetY = cascadingOffSetY;
  }

  public void refreshTable(String sql) {
    table.refresh(sql);
  }

  public void refreshTable(ArrayList<Entry> entryList) {
    table.refreshMap(entryList);
    table.updateSelectedRows();
  }

  class TextFieldContextMenu extends JPopupMenu {
    private final Action cutAction = new DefaultEditorKit.CutAction();
    private final Action copyAction = new DefaultEditorKit.CopyAction();
    // private final Action pasteAction = new DefaultEditorKit.PasteAction();
    private final Action paste = new AbstractAction("paste & search") {

      @Override
      public void actionPerformed(ActionEvent e) {
        queryField.setText("");

        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        Object queryString;
        try {
          queryString = clipboard.getData(DataFlavor.stringFlavor);
          if (queryString instanceof String) {
            queryField.setText(((String) queryString).trim());
          }
        } catch (Exception e1) {
          // TODO 自動生成された catch ブロック
          e1.printStackTrace();
        }

        executeLikeQuery(queryField.getText());
      }
    };
    private final JMenuItem deleteItem;

    protected TextFieldContextMenu() {
      super();
      add(cutAction);
      add(copyAction);
      // add(pasteAction);
      add(paste);
      addSeparator();
      deleteItem = add("delete");
      deleteItem.addActionListener(e -> ((JTextComponent) getInvoker()).replaceSelection(null));
      addSeparator();
      add("select all").addActionListener(e -> ((JTextComponent) getInvoker()).selectAll());
    }

    @Override
    public void show(Component c, int x, int y) {
      if (c instanceof JTextComponent) {
        JTextComponent tc = (JTextComponent) c;
        boolean hasSelectedText = Objects.nonNull(tc.getSelectedText());
        cutAction.setEnabled(hasSelectedText);
        copyAction.setEnabled(hasSelectedText);
        deleteItem.setEnabled(hasSelectedText);
        super.show(c, x, y);
      }
    }
  }

  private void executeLikeQuery(String queryWord) {
    queryWord = Util.quoteForLike(queryWord);
    String sql =
        "select rowid, * from magdb where title like " + queryWord + " or circle like " + queryWord
            + " or author like " + queryWord + " or subtitle like " + queryWord + "escape \'^\';";
    table.refresh(sql);

  }
}

