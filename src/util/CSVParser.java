package util;

import model.Patient;
import model.PatientList;
import model.Solver;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

public class CSVParser {
  private final Solver solver;

  public CSVParser(Solver solver) {
    this.solver = solver;
  }

  public void buildMoveInfoCSV() throws IOException {
    File file = new File(Variables.PATH + "/solutions/csv/" + Variables.INSTANCE +
        "_sol_move_info.csv");
    if (file.exists()) file.delete();
    file.createNewFile();

    FileWriter writer = new FileWriter(file);
    for (String info : solver.getMoveInfo())
      writer.write(info);
    writer.flush();
    writer.close();
  }

  public void buildScheduleCSV() throws IOException {
    File file = new File(Variables.PATH + "/solutions/csv/" + Variables.INSTANCE +
        "_schedule.csv");
    if (file.exists()) file.delete();
    file.createNewFile();

    FileWriter writer = new FileWriter(file);
    solver.setPatientBedNumbers();

    writer.write("Id,Name,Gender,Treatment,Admission,Discharge,Delay,Department,MainSpecialism," +
        "Room,RoomCost,Bed,RoomCode\n");
    for (int i = 0; i < PatientList.getNumberOfPatients(); i++){
      Map<String, String> info = PatientList.getPatient(i).getInfo();
      String sb = info.get("Id") + "," +
          "\"" + info.get("Name") + "\"," +
          "\"" + info.get("Gender") + "\"," +
          "\"" + info.get("Treatment") + "\"," +
          "\"" + info.get("Admission") + "\"," +
          "\"" + info.get("Discharge") + "\"," +
          info.get("Delay") + "," +
          "\"" + info.get("Department") + "\"," +
          info.get("MainSpecialism") + "," +
          "\"" + info.get("Room") + "\"," +
          info.get("RoomCost") + "," +
          "\"" + info.get("Bed") + "\"," +
          "\"" + info.get("Room") + "_" + info.get("Bed") + "\"\n";

      writer.write(sb);
    }
    writer.flush();
    writer.close();
  }
}