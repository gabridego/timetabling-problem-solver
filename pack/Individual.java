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

	private Random rng = new Random();
	
	private Instance instance;
	private Map<Integer,Integer> assignment = new HashMap<>();
	private List<Set<Integer>> timeslots = new ArrayList<>();	// BEWARE!! Index == 0 is unused
	private List<Set<Integer>> acceptableExamsPerTimeslot;		// this will be kept up-to-date as mutations and crossovers happen
	private int[] penaltyPerSlot;
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
			p += Math.pow(2, 5 - Math.abs(i-destTimeslot))*stud_count;
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
				if(formerSlot > 0) {
					boolean is_shared = false;
					for (Integer formerRoommate : timeslots.get(formerSlot))
						if (matrix[formerRoommate][other] != 0) {
							is_shared = true;
							break;
						}
					if (!is_shared)	// conflict removed
						acceptableExamsPerTimeslot.get(formerSlot).add(other);
				}
			}
		acceptableExamsPerTimeslot.get(destTimeslot).removeAll(conflicts);
		if(formerSlot > 0)
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
		//Random rng = new Random();
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
			exam = numPossible.get(first).get(rng.nextInt(numPossible.get(first).size()));	//randomly selects one of the exam that can be placed in less slots
			//if(!this.assignment.containsKey(exam)) {
			conflicts = instance.getConflictMatrix()[exam];
			if(possible.get(exam).size() <= 0) {	//if exam can't be placed anywhere start again
				possible = new HashMap<>();
				numPossible = new TreeMap<>();
				numPossible.put(instance.getNumberOfSlots() - 1, new ArrayList<>());
				for(Set<Integer> ts : timeslots)
					ts.clear();
				for(int i = 1; i <= instance.getMaxExam(); i++) {
					possible.put(i, new ArrayList<>(Arrays.asList(slots)));
					numPossible.get(instance.getNumberOfSlots() - 1).add(i);
				}
				continue;
			}
			slot = possible.get(exam).get(rng.nextInt(possible.get(exam).size()));	//get one of possible timeslots
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
		System.out.println("Moving exam " + exam + " in slot " + destTimeslot);

		// Move the exam in the assignments and temporarily remove it from timeslots for computations
		timeslots.get(formerSlot).remove(exam);
		assignment.put(exam, destTimeslot);

		// Update acceptabilities
		updateAcceptabilities(exam, formerSlot, destTimeslot, instance.getConflictMatrix());

		// Update fitness, to avoid recomputing it entirely
		updateFitness(exam, formerSlot, destTimeslot, instance.getConflictMatrix());
		
		// Place the exam in the new timeslot, now that all computations are done
		timeslots.get(destTimeslot).add(exam);
	}

	// Move a randomly chosen exam to another timeslot, maintaining feasibility.
	public Individual mutate() {
		System.out.println("\nStarting exam mutation...");
		Individual toModify = this.clone();
		Individual toReturnIfError = this.clone();
		toReturnIfError.individualId = newId();
		//Random rng = new Random();
		toModify.computePenaltyPerSlot();

		// Pick a timeslot in a probabilistic manner based on penalty
		int slot = randomSlotByProbability(toModify.penaltyPerSlot);

		// Pick an acceptable exam for that timeslot in a random way (try to avoid local minima)
		//this.acceptableExamsPerTimeslot = computeAcceptabilitiesPerTimeslot();
		List<Integer> acceptables = new ArrayList<>(toModify.acceptableExamsPerTimeslot.get(slot));
		if (acceptables.isEmpty()) return toReturnIfError; 	// no mutations could be performed
		Collections.shuffle(acceptables);
		int exam = acceptables.get(rng.nextInt(acceptables.size()));

		// Move the chosen exam in the new timeslot
		toModify.moveExam(exam, slot);
		toModify.individualId = newId();		// if a mutation happened the ID is different
		return toModify;
	}

	// Compute penalty caused by each slot
	private void computePenaltyPerSlot() {
		penaltyPerSlot = new int[instance.getNumberOfSlots() + 1];
		int slot = 0, other;
		float res;
		Integer[] conflicts;

		for(Set<Integer> exams : this.timeslots) {
			if (slot == 0) { 	// timeslot 0 is fictious
				slot++;
				continue;
			}
			//int startIndex = (slot - 5 < 1)? 1 : slot-5;
			int endIndex = (slot + 5 > timeslots.size()-1)? timeslots.size()-1 : slot+5;
			for(int exam : exams) {		//for each exam in slot
				conflicts = instance.getConflictMatrix()[exam];
				for(int i = 1; i < conflicts.length; i++) {
					other = this.assignment.get(i);		//timeslot of exam i
					if(other > slot && other <= endIndex) {
						res = (float) (Math.pow(2, 5 - Math.abs(slot - other))*conflicts[i]); 	//if the two exams are in same slot conflicts[i]==0
						penaltyPerSlot[slot] += res;
						penaltyPerSlot[other] += res;
					}
				}
			}
			slot++;
		}
	}

	// Pick a random slot probabilistically based on penalty
	private int randomSlotByProbability(int[] probabilities) {
		//Random rng = new Random();
		int tot = Arrays.stream(probabilities).sum(), slot=0;
		int value = rng.nextInt(tot) - probabilities[slot];
		while (value >= 0) {
			slot ++;
			value -= probabilities[slot];
		}
		return slot;
	}

	// Select two slots in a probabilistic manner and swap them
	public Individual swapSlots() {
		int slot1 = 0, slot2 = 0;
		computePenaltyPerSlot();
		System.out.println("\nStarting slot swapping...");
		slot1 = randomSlotByProbability(penaltyPerSlot);
		slot2 = randomSlotByProbability(penaltyPerSlot);

		if(slot1 == slot2)  {
			System.out.println("Slot " + slot1 + " extracted two times, exit");
			return this;
		}

		System.out.println("Swapping exams " + slot1 + " and " + slot2 + "...");
		//Integer[][] matrix = instance.getConflictMatrix();
		//update assignments, fitness and acceptabilities
		for(int exam : this.timeslots.get(slot1)) {
			this.assignment.put(exam, slot2);
			//updateFitness(exam, slot1, slot2, matrix);
		}
		for(int exam : this.timeslots.get(slot2)) {
			this.assignment.put(exam, slot1);
			//updateFitness(exam, slot2, slot1, matrix);
		}
		Collections.swap(this.timeslots, slot1, slot2);

		System.out.println("Done!");
		this.fitness = 1 / computePenalty(instance.getConflictMatrix(), instance.getNumberOfStudents());	//inverse objective function
		this.acceptableExamsPerTimeslot = computeAcceptabilitiesPerTimeslot();
		this.individualId = individualCounter++;
		return this;
	}

	//empty an expensive timeslot and try to move the exams, TODO: decide if destination is slot that contributes more or less to total penalty
	public Individual desrupt() {
		//Random rng = new Random();
		System.out.println("\nStarting slot destruction...");
		this.computePenaltyPerSlot();
		int slot = 0, minp, newSlot = 0;
		List<Integer> possibleSlots = new ArrayList<>();
		slot = randomSlotByProbability(penaltyPerSlot);

		System.out.println("Slot " + slot + " extracted");
		List<Integer> exams = new ArrayList<>(this.timeslots.remove(slot));
		Collections.shuffle(exams);
		this.timeslots.add(slot, new HashSet<>());
		this.acceptableExamsPerTimeslot.get(slot).addAll(exams);
		for(int exam : exams) {
			if(exam == 0)
				continue;
			minp = Integer.MAX_VALUE;	//we consider slots with minimum contribution
			for(int i = 1; i < this.acceptableExamsPerTimeslot.size(); i++) {
				if(this.acceptableExamsPerTimeslot.get(i).contains(exam) && this.penaltyPerSlot[i] <= minp) {	//if a slot can accept the exam and has lowest contribution
					if(this.penaltyPerSlot[i] < minp) {		//new lowest contribution
						minp = this.penaltyPerSlot[i];
						possibleSlots.clear();
					}
					possibleSlots.add(i);
				}
			}
			newSlot = possibleSlots.get(rng.nextInt(possibleSlots.size()));		//randomly select one of available slots with lowest penalty
			this.assignment.put(exam, newSlot);
			this.timeslots.get(newSlot).add(exam);
			System.out.println("Exam " + exam + " moved to slot " + newSlot);
			this.updateFitness(exam, slot, newSlot, instance.getConflictMatrix());
			this.updateAcceptabilities(exam, slot, newSlot, instance.getConflictMatrix());
		}
		System.out.println("Done!");
		this.individualId = individualCounter++;
		return this;
	}

	// Create a new Individual, copy of the first (beware of references)
	private Individual(Individual toCopy) {
		this.fitness = toCopy.fitness;			// float
		this.individualId = toCopy.individualId;	// int
		this.instance = toCopy.instance;			// immutable Instance
		this.assignment = new HashMap<Integer, Integer>(toCopy.assignment);	// mutable Map
		this.penaltyPerSlot = toCopy.penaltyPerSlot;		// mutable List

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
	
	// debug function to test integrity	
	public boolean testIntegrity() {	
		if (instance.getNumberOfExams() != assignment.keySet().size())	// right # exams?	
			return false;	
		for (Integer exam : assignment.keySet()) {	// is each exam in its timeslot? 	
			Integer ts = assignment.get(exam);	
			if (!timeslots.get(ts).contains(exam)) {	
				System.out.println("Timeslot " + ts + " should contain exam " + exam);	
				return false;	
			}	
		}	
		for (int slot = 1; slot<timeslots.size(); slot++)	// are there extra exams in the timeslot?	
			for (Integer exam : timeslots.get(slot)) {	
				if (assignment.get(exam) != slot) {	
					System.out.println("Exam " + exam + " is somehow in timeslot ("+ slot + ") but should be in (" + assignment.get(exam));	
					return false;	
				}	
			}	
		this.acceptableExamsPerTimeslot = this.computeAcceptabilitiesPerTimeslot();	
		List<Integer> counts = new ArrayList<Integer>();	
		acceptableExamsPerTimeslot.stream().map(set -> set.size()).forEach(i -> counts.add(i));	
		System.out.println("# acceptables: " + counts);	
		return true;	
	}

	// Extract the chosen timeslots from the individual and return them (used in crossover). Place all the removed ones in timeslot 0.
	private Map<Integer, Set<Integer>> xoverExtract(Set<Integer> electedSlots) {
		Map<Integer, Set<Integer>> ret = new HashMap<>();
		electedSlots.forEach(slot -> {			// For each timeslot, change all the exam assignments to timeslot 0 (ausiliary). These will be reinserted later
			Set<Integer> removed = timeslots.set(slot.intValue(), new HashSet<Integer>());
			for (Integer ex : removed)
				assignment.replace(ex, 0);
			timeslots.get(0).addAll(removed);
			ret.put(slot, removed);
		});
		return ret;
	}

	// Remove those exams that would become duplicates in the new solution (used in crossover). Assignments will be replaced in the next step.
	private void xoverDuplicates(Map<Integer, Set<Integer>> incoming) {
		for (Integer slot : incoming.keySet())
			for (Integer exam : incoming.get(slot)) {	// For all the exams in the incoming timeslot, change their assignment and remove them from their current timeslot
				Integer pos = this.assignment.get(exam);
				this.timeslots.get(pos).remove(exam);		// Note that this also removes the exams that have been placed in timeslot 0 in the previous step
			}
	}

	// Insert the timeslots from the other solution
	private void xoverInsertOtherTimeslots(Map<Integer, Set<Integer>> incoming) {
		for (Integer slot : incoming.keySet()) {
			for (Integer exam : incoming.get(slot)) {
				System.out.println("Exam " + exam + " placed in slot " + slot);
				this.assignment.replace(exam, slot);
			}
			this.timeslots.set(slot, incoming.get(slot));
		}
	}

	// Crossover
	/* For each timeslot: 		extract the corresponding sets of exams from timeslots and remove all the 'exported' exams (--> xoverExtract() )
	 * 							remove the exams that are in common with the imported timeslot i.e. duplicates (--> xoverDuplicates() )
	 * 							insert each imported timeslot (--> xoverInsertOtherTimeslots() )
	 * 							reinsert the missing elements and throw an exception in case (--> xoverReinsertMissingExams() )
	 */
	public List<Individual> crossover(Individual parent2, float percentage){
		List<Individual> ret = new ArrayList<>();
		Individual p1 = this.clone(), p2=parent2.clone();	// p1 and p2 will be modified
		computePenaltyPerSlot();

		// Choose the timeslots to use for crossover probabilistically, based on penalty (on both sides): maybe moving a timeslot to the other solution improves it
		int nSlots = (int)(percentage * instance.getNumberOfSlots());
		nSlots = (nSlots < 1? 1 : nSlots);
		Set<Integer> electedSlots = new HashSet<>();
		while (electedSlots.size() < nSlots)
			electedSlots.add(randomSlotByProbability(this.penaltyPerSlot));
		System.out.println("\nStarting crossover on slots " + electedSlots + "...");
		if (!testIntegrity())	
			System.out.println("INTEGRITY ERROR BEFORE CROSSOVER!");

		// Extract the chosen timeslots, also marking all the removing as exams as 'missing' (i.e. assigned to -1)
		Map<Integer, Set<Integer>> extracted1 = p1.xoverExtract(electedSlots), extracted2 = p2.xoverExtract(electedSlots);
		// Prepare assignment and timeslots so that no duplicates will be formed by inserting the new assignments
		p1.xoverDuplicates(extracted2); p2.xoverDuplicates(extracted1);
		// Insert the timeslots coming from the other solution
		p1.xoverInsertOtherTimeslots(extracted2); p2.xoverInsertOtherTimeslots(extracted1);
		// Reinsert missing elements, throwing an exception in case it fails
		try {
			p1.xoverReinsertMissingExams(p1.timeslots.get(0));
			p2.xoverReinsertMissingExams(p2.timeslots.get(0));
			p1.timeslots.get(0).clear(); p2.timeslots.get(0).clear();
			p1.individualId=newId();
			p2.individualId=newId();
			ret.add(p1); ret.add(p2);		// feasible solutions reached, this is what will be returned
		}
		catch (CrossoverInsertionFailedException e) {	// failed to get back to feasibility, return parents
			Individual A = this.clone();
			Individual B = parent2.clone();
			A.individualId=newId();
			B.individualId=newId();
			ret.add(A);	// "this" is parent 1
			ret.add(B);
		}

		return ret;
	}

	// Insert the missing exams among the acceptable timeslots following an hardest-first policy like in the individual constructor.
	// Throws an exception if at least an exam can't be placed anywhere
	private void xoverReinsertMissingExams(Set<Integer> missingExams) throws CrossoverInsertionFailedException{
		Map<Integer, List<Integer>> possible = new HashMap<>(), numPossible = new TreeMap<>();
		this.acceptableExamsPerTimeslot = this.computeAcceptabilitiesPerTimeslot();
		System.out.println("Try to place exams " + missingExams + "...");
		
		for(int exam : missingExams)
			possible.put(exam, new ArrayList<>());
		for(int slot = 1; slot < this.acceptableExamsPerTimeslot.size(); slot++)	//inizialization, maps an exam to its possible slots
			for(int exam : missingExams)
				if(this.acceptableExamsPerTimeslot.get(slot).contains(exam))
					possible.get(exam).add(slot);
				
		int size, nfails = 0;
		for(int exam : missingExams) {		//inizialization, maps a slot to the number of slot that can be placed there
			size = possible.get(exam).size();
			if(size <= 0) {
				System.out.println("No possible slot for exam " + exam + ", exit!");
				nfails ++;	
				continue;	
			}	
			if (nfails > 0)	{	
				System.out.println("Total not placeable exams: " + nfails);
				throw new CrossoverInsertionFailedException();
			}
			if(!numPossible.containsKey(size))
				numPossible.put(size, new ArrayList<>());
			numPossible.get(size).add(exam);
		}
		
		Integer exam, first, slot;
		Integer[] conflicts;
		while(!possible.isEmpty()) {
			first = numPossible.keySet().iterator().next();
			exam = numPossible.get(first).get(rng.nextInt(numPossible.get(first).size()));	//randomly selects one of the exam that can be placed in less slots
			conflicts = instance.getConflictMatrix()[exam];
			if(possible.get(exam).size() <= 0) {
				System.out.println("No possible slot for exam " + exam + ", exit!");
				throw new CrossoverInsertionFailedException();
			}
			slot = possible.get(exam).get(rng.nextInt(possible.get(exam).size()));	//get one of possible timeslots
			this.assignment.put(exam, slot);
			this.timeslots.get(slot).add(exam);
			this.updateAcceptabilities(exam, 0, slot, instance.getConflictMatrix());
			System.out.println("Exam " + exam + " placed in slot " + slot);
			possible.remove(exam);		//exam is assigned, is removed

			for(int i = 1; i < conflicts.length; i++) {		//update possible slots based on conflicts
				if(conflicts[i] != 0 && possible.containsKey(i))
					possible.get(i).remove(slot);
			}

			numPossible.clear();		//update number of possible slots
			for(int i : possible.keySet()) {
				if(!numPossible.containsKey(possible.get(i).size()))
					numPossible.put(possible.get(i).size(), new ArrayList<>());
				numPossible.get(possible.get(i).size()).add(i);
			}
		}
	}
	
	//Method called from Population at hybridization step
	public Individual hybridize() {
		//TODO: First approach: first improvement
		//map<int,int> assignment exam,slot
		return new Individual(this);
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
	
	public int newId() {
		return individualCounter++;
	}

	public boolean isFeasible() {
		return this.checkFeasibility(assignment, instance.getConflictMatrix());
	}

}
