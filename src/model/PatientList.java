package model;

import java.util.ArrayList;
import java.util.List;

public class PatientList {
  private static List<Patient> patients;
  private static List<Patient> initialPatients;
  private static List<Patient> registeredPatients;

  public PatientList(List<Patient> patients) {
    PatientList.patients = patients;
    initialPatients = new ArrayList<>();
    registeredPatients = new ArrayList<>();
    setInitialPatients();
  }

  public static void setInitialPatients() {
    for (Patient p : patients)
      if (p.isInitial()) initialPatients.add(p);
      else registeredPatients.add(p);
  }

  public static List<Patient> getInitialPatients() {
    return initialPatients;
  }

  public static List<Patient> getRegisteredPatients() {
    return registeredPatients;
  }

  public static int getNumberOfPatients() {
    return patients.size();
  }

  public static Patient getPatient(int i) {
    return patients.get(i);
  }

  public static Patient getRandomPatient() {
    return patients.get((int) (Math.random() * getNumberOfPatients()));
  }

  public static Patient getRandomShiftPatient() {
    Patient pat;
    do pat = registeredPatients.get((int) (Math.random() * registeredPatients.size()));
    while (pat.getMaxDelay() == 0 && pat.getMaxAdvance() == 0);
    return pat;
  }

}
