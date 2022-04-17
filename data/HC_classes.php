<?php
/*
  OR_PASU_Generator version 2.0 May 4, 2014 
  -------------------------------------------------------------------------------------------------------------------------------------------
  This file is part of the instance generator used in:
 
  - Ceschia S. and Schaerf A. The Dynamic Patient Admission Scheduling with Operating Room Constraints, Flexible Horizon, and Patient Delays. 
  6th Multidisciplinary International Scheduling Conference (MISTA '13), 27-30 August 2013, Gent, Belgium.

  The generator is able to produce test cases of different dimensions and features, that are given as input data. The instances are written 
  in XML language and are validated against the XML schema. 

  The instance generator is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  Copyright (C) 2014 SaTT Group, DIEGM, University of Udine. 
  -------------------------------------------------------------------------------------------------------------------------------------------
*/

/*
  A class representing a treatment.
*/
class Treatment
{ 
  /* The constructor.
     @param t_name, the name of the treatment 
     @param t_type, the type of the treatment (medical or surgical)
     @param mean_LOS, the mean length of stay
     @param sd_LOS, the standard deviation of the length of stay
     @param mean_surgical_times, the mean duration of the surgery (optional, used only for surgical treatments)
     @param sd_surgical_times, the standard deviation of the surgery (optional, used only for surgical treatments)
  */
  public function __construct($t_name, $t_type, $mean_LOS, $sd_LOS, $mean_surgical_times = 0, $sd_surgical_times = 0)
  {
    $this->name = $t_name; 
    $this->type = $t_type;
    $this->ComputeLOS($mean_LOS, $sd_LOS); 
    $this->ComputeDuration($mean_surgical_times, $sd_surgical_times);
  }
  /*
    Checks if the treatment is surgical.
    @return true, if the treatment is a surgery
  */
  public function IsSurgical()
  {   return $this->type == "surgical"; }
  /* Given the mean and standard deviation of the length of this type surgery, it computes the actual duration by 
     taking a sample from a lognormal distribution. The value obtained is then approximate in order to have a minimun
     step of 5 minutes and a minimun duration of 15 minutes.
     @param mean_surgical_times, the mean duration of the surgery 
     @param sd_surgical_times, the standard deviation of the surgery 
  */
  public function ComputeDuration($mean_surgical_times, $sd_surgical_times)
  {
    if($this->type == "surgical")
      {
	$duration =$this->SampleLogNormalDistribution($mean_surgical_times, $sd_surgical_times);
	$this->surgery_length = max(15, round($duration/5)*5);
      }
    else
      $this->surgery_length = 0;
    settype($this->surgery_length, "integer");  
  }
  /* Given the mean and standard deviation of the length of stay, it computes the actual los by 
     taking a sample from a lognormal distribution. 
     @param mean_LOS, the mean los
     @param sd_LOS, the standard los 
  */
  public function ComputeLOS($mean_LOS, $sd_LOS)
  {
    $this->los = max(1, $this->SampleLogNormalDistribution($mean_LOS, $sd_LOS)); 
    settype($this->los, "integer");    
  }
  /*
    Given the mean and standard deviation, it gets a sample from the lognormal distribution.
    @return the valued sampled
  */
  public function SampleLogNormalDistribution($mean, $sd)
  {
    //Scale the values ($mean and $sd refer to the underlying normal distribution)
    $ln_mean = log($mean*$mean/sqrt($sd*$sd+$mean*$mean));
    $ln_sd = sqrt(log(1+($sd*$sd/($mean*$mean))));	 
    $R_command_line = "R --vanilla -q --slave -e 'rlnorm(1, mean=". $ln_mean . ", sd=" . $ln_sd . ")'"; 
    $time_string = exec($R_command_line);
    list($s,$time) = explode(' ', $time_string);
    settype($time, "integer");
    return $time; 
  }
  /* The attributes of a treatment are:
     - $name, name
     - $type, type (medical or surgical)
     - $surgery_length, the surgery length (used only for surgical treatments)
     - $los, length of stay at the hospital
  */
  public $name, $type, $surgery_length, $los;
};
/*
  A class representing a specialism.
*/
class Specialism
{
  /* The constructor.
     @param s_name, the name of the specialism
     @param mean_los, the mean los of all the treatments provided by the specialism (optional)
     @param sd_los, the standard deviation of the los of all the treatments provided by the specialism (optional)
     @param mean_surgical_duration, the mean surgical duration of all the treatments provided by the specialism (optional)
     @param sd_surgical_duration, the standard deviation of the surgical duration of all the treatments provided by the specialism
  */
  public function __construct($s_name, $mean_los = 0, $sd_los = 0, $mean_surgical_duration = 0, $sd_surgical_duration = 0)
  {  
    $this->name = $s_name;
    $this->mean_los = $mean_los;
    $this->sd_los = $sd_los;
    $this->mean_surgical_duration = $mean_surgical_duration;
    $this->sd_surgical_duration = $sd_surgical_duration;
  }
  /*
    Gets the total number of medical treatments provided by the specialism.
    @return the number of medical treatments
  */
  public function NumMedicalTreatments() {return count($this->medical_treatments);}
  /*
    Gets the total number of surgical treatments provided by the specialism.
    @return the number of surgical treatments
  */
  public function NumSurgicalTreatments() {return count($this->surgical_treatments);}
  /*
    Gets the total number of treatments provided by the specialism, either medical or surgical.
    @return the number of treatments
  */
  public function NumTotTreatments() {return count($this->medical_treatments) + count($this->surgical_treatments);}
  /*
    Gets a medical treatment from the array of medical treatments.
    @param i, the index of the treatment in the array
    @return a pointer to the Treatment object
  */
  public function GetMedicalTreatment($index) { return $this->medical_treatments[$index];}
  /*
    Gets a surgical treatment from the array of surgical treatments.
    @param i, the index of the treatment in the array
    @return a pointer to the Treatment object
  */
  public function GetSurgicalTreatment($index) { return $this->surgical_treatments[$index];}
  /*
    Gets a treatment (either medical or surgical).
    @param i, the index of the treatment. If the index is minor than the number of medical treatments, it gets a medical treatment. Otherwise, it gets a surgical treatment.
    @return a pointer to the Treatment object
  */
  public function GetTreatment($index) {
    if($index < $this->NumMedicalTreatments())
      return $this->GetMedicalTreatment($index);
    else
      return $this->GetSurgicalTreatment($index - $this->NumMedicalTreatments());
  }
  /*
    Adds a medical treatment to the corresponding list.
    @param med_treat, a Treatment object
  */
  public function AddMedicalTreatment($med_treat) { $this->medical_treatments[] = $med_treat; }
  /*
    Adds a surgical treatment to the corresponding list.
    @param med_treat, a Treatment object
  */
  public function AddSurgicalTreatment($sur_treat) { $this->surgical_treatments[] = $sur_treat; }
  /*
    Checks if there is at least one surgical treatment.
    return true, if there are surgical treatments.
  */
  public function IsSurgical()  {return $this->NumSurgicalTreatments() > 0; }
  /*
    Checks if there is at least one medical treatment.
    return true, if there are medical treatments.
  */
  public function IsMedical()  {return $this->NumMedicalTreatments() > 0; }
  /* The attributes of a specialism are:
     - $name, the name
     - $medical_treatments, the list of the medical treatment provided by the specialism
     - $surgical_treatments, the list of the surgical treatment provided by the specialism
     - $mean_surgical_duration, the mean surgical duration of all the treatments provided by the specialism
     - $sd_surgical_duration, the standard deviation of the surgical duration of all the treatments provided by the specialism
     - $mean_los, the mean los of all the treatments provided by the specialism
     - $sd_los, the standard deviation of the los of all the treatments provided by the specialism
  */
  public $name; 
  public $medical_treatments = array();
  public $surgical_treatments = array();
  public $mean_surgical_duration;
  public $sd_surgical_duration; 
  public $mean_los; 
  public $sd_los; 
};
/*
  A class representing a department of a hospital. Each department is qualified for the treatment of diseases of various specialisms, but at different levels of expertise (main or auxiliary specialism).
*/
class Department
{
  /* The constructor.
     @param d_name, the name
     @param min_a, the minimum age of patient admitted (optional)
     @param max_a, the maximum age of patient admitted (optional)
  */
  public function __construct($d_name, $min_a = 0, $max_a = 99)
  {
    $this->name = $d_name;
    $this->min_age = $min_a; 
    $this->max_age = $max_a; 
  }
  /*
    Gets the total number of specialisms for which the department is fully qualified.
    @return the number of main specialisms
  */
  public function NumMainSpecialisms() { return count($this->main_specialisms);}
  /*
    Gets the total number of specialisms for which the department is partially qualified.
    @return the number of auxiliary specialisms
  */ 
  public function NumAuxSpecialisms() { return count($this->aux_specialisms);}
  /*
    Adds a main specialism to the corresponding list.
    @param main_spec, a Specialism object
  */
  public function AddMainSpecialisms($main_spec) { $this->main_specialisms[] = $main_spec; }
  /*
    Adds an auxiliary specialism to the corresponding list.
    @param aux_spec, a Specialism object
  */
  public function AddAuxSpecialisms($aux_spec) { $this->aux_specialisms[] = $aux_spec; }
  /*
    Gets a main specialism from the array of main specialisms.
    @param i, the index of the specialism in the array
    @return a pointer to the Specialism object
  */
  public function GetMainSpecialism($index) { return $this->main_specialisms[$index];}
  /*
    Gets an auxiliary specialism from the array of auxiliary specialisms.
    @param i, the index of the specialism in the array
    @return a pointer to the Specialism object
  */
  public function GetAuxSpecialism($index) { return $this->aux_specialisms[$index];}
  /* The attributes of a department.
     - $name, the name of the department
     - $min_age, the minimun age of a patient
     - $max_age, the maximum age of a patient
     - $main_specialisms, the list of main specialism
     - $aux_specialisms, the list of auxiliary specialism
  */
  public $name, $min_age, $max_age;
  public $main_specialisms = array();
  public $aux_specialisms = array();

};
/*
  Some common functions
*/
function SamplePoissonDistribution($mean)
{
  $R_command_line = "R --vanilla -q --slave -e 'rpois(1, ". $mean . ")'";
  $string = exec($R_command_line);
  list($s,$value) = explode(' ', $string);
  settype($value, "integer");
  return $value; 
}
?>