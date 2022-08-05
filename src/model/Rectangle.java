package model;

public class Rectangle implements Comparable<Rectangle> {
  private Point ul, lr;
  int area, c;

  public Rectangle(Point ul, Point lr) {
    this.ul = ul;
    this.lr = lr;
    calculateArea();
    calculateC();
  }

  public void calculateArea() {
    area = (lr.x - ul.x) * (ul.y - lr.y);
  }

  public void calculateC() {
    c =  (int) Math.round(getBottom() + 0.5 * (getTop() - getBottom()));
  }

  public void setLr(Point p) {
    lr = p;
    calculateArea();
    calculateC();
  }

  public Point getUl() {
    return ul;
  }

  public Point getLr() {
    return lr;
  }

  public int getTop() {
    return ul.y;
  }

  public int getBottom() {
    return lr.y;
  }

  public int getRight() {
    return lr.x;
  }

  public int getLeft() {
    return ul.x;
  }

  @Override
  public int compareTo(Rectangle other) {
    return area - other.area;
  }
}
