package org.dockfx.persist;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.*;
import org.dockfx.UiUtil;
import org.dockfx.dock.DockPos;
import org.dockfx.pane.*;

/**
 * Created by jding on 14/06/2017.
 */
public class PersistableTabPane implements Persistable<ContentTabPane> {

  Rec position;
  List<Persistable<?>> children = new ArrayList<>(10);
  
  @Override
  public ContentTabPane reconstruct(Parent parentPane) {
    if (parentPane instanceof ContentSplitPane) {
      ContentSplitPane pane = (ContentSplitPane) parentPane;
      ContentTabPane tabPane = new ContentTabPane();
      tabPane.setContentParent(pane);
      pane.getItems().add(tabPane);
      for(Persistable<?> child:children){
        tabPane.addNode(null,null,(Node)child.reconstruct(tabPane), DockPos.LEFT);
      }
      return tabPane;
    }else{
      return null;
    }
  }

  @Override
  public PersistableTabPane persist(ContentTabPane tabPane) {
    position = UiUtil.getRec(tabPane);
    return this;
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

//  @Override
  public void addChild(Persistable<?> child) {
    children.add(child);
  }
}
