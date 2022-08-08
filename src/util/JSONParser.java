package util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class JSONParser {
  String path, fileName;
  File file;
  FileWriter fileWriter;

  public JSONParser() {
    path = Variables.PATH + "/solutions/json/";
    fileName = Variables.INSTANCE + ".json";
    createFile();
    createFileWriter();
  }

  public void createFile() {
    try {
      file = new File(path + fileName);
      int i = 1;
      while (!file.createNewFile()) {
        fileName = Variables.INSTANCE + "_" + i + ".json";
        file = new File(path + fileName);
        i++;
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void createFileWriter() {
    try {
      fileWriter = new FileWriter(file);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void write(String text) {
    try {
      fileWriter.write(text);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}

