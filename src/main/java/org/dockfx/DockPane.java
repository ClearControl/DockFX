/**
 * @file DockPane.java
 * @brief Class implementing a generic dock pane for the layout of dock nodes.
 *
 * @section License
 *
 *          This file is a part of the DockFX Library. Copyright (C) 2015 Robert B. Colton
 *
 *          This program is free software: you can redistribute it and/or modify it under the terms
 *          of the GNU Lesser General Public License as published by the Free Software Foundation,
 *          either version 3 of the License, or (at your option) any later version.
 *
 *          This program is distributed in the hope that it will be useful, but WITHOUT ANY
 *          WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *          PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 *          You should have received a copy of the GNU Lesser General Public License along with this
 *          program. If not, see <http://www.gnu.org/licenses/>.
 **/

package org.dockfx;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.css.PseudoClass;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.stage.Popup;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;

import com.sun.javafx.css.StyleManager;

import org.dockfx.pane.ContentPane;
import org.dockfx.pane.ContentSplitPane;
import org.dockfx.pane.ContentTabPane;
import org.dockfx.pane.DockNodeTab;

/**
 * Base class for a dock pane that provides the layout of the dock nodes.
 * Stacking the dock nodes to the center in a TabPane will be added in a future
 * release. For now the DockPane uses the relative sizes of the dock nodes and
 * lays them out in a tree of SplitPanes.
 *
 * @since DockFX 0.1
 */
public class DockPane extends StackPane
                      implements EventHandler<DockEvent>
{

  /**
   * The current root node of this dock pane's layout.
   */
  private Node root;

  /**
   * Whether or not this dock pane allows the docking of dock nodes from
   * external sources (i.e., other dock panes).
   */
  private boolean exclusive = false;

  /**
   * Whether a DOCK_ENTER event has been received by this dock pane since the
   * last DOCK_EXIT event was received.
   */
  private boolean receivedEnter = false;

  /**
   * The current node in this dock pane that we may be dragging over.
   */
  private Node dockNodeDrag;
  /**
   * The docking area of the current dock indicator button if any is selected.
   * This is either the root or equal to dock node drag.
   */
  private Node dockAreaDrag;
  /**
   * The docking position of the current dock indicator button if any is
   * selected.
   */
  private DockPos dockPosDrag;

  /**
   * The docking area shape with a dotted animated border on the indicator
   * overlay popup.
   */
  private Rectangle dockAreaIndicator;
  /**
   * The timeline used to animate the borer of the docking area indicator shape.
   * Because JavaFX has no CSS styling for timelines/animations yet we will make
   * this private and offer an accessor for the user to programmatically modify
   * the animation or disable it.
   */
  private Timeline dockAreaStrokeTimeline;
  /**
   * The popup used to display the root dock indicator buttons and the docking
   * area indicator.
   */
  private Popup dockIndicatorOverlay;

  /**
   * The grid pane used to lay out the local dock indicator buttons. This is the
   * grid used to lay out the buttons in the circular indicator.
   */
  private GridPane dockPosIndicator;
  /**
   * The popup used to display the local dock indicator buttons. This allows
   * these indicator buttons to be displayed outside the window of this dock
   * pane.
   */
  private Popup dockIndicatorPopup;

  /**
   * Base class for a dock indicator button that allows it to be displayed
   * during a dock event and continue to receive input.
   *
   * @since DockFX 0.1
   */
  public class DockPosButton extends Button
  {

    /**
     * Whether this dock indicator button is used for docking a node relative to
     * the root of the dock pane.
     */
    private boolean dockRoot = true;
    /**
     * The docking position indicated by this button.
     */
    private DockPos dockPos = DockPos.CENTER;

    /**
     * Creates a new dock indicator button.
     */
    public DockPosButton(boolean dockRoot, DockPos dockPos)
    {
      super();
      this.dockRoot = dockRoot;
      this.dockPos = dockPos;
    }

    /**
     * Whether this dock indicator button is used for docking a node relative to
     * the root of the dock pane.
     *
     * @param dockRoot
     *          Whether this indicator button is used for docking a node
     *          relative to the root of the dock pane.
     */
    public final void setDockRoot(boolean dockRoot)
    {
      this.dockRoot = dockRoot;
    }

    /**
     * The docking position indicated by this button.
     *
     * @param dockPos
     *          The docking position indicated by this button.
     */
    public final void setDockPos(DockPos dockPos)
    {
      this.dockPos = dockPos;
    }

    /**
     * The docking position indicated by this button.
     *
     * @return The docking position indicated by this button.
     */
    public final DockPos getDockPos()
    {
      return dockPos;
    }

    /**
     * Whether this dock indicator button is used for docking a node relative to
     * the root of the dock pane.
     *
     * @return Whether this indicator button is used for docking a node relative
     *         to the root of the dock pane.
     */
    public final boolean isDockRoot()
    {
      return dockRoot;
    }
  }

  /**
   * A collection used to manage the indicator buttons and automate hit
   * detection during DOCK_OVER events.
   */
  private ObservableList<DockPosButton> dockPosButtons;

  private ObservableList<DockNode> undockedNodes;

  /**
   * Creates a new DockPane adding event handlers for dock events and creating
   * the indicator overlays.
   */
  public DockPane()
  {
    super();

    this.addEventHandler(DockEvent.ANY, this);
    this.addEventFilter(DockEvent.ANY, new EventHandler<DockEvent>()
    {

      @Override
      public void handle(DockEvent event)
      {

        if (event.getEventType() == DockEvent.DOCK_ENTER)
        {
          DockPane.this.receivedEnter = true;
        }
        else if (event.getEventType() == DockEvent.DOCK_OVER)
        {
          DockPane.this.dockNodeDrag = null;
        }
      }

    });

    dockIndicatorPopup = new Popup();
    dockIndicatorPopup.setAutoFix(false);

    dockIndicatorOverlay = new Popup();
    dockIndicatorOverlay.setAutoFix(false);

    StackPane dockRootPane = new StackPane();
    dockRootPane.prefWidthProperty().bind(this.widthProperty());
    dockRootPane.prefHeightProperty().bind(this.heightProperty());

    dockAreaIndicator = new Rectangle();
    dockAreaIndicator.setManaged(false);
    dockAreaIndicator.setMouseTransparent(true);

    dockAreaStrokeTimeline = new Timeline();
    dockAreaStrokeTimeline.setCycleCount(Timeline.INDEFINITE);
    // 12 is the cumulative offset of the stroke dash array in the default.css
    // style sheet
    // RFE filed for CSS styled timelines/animations:
    // https://bugs.openjdk.java.net/browse/JDK-8133837
    KeyValue kv =
                new KeyValue(dockAreaIndicator.strokeDashOffsetProperty(),
                             12);
    KeyFrame kf = new KeyFrame(Duration.millis(500), kv);
    dockAreaStrokeTimeline.getKeyFrames().add(kf);
    dockAreaStrokeTimeline.play();

    DockPosButton dockCenter =
                             new DockPosButton(false, DockPos.CENTER);
    dockCenter.getStyleClass().add("dock-center");

    DockPosButton dockTop = new DockPosButton(false, DockPos.TOP);
    dockTop.getStyleClass().add("dock-top");
    DockPosButton dockRight = new DockPosButton(false, DockPos.RIGHT);
    dockRight.getStyleClass().add("dock-right");
    DockPosButton dockBottom =
                             new DockPosButton(false, DockPos.BOTTOM);
    dockBottom.getStyleClass().add("dock-bottom");
    DockPosButton dockLeft = new DockPosButton(false, DockPos.LEFT);
    dockLeft.getStyleClass().add("dock-left");

    DockPosButton dockTopRoot = new DockPosButton(true, DockPos.TOP);
    StackPane.setAlignment(dockTopRoot, Pos.TOP_CENTER);
    dockTopRoot.getStyleClass().add("dock-top-root");

    DockPosButton dockRightRoot = new DockPosButton(true,
                                                    DockPos.RIGHT);
    StackPane.setAlignment(dockRightRoot, Pos.CENTER_RIGHT);
    dockRightRoot.getStyleClass().add("dock-right-root");

    DockPosButton dockBottomRoot = new DockPosButton(true,
                                                     DockPos.BOTTOM);
    StackPane.setAlignment(dockBottomRoot, Pos.BOTTOM_CENTER);
    dockBottomRoot.getStyleClass().add("dock-bottom-root");

    DockPosButton dockLeftRoot =
                               new DockPosButton(true, DockPos.LEFT);
    StackPane.setAlignment(dockLeftRoot, Pos.CENTER_LEFT);
    dockLeftRoot.getStyleClass().add("dock-left-root");

    // TODO: dockCenter goes first when tabs are added in a future version
    dockPosButtons = FXCollections.observableArrayList(dockCenter,
                                                       dockTop,
                                                       dockRight,
                                                       dockBottom,
                                                       dockLeft,
                                                       dockTopRoot,
                                                       dockRightRoot,
                                                       dockBottomRoot,
                                                       dockLeftRoot);

    dockPosIndicator = new GridPane();
    dockPosIndicator.add(dockTop, 1, 0);
    dockPosIndicator.add(dockRight, 2, 1);
    dockPosIndicator.add(dockBottom, 1, 2);
    dockPosIndicator.add(dockLeft, 0, 1);
    dockPosIndicator.add(dockCenter, 1, 1);

    dockRootPane.getChildren().addAll(dockAreaIndicator,
                                      dockTopRoot,
                                      dockRightRoot,
                                      dockBottomRoot,
                                      dockLeftRoot);

    dockIndicatorOverlay.getContent().add(dockRootPane);
    dockIndicatorPopup.getContent().addAll(dockPosIndicator);

    this.getStyleClass().add("dock-pane");
    dockRootPane.getStyleClass().add("dock-root-pane");
    dockPosIndicator.getStyleClass().add("dock-pos-indicator");
    dockAreaIndicator.getStyleClass().add("dock-area-indicator");

    undockedNodes = FXCollections.observableArrayList();
  }

  /**
   * Indicates whether or not the dock pane is in exclusive mode (the default).
   * In exclusive mode, the dock pane ignores dock nodes dragged from other dock
   * panes, and dock nodes dragged from this dock pane are ignored by other dock
   * panes.
   * 
   * @return true or false.
   */
  public boolean isExclusive()
  {
    return exclusive;
  }

  /**
   * Enables/disables exclusive mode.
   * 
   * @param exclusive
   *          true for exclusive mode, and false otherwise.
   */
  public void setExclusive(boolean exclusive)
  {
    this.exclusive = exclusive;
  }

  /**
   * The Timeline used to animate the docking area indicator in the dock
   * indicator overlay for this dock pane.
   *
   * @return The Timeline used to animate the docking area indicator in the dock
   *         indicator overlay for this dock pane.
   */
  public final Timeline getDockAreaStrokeTimeline()
  {
    return dockAreaStrokeTimeline;
  }

  /**
   * Helper function to retrieve the URL of the default style sheet used by
   * DockFX.
   *
   * @return The URL of the default style sheet used by DockFX.
   */
  public final static String getDefaultUserAgentStylesheet()
  {
    return DockPane.class.getResource("default.css").toExternalForm();
  }

  /**
   * Helper function to add the default style sheet of DockFX to the user agent
   * style sheets.
   */
  public final static void initializeDefaultUserAgentStylesheet()
  {
    StyleManager.getInstance()
                .addUserAgentStylesheet(DockPane.class.getResource("default.css")
                                                      .toExternalForm());
  }

  /**
   * A cache of all dock node event handlers that we have created for tracking
   * the current docking area.
   */
  private ObservableMap<Node, DockNodeEventHandler> dockNodeEventFilters =
                                                                         FXCollections.observableHashMap();

  /**
   * A wrapper to the type parameterized generic EventHandler that allows us to
   * remove it from its listener when the dock node becomes detached. It is
   * specifically used to monitor which dock node in this dock pane's layout we
   * are currently dragging over.
   *
   * @since DockFX 0.1
   */
  private class DockNodeEventHandler implements
                                     EventHandler<DockEvent>
  {

    /**
     * The node associated with this event handler that reports to the
     * encapsulating dock pane.
     */
    private Node node = null;

    /**
     * Creates a default dock node event handler that will help this dock pane
     * track the current docking area.
     *
     * @param node
     *          The node that is to listen for docking events and report to the
     *          encapsulating docking pane.
     */
    public DockNodeEventHandler(Node node)
    {
      this.node = node;
    }

    @Override
    public void handle(DockEvent event)
    {
      DockPane.this.dockNodeDrag = node;
    }
  }

  /**
   * Dock the node into this dock pane at the given docking position relative to
   * the sibling in the layout. This is used to relatively position the dock
   * nodes to other nodes given their preferred size.
   *
   * @param node
   *          The node that is to be docked into this dock pane.
   * @param dockPos
   *          The docking position of the node relative to the sibling.
   * @param sibling
   *          The sibling of this node in the layout.
   */
  void dock(Node node, DockPos dockPos, Node sibling)
  {
    DockNodeEventHandler dockNodeEventHandler =
                                              new DockNodeEventHandler(node);
    dockNodeEventFilters.put(node, dockNodeEventHandler);
    node.addEventFilter(DockEvent.DOCK_OVER, dockNodeEventHandler);

    ContentPane pane = (ContentPane) root;
    if (pane == null)
    {
      pane = new ContentSplitPane(node);
      root = (Node) pane;
      this.getChildren().add(root);
      return;
    }

    if (sibling != null && sibling != root)
    {
      Stack<Parent> stack = new Stack<>();
      stack.push((Parent) root);
      pane = pane.getSiblingParent(stack, sibling);
    }

    if (pane == null)
    {
      sibling = root;
      dockPos = DockPos.RIGHT;
      pane = (ContentPane) root;
    }

    if (dockPos == DockPos.CENTER)
    {
      if (sibling instanceof DockNode)
      {
        if (pane instanceof ContentSplitPane)
        {
          // Create a ContentTabPane with two nodes
          DockNode siblingNode = (DockNode) sibling;
          DockNode newNode = (DockNode) node;

          ContentTabPane tabPane = new ContentTabPane();

          tabPane.addDockNodeTab(new DockNodeTab(siblingNode));
          tabPane.addDockNodeTab(new DockNodeTab(newNode));

          tabPane.setContentParent(pane);

          double[] pos =
                       ((ContentSplitPane) pane).getDividerPositions();
          pane.set(sibling, tabPane);
          ((ContentSplitPane) pane).setDividerPositions(pos);
        }
      }
      else
      {
        ContentSplitPane siblingSplitPane =
                                          (ContentSplitPane) sibling;

        ContentPane parent = siblingSplitPane.getContentParent();
        if (parent == null)
        {
          Node child = siblingSplitPane.getChildrenList().get(0);
          if (child instanceof DockNode)
          {
            // If we are docking into a DockNode, make a ContentTabPane of the
            // two
            ContentTabPane tabPane = new ContentTabPane();
            tabPane.addNode(root, null, child, DockPos.CENTER);
            siblingSplitPane.set(child, tabPane);
            tabPane.setContentParent(siblingSplitPane);
            pane = tabPane;
            sibling = null;
          }
          else if (child instanceof ContentSplitPane)
          {
            // TODO: Recursively reorder the panes instead of throwing it in?
            pane = (ContentSplitPane) child;
            dockPos = DockPos.LEFT;
          }
          else if (child instanceof ContentTabPane)
          {
            // If we already have a tab pane, just add the node to it
            pane = (ContentTabPane) child;
          }
        }
      }
    }
    else
    {
      // Otherwise, SplitPane is assumed.
      Orientation requestedOrientation =
                                       (dockPos == DockPos.LEFT
                                        || dockPos == DockPos.RIGHT) ? Orientation.HORIZONTAL
                                                                     : Orientation.VERTICAL;

      if (pane instanceof ContentSplitPane)
      {
        ContentSplitPane split = (ContentSplitPane) pane;

        // if the orientation is different then reparent the split pane
        if (split.getOrientation() != requestedOrientation)
        {
          if (split.getItems().size() > 1)
          {
            ContentSplitPane splitPane = new ContentSplitPane();

            if (split == root && sibling == root)
            {
              this.getChildren().set(this.getChildren().indexOf(root),
                                     splitPane);
              splitPane.getItems().add(split);
              root = splitPane;
            }
            else
            {
              split.set(sibling, splitPane);
              splitPane.setContentParent(split);
              splitPane.getItems().add(sibling);
            }

            split = splitPane;
          }
          split.setOrientation(requestedOrientation);
          pane = split;
        }
      }
      else if (pane instanceof ContentTabPane)
      {

        if (pane.getContentParent() != null)
        {
          ContentSplitPane split =
                                 (ContentSplitPane) pane.getContentParent();

          // if the orientation is different then reparent the split pane
          if (split.getOrientation() != requestedOrientation)
          {
            ContentSplitPane splitPane = new ContentSplitPane();
            if (split == root && sibling == root)
            {
              this.getChildren().set(this.getChildren().indexOf(root),
                                     splitPane);
              splitPane.getItems().add(split);
              root = splitPane;
            }
            else
            {
              pane.setContentParent(splitPane);
              sibling = (Node) pane;
              split.set(sibling, splitPane);
              splitPane.setContentParent(split);
              splitPane.getItems().add(sibling);
            }
            split = splitPane;
          }
          else
          {
            sibling = (Node) pane;
          }

          split.setOrientation(requestedOrientation);
          pane = split;
        }
        else
        {
          ContentSplitPane split = new ContentSplitPane();

          pane.setContentParent(split);
          sibling = (Node) pane;
          split.getItems().add(sibling);

          split.setOrientation(requestedOrientation);
          pane = split;
        }
      }
    }

    // Add a node to the proper pane
    pane.addNode(root, sibling, node, dockPos);

    if (undockedNodes.contains(node))
    {
      undockedNodes.remove(node);
    }
  }

  /**
   * Dock the node into this dock pane at the given docking position relative to
   * the root in the layout. This is used to relatively position the dock nodes
   * to other nodes given their preferred size.
   *
   * @param node
   *          The node that is to be docked into this dock pane.
   * @param dockPos
   *          The docking position of the node relative to the sibling.
   */
  void dock(Node node, DockPos dockPos)
  {
    dock(node, dockPos, root);
  }

  /**
   * Detach the node from this dock pane removing it from the layout.
   *
   * @param node
   *          The node that is to be removed from this dock pane.
   */
  void undock(DockNode node)
  {
    if (!node.closedProperty().get())
      undockedNodes.add(node);

    DockNodeEventHandler dockNodeEventHandler =
                                              dockNodeEventFilters.get(node);
    node.removeEventFilter(DockEvent.DOCK_OVER, dockNodeEventHandler);
    dockNodeEventFilters.remove(node);

    // depth first search to find the parent of the node
    Stack<Parent> findStack = new Stack<Parent>();
    findStack.push((Parent) root);

    while (!findStack.isEmpty())
    {
      Parent parent = findStack.pop();

      if (parent instanceof ContentPane)
      {
        ContentPane pane = (ContentPane) parent;
        pane.removeNode(findStack, node);

        // if there is 0 children left, make sure we remove the split pane
        if (pane.getChildrenList().isEmpty())
        {
          if (root == pane)
          {
            this.getChildren().remove((Node) pane);
            root = null;
          }
        }
        else if (pane.getChildrenList().size() == 1
                 && pane instanceof ContentTabPane
                 && pane.getChildrenList().get(0) instanceof DockNode)
        {
          // if there is only 1-tab left, we replace it with the SplitPane

          List<Node> children = pane.getChildrenList();
          Node sibling = children.get(0);
          ContentPane contentParent = pane.getContentParent();

          contentParent.set((Node) pane, sibling);
          ((DockNode) sibling).tabbedProperty().setValue(false);
        }
      }
    }

  }

  public void removeFloatingNodeFromUndockNodes(DockNode n)
  {
    undockedNodes.remove(n);
  }

  @Override
  public void handle(DockEvent event)
  {
    // handle exclusive mode.
    DockPane otherPane =
                       ((DockNode) event.getContents()).getDockPane();

    if (otherPane != this)
    {
      if (isExclusive())
      {
        // Can't accept nodes from other dock panes.
        return;
      }

      if (otherPane != null && otherPane.isExclusive())
      {
        // Nodes from the other pane cannot be docked elsewhere.
        return;
      }
    }

    if (event.getEventType() == DockEvent.DOCK_ENTER)
    {
      if (!dockIndicatorOverlay.isShowing())
      {
        Point2D originToScreen;
        if (null != root)
        {
          originToScreen = root.localToScreen(0, 0);
        }
        else
        {
          originToScreen = this.localToScreen(0, 0);
        }

        dockIndicatorOverlay.show(DockPane.this,
                                  originToScreen.getX(),
                                  originToScreen.getY());
      }
    }
    else if (event.getEventType() == DockEvent.DOCK_OVER)
    {
      this.receivedEnter = false;

      dockPosDrag = null;
      dockAreaDrag = dockNodeDrag;

      for (DockPosButton dockIndicatorButton : dockPosButtons)
      {
        if (dockIndicatorButton.contains(dockIndicatorButton.screenToLocal(event.getScreenX(),
                                                                           event.getScreenY())))
        {
          dockPosDrag = dockIndicatorButton.getDockPos();
          if (dockIndicatorButton.isDockRoot())
          {
            dockAreaDrag = root;
          }
          dockIndicatorButton.pseudoClassStateChanged(PseudoClass.getPseudoClass("focused"),
                                                      true);
          break;
        }
        else
        {
          dockIndicatorButton.pseudoClassStateChanged(PseudoClass.getPseudoClass("focused"),
                                                      false);
        }
      }

      if (dockPosDrag != null && dockAreaDrag != null)
      {
        Point2D originToScene = dockAreaDrag.localToScreen(0, 0);

        dockAreaIndicator.setVisible(true);
        dockAreaIndicator.relocate(originToScene.getX()
                                   - dockIndicatorOverlay.getAnchorX(),
                                   originToScene.getY() - dockIndicatorOverlay.getAnchorY());
        if (dockPosDrag == DockPos.RIGHT)
        {
          dockAreaIndicator.setTranslateX(dockAreaDrag.getLayoutBounds()
                                                      .getWidth()
                                          / 2);
        }
        else
        {
          dockAreaIndicator.setTranslateX(0);
        }

        if (dockPosDrag == DockPos.BOTTOM)
        {
          dockAreaIndicator.setTranslateY(dockAreaDrag.getLayoutBounds()
                                                      .getHeight()
                                          / 2);
        }
        else
        {
          dockAreaIndicator.setTranslateY(0);
        }

        if (dockPosDrag == DockPos.LEFT
            || dockPosDrag == DockPos.RIGHT)
        {
          dockAreaIndicator.setWidth(dockAreaDrag.getLayoutBounds()
                                                 .getWidth()
                                     / 2);
        }
        else
        {
          dockAreaIndicator.setWidth(dockAreaDrag.getLayoutBounds()
                                                 .getWidth());
        }
        if (dockPosDrag == DockPos.TOP
            || dockPosDrag == DockPos.BOTTOM)
        {
          dockAreaIndicator.setHeight(dockAreaDrag.getLayoutBounds()
                                                  .getHeight()
                                      / 2);
        }
        else
        {
          dockAreaIndicator.setHeight(dockAreaDrag.getLayoutBounds()
                                                  .getHeight());
        }
      }
      else
      {
        dockAreaIndicator.setVisible(false);
      }

      if (dockNodeDrag != null
          && ((DockNode) dockNodeDrag).getDockTitleBar() != null)
      {
        Point2D originToScreen = dockNodeDrag.localToScreen(0, 0);

        double posX = originToScreen.getX()
                      + dockNodeDrag.getLayoutBounds().getWidth() / 2
                      - dockPosIndicator.getWidth() / 2;
        double posY = originToScreen.getY()
                      + dockNodeDrag.getLayoutBounds().getHeight() / 2
                      - dockPosIndicator.getHeight() / 2;

        if (!dockIndicatorPopup.isShowing())
        {
          dockIndicatorPopup.show(DockPane.this, posX, posY);
        }
        else
        {
          dockIndicatorPopup.setX(posX);
          dockIndicatorPopup.setY(posY);
        }

        // set visible after moving the popup
        dockPosIndicator.setVisible(true);
      }
      else
      {
        dockPosIndicator.setVisible(false);
      }
    }

    if (event.getEventType() == DockEvent.DOCK_RELEASED
        && event.getContents() != null)
    {
      if (dockPosDrag != null && dockIndicatorOverlay.isShowing())
      {
        DockNode dockNode = (DockNode) event.getContents();
        dockNode.dock(this, dockPosDrag, dockAreaDrag);
      }
    }

    if ((event.getEventType() == DockEvent.DOCK_EXIT
         && !this.receivedEnter)
        || event.getEventType() == DockEvent.DOCK_RELEASED)
    {
      if (dockIndicatorOverlay.isShowing())
      {
        dockIndicatorOverlay.hide();
      }
      if (dockIndicatorPopup.isShowing())
      {
        dockIndicatorPopup.hide();
      }
    }
  }

  public void storePreference(String filePath)
  {
    ContentPane pane = (ContentPane) root;

    HashMap<String, ContentHolder> contents = new HashMap<>();

    // Floating Nodes collection
    contents.put("_FloatingNodes",
                 new ContentHolder("_FloatingNodes",
                                   ContentHolder.Type.Collection));

    List<DockNode> floatingNodes = new LinkedList<>(undockedNodes);

    for (int i = 0; i < floatingNodes.size(); i++)
    {
      // System.out.println(floatingNodes.get(i).getTitle());
      ContentHolder floatingNode =
                                 new ContentHolder(floatingNodes.get(i)
                                                                .getTitle(),
                                                   ContentHolder.Type.FloatingNode);
      floatingNode.addProperty("Title",
                               floatingNodes.get(i).getTitle());
      floatingNode.addProperty("Size", new Double[]
      { floatingNodes.get(i).getLayoutBounds().getWidth(),
        floatingNodes.get(i).getLayoutBounds().getHeight() });
      floatingNode.addProperty("Minimized",
                               floatingNodes.get(i).isMinimized());

      Point2D loc = floatingNodes.get(i)
                                 .localToScreen(floatingNodes.get(i)
                                                             .getLayoutBounds()
                                                             .getMinX(),
                                                floatingNodes.get(i)
                                                             .getLayoutBounds()
                                                             .getMinY());

      floatingNode.addProperty("Position", new Double[]
      { loc.getX(), loc.getY() });

      contents.get("_FloatingNodes").addChild(floatingNode);
    }

    // Prepare Docking Nodes collection
    List<DockNode> dockingNodes = new LinkedList<>();

    Integer count = 0;

    checkPane(contents, pane, dockingNodes, count);

    contents.get("0").addProperty("Size", new Double[]
    { this.getScene().getWindow().getWidth(),
      this.getScene().getWindow().getHeight() });
    contents.get("0").addProperty("Position", new Double[]
    { this.getScene().getWindow().getX(),
      this.getScene().getWindow().getY() });

    // Try to write layout to file.
    try (
        XMLEncoder encoder =
                           new XMLEncoder(new BufferedOutputStream(new FileOutputStream(filePath))))
    {
      encoder.writeObject(contents);
    }
    catch (FileNotFoundException e)
    {
      Logger.getLogger(DockPane.class.getName()).log(Level.WARNING,
                                                     "Could not save preferences to {0}",
                                                     filePath);
    }
  }

  private ContentHolder checkPane(HashMap<String, ContentHolder> contents,
                                  ContentPane pane,
                                  List<DockNode> dockingNodes,
                                  Integer count)
  {
    ContentHolder holder = null;
    if (pane instanceof ContentSplitPane)
    {
      final String contentSplitPaneName = "" + count++;
      ContentSplitPane splitPane = (ContentSplitPane) pane;

      holder = new ContentHolder(contentSplitPaneName,
                                 ContentHolder.Type.SplitPane);
      contents.put(contentSplitPaneName, holder);

      holder.addProperty("Orientation", splitPane.getOrientation());
      holder.addProperty("DividerPositions",
                         splitPane.getDividerPositions());
    }
    else if (pane instanceof ContentTabPane)
    {
      final String contentTabPaneName = "" + count++;
      ContentTabPane tabPane = (ContentTabPane) pane;

      holder = new ContentHolder(contentTabPaneName,
                                 ContentHolder.Type.TabPane);
      contents.put(contentTabPaneName, holder);

      holder.addProperty("SelectedIndex",
                         tabPane.getSelectionModel()
                                .getSelectedIndex());
    }

    for (Node node : pane.getChildrenList())
    {
      if (node instanceof DockNode)
      {
        dockingNodes.add((DockNode) node);
        holder.addChild(((DockNode) node).getTitle());
        // System.out.println( ( ( DockNode ) node ).getTitle() );
      }
      // else
      // {
      // System.out.println( node.getClass().getCanonicalName() );
      // }

      if (node instanceof ContentPane)
      {
        holder.addChild(checkPane(contents,
                                  (ContentPane) node,
                                  dockingNodes,
                                  count));
      }
    }

    return holder;
  }

  public void loadPreference(String filePath)
  {
    loadPreference(filePath, null);
  }

  public void loadPreference(String filePath,
                             DelayOpenHandler delayOpenHandler)
  {
    HashMap<String, ContentHolder> contents = null;

    try (
        XMLDecoder decoder =
                           new XMLDecoder(new BufferedInputStream(new FileInputStream(filePath)), Thread.currentThread().getContextClassLoader()))
    {
      contents =
               (HashMap<String, ContentHolder>) decoder.readObject();
    }
    catch (NullPointerException e)
    {
      Logger.getLogger(DockPane.class.getName()).log(Level.WARNING,
                                                     "Null filepath, cannot load preferences",
                                                     filePath);
    }
    catch (FileNotFoundException e)
    {
      Logger.getLogger(DockPane.class.getName()).log(Level.WARNING,
                                                     "No preferences found at {0}",
                                                     filePath);
    }
    catch (ArrayIndexOutOfBoundsException e)
    {
      Logger.getLogger(DockPane.class.getName()).log(Level.WARNING,
                                                     "Could not retrieve any preferences from {0}",
                                                     filePath);
    }
    catch (ClassCastException e)
    {
      Logger.getLogger(DockPane.class.getName()).log(Level.WARNING,
                                                     "Could not load preferences in correct format from {0} ",
                                                     filePath);
    }

    if (contents != null)
    {
      applyPane(contents, (ContentPane) root, delayOpenHandler);
    }
  }

  private void collectDockNodes(HashMap<String, DockNode> dockNodes,
                                ContentPane pane)
  {
    for (Node node : pane.getChildrenList())
    {
      if (node instanceof DockNode)
      {
        dockNodes.put(((DockNode) node).getTitle(), (DockNode) node);
      }

      if (node instanceof ContentPane)
      {
        collectDockNodes(dockNodes, (ContentPane) node);
      }
    }
  }

  private void applyPane(HashMap<String, ContentHolder> contents,
                         ContentPane root,
                         DelayOpenHandler delayOpenHandler)
  {
    // Collect the current pane information
    HashMap<String, DockNode> dockNodes = new HashMap<>();

    // undockNodes
    for (DockNode node : undockedNodes)
    {
      dockNodes.put(node.getTitle(), node);
    }

    collectDockNodes(dockNodes, root);

    Double[] windowSize = (Double[]) contents.get("0")
                                             .getProperties()
                                             .get("Size");
    Double[] windowPosition = (Double[]) contents.get("0")
                                                 .getProperties()
                                                 .get("Position");

    Stage currentStage = (Stage) this.getScene().getWindow();

    // In case that the current screen size is smaller than the size stored in
    // the preference file,
    // The below logic prevents from the window hidden in outside the current
    // screen
	Rectangle2D screen = Screen.getPrimary().getBounds();

    if (windowPosition[0] > screen.getMaxX() || windowPosition[0] < screen.getMinX())
    {
      windowPosition[0] = currentStage.getX();
    }

    if (windowPosition[1] > screen.getMaxY() || windowPosition[1] < screen.getMinY())
    {
      windowPosition[1] = currentStage.getY();
    }

    currentStage.setX(windowPosition[0]);
    currentStage.setY(windowPosition[1]);

    currentStage.setWidth(windowSize[0]);
    currentStage.setHeight(windowSize[1]);

    // Set floating docks according to the preference data
    for (Object item : contents.get("_FloatingNodes").getChildren())
    {
      ContentHolder holder = (ContentHolder) item;
      String title = holder.getProperties().getProperty("Title");
      Double[] size = (Double[]) holder.getProperties().get("Size");
      Double[] position = (Double[]) holder.getProperties()
                                           .get("Position");
      boolean minimized = (boolean) holder.getProperties()
                                          .getOrDefault("Minimized",
                                                        false);
      DockNode node = dockNodes.get(title);
      if (null == node && null != delayOpenHandler)
        node = delayOpenHandler.open(title);

      if (null != node)
      {
        node.setFloating(true, null, this);

        node.getStage().setX(position[0]);
        node.getStage().setY(position[1]);

        node.getStage().setWidth(size[0]);
        node.getStage().setHeight(size[1]);

        dockNodes.remove(title);

        node.setMinimized(minimized);
      }
      else
      {
        System.err.println(item + " is not present.");
      }
    }

    // Restore dock location based on the preferences
    // Make it sorted
    ContentHolder rootHolder = contents.get("0");
    Node newRoot = buildPane(null,
                             rootHolder,
                             dockNodes,
                             delayOpenHandler);

    // We have new dock nodes to be added, otherwise we will lose the new nodes
    if (dockNodes.size() > 0)
    {
      for (String title : dockNodes.keySet())
      {
        DockNode node = dockNodes.get(title);
        node.setFloating(true, null, this);

        node.getStage().setX(node.getStage().getX() + 100);
        node.getStage().setY(node.getStage().getY() + 100);
      }

      dockNodes.clear();
    }

    this.root = newRoot;
    this.getChildren().set(0, this.root);
  }

  private Node buildPane(ContentPane parent,
                         ContentHolder holder,
                         HashMap<String, DockNode> dockNodes,
                         DelayOpenHandler delayOpenHandler)
  {
    Node pane = null;
    if (holder.getType().equals(ContentHolder.Type.SplitPane))
    {
      ContentSplitPane splitPane = new ContentSplitPane();
      splitPane.setOrientation((Orientation) holder.getProperties()
                                                   .get("Orientation"));
      splitPane.setDividerPositions((double[]) holder.getProperties()
                                                     .get("DividerPositions"));

      for (Object item : holder.getChildren())
      {
        if (item instanceof String)
        {
          // Use dock node
          if (dockNodes.containsKey(item))
          {
            DockNode n = dockNodes.get(item);
            if (n.tabbedProperty().get())
            {
              n.tabbedProperty().set(false);
            }

            splitPane.getItems().add(dockNodes.get(item));
            dockNodes.remove(item);
          }
          else
          {
            // If delayOpenHandler is provided, we call it
            if (delayOpenHandler != null)
            {
              DockNode newNode = delayOpenHandler.open((String) item);
              if (null != newNode)
              {
                if (newNode.tabbedProperty().get())
                {
                  newNode.tabbedProperty().set(false);
                }
                newNode.dockedProperty().set(true);
                splitPane.getItems().add(newNode);
              }
            }
            else
              System.err.println(item + " is not present.");
          }
        }
        else if (item instanceof ContentHolder)
        {
          // Call this function recursively
          splitPane.getItems()
                   .add(buildPane(splitPane,
                                  (ContentHolder) item,
                                  dockNodes,
                                  delayOpenHandler));
        }
      }
      if (parent != null)
        splitPane.setContentParent(parent);
      pane = splitPane;
    }
    else if (holder.getType().equals(ContentHolder.Type.TabPane))
    {
      ContentTabPane tabPane = new ContentTabPane();

      List<DockNode> dockNodeList = new ArrayList<>();
      for (Object item : holder.getChildren())
      {
        if (item instanceof String)
        {
          // Use dock node
          if (dockNodes.containsKey(item))
          {
            tabPane.addDockNodeTab(new DockNodeTab(dockNodes.get(item)));
            dockNodes.remove(item);
          }
          else
          {
            // If delayOpenHandler is provided, we call it
            if (null != delayOpenHandler)
            {
              DockNode newNode = delayOpenHandler.open((String) item);
              if (newNode != null)
              {
              	dockNodeList.add( newNode );
              }
            }
            else
              System.err.println(item + " is not present.");
          }
        }
      }

      for (DockNode n : dockNodeList)
	  {
		n.dockedProperty().set(true);
		tabPane.addDockNodeTab(new DockNodeTab(n));
	  }

      if (parent != null)
        tabPane.setContentParent(parent);
      tabPane.getSelectionModel()
             .select((int) holder.getProperties()
                                 .get("SelectedIndex"));
      pane = tabPane;
    }

    return pane;
  }
}
