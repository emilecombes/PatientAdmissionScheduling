import model.Scheduler;
import util.XMLParser;

public class Main {
  public static void main(String[] args){
    String instance = "data/or-pas/Instances/or_pas_dept2_short01.xml";
    XMLParser xmlParser = new XMLParser(instance);
    Scheduler scheduler = xmlParser.buildScheduler();

    System.out.println(scheduler);
    scheduler.buildPenaltyMatrix();
    scheduler.makeInitialPlanning();
  }
}
