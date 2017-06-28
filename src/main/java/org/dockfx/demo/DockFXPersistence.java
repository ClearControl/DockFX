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

import javafx.application.Application;
import javafx.beans.value.*;
import javafx.event.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.scene.web.HTMLEditor;
import javafx.stage.Stage;
import org.dockfx.dock.*;
import org.dockfx.persist.*;

import java.io.*;
import java.nio.file.*;
import java.util.Random;

public class DockFXPersistence extends Application {

  public static void main(String[] args) {
    launch(args);
  }
  private final Persistent persistent = new DefaultPersistent();
  @SuppressWarnings("unchecked")
  @Override
  public void start(final Stage primaryStage) {
    primaryStage.setTitle("DockFX");

    // create a dock pane that will manage our dock nodes and handle the layout
    DockPane dockPane = new DockPane();

    // create a default test node for the center of the dock area
    TabPane tabs = new TabPane();
    HTMLEditor htmlEditor = new HTMLEditor();
    try {
      htmlEditor.setHtmlText(new String(Files.readAllBytes(Paths.get("readme.html"))));
    } catch (IOException e) {
      e.printStackTrace();
    }

    // empty tabs ensure that dock node has its own background color when floating
    tabs.getTabs().addAll(new Tab("Tab 1", htmlEditor), new Tab("Tab 2"), new Tab("Tab 3"));

    TableView<String> tableView = new TableView<String>();
    // this is why @SupressWarnings is used above
    // we don't care about the warnings because this is just a demonstration
    // for docks not the table view
    tableView.getColumns().addAll(new TableColumn<String, String>("A"),
        new TableColumn<String, String>("B"), new TableColumn<String, String>("C"));

    // load an image to caption the dock nodes
    Image dockImage = new Image(DockFXPersistence.class.getResource("docknode.png").toExternalForm());

    // create and dock some prototype dock nodes to the middle of the dock pane
    // the preferred sizes are used to specify the relative size of the node
    // to the other nodes

    // we can use this to give our central content a larger area where
    // the top and bottom nodes have a preferred width of 300 which means that
    // when a node is docked relative to them such as the left or right dock below
    // they will have 300 / 100 + 300 (400) or 75% of their previous width
    // after both the left and right node's are docked the center docks end up with 50% of the width

    DockNode tabsDock = new DockNode(tabs, "Tabs Dock", new ImageView(dockImage));
    tabsDock.setPrefSize(300, 100);
    dockPane.dock(tabsDock, DockPos.TOP);

    DockNode tableDock = new DockNode(tableView, "Table");
    tableDock.setPrefSize(300, 100);
    dockPane.dock(tableDock, DockPos.BOTTOM);

    final BorderPane mainBorderPane = new BorderPane();

    MenuItem saveMenuItem = new MenuItem("Save");
    saveMenuItem.setOnAction(new EventHandler<ActionEvent>() {
      @Override
      public void handle(ActionEvent event) {
        if(dirExist(getUserDataDirectory()))
          try {
            persistent.save(primaryStage,getUserDataDirectory() + "dock.pref");
          } catch (Exception e) {
            e.printStackTrace();
          }
      }
    });

    MenuItem restoreMenuItem = new MenuItem("Restore");
    restoreMenuItem.setOnAction(new EventHandler<ActionEvent>() {
      @Override
      public void handle(ActionEvent event) {
        try {
          mainBorderPane.setCenter(persistent.load(getUserDataDirectory() + "dock.pref"));
        } catch (PersistanceException e) {
          e.printStackTrace();
        }
      }
    });

    Menu fileMenu = new Menu("File");
    MenuBar menuBar = new MenuBar(fileMenu);
    fileMenu.getItems().addAll(saveMenuItem, restoreMenuItem);

    mainBorderPane.setTop(menuBar);
    mainBorderPane.setCenter(dockPane);
    
    // show that overlays are relative to the docking area
//    mainBorderPane.setLeft(new AnchorPane(generateRandomTree()));
    
    primaryStage.setScene(new Scene(mainBorderPane, 800, 500));
    primaryStage.sizeToScene();

    primaryStage.show();

    // can be created and docked before or after the scene is created
    // and the stage is shown
    DockNode treeDock = new DockNode(generateRandomTree(), "Tree Dock1", new ImageView(dockImage));
    dockPane.dock(treeDock, DockPos.LEFT);
//    dockPane.dock(generateRandomTree(), DockPos.LEFT,"Tree Dock1");
//    treeDock = new DockNode(generateRandomTree(), "Tree Dock2", new ImageView(dockImage));
//    treeDock.setPrefSize(100, 100);
//    dockPane.dock(treeDock, DockPos.RIGHT);
    dockPane.dock(generateRandomTree(), DockPos.RIGHT,"tree dock2");

	// If you want to get notified when the docknode is closed. You can add ChangeListener to DockNode's closedProperty()
	treeDock.closedProperty().addListener( new ChangeListener< Boolean >()
	{
		@Override public void changed( ObservableValue< ? extends Boolean > observable, Boolean oldValue, Boolean newValue )
		{
			if(newValue)
				System.out.println("TreeDock(DockPos.RIGHT) is closed.");
		}
	} );

    // test the look and feel with both Caspian and Modena
    Application.setUserAgentStylesheet(Application.STYLESHEET_MODENA);
    // initialize the default styles for the dock pane and undocked nodes using the DockFX
    // library's internal Default.css stylesheet
    // unlike other custom control libraries this allows the user to override them globally
    // using the style manager just as they can with internal JavaFX controls
    // this must be called after the primary stage is shown
    // https://bugs.openjdk.java.net/browse/JDK-8132900

    // TODO: after this feel free to apply your own global stylesheet using the StyleManager class
  }

  private TreeView<String> generateRandomTree() {
    // create a demonstration tree view to use as the contents for a dock node
    TreeItem<String> root = new TreeItem<String>("Root");
    TreeView<String> treeView = new TreeView<String>(root);
    treeView.setShowRoot(false);

    // populate the prototype tree with some random nodes
    Random rand = new Random();
    for (int i = 4 + rand.nextInt(8); i > 0; i--) {
      TreeItem<String> treeItem = new TreeItem<String>("Item " + i);
      root.getChildren().add(treeItem);
      for (int j = 2 + rand.nextInt(4); j > 0; j--) {
        TreeItem<String> childItem = new TreeItem<String>("Child " + j);
        treeItem.getChildren().add(childItem);
      }
    }

    return treeView;
  }

  public static boolean dirExist(String dir)
  {
    String path = getUserDataDirectory();
    if(!new File(path).exists())
      return new File(path).mkdirs();
    else
      return true;
  }

  public static String getUserDataDirectory() {
    return System.getProperty("user.home") + File.separator + ".dockfx" + File.separator
           + getApplicationVersionString() + File.separator;
  }

  public static String getApplicationVersionString() {
    return "1.0";
  }

}
