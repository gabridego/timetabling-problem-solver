package pack;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
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
	private Map<Integer,Integer> assignment = new HashMap<>();
	private List<Set<Integer>> timeslots = new ArrayList<>();	// BEWARE!! Index == 0 is unused
	private List<Set<Integer>> acceptableExamsPerTimeslot;		// this will be kept up-to-date as mutations and crossovers happen
	private List<Integer> penaltyPerSlot = new ArrayList<>();
	private float fitness;
	
	private static int individualCounter = 0;
	private int individualId;
	
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
	
	public boolean checkFeasibility(Map<Integer,Integer> assignment, Integer[][] conflictMatrix) {
		boolean flag = true;
		for(Map.Entry<Integer,Integer> e1 : assignment.entrySet()) {
			for(Map.Entry<Integer,Integer> e2 : assignment.entrySet()) {
				if(e1.getValue() == e2.getValue() && conflictMatrix[e1.getKey()][e2.getKey()] != 0) {
					System.out.println("Conflicting exams " + e1.getKey() + " and " + e2.getKey() + " are both scheduled in slot " + e1.getValue());
					flag = false;
				}
			}
		}
		return flag;
	}
	
	public float computePenalty(Integer[][] conflictMatrix, Integer numberOfStudents) {
		float p = 0;
		Integer slot1,slot2;
		Integer[] conflicts;
		for(int exam1 = 1; exam1 < conflictMatrix.length; exam1++) {
			conflicts = conflictMatrix[exam1];
			slot1 = this.assignment.get(exam1);
			for(int exam2 = exam1 + 1; exam2 < conflicts.length; exam2++) {
				slot2 = this.assignment.get(exam2);
				if(conflicts[exam2] != 0 && Math.abs(slot1 - slot2) <= 5)
					p += Math.pow(2, 5 - Math.abs(slot1 - slot2))*conflicts[exam2];
			}
		}
		return p/numberOfStudents;
	}
	
	private void updateFitness(Integer exam, int formerSlot, int destTimeslot, Integer[][] matrix) {
		float p = 1/fitness*instance.getNumberOfStudents();
		int startIndex, endIndex;
			// Subtract the contribution to penalty of the previous position
		startIndex = (formerSlot - 5 < 1)? 1 : formerSlot-5;
		endIndex = (formerSlot + 5 > timeslots.size()-1)? timeslots.size()-1 : formerSlot+5;
		for (int i=startIndex; i<= endIndex; i++) {
			if (i == formerSlot) continue;		// ignore the former slot as there are no conflicts
			int stud_count = timeslots.get(i).stream().map(e -> matrix[exam][e]).mapToInt(Integer::intValue).sum();
			p -= Math.pow(2, 5 - Math.abs(i-formerSlot))*stud_count;
		}
			// Add the contribution of the new one
		startIndex = (destTimeslot - 5 < 1)? 1 : destTimeslot-5;
		endIndex = (destTimeslot + 5 > timeslots.size()-1)? timeslots.size()-1 : destTimeslot+5;
		for (int i=startIndex; i<= endIndex; i++) {
			if (i == destTimeslot) continue;		// ignore the final slot as there are no conflicts
			int stud_count = timeslots.get(i).stream().map(e -> matrix[exam][e]).mapToInt(Integer::intValue).sum();
			p += Math.pow(2, 5 - Math.abs(i-formerSlot))*stud_count;
		}
		this.fitness = 1 / (p/instance.getNumberOfStudents());
	}
	
	private void updateAcceptabilities(Integer exam, int formerSlot, int destTimeslot, Integer[][] matrix) {
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
		acceptableExamsPerTimeslot.get(formerSlot).add(exam);	// the given exam is also acceptable in the timeslots where it comes from
	}
	
	// Generation of an individual, greedy
	public Individual(Instance instance) {
		this.individualId = individualCounter++;
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
		Map<Integer,List<Integer>> possible = new HashMap<>();		//maps exam to list of its feasible timeslots
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
				possible = new HashMap<>();
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
		this.fitness = 1 / computePenalty(instance.getConflictMatrix(), instance.getNumberOfStudents());	//inverse objective function
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

	// List all the exams that can be moved to any given timeslots, for all timeslots
	private List<Set<Integer>> computeAcceptabilitiesPerTimeslot(){
		List<Set<Integer>> ret = new ArrayList<>();
		ret.add(new HashSet<>()); 	// empty list for timeslot 0, which is not used
		int k=0, nExams = instance.getNumberOfExams();
		Integer[][] matrix = instance.getConflictMatrix();
		List<Integer> originalList = instance.getExamList();
		for (Set<Integer> slot : this.timeslots) {
			if (k++ == 0) 	// timeslot 0 is fictious
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
	
	// Move an exam from its current assignment to a destination timeslot (use in conjunction with acceptabilities
	public void moveExam(Integer exam, Integer destTimeslot) {
		Integer formerSlot = assignment.get(exam);
		
		// Move the exam in the assignments and temporarily remove it from timeslots for computations
		timeslots.get(formerSlot).remove(exam);
		assignment.put(exam, destTimeslot);
		
		// Update acceptabilities
		updateAcceptabilities(exam, formerSlot, destTimeslot, instance.getConflictMatrix());
		/*Integer[][] matrix = instance.getConflictMatrix();
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
		acceptableExamsPerTimeslot.get(formerSlot).add(exam);	// the given exam is also acceptable in the timeslots where it comes from*/
		
		// Update fitness, to avoid recomputing it entirely
		updateFitness(exam, formerSlot, destTimeslot, instance.getConflictMatrix());
		/*float p = 1/fitness*instance.getNumberOfStudents();
		int startIndex, endIndex;
			// Subtract the contribution to penalty of the previous position
		startIndex = (formerSlot - 5 < 1)? 1 : formerSlot-5;
		endIndex = (formerSlot + 5 > timeslots.size()-1)? timeslots.size()-1 : formerSlot+5;
		for (int i=startIndex; i<= endIndex; i++) {
			if (i == formerSlot) continue;		// ignore the former slot as there are no conflicts
			int stud_count = timeslots.get(i).stream().map(e -> matrix[exam][e]).mapToInt(Integer::intValue).sum();
			p -= Math.pow(2, 5 - Math.abs(i-formerSlot))*stud_count;
		}
			// Add the contribution of the new one
		startIndex = (destTimeslot - 5 < 1)? 1 : destTimeslot-5;
		endIndex = (destTimeslot + 5 > timeslots.size()-1)? timeslots.size()-1 : destTimeslot+5;
		for (int i=startIndex; i<= endIndex; i++) {
			if (i == destTimeslot) continue;		// ignore the final slot as there are no conflicts
			int stud_count = timeslots.get(i).stream().map(e -> matrix[exam][e]).mapToInt(Integer::intValue).sum();
			p += Math.pow(2, 5 - Math.abs(i-formerSlot))*stud_count;
		}
		this.fitness = 1 / (p/instance.getNumberOfStudents());*/
		
		// Place the exam in the new timeslot, now that all computations are done
		timeslots.get(destTimeslot).add(exam);
	}
	
	// Move a randomly chosen exam to another timeslot, maintaining feasibility. Returns false if no mutations were possible
	public boolean mutate() {
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
		if (acceptables.isEmpty()) return false; 	// no mutations could be performed
		Collections.shuffle(acceptables);
		int exam = acceptables.get(rng.nextInt(acceptables.size()));
		
		// Move the chosen exam in the new timeslot
		moveExam(exam, slot);
		return true;
	}
	
	// Compute penalty caused by each slot, returns double of right one, check
	private void computePenaltyPerSlot() {
		penaltyPerSlot = new ArrayList<>();
		int slot = 0, p, other;
		Integer[] conflicts;
		
		for(Set<Integer> exams : this.timeslots) {
			p = 0;
			if (slot == 0) { 	// timeslot 0 is fictious
				penaltyPerSlot.add(p);
				slot++;
				continue;
			}
			int startIndex = (slot - 5 < 1)? 1 : slot-5;
			int endIndex = (slot + 5 > timeslots.size()-1)? timeslots.size()-1 : slot+5;
			for(int exam : exams) {		//for each exam in slot
				conflicts = instance.getConflictMatrix()[exam];
				for(int i = 1; i < conflicts.length; i++) {
					other = this.assignment.get(i);		//timeslot of exam i
					if(other >= startIndex && other <= endIndex) {
						p += Math.pow(2, 5 - Math.abs(slot - other))*conflicts[i]; 	//if the two exams are in same slot conflicts[i]==0
					}
				}
			}
			penaltyPerSlot.add(p);
			slot++;
		}
	}
	
	// Select two slots in a probabilistic manner and swap them
	public void swapSlots() {
		Random rng = new Random();	
		int slot1 = 0, slot2 = 0;
		
		computePenaltyPerSlot();
		//more penalty a slot cause more probable to choose it
		int tot = this.penaltyPerSlot.stream().mapToInt(Integer::intValue).sum();
		int value = rng.nextInt(tot) - this.penaltyPerSlot.get(slot1);
		while (value >= 0) {
			slot1 ++;
			value -= this.penaltyPerSlot.get(slot1);
		}
		value = rng.nextInt(tot) - this.penaltyPerSlot.get(slot2);
		while (value >= 0) {
			slot2 ++;
			value -= this.penaltyPerSlot.get(slot2);
		}
		
		if(slot1 == slot2)
			return;
		Integer[][] matrix = instance.getConflictMatrix();
		//update assignments, fitness and acceptabilities
		for(int exam : this.timeslots.get(slot1)) {
			this.assignment.put(exam, slot2);
			updateFitness(exam, slot1, slot2, matrix);
		}
		for(int exam : this.timeslots.get(slot2)) {
			this.assignment.put(exam, slot1);
			updateFitness(exam, slot2, slot1, matrix);
		}
		
		Set<Integer> tmp = this.timeslots.remove(slot1);
		this.timeslots.add(slot1, this.timeslots.remove(slot2));
		this.timeslots.add(slot2,tmp);
		this.acceptableExamsPerTimeslot = computeAcceptabilitiesPerTimeslot();
	}
	
	//empty an expensive timeslot and try to move the exams where they cost less, TODO: test
	public void desrupt() {
		Random rng = new Random();
		this.computePenaltyPerSlot();
		int tot = this.penaltyPerSlot.stream().mapToInt(Integer::intValue).sum(), slot=0, minp, newSlot = 0;
		int value = rng.nextInt(tot) - this.penaltyPerSlot.get(slot);
		while (value >= 0) {
			slot ++;
			value -= this.penaltyPerSlot.get(slot);
		}
		
		List<Integer> exams = new ArrayList<>(this.timeslots.remove(slot));
		Collections.shuffle(exams);
		this.timeslots.add(slot, new HashSet<>());
		this.acceptableExamsPerTimeslot.get(slot).addAll(exams);
		for(int exam : exams) {
			if(exam == 0)
				continue;
			minp = Integer.MAX_VALUE;
			for(int i = 1; i < this.acceptableExamsPerTimeslot.size(); i++) {	//we put an exam in the acceptable slot with that contribute less to total penalty
				if(this.acceptableExamsPerTimeslot.get(i).contains(exam) && this.penaltyPerSlot.get(i) < minp) {
					minp = this.penaltyPerSlot.get(i);
					newSlot = i;
				}
			}
			this.assignment.put(exam, newSlot);
			this.timeslots.get(newSlot).add(exam);
			this.updateFitness(exam, slot, newSlot, instance.getConflictMatrix());
			this.updateAcceptabilities(exam, slot, newSlot, instance.getConflictMatrix());
		}
	}
	
	// Create a new Individual, copy of the first (beware of references)
	private Individual(Individual toCopy) {
		this.fitness = toCopy.fitness;			// float
		this.individualId = toCopy.individualId;	// int
		this.instance = toCopy.instance;			// immutable Instance
		this.assignment = new HashMap<Integer, Integer>(toCopy.assignment);	// mutable Map
		this.penaltyPerSlot = new ArrayList<>(toCopy.penaltyPerSlot);		// mutable List
		
		this.acceptableExamsPerTimeslot = new ArrayList<Set<Integer>>();
		this.timeslots = new ArrayList<Set<Integer>>();
		List<Set<Integer>> ts = toCopy.timeslots;
		List<Set<Integer>> acc = toCopy.acceptableExamsPerTimeslot;
		for (int i=0; i<ts.size(); i++) {
			this.acceptableExamsPerTimeslot.add(new HashSet<Integer>(acc.get(i)));
			this.timeslots.add(new HashSet<Integer>(ts.get(i)));
		}
	}
	
	// Wrapper for the private constructor above. Useful to preserve the previous solution, in case some operations fail.
	public Individual clone() {
		return new Individual(this);
	}
	
	// Crossover
	/* 
	 * TODO: add percentage to choose the portion to modify
	 * TODO: is partially mapped an option?
	 * TODO: decide if we need an exception to terminate, and if it will only be used for crossover() of also for mutate()
	 */
	public List<Individual> crossover(Individual parent2){
		List<Individual> ret = new ArrayList<>();
		Individual p1 = this.clone(), p2=parent2.clone();
		
		return ret;
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
	
	public int getId() {
		return individualId;
	}

}
