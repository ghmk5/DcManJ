package com.github.ghmk5.dcmanj.gui;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Objects;
import java.util.Vector;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

/**
 * 拡張版JTable カラムヘッダクリックでソート可能 カラムヘッダ境界ダブルクリックで列幅調整可能<BR>
 * マウスオーバーでセル内容をToolTipText表示 これら以外にOverrideしたい場合は
 *
 * ExtendedTable exTable = new ExtendedTable(dataModel) {
 *
 * @Override public boolean isCellEditable(int row, int column) { return false; } }; とかやる
 */
public class ExtendedTable extends JTable {

  public ExtendedTable() {
    super();
  }

  public ExtendedTable(int numRows, int numColumns) {
    super(numRows, numColumns);
  }

  public ExtendedTable(Object[][] rowData, Object[] columnNames) {
    super(rowData, columnNames);
  }

  public ExtendedTable(TableModel dm, TableColumnModel cm, ListSelectionModel sm) {
    super(dm, cm, sm);
  }

  public ExtendedTable(TableModel dm, TableColumnModel cm) {
    super(dm, cm);
  }

  public ExtendedTable(TableModel dm) {
    super(dm);
  }

  public ExtendedTable(Vector<? extends Vector> rowData, Vector<?> columnNames) {
    super(rowData, columnNames);
  }

  // カラムヘッダクリックでソートできるcolumnModelをoverride
  @Override
  protected JTableHeader createDefaultTableHeader() {
    return new ExtendedTableHeader(super.columnModel);
  }

  // getToolTipText()をオーバーライド
  // 列幅が足りていない場合、マウスポインタが上にあるセルのtooltipにそのセルの内容を設定する
  @Override
  public String getToolTipText(MouseEvent e) {

    java.awt.Point p = e.getPoint();
    int rowIndex = rowAtPoint(p);
    int colIndex = columnAtPoint(p);

    String tip = null;
    int columnWidth = getColumnModel().getColumn(colIndex).getWidth();

    try {
      TableCellRenderer r = getCellRenderer(rowIndex, colIndex); // レンダラー
      Object value = getValueAt(rowIndex, colIndex); // セル内容
      Component c = r.getTableCellRendererComponent(this, value, false, false, rowIndex, colIndex);
      int contentWidth = c.getPreferredSize().width; // セルの最適表示幅
      String contentAsString = getValueAt(rowIndex, colIndex).toString();
      if (contentWidth > columnWidth)
        tip = contentAsString;
    } catch (RuntimeException e1) {
      // catch null pointer exception if mouse is over an empty line
    }

    return tip;
  }

  // テーブル全体を編集不可にする場合
  @Override
  public boolean isCellEditable(int row, int column) {
    return false;
  }

  /**
   * 指定列データ行の字揃えを変更する(ヘッダの字揃えは変更されない)
   *
   * 参考:https://ateraimemo.com/Swing/CellTextAlignment.html
   *
   * @param columnIdx 字揃えを変更したい列のID
   * @param alignment 字揃え指定 SwingConstants.LEFT, SwingConstants.CENTER, SwingConstants.RIGHT など
   */
  public void setColumnAlignment(int columnIdx, int alignment) {
    DefaultTableCellRenderer tableCellRenderer = new DefaultTableCellRenderer();
    tableCellRenderer.setHorizontalAlignment(alignment);
    getColumnModel().getColumn(columnIdx).setCellRenderer(tableCellRenderer);

    // ヘッダの字揃えも変更しようとして↓をやると、データのクラスが同じ他の列でも変更されてしまう
    // getColumnModel().getColumn(columnIdx).setHeaderRenderer(new
    // HorizontalAlignmentHeaderRenderer(alignment));
  }

  /**
   * 列幅を設定する テーブルの列に予めidentifierを指定しておく必要がある
   *
   * @param columnWidthMap キーは列のidentifier, 値は列幅
   */
  public void setColumnWidth(HashMap<String, Integer> columnWidthMap) {
    DefaultTableColumnModel defaultTableColumnModel = (DefaultTableColumnModel) getColumnModel();
    TableColumn column = null;
    int columnIdx;

    for (String key : columnWidthMap.keySet()) {
      try {
        columnIdx = defaultTableColumnModel.getColumnIndex(key);
        column = defaultTableColumnModel.getColumn(columnIdx);
        if (Objects.nonNull(columnWidthMap.get(key))) {
          column.setPreferredWidth(columnWidthMap.get(key));
        }
      } catch (Exception e) {
        System.out.println(key + " seems removed");
      }
    }
  }

  /**
   * 列幅を記録したマップを返す
   *
   * @return HashMap<String, Integer> キーは列に設定されたidentifierr、値は列幅
   */
  public HashMap<String, Integer> getColumnWidth() {
    HashMap<String, Integer> columnWidthMap = new HashMap<String, Integer>();
    DefaultTableColumnModel columnModel = (DefaultTableColumnModel) getColumnModel();
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

  public ArrayList<Object> getSelectedColumnValues(String identifier)
      throws IllegalArgumentException {
    DefaultTableColumnModel columnModel = (DefaultTableColumnModel) getColumnModel();
    int tableColumnIdx = columnModel.getColumnIndex(identifier);
    ArrayList<Object> objectList = new ArrayList<Object>();
    for (int tableRowIdx : getSelectedRows()) {
      tableRowIdx = convertRowIndexToModel(tableRowIdx);
      objectList.add(getModel().getValueAt(tableRowIdx, tableColumnIdx));
    }
    return objectList;
  }

  /**
   * JTableセル内容の水平方向表示位置を指定するためのレンダラ拡張クラス ヘッダの字揃え変更を諦めたので現在使われていない
   *
   * 参考: https://ateraimemo.com/Swing/HorizontalAlignmentHeaderRenderer.html
   *
   */
  class HorizontalAlignmentHeaderRenderer implements TableCellRenderer {
    private final int horizontalAlignment; // = SwingConstants.LEFT;

    protected HorizontalAlignmentHeaderRenderer(int horizontalAlignment) {
      this.horizontalAlignment = horizontalAlignment;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
        boolean hasFocus, int row, int column) {
      TableCellRenderer r = table.getTableHeader().getDefaultRenderer();
      JLabel l =
          (JLabel) r.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
      l.setHorizontalAlignment(horizontalAlignment);
      return l;
    }
  }

  /**
   * ダブルクリックで列幅調節機能＋αを盛り込んだ改変版TableHeader
   * http://www.ne.jp/asahi/hishidama/home/tech/java/swing/JTable.html
   */
  class ExtendedTableHeader extends JTableHeader {

    /** コンストラクター */
    public ExtendedTableHeader(TableColumnModel columnModel) {
      super(columnModel);
    }

    @Override
    protected void processMouseEvent(MouseEvent e) {
      if (e.getID() == MouseEvent.MOUSE_CLICKED // クリックイベント
          && SwingUtilities.isLeftMouseButton(e)) { // 左クリック
        Cursor cur = super.getCursor();
        if (cur.getType() == Cursor.E_RESIZE_CURSOR) { // 矢印カーソル
          int cc = e.getClickCount();
          if (cc % 2 == 1) {
            // シングルクリック
            // ここでリターンしない場合、ソート機能が働いてしまう
            return;
          } else {
            // ダブルクリック
            Point pt = new Point(e.getX() - 3, e.getY()); // 列幅変更の場合、3ピクセルずらされて考慮されている
            int vc = super.columnAtPoint(pt);
            if (vc >= 0) {
              sizeWidthToFitData(vc);
              e.consume();
              return;
            }
          }
        }
      }
      super.processMouseEvent(e);
    }

    /**
     * テーブルカラムをデータの幅に合わせる.
     *
     * @param columnIdx 表示列番号
     */
    public void sizeWidthToFitData(int columnIdx) {
      JTable table = super.getTable();
      TableColumn tc = table.getColumnModel().getColumn(columnIdx);

      int w = 0;
      int max = 0;

      int vrows = table.getRowCount(); // 表示行数
      TableCellRenderer r;
      Component c;
      for (int i = 0; i < vrows; i++) {
        r = table.getCellRenderer(i, columnIdx); // レンダラー
        Object value = table.getValueAt(i, columnIdx); // データ
        c = r.getTableCellRendererComponent(table, value, false, false, i, columnIdx);
        w = c.getPreferredSize().width; // データ毎の幅
        if (max < w) {
          max = w;
        }
      }

      // ヘッダの表示に必要な幅を考慮
      r = table.getTableHeader().getDefaultRenderer();
      c = r.getTableCellRendererComponent(table, tc.getHeaderValue(), false, false, 0, columnIdx);
      w = c.getPreferredSize().width; // ヘッダセルの幅
      if (max < w) {
        max = w;
      }

      tc.setPreferredWidth(max + 8); // いくらか足してやらないとギリギリで省略表示になってしまう 最適な加算値はおそらくL&Fによって違う
    }
  }

}
