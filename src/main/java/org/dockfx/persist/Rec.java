package org.dockfx.persist;

import java.io.Serializable;

/**
 * Created by jding on 14/06/2017.
 */
public class Rec implements Serializable{
  double x;
  double y;
  double width;
  double height;

  public double getX() {
    return x;
  }

  public void setX(double x) {
    this.x = x;
  }

  public double getY() {
    return y;
  }

  public void setY(double y) {
    this.y = y;
  }

  public double getWidth() {
    return width;
  }

  public void setWidth(double width) {
    this.width = width;
  }

  public double getHeight() {
    return height;
  }

  public void setHeight(double height) {
    this.height = height;
  }
}
