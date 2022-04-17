# The Dynamic Patient Admission Scheduling with Operating Room Constraints, Flexible Horizon, and Patient Delays

This repository contains instances, generator, solutions, and
validator for the problem defined in the paper *The Dynamic Patient
Admission Scheduling with Operating Room Constraints, Flexible
Horizon, and Patient Delays* by Sara Ceschia and Andrea
Schaerf. Journal of Scheduling, 19(4):377-389, 2016. 

## Update (July, 2018)

As pointed out by Yi-Hang Zhu, Tulio Toffolo, Wim Vancroonenburg and
Greet Vanden Berghe, there were some bugs on the computation of the
solution cost. The source code and the validator have been corrected accordingly and
we re-run all the experiments, whose results are reported on the tables below.

## Generator

The generator OrPasXMLGenerator.php generates instances of any size by running:

	$ ./OrPasXMLGenerator.php <departments> <rooms> <features> <patients> <days> <or_rooms>

The files `HC_classes.php` and `HC_Data.php` are necessary to run the generator.

##Instances

Generated instances are stored in folder `Instances`. Each instance is validated again the `OrPasInstance.xsd` XML schema. 
They are split into 6 families with the following main features:

| Family | Rooms | Departments | Operating Rooms | Specialisms | Treatments | Patients | Days
| :------| ------:| ------:| ------:| ------:| ------:| ------:| 
| Short1 | 25 | 2 | 2 | 9 | 15 | 391-439 | 14
| Short2 | 50 | 4 | 4 | 18 | 25 | 574-644 | 14
| Short3 | 75 | 6 | 5 | 23 | 35 | 821-925 | 14
| Long1 | 25 | 2 | 2 | 9 | 15 | 693-762 | 28
| Long2 | 50 | 4 | 4 | 18 | 25 | 1089-1169 | 28
| Long3 | 75 | 6 | 5 | 23 | 35 | 1488-1602 | 28

## Results

### Results of the dynamic (regular) solver in its best parameter configuration

| Instance | Avg     | Dev     | Med     | Best    
| :--------  | ---:|  ---:|  ---:|  ----: | 
| or_pas_dept2_short00 | 57906.39 | 418.93 | 57837 | 57088
| or_pas_dept2_short01 | 61897.74 | 705.05 | 61792 | 60482
| or_pas_dept2_short02 | 46469.58 | 395.60 | 46383 | 45600
| or_pas_dept2_short03 | 65014.68 | 680.09 | 64942 | 63825
| or_pas_dept2_short04 | 44318.84 | 398.31 | 44301 | 43111
| or_pas_dept4_short00 | 135772.60 | 690.62 | 135861 | 134426
| or_pas_dept4_short01 | 105933.68 | 1017.42 | 105748 | 103554
| or_pas_dept4_short02 | 109126.37 | 775.72 | 109036 | 107697
| or_pas_dept4_short03 | 99476.411 | 969.37 | 99319 | 98023
| or_pas_dept4_short04 | 92909.82 | 826.81 | 92860 | 91231
| or_pas_dept6_short00 | 161086.30 | 929.73 | 161055 | 158880
| or_pas_dept6_short01 | 165602.24 | 930.55 | 165668 | 163633
| or_pas_dept6_short02 | 165228.04 | 1378.19 | 165381 | 162297
| or_pas_dept6_short03 | 153397.32 | 882.10 | 153312 | 151617
| or_pas_dept6_short04 | 177889.16 | 778.41 | 177918 | 175799
| or_pas_dept2_long00 | 145463.30 | 1766.27 | 145658 | 141610
| or_pas_dept2_long01 | 137945.16 | 1028.02 | 137903 | 135869
| or_pas_dept2_long02 | 136800.20 | 1609.77 | 136594 | 133727
| or_pas_dept2_long03 | 112523.64 | 1365.98 | 112566 | 109089
| or_pas_dept2_long04 | 106351.86 | 844.39 | 106275 | 104609
| or_pas_dept4_long00 | 174088.28 | 1364.81 | 174166 | 170850
| or_pas_dept4_long01 | 207159.67 | 2022.69 | 207483 | 203036
| or_pas_dept4_long02 | 220895.89 | 1845.20 | 220997 | 217072
| or_pas_dept4_long03 | 256604.22 | 2299.89 | 257016 | 251793
| or_pas_dept4_long04 | 216852.85 | 2416.89 | 216968 | 208635
| or_pas_dept6_long00 | 412807.38 | 3266.53 | 412716 | 405401
| or_pas_dept6_long01 | 450951.08 | 4445.77 | 451416 | 439115
| or_pas_dept6_long02 | 372260.16 | 2752.94 | 372233 | 366010
| or_pas_dept6_long03 | 387728.40 | 2500.19 | 387791 | 382817
| or_pas_dept6_long04 | 265879.83 | 2312.38 | 265772 | 261854

### Results of the static (crystal-ball) solver in its best parameter configuration

| Instance | Avg     | Dev     | Med     | Best    
| :--------  | ---:|  ---:|  ---:|  ----: | 
| or_pas_dept2_short00 | 58680.51 | 293.23 | 58675 | 58081
| or_pas_dept2_short01 | 62004.45 | 647.72 | 62043 | 60487
| or_pas_dept2_short02 | 46107.98 | 186.59 | 46102 | 45476
| or_pas_dept2_short03 | 63288.43 | 300.22 | 63292 | 62420
| or_pas_dept2_short04 | 43867.27 | 266.38 | 43879 | 43252
| or_pas_dept4_short00 | 135187.06 | 467.68 | 135249 | 134198
| or_pas_dept4_short01 | 101130.65 | 431.88 | 101059 | 100284
| or_pas_dept4_short02 | 109100.10 | 488.88 | 109171 | 107977
| or_pas_dept4_short03 | 97679.63 | 386.94 | 97632 | 96869
| or_pas_dept4_short04 | 91632.33 | 430.59 | 91600 | 90799
| or_pas_dept6_short00 | 158647.02 | 442.15 | 158680 | 157732
| or_pas_dept6_short01 | 163009.90 | 595.52 | 162929 | 161705
| or_pas_dept6_short02 | 154832.88 | 733.25 | 154939 | 153438
| or_pas_dept6_short03 | 150459.28 | 588.58 | 150465 | 148935
| or_pas_dept6_short04 | 177892.48 | 464.72 | 177921 | 176651
| or_pas_dept2_long00 | 135489.74 | 1010.33 | 135492 | 133418
| or_pas_dept2_long01 | 135713.36 | 769.59 | 135786 | 133352
| or_pas_dept2_long02 | 128645.52 | 661.63 | 128537 | 127304
| or_pas_dept2_long03 | 108735.14 | 797.23 | 108635 | 107049
| or_pas_dept2_long04 | 106707.16 | 383.41 | 106756 | 105790
| or_pas_dept4_long00 | 168485.92 | 995.52 | 168367 | 166888
| or_pas_dept4_long01 | 202896.66 | 1016.09 | 203026 | 200683
| or_pas_dept4_long02 | 213373.73 | 578.12 | 213377 | 212172
| or_pas_dept4_long03 | 238857.12 | 1760.31 | 238729 | 235303
| or_pas_dept4_long04 | 197818.22 | 1341.73 | 197804 | 195143
| or_pas_dept6_long00 | 385031.02 | 2118.53 | 385123 | 379907
| or_pas_dept6_long01 | 426449.71 | 2092.63 | 426679 | 421770
| or_pas_dept6_long02 | 344382.53 | 1843.91 | 344351 | 340338
| or_pas_dept6_long03 | 374972.76 | 1308.84 | 374693 | 373143
| or_pas_dept6_long04 | 261185.47 | 1139.60 | 261118 | 258819


## Solutions

Best solutions are available in the folders `DynamicSolutions` and `StaticSolutions` for the two solvers, respectively.
Each solution is validated against the `OrPasSolution.xsd` XML schema.

## Validator

The solution validator is available as the C++ source file `or_pas_validator.cc`. The compilation command is provided on top of the file as a comment. The library `libxml++` needs to be installed.

# The Dynamic Patient Admission Scheduling Problem Under Uncertainty

Instances and solutions for the *Dynamic Patient Admission Scheduling Problem Under Uncertainty*, introduced by Sara Ceschia and Andrea Schaerf *Modeling and solving the dynamic patient admission scheduling problem under uncertainty* 
(Artificial Intelligence in Medicine, 2012), can be found at <https://bitbucket.org/satt/pasu>.