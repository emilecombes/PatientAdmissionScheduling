# TODO's

## Global
[x] Efficient swap patient
[] Add overstay risk costs
[] Correct updating load cost

## Analytics
[] Add aborted while generating moves to map
[] Add time to find new solution
[] (later) Method to compare schedules

## Production
[x] Create class with all global variables
[] Make all configurable parameters external
[] Compact output files
[] Jar executable
[] Bash script

## Box splitting
[] Copy Schedule
[] Reset Schedule
[x] Create optPatientComfort()
[] Create optWorkloadEquity(epsilon)
[] Maintain approximation set


## Tomorrow
[] Class to store a found solution

### Solution
Map<Patient, Integer> delays
Map<Patient, Integer> assignedRooms
Map<Room, List<Set<Patient>>> rooms
Map<String, Integer> costs

## Parameters to set
EXTEND
SA OPT_PC & OPT_WE
EXHAUSTIVE
SWAP_LOOPS
RESTART
TIMEOUT_NEW_SOLUTION
TIMEOUT_INSTANCE
TRADEOFF




