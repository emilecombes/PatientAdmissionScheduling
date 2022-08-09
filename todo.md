# TODO's

[] Repair procedure when initial solution isn't feasible.
[] Correct JSON output

## Global
[x] Efficient swap patient
[] Add overstay risk costs
[] Correct updating load cost
[] Is equity cost correct?

## Analytics
[] Add aborted while generating moves to map
[x] Add time to find new solution
[] (later) Method to compare schedules

## Production
[x] Create class with all global variables
[x] Make all configurable parameters external
[] Compact output files
[x] Jar executable
[x] Bash script

## Box splitting
[x] Copy Schedule
[x] Reset Schedule
[x] Create optPatientComfort()
[] Create optWorkloadEquity(epsilon)
[x] Maintain approximation set


## Tomorrow
[x] Class to store a found solution

### Solution
Map<Patient, Integer> delays
Map<Patient, Integer> assignedRooms
Map<Room, List<Set<Patient>>> rooms
Map<String, Integer> costs




