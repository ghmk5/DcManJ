package com.github.ghmk5.dcmanj.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Objects;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.JTextComponent;
import com.github.ghmk5.dcmanj.main.DcManJ;
import com.github.ghmk5.dcmanj.util.Util;

public class BrowserWindow extends JFrame implements WindowListener, ComponentListener {

  DcManJ main;
  static int CascadingOffSetX = 20;
  static int CascadingOffSetY = 20;
  JTextField queryField;
  BrowserTable table;
  DefaultTableModel model;

  public BrowserWindow(DcManJ main) {
    super();
    this.main = main;
    this.addWindowListener(this);

    this.getContentPane().setLayout(new BorderLayout());

    JMenuBar menuBar = new JMenuBar();
    this.getContentPane().add(menuBar, BorderLayout.NORTH);
    JMenu menu = new JMenu("File");
    menuBar.add(menu);
    JMenuItem mntm = new JMenuItem("New Window");
    mntm.addActionListener(new openNewWindow(this));
    menu.add(mntm);
    mntm = new JMenuItem("add new Entries...");
    mntm.addActionListener(new mntmPackListner(this));
    menu.add(mntm);

    JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    this.getContentPane().add(panel, BorderLayout.CENTER);

    JTabbedPane tabbedPane = new JTabbedPane();
    panel.add(tabbedPane);

    JPanel tabPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
    // panel.add(upperPanel);
    tabbedPane.addTab("通常検索", tabPanel);

    queryField = new JTextField(36);
    queryField.setToolTipText("タイトル、著者名、サークル名、副題のいずれかに所与の文字列を含むレコードを検索する");
    queryField.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        String query = queryField.getText();
        executeLikeQuery(query);
      }
    });
    TextFieldContextMenu textFieldContextMenu = new TextFieldContextMenu();
    queryField.setComponentPopupMenu(textFieldContextMenu);
    tabPanel.add(queryField);

    JCheckBox checkBox = new JCheckBox("成");
    tabPanel.add(checkBox);

    JButton button = new JButton("全表示");
    button.setFont(main.tableFont);
    button.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        String sql;
        if (checkBox.isSelected()) {
          sql = "select rowid, * from magdb where adult = 'true' order by rowid desc;";
        } else {
          sql = "select rowid, * from magdb order by rowid desc;";
        }
        try {
          table.refresh(sql);
        } catch (SQLException e1) {
          // TODO 自動生成された catch ブロック
          e1.printStackTrace();
        }
      }
    });
    tabPanel.add(button);

    tabPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 5));
    tabbedPane.addTab("詳細検索", tabPanel);

    tabPanel.add(Box.createHorizontalStrut(10));
    String[] fieldOptions = {"著者", "サークル", "タイトル", "備考"};
    JComboBox<String> comboBox = new JComboBox<String>(fieldOptions);
    tabPanel.add(comboBox);
    String[] statementOptions = {"IS", "LIKE", "GLOB"};
    comboBox = new JComboBox<String>(statementOptions);
    // comboBox.setFont(comboBox.getFont().deriveFont(comboBox.getFont().getSize() - 1.0f));
    tabPanel.add(comboBox);
    JTextField textField = new JTextField(8);
    tabPanel.add(textField);

    // button = new JButton("詳細検索");
    // button.setFont(main.tableFont);
    // upperPanel.add(button);

    // button = new JButton("追加...");
    // button.setFont(main.tableFont);
    // upperPanel.add(button);

    JPanel lowerPanel = new JPanel(new BorderLayout());
    panel.add(lowerPanel);

    table = new BrowserTable(main);

    table.getTableHeader().setFont(main.tableFont);

    table.setFont(main.tableFont);
    // table.setSelectionForeground(Color.WHITE);
    // table.setSelectionBackground(UIManager.getColor("EditorPane.selectionBackground"));

    HashMap<String, Integer> columnWidthMap = main.appInfo.getColumnWidthMap();
    if (Objects.nonNull(columnWidthMap)) {
      table.setColumnWidth(columnWidthMap);
    }

    JScrollPane scrollPane = new JScrollPane(table);
    lowerPanel.add(scrollPane, BorderLayout.CENTER);


    this.setTitle("DcManJ");
    this.pack();
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
      newWindow.setVisible(true);
    }
  }

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

  @Override
  public void componentResized(ComponentEvent e) {
    // TODO 自動生成されたメソッド・スタブ

  }

  @Override
  public void componentMoved(ComponentEvent e) {
    // TODO 自動生成されたメソッド・スタブ

  }

  @Override
  public void componentShown(ComponentEvent e) {
    // TODO 自動生成されたメソッド・スタブ

  }

  @Override
  public void componentHidden(ComponentEvent e) {
    // TODO 自動生成されたメソッド・スタブ

  }

  @Override
  public void windowOpened(WindowEvent e) {
    // TODO 自動生成されたメソッド・スタブ

  }

  @Override
  public void windowClosing(WindowEvent e) {
    main.listBrowserWindows.remove(this);
    if (main.listBrowserWindows.size() == 0) {
      main.appInfo.setRectMain(this.getBounds());
      HashMap<String, Integer> columnWidthMap = getColumnWidth();
      main.appInfo.setColumnWidthMap(columnWidthMap);
      Util.writeBean(main.prefFile, main.appInfo);
      this.setDefaultCloseOperation(EXIT_ON_CLOSE);
    }

  }

  @Override
  public void windowClosed(WindowEvent e) {
    // TODO 自動生成されたメソッド・スタブ

  }

  @Override
  public void windowIconified(WindowEvent e) {
    // TODO 自動生成されたメソッド・スタブ

  }

  @Override
  public void windowDeiconified(WindowEvent e) {
    // TODO 自動生成されたメソッド・スタブ

  }

  @Override
  public void windowActivated(WindowEvent e) {
    // TODO 自動生成されたメソッド・スタブ

  }

  @Override
  public void windowDeactivated(WindowEvent e) {
    // TODO 自動生成されたメソッド・スタブ

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

  private HashMap<String, Integer> getColumnWidth() {
    HashMap<String, Integer> columnWidthMap = new HashMap<String, Integer>();
    DefaultTableColumnModel columnModel = (DefaultTableColumnModel) this.table.getColumnModel();
    // int numColumns = columnModel.getColumnCount();
    TableColumn tableColumn;
    String identifier;
    Enumeration<TableColumn> e = columnModel.getColumns();
    while (e.hasMoreElements()) {
      tableColumn = e.nextElement();
      identifier = (String) tableColumn.getIdentifier();
      columnWidthMap.put(identifier, tableColumn.getWidth());
    }
    return columnWidthMap;
  }

  public void refreshTable(String sql) throws SQLException {
    table.refresh(sql);
  }

  class TextFieldContextMenu extends JPopupMenu {
    private final Action cutAction = new DefaultEditorKit.CutAction();
    private final Action copyAction = new DefaultEditorKit.CopyAction();
    private final Action pasteAction = new DefaultEditorKit.PasteAction();
    private final Action paste = new AbstractAction("paste & search") {

      @Override
      public void actionPerformed(ActionEvent e) {
        queryField.setText("");
        queryField.paste();
        executeLikeQuery(queryField.getText());
      }
    };
    private final JMenuItem deleteItem;

    protected TextFieldContextMenu() {
      super();
      add(cutAction);
      add(copyAction);
      add(pasteAction);
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
    try {
      table.refresh(sql);
    } catch (SQLException e1) {
      // TODO 自動生成された catch ブロック
      e1.printStackTrace();
    }

  }
}

