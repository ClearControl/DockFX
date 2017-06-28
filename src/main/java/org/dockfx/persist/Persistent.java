package org.dockfx.persist;

import java.util.List;
import org.dockfx.dock.DockPane;

import javafx.stage.Stage;


/**
 * Created by jiang ding on 16/06/2017.
 */
public interface Persistent {
  DockPane load(String filePath) throws PersistanceException;
  void save(Stage primaryStage, String filePath) throws  PersistanceException;
}
