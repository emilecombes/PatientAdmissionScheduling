import model.Scheduler;
import util.XMLParser;

public class Main {
  public static void main(String[] args){
    String instance = "or_pas_dept2_short01";
    XMLParser xmlParser = new XMLParser("data/or-pas/Instances/" + instance);
    Scheduler scheduler = xmlParser.buildScheduler();

    scheduler.dynamicSolve();
    xmlParser.writeSolution(instance, scheduler);
  }
}
