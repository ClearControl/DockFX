package org.dockfx.dock;

import javafx.event.EventHandler;
import javafx.scene.Node;

/**
 * A wrapper to the type parameterized generic EventHandler that allows us to
 * remove it from its listener when the dock node becomes detached. It is
 * specifically used to monitor which dock node in this dock pane's layout we
 * are currently dragging over.
 *
 * @since DockFX 0.1
 */
public class DockNodeEventHandler implements EventHandler<DockEvent> {

  private DockPane dockPane;
  /**
   * The node associated with this event handler that reports to the encapsulating
   * dock pane.
   */
  private final Node node;

  /**
   * Creates a default dock node event handler that will help this dock pane track
   * the current docking area.
   *
   * @param node
   *            The node that is to listen for docking events and report to the
   *            encapsulating docking pane.
   */
  public DockNodeEventHandler(DockPane dockPane, Node node) {
    this.dockPane = dockPane;
    this.node = node;
  }

  @Override
  public void handle(DockEvent event) {
    dockPane.setDockNodeDrag(node);
  }
}
