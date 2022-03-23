package util;

import model.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class CSVParser {
  private final String instanceName;
  private final List<Room> rooms;
  private final List<String> dates;
  private List<Patient> patients;
  private PrintWriter pw;

  public CSVParser(String instanceName, List<Room> rooms, List<String> dates) {
    this.instanceName = instanceName;
    this.rooms = rooms;
    this.dates = dates;
  }

  public void setPatients(List<Patient> patients) {
    this.patients = patients;
  }

  public void createGanttChart() {
    String path = "out/csv" + instanceName + "-GanttChart.csv";
    try {
      Files.deleteIfExists(Path.of(path));
      pw = new PrintWriter(path);
      pw.println("Name,Pathology,Admission,Discharge");
      for (Patient p : patients) {
        pw.println(p.getName() + "," + p.getTreatment() + "," +
            p.getActualAdmission() + "," + p.getActualDischarge()
        );
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}
