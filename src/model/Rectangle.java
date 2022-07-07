package model;

public class Rectangle {
  private Point upperLeft, lowerRight;

  public Rectangle(Point ul, Point lr) {
    upperLeft = ul;
    lowerRight = lr;
  }

  public void setLowerRight(Point p) {
    lowerRight = p;
  }

  public Point getUpperLeft() {
    return upperLeft;
  }

  public Point getLowerRight() {
    return lowerRight;
  }

  public int getTop() {
    return upperLeft.y;
  }

  public int getBottom() {
    return lowerRight.y;
  }

  public int getRight() {
    return lowerRight.x;
  }

  public int getLeft() {
    return upperLeft.x;
  }
}
