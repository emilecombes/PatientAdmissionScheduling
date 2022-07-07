package model;

public class Point implements Comparable<Point> {
  int x, y;

  public Point(int x, int y) {
    this.x = x;
    this.y = y;
  }

  @Override
  public int compareTo(Point other) {
    return x - other.x;
  }
}