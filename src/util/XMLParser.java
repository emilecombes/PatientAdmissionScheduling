package util;

import model.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class XMLParser {
  private String inputFile;
  private DocumentBuilderFactory factory;
  private DocumentBuilder builder;
  private Document document;

  public XMLParser(String inputFile) {
    this.inputFile = inputFile;
    try {
      factory = DocumentBuilderFactory.newInstance();
      builder = factory.newDocumentBuilder();
      document = builder.parse(new File(inputFile));
      document.getDocumentElement().normalize();
    } catch (Exception e) {
      System.out.println("Something went wrong...");
    }
  }

  public Scheduler buildScheduler() {
    Scheduler scheduler = new Scheduler();
    readDescriptor(scheduler);
    readSpecialisms(scheduler);
    readFeatures(scheduler);
    readDepartments(scheduler);
    readRooms(scheduler);
    readTreatments(scheduler);
    readPatients(scheduler);
    return scheduler;
  }

  public void readDescriptor(Scheduler scheduler) {
    Element descriptor = (Element) document.getElementsByTagName("descriptor").item(0);
    int nDepartments = Integer.parseInt(
        descriptor.getElementsByTagName("Departments").item(0).getTextContent()
    );
    scheduler.setNumberOfDepartments(nDepartments);
    int nRooms = Integer.parseInt(
        descriptor.getElementsByTagName("Rooms").item(0).getTextContent()
    );
    scheduler.setNumberOfRooms(nRooms);
    int nFeatures = Integer.parseInt(
        descriptor.getElementsByTagName("Features").item(0).getTextContent()
    );
    scheduler.setNumberOfFeatures(nFeatures);
    int nPatients = Integer.parseInt(
        descriptor.getElementsByTagName("Patients").item(0).getTextContent()
    );
    scheduler.setNumberOfPatients(nPatients);
    int nSpecialisms = Integer.parseInt(
        descriptor.getElementsByTagName("Specialisms").item(0).getTextContent()
    );
    scheduler.setNumberOfSpecialisms(nSpecialisms);
    int nTreatments = Integer.parseInt(
        descriptor.getElementsByTagName("Treatments").item(0).getTextContent()
    );
    scheduler.setNumberOfTreatments(nTreatments);
    Node horizon = descriptor.getElementsByTagName("Horizon").item(0);
    int nDays = Integer.parseInt(
        horizon.getAttributes().getNamedItem("num_days").getTextContent()
    );
    scheduler.setNumberOfDays(nDays);
    String startDay = horizon.getAttributes().getNamedItem("start_day").getTextContent();
    scheduler.setStartDay(startDay);
  }

  public void readSpecialisms(Scheduler scheduler) {
    Node specialismsNode = document.getElementsByTagName("specialisms").item(0);
    NodeList nodeList = specialismsNode.getChildNodes();
    List<String> specialisms = new LinkedList<>();
    for (int i = 0; i < nodeList.getLength(); i++) {
      Node node = nodeList.item(i);
      if (node.getNodeType() == Node.ELEMENT_NODE) {
        Element element = (Element) node;
        specialisms.add(element.getTextContent());
      }
    }
    scheduler.setSpecialisms(specialisms);
  }

  public void readFeatures(Scheduler scheduler) {
    Node featuresNode = document.getElementsByTagName("features").item(0);
    NodeList nodeList = featuresNode.getChildNodes();
    List<String> roomFeatures = new LinkedList<>();
    for (int i = 0; i < nodeList.getLength(); i++) {
      Node node = nodeList.item(i);
      if (node.getNodeType() == Node.ELEMENT_NODE) {
        roomFeatures.add(node.getTextContent());
      }
    }
    scheduler.setRoomFeatures(roomFeatures);
  }

  public void readDepartments(Scheduler scheduler) {
    Node departmentsNode = document.getElementsByTagName("departments").item(0);
    NodeList nodeList = departmentsNode.getChildNodes();
    List<Department> departments = new LinkedList<>();
    for (int i = 0; i < nodeList.getLength(); i++) {
      Node node = nodeList.item(i);
      if (node.getNodeType() == Node.ELEMENT_NODE) {
        Element element = (Element) node;
        Department department = new Department(element.getAttribute("name"));

        NodeList mainSecialismNodes =
            ((Element) node).getElementsByTagName("main_specialism");
        Set<String> mainSpecialisms = new HashSet<>();
        for (int j = 0; j < mainSecialismNodes.getLength(); j++) {
          Node mainSpecialismNode = mainSecialismNodes.item(j);
          if (mainSpecialismNode.getNodeType() == Node.ELEMENT_NODE) {
            mainSpecialisms.add(mainSpecialismNode.getTextContent());
          }
        }
        department.setMainSpecialisms(mainSpecialisms);

        NodeList auxSecialismNodes =
            ((Element) node).getElementsByTagName("aux_specialism");
        Set<String> auxSpecialisms = new HashSet<>();
        for (int j = 0; j < auxSecialismNodes.getLength(); j++) {
          Node mainSpecialismNode = auxSecialismNodes.item(j);
          if (mainSpecialismNode.getNodeType() == Node.ELEMENT_NODE) {
            auxSpecialisms.add(mainSpecialismNode.getTextContent());
          }
        }
        department.setAuxSpecialisms(auxSpecialisms);

        departments.add(department);
      }
    }
    scheduler.setDepartments(departments);
  }

  public void readRooms(Scheduler scheduler) {
    Node roomsNode = document.getElementsByTagName("rooms").item(0);
    NodeList roomsNodeList = roomsNode.getChildNodes();
    List<Room> rooms = new LinkedList<>();
    for (int i = 0; i < roomsNodeList.getLength(); i++) {
      Node roomNode = roomsNodeList.item(i);
      if (roomNode.getNodeType() == Node.ELEMENT_NODE) {
        Element roomElement = (Element) roomNode;
        Room room = new Room(
            roomElement.getAttribute("name"),
            Integer.parseInt(roomElement.getAttribute("capacity")),
            roomElement.getAttribute("gender_policy")
        );
        room.setDepartment(scheduler.getDepartment(
            roomElement.getAttribute("department")
        ));

        NodeList featureNodes = roomElement.getElementsByTagName("feature");
        Set<String> features = new HashSet<>();
        for (int j = 0; j < featureNodes.getLength(); j++) {
          Node featureNode = featureNodes.item(j);
          if (featureNode.getNodeType() == Node.ELEMENT_NODE) {
            features.add(featureNode.getTextContent());
          }
        }
        room.setRoomFeatures(features);
        rooms.add(room);
      }
    }
    scheduler.setRooms(rooms);
  }

  public void readTreatments(Scheduler scheduler) {
    Node treatmentsNode = document.getElementsByTagName("treatments").item(0);
    NodeList treatmentsNodeList = treatmentsNode.getChildNodes();
    List<Treatment> treatments = new ArrayList<>();
    for (int i = 0; i < treatmentsNodeList.getLength(); i++) {
      Node treatmentNode = treatmentsNodeList.item(i);
      if (treatmentNode.getNodeType() == Node.ELEMENT_NODE) {
        Element treatmentElement = (Element) treatmentNode;
        Treatment treatment = new Treatment(
            treatmentElement.getAttribute("name"),
            treatmentElement.getAttribute("specialism")
        );
        treatments.add(treatment);
      }
    }
    scheduler.setTreatments(treatments);
  }

  public void readPatients(Scheduler scheduler) {
    Node patientsNode = document.getElementsByTagName("patients").item(0);
    NodeList patientsNodeList = patientsNode.getChildNodes();
    List<Patient> patients = new ArrayList<>();
    for (int i = 0; i < patientsNodeList.getLength(); i++){
      Node patientNode = patientsNodeList.item(i);
      if(patientNode.getNodeType() == Node.ELEMENT_NODE){
        Element patientElement = (Element) patientNode;
        Patient patient = new Patient(
            patientElement.getAttribute("name"),
            Integer.parseInt(patientElement.getAttribute("age")),
            patientElement.getAttribute("gender"),
            patientElement.getAttribute("registration"),
            patientElement.getAttribute("admission"),
            patientElement.getAttribute("discharge"),
            Integer.parseInt(patientElement.getAttribute("variability")),
            patientElement.getAttribute("max_admission"),
            patientElement.getAttribute("preferred_capacity")
        );
        patient.setTreatment(scheduler.getTreatment(
            patientElement.getAttribute("treatment")
        ));

        Set<String> preferredProperties = new HashSet<>();
        Set<String> neededProperties = new HashSet<>();
        NodeList propertyNodeList = patientElement.getElementsByTagName(
            "room_property");
        for(int j = 0; j < propertyNodeList.getLength(); j++){
          Node propertyNode = propertyNodeList.item(j);
          if(propertyNode.getNodeType() == Node.ELEMENT_NODE){
            Element propertyElement = (Element) propertyNode;
            String name = propertyElement.getAttribute("name");
            String type = propertyElement.getAttribute("type");
            if(type.equals("preferred")) preferredProperties.add(name);
            else neededProperties.add(name);
          }
          patients.add(patient);
        }
        patient.setPreferredRoomProperties(preferredProperties);
        patient.setNeededRoomProperties(neededProperties);
      }
    }
    scheduler.setPatients(patients);
  }

}
