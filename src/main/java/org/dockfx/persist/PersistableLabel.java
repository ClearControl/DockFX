package org.dockfx.persist;

import javafx.scene.Parent;
import javafx.scene.control.Label;
import org.dockfx.pane.ContentPane;

/**
 * Created by jding on 28/06/2017.
 */
public class PersistableLabel implements Persistable<Label>{
  String label = "please implement the node as Persistable";

  @Override
  public Label reconstruct(Parent contentPane) {
    return new Label(label);
  }

  @Override
  public Persistable persist(Label label) {
    return null;
  }

  @Override
  public void addChild(Persistable child) {

  }
}
