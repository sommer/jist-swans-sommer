//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <GuiLog.java Tue 2004/04/06 11:59:20 barr pompom.cs.cornell.edu>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.runtime.guilog;

import jist.runtime.Event;
import jist.runtime.Controller;
import jist.runtime.Util;

import java.rmi.RemoteException;

import java.lang.reflect.*;
import java.awt.*;
import java.awt.event.*;

import java.util.Comparator;
import java.util.Hashtable;
import java.util.Arrays;
import java.util.Vector;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.*;
import javax.swing.ImageIcon;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JOptionPane;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import java.awt.Dimension;

/**
 * <p>Title: GuiLog</p>
 * <p>Description: Creates a GUI that displays event information.</p>
 * @author Mark Fong
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version 1.0
 */
public class GuiLog
{
  //////////////////////////////////////////////////
  // statics
  //

  /**
   * The font used for most of the table.
   */
  public static Font   tableFont;

  /**
   * Object that acquires the lock for pausing the Controller.
   */
  private static Object pauselock = new Object();

  /**
   * Pause-status of the Controller.
   */
  private static boolean paused = false;

  /**
   * Whether to Step through one event or not.
   */
  private static boolean isStep = false;

  /**
   * Re-used Icon.  Displayed on pauseButton.
   */
  public static ImageIcon pauseIcon  = createImageIcon("images/pause.jpg");

  /**
   * Re-used Icon.  Displayed on pauseButton.
   */
  public static ImageIcon resumeIcon = createImageIcon("images/resume.jpg");

  /**
   * Re-used Icon.  Displayed on stepButton.
   */
  public static ImageIcon stepIcon = createImageIcon("images/step.jpg");

  /**
   * Re-used Icon.  Displayed on untilButton.
   */
  public static ImageIcon untilIcon = createImageIcon("images/clock.jpg");

  /**
   * Re-used Icon.  Displayed on upButton.
   */
  public static ImageIcon upIcon     = createImageIcon("images/up.gif");

  /**
   * Re-used Icon.  Displayed on downButton.
   */
  public static ImageIcon downIcon   = createImageIcon("images/down.gif");

  /**
   * Creates an ImageIcon.
   *
   * @param path String The path where the image is located.
   * @return ImageIcon with the specified image.
   */
  public static ImageIcon createImageIcon(String path)
  {
    java.net.URL imgURL = GuiLog.class.getResource(path);
    if(imgURL==null)
    {
      System.err.println("Couldn't find file: " + path);
      return null;
    }
    return new ImageIcon(imgURL);
  }

  static
  {
    JFrame.setDefaultLookAndFeelDecorated(true);
  }

  // reused components

  /**
   * Re-used JButton.  Points upwards to indicate position of event-relative.
   */
  public static JButton cellUpButton;

  /**
   * Re-used JButton.  Points downwards to indicate position of event-relative.
   */
  public static JButton cellDownButton;

  static
  {
    // initialize the table-font and html font tags
    tableFont       = new Font("Arial",Font.PLAIN,10);

    // initialize table cell up button
    cellUpButton = new JButton(upIcon);
    cellUpButton.setVerticalTextPosition(SwingConstants.BOTTOM);
    cellUpButton.setHorizontalTextPosition(SwingConstants.CENTER);
    cellUpButton.setFont(tableFont);

    // initialize table cell down button
    cellDownButton = new JButton(downIcon);
    cellDownButton.setVerticalTextPosition(SwingConstants.TOP);
    cellDownButton.setHorizontalTextPosition(SwingConstants.CENTER);
    cellDownButton.setFont(tableFont);
  }

  //////////////////////////////////////////////////
  // instance
  //

  /**
   *  The JFrame for displaying the GUI.
   */
  public static JFrame frame;

  /**
   * The JPanel for displaying the GUI.
   */
  private JPanel panel;

  /**
   * The JTable for displaying the Event information.
   */
  private JTable table;

  /**
   * The JButton that pauses and resumes the GuiLog.
   */
  private static JButton pauseButton;

  /**
   * The JButton that allows one event to be added, and then pauses.
   */
  private static JButton stepButton;

  /**
   * The JButton that starts the Until-timer dialog.
   */
  private static JButton untilButton;

  /**
   * A list used to track the events as they are generated so that they can easily be deleted
   * first-in, first-out.
   */
  private LinkedList list;

  /**
   * Maximum number of Events allowed to be displayed in table.
   */
  private int numEventsThreshHold;

  /**
   * The data model for the table.
   */
  private EventTableModel model;

  /**
   * GuiLog creates and shows the GUI when an outside synchronized thread instantiates an instance of GuiLog.
   *
   * @param maxNumEvents int Maximum number of Events allowed to be displayed in table.
   */
  public GuiLog(int maxNumEvents)
  {
    //Schedule a job for the event-dispatching thread:
    //creating and showing this application's GUI.
    javax.swing.SwingUtilities.invokeLater(new Runnable()
    {
      public void run()
      {
        createAndShowGUI();
      }
    });
    numEventsThreshHold = maxNumEvents;
    list = new LinkedList();
    synchronized(this)
    {
      while(frame==null)
      {
        try
        {
          wait();
        }
        catch(InterruptedException e)
        {
        }
      }
    }
    frame.invalidate();
  }

  /**
   * Creates the JTable and shows it.
   */
  private synchronized void createAndShowGUI()
  {
    JFrame frame = new JFrame("GuiLog");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    // create table panel
    GridBagLayout layout = new GridBagLayout();// added
    panel = new JPanel(layout); // added
    panel.setOpaque(true);
    frame.setContentPane(panel);

    // create table model and specify table column formatting
    model = new EventTableModel();
    TableColumnModel myTableColumnModel = new DefaultTableColumnModel();
    String[] columnNames = {"T","Type","Method","Object","Cont","Parent","Children"};
    int[]modelIndex = {0,1,2,3,4,5,6};
    int[]widths = {120,40,250,200,75,120,130};
    boolean[] resizeable = {true,true,true,true,true,true,true};
    for(int i=0;i<columnNames.length;i++)
    {
      myTableColumnModel.addColumn(createTableColumn(columnNames[i],widths[i],modelIndex[i],resizeable[i]));
    }

    // Calculate and Set the window size
    int sum=0;
    for(int i=0;i<widths.length;i++)
    {
      sum+=widths[i];
    }
    sum+=myTableColumnModel.getColumnMargin()*(widths.length)*2+1;
    Dimension windowSize = new Dimension(sum,700);

    // create table
    table = new JTable(model,myTableColumnModel);
    table.setRowHeight(40);
    table.setRowMargin(0);
    table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
    final TableCellRenderer cellRenderer = new EventCellRenderer();
    for(int i=0;i<=6;i++)
    {
      myTableColumnModel.getColumn(i).setCellRenderer(cellRenderer);
    }
    final TableCellEditor cellEditor = new ChildrenCellEditor(table, model);
    myTableColumnModel.getColumn(6).setCellEditor(cellEditor);
    table.addMouseListener(new MouseAdapterHandler(table,model));
    table.setPreferredScrollableViewportSize(windowSize);

    // disable column reordering
    table.getTableHeader().setReorderingAllowed(false);
    // set the table's header's font
    table.getTableHeader().setFont(new Font("Arial",Font.BOLD,14));

    // create table scrollPane
    JScrollPane scrollPane = new JScrollPane(table);

    // create the ButtonHandler that is used by multiple
    ButtonHandler buttonHandler = new ButtonHandler();

    // create the pauseButton
    pauseButton = new JButton("Pause", pauseIcon);
    pauseButton.setSize(new Dimension(1,1));
    pauseButton.addActionListener(buttonHandler);

    // create the stepButton
    stepButton = new JButton("Step", stepIcon);
    stepButton.setSize(new Dimension(1,1));
    stepButton.setEnabled(false);
    stepButton.addActionListener(buttonHandler);

    // create the untilButton
    untilButton = new JButton("Until", untilIcon);
    untilButton.setSize(new Dimension(1,1));
    untilButton.setEnabled(false);
    untilButton.addActionListener(buttonHandler);

    // add the components to the JPanel
    GridBagConstraints constraints = new GridBagConstraints();
    // constraints for the scrollPane
    constraints.fill    = GridBagConstraints.BOTH;
    constraints.anchor  = GridBagConstraints.NORTH;
    constraints.weightx = 100;
    constraints.weighty = 100;
    // add the scrollPane
    addComponent(scrollPane,constraints,0,1,1,1);
    // constraints for the pauseButton
    constraints.fill    = GridBagConstraints.NONE;
    constraints.anchor  = GridBagConstraints.WEST;
    constraints.weightx = 0;
    constraints.weighty = 0;
    // add the pauseButton
    addComponent(pauseButton,constraints,0,0,1,1);
    // constraints for the stepButton
    constraints.fill    = GridBagConstraints.NONE;
    constraints.anchor  = GridBagConstraints.CENTER;
    constraints.weightx = 0;
    constraints.weighty = 0;
    // add the pauseButton
    addComponent(stepButton,constraints,0,0,1,1);
    // constraints for the untilButton
    constraints.fill    = GridBagConstraints.NONE;
    constraints.anchor  = GridBagConstraints.EAST;
    constraints.weightx = 0;
    constraints.weighty = 0;
    addComponent(untilButton,constraints,0,0,1,1);

    // show frame
    frame.pack();
    frame.setVisible(true);

    this.frame = frame;
    notifyAll();
  }

  //////////////////////////////////////////////////
  // helper methods
  //

  /**
   * helper method.  Creates a TableColumn.
   *
   * @param name String TableColumn's HeaderValue.
   * @param width int TableColumn's width.
   * @param colIndex int TableColumn's model index.
   * @param resize boolean If the TableColumn can be resized.
   * @return TableColumn
   */
  private TableColumn createTableColumn(String name, int width, int colIndex, boolean resize)
  {
    TableColumn tc = new TableColumn();
    tc.setHeaderValue(name);
    tc.setPreferredWidth(width);
    tc.setModelIndex(colIndex);
    tc.setResizable(resize);
    return tc;
  }

  /**
   * helper method.  Adds a component with layout constraints to the GuiLog's JPanel.
   *
   * @param c Component The component to be added.
   * @param constraints GridBagConstraints The constraints for the GridBagLayout manager.
   * @param x int The GridBagConstraints' gridx field.
   * @param y int The GridBagConstraints' gridy field.
   * @param w int The GridBagConstraints' gridwidth field.
   * @param h int The GridBagConstraints' gridheight field.
   */
  private void addComponent(Component c, GridBagConstraints constraints, int x, int y, int w, int h)
  {
    constraints.gridx      = x;
    constraints.gridy      = y;
    constraints.gridwidth  = w;
    constraints.gridheight = h;
    panel.add(c,constraints);
  }

  /**
   * Getter method.
   * @return JButton
   */
  public static JButton getPauseButton()
  {
    return pauseButton;
  }
  /**
   * Getter method.
   * @return JButton
   */
  public static JButton getStepButton()
  {
    return stepButton;
  }

  /**
   * Getter method.
   * @return JButton
   */
  public static JButton getUntilButton()
  {
    return untilButton;
  }

  /**
   * Defines how mouse clicks on the Parent column are handled.
   */
  public static class MouseAdapterHandler extends MouseAdapter
  {
    /**
     * The table that is receiving mouse clicks.
     */
    private JTable table;

    /**
     * The table's data model.
     */
    private EventTableModel model;

    /**
     * The MouseAdapterHandler's constructor.
     *
     * @param t JTable The table that is receiving mouse clicks.
     * @param m EventTableModel The table's data model.
     */
    public MouseAdapterHandler(JTable t, EventTableModel m)
    {
      this.table = t;
      this.model = m;
    }

    /**
     * Determines what to do when a mouse is clicked on the table.
     *
     * @param e MouseEvent The mouse event that occurs.
     */
    public void mouseClicked(MouseEvent e)
    {
      JTable clickedTable = (JTable)e.getSource();
      int selectedRow     = clickedTable.getSelectedRow();    // the row that was clicked on
      int selectedColumn  = clickedTable.getSelectedColumn(); // the column that was clicked on
      int rowHeight       = clickedTable.getRowHeight();      // the row height of each row in the table
      int jumpRow         = -1;                               // the row that we jump to
      boolean doJump      = false;                            // if cell is empty, false. otherwise true.

      switch(selectedColumn)
      {
        case 5: // get the parent's row index
          if(model.getParentIndex(selectedRow)!=-1)
          {
            jumpRow = model.getParentIndex(selectedRow);
            doJump  = true;
          }
          break;
        default:break;
      }
      int x      = 0;                   // x-coordinate of top-left corner of new rectangle view
      int y      = rowHeight * jumpRow; // y-coordinate of top-left corner of new rectangle view
      int width  = (int)clickedTable.getPreferredScrollableViewportSize().getHeight();  // width of new rectangle view
      int height = (int)clickedTable.getPreferredScrollableViewportSize().getHeight();  // height of new rectangle view

      // If the column selected is the parent column, and the parent is still present, jump to the parent's row
      if(selectedColumn>=5 && doJump)
      {
        table.scrollRectToVisible(new Rectangle(x,y,width,height));
        // highlight the selected row
        table.setRowSelectionInterval(jumpRow,jumpRow);
        table.setSelectionBackground(Color.CYAN);
      }
    }
  }

  //////////////////////////////////////////////////
  // EventCellRenderer
  //

  /**
   * EventCellRenderer is a customized TableCellRenderer for JiST Events.
   */
  public class EventCellRenderer extends DefaultTableCellRenderer
  {
    /**
     * Returns the Component that renders the TableCell.  Parameters are the same as those in the
     * getTableCellRendererComponent() method in interface TableCellRenderer.
     *
     * @param table JTable
     * @param value Object
     * @param isSelected boolean
     * @param hasFocus boolean
     * @param row int
     * @param column int
     * @return Component
     */
    public synchronized Component getTableCellRendererComponent(JTable table,
        Object value, boolean isSelected, boolean hasFocus, int row, int column)
    {
      EventNode evn = (EventNode)value;
      JComponent cell = new JLabel();
      // the cell's contents
      String cellString = "";
      // The cell's alignment
      int alignment = SwingConstants.LEFT;
      switch(table.getColumnModel().getColumn(column).getModelIndex())
      {
        case 0:// set the time
          cellString = Long.toString(evn.getEvent().time);
          alignment  = SwingConstants.RIGHT;
          break;
        case 1:// set the type
          cellString = evn.getEvent().getTypeString();
          alignment  = SwingConstants.CENTER;
          break;
        case 2:// set the method
          cellString = evn.getEvent().method.toString();
          break;
        case 3:// set the object
          try
          {
            cellString = Controller.toString(evn.getEvent().ref);
          }
          catch(RemoteException e)
          {
            cellString = "RemoteException error: " +e.toString();
          }
          break;
        case 4:// set the continuation
          cellString = evn.getEvent().getContinuationString();
          break;
        case 5:// set the parent
          if(evn.getParent()!=null)
          {
            cell = cellUpButton;
            cellString = Long.toString(evn.getParent().getEvent().time);
          }
          break;
        case 6:// set the default text for the children
          if(evn.getNumChildren()>0)
          {
            cell = cellDownButton;
            cellString = "Num children = " + evn.getNumChildren();
          }
          else
          {
            cellString = "Num children = 0";
            alignment  = SwingConstants.CENTER;
          }
          break;
        default:
          throw new RuntimeException("should not reach here");
      } // end switch

      if (cell instanceof JLabel)
      {
        JLabel a = new JLabel(cellString);
        a.setHorizontalAlignment(alignment);
        a.setFont(tableFont);
        cell = a;
      }
      else if (cell instanceof JButton)
      {
        ((JButton)cell).setText(cellString);
      }
      else
      {
        throw new RuntimeException("invalid cell component type.");
      }
      cell.setOpaque(true);
      cell.setBackground(isSelected ? Color.CYAN :
      row % 2 == 0 ? Color.WHITE : Color.LIGHT_GRAY);

      return cell;
    }

  }

  //////////////////////////////////////////////////
  // ChildrenCellEditor
  //

  /**
   * ChildrenCellEditor is a customized TableCellEditor for JiST Events.
   */
  public static class ChildrenCellEditor extends AbstractCellEditor implements TableCellEditor, ActionListener
  {
    /**
     * The table that the ChildrenCellEditor edits.
     */
    private JTable table;

    /**
     * The table's data model.
     */
    private EventTableModel model;

    /**
     * Hashtable for storing children's location in the table.
     */
    private Hashtable ht;

    /**
     * The ChildrenCellEditorConstructor.
     *
     * @param t JTable The table that the ChildrenCellEditor edits.
     * @param m EventTableModel The table's data model.
     */
    public ChildrenCellEditor(JTable t, EventTableModel m)
    {
      this.table = t;
      this.model = m;
      this.ht = new Hashtable();
    }

    /**
     * Returns the Component that edits the TableCell.  Parameters are the same as those in the
     * getTableCellEditorComponent() method in interface TableCellEditor.
     *
     * @param table JTable
     * @param value Object
     * @param isSelected boolean
     * @param row int
     * @param column int
     * @return Component
     */
    public Component getTableCellEditorComponent(JTable table,
        Object value, boolean isSelected, int row, int column)
    {
      JComboBox comboBox = new JComboBox();
      Component returnedComponent = null;
      comboBox.putClientProperty("JComboBox.isTableCellEditor", Boolean.TRUE);
      EventNode evn = (EventNode) value;
      int numChildren = evn.getNumChildren();
      // if there are children, add them to the comboBox
      if(numChildren>0)
      {
        for (int i = 0; i < numChildren; i++)
        {
          // store the child event's time and location in the table into a hashtable
          String key = Long.toString(evn.getChild(i).getEvent().time);
          String hashvalue = Integer.toString(model.getChildIndex(row, i));
          ht.put(key, hashvalue);
          // add the child event to the comboBox
          comboBox.addItem(Long.toString(evn.getChild(i).getEvent().time));
        }
        comboBox.setFont(tableFont);
        returnedComponent = comboBox;
      }
      else if(numChildren<1)
      {
        // set strings
        String cellString = "Num children = 0";
        // create the JLabel
        JLabel cell = new JLabel(cellString);
        cell.setHorizontalAlignment(SwingConstants.CENTER);
        cell.setOpaque(true);
        returnedComponent = cell;
      }
      comboBox.addActionListener(this);
      returnedComponent.setFont(tableFont);

      return returnedComponent;
    }

    /**
     * Overloaded to satisfy the CellEditor interface.
     * @return Object
     */
    public Object getCellEditorValue()
    {
      return table;
    }

    /**
     * Defines how JComboBox selections on the Children column are handled.
     * @param e ActionEvent
     */
    public void actionPerformed(ActionEvent e)
    {
      int jumpRow = -1;
      try
      {
        JComboBox clickedComboBox = (JComboBox)e.getSource();
        String selectedItem = (String)clickedComboBox.getSelectedItem();
        if(!selectedItem.equals("no children"))
        {
          jumpRow = Integer.parseInt((String) ht.get(selectedItem));
        }
      }
      catch(NullPointerException f)
      {
        System.out.println("no children");
      }
      int rowHeight = table.getRowHeight();
      int x      = 0;                   // x-coordinate of top-left corner of new rectangle view
      int y      = rowHeight * jumpRow; // y-coordinate of top-left corner of new rectangle view
      int width  = (int)table.getPreferredScrollableViewportSize().getWidth();  // width of new rectangle view
      int height = (int)table.getPreferredScrollableViewportSize().getHeight();  // height of new rectangle view

      if(jumpRow!=-1)
      {
        table.scrollRectToVisible(new Rectangle(x,y,width,height));
        // highlight the selected row
        table.setRowSelectionInterval(jumpRow,jumpRow);
        table.setSelectionBackground(Color.CYAN);
        cancelCellEditing(); // stop editing the cell.  turns off the JComboBox.
      }
    }
  }

  //////////////////////////////////////////////////
  // Button handler
  //

  /**
   * Defines how mouse clicks on the buttons are handled.
   */
  public static class ButtonHandler implements ActionListener
  {
    /**
     * Decides what action to perform depending on which button is pressed.
     *
     * @param e ActionEvent The event of clicking on the button.
     */
    public void actionPerformed(ActionEvent e)
    {
      JButton button = (JButton)e.getSource();
      JButton stepButton = GuiLog.getStepButton();

      if("Pause".equals(e.getActionCommand()))
      {
        // Change the Pause button to the Resume button.
        button.setText("Resume");
        button.setIcon(resumeIcon);
        GuiLog.pause();
        // Enable the Step and Until button.
        stepButton.setEnabled(true);
        untilButton.setEnabled(true);

      }
      else if("Resume".equals(e.getActionCommand()))
      {
        // Change the Resume button to the Pause button.
        button.setText("Pause");
        button.setIcon(pauseIcon);
        GuiLog.resume();
        // Disable the Step and Until button.
        stepButton.setEnabled(false);
        untilButton.setEnabled(false);
      }
      else if("Step".equals(e.getActionCommand()))
      {
        // Step through one event.
        GuiLog.stepEvent();
      }
      else if("Until".equals(e.getActionCommand()))
      {
        // Start the GuiLog's timer
        GuiLog.untilTimer();
      }
    }
  }

  //////////////////////////////////////////////////
  // Until Task
  //

  /**
   * UntilTask is a task that can be scheduled to either Pause or Resume the GuiLog.
   */
  public static class UntilTask extends TimerTask
  {
    /**
     * The GuiLog method to be called.
     */
    private String command;

    /**
     * The constructor initializes command.
     *
     * @param c String The GuiLog method.
     */
    public UntilTask(String c)
    {
      command = c;
    }

    /**
     * Call the appropriate GuiLog method according to command.
     */
    public void run()
    {
      if(command.equals("Pause"))
      {
        GuiLog.pause();
      }
      else if(command.equals("Resume"))
      {
        GuiLog.resume();
      }
      else
      {
        System.out.println("Should not reach here.");
      }
    }
  }

  //////////////////////////////////////////////////
  // Event node
  //

  /**
   * An EventNode encapsulates the construct formed by an Event and the relationships
   * it has with its parent and children Events.  EventNodes strung together form a
   * one-to-many tree structure.
   */
  public static class EventNode
  {
    /**
     * The node's Event.
     */
    private Event ev;

    /**
     * The node's and Event's parent EventNode.
     */
    private EventNode parent;

    /**
     * The node's and Event's child EventNode(s).
     */
    private Vector children;

    /**
     * The constructor for the EventNode creates the relationships of Parent and Children
     * between the Event and its Parent.
     *
     * @param ev Event The node's Event.
     * @param parent EventNode The node's parent EventNode.
     */
    public EventNode(Event ev, EventNode parent)
    {
      this.ev = ev;
      this.parent = parent;
      this.children = new Vector();
      if(parent!=null) parent.addChild(this);
    }

    /**
     * Returns the node's Event.
     *
     * @return Event
     */
    public Event getEvent()
    {
      return ev;
    }

    /**
     * Returns the node's Parent.
     *
     * @return EventNode
     */
    public EventNode getParent()
    {
      return parent;
    }

    /**
     * Returns the node's number of Children.
     *
     * @return int
     */
    public int getNumChildren()
    {
      return children.size();
    }

    /**
     * Returns a node's specific Child.
     *
     * @param i int The desired index for the Child.
     * @return EventNode The Child at the desired index.
     */
    public EventNode getChild(int i)
    {
      return (EventNode)children.elementAt(i);
    }

    /**
     * Adds a child EventNode to the node.
     *
     * @param child EventNode The child EventNode to add.
     */
    public void addChild(EventNode child)
    {
      children.add(child);
      child.parent=this;
    }

    /**
     * Remove a child EventNode from the node.
     *
     * @param child EventNode The child EventNode to remove.
     */
    public void removeChild(EventNode child)
    {
      assert(child.parent==this);
      children.remove(child);
      child.parent = null;
    }

    /**
     * Removes all child EventNodes from the node.
     */
    public void removeChildren()
    {
      while(getNumChildren()>0)
      {
        removeChild(getChild(0));
      }
    }
  }

  //////////////////////////////////////////////////
  // EventComparator
  //

  /**
   * Used for sorting Events.
   */
  public static final EventNodeComparator EVENT_COMPARE = new EventNodeComparator();

  /**
   * The EventNodeComparator class defines how to sort two Events.
   */
  public static class EventNodeComparator implements Comparator
  {
    /**
     * Compares o1 to o2 and returns a negative number, 0, or a positive number if the
     * o1 is less than, equal to, or greater than o2.
     *
     * @param o1 Object
     * @param o2 Object
     * @return int
     */
    public int compare(Object o1, Object o2)
    {
      if (o1 == null)return 1;
      if (o2 == null)return -1;
      return Util.sign(((EventNode) o1).getEvent().time - ((EventNode) o2).getEvent().time);
    }
  }

  //////////////////////////////////////////////////
  // EventTableModel
  //

  /**
   * The EventTableModel class is the data model for the JTable.
   */
  public static class EventTableModel extends AbstractTableModel
  {
    /**
     * Array containing the Events.
     */
    private EventNode[] events;

    /**
     * Number of Events.
     */
    private int numEvents;

    /**
     * The EventTableModel's constructor initializes the two members.
     */
    public EventTableModel()
    {
      this.events = new EventNode[10];
      numEvents = 0;
    }

    //////////////////////////////////////////////////
    // model updates
    //

    /**
     * Given an Event, returns the corresponding EventNode in the events[] array.
     *
     * @param ev Event The Event being located.
     * @return EventNode The desired EventNode containing the Event.
     */
    public EventNode findNode(Event ev)
    {
      int i = findNodeIndex(ev);
      return i==-1 ? null : events[i];
    }

    /**
     * Given an Event, returns the index of the events[] array where that Event is located.
     *
     * @param ev Event The desired Event.
     * @return int The index of the events[] array where the desired Event is located.
     */
    public int findNodeIndex(Event ev)
    {
      for(int i=0; i<numEvents; i++)
      {
        if(((EventNode)events[i]).getEvent().equals(ev)) return i;
      }
      return -1;
    }

    /**
     * Given an EventNode's index, returns the Parent EventNode's index in the events[] array.
     * This is also equivalent to the table row number.
     *
     * @param index int The index of the EventNode.
     * @return int The index of the EventNode's Parent EventNode.
     */
    public int getParentIndex(int index)
    {
      if(events[index].getParent()!=null)
      {
        return findNodeIndex(events[index].getParent().getEvent());
      }
      return -1;
    }

    /**
     * Given an EventNode's index and a Child number, returns the specified Child EventNode's index in the events[] array.
     * This is also equivalent to the table row number.
     *
     * @param index int The index of the EventNode.
     * @param childNum int The number of the desired Child EventNode.
     * @return int The specified Child EventNode's index in the events[] array.
     */
    public int getChildIndex(int index, int childNum)
    {
      if(events[index].getNumChildren()>childNum)
      {
        return findNodeIndex(events[index].getChild(childNum).getEvent());
      }
      return -1;
    }

    /**
     * Add an event to the table's data model.
     *
     * @param ev Event The Event that is being added.
     * @param parent Event The Event's Parent Event.
     */
    public void add(Event ev, Event parent)
    {
      // Create the wrapper for the two inputs.
      EventNode evn = new EventNode(ev, findNode(parent));
      ensureCapacity();
      // Add the Event.
      events[numEvents] = evn;
      // Sort the events array.
      Arrays.sort(events, EVENT_COMPARE);
      numEvents++;
      // Notifty the GUI that the table has changed.
      fireTableRowsInserted(numEvents-1,numEvents);
    }

    /**
     * Delete an event from the table's data model.
     *
     * @param ev Event The Event that is being deleted.
     */
    public void del(Event ev)
    {
      int i = findNodeIndex(ev);
      if(i!=-1)
      {
        // Delete the Event and its Children.
        events[i].removeChildren();
        events[i]=null;
        Arrays.sort(events, EVENT_COMPARE);
        numEvents--;
        // Notify the GUI that hte table has changed.
        fireTableRowsUpdated(numEvents,numEvents);
      }
    }


    /**
     * If events[] is too small, increase the size.
     */
    private void ensureCapacity()
    {
      if(numEvents==events.length)
      {
        EventNode[] events2 = new EventNode[events.length*2];
        System.arraycopy(events, 0, events2, 0, events.length);
        events = events2;
      }
    }

    //////////////////////////////////////////////////
    // TableModel methods
    //

    /**
     * Returns the number of rows in the table.
     * Overrided to comply with AbstractTableModel Class.
     *
     * @return int
     */
    public int getRowCount()
    {
      return numEvents;
    }

    /**
     * Returns the number of columns in the table.
     * Overrided to comply with AbstractTableModel Class.
     *
     * @return int
     */
    public int getColumnCount()
    {
      return 7;
    }

    /**
     * Returns the EventNode corresponding to the requested row.
     * This method is called internally by the TableCellRenderer Class.
     * Overrided to comply with AbstractTableModel Class.
     *
     * @param row int
     * @param col int
     * @return Object The EventNode corresponding to the requested row.
     */
    public Object getValueAt(int row, int col)
    {
      return events[row];
    }

    /**
     * Returns whether the cell at (row,col) is editable or not.
     * Overrided to comply with AbstractTableModel Class.
     *
     * @param row int
     * @param col int
     * @return boolean
     */
    public boolean isCellEditable(int row, int col)
    {
      // Only column 6 can be edited, which is done by the ChildrenCellEditor.
      if(col!=6)
      {
        return false;
      }
      return true;
    }
  } // end EventTableModel

  //////////////////////////////////////////////////
  // GuiLog continued
  //

  /**
   * Adds an Event to the GUI.
   *
   * @param id Event
   * @param parent Event
   */
  public void add(Event id, Event parent)
  {
    // If the GUI is paused, wait.
    checkLock();
    checkListSize();

    // Create a copy of id and parent.  This is done just in case methods that call add() reuse the same address
    // for its Event id and Event parent.  Also, must check if either is null, else copy constructor will throw exception.
    if(id!=null)
    {
      Event idCopy = new Event(id);
      id = idCopy;
    }
    if(parent!=null)
    {
      Event parentCopy = new Event(parent);
      parent = parentCopy;
    }

    // add the event and its parent to the model and list.
    model.add(id, parent);
    list.add(id);
    panel.repaint();
  }

  /**
   * Deletes an Event from the GUI.
   *
   * @param id Event
   */
  public void del(Event id)
  {
    // Delete the Event from the data model.
    model.del(id);
    // If event is on the list, delete it.  if it is not, print error msg.
    if(!this.list.remove(id))
    {
      System.out.println("Event not deleted: " + id.toString());
    }
    panel.repaint();
  }

  /**
   * If not stepping through one Event, checkLock attempts to acquire a lock on pauseLock.  If it is successful,
   * the method returns; otherwise, it waits.
   * If stepping through one Event, then checkLock returns once so that one Event can be added.
   */
  public void checkLock()
  {
    synchronized(pauselock)
    {
      // If not stepping one Event, proceed as normally and attempt to acquire a lock
      if(!isStep)
      {
        while(paused)
        {
          try
          {
            pauselock.wait();
          }
          catch(InterruptedException exception)
          {
            System.out.println("InterruptedException: " + exception.toString());
          }
        }
      }
      else
      {
        isStep = false;
        try
        {
          pauselock.wait();
        }
        catch(InterruptedException exception)
        {
          System.out.println("InterruptedException: " + exception.toString());
        }
      }
    }
  }

  /**
   * Step through one Event.
   */
  public static void stepEvent()
  {
    // Set the booleans below so that checkLock() returns once.
    paused = false;
    isStep = true;
    synchronized(pauselock)
    {
      pauselock.notify();
    }
  }

  /**
   * Prompts the user for the number of seconds to unpause the GuiLog, then unpauses it.  After the user-entered time,
   * pauses the GuiLog.
   */
  public static void untilTimer()
  {
    // Prompt the user for the number of seconds to run the simulation.
    int numSeconds   = Integer.parseInt((String)JOptionPane.showInputDialog(frame, "Enter number of seconds to run simulation:"));

    // Timers for unpausing and pausing the GuiLog
    Timer timerStart = new Timer();
    Timer timerEnd   = new Timer();

    // Schedule a task to Resume the GuiLog immediately
    timerStart.schedule(new UntilTask("Resume"),0);
    // Schedule a task to Pause the GuiLog numSeconds later
    timerEnd.schedule(new UntilTask("Pause"),numSeconds*1000);
  }

  /**
   * Checks the number of Events in the list.  If the thresh hold limit has been reached, one event is deleted.
   */
  public void checkListSize()
  {
    // If numEventsThreshHold has been reached, delete one event.  This creates a smooth moving window of viewable Events.
    if(list.size()>=this.numEventsThreshHold)
    {
      this.del((Event)this.list.getFirst());
    }
  }

  /**
   * Sets paused to true.
   */
  public static void pause()
  {
    paused = true;
  }

  /**
   * Sets paused to false and notifies all methods that are synchronized on pauseLock.
   */
  public static void resume()
  {
    paused = false;
    synchronized(pauselock)
    {
      pauselock.notify();
    }
  }

  /**
   * Dummy method for testing.  Used in GuiLog's main method.
   */
  private static final Method m;
  static
  {
    try
    {
      m = GuiLog.class.getDeclaredMethod("main",
          new Class[] { String[].class });
    }
    catch(NoSuchMethodException e)
    {
      throw new RuntimeException("should never happen", e);
    }
  }

  /**
   * Test program.  Creates a GUI with a JTable.  Creates Events, then adds and deletes
   * them from the GuiLog.
   *
   * @param args String[]
   */
  public static void main(String[] args)
  {
    Event A = new Event(1, m, null, null);
    Event B = new Event(2, m, null, null);
    Event C = new Event(3, m, null, null);
    Event D = new Event(4, m, null, null);
    Event E = new Event(5, m, null, null);
    Event F = new Event(6, m, null, null);

    Event G = new Event(7, m, null, null);
    Event H = new Event(8, m, null, null);
    Event I = new Event(9, m, null, null);
    Event J = new Event(10, m, null, null);
    Event K = new Event(11, m, null, null);
    Event L = new Event(12, m, null, null);
    Event M = new Event(130000000000L, m, null, null);

    Event N = new Event(140000000000L, m, null, null);
    Event O = new Event(150000000000L, m, null, null);
    Event P = new Event(160000000000L, m, null, null);
    Event Q = new Event(170000000000L, m, null, null);
    Event R = new Event(180000000000L, m, null, null);
    Event S = new Event(190000000000L, m, null, null);


    Event XX = new Event(60, m, null, null);  // this event is created but not added

    GuiLog gl = Controller.guilog;
    if(gl==null) gl = new GuiLog(50);
    try
    {
      int sleepTime = 500; //miliseconds to wait between additions of Events.

      gl.add(A, null); // 1 has no parent
      Thread.sleep(sleepTime);
      gl.add(B, A); // 2 's parent is 1
      Thread.sleep(sleepTime);
      gl.add(C, A); // 3's parent is 1
      Thread.sleep(sleepTime);
      gl.add(D, C); // 4's parent is 3
      Thread.sleep(sleepTime);
      Thread.sleep(sleepTime);
      gl.add(E, C); // 5's parent is 3
      Thread.sleep(sleepTime);
      gl.add(F, A); // 6's parent is 1
      Thread.sleep(sleepTime);

      gl.add(G, E); // 7's parent is 5
      Thread.sleep(sleepTime);
      gl.add(H, G); // 8's parent is 7
      Thread.sleep(sleepTime);
      gl.add(I, G); // 9's parent is 7
      Thread.sleep(sleepTime);
      Thread.sleep(sleepTime);
      gl.add(J, I); // 10's parent is 9
      Thread.sleep(sleepTime);
      gl.add(K, F); // 11's parent is 6
      Thread.sleep(sleepTime);
      gl.add(L, A); // 12's parent is 1
      Thread.sleep(sleepTime);
      gl.add(M, G); // 13's parent is 7
      Thread.sleep(sleepTime);
      gl.add(N, I); // 14's parent is 9
      Thread.sleep(sleepTime);
      gl.add(O, I); // 15's parent is 9
      Thread.sleep(sleepTime);
      gl.add(P, I); // 16's parent is 9
      Thread.sleep(sleepTime);
      gl.add(Q, I); // 17's parent is 9
      Thread.sleep(sleepTime);
      gl.add(R, I); // 18's parent is 9
      Thread.sleep(sleepTime);
      gl.add(S, R); // 19's parent is 18
      Thread.sleep(sleepTime);
      gl.del(XX);
    }
    catch(InterruptedException e)
    {
      System.out.println("InterruptedException: " +e.toString());
    }
  }
}
