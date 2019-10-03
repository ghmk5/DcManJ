package com.github.ghmk5.dcmanj.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.font.TextAttribute;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.Vector;
import javax.swing.BorderFactory;
import javax.swing.ComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.plaf.basic.BasicComboBoxUI;

/**
 * 一見JLabelに見えるコンボボックス この拡張クラスに限った話ではないが、ポップアップリストが表示される位置はコンポーネントの下端なので、
 * BorderLayout.CENTERとかで自動的にfillされる配置方法だと、親コンポーネントいっぱいに広げられる結果、
 * メニューの表示位置が親コンポーネントの下端になり、ポップアップリストだけが離れた位置に表示されているように見える
 * 普通のJComboBoxならComboBox自体の描画色が親コンポーネントと異なるので、自身が広げられているならひと目で分かるが、
 * この拡張クラスでは親コンポーネントの色と区別がつかないので妙な表示に見える
 *
 * あと、MacOSでは、起動後はじめてポップアップリストを出したとき、拡幅されたリストの真ん中に幅が狭いままの要素と
 * スクロールバーが配置され、リストの左右に余白があるという妙な表示になる。二回目以降はまともな表示になるのだが
 * Windows環境ではこの現象は見られないので、MacOS版VM特有の問題と思われる(故に解決策の見当がつかない)
 *
 * @author mk5
 *
 * @param <E>
 */
public class LabelLikeComboBox<E> extends JComboBox<E> {

  private int popupWidth;

  {
    popupWidth = 0;
  }

  public LabelLikeComboBox() {
    super();
  }

  public LabelLikeComboBox(ComboBoxModel<E> aModel) {
    super(aModel);
  }

  public LabelLikeComboBox(E[] items) {
    super(items);
  }

  public LabelLikeComboBox(Vector<E> items) {
    super(items);
  }

  /**
   * ComboBox本体の幅とは別にポップアップリストのリストの幅を指定する ポップアップリストの幅は登録されたデータモデル中の最も幅の大きいアイテムの幅に合わせられる
   * considerScrollBarがtrueのとき、スクロールバー分の幅が足される
   *
   * @param considerScrollBar
   */
  public void setPopupWidth(Boolean considerScrollBar) {

    ComboBoxModel<E> aModel = this.getModel();
    if (aModel.getSize() > 0 && (aModel.getElementAt(0) instanceof String)) {
      String modelItemString;
      Font font = this.getFont();
      BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
      Graphics2D g2d = img.createGraphics();
      g2d.setFont(font);
      FontMetrics fm = g2d.getFontMetrics();
      for (int i = 0; i < aModel.getSize(); i++) {
        modelItemString = aModel.getElementAt(i).toString();
        if (this.popupWidth < fm.stringWidth(modelItemString)) {
          this.popupWidth = fm.stringWidth(modelItemString);
          if (considerScrollBar)
            this.popupWidth += UIManager.getInt("ScrollBar.width") + 4;
        }
      }
    }

    this.addPopupMenuListener(new WidePopupMenuListener(this.popupWidth));
  }

  /**
   * ComboBox本体の幅とは別にポップアップリストのリストの幅を指定する considerScrollBarがtrueのとき、スクロールバー分の幅が足される
   *
   * @param popupWidth ポップアップリストの幅(ピクセル数)
   * @param considerScrollBar
   */
  public void setPopupWidth(int popupWidth, Boolean considerScrollBar) {
    this.popupWidth = popupWidth;
    setPopupWidth(considerScrollBar);
  }

  /**
   * ポップアップリストが表示されている間だけ本体の幅を広げるリスナ
   *
   * @author mk5
   *
   */
  private class WidePopupMenuListener implements PopupMenuListener {
    private int minWidth;
    private boolean adjusting;

    /**
     * ポップアップリストが表示されている間だけ本体の幅を広げるリスナ
     *
     * @param minWidth ComboBoxに登録されたデータモデル中の最大幅に関わりなく、 ポップアップリストの幅は最小でもこの数値を維持する
     *        (データモデル中の最大幅がこの数値を超える場合、ポップアップリスト幅は データモデル側の数値に従う)
     */
    public WidePopupMenuListener(int minWidth) {
      super();
      this.minWidth = minWidth;
    }

    @Override
    public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
      JComboBox<?> combo = (JComboBox<?>) e.getSource();
      Dimension size = combo.getSize();
      if (size.width >= this.minWidth) {
        return;
      }
      if (!adjusting) {
        adjusting = true;
        combo.setSize(this.minWidth, size.height);
        combo.showPopup();
      }
      combo.setSize(size);
      adjusting = false;
    }

    @Override
    public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
      /* not needed */
    }

    @Override
    public void popupMenuCanceled(PopupMenuEvent e) {
      /* not needed */
    }
  }

  // コンボボックスの見た目を変える
  @Override
  public void updateUI() {
    super.updateUI();
    final Color lightBlue = new Color(96, 96, 225);
    final Color lightOrange = new Color(212, 160, 48);

    UIManager.put("ComboBox.squareButton", Boolean.FALSE);
    // ↓ここ、問答無用でJPanelのデフォルト背景色を指定しているが、ホントなら親コンポーネントの色を取得してくるべきところじゃなかろうか
    // Javaだとaddの主体が親コンポーネントの方なので、配置するメソッドをoverrideするわけにも行かないし。
    // 色を変えたパネルに配置した場合は、インスタンス化したあとでsetBackgroundするしかないか
    UIManager.put("ComboBox.background", UIManager.getColor(UIManager.get("Panel.background")));
    UIManager.put("ComboBox.disabledBackground",
        UIManager.getColor(UIManager.get("Panel.background")));

    setUI(new BasicComboBoxUI() {

      // isEnabled(false)したときの背景色をパネルと同じに変更 isEnabled(true)のときはアンダーラインあり
      // isEnalbled(false)のときはアンダーラインなし
      @Override
      public void paintCurrentValue(Graphics g, Rectangle bounds, boolean hasFocus) {
        ListCellRenderer renderer = comboBox.getRenderer();
        Component c;

        if (hasFocus && !isPopupVisible(comboBox)) {
          c = renderer.getListCellRendererComponent(listBox, comboBox.getSelectedItem(), -1, true,
              false);
        } else {
          c = renderer.getListCellRendererComponent(listBox, comboBox.getSelectedItem(), -1, false,
              false);
          c.setBackground(UIManager.getColor("ComboBox.background"));
        }
        c.setFont(comboBox.getFont());
        Font font = c.getFont();
        Map fontAttributes = c.getFont().getAttributes();
        if (hasFocus && !isPopupVisible(comboBox)) {
          c.setForeground(listBox.getSelectionForeground());
          c.setBackground(listBox.getSelectionBackground());
        } else {
          if (comboBox.isEnabled()) {
            c.setForeground(comboBox.getForeground());
            c.setBackground(comboBox.getBackground());
            fontAttributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
            c.setFont(font.deriveFont(fontAttributes));
          } else {

            c.setForeground((Color) UIManager.get("Label.disabledForeground"));
            c.setBackground((Color) UIManager.get("Panel.background"));
            fontAttributes.put(TextAttribute.UNDERLINE, null);
            c.setFont(font.deriveFont(fontAttributes));
          }

          if (comboBox.getSelectedItem().toString().startsWith("|")) {
            c.setForeground(lightBlue);
          } else if (comboBox.getSelectedItem().toString().startsWith("\\")
              || comboBox.getSelectedItem().toString().equals(">>")) {
            c.setForeground(lightOrange);
          }

        }

        // Fix for 4238829: should lay out the JPanel.
        boolean shouldValidate = false;
        if (c instanceof JPanel) {
          shouldValidate = true;
        }

        int x = bounds.x, y = bounds.y, w = bounds.width, h = bounds.height;
        if (padding != null) {
          x = bounds.x + padding.left;
          y = bounds.y + padding.top;
          w = bounds.width - (padding.left + padding.right);
          h = bounds.height - (padding.top + padding.bottom);
        }

        currentValuePane.paintComponent(g, c, comboBox, x, y, w, h, shouldValidate);
      }

      // 矢印を出さなくする
      @Override
      protected JButton createArrowButton() {
        JButton button = new JButton(); // .createArrowButton();
        button.setBorder(BorderFactory.createEmptyBorder());
        button.setVisible(false);
        return button;
      }
    });

    ListCellRenderer<? super String> r = (ListCellRenderer<? super String>) getRenderer();
    setRenderer((ListCellRenderer<? super E>) new ListCellRenderer<String>() {
      private final Color bgc = UIManager.getColor("ComboBox.background");

      // ポップアップリスト項目の書式と文字色背景色
      @Override
      public Component getListCellRendererComponent(JList<? extends String> list, String value,
          int index, boolean isSelected, boolean cellHasFocus) {
        JLabel c =
            (JLabel) r.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        Font font = c.getFont();
        Map fontAttributes = c.getFont().getAttributes();
        fontAttributes.put(TextAttribute.UNDERLINE, null);
        c.setFont(font.deriveFont(fontAttributes));
        c.setHorizontalAlignment(SwingConstants.CENTER);
        if (isSelected) {
          c.setForeground(list.getSelectionForeground());
          c.setBackground(list.getSelectionBackground());
        } else {
          c.setForeground(list.getForeground());
          c.setBackground(bgc);
        }
        if (c.getText().startsWith("|")) {
          c.setForeground(lightBlue);
        } else if (c.getText().startsWith("\\") || c.getText().equals(">>")) {
          c.setForeground(lightOrange);
        }
        return c;
      }
    });
    setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
    setOpaque(false);
    setFocusable(false);
  }

}
