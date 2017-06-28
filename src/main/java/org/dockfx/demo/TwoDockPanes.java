/**
 * @file DockFX.java
 * @brief Driver demonstrating basic dock layout with prototypes. Maintained in a separate package
 *        to ensure the encapsulation of org.dockfx private package members.
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

package org.dockfx.demo;


import org.dockfx.dock.DockNode;
import org.dockfx.dock.DockPane;
import org.dockfx.dock.DockPos;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToolBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * This app creates two dock panes, one over the other. Nodes can be added to either.
 * If dock pane A is "exclusive", then A will ignore nodes from B (because A is 
 * exclusive and won't accept nodes from any other dockpane, and B will ignore nodes
 * from A because A is exclusive and won't let go of them.
 * 
 * If neither A or B is exclusive, Issue #24 from RobertBColton/DockFX will occur. 
 * @author will
 */
public class TwoDockPanes extends Application {

  public static void main(String[] args) {
    launch(args);
  }

  private VBox vbox;
  private DockPane dp1;
  private DockPane dp2;
  private int counter = 0;
  private final Image dockImage = 
    new Image(DockFX.class.getResource("docknode.png").toExternalForm());

  @Override
  public void start(Stage primaryStage) {
    primaryStage.setTitle("DockFX");
   
    vbox = new VBox();

    dp1 = makeDockPane("A");
    dp2 = makeDockPane("B");

    dp1.setExclusive(true);
    

    primaryStage.setScene(new Scene(vbox, 800, 500));
    primaryStage.sizeToScene();
    primaryStage.show();

    // test the look and feel with both Caspian and Modena
    Application.setUserAgentStylesheet(Application.STYLESHEET_MODENA);
  }

  private DockPane makeDockPane(String name) {
    ToolBar bar = new ToolBar();
    Label lab = new Label(name);
    Button addButton = new Button("Add");
    bar.getItems().add(lab);
    bar.getItems().add(addButton);

    DockPane dp = new DockPane();
    VBox.setVgrow(dp, Priority.ALWAYS);

    addButton.setOnAction(evt -> addNode(dp, name));

    vbox.getChildren().add(bar);
    vbox.getChildren().add(dp);

    return dp;
  }

  private void addNode(DockPane dp, String dockName) {
    ++counter;
    String title = dockName + "Node " + counter;
    TextArea ta = new TextArea();
    ta.setText(title + "\n\nJust some test data"); 
    DockNode dn = new DockNode(ta, title, new ImageView(dockImage));
    dp.dock(dn, DockPos.BOTTOM);
  }


}
