package model;

import java.util.ArrayList;
import java.util.List;

public class PatientList {
  private final List<Patient> patients;
  private final List<Patient> initialPatients;
  private final List<Patient> registeredPatients;

  public PatientList(List<Patient> patients) {
    this.patients = patients;
    initialPatients = new ArrayList<>();
    registeredPatients = new ArrayList<>();
    setInitialPatients();
  }

  public void setInitialPatients() {
    for (Patient p : patients)
      if (p.isInitial()) initialPatients.add(p);
      else registeredPatients.add(p);
  }

  public List<Patient> getInitialPatients() {
    return initialPatients;
  }

  public List<Patient> getRegisteredPatients() {
    return registeredPatients;
  }

  public int getNumberOfPatients() {
    return patients.size();
  }

  public Patient getPatient(int i) {
    return patients.get(i);
  }

  public Patient getRandomPatient() {
    // TODO
    return patients.get(0);
  }

  public Patient getRandomShiftPatient() {
    // TODO don't return initial patients
    return null;
  }

}
