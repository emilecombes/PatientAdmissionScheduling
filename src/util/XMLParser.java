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
  private final String inputInstance;
  private Document document;

  public XMLParser(String instance) {
    inputInstance = instance;
    String inputFile = "data/Instances/" + inputInstance + ".xml";
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      DocumentBuilder builder = factory.newDocumentBuilder();
      document = builder.parse(new File(inputFile));
      document.getDocumentElement().normalize();
    } catch (Exception e) {
      System.out.println("Something went wrong...");
    }
  }

  public void buildDateConverter(int extend) {
    Element descriptor = (Element) document.getElementsByTagName("descriptor").item(0);
    Node horizon = descriptor.getElementsByTagName("Horizon").item(0);
    int nDays = Integer.parseInt(
        horizon.getAttributes().getNamedItem("num_days").getTextContent()
    );
    String startDay = horizon.getAttributes().getNamedItem("start_day").getTextContent();
    new DateConverter(startDay, nDays, extend);
  }

  public RoomList buildRoomList() {
    Node roomsNode = document.getElementsByTagName("rooms").item(0);
    NodeList roomsNodeList = roomsNode.getChildNodes();
    List<Room> rooms = new LinkedList<>();
    int index = 0;
    for (int i = 0; i < roomsNodeList.getLength(); i++) {
      Node roomNode = roomsNodeList.item(i);
      if (roomNode.getNodeType() == Node.ELEMENT_NODE) {
        Element roomElement = (Element) roomNode;

        NodeList featureNodes = roomElement.getElementsByTagName("feature");
        Set<String> features = new HashSet<>();
        for (int j = 0; j < featureNodes.getLength(); j++) {
          Node featureNode = featureNodes.item(j);
          if (featureNode.getNodeType() == Node.ELEMENT_NODE)
            features.add(featureNode.getTextContent());
        }

        Room room = new Room(
            index++,
            roomElement.getAttribute("name"),
            Integer.parseInt(roomElement.getAttribute("capacity")),
            roomElement.getAttribute("gender_policy"),
            roomElement.getAttribute("department"),
            features
        );

        rooms.add(room);
      }
    }

    NodeList departmentNodes = document.getElementsByTagName("departments").item(0).getChildNodes();
    List<String> departments = new ArrayList<>();
    for (int i = 0; i < departmentNodes.getLength(); i++) {
      if (departmentNodes.item(i).getNodeType() == Node.ELEMENT_NODE) {
        Element departmentElement = (Element) departmentNodes.item(i);
        departments.add(departmentElement.getAttribute("name"));
      }
    }

    RoomList roomList = new RoomList(rooms, departments);

    // Add department specialisms
    for (int i = 0; i < departmentNodes.getLength(); i++) {
      if (departmentNodes.item(i).getNodeType() == Node.ELEMENT_NODE) {
        Element departmentElement = (Element) departmentNodes.item(i);
        String department = departmentElement.getAttribute("name");
        NodeList mainSpecs = departmentElement.getElementsByTagName("main_specialism");
        NodeList auxSpecs = departmentElement.getElementsByTagName("aux_specialism");
        for (int j = 0; j < mainSpecs.getLength(); j++)
          if (mainSpecs.item(j).getNodeType() == Node.ELEMENT_NODE)
            roomList.addMainSpecialism(department, mainSpecs.item(j).getTextContent());
        for (int j = 0; j < auxSpecs.getLength(); j++)
          if (auxSpecs.item(j).getNodeType() == Node.ELEMENT_NODE)
            roomList.addAuxSpecialism(department, auxSpecs.item(j).getTextContent());
      }
    }

    // Add treatment - specialism mapping
    NodeList treatmentNodes = document.getElementsByTagName("treatments").item(0).getChildNodes();
    for (int i = 0; i < treatmentNodes.getLength(); i++) {
      if (treatmentNodes.item(i).getNodeType() == Node.ELEMENT_NODE) {
        Element treatmentElement = (Element) treatmentNodes.item(i);
        roomList.addSpecialism(
            treatmentElement.getAttribute("name"),
            treatmentElement.getAttribute("specialism")
        );
      }
    }

    return roomList;
  }

  public PatientList buildPatientList() {
    Node patientsNode = document.getElementsByTagName("patients").item(0);
    NodeList patientsNodeList = patientsNode.getChildNodes();
    List<Patient> patients = new ArrayList<>();
    int index = 0;
    for (int i = 0; i < patientsNodeList.getLength(); i++) {
      Node patientNode = patientsNodeList.item(i);
      if (patientNode.getNodeType() == Node.ELEMENT_NODE) {
        Element patientElement = (Element) patientNode;

        Set<String> neededProps = new HashSet<>();
        Set<String> preferredProps = new HashSet<>();
        NodeList propList = patientElement.getElementsByTagName("room_property");
        for (int j = 0; j < propList.getLength(); j++) {
          Node propertyNode = propList.item(j);
          if (propertyNode.getNodeType() == Node.ELEMENT_NODE) {
            Element propertyElement = (Element) propertyNode;
            String name = propertyElement.getAttribute("name");
            String type = propertyElement.getAttribute("type");
            if (type.equals("preferred")) preferredProps.add(name);
            else neededProps.add(name);
          }
        }

        String pc = patientElement.getAttribute("preferred_capacity");
        int preferredCapacity = (pc.equals("")) ? -1 : Integer.parseInt(pc);
        Patient patient = new Patient(
            index++,
            patientElement.getAttribute("name"),
            patientElement.getAttribute("gender"),
            patientElement.getAttribute("treatment"),
            DateConverter.getDateIndex(patientElement.getAttribute("admission")),
            DateConverter.getDateIndex(patientElement.getAttribute("discharge")),
            DateConverter.getDateIndex(patientElement.getAttribute("max_admission")),
            preferredCapacity,
            neededProps,
            preferredProps
        );

        if (!patientElement.getAttribute("room").isEmpty()) {
          int roomIndex = RoomList.getRoomIndex(patientElement.getAttribute("room"));
          for (int day = patient.getAdmission(); day < patient.getDischarge(); day++) {
            patient.setInitial();
            patient.assignRoom(roomIndex, day);
          }
        }

        patients.add(patient);
      }
    }

    return new PatientList(patients);
  }

  public void writeSolution(Solver solver) {
    String outputFile = "out/solutions/" + inputInstance + "_sol.xml";
    PatientList patientList = solver.getPatientList();
    RoomList roomList = solver.getRoomList();

    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder = null;
    try {
      builder = factory.newDocumentBuilder();
    } catch (Exception e) {
      System.out.println("Something went wrong...");
    }
    assert builder != null;
    Document doc = builder.newDocument();
    Element root = doc.createElement("OrPasu_main_out");
    doc.appendChild(root);

    // Write planning horizon
    HashMap<String, String> planningHorizon = solver.getPlanningHorizon();
    Element planningHorizonElement = doc.createElement("planning_horizon");
    root.appendChild(planningHorizonElement);
    for (String key : planningHorizon.keySet()) {
      Element element = doc.createElement(key);
      element.setTextContent(planningHorizon.get(key));
      planningHorizonElement.appendChild(element);
    }

    // Write patients scheduling
    Element patientsElement = doc.createElement("patients_scheduling");
    root.appendChild(patientsElement);
    for (int i = 0; i < patientList.getNumberOfPatients(); i++) {
      Patient p = patientList.getPatient(i);
      Element patientElement = doc.createElement("patient");
      patientElement.setAttribute("name", p.getName());
      patientElement.setAttribute("delay", String.valueOf(p.getDelay()));
      patientElement.setAttribute("status", p.getStatus());

      for (int j = p.getAdmission(); j < p.getDischarge(); j++) {
        Element stayElement = doc.createElement("stay");
        stayElement.setAttribute("day", DateConverter.getDateString(j));
        stayElement.setAttribute("room", roomList.getRoom(p.getRoom(j)).getName());
        patientElement.appendChild(stayElement);
      }
      patientsElement.appendChild(patientElement);
    }

    // Write costs
    Element costsElement = doc.createElement("costs");
    root.appendChild(costsElement);
    int capacityViolations = solver.getCapacityViolations();
    int transfer = solver.getTransferCost();
    int delay = solver.getDelayCost();

    // Write to file
    try (FileOutputStream output = new FileOutputStream(outputFile)) {
      writeXml(doc, output);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static void writeXml(Document doc, OutputStream output) throws TransformerException {
    TransformerFactory transformerFactory = TransformerFactory.newInstance();
    Transformer transformer = transformerFactory.newTransformer();
    DOMSource source = new DOMSource(doc);
    StreamResult result = new StreamResult(output);
    transformer.transform(source, result);
  }

}