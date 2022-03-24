import model.*;
import util.*;

public class Main {
  public static void main(String[] args){
    int extend = 14;
    String instance = "or_pas_dept2_short01";
    XMLParser xmlParser = new XMLParser(instance);
    xmlParser.buildDateConverter(extend);
  }
}
