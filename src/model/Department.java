package model;

import java.util.Set;

public class Department {
  private String name;
  private Set<String> mainSpecialisms;
  private Set<String> auxSpecialisms;

  private Set<Room> rooms;

  public Department(String n){
    name = n;
  }

  public void setMainSpecialisms(Set<String> mainSpecialisms) {
    this.mainSpecialisms = mainSpecialisms;
  }

  public void setAuxSpecialisms(Set<String> auxSpecialisms) {
    this.auxSpecialisms = auxSpecialisms;
  }

  public String getName(){
    return name;
  }

  public boolean hasMainSpecialism(String specialism){
    return mainSpecialisms.contains(specialism);
  }

  public boolean hasAuxSpecialism(String specialism){
    return auxSpecialisms.contains(specialism);
  }
}
