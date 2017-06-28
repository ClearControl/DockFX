package org.dockfx.persist;

import javafx.scene.*;
import org.dockfx.UiUtil;
import org.dockfx.dock.DockPane;
import org.dockfx.pane.ContentPane;

import java.util.*;

/**
 * Created by jding on 28/06/2017.
 */
public class PersistableDockPane implements Persistable<DockPane>{
  static DockPane dockPane;
  Rec position;
  List<Persistable<?>> children = new LinkedList<>();
  @Override
  public DockPane reconstruct(Parent parentContent) {
    dockPane = new DockPane();
    dockPane.setPrefHeight(position.getHeight());
    dockPane.setPrefWidth(position.getWidth());
    for(Persistable<?> child : children){
      child.reconstruct(dockPane);
    }
    return dockPane;
  }

  @Override
  public Persistable<DockPane> persist(DockPane dockPane) {
    position = UiUtil.getRec(dockPane);
    return this;
  }

  @Override
  public void addChild(Persistable<?> child) {
    children.add(child);
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
}
