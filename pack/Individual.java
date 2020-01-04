package pack;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

public class Individual {
	private static final int MAX_ITER = 12;
	//private static final int SEED = 42;
	
	private Map<Integer,Integer> assignment;
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
		// Random r = new Random(SEED);
		Random r = new Random();
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
				} while(hasConflict(slot, exam, this.assignment, instance.getConflictMatrix()) && counter++ < MAX_ITER);
				// try until a non conflicting slot is found, after MAX_ITER iterations restart generation
				
				if(counter >= MAX_ITER) {
					end = false;
					break;
				} else
					this.assignment.put(exam, slot);
			}
		}
		
		// Compute fitness
		float p = 0;
		Integer slot2;
		Integer[] conflicts;
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

	public Map<Integer, Integer> getAssignment() {
		return assignment;
	}

	public float getFitness() {
		return fitness;
	}

}
