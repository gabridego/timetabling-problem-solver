package pack;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Population {
	private Integer popSize;
	private Individual[] pop;
	private int individualsToUpdatePerIteration;
	private long start;
	private long duration;
	private float[] genOpProbabilities;
	
	//Arbitrary parameters (NOT BAD at 0.9 0.5 0.5 0.7)
	final private float crossover = (float) 0.9;
	final private float mutation = (float) 0.05;
	final private float inversion = (float) 0.05;
	final private float maxMovingProbability = (float) 0.7;
	
	public Population(Integer popSize, Instance instance, float percentage, long start, long duration) {
		this.popSize = popSize;
		
		this.individualsToUpdatePerIteration=(int) (this.popSize*(percentage/100));
		if (this.individualsToUpdatePerIteration<1 || this.individualsToUpdatePerIteration>this.popSize) {this.individualsToUpdatePerIteration=1;}
		
		this.start = start;
		this.duration=duration;
		
		this.genOpProbabilities = new float[3];
		this.genOpProbabilities[0]=crossover;
		this.genOpProbabilities[1]=mutation;
		this.genOpProbabilities[2]=inversion;
		
		pop = new Individual[popSize];
		for(int i = 0; i < popSize; i++) {
			pop[i] = new Individual(instance);
			System.out.println(1/pop[i].getFitness());
		}
	}

	public Integer getPopSize() {
		return popSize;
	}

	public Individual[] getPopulation() {
		return pop;
	}
	
	private void adjustProbabilities() {
		float passedTimePercentage = (float) 100/( (float) duration / ( (float) (System.nanoTime()-this.start) ) );	//compute percentage of elapsed time
		System.out.println(passedTimePercentage+"% of the available time has passed");
																													//Starting from a fixed max amount of movable probability
		float movingProbability = (float) this.maxMovingProbability*passedTimePercentage/100;						//move a portion of that amount proportionally to elapsed time
		
		this.genOpProbabilities[0] = this.crossover - movingProbability;											//from the crossover (most probable at beginning)
		this.genOpProbabilities[1] = (float) (this.mutation + movingProbability*0.66);								//to mutation (most probable at end)
		this.genOpProbabilities[2] = (float) (this.inversion + movingProbability*0.34);								//to inversion (more probable at end wrt beginning)
		
		System.out.println("");
		System.out.println("New probabilities:");
		System.out.println("	crossover: "+this.genOpProbabilities[0]);
		System.out.println("	mutation: "+this.genOpProbabilities[1]);
		System.out.println("	inversion: "+this.genOpProbabilities[2]);
		
		try {																										//TODO: remove this waiting block
			Thread.sleep(500);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private Individual[] hybridization(Individual[] offsprings) {
		//TODO: implement local search method to explore neighborhood of each offspring to improve it
		Individual[] result = offsprings;
		return result;
	}
	
	public void evolve() {
		//Method to be called on the Population object to start the evolutionary process after initialization
		/*
		 * STEPS:
		 * 0. Allocate a structure for storing fitness of each population element 					--> DONE: map
		 * 1. Select individuals for reproduction 													--> DONE
		 * 	1.1 Decide how many must reproduce 															--> DONE: percentage passed as Population argument
		 * 	1.2 Find the ones with best fitness 														--> DONE: stream the map, sort it and limit it
		 * 2. Reproduction through some genetic operators											--> W.I.P.
		 * 	2.1 Manage probabilistic aspect of genetic operator selection 								--> DONE: custom probability distribution system
		 * 		(possibly with varying probabilities at runtime, this may exploit the time arg)
		 *   2.1.1 Bigger use of crossover at beginning													--> DONE
		 *   2.1.2 Mutation to diversify (Comparison of best fitness with average fitness)				--> DONE
		 * 	2.2 Apply one of the three genetic operators: 												--> W.I.P.
		 * 		crossover (standard/order/partiallyMapped), mutation, inversion
		 *  2.3 Decide whether to improve offsprings or not through local search 						--> W.I.P.
		 *  	(hybridization --> memetic algorithm) 
		 * 3. Population updating
		 * 	3.1 select whether to use Population replacement or Steady state and with which parameters (elitist approach / % of pop subtituted) ( keep constant total population)
		 * 	3.2 select weakest elements of the population to be substituted
		 * 4. Save the result (write the file)
		 * 	4.1 find new best solution in the population
		 * 	4.2 write the file (pay attention to the format) with the new best solution
		 */
		//TODO: find a way to use the time argument --> suggestion: to affect probabilities of G.op 
		
		
		System.out.println("");
		System.out.println("--------------------");
		System.out.println("Beginning evolution:");
		
		
		// 0. Data structure allocation:
		Map<Integer,Float> fitnessMap = new HashMap<>(); 	// build a map to store couples: individualId - fitness
		for (Individual i : pop) {
			fitnessMap.put(i.getId(), i.getFitness());
		}
		//System.out.println(fitnessMap);

		
		int iteratCnt = 1;
		Random rand = new Random();
		while(iteratCnt>0 && (System.nanoTime()-start)<duration) {						//Endless loop - TODO: remove time constraint
			System.out.println("Iteration: "+iteratCnt);
			
			
			//1. Select individuals for reproduction
			Map<Integer, Float> strongestFitnessMap =										//Map with the only elements to reproduce
				    fitnessMap.entrySet().stream()
				       .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
				       .limit(individualsToUpdatePerIteration)
				       .collect(Collectors.toMap(
				    		   Map.Entry::getKey, Map.Entry::getValue, (e1,e2) -> e1, LinkedHashMap::new));
			//System.out.println(strongestFitnessMap);
			
			
			//2. Reproduction
			this.adjustProbabilities();										//Rebalance probabilities according to elapsed time
			int r = rand.nextInt(100) + 1;												//Generate random number in range [1,100]
			Individual[] offsprings = new Individual[individualsToUpdatePerIteration];	//the amount of generated offsprings is the same of the substituted ones			
			if (r<=genOpProbabilities[0]*100) {											//Pick genetic operator according to generated number and probabilities
				//TODO: implement crossover --> add offsprings to their array
				System.out.println("crossover");
			} else if (r<=100*(genOpProbabilities[0]+genOpProbabilities[1])) {
				//TODO: implement mutation --> add offsprings to their array
				System.out.println("mutation");
			} else {
				//TODO: implement inversion --> add offsprings to their array
				System.out.println("inversion");
			}
			
			//Hybridization step:
			offsprings = this.hybridization(offsprings);
			
			
			//3. Population updating
			Map<Integer, Float> weakestFitnessMap =										//Map with the elements to substitute
				    fitnessMap.entrySet().stream()
				       .sorted(Map.Entry.comparingByValue()) 	
				       .limit(individualsToUpdatePerIteration)
				       .collect(Collectors.toMap(
				    		   Map.Entry::getKey, Map.Entry::getValue, (e1,e2) -> e1, LinkedHashMap::new));
			//System.out.println(weakestFitnessMap);
			//TODO: add elitist approach to use in case individualsToUpdatePerIteration = popSize
			
			
			//4. Save results
			
			iteratCnt++;
			System.out.println("");
			System.out.println("--------------------");
			System.out.println("");
		}
		
		
	}
}
