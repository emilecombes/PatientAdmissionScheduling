package util;

import model.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.util.*;

public class XMLParser {
  private final String inputFile;
  private DocumentBuilderFactory factory;
  private DocumentBuilder builder;
  private Document document;

  public XMLParser(String inputFile) {
    this.inputFile = "data/Instances/" + inputFile + ".xml";
    try {
      factory = DocumentBuilderFactory.newInstance();
      builder = factory.newDocumentBuilder();
      document = builder.parse(new File(this.inputFile));
      document.getDocumentElement().normalize();
    } catch (Exception e) {
      System.out.println("Something went wrong...");
    }
  }

  public void buildDateConverter(int extend){
    Element descriptor = (Element) document.getElementsByTagName("descriptor").item(0);
    Node horizon = descriptor.getElementsByTagName("Horizon").item(0);
    int nDays = Integer.parseInt(
        horizon.getAttributes().getNamedItem("num_days").getTextContent()
    );
    String startDay = horizon.getAttributes().getNamedItem("start_day").getTextContent();
    DateConverter dateConverter = new DateConverter(startDay, nDays, extend);
  }

  public void buildPatientList(){
    Element patients = (Element) document.getElementsByTagName("patients").item(0);
  }
}