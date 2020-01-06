package pack;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

public class Individual {
	//private static final int MAX_ITER = 12;
	//private static final int SEED = 42;
	
	private Instance instance;
	private Map<Integer,Integer> assignment = new TreeMap<>();
	private List<Set<Integer>> timeslots = new ArrayList<>();	// BEWARE!! Index == 0 is unused
	private List<Set<Integer>> acceptableExamsPerTimeslot;					// this will be kept up-to-date as mutations and crossovers happen
	private float fitness;
	
	// Returns true if exam is in conflict with another one scheduled in slot
	public Boolean hasConflict(Integer slot, Integer exam, Map<Integer,Integer> assignment, Integer[][] conflictMatrix) {
		for(Map.Entry<Integer,Integer> e : assignment.entrySet()) {
			if(e.getValue() == slot) {
				if(conflictMatrix[exam][e.getKey()] != 0)
					return true;
			}
		}
		return false;
	}
	
	public void checkFeasibility(Map<Integer,Integer> assignment, Integer[][] conflictMatrix) {
		for(Map.Entry<Integer,Integer> e1 : assignment.entrySet()) {
			for(Map.Entry<Integer,Integer> e2 : assignment.entrySet()) {
				if(e1.getValue() == e2.getValue() && conflictMatrix[e1.getKey()][e2.getKey()] != 0)
					System.out.println("Conflicting exams " + e1.getKey() + " and " + e2.getKey() + " are both scheduled in slot " + e1.getValue());
			}
		}
	}
	
	// Generation of an individual, greedy
	public Individual(Instance instance) {
		// METHOD 1: 	exams ordered for total number of conflicting students, for each exam randomly select a slot and if there is no conflict it
		// 				is assigned. if conflict, randomly try with the other slots, if no one is ok restart from the beginning. 
		// Random r = new Random(SEED);
		/*Random r = new Random();
		Integer slot, counter;
		List<Integer> tried;
		Boolean end = false;
		while(!end) {
			end = true;
			this.assignment = new TreeMap<>();
			for(int exam : instance.getConflictingStudents().keySet()) {
				counter = 0;
				tried = new ArrayList<>();
				do {
					do
						slot = r.ints(1, 1, instance.getNumberOfSlots() + 1).findFirst().getAsInt();
					while(tried.contains(slot));	//try once for each slot
					tried.add(slot);
				} while(hasConflict(slot, exam, this.assignment, instance.getConflictMatrix()) && counter++ < instance.getNumberOfSlots()-1);
				// try until a non conflicting slot is found, after MAX_ITER iterations restart generation
				
				if(counter >= instance.getNumberOfSlots()-1) {
					end = false;
					break;
				} else
					this.assignment.put(exam, slot);
			}
		}*/
		
		// METHOD 2:	based on number of possible slots per exam. map each exam to its possible slot and number of possible slots to exam with that number.
		//				at each iteration randomly selects one of the exams with the lowest number of possible slots, one of its available slots, assigns it
		//				and updates possible slots for conflicting exams. in case an exam can't be placed anywhere restart.
		this.instance = instance;
		Random r = new Random();
		for (int i=0; i<instance.getNumberOfSlots()+1; i++)
			this.timeslots.add(new HashSet<>());
		Map<Integer,List<Integer>> possible = new TreeMap<>();		//maps exam to list of its feasible timeslots
		Map<Integer,List<Integer>> numPossible = new TreeMap<>();	//maps number of possible timeslots to list of exam with that number of possible slots
		Integer first;
		Integer exam,slot;
		Integer[] slots = new Integer[instance.getNumberOfSlots()], conflicts;
		for(int i = 1; i <= instance.getNumberOfSlots(); i++) {
			slots[i-1] = i;
		}
		numPossible.put(instance.getNumberOfSlots() - 1, new ArrayList<>());	//initialization
		for(int i = 1; i <= instance.getMaxExam(); i++) {
			possible.put(i, new ArrayList<>(Arrays.asList(slots)));
			numPossible.get(instance.getNumberOfSlots() - 1).add(i);
		}
		
		while(!possible.isEmpty()) {	//stop when all exams are assigned
			first = numPossible.keySet().iterator().next();
			exam = numPossible.get(first).get(r.nextInt(numPossible.get(first).size()));	//randomly selects one of the exam that can be placed in less slots
			//if(!this.assignment.containsKey(exam)) {
			conflicts = instance.getConflictMatrix()[exam];
			if(possible.get(exam).size() <= 0) {	//if exam can't be placed anywhere start again
				possible = new TreeMap<>();
				numPossible = new TreeMap<>();
				numPossible.put(instance.getNumberOfSlots() - 1, new ArrayList<>());
				for(int i = 1; i <= instance.getMaxExam(); i++) {
					possible.put(i, new ArrayList<>(Arrays.asList(slots)));
					numPossible.get(instance.getNumberOfSlots() - 1).add(i);
				}
				continue;
			}
			slot = possible.get(exam).get(r.nextInt(possible.get(exam).size()));	//get one of possible timeslots
			this.assignment.put(exam, slot);
			this.timeslots.get(slot).add(exam);
			possible.remove(exam);		//exam is assigned, is removed
			
			for(int i = 1; i < conflicts.length; i++) {		//update possible slots based on conflicts
				if(conflicts[i] != 0 && possible.containsKey(i))
					possible.get(i).remove(slot);
			}
			
			numPossible = new TreeMap<>();		//update number of possible slots
			for(int i : possible.keySet()) {
				if(!numPossible.containsKey(possible.get(i).size()))
					numPossible.put(possible.get(i).size(), new ArrayList<>());
				numPossible.get(possible.get(i).size()).add(i);
			}
			//}
		}
		
		// Compute fitness
		float p = 0;
		Integer slot2;
		for(int exam1 = 1; exam1 < instance.getConflictMatrix().length; exam1++) {
			conflicts = instance.getConflictMatrix()[exam1];
			slot = this.assignment.get(exam1);
			for(int exam2 = exam1 + 1; exam2 < conflicts.length; exam2++) {
				slot2 = this.assignment.get(exam2);
				if(conflicts[exam2] != 0 && Math.abs(slot - slot2) <= 5)
					p += Math.pow(2, 5 - Math.abs(slot-slot2))*conflicts[exam2];
			}
		}
		this.fitness = 1 / (p/instance.getNumberOfStudents());	//inverse objective function
		this.acceptableExamsPerTimeslot = computeAcceptabilitiesPerTimeslot();
	}
	
	public void printIndividual() {
		System.out.println(this.assignment);
	}
	
	public void printIndividual(String fileName) throws IOException {
		FileWriter fw = new FileWriter(fileName);
		PrintWriter pw = new PrintWriter(fw);
		for(Map.Entry<Integer, Integer> e : this.assignment.entrySet()) {
			//System.out.println(e.getKey() + " " + e.getValue());
			pw.println(e.getKey() + " " + e.getValue());
		}
		pw.close();
		//fw.close();
	}

	private List<Set<Integer>> computeAcceptabilitiesPerTimeslot(){
		List<Set<Integer>> ret = new ArrayList<>();
		ret.add(new HashSet<>()); 	// empty list for timeslot 0, which is not used
		int nExams = instance.getNumberOfExams();
		Integer[][] matrix = instance.getConflictMatrix();
		List<Integer> originalList = instance.getExamList();
		for (Set<Integer> slot : this.timeslots) {
			if (slot.equals(timeslots.get(0))) 	// timeslot 0 is fictious
				continue;
			Set<Integer> exams = new HashSet<>(originalList);	// new object to avoid modifications
			for (Integer exam : slot) {
				// Remove all the exams that are in conflict with those in the timeslot, 
				// and those exams themselves as they are already in.
				for (Integer otherExam=1; otherExam <= nExams; otherExam++)
					if (matrix[exam][otherExam] != 0)
						exams.remove(otherExam);
				exams.remove(exam);
			}
			ret.add(exams);		// the remaining ones can still be added to this timeslot without causing issues
		}
		return ret;
	}
	
	private void moveExam(Integer exam, Integer destTimeslot) {
		Integer formerSlot = assignment.get(exam);
		
		// Move the exam in the assignments and timeslots
		timeslots.get(formerSlot).remove(exam);
		assignment.put(exam, destTimeslot);
		
		// Update acceptabilities
		Integer[][] matrix = instance.getConflictMatrix();
		HashSet<Integer> conflicts = new HashSet<>();
		for (Integer other=1; other <= instance.getNumberOfExams(); other++)
			if (matrix[exam][other] != 0) {
				conflicts.add(other);
				// Then check if "roommates" in the former slot share this conflict,
				// to decide whether it can be removed or not.
				boolean is_shared = false;
				for (Integer formerRoommate : timeslots.get(formerSlot))
					if (matrix[formerRoommate][other] != 0) {
						is_shared = true;
						break;
					}
				if (!is_shared)	// conflict removed
					acceptableExamsPerTimeslot.get(formerSlot).add(other);
			}
		acceptableExamsPerTimeslot.get(destTimeslot).removeAll(conflicts);
		
		// TODO: update fitness as well!
	}
	
	public void mutate() {
		Random rng = new Random();
		
		// Pick a timeslot in a probabilistic manner
		List<Integer> tsProb = new ArrayList<>();
		acceptableExamsPerTimeslot.stream().map(set -> set.size()).forEachOrdered(n -> tsProb.add(n));
		int tot = tsProb.stream().mapToInt(Integer::intValue).sum();
		int slot = 0;
		int value = rng.nextInt(tot) - tsProb.get(slot);
		while (value >= 0) {
			slot ++;
			value -= tsProb.get(slot);
		}
		
		// Pick an acceptable exam for that timeslot
		List<Integer> acceptables = new ArrayList<>(acceptableExamsPerTimeslot.get(slot));
		Collections.shuffle(acceptables);
		int exam = acceptables.get(rng.nextInt(acceptables.size()));
		
		// Move the chosen exam in the new timeslot
		moveExam(exam, slot);
	}
	
	public Map<Integer, Integer> getAssignment() {
		return assignment;
	}

	public float getFitness() {
		return fitness;
	}
	
	public List<Set<Integer>> getTimeslots() {
		return timeslots;
	}

	public List<Set<Integer>> getAcceptableExamsPerTimeslot() {
		return acceptableExamsPerTimeslot;
	}

}
