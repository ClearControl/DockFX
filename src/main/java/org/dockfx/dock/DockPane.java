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

package org.dockfx.dock;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import org.dockfx.pane.ContentPane;
import org.dockfx.pane.ContentSplitPane;
import org.dockfx.pane.ContentTabPane;
import org.dockfx.pane.DockNodeTab;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.css.PseudoClass;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Base class for a dock pane that provides the layout of the dock nodes.
 * Stacking the dock nodes to the center in a TabPane will be added in a future
 * release. For now the DockPane uses the relative sizes of the dock nodes and
 * lays them out in a tree of SplitPanes.
 *
 * @since DockFX 0.1
 */
public class DockPane extends StackPane implements EventHandler<DockEvent>{

	/**
	 * The current root node of this dock pane's layout.
	 */
	private Node root;

	/**
	 * Whether or not this dock pane allows the docking of dock nodes from external
	 * sources (i.e., other dock panes).
	 */
	private boolean exclusive = false;

	/**
	 * Whether a DOCK_ENTER event has been received by this dock pane since the last
	 * DOCK_EXIT event was received.
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
	 * The docking position of the current dock indicator button if any is selected.
	 */
	private DockPos dockPosDrag;

	/**
	 * The docking area shape with a dotted animated border on the indicator overlay
	 * popup.
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
	 * The popup used to display the local dock indicator buttons. This allows these
	 * indicator buttons to be displayed outside the window of this dock pane.
	 */
	private Popup dockIndicatorPopup;
	
	private Stage stage;
	
	private final List<DockNode> allNodes = new LinkedList<>();

	/**
	 * A collection used to manage the indicator buttons and automate hit detection
	 * during DOCK_OVER events.
	 */
	private List<DockPosButton> dockPosButtons;

	private ObservableList<DockNode> floatingNodes;

	/**
	 * Creates a new DockPane adding event handlers for dock events and creating the
	 * indicator overlays.
	 */
	public DockPane() {
		this.sceneProperty().addListener(new InvalidationListener() {
			
			@Override
			public void invalidated(Observable observable) {
				Scene scene = DockPane.this.sceneProperty().get();
				if(scene!=null) {
					String cssUrl = DockPane.this.getClass().getResource("default.css").toExternalForm();
					List<String> sheets = scene.getStylesheets();
					if(!sheets.contains(cssUrl)) {
						sheets.add(cssUrl);
					}
					DockPane.this.sceneProperty().removeListener(this);
				}
			}
		});
		
		this.addEventHandler(DockEvent.ANY, this);
		this.addEventFilter(DockEvent.ANY, new EventHandler<DockEvent>() {

			@Override
			public void handle(DockEvent event) {

				if (event.getEventType() == DockEvent.DOCK_ENTER) {
					DockPane.this.receivedEnter = true;
				} else if (event.getEventType() == DockEvent.DOCK_OVER) {
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
		KeyValue kv = new KeyValue(dockAreaIndicator.strokeDashOffsetProperty(), 12);
		KeyFrame kf = new KeyFrame(Duration.millis(500), kv);
		dockAreaStrokeTimeline.getKeyFrames().add(kf);
		dockAreaStrokeTimeline.play();

		DockPosButton dockCenter = new DockPosButton(false, DockPos.CENTER);
		dockCenter.getStyleClass().add("dock-center");
		DockPosButton dockTop = new DockPosButton(false, DockPos.TOP);
		dockTop.getStyleClass().add("dock-top");
		DockPosButton dockRight = new DockPosButton(false, DockPos.RIGHT);
		dockRight.getStyleClass().add("dock-right");
		DockPosButton dockBottom = new DockPosButton(false, DockPos.BOTTOM);
		dockBottom.getStyleClass().add("dock-bottom");
		DockPosButton dockLeft = new DockPosButton(false, DockPos.LEFT);
		dockLeft.getStyleClass().add("dock-left");

		DockPosButton dockTopRoot = new DockPosButton(true, DockPos.TOP);
		StackPane.setAlignment(dockTopRoot, Pos.TOP_CENTER);
		dockTopRoot.getStyleClass().add("dock-top-root");
		DockPosButton dockRightRoot = new DockPosButton(true, DockPos.RIGHT);
		StackPane.setAlignment(dockRightRoot, Pos.CENTER_RIGHT);
		dockRightRoot.getStyleClass().add("dock-right-root");
		DockPosButton dockBottomRoot = new DockPosButton(true, DockPos.BOTTOM);
		StackPane.setAlignment(dockBottomRoot, Pos.BOTTOM_CENTER);
		dockBottomRoot.getStyleClass().add("dock-bottom-root");
		DockPosButton dockLeftRoot = new DockPosButton(true, DockPos.LEFT);
		StackPane.setAlignment(dockLeftRoot, Pos.CENTER_LEFT);
		dockLeftRoot.getStyleClass().add("dock-left-root");

		// TODO: dockCenter goes first when tabs are added in a future version
		dockPosButtons = Arrays.asList(dockCenter, dockTop, dockRight, dockBottom, dockLeft, dockTopRoot, dockRightRoot,
				dockBottomRoot, dockLeftRoot);

		dockPosIndicator = new GridPane();
		dockPosIndicator.add(dockTop, 1, 0);
		dockPosIndicator.add(dockRight, 2, 1);
		dockPosIndicator.add(dockBottom, 1, 2);
		dockPosIndicator.add(dockLeft, 0, 1);
		dockPosIndicator.add(dockCenter, 1, 1);

		dockRootPane.getChildren().addAll(dockAreaIndicator, dockTopRoot, dockRightRoot, dockBottomRoot, dockLeftRoot);

		dockIndicatorOverlay.getContent().add(dockRootPane);
		dockIndicatorPopup.getContent().addAll(dockPosIndicator);

		this.getStyleClass().add("dock-pane");
		dockRootPane.getStyleClass().add("dock-root-pane");
		dockPosIndicator.getStyleClass().add("dock-pos-indicator");
		dockAreaIndicator.getStyleClass().add("dock-area-indicator");

		floatingNodes = FXCollections.observableArrayList();
	}

	/**
	 * Indicates whether or not the dock pane is in exclusive mode (the default). In
	 * exclusive mode, the dock pane ignores dock nodes dragged from other dock
	 * panes, and dock nodes dragged from this dock pane are ignored by other dock
	 * panes.
	 * 
	 * @return true or false.
	 */
	public boolean isExclusive() {
		return exclusive;
	}

	/**
	 * Enables/disables exclusive mode.
	 * 
	 * @param exclusive
	 *            true for exclusive mode, and false otherwise.
	 */
	public void setExclusive(boolean exclusive) {
		this.exclusive = exclusive;
	}

	/**
	 * The Timeline used to animate the docking area indicator in the dock indicator
	 * overlay for this dock pane.
	 *
	 * @return The Timeline used to animate the docking area indicator in the dock
	 *         indicator overlay for this dock pane.
	 */
	public final Timeline getDockAreaStrokeTimeline() {
		return dockAreaStrokeTimeline;
	}

	/**
	 * Helper function to retrieve the URL of the default style sheet used by
	 * DockFX.
	 *
	 * @return The URL of the default style sheet used by DockFX.
	 */
	public final static String getDefaultUserAgentStylesheet() {
		return DockPane.class.getResource("default.css").toExternalForm();
	}
	/**
	 * A cache of all dock node event handlers that we have created for tracking the
	 * current docking area.
	 */
	private ObservableMap<Node, DockNodeEventHandler> dockNodeEventFilters = FXCollections.observableHashMap();

	/**
	 * Dock the node into this dock pane at the given docking position relative to
	 * the sibling in the layout. This is used to relatively position the dock nodes
	 * to other nodes given their preferred size.
	 *
	 * @param node
	 *            The node that is to be docked into this dock pane.
	 * @param dockPos
	 *            The docking position of the node relative to the sibling.
	 * @param sibling
	 *            The sibling of this node in the layout.
	 */
	void dock(DockNode node, DockPos dockPos, Node sibling) {
		if(!allNodes.contains(node)){
			allNodes.add(node);
		}
		node.dockImpl(this, dockPos, sibling);
		DockNodeEventHandler dockNodeEventHandler = new DockNodeEventHandler(this, node);
		dockNodeEventFilters.put(node, dockNodeEventHandler);
		node.addEventFilter(DockEvent.DOCK_OVER, dockNodeEventHandler);

		ContentPane pane = (ContentPane) root;
		if (pane == null) {
			pane = new ContentSplitPane(node);
			root = (Node) pane;
			this.getChildren().add(root);
			return;
		}

		if (sibling != null && sibling != root) {
			Stack<Parent> stack = new Stack<>();
			stack.push((Parent) root);
			pane = pane.getSiblingParent(stack, sibling);
		}

		if (pane == null) {
			sibling = root;
			dockPos = DockPos.RIGHT;
			pane = (ContentPane) root;
		}

		if (dockPos == DockPos.CENTER) {
			if (sibling instanceof DockNode) {
				if (pane instanceof ContentSplitPane) {
					// Create a ContentTabPane with two nodes
					DockNode siblingNode = (DockNode) sibling;
					DockNode newNode = (DockNode) node;

					ContentTabPane tabPane = new ContentTabPane();

					tabPane.addDockNodeTab(new DockNodeTab(siblingNode));
					tabPane.addDockNodeTab(new DockNodeTab(newNode));

					tabPane.setContentParent(pane);

					double[] pos = ((ContentSplitPane) pane).getDividerPositions();
					pane.set(sibling, tabPane);
					((ContentSplitPane) pane).setDividerPositions(pos);
				}
			} else {
				ContentSplitPane siblingSplitPane = (ContentSplitPane) sibling;

				ContentPane parent = siblingSplitPane.getContentParent();
				if (parent == null) {
					Node child = siblingSplitPane.getChildrenList().get(0);
					if (child instanceof DockNode) {
						// If we are docking into a DockNode, make a ContentTabPane of the
						// two
						ContentTabPane tabPane = new ContentTabPane();
						tabPane.addNode(root, null, child, DockPos.CENTER);
						siblingSplitPane.set(child, tabPane);
						tabPane.setContentParent(siblingSplitPane);
						pane = tabPane;
						sibling = null;
					} else if (child instanceof ContentSplitPane) {
						// TODO: Recursively reorder the panes instead of throwing it in?
						pane = (ContentSplitPane) child;
						dockPos = DockPos.LEFT;
					} else if (child instanceof ContentTabPane) {
						// If we already have a tab pane, just add the node to it
						pane = (ContentTabPane) child;
					}
				}
			}
		} else {
			// Otherwise, SplitPane is assumed.
			Orientation requestedOrientation = (dockPos == DockPos.LEFT || dockPos == DockPos.RIGHT)
					? Orientation.HORIZONTAL
					: Orientation.VERTICAL;

			if (pane instanceof ContentSplitPane) {
				ContentSplitPane split = (ContentSplitPane) pane;

				// if the orientation is different then reparent the split pane
				if (split.getOrientation() != requestedOrientation) {
					if (split.getItems().size() > 1) {
						ContentSplitPane splitPane = new ContentSplitPane();

						if (split == root && sibling == root) {
							this.getChildren().set(this.getChildren().indexOf(root), splitPane);
							splitPane.getItems().add(split);
							root = splitPane;
						} else {
							split.set(sibling, splitPane);
							splitPane.setContentParent(split);
							splitPane.getItems().add(sibling);
						}

						split = splitPane;
					}
					split.setOrientation(requestedOrientation);
					pane = split;
				}
			} else if (pane instanceof ContentTabPane) {

				if (pane.getContentParent() != null) {
					ContentSplitPane split = (ContentSplitPane) pane.getContentParent();

					// if the orientation is different then reparent the split pane
					if (split.getOrientation() != requestedOrientation) {
						ContentSplitPane splitPane = new ContentSplitPane();
						if (split == root && sibling == root) {
							this.getChildren().set(this.getChildren().indexOf(root), splitPane);
							splitPane.getItems().add(split);
							root = splitPane;
						} else {
							pane.setContentParent(splitPane);
							sibling = (Node) pane;
							split.set(sibling, splitPane);
							splitPane.setContentParent(split);
							splitPane.getItems().add(sibling);
						}
						split = splitPane;
					} else {
						sibling = (Node) pane;
					}

					split.setOrientation(requestedOrientation);
					pane = split;
				} else {
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

		if (floatingNodes.contains(node)) {
			floatingNodes.remove(node);
		}
	}

	
	
	public void dock(Node node, DockPos dockPos,String title) {
		DockNode dockNode = new DockNode(node,title,null);
		dock(dockNode,dockPos);
	}
	/**
	 * Dock the node into this dock pane at the given docking position relative to
	 * the root in the layout. This is used to relatively position the dock nodes to
	 * other nodes given their preferred size.
	 *
	 * @param node
	 *            The node that is to be docked into this dock pane.
	 * @param dockPos
	 *            The docking position of the node relative to the sibling.
	 */
	public void dock(DockNode node, DockPos dockPos) {
		dock(node, dockPos, root);
	}

	/**
	 * Detach the node from this dock pane removing it from the layout.
	 *
	 * @param node
	 *            The node that is to be removed from this dock pane.
	 */
	void floatingNode(DockNode node) {
		if (!node.closedProperty().get())
			floatingNodes.add(node);

		DockNodeEventHandler dockNodeEventHandler = dockNodeEventFilters.get(node);
		node.removeEventFilter(DockEvent.DOCK_OVER, dockNodeEventHandler);
		dockNodeEventFilters.remove(node);

		// depth first search to find the parent of the node
		Stack<Parent> findStack = new Stack<Parent>();
		findStack.push((Parent) root);

		while (!findStack.isEmpty()) {
			Parent parent = findStack.pop();

			if (parent instanceof ContentPane) {
				ContentPane pane = (ContentPane) parent;
				pane.removeNode(findStack, node);

				// if there is 0 children left, make sure we remove the split pane
				if (pane.getChildrenList().isEmpty()) {
					if (root == pane) {
						this.getChildren().remove((Node) pane);
						root = null;
					}
				} else if (pane.getChildrenList().size() == 1 && pane instanceof ContentTabPane
						&& pane.getChildrenList().get(0) instanceof DockNode) {
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

	public void removeFloatingNodeFromUndockNodes(DockNode n) {
		floatingNodes.remove(n);
	}

	@Override
	public void handle(DockEvent event) {
		// handle exclusive mode.
		DockPane otherPane = ((DockNode) event.getContents()).getDockPane();

		if (otherPane != this) {
			if (isExclusive()) {
				// Can't accept nodes from other dock panes.
				return;
			}

			if (otherPane != null && otherPane.isExclusive()) {
				// Nodes from the other pane cannot be docked elsewhere.
				return;
			}
		}

		if (event.getEventType() == DockEvent.DOCK_ENTER) {
			if (!dockIndicatorOverlay.isShowing()) {
				Point2D originToScreen;
				if (null != root) {
					originToScreen = root.localToScreen(0, 0);
				} else {
					originToScreen = this.localToScreen(0, 0);
				}

				dockIndicatorOverlay.show(DockPane.this, originToScreen.getX(), originToScreen.getY());
			}
		} else if (event.getEventType() == DockEvent.DOCK_OVER) {
			this.receivedEnter = false;

			dockPosDrag = null;
			dockAreaDrag = dockNodeDrag;

			for (DockPosButton dockIndicatorButton : dockPosButtons) {
				if (dockIndicatorButton
						.contains(dockIndicatorButton.screenToLocal(event.getScreenX(), event.getScreenY()))) {
					dockPosDrag = dockIndicatorButton.getDockPos();
					if (dockIndicatorButton.isDockRoot()) {
						dockAreaDrag = root;
					}
					dockIndicatorButton.pseudoClassStateChanged(PseudoClass.getPseudoClass("focused"), true);
					break;
				} else {
					dockIndicatorButton.pseudoClassStateChanged(PseudoClass.getPseudoClass("focused"), false);
				}
			}

			if (dockPosDrag != null && dockAreaDrag != null) {
				Point2D originToScene = dockAreaDrag.localToScreen(0, 0);

				dockAreaIndicator.setVisible(true);
				dockAreaIndicator.relocate(originToScene.getX() - dockIndicatorOverlay.getAnchorX(),
						originToScene.getY() - dockIndicatorOverlay.getAnchorY());
				if (dockPosDrag == DockPos.RIGHT) {
					dockAreaIndicator.setTranslateX(dockAreaDrag.getLayoutBounds().getWidth() / 2);
				} else {
					dockAreaIndicator.setTranslateX(0);
				}

				if (dockPosDrag == DockPos.BOTTOM) {
					dockAreaIndicator.setTranslateY(dockAreaDrag.getLayoutBounds().getHeight() / 2);
				} else {
					dockAreaIndicator.setTranslateY(0);
				}

				if (dockPosDrag == DockPos.LEFT || dockPosDrag == DockPos.RIGHT) {
					dockAreaIndicator.setWidth(dockAreaDrag.getLayoutBounds().getWidth() / 2);
				} else {
					dockAreaIndicator.setWidth(dockAreaDrag.getLayoutBounds().getWidth());
				}
				if (dockPosDrag == DockPos.TOP || dockPosDrag == DockPos.BOTTOM) {
					dockAreaIndicator.setHeight(dockAreaDrag.getLayoutBounds().getHeight() / 2);
				} else {
					dockAreaIndicator.setHeight(dockAreaDrag.getLayoutBounds().getHeight());
				}
			} else {
				dockAreaIndicator.setVisible(false);
			}

			if (dockNodeDrag != null && ((DockNode) dockNodeDrag).getDockTitleBar() != null) {
				Point2D originToScreen = dockNodeDrag.localToScreen(0, 0);

				double posX = originToScreen.getX() + dockNodeDrag.getLayoutBounds().getWidth() / 2
						- dockPosIndicator.getWidth() / 2;
				double posY = originToScreen.getY() + dockNodeDrag.getLayoutBounds().getHeight() / 2
						- dockPosIndicator.getHeight() / 2;

				if (!dockIndicatorPopup.isShowing()) {
					dockIndicatorPopup.show(DockPane.this, posX, posY);
				} else {
					dockIndicatorPopup.setX(posX);
					dockIndicatorPopup.setY(posY);
				}

				// set visible after moving the popup
				dockPosIndicator.setVisible(true);
			} else {
				dockPosIndicator.setVisible(false);
			}
		}

		if (event.getEventType() == DockEvent.DOCK_RELEASED && event.getContents() != null) {
			if (dockPosDrag != null && dockIndicatorOverlay.isShowing()) {
				DockNode dockNode = (DockNode) event.getContents();
				this.dock(dockNode, dockPosDrag, dockAreaDrag);
			}
		}

		if ((event.getEventType() == DockEvent.DOCK_EXIT && !this.receivedEnter)
				|| event.getEventType() == DockEvent.DOCK_RELEASED) {
			if (dockIndicatorOverlay.isShowing()) {
				dockIndicatorOverlay.hide();
			}
			if (dockIndicatorPopup.isShowing()) {
				dockIndicatorPopup.hide();
			}
		}
	}

	public List<DockNode> getAllNodes() {
		return allNodes;
	}

	void setDockNodeDrag(Node node){
		dockNodeDrag = node;
	}

	public void addDockNodeEventFilter(DockNode dockNode,DockNodeEventHandler handler){
		dockNodeEventFilters.put(dockNode,handler);
	}

	public void setRoot(Node root){
		this.root = root;
	}
}
