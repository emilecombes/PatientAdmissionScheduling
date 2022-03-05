import model.Scheduler;
import util.XMLParser;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

public class Main {
  public static void main(String[] args) throws ParserConfigurationException, TransformerException {
    String instance = "data/or-pas/Instances/or_pas_dept2_short01.xml";
    XMLParser xmlParser = new XMLParser(instance);
    Scheduler scheduler = xmlParser.buildScheduler();

    scheduler.dynamicSolve();
    xmlParser.writeFile("yolo", scheduler);
  }
}
