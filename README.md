# Patient Admission Scheduling (PAS)
Java 8 was used for this

## Onderzoeksonderwerpen

### Optimize workload equity
- Hoe wordt workkload equity beschreven?

### HBS algorithm
- Toon hoe de single objective procedure kan gebruikt worden om een
  approximation set te vinden.

### HBS with diverse initial set
- Om ervoor te zorgen dat de oplossingen divers zijn rekenen we eerst een
  set initiele oplossingen uit om diversiteit te verhogen.
    - Toon het verschil in qualiteit tussen de approximation sets en/of
      rekentijd.
    - Toon het verschil in diversiteit van de oplossingen.
    - Hoe wordt de initiele set bepaald?


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
