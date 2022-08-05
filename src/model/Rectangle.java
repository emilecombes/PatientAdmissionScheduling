package model;

public class Rectangle implements Comparable<Rectangle> {
  private Point upperLeft, lowerRight;
  int area;

  public Rectangle(Point ul, Point lr) {
    upperLeft = ul;
    lowerRight = lr;
    area = (lr.x - ul.x) * (ul.y - lr.y);
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

  public int getC() {
    return (int) (getBottom() + 0.5 * (getTop() - getBottom()));
  }

  @Override
  public int compareTo(Rectangle other) {
    return area - other.area;
  }
}
