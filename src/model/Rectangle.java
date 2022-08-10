package model;

import util.Variables;

public class Rectangle implements Comparable<Rectangle> {
  private Point ul, lr;
  private int c, dl, il;
  float area;

  public Rectangle(Point ul, Point lr) {
    this.ul = ul;
    this.lr = lr;
    calculateArea();
    calculateProperties();
  }

  public void calculateArea() {
    area = (lr.x - ul.x) * (ul.y - lr.y);
  }

  public void calculateProperties() {
    c = (int) Math.round(getBottom() + 0.5 * (getTop() - getBottom()));
    int height = getTop() - getBottom();
    il = (int) (c - Variables.PENALTY_UPDATE_OFFSET * (c - getBottom()));
    dl = il - (int) (Variables.CONSTANT_PENALTY_ZONE * height);
  }

  public void setBottom(int b) {
    lr = new Point(lr.x, b);
    calculateArea();
    calculateProperties();
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

  public int getC() {
    return c;
  }

  public int getDecreaseLimit() {
    return dl;
  }

  public int getIncreaseLimit() {
    return il;
  }

  @Override
  public int compareTo(Rectangle other) {
    return (int) ((other.area - area) / 1000);
  }

  public String toString() {
    return "{\"area\":\"" + area +
        "\",\"x_1\":\"" + getLeft() + "\",\"x_2\":\"" + getRight() +
        "\",\"y_1\":\"" + getBottom() + "\",\"y_2\":\"" + getTop() + "\"},";
  }
}
