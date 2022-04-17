   /*
     OR_PASU_Generator version 2.0 May 4, 2014.
     -------------------------------------------------------------------------------------------------------------------------------------------
     Licence:
      These files implement the instance generator used in:
 
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
     Requirements:
     - PHP
     - R package
    -------------------------------------------------------------------------------------------------------------------------------------------
    Usage:
    The files HC_classes.php and HC_Data.php are necessary to run the generator. To generate an instance, just run:

    ./generatorXML.php <num_departments> <num_rooms> <num_features> <num_patients> <num_days> <num_or_rooms>

    Each instance is validated against the XML schema "OrPasInstance.xsd" and then printed in the standard output.
    -------------------------------------------------------------------------------------------------------------------------------------------
    Data:
    All the data necessary to generate an instance (statistical distributions, specialism types, treatments, ...), but not the one given as input, 
    is set in the "HC_data.php" file. 
    */
