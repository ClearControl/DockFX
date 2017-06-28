package org.dockfx.persist;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.*;
import org.dockfx.UiUtil;
import org.dockfx.dock.*;
import org.dockfx.pane.*;

import javafx.geometry.Orientation;

/**
 * Created by jding on 14/06/2017.
 */
public class PersistableSplitPane implements Persistable<ContentSplitPane> {

  double[] spliterPostion; //for splitPane
  Orientation orientation; //for split pane
  Rec position;
  List<Persistable<?>> children = new ArrayList<>(10);

  @Override
  public ContentSplitPane reconstruct(Parent parentPane) {
    ContentSplitPane splitPane = new ContentSplitPane();
    splitPane.setOrientation(orientation);
    if (parentPane instanceof ContentSplitPane) {
      ContentSplitPane pane = (ContentSplitPane) parentPane;
      splitPane.setContentParent(pane);
      pane.getItems().add(splitPane);
    }else{
      DockPane pane = (DockPane)parentPane;
      pane.setRoot( splitPane);
      pane.getChildren().setAll(splitPane);
    }
    for(Persistable<?> child : children){
      child.reconstruct(splitPane);
    }
    splitPane.setDividerPositions(spliterPostion);
    return splitPane;
  }

  @Override
  public PersistableSplitPane persist(ContentSplitPane splitPane) {
    orientation = splitPane.getOrientation();
    spliterPostion = splitPane.getDividerPositions();
    position = UiUtil.getRec(splitPane);
    return this;
  }

  public double[] getSpliterPostion() {
    return spliterPostion;
  }

  public void setSpliterPostion(double[] spliterPostion) {
    this.spliterPostion = spliterPostion;
  }
  public Orientation getOrientation() {
    return orientation;
  }

  public void setOrientation(Orientation orientation) {
    this.orientation = orientation;
  }

  public Rec getPosition() {
    return position;
  }

  public void setPosition(Rec position) {
    this.position = position;
  }

  public List<Persistable<?>> getChildren() {
    return children;
  }

  public void setChildren(List<Persistable<?>> children) {
    this.children = children;
  }

  public void addChild(Persistable<?> child) {
    children.add(child);
  }
}
