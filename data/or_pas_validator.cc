// or_pas_validator.cc file
// version: 1.0
// date: May 4, 2014
// author: Sara Ceschia & Andrea Schaerf
// OS: ubuntu linux
// g++ or_pas_validator.cc -o or_pas_validator `pkg-config libxml++-2.6 --cflags --libs` -std=c++11 
// run with: or_pas_validator <instance_file> <solution_file>
#include <iostream>
#include <string>
#include <vector>
#include <cassert>
#include <fstream>
#include <libxml++/libxml++.h>
#include <stdexcept>
#include <iomanip>
#include <cmath>
#include <cstdlib>

using namespace std;

enum Gender {MALE, FEMALE};
enum GenderPolicy {SAME_GENDER, MALE_ONLY, FEMALE_ONLY, TOGETHER};
enum Request {NEEDED, PREFERRED, DONT_CARE}; 
enum DoctoringLevel {COMPLETE, PARTIAL, NONE};
enum PatientStatus {UNREGISTERED, REGISTERED, ARRIVED, DISCHARGED};

const string instance_xml_schema = "OrPasInstance.xsd";
const string output_xml_schema = "OrPasSolution.xsd"; 

class Room 
{
public:
  Room(const xmlpp::Element*);
  string name;
  unsigned capacity;
  unsigned department;
  GenderPolicy policy;
};

class Patient 
{
public:
  Patient(const xmlpp::Element*); 
  unsigned StayLength() const { return discharge_day - admission_day; }
  unsigned MaxDelay() const { return max_admission_day - admission_day; }
  bool Male() const { return gender == MALE; }
  bool Female() const { return gender == FEMALE; }

  bool OverstayRisk() const { return variability != 0; }
  bool AlreadyPresent() const { return initial_room != -1; }
  unsigned InitialRoom() const { return (unsigned) initial_room; }
  
  bool Urgent() const { return !elective; }
  bool Elective() const { return elective; }
  bool Undelayable() const { return AlreadyPresent() || MaxDelay() == 0; }

  unsigned HospitalizationBeforeSurgery() const {assert(surgery_day >= static_cast<int>(admission_day)); return surgery_day - admission_day;}

  string name;
  unsigned age;
  Gender gender;
  unsigned registration_day, admission_day, discharge_day;
  unsigned variability; // At present 0/1: with 0 = fixed discharge, 1 = risk of overstay by 1 one
  unsigned max_admission_day;
  unsigned preferred_room_capacity;

  int initial_room; // room occupied before scheduling period (if it exists, -1 otherwise)
  unsigned treatment; 
  int surgery_day; // -1 if the treatment is not a surgical operation or the patient has been already operated
  bool elective;
};

class Treatment 
{
public: 
  Treatment(string n, unsigned spec, bool s = false, unsigned l = 0)
    :name(n), specialism(spec), surgery(s), length(l){}
  
  string name; 
  unsigned specialism; 
  bool surgery; 
  unsigned length; 
}; 

class Date
{
  friend bool operator==(const Date& d1, const Date& d2);
  friend bool operator!=(const Date& d1, const Date& d2);
  friend bool operator<(const Date& d1, const Date& d2);
  friend bool operator<=(const Date& d1, const Date& d2);
  friend bool operator>(const Date& d1, const Date& d2);
  friend bool operator>=(const Date& d1, const Date& d2);
  friend int operator-(const Date& d1, const Date& d2);
  friend istream& operator>>(istream&, Date&);
  friend ostream& operator<<(ostream&, const Date&);
 public:
  Date();
  Date(unsigned g, unsigned m, unsigned a);
  unsigned Day() const { return day; }
  unsigned Month() const { return month; }
  unsigned Year() const { return year; }
  void operator++();
  void operator--();
  void operator+=(int n);
  Date operator+(int n);
 private:
  unsigned day;
  unsigned month;
  unsigned year;
  bool IsValid() const;
  bool IsLeapYear() const;
  unsigned DaysOfMonth() const;
};

template <typename T>
class Matrix 
{
public:
  Matrix(); // Default constructor
  Matrix(const unsigned int n, const unsigned int m); // Construct a n x m matrix
  Matrix(const T& a, const unsigned int n, const unsigned int m); // Initialize the content to constant a
   Matrix(const T* a, const unsigned int n, const unsigned int m); // Initialize to array 
  Matrix(const Matrix<T>& rhs); // Copy constructor
  ~Matrix(); // destructor
	
  inline T* operator[](const unsigned int& i) { return v[i]; } // Subscripting: row i
  inline const T* operator[](const unsigned int& i) const { return v[i]; }; // const subsctipting
	
  inline void resize(const unsigned int n, const unsigned int m);
  inline void resize(const T& a, const unsigned int n, const unsigned int m);
	
	
  inline unsigned int nrows() const { return n; } // number of rows
  inline unsigned int ncols() const { return m; } // number of columns
	
  inline Matrix<T>& operator=(const Matrix<T>& rhs); // Assignment operator
  inline Matrix<T>& operator=(const T& a); // Assign to every element value a
private:
  unsigned int n; // number of rows
  unsigned int m; // number of columns
  T **v; // storage for data
};

template <typename T>
Matrix<T>::Matrix() 
  : n(0), m(0), v(0)
{}

template <typename T>
Matrix<T>::Matrix(unsigned int n, unsigned int m)
  : v(new T*[n])
{
  register unsigned int i;
  this->n = n; this->m = m;
  v[0] = new T[m * n];
  for (i = 1; i < n; i++)
    v[i] = v[i - 1] + m;
}

template <typename T>
Matrix<T>::Matrix(const T& a, unsigned int n, unsigned int m)
  : v(new T*[n])
{
  register unsigned int i, j;
  this->n = n; this->m = m;
  v[0] = new T[m * n];
  for (i = 1; i < n; i++)
    v[i] = v[i - 1] + m;
  for (i = 0; i < n; i++)
    for (j = 0; j < m; j++)
      v[i][j] = a;
}

template <class T> 
Matrix<T>::Matrix(const T* a, unsigned int n, unsigned int m) 
  : v(new T*[n])
{ 
  register unsigned int i, j;
  this->n = n; this->m = m;
  v[0] = new T[m * n]; 
  for (i = 1; i < n; i++) 
    v[i] = v[i - 1] + m; 
  for (i = 0; i < n; i++) 
    for (j = 0; j < m; j++) 
      v[i][j] = *a++; 
} 

template <typename T>
Matrix<T>::Matrix(const Matrix<T>& rhs)
  : v(new T*[rhs.n])
{
  register unsigned int i, j;
  n = rhs.n; m = rhs.m;
  v[0] = new T[m * n]; 
  for (i = 1; i < n; i++) 
    v[i] = v[i - 1] + m;
  for (i = 0; i < n; i++)
    for (j = 0; j < m; j++)
      v[i][j] = rhs[i][j];
}

template <typename T> 
Matrix<T>::~Matrix() 
{ 
  if (v != 0) { 
    delete[] (v[0]); 
    delete[] (v); 
  } 
}
				
template <typename T> 
inline Matrix<T>& Matrix<T>::operator=(const Matrix<T> &rhs) 
// postcondition: normal assignment via copying has been performed; 
// if matrix and rhs were different sizes, matrix 
// has been resized to match the size of rhs 
{ 
  register unsigned int i, j;
  if (this != &rhs) 
    {
      resize(rhs.n, rhs.m);
      for (i = 0; i < n; i++) 
	for (j = 0; j < m; j++) 
	  v[i][j] = rhs[i][j]; 
    } 
  return *this; 
} 

template <typename T> 
inline void Matrix<T>::resize(const unsigned int n, const unsigned int m) 
{
  register unsigned int i;
  if (n == this->n && m == this->m)
    return;
  if (v != 0) 
    { 
      delete[] (v[0]); 
      delete[] (v); 
    } 
  this->n = n; this->m = m;
  v = new T*[n]; 
  v[0] = new T[m * n];  
  for (i = 1; i < n; i++)
    v[i] = v[i - 1] + m;
} 

template <typename T> 
inline void Matrix<T>::resize(const T& a, const unsigned int n, const unsigned int m) 
{
  register unsigned int i, j;
  resize(n, m);
  for (i = 0; i < n; i++)
    for (j = 0; j < m; j++)
      v[i][j] = a;
} 

template <typename T> 
std::ostream& operator<<(std::ostream& os, const Matrix<T>& m)
{
  os << std::endl << m.nrows() << " " << m.ncols() << std::endl;
  for (unsigned int i = 0; i < m.nrows(); i++)
    {
      for (unsigned int j = 0; j < m.ncols() - 1; j++)
	os << std::setw(20) << std::setprecision(16) << m[i][j] << ", ";
      os << std::setw(20) << std::setprecision(16) << m[i][m.ncols() - 1] << std::endl;
    }
	
  return os;
}

class PASU_Manager
{
  friend ostream& operator<<(ostream& os, const PASU_Manager& in);
public:
  PASU_Manager(string file_name, const vector<unsigned>& weights);
  virtual ~PASU_Manager() {}
  void ComputePreprocessing();

  Patient* GetPatient(unsigned i) const { return patients[i];}//return patients.at(i); }
  Room* GetRoom(unsigned i) const { return rooms[i]; }

  unsigned Rooms() const { return num_rooms; }
  unsigned RoomProperties() const { return num_room_properties; }
  unsigned Beds() const { return num_beds; }
  unsigned Departments() const { return num_departments; }
  unsigned Specialisms() const { return num_specialisms; }
  unsigned Patients() const { return num_patients; }
  unsigned Days() const { return num_days; }
  unsigned OriginalPlanningHorizon() const {return original_planning_horizon;}
  string Instance() const {return instance; }

  string GetSpecialismName(unsigned spec) const {return  specialism_names[spec];}
  string GetFeaturesName(unsigned feat) const {return  feature_names[feat];}
  string GetDepartmentName(unsigned dept) const {return  department_names[dept];}
 
  bool RoomProperty(unsigned i, unsigned j) const { return room_property[i][j]; }
  DoctoringLevel DeptSpecialismLevel(unsigned i, unsigned j) const { return  dept_specialism_level[i][j];  }
  unsigned PatientSpecialismNeeded(unsigned i) const { return  patient_specialism_needed[i];  }
  Request PatientPropertyRequest(unsigned i, unsigned j) const { return  patient_property_level[i][j]; }
  bool PatientNeededProperty(unsigned i, unsigned j) const { return  patient_property_level[i][j] == NEEDED; }
  bool PatientPreferredProperty(unsigned i, unsigned j) const { return  patient_property_level[i][j] == PREFERRED; }
  unsigned DepartmentMinAge(unsigned i) const { return department_age_limits[i].first; }
  unsigned DepartmentMaxAge(unsigned i) const { return department_age_limits[i].second; }
  unsigned PatientOverlap(unsigned i, unsigned j)  const { return patient_overlap[i][j]; }
  int PatientInitialRoom(unsigned p)  const { return assignment[p]; }

  unsigned TotalPatientRoomCost(unsigned i, unsigned j) const { return total_patient_room_cost[i][j];  }

  bool PatientRoomAvailability(unsigned i, unsigned j) const { assert(j<patient_room_availability.ncols()); assert(i<patient_room_availability.nrows());
    return patient_room_availability[i][j];  }
  
  GenderPolicy RoomGenderPolicy(unsigned r) const { return rooms[r]->policy; }

  unsigned LowerBound() const { return lower_bound; }
  // for updating the output
  int FindPatient(string name, unsigned i = 0) const; // find the index of a patient, starting from i (for efficency)
  int FindSpecialism(string name) const;
  int FindDepartment(string name) const;
  int FindFeature(string name) const;
  int FindTreatment(string name) const;
  int FindRoom(string name) const;
  int FindDay(Date d) const;

  string CreateDate(int d = 0) const; // It gets the day in the format yyyy-mm-dd, adding d days to the first_day
  const Date& GetFirstDay() const {return first_day;}

  const unsigned PREFERRED_PROPERTY_WEIGHT, PREFERENCE_WEIGHT, SPECIALISM_WEIGHT, GENDER_WEIGHT, TRANSFER_WEIGHT, DELAY_WEIGHT, OVERCROWD_RISK_WEIGHT, IDLE_OPERATING_ROOM_WEIGHT, IDLE_ROOM_CAPACITY_WEIGHT, OVERTIME_WEIGHT;
  const unsigned MAX_CAPACITY;
  const unsigned ADMITTED_OVERTIME; // it is acceptable overtime for timeslot (in minutes)
  const unsigned ADMITTED_TOTAL_OVERTIME; // it is acceptable total overtime for day (in minutes)
 
  void NextDay();
  void ReadSolution(string out_file);

  Treatment* GetTreatment(unsigned i) const {return treatments[i];}
  unsigned OrSlots(unsigned day, unsigned spec) const {return day_spec_or_slots[day][spec];} // Gets the or time available in minutes
  unsigned NumOrSlots(unsigned day, unsigned spec) const {return day_spec_or_slots[day][spec]/or_slot_length;} // Gets the number of slots
  unsigned AdmittedOvertime(unsigned day, unsigned spec) const {return NumOrSlots(day, spec) * ADMITTED_OVERTIME;}
  unsigned AdmittedTotalOvertime(unsigned day) const {return ADMITTED_TOTAL_OVERTIME;}
  unsigned DayOrSlots(unsigned day) const { return day_or_slots[day]; } // Gets the total or time available in a day
  bool PatientToOperate(unsigned p) const {return GetPatient(p)->surgery_day > -1;}
  unsigned SurgeryLength(unsigned p) const {return GetTreatment(patients[p]->treatment)->length;}
  void RepeatMSS(); // Repeats the MSS 
 
  int DayAssigment(unsigned p, unsigned d) const { return day_assignment[p][d]; }

  bool Registered(unsigned p) const { return status[p] == REGISTERED; }
  bool Discharged(unsigned p) const { return status[p] == DISCHARGED; }
  bool Arrived(unsigned p) const { return status[p] == ARRIVED; }

  unsigned AdmissionDay(unsigned p) const { assert(status[p] >= ARRIVED); return static_cast<unsigned>(arrival_day[p]); }
  unsigned PlannedAdmissionDay(unsigned p) const 
  { if (status[p] >= ARRIVED) return AdmissionDay(p);
    else
      for (unsigned d = 0; d < num_days; d++)
	if (day_assignment[p][d] != -1)
	  return d;
    return 0;
  }
  void WriteCosts(ostream& os);
  // For full solve
  void SetCurrentDay(unsigned d) { current_day = d; }
  unsigned GetCurrentDay() { return current_day; }
  void SetDischarged(unsigned p) { status[p] = DISCHARGED; }
  void SetArrived(unsigned p, unsigned d) { status[p] = ARRIVED; arrival_day[p] = d; }

  pair<unsigned,unsigned> ComputeCost();

  void WriteOutputToXML(string file_name, double time); 
protected:
  void ReadFromXML(string file_name);
  void ReadOutputFromXML(string file_name);
  
  string instance;
  unsigned num_rooms;
  unsigned num_room_properties;
  unsigned num_beds;
  unsigned num_departments; 
  unsigned num_specialisms; 
  unsigned num_patients; 
  unsigned num_days;
  unsigned original_planning_horizon;
  unsigned num_treatments;

  Date first_day; 
  Date last_day; // first_day + num_days

  vector<string> specialism_names; 
  vector<string> feature_names; 
  vector<string> department_names; 

  unsigned lower_bound;

  Matrix<bool> room_property;
  Matrix<DoctoringLevel> dept_specialism_level;
  Matrix<unsigned> total_patient_room_cost; 
  Matrix<bool> patient_room_availability; 
  vector<unsigned> patient_specialism_needed; 

  Matrix<Request> patient_property_level;
  Matrix<unsigned> patient_overlap; //  number of days in which they are both in the hospital

  vector<pair<unsigned,unsigned> > department_age_limits;
  vector<Treatment*> treatments; 
  Matrix<unsigned> day_spec_or_slots; // stores the total length, not the number of slots
  vector<unsigned> day_or_slots; // stores the total length per day
  unsigned or_slot_length;
  vector<Room*> rooms;
  vector<Patient*> patients;

  vector<int> assignment; // the room assigned for patients already admitted

  unsigned current_day;

  Matrix<int> day_assignment; // -1 means no assignment
  vector<PatientStatus> status; 
  vector<int> arrival_day; // -1 if not arrived

  unsigned total_or_requests, total_or_slots, total_room_requests;

  // costs
  unsigned max_admission_viol;
  unsigned patient_room_cost, gender_cost, transfer_cost, delay_cost, overcrowd_risk_cost, violations;
  unsigned properties_viol, preference_viol, department_viol, room_fixed_gender_viol, capacity_viol;
  unsigned idle_room_cost;
  float room_utilization;
  unsigned operating_room_capacity_viol, operating_room_overtime_cost, idle_operating_room_cost;
  float operating_room_utilization;
  Matrix<bool> or_assignment; // true, if patient p is operated on day d
};

Room::Room(const xmlpp::Element* room) 
{
  string gender_policy;

  const xmlpp::Element::AttributeList& attributes = room->get_attributes();
  for (xmlpp::Element::AttributeList::const_iterator iter = attributes.begin(); iter != attributes.end(); iter++)
  {
    const xmlpp::Attribute* attribute = *iter;
    if (attribute->get_name() == "name")
      this->name = attribute->get_value();
    else if (attribute->get_name() == "capacity")
      this->capacity = atoi(attribute->get_value().c_str());
    else if (attribute->get_name() == "gender_policy")
      gender_policy = attribute->get_value();
  }

   if (gender_policy == "SameGender")
      this->policy = SAME_GENDER;
    else if (gender_policy == "MaleOnly")
      this->policy = MALE_ONLY;
    else if (gender_policy == "FemaleOnly")
      this->policy = FEMALE_ONLY;
    else if (gender_policy == "Any")
      this->policy = TOGETHER;
    else
      //error
      this->policy = TOGETHER;
}

Patient :: Patient(const xmlpp::Element* patient)
{
  string g;
  Date day; 

  const xmlpp::Element::AttributeList& attributes = patient->get_attributes();
  for (xmlpp::Element::AttributeList::const_iterator iter = attributes.begin(); iter != attributes.end(); iter++)
    {
      const xmlpp::Attribute* attribute = *iter;
      if (attribute->get_name() == "name")
	this->name = attribute->get_value();
      else if (attribute->get_name() == "age")
	this->age = atoi(attribute->get_value().c_str());
      else if (attribute->get_name() == "gender")
	g = attribute->get_value().c_str();
    }
  if (g == "Male")
    this->gender = MALE;
  else if (g == "Female")
    this->gender = FEMALE;
  else //error
    this->gender = FEMALE;
}

PASU_Manager::PASU_Manager(string instance, const vector<unsigned>& weights)
  : PREFERRED_PROPERTY_WEIGHT(weights[0]), PREFERENCE_WEIGHT(weights[1]), SPECIALISM_WEIGHT(weights[2]), 
    GENDER_WEIGHT(weights[3]), TRANSFER_WEIGHT(weights[4]), DELAY_WEIGHT(weights[5]), OVERCROWD_RISK_WEIGHT(weights[6]), IDLE_OPERATING_ROOM_WEIGHT(weights[7]), IDLE_ROOM_CAPACITY_WEIGHT(weights[8]), OVERTIME_WEIGHT(weights[9]), MAX_CAPACITY(6), ADMITTED_OVERTIME(30), ADMITTED_TOTAL_OVERTIME(60)

{
  total_or_requests = 0;
  total_room_requests = 0;
  total_or_slots = 0;

  if (instance.find(".xml") != string::npos)
    ReadFromXML(instance);
  else 
    assert(false);
  current_day = 0;

  // manage the solution
  patient_room_cost = 0; 
  gender_cost = 0; 
  transfer_cost = 0; 
  delay_cost = 0; 
  overcrowd_risk_cost = 0;

  properties_viol = 0;
  preference_viol = 0;
  department_viol = 0;
  room_fixed_gender_viol = 0;

  violations = 0;
  capacity_viol = 0;

  idle_room_cost = 0; 
  room_utilization = 0;

  operating_room_capacity_viol = 0; 
  operating_room_overtime_cost = 0;
  idle_operating_room_cost = 0; 
  operating_room_utilization = 0;
  day_assignment.resize( -1, num_patients, num_days);
  or_assignment.resize(false,num_patients,num_days);

  
}

void PASU_Manager:: ReadFromXML(string file_instance)
{ 
  if (file_instance.find(".xml") == string::npos)
    throw std::logic_error("Unrecognized file type " + file_instance);

  unsigned p,  total_days = 0, treatment; 
  string s, name, gen, max_ad_string, pref_cap_string, treatment_string, room_string, type, department_name;
  int d, r, max_ad, spec, f; 
  Date day; 

  xmlpp::DomParser document_parser, schema_parser;
  document_parser.set_substitute_entities();
  schema_parser.set_substitute_entities();  
  document_parser.parse_file(file_instance);
  if (!document_parser)
    throw std::logic_error("Could not read the XML file " + file_instance);
  schema_parser.parse_file(instance_xml_schema);
  if (!schema_parser)
    throw std::logic_error("Could not read the XML Schema file" + instance_xml_schema);

  xmlpp::Document* document = document_parser.get_document();
  xmlpp::Schema schema(schema_parser.get_document());
  xmlpp::SchemaValidator schema_validator(&schema);

  schema_validator.validate(document);

  const xmlpp::Element* instance_node = dynamic_cast<const xmlpp::Element*>(document->get_root_node()); 
  xmlpp::NodeSet result_nodes,  result_nodes_2,  result_nodes_3;

  result_nodes = instance_node->find("./attribute::name");
  instance = dynamic_cast<const xmlpp::Attribute*>(*result_nodes.begin())->get_value();
  result_nodes = instance_node->find("./descriptor/*");  

  for (xmlpp::NodeSet::const_iterator iter = result_nodes.begin(); iter != result_nodes.end(); iter++) 
    {
      const xmlpp::Element* description_entry = dynamic_cast<const xmlpp::Element*>(*iter);
      const xmlpp::TextNode* text = description_entry->get_child_text(); 

      if(description_entry->get_name() == "Departments")
	num_departments = atoi(text->get_content().c_str());
      else if(description_entry->get_name() == "Rooms")
	num_rooms = atoi(text->get_content().c_str());
      else if(description_entry->get_name() == "Features")
	num_room_properties = atoi(text->get_content().c_str());
      else if(description_entry->get_name() == "Patients")
	num_patients = atoi(text->get_content().c_str());
      else if(description_entry->get_name() == "Specialisms")
	num_specialisms = atoi(text->get_content().c_str());
      else if(description_entry->get_name() == "Horizon")
	{
	  original_planning_horizon = atoi(description_entry->get_attribute_value("num_days").c_str());
	  stringstream is(description_entry->get_attribute_value("start_day"));
	  is >> first_day;
	  
	}
      else if(description_entry->get_name() == "Treatments")
	num_treatments = atoi(text->get_content().c_str());
    }
  const unsigned REPEAT_PLANNING_HORIZON = 2;
  num_days = static_cast<unsigned>(ceil(REPEAT_PLANNING_HORIZON*original_planning_horizon)); // to simulate a longer planning horizon  
  last_day = first_day + num_days;
  num_beds = 0;
 
  room_property.resize(false, num_rooms, num_room_properties);
  dept_specialism_level.resize(NONE, num_departments, num_specialisms);
  department_age_limits.resize(num_departments,make_pair(0,120));
  patient_specialism_needed.resize(num_patients);
  patient_property_level.resize(DONT_CARE, num_patients, num_room_properties);
  patient_overlap.resize(0,num_patients, num_patients);
  total_patient_room_cost.resize(0,num_patients, num_rooms);
  patient_room_availability.resize(true,num_patients, num_rooms);
  assignment.resize(num_patients,-1);
  status.resize(num_patients,REGISTERED); // was UNREGISTERED, for the validator all patients are registered
  arrival_day.resize(num_patients,UNREGISTERED);

  specialism_names.resize(num_specialisms);
  feature_names.resize(num_room_properties);
  department_names.resize(num_departments);

  day_spec_or_slots.resize(0, num_days, num_specialisms);
  day_or_slots.resize(num_days, 0);

  //Specialisms
  spec = 0;
  result_nodes = instance_node->find("./specialisms/specialism");
  for (xmlpp::NodeSet::const_iterator iter = result_nodes.begin(); iter != result_nodes.end(); iter++) 
    {
      const xmlpp::Element* specialism_entry = dynamic_cast<const xmlpp::Element*>(*iter);
      const xmlpp::TextNode* specialism_text = specialism_entry->get_child_text(); 
      specialism_names[spec] = specialism_text->get_content();
      spec++;
    }

  //Features
  f = 0; 
  result_nodes = instance_node->find("./features/feature");
  for (xmlpp::NodeSet::const_iterator iter = result_nodes.begin(); iter != result_nodes.end(); iter++) 
    {
      const xmlpp::Element* feature_entry = dynamic_cast<const xmlpp::Element*>(*iter);
      const xmlpp::TextNode* feature_text = feature_entry->get_child_text(); 
      feature_names[f] = feature_text->get_content();
      f++;
    }

  //Departments
  d = 0; 
  result_nodes = instance_node->find("./departments/department");
  for (xmlpp::NodeSet::const_iterator iter = result_nodes.begin(); iter != result_nodes.end(); iter++) 
    {
      const xmlpp::Element* department = dynamic_cast<const xmlpp::Element*>(*iter);
 
      const xmlpp::Element::AttributeList& result_attributes = department->get_attributes();
      for (xmlpp::Element::AttributeList::const_iterator iter1 = result_attributes.begin(); iter1 != result_attributes.end(); iter1++) 
	{
	  const xmlpp::Attribute* attribute = *iter1;
	  if(attribute->get_name() == "name")
	    department_names[d] = department->get_attribute_value("name"); 
	  else if (attribute->get_name() == "min_age")
	    department_age_limits[d].first = atoi(department->get_attribute_value("min_age").c_str());
	  else if (attribute->get_name() == "max_age")
	    department_age_limits[d].second = atoi(department->get_attribute_value("max_age").c_str());
	}

      // else: keep the default limits (0,120)
 
      result_nodes_2 = department->find("./main_specialisms/main_specialism");    
      for (xmlpp::NodeSet::const_iterator iter2 = result_nodes_2.begin(); iter2 != result_nodes_2.end(); iter2++) 
	{
	  const xmlpp::Element* main_specialism = dynamic_cast<const xmlpp::Element*>(*iter2);
	  const xmlpp::TextNode* text = main_specialism->get_child_text(); 

	  name = text->get_content();
	  spec = FindSpecialism(name);
	  assert(spec >= 0);
	  dept_specialism_level[d][spec] = COMPLETE;
	}

     
      result_nodes_2 = department->find("./aux_specialisms/aux_specialism");    
      for (xmlpp::NodeSet::const_iterator iter2 = result_nodes_2.begin(); iter2 != result_nodes_2.end(); iter2++) 
	{
	  const xmlpp::Element* aux_specialism = dynamic_cast<const xmlpp::Element*>(*iter2);
	  const xmlpp::TextNode* text = aux_specialism->get_child_text(); 

	  name = text->get_content();
	  spec = FindSpecialism(name);
	  assert(spec >= 0);
	  dept_specialism_level[d][spec] = PARTIAL;
	}
      d++; 
    }

  // Rooms
  r = 0;
  result_nodes = instance_node->find("./rooms/room");
  for (xmlpp::NodeSet::const_iterator iter = result_nodes.begin(); iter != result_nodes.end(); iter++) 
    {
      const xmlpp::Element* room = dynamic_cast<const xmlpp::Element*>(*iter);
      Room *rm = new Room(room);
      num_beds+= rm->capacity; 
      
      department_name = room->get_attribute_value("department");
      d = FindDepartment(department_name); 
      assert(d >= 0); 

      rm->department = d;

      result_nodes_2 = room->find("./features/feature");    
      for (xmlpp::NodeSet::const_iterator iter2 = result_nodes_2.begin(); iter2 != result_nodes_2.end(); iter2++) 
	{
	  const xmlpp::Element* feature = dynamic_cast<const xmlpp::Element*>(*iter2);
	  const xmlpp::TextNode* text = feature->get_child_text(); 
	  name = text->get_content();
	  f = FindFeature(name);
	  assert(f >= 0);
	  room_property[r][f] = true;
	}
      rooms.push_back(rm);
      r++;
    }

  // Treatments
  unsigned surgery, surgery_length, number_or_slots; 
  string treat_name, spec_name; 

  result_nodes = instance_node->find("./treatments/treatment");
  for (xmlpp::NodeSet::const_iterator iter = result_nodes.begin(); iter != result_nodes.end(); iter++) 
    {
      const xmlpp::Element* treatment = dynamic_cast<const xmlpp::Element*>(*iter);
      const xmlpp::Element::AttributeList& result_attributes = treatment->get_attributes();
      surgery = 0; 
      surgery_length = 0; 

      for (xmlpp::Element::AttributeList::const_iterator iter2 = result_attributes.begin(); iter2 != result_attributes.end(); iter2++) 
	{
	  const xmlpp::Attribute* attribute = *iter2;
	  if(attribute->get_name() == "name")
	    treat_name = attribute-> get_value();
	  else if(attribute->get_name() == "specialism")
	    spec_name = attribute-> get_value();
	  else if(attribute->get_name() == "surgery")
	    surgery = atoi((attribute->get_value()).c_str());
	  else if(attribute->get_name() == "length")
	    surgery_length = atoi((attribute->get_value()).c_str());
	}
      spec = FindSpecialism(spec_name);
      assert(spec >= 0);

      Treatment* treat = new Treatment(treat_name, spec, surgery, surgery_length);
      treatments.push_back(treat);
    }
  
  // OR-slots
  result_nodes = instance_node->find("./or_slots/attribute::slot_length");
  or_slot_length = atoi(dynamic_cast<const xmlpp::Attribute*>(*result_nodes.begin())->get_value().c_str());
  result_nodes = instance_node->find("./or_slots/day");

  for (xmlpp::NodeSet::const_iterator iter = result_nodes.begin(); iter != result_nodes.end(); iter++) 
    {
      const xmlpp::Element* day_entry = dynamic_cast<const xmlpp::Element*>(*iter);
      istringstream iss(day_entry->get_attribute_value("name"), istringstream::in);
      iss >> day; 
      d = FindDay(day); 
      assert(d >= 0);

      result_nodes_2 = day_entry->find("./specialism_assignment");
      for (xmlpp::NodeSet::const_iterator iter2 = result_nodes_2.begin(); iter2 != result_nodes_2.end(); iter2++)
	{
	  const xmlpp::Element* specialism_assignment = dynamic_cast<const xmlpp::Element*>(*iter2);
	  spec_name = specialism_assignment->get_attribute_value("name"); 
	  number_or_slots = atoi(specialism_assignment->get_attribute_value("number_or_slots").c_str());
	  spec = FindSpecialism(spec_name);
	  assert(spec >= 0);
	  day_spec_or_slots[d][spec] = number_or_slots*or_slot_length; 
	}
    }
  
  if(REPEAT_PLANNING_HORIZON>1)
    RepeatMSS();// Extends the MSS by repeating it
 
  //Patients
  p = 0;
  result_nodes = instance_node->find("./patients/patient");
  for (xmlpp::NodeSet::const_iterator iter = result_nodes.begin(); iter != result_nodes.end(); iter++) 
    {
      const xmlpp::Element* patient = dynamic_cast<const xmlpp::Element*>(*iter);
      Patient *pat = new Patient(patient);
  
      const xmlpp::Element::AttributeList& result_attributes = patient->get_attributes();

      max_ad = -1;
      r = -1; // the room preassigned
      pat->preferred_room_capacity = MAX_CAPACITY;
      pat->surgery_day = -1;
  
	for (xmlpp::Element::AttributeList::const_iterator iter1 = result_attributes.begin(); iter1 != result_attributes.end(); iter1++) 
	  {
	    const xmlpp::Attribute* attribute = *iter1;
	    if (attribute->get_name() == "registration")
	      {
		istringstream iss(attribute->get_value());
		iss >> day;
		d = FindDay(day);
		assert(d >= 0);
		pat->registration_day = d; 
		if (d == 0)
		  status[p] = REGISTERED;
	      }
	    else if (attribute->get_name() == "admission")
	      {
		istringstream iss(attribute->get_value());
		iss >> day;
		d = FindDay(day);
		assert(d >= 0);
		pat->admission_day = d;
	      }
	    else if (attribute->get_name() == "discharge")
	      {
		istringstream iss(attribute->get_value());
		iss >> day;
		d = FindDay(day);
		assert(d >= 0);
		pat->discharge_day = d; 
	      }
	    else if (attribute->get_name() == "variability")
	      pat->variability = atoi(attribute->get_value().c_str());
	    else if(attribute->get_name() == "max_admission")
	      {
		istringstream iss(attribute->get_value());
		iss >> day;
		max_ad = FindDay(day);
		assert(max_ad >= 0);
	      }
	    else if(attribute->get_name() == "preferred_capacity")
	      pat->preferred_room_capacity= atoi(patient->get_attribute_value("preferred_capacity").c_str());
	    else if (attribute->get_name() == "treatment")
	      treatment_string = patient->get_attribute_value("treatment");
	    else if(attribute->get_name() == "room")
	      {
		room_string = patient->get_attribute_value("room");
		r = FindRoom(room_string);
		assert(r >= 0);
	      }
	    else if (attribute->get_name() == "surgery_day") 
	      {
		istringstream iss(attribute->get_value());
		iss >> day;
		d = FindDay(day);
		assert(d >= 0);
		pat->surgery_day = d; 
	      }
	  }
	assert(pat->admission_day >= pat->registration_day);
	assert(pat->discharge_day > pat->admission_day);
	if(pat->surgery_day >= 0)
	  assert (pat->surgery_day >= static_cast<int>(pat->admission_day) && pat->surgery_day < static_cast<int>(pat->discharge_day));
       
	if(max_ad == -1)
 	  max_ad = num_days - (pat->discharge_day - pat->admission_day);
	pat->max_admission_day = max_ad;
	total_days += pat->discharge_day - pat->admission_day;        
	treatment = FindTreatment(treatment_string); 
	assert(treatment >= 0); 
	pat->treatment = treatment;  // Move inside the Patient constructor?
	unsigned specialism = treatments[treatment]->specialism; 
	patient_specialism_needed[p] = specialism;
	
	if(r > -1)
	  {
	    assignment[p] = r; 
	    status[p] = ARRIVED;
	    arrival_day[p] = 0;
	  }

      result_nodes_2 = patient->find("./room_properties/room_property");
      for (xmlpp::NodeSet::const_iterator iter2 = result_nodes_2.begin(); iter2 != result_nodes_2.end(); iter2++) 
	{
	  const xmlpp::Element* room_property = dynamic_cast<const xmlpp::Element*>(*iter2);
   
	  name = room_property->get_attribute_value("name");    
	  f = FindFeature(name);
	  assert(f >= 0);
	  type = room_property->get_attribute_value("type");
	  if(type == "needed")
	    patient_property_level[p][f] = NEEDED;
	  else 
	    patient_property_level[p][f] = PREFERRED;	   
	}
      patients.push_back(pat);
      p++;
    }

  ComputePreprocessing(); 
}


void PASU_Manager :: ComputePreprocessing() 
{
  unsigned p, p1, p2, r, pr, d, sp, dep; 

  // Compute patient-patient overlap
  for (p1 = 0; p1 < num_patients-1; p1++) 
    for (p2 = p1+1; p2 < num_patients; p2++)  
      {
	for (d = patients[p1]->admission_day; d < patients[p1]->discharge_day; d++) 
	  if (patients[p2]->admission_day <= d && patients[p2]->discharge_day > d)
	    {
	      patient_overlap[p1][p2]++;
	      patient_overlap[p2][p1]++;
	    }
      }
  // Compute total patient room cost (and availability)
  for (p = 0; p < num_patients; p++)
    {
      sp = patient_specialism_needed[p];
      for (r = 0; r < num_rooms; r++)
	{
	  // Properties
	  for (pr = 0; pr < num_room_properties; pr++)
	    {
	      if (patient_property_level[p][pr] == NEEDED && !room_property[r][pr])
		patient_room_availability[p][r] = false;
	      if (patient_property_level[p][pr] == PREFERRED && !room_property[r][pr])
		total_patient_room_cost[p][r] += PREFERRED_PROPERTY_WEIGHT;
	    }
	  
	  // Preferences
	  if (patients[p]->preferred_room_capacity < rooms[r]->capacity)
	    total_patient_room_cost[p][r] += PREFERENCE_WEIGHT;
	  
	  // Specialism
	  dep = rooms[r]->department;
  	  if (dept_specialism_level[dep][sp] == PARTIAL)
  	    total_patient_room_cost[p][r] += SPECIALISM_WEIGHT; // * RoomDeptSpecialismLevel(r][sp] * (always 1)
	  if (dept_specialism_level[dep][sp] == NONE)
	    patient_room_availability[p][r] = false;

	  // Department age
	  if (department_age_limits[dep].first != 0 && patients[p]->age < department_age_limits[dep].first)
	    patient_room_availability[p][r] = false;
	  if (department_age_limits[dep].second != 0 && patients[p]->age > department_age_limits[dep].second)
	    patient_room_availability[p][r] = false;

	  // Gender 
	  if (rooms[r]->policy == MALE_ONLY && patients[p]->Female())
	    total_patient_room_cost[p][r] += GENDER_WEIGHT;
	  if (rooms[r]->policy == FEMALE_ONLY && patients[p]->Male())
	    total_patient_room_cost[p][r] += GENDER_WEIGHT;
	}
    }    


  for (p = 0; p < num_patients; p++)
    {
      if (patients[p]->registration_day == patients[p]->admission_day && patients[p]-> MaxDelay() == 0)
	patients[p]->elective = false;
      else
	patients[p]->elective = true;

      if(PatientToOperate(p))
	total_or_requests += SurgeryLength(p);

      for (d = 0; d < OriginalPlanningHorizon(); d++)
	if (d >= patients[p]->admission_day && d < patients[p]->discharge_day)
	  total_room_requests++;
    }

  for (d = 0; d < OriginalPlanningHorizon(); d++)	  
    for (sp = 0; sp < num_specialisms; sp++)
      total_or_slots += OrSlots(d, sp);

  for (d = 0; d < num_days; d++)	  
    for (sp = 0; sp < num_specialisms; sp++)
      day_or_slots[d] += OrSlots(d, sp); 

  // compute lower bound
  vector<int> patient_min_cost(num_patients, -1);
  for (p = 0; p < num_patients; p++)
    {
      for (r = 0; r < num_rooms; r++)
	if (patient_room_availability[p][r])
	  {
	    if (patient_min_cost[p] == -1 || total_patient_room_cost[p][r] < static_cast<unsigned>(patient_min_cost[p]))
	      patient_min_cost[p] = total_patient_room_cost[p][r];
	  }
      if (patient_min_cost[p] == -1)
	{
	  cerr << "Infeasible for patient " << GetPatient(p)->name << endl;
	  cout << "Infeasible" << endl; // Otherwise it is not possible to create a RandomState
	  exit(1); 
	}

      else
	lower_bound += static_cast<unsigned>(patient_min_cost[p]) * GetPatient(p)->StayLength();
    }
}

int PASU_Manager::FindPatient(string name, unsigned k) const
{
  unsigned i;
  for (i = k; i < num_patients; i++)
    if (patients[i]->name == name)
      return i;
  return -1;
}

int PASU_Manager::FindSpecialism(string name) const
{
  unsigned i;
  for (i = 0; i < num_specialisms; i++)
    if (specialism_names[i] == name)
      return i;
  return -1;
}

int PASU_Manager::FindFeature(string name) const
{
  unsigned i;
  for (i = 0; i < num_room_properties; i++)
    if (feature_names[i] == name)
      return i;
  return -1;
}

int PASU_Manager::FindDepartment(string name) const
{
 unsigned i;
  for (i = 0; i < num_departments; i++)
    if (department_names[i] == name)
      return i;
  return -1;
}


int PASU_Manager::FindTreatment(string name) const
{
  unsigned i;
  for (i = 0; i < num_treatments; i++)
    if (treatments[i]->name == name)
      return i;
  return -1;
}

int PASU_Manager::FindRoom(string name) const
{
  unsigned i;
  for (i = 0; i < num_rooms; i++)
    if (rooms[i]->name == name)
      return i;
  return -1;
}

int PASU_Manager::FindDay(Date day) const
{
  if(day < first_day || day > last_day)
    return -1;

  return day - first_day;
}

string PASU_Manager::CreateDate(int d) const
{
  ostringstream oss (ostringstream::out);
  Date new_date = first_day; 

  new_date += d;
  oss.clear(); 
  oss << new_date; 
  return oss.str(); 
}

void PASU_Manager:: RepeatMSS()
{
  unsigned d, s;

  for (d = OriginalPlanningHorizon(); d < num_days; d++)
    {
      for(s = 0; s < num_specialisms; s++)
	day_spec_or_slots[d][s] = day_spec_or_slots[d%OriginalPlanningHorizon()][s];    
      day_or_slots[d] = day_or_slots[d%OriginalPlanningHorizon()];
    }    
      
}

void PASU_Manager::WriteCosts(ostream& os)
{
  os << "VIOLATIONS:" << endl;
  os << "Max admission: " << max_admission_viol << endl;
  os << "Capacity = " << capacity_viol << endl;
  os << "Operating room = " << operating_room_capacity_viol << endl;

  os << endl << "COSTS:" << endl;

  os << "Patient/room total cost = " << patient_room_cost 
     << ": features = " << properties_viol << " X " << PREFERRED_PROPERTY_WEIGHT 
     << ", preference = " << preference_viol << " X " << PREFERENCE_WEIGHT 
     << ", dept = " << department_viol <<  " X " << SPECIALISM_WEIGHT
     << ", fixed gender = " << room_fixed_gender_viol << " X " << GENDER_WEIGHT << endl;
  os << "Gender cost = " << gender_cost << endl;
  os << "Transfer cost = " << transfer_cost << endl;
  os << "Delay cost = " << delay_cost << endl;
  os << "Overcrowd risk cost = " << overcrowd_risk_cost << endl;
  os << "Idle room cost = " << idle_room_cost  << endl;
  os << "Idle operating room cost = " << idle_operating_room_cost  << endl;
  os << "Operating room overtime cost = " << operating_room_overtime_cost << endl;
}

void PASU_Manager :: WriteOutputToXML(string file_name, double time) 
{
  unsigned p, d, delay; 
  string sol_name, status, room_name;
  const int BUF_SIZE = 33; 
  char buf[BUF_SIZE]; 
  pair<unsigned,unsigned> cost_pair;

  sol_name = "sol-" + instance;
  xmlpp::Document document("1.0");
  //standalone="no"
  
  xmlpp::NodeSet result_nodes;
  xmlpp::Element* solution  = document.create_root_node("OrPasu_main_out");

  solution->set_namespace_declaration("http://www.w3.org/2001/XMLSchema-instance", "xsi");
  solution->set_attribute("schemaLocation", "http://www.diegm.uniud.it/ceschia/uploads/VRCLP/XML/OrPasu_main_out.xsd", "xsi");
  solution->set_attribute("name", sol_name);
  solution->set_attribute("instance", instance);

  xmlpp::Element* planning_horizon = solution->add_child("planning_horizon");
  xmlpp::Element* patients_scheduling = solution->add_child("patients_scheduling");
  xmlpp::Element* costs = solution->add_child("costs");

  xmlpp::Element* start_day = planning_horizon->add_child("start_day");
  start_day->set_child_text(CreateDate());

  xmlpp::Element* num_days_entry = planning_horizon->add_child("num_days");
  sprintf(buf, "%d", num_days);
  num_days_entry->set_child_text(buf);

  xmlpp::Element* current_day_entry = planning_horizon->add_child("current_day");
  current_day_entry->set_child_text(CreateDate(current_day));//FIX ME
  
  for (p = 0; p < num_patients; p++)
    {
      if (Registered(p) || Arrived(p) || Discharged(p))
	{
	  xmlpp::Element* patient = patients_scheduling->add_child("patient");
	  patient->set_attribute("name", GetPatient(p)->name); 
	  if (Discharged(p))	    
	    status = "discharged"; 
	  else if (Arrived(p))
	    status = "arrived"; 
	  else
	    status = "registered"; 
	  patient->set_attribute("status", status); 

	  for (d = 0; d < num_days; d++)
	    {
	      if (day_assignment[p][d] >= 0)
		{
		  xmlpp::Element* stay = patient->add_child("stay");
		  stay->set_attribute("day", CreateDate(d));
		  room_name = GetRoom(day_assignment[p][d])->name; 
		  stay->set_attribute("room", room_name);
		}	     
	    }
	  if (Registered(p) && //(Arrived(p) || Discharged(p)) && // contiamo tutti i costi
	      PlannedAdmissionDay(p) > GetPatient(p)->admission_day)
	    //	  if (Arrived(p) || Discharged(p))
	    {
	      delay = PlannedAdmissionDay(p) - GetPatient(p)->admission_day;
	      sprintf(buf, "%d", delay);
	      if (delay > 0)
		patient->set_attribute("delay", buf);
	    }
	} 
    }

  cost_pair = ComputeCost();

  xmlpp::Element* patient_room  = costs->add_child("patient_room");
  sprintf(buf, "%d", patient_room_cost );
  patient_room->set_attribute("value", buf);

  xmlpp::Element* properties  = patient_room->add_child("properties");
  sprintf(buf, "%d", properties_viol);
  properties->set_attribute("violations", buf);
  sprintf(buf, "%d", PREFERRED_PROPERTY_WEIGHT);
  properties->set_attribute("weight", buf);

  xmlpp::Element* preference  = patient_room->add_child("preference");
  sprintf(buf, "%d", preference_viol);
  preference->set_attribute("violations", buf);
  sprintf(buf, "%d", PREFERENCE_WEIGHT);
  preference->set_attribute("weight", buf);

  xmlpp::Element* specialism  = patient_room->add_child("specialism");
  sprintf(buf, "%d", department_viol);
  specialism->set_attribute("violations", buf);
  sprintf(buf, "%d", SPECIALISM_WEIGHT);
  specialism->set_attribute("weight", buf);

  xmlpp::Element* gender  = patient_room->add_child("gender");
  sprintf(buf, "%d", room_fixed_gender_viol);
  gender->set_attribute("violations", buf);
  sprintf(buf, "%d", GENDER_WEIGHT);
  gender->set_attribute("weight", buf);
  
  xmlpp::Element* gender_cost_entry  = costs->add_child("gender");
  sprintf(buf, "%d", gender_cost); 
  gender_cost_entry->set_child_text(buf);
  xmlpp::Element* transfer_cost_entry  = costs->add_child("transfer");
  sprintf(buf, "%d", transfer_cost); 
  transfer_cost_entry->set_child_text(buf);
  xmlpp::Element* delay_cost_entry  = costs->add_child("delay");
  sprintf(buf, "%d", delay_cost); 
  delay_cost_entry->set_child_text(buf);
  xmlpp::Element* overcrowd_risk_cost_entry  = costs->add_child("overcrowd_risk");
  sprintf(buf, "%d", overcrowd_risk_cost); 
  overcrowd_risk_cost_entry->set_child_text(buf);

  xmlpp::Element* or_overtime_cost_entry  = costs->add_child("or_overtime_cost");
  sprintf(buf, "%d", operating_room_overtime_cost); 
   or_overtime_cost_entry->set_child_text(buf);

   xmlpp::Element* idle_room_cost_entry  = costs->add_child("idle_room_cost");
   sprintf(buf, "%d", idle_room_cost); 
   idle_room_cost_entry->set_child_text(buf);
   
   xmlpp::Element* idle_operating_room_cost_entry  = costs->add_child("idle_operating_room_cost");
   sprintf(buf, "%d",idle_operating_room_cost); 
   idle_operating_room_cost_entry->set_child_text(buf);

  xmlpp::Element* capacity_viol_entry  = costs->add_child("capacity_violations");
  sprintf(buf, "%d", capacity_viol); 
  // capacity_viol_entry->set_child_text(buf);
  capacity_viol_entry->set_attribute("value", buf);
  sprintf(buf, "%.2f", room_utilization*100); 
  capacity_viol_entry->set_attribute("room_utilization", buf);

  xmlpp::Element* or_violations_entry  = costs->add_child("or_violations");
  sprintf(buf, "%d", operating_room_capacity_viol); 
  or_violations_entry->set_attribute("value", buf);
  sprintf(buf, "%.2f", operating_room_utilization*100); 
  or_violations_entry->set_attribute("or_utilization", buf);
 
  sprintf(buf, "%d", cost_pair.first); 
  costs->set_attribute("violations", buf);
  sprintf(buf, "%d", cost_pair.second); 
  costs->set_attribute("objectives", buf);
  sprintf(buf, "%.2f", time); 
  costs->set_attribute("time", buf);

  //Validate the document
  string schema_file = output_xml_schema;
   xmlpp::DomParser schema_parser;
   schema_parser.set_substitute_entities();  
   schema_parser.parse_file(schema_file);
   if (!schema_parser)
     throw std::logic_error("Could not read the XML Schema file "+ schema_file);
   
   xmlpp::Schema schema(schema_parser.get_document());
   xmlpp::SchemaValidator schema_validator(&schema);
   
   schema_validator.validate(&document);
   document.write_to_file_formatted(file_name, "UTF-8");

}
 
void PASU_Manager::ReadSolution(string out_file)
{
  /*
    It reads a complete solution from a file in the xml format (used by the STATIC_EVALUATE action)
   */

  ifstream is(out_file.c_str());
  assert(!is.fail());

  xmlpp::DomParser document_parser;
  document_parser.set_substitute_entities();
  document_parser.parse_stream(is);

  if (!document_parser)
    throw std::logic_error("Could not read input stream ");

  unsigned d, p, r, num_days; 
  string patient_name,  patient_status, room_string; 

  Date first_day, day; 
  
  xmlpp::Document* document = document_parser.get_document();
  const xmlpp::Element* main_out_solution_node = dynamic_cast<const xmlpp::Element*>(document->get_root_node()); 

  xmlpp::NodeSet result_nodes, result_nodes_2;

  result_nodes = main_out_solution_node->find("./planning_horizon/*");
 for (xmlpp::NodeSet::const_iterator iter = result_nodes.begin(); iter != result_nodes.end(); iter++) 
    {
      const xmlpp::Element* horizon_entry = dynamic_cast<const xmlpp::Element*>(*iter);
      const xmlpp::TextNode* text = horizon_entry->get_child_text(); 
      if(horizon_entry->get_name() == "start_day") // not used
	{
	  stringstream is(text->get_content().c_str());
	  is >> first_day;
	}
      else if(horizon_entry->get_name() == "num_days") // not used
	{
	  num_days = atoi(text->get_content().c_str());
	  assert(num_days <= this->num_days);
	}
      else if(horizon_entry->get_name() == "current_day")
	{
	  stringstream is(text->get_content().c_str());
	  is >> day;
	}
    }
 
 assert(current_day == 0);
 assert(static_cast<int>(current_day) == FindDay(first_day));
 
 result_nodes = main_out_solution_node->find("./patients_scheduling");
 const xmlpp::Element* patients_scheduling = dynamic_cast<const xmlpp::Element*>(*result_nodes.begin());
 
 result_nodes = patients_scheduling->find("./patient");
 
 for(p=0; p < Patients(); p++)
   {
     // reset the assignments
     for (d = 0; d < this->num_days; d++)
       day_assignment[p][d] = -1;

     for (xmlpp::NodeSet::const_iterator iter = result_nodes.begin(); iter != result_nodes.end(); iter++) 
	{
	  const xmlpp::Element* patient_entry = dynamic_cast<const xmlpp::Element*>(*iter);
	  patient_name = patient_entry->get_attribute_value("name");

	  if(GetPatient(p)->name == patient_name)
	    {
	      patient_status = patient_entry->get_attribute_value("status");
	      result_nodes_2 = patient_entry->find("./stay");

	      for (xmlpp::NodeSet::const_iterator iter2 = result_nodes_2.begin(); iter2 != result_nodes_2.end(); iter2++) 
		{
		  const xmlpp::Element* patient_day_entry = dynamic_cast<const xmlpp::Element*>(*iter2);
		  istringstream iss(patient_day_entry->get_attribute_value("day"), istringstream::in);
		  iss >> day; 
		  d = FindDay(day); 
		  assert(d >= 0);
		  room_string = patient_day_entry->get_attribute_value("room");
		  r = FindRoom(room_string);
		  assert(r >= 0);
		  day_assignment[p][d] = r;
		  if(iter2 == result_nodes_2.begin())
		    {
		      if (d == current_day)
			{
			  status[p] = ARRIVED;
			  arrival_day[p] = d;
			}
		      if (PatientToOperate(p))
			or_assignment[p][d+GetPatient(p)->HospitalizationBeforeSurgery()] = true;
		    }
		}
	    }
	}
    }
}


pair<unsigned,unsigned> PASU_Manager::ComputeCost() 
{ 
  unsigned p, r, d, sp, rp, dep, potential_occupancy, spec;
  int room, room1, room2;
  vector<vector<unsigned> > occupancy(num_rooms,vector<unsigned>(num_days,0));
  vector<vector<unsigned> > or_occupancy(num_days,vector<unsigned>(num_specialisms,0));
  vector<unsigned> day_or_occupancy(num_days,0);

  violations = 0;
  max_admission_viol = 0;
  capacity_viol = 0;
  patient_room_cost = 0;
  gender_cost = 0;
  transfer_cost = 0;
  delay_cost = 0;
  overcrowd_risk_cost = 0;

  properties_viol = 0;
  preference_viol = 0;
  department_viol = 0;
  room_fixed_gender_viol = 0;

  idle_room_cost = 0; 
  room_utilization = 0;
 
  operating_room_capacity_viol = 0;
  operating_room_overtime_cost = 0;
  idle_operating_room_cost = 0;
  operating_room_utilization = 0;

  // patient room cost
  for (p = 0; p < num_patients; p++)
    {
      sp = PatientSpecialismNeeded(p);
      for (d = 0; d < num_days; d++)
	{
	  room = day_assignment[p][d];
	  if (room != -1)
	    {
	      if (!PatientRoomAvailability(p,room))
		{
		  cerr << "VIOLATION: Patient " << GetPatient(p)->name << " in room " << GetRoom(room)->name << " on day " << CreateDate(d) << endl;
		  violations++;
		}
	      else
		patient_room_cost += TotalPatientRoomCost(p,room);
	      
	      // detailed costs
	      for (rp = 0; rp < RoomProperties(); rp++)
		{
		  if (PatientPreferredProperty(p,rp) && !RoomProperty(room,rp))
		    {
		      properties_viol++;
		    }
		}
	      if (GetPatient(p)->preferred_room_capacity < GetRoom(room)->capacity)
		{
		  preference_viol++;
		}
	      dep = GetRoom(room)->department;
	      if (DeptSpecialismLevel(dep,sp) > 0)
		{
		  department_viol++;
		}
	      if (RoomGenderPolicy(room) == MALE_ONLY && GetPatient(p)->Female())
		{
		  room_fixed_gender_viol++;
		}
	      if (RoomGenderPolicy(room) == FEMALE_ONLY && GetPatient(p)->Male())
		{
		  room_fixed_gender_viol++;
		}
	    }
	} 
    }
  
  // room capacity cost
  for (r = 0; r < num_rooms; r++)
    for (d = 0; d < num_days; d++)
      {
	for (p = 0; p < num_patients; p++)
	  if (day_assignment[p][d] != -1 && static_cast<unsigned>(day_assignment[p][d]) == r)
	    occupancy[r][d]++;
	if (occupancy[r][d] > GetRoom(r)->capacity)
	  {
	    capacity_viol += occupancy[r][d] - GetRoom(r)->capacity; 
	    violations += occupancy[r][d] - GetRoom(r)->capacity;
	    cerr << "VIOLATION: Room " << GetRoom(r)->name << " overcrowded (by " 
		 << occupancy[r][d] - GetRoom(r)->capacity << ") in day " << CreateDate(d) << endl;
	  }	   
      }
  
  // operating_room_cost 
  unsigned overtime;
  for (p = 0; p < num_patients; p++)
    {
      spec = PatientSpecialismNeeded(p); 	
      for (d = 0; d < num_days; d++)
	{
	  if(or_assignment[p][d])
	    {
	      if (GetPatient(p)->Elective())
		    or_occupancy[d][spec] += SurgeryLength(p);		  
	      day_or_occupancy[d] += SurgeryLength(p);
	    }
	}	  
    }
  
  for (d = 0; d < num_days; d++)
    {
      for (spec = 0; spec < Specialisms(); spec++)
	{
	  // Operating room capacity violations
	  if(or_occupancy[d][spec] > OrSlots(d, spec) + AdmittedOvertime(d, spec))
	    {
	      /* operating_room_capacity_viol += (or_occupancy[d][spec] - OrSlots(d, spec));
		 violations += or_occupancy[d][spec] - OrSlots(d, spec); 
		 cerr << "VIOLATION: OR Specialism " << GetSpecialismName(spec) << " overcrowded by " 
		 << (or_occupancy[d][spec] - OrSlots(d, spec)) << " (" 
		 << or_occupancy[d][spec] << "-" << OrSlots(d, spec) << ") in day " << CreateDate(d) << endl;
	      */
	      
	      operating_room_capacity_viol += (or_occupancy[d][spec] - OrSlots(d, spec) - AdmittedOvertime(d, spec));
	      violations += (or_occupancy[d][spec] - OrSlots(d, spec) - AdmittedOvertime(d, spec));
	      
	      cerr << "VIOLATION: OR Specialism " << GetSpecialismName(spec) << " overcrowded by " 
		   << (or_occupancy[d][spec] - OrSlots(d, spec) - AdmittedOvertime(d, spec)) << " (" 
		   << or_occupancy[d][spec] << "-" << OrSlots(d, spec) << "-" <<  AdmittedOvertime(d, spec) 
		   << ") in day " << CreateDate(d) << endl;
	      
	    }
	  // Operating Room Overtime
	  if(or_occupancy[d][spec] > OrSlots(d, spec))
	    {
	      if(or_occupancy[d][spec] > OrSlots(d, spec) + AdmittedOvertime(d, spec)) 
		overtime = AdmittedOvertime(d, spec);
	      else
		overtime = or_occupancy[d][spec] - OrSlots(d, spec);
	      
	      operating_room_overtime_cost += overtime; 
	    }   
	}
      
      // Day-based OR occupancy
      if(day_or_occupancy[d] > DayOrSlots(d) + AdmittedTotalOvertime(d))
	{		  
	  operating_room_capacity_viol += (day_or_occupancy[d] - DayOrSlots(d) - AdmittedTotalOvertime(d));
	  violations += (day_or_occupancy[d] - DayOrSlots(d) - AdmittedTotalOvertime(d));
	  
	  cerr << "VIOLATION: Total OR overcrowded by " 
	       << (day_or_occupancy[d] - DayOrSlots(d) - AdmittedTotalOvertime(d)) << " (" 
	       << day_or_occupancy[d] << "-" << DayOrSlots(d) << "-" <<  AdmittedTotalOvertime(d) 
	       << ") in day " << CreateDate(d) << endl;
	  
	}
      // Operating Room Overtime
      if(day_or_occupancy[d] > DayOrSlots(d))
	{
	  if(day_or_occupancy[d] > DayOrSlots(d) + AdmittedTotalOvertime(d)) 
	    overtime = AdmittedTotalOvertime(d);
	  else
	    overtime = day_or_occupancy[d] - DayOrSlots(d);
	  
	  operating_room_overtime_cost += overtime; 
	}    
    }
  operating_room_overtime_cost *= OVERTIME_WEIGHT; 

  // gender cost
  for (d = 0; d < num_days; d++)
    {
      vector<unsigned> room_males(num_rooms,0), room_females(num_rooms,0);
      for (p = 0; p < num_patients; p++)
	{
	  room = day_assignment[p][d];
	  if (room != -1)
	    {
	      if (GetPatient(p)->Male())
		room_males[room]++;
	      else
		room_females[room]++;
	    }
	}	     
      for (r = 0; r < num_rooms; r++)
	if (RoomGenderPolicy(r) == SAME_GENDER)
	  {
	    if (min(room_males[r], room_females[r]) >= 1)
	      gender_cost += 1; //min(room_males[r], room_females[r]);
	  }
    }
  gender_cost *= GENDER_WEIGHT;


  // patient room cost
  for (p = 0; p < num_patients; p++)
    for (d = 0; d < num_days; d++)
      {
	if(d == 0)
	  room1 = PatientInitialRoom(p);
	else
	  room1 = day_assignment[p][d-1];
	room2 = day_assignment[p][d];
	if (room1 != -1 && room2 != -1 && room1 != room2)
	  transfer_cost++;
      }
  transfer_cost *= TRANSFER_WEIGHT;

  // delay cost
  for (p = 0; p < num_patients; p++)
    {      
      if ((Registered(p) || Arrived(p) || Discharged(p)) && // contiamo tutti i costi
	  PlannedAdmissionDay(p) > GetPatient(p)->admission_day)
	delay_cost += PlannedAdmissionDay(p) - GetPatient(p)->admission_day;
    }
  delay_cost *= DELAY_WEIGHT;


  // max admission violations
  for (p = 0; p < num_patients; p++)
    {
      if ((Registered(p) || Arrived(p) || Discharged(p)) && // contiamo tutti i costi
	  PlannedAdmissionDay(p) > GetPatient(p)->max_admission_day)
	{
	  max_admission_viol += PlannedAdmissionDay(p) - GetPatient(p)->max_admission_day;
	  violations += PlannedAdmissionDay(p) - GetPatient(p)->max_admission_day;
	  cerr << "VIOLATION: Patient " << GetPatient(p)->name << " admitted on day " <<
	    PlannedAdmissionDay(p) << ", with maximum admission day " <<  GetPatient(p)->max_admission_day << endl;}
    }

  // overcrowd risk cost

  for (r = 0; r < num_rooms; r++)
    for (d = 1; d < num_days; d++)
      {
	potential_occupancy = occupancy[r][d];
	for (p = 0; p < num_patients; p++)
	  if (static_cast<unsigned>(day_assignment[p][d-1]) == r && day_assignment[p][d] == -1 && GetPatient(p)->OverstayRisk())
	    potential_occupancy++;
	if (potential_occupancy > GetRoom(r)->capacity)
	  {
	    overcrowd_risk_cost += potential_occupancy - GetRoom(r)->capacity;
	  }	   
      }
  overcrowd_risk_cost *= OVERCROWD_RISK_WEIGHT;


  //IdleOperatingRoom
  idle_operating_room_cost = min(total_or_requests, total_or_slots);
  for (d = 0; d < OriginalPlanningHorizon(); d++)
    for (spec = 0; spec < num_specialisms; spec++)
      if(or_occupancy[d][spec] < OrSlots(d, spec))
	idle_operating_room_cost -= or_occupancy[d][spec];
      else
	idle_operating_room_cost -= OrSlots(d, spec);

  idle_operating_room_cost *= IDLE_OPERATING_ROOM_WEIGHT;
  
  // IdleRoomCapacity
  idle_room_cost = min(total_room_requests, num_beds * OriginalPlanningHorizon());
  for (d = 0; d < OriginalPlanningHorizon(); d++)
    {
      for (r = 0; r < num_rooms; r++)
	if (occupancy[r][d] <= GetRoom(r)->capacity)
	  idle_room_cost -= occupancy[r][d];
    }
 idle_room_cost *= IDLE_ROOM_CAPACITY_WEIGHT;

 //Room Utilization (not included in the cost but reported in the output)
 vector<float> day_occupancy(OriginalPlanningHorizon(), 0);
  for (d = 0; d < OriginalPlanningHorizon(); d++)
    {
      for (r = 0; r < num_rooms; r++)
	day_occupancy[d] += occupancy[r][d];
      room_utilization += day_occupancy[d]/Beds();
    }
  room_utilization /= OriginalPlanningHorizon();

 //Operating Room Utilization (not included in the cost but reported in the output)
  unsigned count_spec, count_days = 0;
  for (d = 0; d < OriginalPlanningHorizon(); d++)
    {
      count_spec = 0;
      day_occupancy[d] = 0;
      for (spec = 0; spec < num_specialisms; spec++)
	{
	  if(OrSlots(d, spec) > 0)
	    {
	      day_occupancy[d] += or_occupancy[d][spec]/(float)OrSlots(d, spec) ;
	      count_spec++;
	    }
	}
      if(DayOrSlots(d) > 0)
	{
	  operating_room_utilization += day_occupancy[d]/count_spec;	 
	  count_days++; 
	}
    }
  operating_room_utilization /= count_days;

  return make_pair(violations, patient_room_cost + gender_cost + transfer_cost + delay_cost + overcrowd_risk_cost + operating_room_overtime_cost + idle_operating_room_cost + idle_room_cost);
}

Date::Date()
{ //  Conventional date 
  day = 1;
  month = 1;
  year = 1970;
}

Date::Date(unsigned g, unsigned m, unsigned a)
{
  day = g;
  month = m;
  year = a;
  if (!IsValid())
    {
      day = 1;
      month = 1;
      year = 1970;
    }
}

bool Date::IsValid() const
{
  return year >=1 && month >= 1 && month <= 12 
    && day >= 1 && day <= DaysOfMonth();
}

unsigned Date::DaysOfMonth() const
{
  if (month == 4 || month == 6 || month == 9 || month == 11)
    return 30;
  else if (month == 2)
    if (IsLeapYear())
      return 29;
    else
      return 28;
  else 
    return 31;
}         

bool Date::IsLeapYear() const
{
  if (year % 4 != 0)
    return false;
  else if (year % 100 != 0)
    return true;
  else if (year % 400 != 0)
    return false;
  else 
    return true;
}

istream& operator>>(istream& is, Date& d)
{
  char ch;
  is >> d.year >> ch >> d.month >> ch >> d.day;
  assert(d.IsValid());
  return is;
}
ostream& operator<<(ostream& os, const Date& d)
{
  os << d.year << '-' << setfill ('0') << setw(2) << d.month << '-' << setfill('0') << setw(2) << d.day;
  return os;
}

void Date::operator--()
{ 
  if (day != 1)
    day--;
  else 
    if (month != 1)  
      { 
        month--;
        day = DaysOfMonth();
      }
    else
      { 
        year--;
        month = 12;
        day = DaysOfMonth();
      }
}

void Date::operator++()
{ 
  if (day != DaysOfMonth())
    day++;
  else 
    if (month != 12)  
      { 
        day = 1;
        month++;
      }
    else
      { 
        day = 1;
        month = 1;
        year++;
      }
}

void Date::operator+=(int n)
{
  int i;
  if (n > 0)
    for (i = 0; i < n; i++)
      ++(*this);
  else
    for (i = 0; i < -n; i++)
      --(*this);
}

Date Date::operator+(int n)
{
  Date d = *this;
  
  int i;
  if (n > 0)
    for (i = 0; i < n; i++)
      ++d;
  else
    for (i = 0; i < -n; i++)
      --d;
  return d;
}

int operator-(const Date& d1, const Date& d2)
{
  int conta = 0;
  Date d;
  if (d1 <= d2)
    {
      d = d1;
      while (d != d2)
	{
	  ++d;
	  conta--;
	}
    }
  else
    {
      d = d2;
      while (d != d1)
	{
	  ++d;
	  conta++;
	}
    }
  return conta;
}



bool operator==(const Date& d1, const Date& d2)
{
  return d1.day == d2.day && d1.month == d2.month && d1.year == d2.year;
}

bool operator!=(const Date& d1, const Date& d2)
{
  return d1.day != d2.day || d1.month != d2.month || d1.year != d2.year;
}

bool operator<(const Date& d1, const Date& d2)
{
  return (d1.year < d2.year) 
    || (d1.year == d2.year && d1.month < d2.month) 
    || (d1.year == d2.year && d1.month == d2.month && d1.day < d2.day); 
}

bool operator<=(const Date& d1, const Date& d2)
{
  return (d1.year < d2.year) 
    || (d1.year == d2.year && d1.month < d2.month) 
    || (d1.year == d2.year && d1.month == d2.month && d1.day <= d2.day); 
}

bool operator>(const Date& d1, const Date& d2)
{
  return (d1.year > d2.year) 
    || (d1.year == d2.year && d1.month > d2.month) 
    || (d1.year == d2.year && d1.month == d2.month && d1.day > d2.day); 
}

bool operator>=(const Date& d1, const Date& d2)
{
  return (d1.year > d2.year) 
    || (d1.year == d2.year && d1.month > d2.month) 
    || (d1.year == d2.year && d1.month == d2.month && d1.day >= d2.day); 
}

int main(int argc, char* argv[]) 
{
  if (argc != 3)
    {
      cerr << "Usage: " << argv[0] << " <input file> <solution file>" << endl;
      return 1;
    }

  vector<unsigned> weights(10); 
  weights[0] = 20;  // ROOM_PROPERTY
  weights[1] = 10;	// ROOM_PREFERENCE
  weights[2] = 20;	// SPECIALISM
  weights[3] = 50;	// GENDER POLICY
  weights[4] = 100;	// TRANSFER
  weights[5] = 5;   // DELAY       (X day)              
  weights[6] = 1;   // OVERCROWD_RISK 
  weights[7] = 10;  // IDLE_OPERATING_ROOM (X minute)
  weights[8] = 20;  // IDLE_ROOM_CAPACITY    (X day)
  weights[9] = 3;  // OVERTIME     (X minute)             

  PASU_Manager manager(argv[1], weights);
  manager.ReadSolution(argv[2]);      
  pair<unsigned,unsigned> cost = manager.ComputeCost();
  manager.WriteCosts(cout);
  cout << "Total Violations: " << cost.first << ", Total Cost: " << cost.second << endl;

  return 0;
}
