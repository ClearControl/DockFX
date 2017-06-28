package org.dockfx.persist;

import javafx.scene.*;
import org.dockfx.pane.ContentPane;

/**
 * Created by ding jiangfeng on 14/06/2017.
 */
public interface Persistable<T> {
  T reconstruct(Parent parentPane);
  Persistable<T> persist(T t);
  void addChild(Persistable<?> child);
}
