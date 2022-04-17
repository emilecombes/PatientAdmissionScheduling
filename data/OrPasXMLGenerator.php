#!/usr/bin/php -q
<?php
   /*
     -------------------------------------------------------------------------------------------------------------------------------------------
     This file is part of the instance generator used in
 
     - Ceschia S. and Schaerf A. The Dynamic Patient Admission Scheduling with Operating Room Constraints, Flexible Horizon, and Patient Delays. 
     6th Multidisciplinary International Scheduling Conference (MISTA '13), 27-30 August 2013, Gent, Belgium.

     The generator is able to produce test cases of different dimensions and features, that are given as input data. The instances are written 
     in XML language and are validated against the XML schema. 

     The instance generator is distributed in the hope that it will be useful,
     but WITHOUT ANY WARRANTY; without even the implied warranty of
     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
     GNU General Public License for more details.

     Copyright (C) 2014 SaTT Group, DIEGM, University of Udine. 
   */
date_default_timezone_set('Europe/Rome');

if ($argc < 7)
  {
    echo "Usage: " . $argv[0] . " <departments> <rooms> <features> <patients> <days> <or_rooms>\n";
    exit(1);
  }


// Reads the date from the command line input
$departments = $argv[1]; // number of departments
$rooms = $argv[2]; // number of rooms
$features = $argv[3]; // number of features
$patients = $argv[4]; // number of patients
$days = $argv[5]; // number of days of the planning horizon
$or_rooms = $argv[6]; // number of operating rooms


include "HC_classes.php"; 
include "HC_data.php"; 


//    ----------------------------------------------------------------------------------------------------------------------------------------
// Creates distributions for specialisms and departments

$surgical_specialisms_distribution = array();
$specialisms_distribution = array();
$departments_distribution = array(); 

foreach($specialisms_list as $spec_name => $specialism)
  {
    $specialisms_distribution[$spec_name] = rand(5, 10);
    // Correction introduced to link the distribution to the mean surgical duration
    if($specialism->IsSurgical() && $specialism->IsMedical())
      $surgical_specialisms_distribution[$spec_name] = ceil($specialism->mean_surgical_duration*$specialisms_distribution[$spec_name]*$prob_surgery/100); 
    else if($specialism->IsSurgical())
      $surgical_specialisms_distribution[$spec_name] = $specialism->mean_surgical_duration*$specialisms_distribution[$spec_name]; 
  }

// Builds the departments distribution from the specialism distribution and the number of departments that have the specialism
for($d = 0; $d < $departments; $d++)
  {
    $department = $department_list[$d];
    if(!isset($departments_distribution[$department->name]))
      $departments_distribution[$department->name] = 0; 
    foreach($department->main_specialisms as $main_specialism)  
      $departments_distribution[$department->name] += $specialisms_distribution[$main_specialism->name]/$num_dept_have_spec[$main_specialism->name];
    foreach($department->aux_specialisms as $aux_specialism)  
      $departments_distribution[$department->name] += $specialisms_distribution[$aux_specialism->name]/$num_dept_have_spec[$aux_specialism->name];
    $departments_distribution[$department->name] = ceil($departments_distribution[$department->name]); 
  }
 

$ors_day_slots = ceil($or_day_slots * $or_rooms); // total number of OR-slots available in a day
settype($ors_day_slots, "integer");
$or_rooms = ceil($or_rooms);
settype($or_rooms, "integer");

// Assignments used to compute the room and operating room utilization and infeasible cases
$day_spec_surgical_assignments = array(); // total duration of the assigned surgeries for a specific day and specialism
$day_spec_surgical_assignments_incumbent = array();  // total duration of the assigned surgeries for a specific day and specialism, considering only incumbent patients ($registration < $initial_solution_days)

//Initialization of arrays
 for($k = 0; $k < $days; $k++)
  {
    $day_name = CreateDateFromToday($today, $k); 
    foreach($surgical_specialisms as $spec_name => $specialism)
      {
	$day_spec_surgical_assignments[$day_name][$spec_name] = 0;
	$day_spec_surgical_assignments_incumbent[$day_name][$spec_name] = 0;
      }
  }

// statistics
$total_capacity = 0;
$total_occupancy = 0;
$daily_occupancy = array_fill(0,$days, 0);
$daily_surgery_occupancy = array_fill(0,$days, 0);
$avg_num_patients_day = ceil($patients/$days);

//Instance generation: to generate the instance in XML we use the DOMDOCUMENT class

$schema_file = "OrPasInstance.xsd";
$instance_name = "OR_PASU" . date('ymd-his');
$document = new DOMDocument('1.0', 'UTF-8');
$document->xmlStandalone = false;
$document->formatOutput = true;

$instance = $document->createElement("OrPasu_instance");
$document->appendChild($instance);

$document->createAttributeNS( 'http://www.w3.org/2001/XMLSchema-instance', 'xsi:attr' );
 
$instance->setAttribute("xsi:schemaLocation", "https://bitbucket.org/satt/or-pas/raw/master/OrPasInstance.xsd");
$instance->setAttribute("name", $instance_name);

//Descriptor
$descriptor = $document->createElement("descriptor");
$instance->appendChild($descriptor);

$descriptor->appendChild($document->createElement("Departments", $departments));
$descriptor->appendChild($document->createElement("Rooms", $rooms));
$descriptor->appendChild($document->createElement("Features", $features));
$pat_node = $document->createElement("Patients", $patients);
$descriptor->appendChild($pat_node);
$descriptor->appendChild($document->createElement("Specialisms", $specialisms));

$horizon_entry = $document->createElement("Horizon");
$descriptor->appendChild($horizon_entry); 
$horizon_entry->setAttribute("start_day", CreateDateFromToday($today));
$horizon_entry->setAttribute("num_days", $days);
$descriptor->appendChild($document->createElement("Treatments", $treatments));

// Specialisms
$specialisms_entry = $document->createElement("specialisms");
$instance->appendChild($specialisms_entry);

foreach ($specialisms_list as $specialism)
  {
    $spec_name = $specialism->name; 
    $specialism_entry = $document->createElement("specialism", $spec_name);
    $specialisms_entry->appendChild($specialism_entry);
  }

// Features
$features_entry = $document->createElement("features");
$instance->appendChild($features_entry);

for($i = 0; $i < $features; $i++)
  {
    $feat_name = $room_property_names[$i]; 
    $feature_entry = $document->createElement("feature", $feat_name);
    $features_entry->appendChild($feature_entry);
  }

// Departments
$departments_entry = $document->createElement("departments");
$instance->appendChild($departments_entry);

for($i = 0; $i < $departments; $i++)
  {
    $dep_name = $department_list[$i]->name; 

    $department_entry = $document->createElement("department");
    $departments_entry->appendChild($department_entry);
    $department_entry->setAttribute("name", $dep_name);
    
    if($department_list[$i]->max_age <= 16)
      $department_entry->setAttribute("max_age", $department_list[$i]->max_age);
    if($department_list[$i]->min_age >= 65)
      $department_entry->setAttribute("min_age", $department_list[$i]->min_age);

    $main_specialisms_entry = $document->createElement("main_specialisms");
    $department_entry->appendChild($main_specialisms_entry);

    for($j = 0; $j < $department_list[$i]->NumMainSpecialisms(); $j++)
      {
	$s = $department_list[$i]->GetMainSpecialism($j)->name;
	$main_specialisms_entry->appendChild($document->createElement("main_specialism", $s));
      }

    $aux_specialisms_entry = $document->createElement("aux_specialisms");
    $department_entry->appendChild($aux_specialisms_entry);

    for($j = 0; $j < $department_list[$i]->NumAuxSpecialisms(); $j++)	 
      {
	$s = $department_list[$i]->GetAuxSpecialism($j)->name;
	$aux_specialisms_entry->appendChild($document->createElement("aux_specialism", $s));
      }
  }

// Rooms
$rooms_entry = $document->createElement("rooms");
$instance->appendChild($rooms_entry);

for($i = 0; $i < $rooms; $i++)
  {
    $room_entry = $document->createElement("room");
    $rooms_entry->appendChild($room_entry);

    $room_name = "R_" . $i;
    $capacity = $available_capacities[rand(0,count($available_capacities)-1)];
    $total_capacity += $capacity;
    $dep_name = SelectFromDistribution($departments_distribution);

    if($dep_name == "Maternity")
      $policy =  (rand(1,100) <= 60) ? 2 : 3; 
    else
      $policy = SelectFromDistribution($policy_distributions); 
 
    $room_entry->setAttribute("name", $room_name);
    $room_entry->setAttribute("capacity", $capacity);
    $room_entry->setAttribute("department", $dep_name);
    $room_entry->setAttribute("gender_policy", $policies[$policy]);

    $features_entry = $document->createElement("features");
    $room_entry->appendChild($features_entry);

    $num_features = rand(2, $features);
    $selected_features = array();
    for($j = 1; $j <= $num_features; $j++)
      {
	do 
	  $feature = rand(0,$features-1);
	while (array_search($feature,$selected_features) !== FALSE);
	$selected_features[] = $feature;
      }
    sort($selected_features);

    for($j = 0; $j < $num_features; $j++)
      {
	$s = $room_property_names[$selected_features[$j]];
	$features_entry->appendChild($document->createElement("feature", $s));
      }
  }

  
// Treatments
$treatments_entry = $document->createElement("treatments");
$instance->appendChild($treatments_entry);

foreach($specialisms_list as $spec_name => $specialism)
  {
    for($j = 0; $j < $specialism->NumTotTreatments(); $j++)
      {
	$treatment_entry = $document->createElement("treatment");
	$treatments_entry->appendChild($treatment_entry);
	$treat = $specialism->GetTreatment($j); 	   

	$treatment_entry->setAttribute("name", $treat->name);
	$treatment_entry->setAttribute("specialism", $specialism->name);

	if($treat->IsSurgical())
	  {
	    $surgery = 1;
	    $treatment_entry->setAttribute("length", $treat->surgery_length);
	  }
	else
	  $surgery = 0;
	$treatment_entry->setAttribute("surgery", $surgery);
      }
  }

// Master Surgical Schedule
$or_slots_entry = $document->createElement("or_slots");
$instance->appendChild($or_slots_entry);
$or_slots_entry->setAttribute("slot_length", $slot_length);

// The MSS is created through a routine that assigns OR-slots to specialism following the surgical specialism distributions. The timetable is created just for a period and then repeated for the whole planning horizon.

$spec_slots_ass = array(); // target number of slots to assign to a specialism in period following the surgical specialism distributions
$slots = array(); // slots to assign 
$day_slots = array(); // slots free in each day
$day_spec_slots = array(); // slots assigned to a specialism in a day

//Inizialization of data structures
for($d = 0;  $d<$days; $d++)
  {
    $day_name = CreateDateFromToday($today, $d);
    foreach($surgical_specialisms as $spec_name => $specialism)
      $day_spec_slots[$day_name][$spec_name] = 0;
  }

for($k = 0; $k < min($MSS_periodicity, $days); $k++)
  if(IsWorkingDay(CreateDateFromToday($today, $k)))
    $day_slots[$k] = $ors_day_slots;
  else
    $day_slots[$k] = 0;

$k = 0;

//Assignment of OR slots to specialism according to the specialism distribution
foreach($surgical_specialisms as $spec_name => $specialism)
  {
    $spec_slots_ass[$spec_name] = floor($surgical_specialisms_distribution[$spec_name]/array_sum($surgical_specialisms_distribution)*$ors_day_slots*$num_working_days);
    if($spec_slots_ass[$spec_name] == 0)//Each surgical specialism has at least 1 slot
      $spec_slots_ass[$spec_name] = 1;
  }

// To assign all the available slots to the specialisms
while($ors_day_slots*$num_working_days > array_sum($spec_slots_ass))
  {
    $spec_name = SelectFromDistribution($surgical_specialisms_distribution);
    $spec_slots_ass[$spec_name]++;
  }

foreach($surgical_specialisms as $spec_name => $specialism) 
  for($i = 0; $i < $spec_slots_ass[$spec_name]; $i++)
    {
      $slots[$k] =  $spec_name;
      $k++;
    }


// Greedy algorithm that creates the MSS: it randomly selects a slot of a specialism and it assigns to it the emptier day

while(count($slots) > 0)
  {
    $ss = rand(0, count($slots)-1); // It randomly selects a slot not already assigned
    // It looks for the emptier day
    $max = 0;
    $d = 0;
    for($k = 0; $k < count($day_slots); $k++)
      {
	if($day_slots[$k] > $max)
	  {
	    $max = $day_slots[$k];
	    $d = $k;
	  }	  
      }
    $day_name = CreateDateFromToday($today, $d);
      $day_spec_slots[$day_name][$slots[$ss]]++;
    unset($slots[$ss]);
    $slots = array_values($slots);
    $day_slots[$d]--;
  }

// Repeat the MSS for all the planning horizon
for($k = $MSS_periodicity; $k < $days; $k++) 
  {
    $original_day_name = CreateDateFromToday($today, $k%$MSS_periodicity);
    $day_name = CreateDateFromToday($today, $k); 	    
    foreach($surgical_specialisms as $spec_name => $specialism)
 	$day_spec_slots[$day_name][$spec_name] = $day_spec_slots[$original_day_name][$spec_name];
  }

// Write the xml entries
foreach($day_spec_slots as $day_name => $day_assignment)
  {
    $day_entry = $document->createElement("day");
    $or_slots_entry->appendChild($day_entry);
    $day_entry->setAttribute("name", $day_name);
    foreach($day_assignment as $spec_name => $slots)
      {
	if($slots > 0)
	  {
	    $specialism_entry = $document->createElement("specialism_assignment");
	    $day_entry->appendChild($specialism_entry);
	    $specialism_entry->setAttribute("name", $spec_name);
	    $specialism_entry->setAttribute("number_or_slots", $slots);	  //1 slot = 180 minutes

	  }
      }
    if(!$day_entry->hasChildNodes())
      $or_slots_entry->removeChild($day_entry);// remove the last day_entry
  }      
  
// Patients
$patients_entry = $document->createElement("patients");
$instance->appendChild($patients_entry);

$tot_patients = 0;
for($k = 0; $k < $days; $k++)
  {
    $npd = SamplePoissonDistribution($avg_num_patients_day);

    for($ii = 0; $ii < $npd; $ii++)
      {
	    $patient_name = "Pat_" . $tot_patients;
	    /* echo $patient_name . " " . $ii .  "\n" ;  */

	    $age = rand(1,$max_age);
	    if (rand(0,1))
	      $gender = "Female";
	    else
	      $gender = "Male";
    
	    $patient_entry = $document->createElement("patient");
	    $patients_entry->appendChild($patient_entry);

	    $patient_entry->setAttribute("name", $patient_name);
	    $patient_entry->setAttribute("age", $age);	  
	    $patient_entry->setAttribute("gender", $gender);	  
 
	    do{
	      /** 
		  Other conditions about patients and specialisms:
		  - no urological_surgery_spec for females
		  - no gynaecology_spec for males
		  - no pediatrics_spec for adults
		  - no neonatology_spec for people over 1 year
		  - no elderly_care up to 64 years
	      */
	      do
		$specialism = SelectFromDistribution($specialisms_distribution);
	      while(
		    ($specialism == "Urological_Surgery" && $gender == "Female") ||
		    ($specialism == "Gynaecology" && $gender == "Male") ||
		    ($specialism == "Pediatrics" && $age > 16) ||
		    ($specialism == "Neonatology" && $age > 1) ||
		    ($specialism == "Elderly_Care" && $age < 65) ||
		    (((DayOfTheWeek(CreateDateFromToday($today, $k)) == 6 && (($k == $days-2) ||($k == $days-1))) || 
		      (DayOfTheWeek(CreateDateFromToday($today, $k)) == 7 && ($k == $days-1))) && $specialisms_list[$specialism]->IsSurgical()) // no surgical patients if $k is a weekend day at the end of the planning horizon
		    );

	      if($specialisms_list[$specialism]->IsSurgical() && $specialisms_list[$specialism]->IsMedical())
		{
		  $surgical_patient =  (rand(1,100) <= $prob_surgery) ? 1 : 0;
		  if($surgical_patient)
		    {
		      $treatment = rand(0,$specialisms_list[$specialism]->NumSurgicalTreatments()-1); 
		      $treat = $specialisms_list[$specialism]->GetSurgicalTreatment($treatment); 
		    }
		  else
		    {
		      $treatment = rand(0,$specialisms_list[$specialism]->NumMedicalTreatments()-1);
		      $treat = $specialisms_list[$specialism]->GetMedicalTreatment($treatment);
		    }
		}
	      else if($specialisms_list[$specialism]->IsSurgical())
		{
		  $surgical_patient = 1; 
		  $treatment = rand(0,$specialisms_list[$specialism]->NumSurgicalTreatments()-1);
		  $treat = $specialisms_list[$specialism]->GetSurgicalTreatment($treatment);
		}
	      else
		{
		  $surgical_patient = 0; 
		  $treatment = rand(0,$specialisms_list[$specialism]->NumMedicalTreatments()-1);
		  $treat = $specialisms_list[$specialism]->GetMedicalTreatment($treatment);
		}

 
	      $urgent_patient =  (rand(1,100) <= $prob_urgency) ? 1 : 0;
	      $treat_time = ($surgical_patient == true) ? $treat->surgery_length : 0;	  
  
	      // Note: surgical patients and non-urgent patient can by admitted only on working days
	      if($urgent_patient && !$surgical_patient)
		{
		  $registration = $k;
		  $admission = $registration;
		}
	      else if((DayOfTheWeek(CreateDateFromToday($today, $k)) == 6 && (($k == $days-2) ||($k == $days-1))) || 
		       (DayOfTheWeek(CreateDateFromToday($today, $k)) == 7 && ($k == $days-1)))
		{
		  $registration = $k;
		  $admission = rand($k,$days-1);
		}
	      else
		{
		  $registration = $k;
		  do
		    {
		      $admission = (rand(0,1) == 0) ? rand($k,min($k+7,$days-1)) : rand($k,$days-1); // Two groups: up to a week, longer
		      $day_name = CreateDateFromToday($today, $admission);
		    }while(!IsWorkingDay($day_name));	   
		}

	      $length = min($treat->los, $days - $admission);
	      $discharge = $admission + $length;
	      $variability = (rand(1,100) <=  $over_stay_risk) ? 1 : 0; 
	      if($surgical_patient)
		{
		  $day_name = CreateDateFromToday($today, $admission); 
	      
		  if(DayOfTheWeek($day_name) == 5) // It is Friday
		    $surgery_day = $admission;
		  else
		    $surgery_day = min($admission + rand(0,1), $discharge-1);
		}

	      if ($urgent_patient)
		$max_admission = $admission;
	      else if (rand(0,9) == 0) // 10% of patients that cannot delayed more than a fix number of days (they could die in the mean time!)
		$max_admission = rand($admission, $days - $length - 1); 
	      else
		$max_admission = -1;

	      if($admission == $max_admission)
		$incumbent_patient = true;
	      else
		$incumbent_patient = false;
	      if($surgical_patient)
		{
		  $surgery_day_name = CreateDateFromToday($today, $surgery_day); 

		  // If the patient is incumbent (he/she is registered in the first days or can not be delayed) we check whether the assignment is feasible or not. Otherwise we check if there is at least one OR-slot available for the specialism in the day

		  if($incumbent_patient)	    
		    $condition = !CheckSurgicalFeasibility($day_spec_slots[$surgery_day_name][$specialism]*$slot_length, $day_spec_surgical_assignments_incumbent[$surgery_day_name][$specialism], $treat_time);
		  else 
		    $condition = ($day_spec_slots[$surgery_day_name][$specialism] == 0);
		}
	      else $condition = false; 
       
	    }while($condition);

	    // Update some statistics on the occupation
	    for ($j = $admission; $j < $admission + $length; $j++)
	      $daily_occupancy[$j]++;
	    $total_occupancy += $length;

	    $patient_entry->setAttribute("treatment",  $treat->name);	  

	    $capacity_preference = $available_capacities[rand(0,count($available_capacities)-1)];

	    // We update the assignments 
	    if($surgical_patient && $incumbent_patient)
	      $day_spec_surgical_assignments_incumbent[$surgery_day_name][$specialism] += $treat_time;
  	
	    if($surgical_patient)
	      $day_spec_surgical_assignments[$surgery_day_name][$specialism] += $treat_time;
    

	    //registration
	    $day_name = CreateDateFromToday($today, $registration);
	    $patient_entry->setAttribute("registration", $day_name);
	  
	    //admission
	    $day_name = CreateDateFromToday($today, $admission);
	    $patient_entry->setAttribute("admission", $day_name);
	
	    //surgery_day
	    if($surgical_patient)
	      {
		$day_name = CreateDateFromToday($today, $surgery_day);
		$patient_entry->setAttribute("surgery_day", $day_name);
	      }	  

	    //discharge
	    $day_name = CreateDateFromToday($today, $discharge);
	    $patient_entry->setAttribute("discharge", $day_name);	  
 
	    $patient_entry->setAttribute("variability", $variability);	  
	    if($max_admission != -1)
	      {	
		$day_name = CreateDateFromToday($today, $max_admission);
		$patient_entry->setAttribute("max_admission", $day_name);
	      }
	    if ($capacity_preference != $available_capacities[count($available_capacities)-1])	  
	      $patient_entry->setAttribute("preferred_capacity", $capacity_preference);	  

	    $room_properties_entry = $document->createElement("room_properties");
	    $patient_entry->appendChild($room_properties_entry);

	    // biased selection (quadratic)
	    $squared_num_features = rand(0, $features*$features);
	    $num_features = ceil(sqrt($squared_num_features));
	    if ($num_features > 0)
	      {
		$selected_features = array();
		for ($j = 1; $j <= $num_features; $j++)
		  {
		    do 
		      $feature = rand(0,$features-1);
		    while (array_search($feature,$selected_features) !== FALSE);
		    $selected_features[] = $feature;
		  }
		sort($selected_features);
		for ($j = 0; $j < $num_features; $j++)
		  {
		    $room_property_entry = $document->createElement("room_property");
		    $room_properties_entry->appendChild($room_property_entry);

		    $room_property_name = $room_property_names[$selected_features[$j]]; 
 
		    if($room_property_name == "television")
		      $need = "preferred"; 
		    else
		      $need = (rand(1,100) <= $prob_needed_feature) ? "needed" : "preferred";

		    $room_property_entry->setAttribute("name", $room_property_name);	  
		    $room_property_entry->setAttribute("type", $need);	  
		  }
	      }
	$tot_patients++;
      }
  }

//$document->save("filename.xml");

// The initial number of patients is corrected given that each day the arrivals are sampled from a Poisson distribution
$pat_new_node = $document->createElement("Patients", $tot_patients);
$descriptor->replaceChild($pat_new_node,$pat_node);


if($document->schemaValidate($schema_file))
  echo $document->saveXML();
else
  echo "Document not valid for schema " . $schema_file;

// Some statistics that can be shown to evaluate the instance
//echo "\nEND.\n";

/* echo "--------------------------------------------------\n"; */
/* echo "Total capacity = " . $total_capacity . " X " . $days . " = " . $total_capacity*$days . " beds*days\n"; */
/* echo "Total occupancy = " . $total_occupancy . " (" . $total_occupancy/($total_capacity*$days) * 100 . "%)\n"; */
/* echo "Daily occupancy = "; */
/* foreach($daily_occupancy as $day_occupancy) */
/* echo (int) (100 * $day_occupancy / $total_capacity) . "%, "; */
/* echo "\n"; */

/* ksort($day_spec_surgical_assignments); */

/* echo "Daily SPEC OR occupancy = \n"; */
/* foreach($day_spec_surgical_assignments as $day => $spec_surgical_assignments) */
/*   { */
/*     foreach($spec_surgical_assignments as $spec => $time) */
/*       { */
/* 	if($time > 0) */
/* 	      { */
/*  		echo  (int) (100 * $time/($day_spec_slots[$day][$spec]*$slot_length)) . "% "; */
/*  		/\* echo $day . " " . $spec . " " .  $time . " " . $day_spec_slots[$day][$spec] ."\n";		 *\/ */
/* 		$tot_OR_occupancy += 100 * $time/($day_spec_slots[$day][$spec]*$slot_length); */
/* 		$or_active_days++; */
/* 	      } */
/*       } */
/*     echo ", "; */
/*   } */
/* echo "\nOR average occupancy " . $tot_OR_occupancy/$or_active_days ."\n"; */

 
function SelectFromDistribution($distribution)
{
  $sum = array_sum($distribution);
  $val = rand(1,$sum);

  $accumulated_prob = 0;
  foreach($distribution as $j => $value)
    {
      if ($val <= $value + $accumulated_prob)
	break;
      $accumulated_prob +=  $value;
    }
  return $j;
}

/**
   Gets TRUE if the OR-time still available is sufficient
   @param day_spec_slots, the total OR time available
   @param day_spec_surgical_assignment, the time already assigned (occupied by other surgeries)
   @param length, the duration of the surgery
   @return true, if the time available is sufficient
*/
function CheckSurgicalFeasibility($day_spec_slots, $day_spec_surgical_assignment, $length)
{
  return($day_spec_surgical_assignment + $length <= $day_spec_slots);
}

/**
   Gets TRUE if the day is a working day (no Saturday or Sunday)
   @param day_name, the date in the format YYYY-MM-DD
   @return true, if the day is a working da
*/
function IsWorkingDay($day_name)
{
  $day_of_week = DayOfTheWeek($day_name);
  if($day_of_week == 6 || $day_of_week == 7)
    return false;

  return true;
}
/**
   Gets a numeric representation of the day of the week: 1 (for Monday) through 7 (for Sunday)
   @param day_name, the date in the format YYYY-MM-DD
   @return a numeric representation of the day
*/
function DayOfTheWeek($day_name) // Numeric representation of the day of the week (1 (for Monday) through 7 (for Sunday))
{
  $date = new DateTime($day_name);
  return date_format($date, 'N'); 
}
/**
   Creates a DateTime object by adding $n days to today, and return a date as string in the YYYY-MM-DD format.
   @param day_zero, a DateTime object to be considered the first day
   @param n, the number of days to add to day_zero
   @return the name of the day obtained by adding n days to day zero
*/
function CreateDateFromToday($day_zero, $n = 0) 
{
  $time_interval_string = "P" . $n . "D"; 
  $day_zero_string = $day_zero->format('Y-m-d');
  $day = new DateTime($day_zero_string);
  date_add($day, new DateInterval($time_interval_string)); 
  $day_name = $day->format('Y-m-d');
  return $day_name; 
}
?>