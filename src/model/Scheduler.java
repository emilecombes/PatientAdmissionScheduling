package model;

import java.util.List;
import java.util.Set;

public class Scheduler {
  private int numberOfDepartments, numberOfRooms, numberOfFeatures,
      numberOfPatients, numberOfSpecialisms, numberOfTreatments, numberOfDays;
  private String startDay;
  private List<String> specialisms;
  private List<String> roomFeatures;
  private List<Department> departments;
  private List<Room> rooms;
  private List<Treatment> treatments;
  private List<Patient> patients;

  private Set<Patient>[][] schedule;

  public Scheduler(){}

  public void setNumberOfDepartments(int numberOfDepartments) {
    this.numberOfDepartments = numberOfDepartments;
  }

  public void setNumberOfRooms(int numberOfRooms) {
    this.numberOfRooms = numberOfRooms;
  }

  public void setNumberOfFeatures(int numberOfFeatures) {
    this.numberOfFeatures = numberOfFeatures;
  }

  public void setNumberOfPatients(int numberOfPatients) {
    this.numberOfPatients = numberOfPatients;
  }

  public void setNumberOfSpecialisms(int numberOfSpecialisms) {
    this.numberOfSpecialisms = numberOfSpecialisms;
  }

  public void setNumberOfTreatments(int numberOfTreatments) {
    this.numberOfTreatments = numberOfTreatments;
  }

  public void setNumberOfDays(int numberOfDays) {
    this.numberOfDays = numberOfDays;
  }

  public void setStartDay(String startDay) {
    this.startDay = startDay;
  }

  public void setSpecialisms(List<String> specialisms) {
    this.specialisms = specialisms;
  }

  public void setRoomFeatures(List<String> roomFeatures) {
    this.roomFeatures = roomFeatures;
  }

  public void setDepartments(List<Department> departments) {
    this.departments = departments;
  }

  public void setRooms(List<Room> rooms) {
    this.rooms = rooms;
  }

  public void setTreatments(List<Treatment> treatments) {
    this.treatments = treatments;
  }

  public void setPatients(List<Patient> patients) {
    this.patients = patients;
  }

  public Department getDepartment(String name){
    for(Department d : departments){
      if(d.getName().equals(name)) return d;
    }
    return null;
  }

  public Treatment getTreatment(String name){
    for(Treatment t : treatments){
      if(t.getName().equals(name)) return t;
    }
    return null;
  }

  public void makeInitialPlanning(){
    System.out.println("Hello");
  }
}
