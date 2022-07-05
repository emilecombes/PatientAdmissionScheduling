# Patient Admission Scheduling (PAS)

## Building Approximation Set

_How to find a complete set of non-dominated solutions?_

- Set<Schedule> approximationSet

- void buildInitialSchedule()
  _Builds a schedule where all patients are admitted with random insert._
- void optimizePatientComfort()
  _Builds a schedule optimized for patient comfort._
- void optimizeWorkloadEquity(Schedule initialSchedule, int epsilon)
  _Builds a schedule from intialSchedule optimized for workload equity
  where patient cost does not exceed epsilon._
- void removeDominatedSolutions()
  _Checks all schedules in approximationSet if they're dominated._
- int calculateNextEpsilon()
  _Uses the solutions in the approximation set to calculate the
  biggest square._

## Less Exhaustive Swap Patients

## Implementing Overstay Risk
