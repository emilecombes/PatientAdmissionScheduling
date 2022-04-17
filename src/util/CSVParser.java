package util;

import model.Solver;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class CSVParser {
  private String outputPath = "./out/solutions/csv/";
  private String instance;
  private Solver solver;

  public CSVParser(String instance, Solver solver) {
    this.instance = instance;
    this.solver = solver;
  }

  public void buildMoveInfoCSV() throws IOException {
    File file = new File(outputPath + instance + "_move_info.csv");
    if (file.exists()) file.delete();
    file.createNewFile();

    FileWriter writer = new FileWriter(file);
    for (String info : solver.getMoveInfo())
      writer.write(info);
    writer.flush();
    writer.close();

  }
}