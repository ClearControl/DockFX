package org.dockfx.pane;

import java.util.Comparator;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;

import org.dockfx.dock.DockNode;
import org.dockfx.dock.DockPos;
import org.dockfx.pane.skin.ContentTabPaneSkin;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Skin;
import javafx.scene.control.TabPane;

/**
 * ContentTabPane holds multiple tabs
 *
 * @author HongKee Moon
 */
public class ContentTabPane extends TabPane implements ContentPane {

  ContentPane parent;

  public ContentTabPane() {
  }

  /** {@inheritDoc} */
  @Override protected Skin<?> createDefaultSkin() {
	return new ContentTabPaneSkin(this);
  }

  public Type getType() {
    return Type.TabPane;
  }

  public void setContentParent(ContentPane pane) {
    parent = pane;
  }

  public ContentPane getContentParent() {
    return parent;
  }

  public ContentPane getSiblingParent(Stack<Parent> stack, Node sibling) {
    ContentPane pane = null;

    while (!stack.isEmpty()) {
      Parent parent = stack.pop();

      List<Node> children = parent.getChildrenUnmodifiable();

      if (parent instanceof ContentPane) {
        children = ((ContentPane) parent).getChildrenList();
      }

      for (int i = 0; i < children.size(); i++) {
        if (children.get(i) == sibling) {
          pane = (ContentPane) parent;
        } else if (children.get(i) instanceof Parent) {
          stack.push((Parent) children.get(i));
        }
      }
    }
    return pane;
  }

  public boolean removeNode(Stack<Parent> stack, Node node) {
    List<Node> children = getChildrenList();

    for (int i = 0; i < children.size(); i++) {
      if (children.get(i) == node) {
        getTabs().remove(i);
        return true;
      }
    }

    return false;
  }

  public void set(int idx, Node node) {
    DockNode newNode = (DockNode) node;
    getTabs().set(idx, new DockNodeTab(newNode));
	getSelectionModel().select( idx );
  }

  public void set(Node sibling, Node node) {
    set(getChildrenList().indexOf(sibling), node);
  }

  public List<Node> getChildrenList() {
    return getTabs().stream().map(i -> i.getContent()).collect(Collectors.toList());
  }

  public void addNode(Node root, Node sibling, Node node, DockPos dockPos) {
    DockNode newNode = (DockNode) node;
	DockNodeTab t = new DockNodeTab(newNode);
	addDockNodeTab( t );
  }

  public void addDockNodeTab(DockNodeTab dockNodeTab)
  {
	  getTabs().add(dockNodeTab);
	  getSelectionModel().select( dockNodeTab );
  }

  @Override
  protected double computeMaxWidth(double height) {
    return getTabs().stream().map(i -> i.getContent().maxWidth(height)).min(Comparator.naturalOrder()).get();
  }
}
