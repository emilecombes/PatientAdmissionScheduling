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
  private Document document;

  public XMLParser() {
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      DocumentBuilder builder = factory.newDocumentBuilder();
      document = builder.parse(new File(
          Variables.PATH + "/Instances/" + Variables.INSTANCE + ".xml"));
      document.getDocumentElement().normalize();
    } catch (Exception e) {
      System.out.println("Something went wrong...");
    }
  }

  public void buildDateConverter() {
    Element descriptor = (Element) document.getElementsByTagName("descriptor").item(0);
    Node horizon = descriptor.getElementsByTagName("Horizon").item(0);
    int nDays = (int) (Variables.EXTEND * Integer.parseInt(
        horizon.getAttributes().getNamedItem("num_days").getTextContent()
    ));
    String startDay = horizon.getAttributes().getNamedItem("start_day").getTextContent();
    new DateConverter(startDay, nDays);
  }

  public void buildDepartmentList() {
    // Build rooms
    List<Room> rooms = new ArrayList<>();
    Node roomsNode = document.getElementsByTagName("rooms").item(0);
    NodeList roomsNodeList = roomsNode.getChildNodes();
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

    // Build departments
    Map<String, Map<Integer, Room>> departmentRooms = new HashMap<>();
    Map<String, Set<String>> mainSpecialisms = new HashMap<>();
    Map<String, Set<String>> auxSpecialisms = new HashMap<>();
    NodeList departmentNodes = document.getElementsByTagName("departments").item(0).getChildNodes();
    for (int i = 0; i < departmentNodes.getLength(); i++) {
      if (departmentNodes.item(i).getNodeType() == Node.ELEMENT_NODE) {
        Element departmentElement = (Element) departmentNodes.item(i);
        String name = departmentElement.getAttribute("name");
        departmentRooms.put(name, new HashMap<>());
        for (Room room : rooms)
          if (room.getDepartment().equals(name))
            departmentRooms.get(name).put(room.getId(), room);

        mainSpecialisms.put(name, new HashSet<>());
        NodeList mainSpecs = departmentElement.getElementsByTagName("main_specialism");
        for (int j = 0; j < mainSpecs.getLength(); j++)
          if (mainSpecs.item(j).getNodeType() == Node.ELEMENT_NODE)
            mainSpecialisms.get(name).add(mainSpecs.item(j).getTextContent());

        auxSpecialisms.put(name, new HashSet<>());
        NodeList auxSpecs = departmentElement.getElementsByTagName("aux_specialism");
        for (int j = 0; j < auxSpecs.getLength(); j++)
          if (auxSpecs.item(j).getNodeType() == Node.ELEMENT_NODE)
            auxSpecialisms.get(name).add(auxSpecs.item(j).getTextContent());
      }
    }

    // Build treatment - specialism mapping
    Map<String, String> treatmentToSpecialism = new HashMap<>();
    NodeList treatmentNodes = document.getElementsByTagName("treatments").item(0).getChildNodes();
    for (int i = 0; i < treatmentNodes.getLength(); i++) {
      if (treatmentNodes.item(i).getNodeType() == Node.ELEMENT_NODE) {
        Element treatmentElement = (Element) treatmentNodes.item(i);
        treatmentToSpecialism.put(
            treatmentElement.getAttribute("name"),
            treatmentElement.getAttribute("specialism")
        );
      }
    }

    // Build DepartmentList
    int id = 0;
    Map<String, Department> departments = new HashMap<>();
    for (String name : departmentRooms.keySet()) {
      departments.put(name, new Department(
          id++,
          departmentRooms.get(name),
          mainSpecialisms.get(name),
          auxSpecialisms.get(name))
      );
    }

    new DepartmentList(rooms, departments, treatmentToSpecialism);
  }

  public void buildPatientList() {
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
        int room = DepartmentList.getRoomIndex(patientElement.getAttribute("room"));
        Patient patient = new Patient(
            index++,
            patientElement.getAttribute("name"),
            patientElement.getAttribute("gender"),
            patientElement.getAttribute("treatment"),
            DateConverter.getDateIndex(patientElement.getAttribute("admission")),
            DateConverter.getDateIndex(patientElement.getAttribute("discharge")),
            DateConverter.getDateIndex(patientElement.getAttribute("max_admission")),
            room,
            preferredCapacity,
            neededProps,
            preferredProps
        );

        patients.add(patient);
      }
    }

    new PatientList(patients);
  }

  public void writeSolution(Solution sol) {
    String outputFile = Variables.PATH + "/solutions/xml/" + Variables.INSTANCE + ".xml";
    File file = new File(outputFile);
    int n = 0;
    while (file.exists()) {
      outputFile = Variables.PATH + "/solutions/xml/" + Variables.INSTANCE + "_" + n + ".xml";
      file = new File(outputFile);
    }
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
    Element planningHorizonElement = doc.createElement("planning_horizon");
    Element startDay = doc.createElement("start_day");
    startDay.setTextContent(DateConverter.getDateString(0));
    planningHorizonElement.appendChild(startDay);
    Element numDays = doc.createElement("num_days");
    startDay.setTextContent(String.valueOf(DateConverter.getTotalHorizon()));
    planningHorizonElement.appendChild(numDays);
    Element currentDay = doc.createElement("current_day");
    startDay.setTextContent(DateConverter.getDateString(0));
    planningHorizonElement.appendChild(currentDay);
    root.appendChild(planningHorizonElement);

    int dynamicGenderCosts = sol.getDynamicGenderViolations() * Variables.GENDER_PEN;
    int capacityViolations = sol.getCapacityViolations();
    int patientRoomCosts = 0;
    int propertyViolations = 0;
    int preferenceViolations = 0;
    int specialityViolations = 0;
    int genderViolations = 0;
    int transferCosts = 0;
    int delays = 0;
    Element patientsElement = doc.createElement("patients_scheduling");
    root.appendChild(patientsElement);
    for (int i = 0; i < PatientList.getNumberOfPatients(); i++) {
      Patient p = PatientList.getPatient(i);
      Room r = sol.getAssignedRoom(p);
      int delay = sol.getDelay(p);
      delays += delay;
      patientRoomCosts += p.getRoomCost(r.getId());
      propertyViolations +=
          p.getSpecificRoomCost(r.getId(), "room_property") / Variables.ROOM_PROP_PEN;
      specialityViolations +=
          p.getSpecificRoomCost(r.getId(), "speciality") / Variables.SPECIALITY_PEN;
      preferenceViolations +=
          p.getSpecificRoomCost(r.getId(), "capacity_preference") / Variables.PREF_CAP_PEN;
      genderViolations += p.getSpecificRoomCost(r.getId(), "gender") / Variables.GENDER_PEN;
      transferCosts += p.getSpecificRoomCost(r.getId(), "transfer");

      Element patientElement = doc.createElement("patient");
      patientElement.setAttribute("name", p.getName());
      patientElement.setAttribute("delay", String.valueOf(delay));
      patientElement.setAttribute("status", p.getStatus());

      for (int j = 0; j < p.getStayLength(); j++) {
        Element stayElement = doc.createElement("stay");
        stayElement.setAttribute("day", DateConverter.getDateString(p.getOriginalAD() + delay + j));
        stayElement.setAttribute("room", r.getName());
        patientElement.appendChild(stayElement);
      }
      patientsElement.appendChild(patientElement);
    }


    // Write costs
    Element costsElement = doc.createElement("costs");
    root.appendChild(costsElement);
    costsElement.setAttribute("violations", String.valueOf(capacityViolations));
    costsElement.setAttribute("time",
        String.valueOf((sol.getCreationTime() - Variables.START_TIME) * 0.001));
    costsElement.setAttribute("objectives",
        String.valueOf(sol.getPatientCost() - Variables.CAP_VIOL_PEN * capacityViolations));

    Element pr = doc.createElement("patient_room");
    costsElement.appendChild(pr);
    pr.setAttribute("value", String.valueOf(patientRoomCosts - transferCosts));

    Element roomProperties = doc.createElement("properties");
    pr.appendChild(roomProperties);
    roomProperties.setAttribute("violations", String.valueOf(propertyViolations));
    roomProperties.setAttribute("weight", String.valueOf(Variables.ROOM_PROP_PEN));

    Element roomPreference = doc.createElement("preference");
    pr.appendChild(roomPreference);
    roomPreference.setAttribute("violations", String.valueOf(preferenceViolations));
    roomPreference.setAttribute("weight", String.valueOf(Variables.PREF_CAP_PEN));

    Element roomSpecialisms = doc.createElement("specialism");
    pr.appendChild(roomSpecialisms);
    roomSpecialisms.setAttribute("violations", String.valueOf(specialityViolations));
    roomSpecialisms.setAttribute("weight", String.valueOf(Variables.SPECIALITY_PEN));

    Element gender = doc.createElement("gender");
    pr.appendChild(gender);
    gender.setAttribute("violations", String.valueOf(genderViolations));
    gender.setAttribute("weight", String.valueOf(Variables.GENDER_PEN));

    Element dynamicGender = doc.createElement("gender");
    costsElement.appendChild(dynamicGender);
    dynamicGender.setTextContent(String.valueOf(dynamicGenderCosts));

    Element transfer = doc.createElement("transfer");
    costsElement.appendChild(transfer);
    transfer.setTextContent(String.valueOf(transferCosts * Variables.TRANSFER_PEN));

    Element delay = doc.createElement("delay");
    costsElement.appendChild(delay);
    delay.setTextContent(String.valueOf(delays * Variables.DELAY_PEN));

    Element totalPatientCost = doc.createElement("patient_cost");
    costsElement.appendChild(totalPatientCost);
    totalPatientCost.setTextContent(String.valueOf(sol.getPatientCost()));

    Element totalEquityCost = doc.createElement("equity_cost");
    costsElement.appendChild(totalEquityCost);
    totalEquityCost.setTextContent(String.valueOf((int) sol.getEquityCost()));

    Element capViolations = doc.createElement("capacity_violations");
    costsElement.appendChild(capViolations);
    capViolations.setTextContent(String.valueOf(capacityViolations));

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