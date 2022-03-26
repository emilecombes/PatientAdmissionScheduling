package model;

import java.util.List;

public class PatientList {
  private final List<Patient> patients;

  public PatientList(List<Patient> patients) {
    this.patients = patients;
  }

  public Patient getRandomPatient(){
    // TODO
    return patients.get(0);
  }

  public Patient getRandomShiftPatient(){
    // TODO don't return initial patients
    return null;
  }

}
