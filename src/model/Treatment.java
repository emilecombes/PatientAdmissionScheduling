package model;

public class Treatment {
  private String name;
  private String specialism;

  public Treatment(String n, String s){
    name = n;
    specialism = s;
  }

  public String getName(){
    return name;
  }
}
