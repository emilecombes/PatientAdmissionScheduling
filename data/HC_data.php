<?php
/*
  OR_PASU_Generator version 2.0 May 4, 2014 
  -------------------------------------------------------------------------------------------------------------------------------------------
  This file defines the data necessary to generate the instances. It is part of the instance generator used in:
 
  - Ceschia S. and Schaerf A. The Dynamic Patient Admission Scheduling with Operating Room Constraints, Flexible Horizon, and Patient Delays. 
  6th Multidisciplinary International Scheduling Conference (MISTA '13), 27-30 August 2013, Gent, Belgium.

  The instance generator is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  Copyright (C) 2014 SaTT Group, DIEGM, University of Udine. 
  -------------------------------------------------------------------------------------------------------------------------------------------
*/

/*   ----------------------------------------------------------------------------------------------------------------------------------------
     Parameters kept fixed for all instances. Probabilities are in %.
*/
// Probabilities
$prob_needed_feature = 10; // probability that a feature is needed (respect to desired)
$over_stay_risk = 30; // the overstay risk
$prob_urgency = 10; // the probability that a patient is urgent (i.e. the registration day is equal to the admission day and he/she can not be delayed)
$prob_surgery = 60; // the probability that a treatment is a surgery
$policies = array("SameGender","MaleOnly","FemaleOnly","Any"); // The gender policy available (for clarifications see: Demeester P, Souffriau W, De Causmaecker P, Vanden Berghe G. A hybrid tabu search algorithm for automatically assigning patients to beds. Artificial Intelligence in Medicine 2010;48(1):61–70.)
$policy_distributions = array(60,15,15,10); // the probabilities used for the gender policies (60% SameGender, 15% MaleOnly or FemaleOnly, 10% Any)

// Initial State
$initial_solution_days = 3; // the number of days used to create the initial situation (in order to not have the hospital empty in the first day of the planning horizon)

// Other data
$max_age = 90; // the maximum age of a patient
$available_capacities = array(1,2,4,6); // capacity of rooms (single room, 2-4-6 beds)
// Operating rooms
$or_day_slots = 3; // number of slots in a working day for an operating room (during weekend $or_day_slots = 0)
$slot_length = 180; // length of a slot in minutes
$MSS_periodicity = 7; // period of the MSS (in days)
$num_working_days = 5; // number of working days in a period
$today = new DateTime(); // the first day if the planning horizon is today

//    ----------------------------------------------------------------------------------------------------------------------------------------

// Room Properties
$room_property_names = array("oxygen", "telemetry", "nitrogen", "infusion_pump", "television"); 
sort($room_property_names); 

if($features > count($room_property_names))
  {
    echo "Only " . count($room_property_names) . " features available \n";
    exit(1); 
  }

//    ----------------------------------------------------------------------------------------------------------------------------------------

// Departments
$general_medicine_dept = new Department("General_Medicine");
$spec_medicine_dept = new Department("Specialized_Medicine");
$general_surgery_dept = new Department("General_Surgery");
$spec_surgery_dept = new Department("Specialized_Surgery");
$maternity_dept = new Department("Maternity");
$cardiology_dept = new Department("Cardiology");
$neuroscience_dept = new Department("Neuroscience");
$psychiatry_dept = new Department("Psychiatry");
$oncology_dept = new Department("Oncology");
$elderly_care_dept = new Department("Elderly_Care", 65, 100);
//$emergency_dept = new Department("Emergency");

$department_list = array($general_medicine_dept, $general_surgery_dept, $spec_medicine_dept, $spec_surgery_dept, $maternity_dept, $cardiology_dept, $neuroscience_dept, $psychiatry_dept, $oncology_dept, $elderly_care_dept);

if($departments > count($department_list))
  {
    echo "Only " . count($department_list) . " departments available \n";
    exit(1); 
  }

//    ----------------------------------------------------------------------------------------------------------------------------------------

// Specialisms

// Parameters:

// - Generic values from Kao, E. P. C., & Tung, G. G. (1981). Bed Allocation in a Public Health Care Delivery System. Management Science, 27(5), 507 – 520. 

//$mean_LOS_med = 8.57;
$mean_LOS_sur = 6.75;
//$sd_LOS_med = 3.17;
$sd_LOS_sur = 2.99;
// $mean_surgical_times = 180; // in minutes
// $sd_surgical_times = 60; // in minutes

/** - Values dependent on the specialism:
    - for LOS from Harper, Shahani, Journal fo Operational Research Society (2002) 53, 11-18) (sd = (q_2/q_1)^(1/1.349)).
    - for surgical times from MIN, YIN, EJOR 206 (2010) 642-652.
*/

$mean_LOS_cardiology = 2.7; 
$sd_LOS_cardiology = 2.25;

$mean_LOS_dermatology = 15.7; 
$sd_LOS_dermatology = 2.48;

$mean_LOS_elderly_care = 18.1; 
$sd_LOS_elderly_care = 3.42;

$mean_LOS_gastroenterology = 2.3;
$sd_LOS_gastroenterology = 1.67;

$mean_LOS_general_medicine = 2.9;
$sd_LOS_general_medicine = 1.67;

$mean_LOS_neurology = 4.7;
$sd_LOS_neurology = 1.97;

$mean_LOS_rehabilitation = 55;
$sd_LOS_rehabilitation = 3.26;

$mean_LOS_rheumatology = 11.2;
$sd_LOS_rheumatology = 1.8;

$mean_LOS_thoracic_medicine = 1.7;
$sd_LOS_thoracic_medicine = 4.66;

$mean_surgical_times_oto = 74;
$sd_surgical_times_oto = 37;

$mean_surgical_times_gyn = 86;
$sd_surgical_times_gyn = 40;

$mean_surgical_times_ortho = 107;
$sd_surgical_times_ortho = 44;

$mean_surgical_times_neuro = 160;
$sd_surgical_times_neuro = 77;

$mean_surgical_times_general = 93;
$sd_surgical_times_general = 49;

$mean_surgical_times_ophth = 38;
$sd_surgical_times_ophth = 19;

$mean_surgical_times_vascular = 120;
$sd_surgical_times_vascular = 61;

$mean_surgical_times_cardiac = 240;
$sd_surgical_times_cardiac = 103;

$mean_surgical_times_urol = 64;
$sd_surgical_times_urol = 52;


$internal_medicine_spec = new Specialism("Internal_Medicine", $mean_LOS_general_medicine, $sd_LOS_general_medicine);
$critical_care_spec = new Specialism("Critical_Care", $mean_LOS_general_medicine, $sd_LOS_general_medicine); // da correggere
//$emergency_spec = new Specialism("Emergency");
$endocrinology_spec = new Specialism("Endocrinology", $mean_LOS_general_medicine, $sd_LOS_general_medicine);
$diabetology_spec = new Specialism("Diabetology", $mean_LOS_general_medicine, $sd_LOS_general_medicine);
$nephrology_spec = new Specialism("Nephrology", $mean_LOS_general_medicine, $sd_LOS_general_medicine);
$dermatology_spec = new Specialism("Dermatology", $mean_LOS_dermatology, $sd_LOS_dermatology);
$infectious_disease_spec = new Specialism("Infectious_Disease", $mean_LOS_general_medicine, $sd_LOS_general_medicine);
$rheumatology_spec = new Specialism("Rheumatology", $mean_LOS_rheumatology, $sd_LOS_rheumatology);
$general_surgery_spec = new Specialism("General_Surgery", $mean_LOS_sur, $sd_LOS_sur, $mean_surgical_times_general, $sd_surgical_times_general);
$urological_surgery_spec = new Specialism("Urological_Surgery", $mean_LOS_sur, $sd_LOS_sur, $mean_surgical_times_urol, $sd_surgical_times_urol);
$vascular_surgery_spec = new Specialism("Vascular_Surgery", $mean_LOS_sur, $sd_LOS_sur, $mean_surgical_times_vascular, $sd_surgical_times_vascular);
$orthopaedics_spec = new Specialism("Orthopaedics", $mean_LOS_sur, $sd_LOS_sur, $mean_surgical_times_ortho, $sd_surgical_times_ortho);
$gastroenterology_spec = new Specialism("Gastroenterology", $mean_LOS_gastroenterology, $sd_LOS_gastroenterology, $mean_surgical_times_general, $sd_surgical_times_general);
$ophthalmology_spec = new Specialism("Ophthalmology", $mean_LOS_sur, $sd_LOS_sur, $mean_surgical_times_ophth, $sd_surgical_times_ophth);
$otolaryngology_spec = new Specialism("Otolaryngology", $mean_LOS_sur, $sd_LOS_sur, $mean_surgical_times_oto, $sd_surgical_times_oto);
$maxillofacial_surgery_spec = new Specialism("Maxillofacial_Surgery", $mean_LOS_sur, $sd_LOS_sur, $mean_surgical_times_general, $sd_surgical_times_general);
$plastic_surgery_spec = new Specialism("Plastic_Surgery", $mean_LOS_sur, $sd_LOS_sur, $mean_surgical_times_general, $sd_surgical_times_general);
$pediatrics_spec = new Specialism("Pediatrics", $mean_LOS_general_medicine, $sd_LOS_general_medicine);
$gynaecology_spec = new Specialism("Gynaecology", $mean_LOS_sur, $sd_LOS_sur, $mean_surgical_times_gyn, $sd_surgical_times_gyn);
$neonatology_spec = new Specialism("Neonatology", $mean_LOS_general_medicine, $sd_LOS_general_medicine);
$oncology_spec = new Specialism("Oncology", $mean_LOS_general_medicine, $sd_LOS_general_medicine);
$haematology_spec = new Specialism("Haematology", $mean_LOS_general_medicine, $sd_LOS_general_medicine);
$immunology_spec = new Specialism("Immunology", $mean_LOS_general_medicine, $sd_LOS_general_medicine);
$neurology_spec = new Specialism("Neurology", $mean_LOS_neurology, $sd_LOS_neurology, $mean_surgical_times_neuro, $sd_surgical_times_neuro);
$rehabilitation_spec = new Specialism("Rehabilitation", $mean_LOS_rehabilitation, $sd_LOS_rehabilitation);
$cardiology_spec =  new Specialism("Cardiology", $mean_LOS_cardiology, $sd_LOS_cardiology, $mean_surgical_times_cardiac, $sd_surgical_times_cardiac); 
$psychiatry_spec = new Specialism("Psychiatry", $mean_LOS_general_medicine, $sd_LOS_general_medicine);
$elderly_care_spec = new Specialism("Elderly_Care", $mean_LOS_elderly_care, $sd_LOS_elderly_care);


// Depending on the number of departments, different specialism are used.

$general_medicine_dept->AddMainSpecialisms($internal_medicine_spec);
$general_medicine_dept->AddMainSpecialisms($endocrinology_spec);
$general_medicine_dept->AddMainSpecialisms($diabetology_spec);

if($departments >= 2)
  $general_medicine_dept->AddAuxSpecialisms($critical_care_spec);
if($departments > 2)
  {
    $general_medicine_dept->AddAuxSpecialisms($haematology_spec); 
    $general_medicine_dept->AddAuxSpecialisms($rheumatology_spec);
    $general_medicine_dept->AddAuxSpecialisms($dermatology_spec);
  }
if($departments > 4)
  $general_medicine_dept->AddAuxSpecialisms($gynaecology_spec);
if($departments > 6)
  $general_medicine_dept->AddAuxSpecialisms($neurology_spec);
if($departments > 9)
  $general_medicine_dept->AddAuxSpecialisms($elderly_care_spec);


$spec_medicine_dept->AddMainSpecialisms($nephrology_spec);
$spec_medicine_dept->AddMainSpecialisms($haematology_spec);
$spec_medicine_dept->AddMainSpecialisms($dermatology_spec);
$spec_medicine_dept->AddMainSpecialisms($infectious_disease_spec);
$spec_medicine_dept->AddMainSpecialisms($rheumatology_spec);
$spec_medicine_dept->AddAuxSpecialisms($internal_medicine_spec);
$spec_medicine_dept->AddAuxSpecialisms($endocrinology_spec);
$spec_medicine_dept->AddAuxSpecialisms($diabetology_spec);

$general_surgery_dept->AddMainSpecialisms($general_surgery_spec);
$general_surgery_dept->AddMainSpecialisms($critical_care_spec);
$general_surgery_dept->AddMainSpecialisms($urological_surgery_spec);
$general_surgery_dept->AddMainSpecialisms($vascular_surgery_spec);
$general_surgery_dept->AddMainSpecialisms($orthopaedics_spec);
$general_surgery_dept->AddMainSpecialisms($gastroenterology_spec);
if($departments >= 2)
  $general_surgery_dept->AddAuxSpecialisms($internal_medicine_spec);
if($departments > 2)
  {
    $general_surgery_dept->AddAuxSpecialisms($otolaryngology_spec);
    $general_surgery_dept->AddAuxSpecialisms($maxillofacial_surgery_spec);
  }
if($departments > 5)
  $general_surgery_dept->AddAuxSpecialisms($cardiology_spec); 
if($departments > 6)
  $general_surgery_dept->AddAuxSpecialisms($neurology_spec);

$spec_surgery_dept->AddMainSpecialisms($ophthalmology_spec);
$spec_surgery_dept->AddMainSpecialisms($otolaryngology_spec);
$spec_surgery_dept->AddMainSpecialisms($maxillofacial_surgery_spec);
$spec_surgery_dept->AddMainSpecialisms($plastic_surgery_spec);
$spec_surgery_dept->AddMainSpecialisms($critical_care_spec);
$spec_surgery_dept->AddAuxSpecialisms($general_surgery_spec);
$spec_surgery_dept->AddAuxSpecialisms($vascular_surgery_spec);
$spec_surgery_dept->AddAuxSpecialisms($orthopaedics_spec);
if($departments > 5)
  $general_surgery_dept->AddAuxSpecialisms($cardiology_spec); 

$maternity_dept->AddMainSpecialisms($pediatrics_spec);
$maternity_dept->AddMainSpecialisms($gynaecology_spec);
$maternity_dept->AddMainSpecialisms($neonatology_spec);

$cardiology_dept->AddMainSpecialisms($cardiology_spec);
$cardiology_dept->AddMainSpecialisms($critical_care_spec); 
$cardiology_dept->AddAuxSpecialisms($general_surgery_spec);
$cardiology_dept->AddAuxSpecialisms($rehabilitation_spec); 
$cardiology_dept->AddAuxSpecialisms($vascular_surgery_spec);

$neuroscience_dept->AddMainSpecialisms($critical_care_spec);
$neuroscience_dept->AddMainSpecialisms($neurology_spec);
$neuroscience_dept->AddAuxSpecialisms($rehabilitation_spec);
if($departments > 7)
  $neuroscience_dept->AddAuxSpecialisms($psychiatry_spec); 
 
$psychiatry_dept->AddMainSpecialisms($psychiatry_spec); 
$psychiatry_dept->AddAuxSpecialisms($rehabilitation_spec); 
$psychiatry_dept->AddAuxSpecialisms($neurology_spec); 

$oncology_dept->AddMainSpecialisms($oncology_spec);
$oncology_dept->AddMainSpecialisms($critical_care_spec);
$oncology_dept->AddAuxSpecialisms($haematology_spec);
$oncology_dept->AddAuxSpecialisms($rehabilitation_spec);

$elderly_care_dept->AddMainSpecialisms($elderly_care_spec); 
$elderly_care_dept->AddAuxSpecialisms($rehabilitation_spec); 

//    ----------------------------------------------------------------------------------------------------------------------------------------

// Treatments

$generic_emergency_treat = new Treatment("Generic_Emergency_Treatment", "medical", $mean_LOS_general_medicine, $sd_LOS_general_medicine);
$cardiac_insufficiency_treat = new Treatment("Cardiac_Insufficiency_Treatment", "medical", $mean_LOS_general_medicine, $sd_LOS_general_medicine);
$respiratory_failure_treat = new Treatment("Respiratory_Failure_Treatment", "medical", $mean_LOS_general_medicine, $sd_LOS_general_medicine);

$gastroenteric_treat =  new Treatment("Gastroenteric_Diseases_Treatment", "medical", $mean_LOS_gastroenterology, $sd_LOS_gastroenterology);
$haematology_treat  = new Treatment("Haematology_Disease_Treatment", "medical", $mean_LOS_general_medicine, $sd_LOS_general_medicine);
$endocrine_treat = new Treatment("Endocrine_Disease_Treatment", "medical", $mean_LOS_general_medicine, $sd_LOS_general_medicine);
$diabetology_treat = new Treatment("Diabetology_Disease_Treatment", "medical", $mean_LOS_general_medicine, $sd_LOS_general_medicine);
$elderly_care_treat = new Treatment("Elderly_Care_Disease_Treatment", "medical", $mean_LOS_elderly_care, $sd_LOS_elderly_care);

$abdominal_surgery_treat = new Treatment("Abdominal_Surgery_Treatment", "surgical", $mean_LOS_sur, $sd_LOS_sur, $mean_surgical_times_general, $sd_surgical_times_general);
$breast_surgery_treat = new Treatment("Breast_Surgery_Treatment", "surgical", $mean_LOS_sur, $sd_LOS_sur, $mean_surgical_times_general, $sd_surgical_times_general);
$obesity_surgery_treat = new Treatment("Obesity_Surgery_Treatment", "surgical", $mean_LOS_sur, $sd_LOS_sur, $mean_surgical_times_general, $sd_surgical_times_general);
$urological_surgery_treat = new Treatment("Urological_Surgery_Treatment", "surgical", $mean_LOS_sur, $sd_LOS_sur, $mean_surgical_times_urol, $sd_surgical_times_urol);
$vascular_surgery_treat = new Treatment("Vascular_Surgery_Treatment", "surgical", $mean_LOS_sur, $sd_LOS_sur, $mean_surgical_times_vascular, $sd_surgical_times_vascular);
$orthopaedics_surgery_treat = new Treatment("Orthopaedics_Surgery_Treatment", "surgical", $mean_LOS_sur, $sd_LOS_sur, $mean_surgical_times_ortho, $sd_surgical_times_ortho);
$orthopaedics_treat = new Treatment("Orthopaedics_Hospitalization", "medical", $mean_LOS_general_medicine, $sd_LOS_general_medicine);

$gastroenterology_surgery_treat = new Treatment("Gastroenterology_Surgery_Treatment", "surgical", $mean_LOS_gastroenterology, $sd_LOS_gastroenterology, $mean_surgical_times_general, $sd_surgical_times_general);
//$intensive_care_treat = new Treatment("Intensive_Care_Hospitalization", $mean_LOS_general_medicine, $sd_LOS_general_medicine);
$nephrology_treat = new Treatment("Nephrology_Disease_Treatment","medical", $mean_LOS_general_medicine, $sd_LOS_general_medicine);
$immunology_treat  = new Treatment("Immunology_Disease_Treatment", "medical", $mean_LOS_general_medicine, $sd_LOS_general_medicine);
$dermatology_treat = new Treatment("Dermatology_Hospitalization", "medical", $mean_LOS_dermatology, $sd_LOS_dermatology);
$infectious_disease_treat = new Treatment("Infectious_Disease_Treatment", "medical", $mean_LOS_general_medicine, $sd_LOS_general_medicine);
$rheumatology_treat = new Treatment("Rheumatology_Hospitalization", "medical", $mean_LOS_rheumatology, $sd_LOS_rheumatology);
$ophthalmology_surgery_treat = new Treatment("Ophthalmology_Surgery_Treatment", "surgical", $mean_LOS_sur, $sd_LOS_sur, $mean_surgical_times_ophth, $sd_surgical_times_ophth);
$otolaryngology_surgery_treat = new Treatment("Otolaryngology_Surgery_Treatment", "surgical", $mean_LOS_sur, $sd_LOS_sur, $mean_surgical_times_oto, $sd_surgical_times_oto);
$maxillofacial_surgery_treat = new Treatment("Maxillofacial_Surgery_Treatment", "surgical", $mean_LOS_sur, $sd_LOS_sur, $mean_surgical_times_general, $sd_surgical_times_general);
$plastic_surgery_treat = new Treatment("Plastic_Surgery_Treatment", "surgical", $mean_LOS_sur, $sd_LOS_sur, $mean_surgical_times_general, $sd_surgical_times_general);

$gynaecology_treat = new Treatment("Gynaecology_Hospitalization", "medical", $mean_LOS_general_medicine, $sd_LOS_general_medicine);
$gynaecology_surgery_treat = new Treatment("Gynaecology_Surgery_Treatment", "surgical", $mean_LOS_sur, $sd_LOS_sur, $mean_surgical_times_gyn, $sd_surgical_times_gyn);
$pediatrics_treat = new Treatment("Pediatrics_Hospitalization", "medical",  $mean_LOS_general_medicine, $sd_LOS_general_medicine);
$neonatology_treat = new Treatment("Neonatology_Hospitalization", "medical",  $mean_LOS_general_medicine, $sd_LOS_general_medicine);

$cardiac_surgery_treat = new Treatment("Cardiac_Surgery_Treatment", "surgical", $mean_LOS_cardiology, $sd_LOS_cardiology, $mean_surgical_times_cardiac, $sd_surgical_times_cardiac);
$thoracic_surgery_treat = new Treatment("Thoracic_Surgery_Treatment", "surgical", $mean_LOS_thoracic_medicine, $sd_LOS_thoracic_medicine, $mean_surgical_times_general, $sd_surgical_times_general);
$pulmonology_surgery_treat = new Treatment("Pulmonology_Surgery_Treatment", "surgical", $mean_LOS_sur, $sd_LOS_sur, $mean_surgical_times_general, $sd_surgical_times_general);
$pulmonology_disease_treat = new Treatment("Pulmonology_Disease_Treatment", "medical", $mean_LOS_general_medicine, $sd_LOS_general_medicine);
$cardiology_treat = new Treatment("Cardiology_Hospitalization", "medical", $mean_LOS_cardiology, $sd_LOS_cardiology);

$neurosurgery_surgery_treat = new Treatment("Neurosurgery_Surgery_Treatment", "surgical", $mean_LOS_neurology, $sd_LOS_neurology, $mean_surgical_times_neuro, $sd_surgical_times_neuro);
$neurology_treat = new Treatment("Neurology_Hospitalization", "medical", $mean_LOS_neurology, $sd_LOS_neurology);

$rehabilitation_treat = new Treatment("Rehabilitation", "medical", $mean_LOS_rehabilitation, $sd_LOS_rehabilitation);

$psychiatry_treat = new Treatment("Psychiatry_Hospitalization", "medical", $mean_LOS_general_medicine, $sd_LOS_general_medicine);

$oncology_treat = new Treatment("Oncology_Hospitalization", "medical", $mean_LOS_general_medicine, $sd_LOS_general_medicine);
$elderly_care_treat = new Treatment("Elderly_Care_Treatment", "medical", $mean_LOS_elderly_care, $sd_LOS_elderly_care);
$critical_care_treat = new Treatment("Critical_Care_Treatment", "medical", $mean_LOS_general_medicine, $sd_LOS_general_medicine);

$critical_care_spec->AddMedicalTreatment($critical_care_treat); 

$internal_medicine_spec->AddMedicalTreatment($generic_emergency_treat);
$internal_medicine_spec->AddMedicalTreatment($cardiac_insufficiency_treat);
$internal_medicine_spec->AddMedicalTreatment($respiratory_failure_treat);

$endocrinology_spec->AddMedicalTreatment($endocrine_treat); 
$diabetology_spec->AddMedicalTreatment($diabetology_treat); 
$elderly_care_spec->AddMedicalTreatment($elderly_care_treat);

$general_surgery_spec->AddSurgicalTreatment($abdominal_surgery_treat); 
$general_surgery_spec->AddSurgicalTreatment($breast_surgery_treat); 
$general_surgery_spec->AddSurgicalTreatment($obesity_surgery_treat); 

$urological_surgery_spec->AddSurgicalTreatment($urological_surgery_treat); 
$vascular_surgery_spec->AddSurgicalTreatment($vascular_surgery_treat); 
$orthopaedics_spec->AddSurgicalTreatment($orthopaedics_surgery_treat); 
$orthopaedics_spec->AddMedicalTreatment($orthopaedics_treat); 


$gastroenterology_spec->AddSurgicalTreatment($gastroenterology_surgery_treat);
$gastroenterology_spec->AddMedicalTreatment($gastroenteric_treat);

$nephrology_spec->AddMedicalTreatment($nephrology_treat);
$haematology_spec->AddMedicalTreatment($haematology_treat); 
$haematology_spec->AddMedicalTreatment($immunology_treat); 
$dermatology_spec->AddMedicalTreatment($dermatology_treat); 
$infectious_disease_spec->AddMedicalTreatment($infectious_disease_treat); 
$rheumatology_spec->AddMedicalTreatment($rheumatology_treat); 

$ophthalmology_spec->AddSurgicalTreatment($ophthalmology_surgery_treat); 
$otolaryngology_spec->AddSurgicalTreatment($otolaryngology_surgery_treat); 
$maxillofacial_surgery_spec->AddSurgicalTreatment($maxillofacial_surgery_treat); 
$plastic_surgery_spec->AddSurgicalTreatment($plastic_surgery_treat);

$gynaecology_spec->AddMedicalTreatment($gynaecology_treat);
$gynaecology_spec->AddSurgicalTreatment($gynaecology_surgery_treat); 
$pediatrics_spec->AddMedicalTreatment($pediatrics_treat);
$neonatology_spec->AddMedicalTreatment($neonatology_treat);

$cardiology_spec->AddSurgicalTreatment($cardiac_surgery_treat);
$cardiology_spec->AddSurgicalTreatment($thoracic_surgery_treat);
$cardiology_spec->AddSurgicalTreatment($pulmonology_surgery_treat);
$cardiology_spec->AddMedicalTreatment($pulmonology_disease_treat); 
$cardiology_spec->AddMedicalTreatment($cardiology_treat); 
$neurology_spec->AddSurgicalTreatment($neurosurgery_surgery_treat); 
$neurology_spec->AddMedicalTreatment($neurology_treat); 
$psychiatry_spec->AddMedicalTreatment($psychiatry_treat); 
$rehabilitation_spec->AddMedicalTreatment($rehabilitation_treat);
$oncology_spec->AddMedicalTreatment($oncology_treat);

//    ----------------------------------------------------------------------------------------------------------------------------------------
// Create arrays of medical and surgical specialisms

$specialisms_list = array(); // redundant
$surgical_specialisms = array(); 
$medical_specialisms = array();
$surgical_treatments = array(); 
$medical_treatments = array();

$num_dept_have_spec = array(); // number of departments that have the specialism (main and aux)

$treatments = 0;

for($d = 0; $d < $departments; $d++)
  {
    $department = $department_list[$d];
    foreach($department->main_specialisms as $main_specialism)
      {
	if($main_specialism->IsSurgical() && !array_key_exists($main_specialism->name, $surgical_specialisms))
	  $surgical_specialisms[$main_specialism->name] = $main_specialism; 
	if($main_specialism->IsMedical() && !array_key_exists($main_specialism->name, $medical_specialisms))
	  $medical_specialisms[$main_specialism->name] = $main_specialism; 
	if(!array_key_exists($main_specialism->name, $specialisms_list))
	  $specialisms_list[$main_specialism->name] = $main_specialism; 

	if(isset($num_dept_have_spec[$main_specialism->name]))
	  $num_dept_have_spec[$main_specialism->name]++;
	else
	  $num_dept_have_spec[$main_specialism->name] = 1;
      }

    foreach($department->aux_specialisms as $aux_specialism)
      {
	if($aux_specialism->IsSurgical() && !array_key_exists($aux_specialism->name, $surgical_specialisms))
	  $surgical_specialisms[$aux_specialism->name] = $aux_specialism; 
	if($aux_specialism->IsMedical() && !array_key_exists($aux_specialism->name, $medical_specialisms))
	  $medical_specialisms[$aux_specialism->name] = $aux_specialism; 
	if(!array_key_exists($aux_specialism->name, $specialisms_list))
	  $specialisms_list[$aux_specialism->name] = $aux_specialism; 

	if(isset($num_dept_have_spec[$aux_specialism->name]))
	  $num_dept_have_spec[$aux_specialism->name]++;
	else
	  $num_dept_have_spec[$aux_specialism->name] = 1;
      }
  }

ksort($specialisms_list);
$specialisms = count($specialisms_list);

// Splits surgical and medical specialisms and counts the total number of treatments (the treatment arrays are not used at the moment)

foreach($surgical_specialisms as $surgical_specialism)
  {
    foreach($surgical_specialism->surgical_treatments as $surgical_treatment)
      {
	if(!array_key_exists($surgical_treatment->name, $surgical_treatments))
	  $surgical_treatments[$surgical_treatment->name] = $surgical_treatment;
      }
  }

foreach($medical_specialisms as $medical_specialism)
  {
    foreach($medical_specialism->medical_treatments as $medical_treatment)
      {
	if(!array_key_exists($medical_treatment->name, $medical_treatments))
	  $medical_treatments[$medical_treatment->name] = $medical_treatment;
      }
  }
  
$treatments_list = array_merge((array)$surgical_treatments, (array)$medical_treatments);
$treatments = count($treatments_list);
ksort($treatments_list);
?>