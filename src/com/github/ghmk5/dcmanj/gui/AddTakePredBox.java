package com.github.ghmk5.dcmanj.gui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.border.EmptyBorder;

public class AddTakePredBox extends Box {

  BrowserWindow owner;
  JButton addButton;
  JButton takeButton;

  public AddTakePredBox(BrowserWindow owner) {
    super(BoxLayout.Y_AXIS);
    this.owner = owner;
    addButton = new JButton("+");
    addButton.setMaximumSize(addButton.getMinimumSize());
    addButton.setBorder(new EmptyBorder(2, 2, 2, 2));
    addButton.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        if (owner.predicateList.size() > 3) {
          return;
        }
        Component[] components = owner.advancedSearchPanel.getComponents();
        owner.advancedSearchPanel.remove(components[components.length - 1]);
        owner.advancedSearchPanel.remove(components[components.length - 2]);
        JComboBox<String> andOrBox = new JComboBox<String>(new String[] {"AND", "OR"});
        int w = andOrBox.getPreferredSize().width;
        andOrBox.setMaximumSize(new Dimension(w, andOrBox.getMaximumSize().height));
        owner.advancedSearchPanel.add(andOrBox);
        PredicateBox predicateBox = new PredicateBox(owner);
        owner.predicateList.add(predicateBox);
        owner.advancedSearchPanel.add(predicateBox);
        owner.advancedSearchPanel.add(new AddTakePredBox(owner));
        owner.advancedSearchPanel.add(Box.createHorizontalGlue());
        owner.advancedSearchPanel.updateUI();
      }
    });
    add(addButton);
    takeButton = new JButton("-");
    takeButton.setMaximumSize(takeButton.getMinimumSize());
    takeButton.setBorder(new EmptyBorder(2, 2, 2, 2));
    takeButton.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        if (owner.predicateList.size() == 1) {
          return;
        }
        Component[] components = owner.advancedSearchPanel.getComponents();
        owner.advancedSearchPanel.remove(components[components.length - 3]);
        owner.advancedSearchPanel.remove(components[components.length - 4]);
        owner.predicateList.remove(owner.predicateList.size() - 1);
        owner.advancedSearchPanel.updateUI();
      }
    });
    add(takeButton);
    setBorder(new EmptyBorder(2, 4, 2, 4));
  }
}
