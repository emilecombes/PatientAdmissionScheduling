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
import java.util.stream.IntStream;

public class XMLParser {
  private String inputFile;
  private DocumentBuilderFactory factory;
  private DocumentBuilder builder;
  private Document document;

  public XMLParser(String inputFile) {
    this.inputFile = inputFile + ".xml";
    try {
      factory = DocumentBuilderFactory.newInstance();
      builder = factory.newDocumentBuilder();
      document = builder.parse(new File(this.inputFile));
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
    int nRooms = Integer.parseInt(
        descriptor.getElementsByTagName("Rooms").item(0).getTextContent()
    );
    int nFeatures = Integer.parseInt(
        descriptor.getElementsByTagName("Features").item(0).getTextContent()
    );
    int nPatients = Integer.parseInt(
        descriptor.getElementsByTagName("Patients").item(0).getTextContent()
    );
    int nSpecialisms = Integer.parseInt(
        descriptor.getElementsByTagName("Specialisms").item(0).getTextContent()
    );
    int nTreatments = Integer.parseInt(
        descriptor.getElementsByTagName("Treatments").item(0).getTextContent()
    );
    Node horizon = descriptor.getElementsByTagName("Horizon").item(0);
    int nDays = Integer.parseInt(
        horizon.getAttributes().getNamedItem("num_days").getTextContent()
    );
    String startDay = horizon.getAttributes().getNamedItem("start_day").getTextContent();
    scheduler.setDescription(nDepartments, nRooms, nFeatures, nPatients, nSpecialisms,
        nTreatments, nDays, startDay);
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
    HashMap<String, String> treatments = new HashMap<>();
    for (int i = 0; i < treatmentsNodeList.getLength(); i++) {
      Node treatmentNode = treatmentsNodeList.item(i);
      if (treatmentNode.getNodeType() == Node.ELEMENT_NODE) {
        Element treatmentElement = (Element) treatmentNode;
        treatments.put(
            treatmentElement.getAttribute("name"),
            treatmentElement.getAttribute("specialism")
        );
      }
    }
    scheduler.setTreatmentSpecialismMap(treatments);
  }

  public void readPatients(Scheduler scheduler) {
    Node patientsNode = document.getElementsByTagName("patients").item(0);
    NodeList patientsNodeList = patientsNode.getChildNodes();
    List<Patient> patients = new ArrayList<>();
    for (int i = 0; i < patientsNodeList.getLength(); i++) {
      Node patientNode = patientsNodeList.item(i);
      if (patientNode.getNodeType() == Node.ELEMENT_NODE) {
        Element patientElement = (Element) patientNode;

        String ma = patientElement.getAttribute("max_admission");
        int maxAdmission = (ma.equals("")) ? -1 : scheduler.getDateIndex(ma);
        String pc = patientElement.getAttribute("preferred_capacity");
        int preferredCapacity = (pc.equals("")) ? -1 : Integer.parseInt(pc);
        Patient patient = new Patient(
            patientElement.getAttribute("name"),
            Integer.parseInt(patientElement.getAttribute("age")),
            patientElement.getAttribute("gender"),
            scheduler.getDateIndex(patientElement.getAttribute("registration")),
            scheduler.getDateIndex(patientElement.getAttribute("admission")),
            scheduler.getDateIndex(patientElement.getAttribute("discharge")),
            Integer.parseInt(patientElement.getAttribute("variability")),
            maxAdmission,
            preferredCapacity,
            patientElement.getAttribute("treatment"),
            scheduler.getSpecialism(patientElement.getAttribute("treatment"))
        );

        if (!patientElement.getAttribute("room").isEmpty()) {
          int roomIndex = scheduler.getRoomIndex(patientElement.getAttribute("room"));
          for (int day = -1; day < patient.getDischargeDate(); day++) {
            patient.assignRoom(day, roomIndex);
          }
        }

        Set<String> preferredProperties = new HashSet<>();
        Set<String> neededProperties = new HashSet<>();
        NodeList propertyNodeList = patientElement.getElementsByTagName(
            "room_property");
        for (int j = 0; j < propertyNodeList.getLength(); j++) {
          Node propertyNode = propertyNodeList.item(j);
          if (propertyNode.getNodeType() == Node.ELEMENT_NODE) {
            Element propertyElement = (Element) propertyNode;
            String name = propertyElement.getAttribute("name");
            String type = propertyElement.getAttribute("type");
            if (type.equals("preferred")) preferredProperties.add(name);
            else neededProperties.add(name);
          }
        }
        patient.setPreferredRoomProperties(preferredProperties);
        patient.setNeededRoomProperties(neededProperties);
        patients.add(patient);
      }
    }

    List<List<Patient>> registeredPatients = new ArrayList<>();
    for (int i = 0; i < scheduler.getNDays(); i++) {
      List<Patient> patientList = new ArrayList<>();
      registeredPatients.add(patientList);
    }
    for (Patient patient : patients) {
      registeredPatients.get(patient.getRegistrationDate()).add(patient);
    }
    for (int i = registeredPatients.size() - 1; i >= 0; i--) {
      for (int j = 0; j < i; j++) {
        registeredPatients.get(i).addAll(registeredPatients.get(j));
      }
    }

    scheduler.setPatients(registeredPatients);
  }

  public void writeSolution(String title, Scheduler scheduler) {
    DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder docBuilder = null;
    try {
      docBuilder = docFactory.newDocumentBuilder();
    } catch (Exception e) {
      System.out.println("Something went wrong...");
    }

    // root element
    Document doc = docBuilder.newDocument();
    Element rootElement = doc.createElement("OrPasu_main_out");
    doc.appendChild(rootElement);

    // Dates
    List<String> dates = new ArrayList<>();
    GregorianCalendar date = scheduler.getStartDay();
    for (int i = 0; i < scheduler.getNDays(); i++) {
      dates.add(scheduler.getDateString(date));
      date.add(Calendar.DAY_OF_YEAR, 1);
    }

    // Planning Horizon
    Element planningElement = doc.createElement("planning_horizon");
    rootElement.appendChild(planningElement);
    Element startDay = doc.createElement("start_day");
    startDay.setTextContent(dates.get(0));
    planningElement.appendChild(startDay);
    Element numDays = doc.createElement("num_days");
    numDays.setTextContent(String.valueOf(dates.size()));
    planningElement.appendChild(numDays);
    Element currentDay = doc.createElement("current_day");
    currentDay.setTextContent(dates.get(dates.size() - 1));
    planningElement.appendChild(currentDay);

    // Patients Scheduling
    List<Patient> patients = scheduler.getAllPatients();
    List<Room> rooms = scheduler.getRooms();
    Element patientsElement = doc.createElement("patients_scheduling");
    rootElement.appendChild(patientsElement);
    for (Patient patient : patients) {
      Element patientElement = doc.createElement("patient");
      patientElement.setAttribute("name", patient.getName());

      for(int i : patient.getAssignedRooms().keySet()){
        if(patient.getAssignedRoom(i) != -1 && i != -1){
          Element stay = doc.createElement("stay");
          stay.setAttribute("day", dates.get(i));
          stay.setAttribute("room", rooms.get(patient.getAssignedRoom(i)).getName());
          patientElement.appendChild(stay);
        }
      }
      patientsElement.appendChild(patientElement);
    }

    // Costs
    Element costsElement = doc.createElement("costs");
    rootElement.appendChild(costsElement);
    int[] prc = scheduler.getPRCosts();
    int[] weights = {20, 10, 20, 50};
    int[] totals = prc.clone();
    for(int i = 0; i < prc.length; i++) totals[i] *= weights[i];
    String[] prViolations = {"properties", "preference", "specialism", "gender"};
    Element prElement = doc.createElement("patient_room");
    prElement.setAttribute("objectives", String.valueOf(IntStream.of(totals).sum()));
    for(int i = 0; i < prc.length; i++){
      Element el = doc.createElement(prViolations[i]);
      el.setAttribute("violations", String.valueOf(prc[i]));
      el.setAttribute("weight", String.valueOf(weights[i]));
      prElement.appendChild(el);
    }
    costsElement.appendChild(prElement);

    Element gElement = doc.createElement("gender");
    gElement.setTextContent(String.valueOf(scheduler.getGCost() * 50));
    costsElement.appendChild(gElement);

    Element tElement = doc.createElement("transfer");
    tElement.setTextContent(String.valueOf(scheduler.getTCost() * 100));
    costsElement.appendChild(tElement);

    Element dElement = doc.createElement("delay");
    dElement.setTextContent(String.valueOf(scheduler.getDCost()));
    costsElement.appendChild(dElement);


    // write dom document to a file
    try (FileOutputStream output = new FileOutputStream("./out/sol/" + title + "_sol.xml")) {
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
