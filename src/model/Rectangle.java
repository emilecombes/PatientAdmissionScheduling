package model;

public class Rectangle implements Comparable<Rectangle> {
  private Point ul, lr;
  int c;
  float area;

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
    c = (int) Math.round(getBottom() + 0.5 * (getTop() - getBottom()));
  }

  public void setBottom(int b) {
    lr = new Point(lr.x, b);
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

  public boolean isDominatedBy(Solution s) {
    return s.getPatientCost() <= getLeft() && s.getEquityCost() <= getBottom();
  }

  @Override
  public int compareTo(Rectangle other) {
    return (int) ((other.area - area) / 1000);
  }
}
