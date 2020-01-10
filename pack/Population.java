package pack;

import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
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
	private String outputFile;
	
	//Arbitrary parameters (NOT BAD at 0.9 0.1 0.7)
	final private float crossover = (float) 0.9;
	final private float mutation = (float) 0.1;
	final private float maxMovingProbability = (float) 0.7;
	
	public Population(Integer popSize, Instance instance, float percentage, long start, long duration, String outputFile) {
		this.popSize = popSize;
		this.outputFile = outputFile+"_DMOgroup07.sol";
		
		this.individualsToUpdatePerIteration=(int) (this.popSize*(percentage/100));
		if (this.individualsToUpdatePerIteration<1) {
			this.individualsToUpdatePerIteration=1;
		} else if(this.individualsToUpdatePerIteration>=this.popSize) {
			this.individualsToUpdatePerIteration=this.popSize-1;
		}
		
		this.start = start;
		this.duration=duration;
		
		this.genOpProbabilities = new float[2];
		this.genOpProbabilities[0]=crossover;
		this.genOpProbabilities[1]=mutation;
		
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
		//TODO: possibly do something more in case crossover/mutation failed at previous iteration
		float passedTimePercentage = (float) 100/( (float) duration / ( (float) (System.nanoTime()-this.start) ) );	//compute percentage of elapsed time
		System.out.println(passedTimePercentage+"% of the available time has passed");
																													//Starting from a fixed max amount of movable probability
		float movingProbability = (float) this.maxMovingProbability*passedTimePercentage/100;						//move a portion of that amount proportionally to elapsed time
		
		this.genOpProbabilities[0] = this.crossover - movingProbability;											//from the crossover (most probable at beginning)
		this.genOpProbabilities[1] = this.mutation + movingProbability;												//to mutation (most probable at end)
		
		System.out.println("");
		System.out.println("New probabilities:");
		System.out.println("	crossover: "+this.genOpProbabilities[0]);
		System.out.println("	mutation: "+this.genOpProbabilities[1]);
		
		try {																										//TODO: remove this waiting block
			Thread.sleep(250);
		} catch (InterruptedException e) {}
	}
	
	private Individual[] hybridization(Individual[] offsprings) {
		Individual[] hybridizedOffsprings = new Individual[individualsToUpdatePerIteration];						//new array with same size as offspings
		int c = 0;
		for(Individual i : offsprings) {
			hybridizedOffsprings[c] = i.hybridize();																//new offsprings are the improved ones
			c++;
		}
		return hybridizedOffsprings;
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

/*		
		int iteratCnt = 1;
		Random rand = new Random();
		while(iteratCnt>0 && (System.nanoTime()-start)<duration) {							//Endless loop - TODO: remove time constraint
			System.out.println("Iteration: "+iteratCnt);
			
			
			//1. Select individuals for reproduction
			Map<Integer, Float> strongestFitnessMap =										//Map with the elements to reproduce (plus one)
				    fitnessMap.entrySet().stream()											//i.e. if want to reproduce 2 elements --> map has 3
				       .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))		//and one of them will be removed randomly
				       .limit(individualsToUpdatePerIteration+1)
				       .collect(Collectors.toMap(
				    		   Map.Entry::getKey, Map.Entry::getValue, (e1,e2) -> e1, LinkedHashMap::new));
			Random q = new Random();														//Get a random number in range [0, individualsToUpdatePerIteration+1]
			int indexOfElemToBeRemoved = q.nextInt(individualsToUpdatePerIteration+1);		//that will correspond to the position of one of the elements in the map
			for(int i : strongestFitnessMap.keySet()) {										//the element will be removed in order to have a kind of
				if (indexOfElemToBeRemoved == 0) {											//randomization in the choice of the best elements to reproduce
					strongestFitnessMap.remove(i);
					break;
				}
				indexOfElemToBeRemoved--;
			}																				//strongestFitnessMap now contains the (randomized) individuals to reproduce
			
			
			//2. Reproduction
			this.adjustProbabilities();													//Rebalance probabilities according to elapsed time
			int r = rand.nextInt(100) + 1;												//Generate random number in range [1,100]
			Individual[] offsprings = new Individual[individualsToUpdatePerIteration];	//the amount of generated offsprings is the same of the substituted ones
			
			int reproducedElem = 0;														//keep count of how many reproduced up to now
			boolean crossoverFlag = false;												//crossover takes two elements --> this is needed to skip an element
			int tmpElem = -1;															//to store temporarily an element before crossover
			
			for (int i : strongestFitnessMap.keySet()){									//loop on the IDs of the individuals to reproduce
				//System.out.println("CURRENT POPULATION: ");
				//for (Individual k : pop) {
				//	System.out.println(k.getId());
				//}
				if (crossoverFlag == true) {											//intercept this loop if crossover must be done
					Individual A=null;
					Individual B=null;
					for (Individual ind : pop) {										//find the two individuals
						//System.out.println("Looking for "+i+" and "+tmpElem+" and found "+ind.getId());
						if (ind.getId()==i) {
							A = ind;
							//System.out.println("found A at "+i);
						}
						if (ind.getId()==tmpElem) {
							B = ind;
							//System.out.println("found B at "+tmpElem);
						}
					}
					
					List<Individual> l = A.crossover(B, (float)0.1);
					
					reproducedElem++;													//mark this element as reproduced
					tmpElem=0;															//reused just as a counter
					for(Individual off : l) {											//copy returned list into the array of the offsprings
						offsprings[reproducedElem-1-tmpElem] = off;
						tmpElem++;
					}
					
					crossoverFlag=false;												//mark crossover as happened
					System.out.println("crossover end");
					continue;															//go to next element
				}
																						//Pick gen. op according to generated number and probabilities
				if (r<=genOpProbabilities[0]*100 && (individualsToUpdatePerIteration-reproducedElem)>1) { //crossover can be one if there are at least 2 elements to reproduce
					System.out.println("crossover start");
					crossoverFlag = true;												//flag that crossover is picked, setting up and ready to happen
					tmpElem=i;															//store the ID of this individual
					reproducedElem++;													//mark it as reproduced
					continue;															//go to next element
				} else {
					System.out.println("mutation");
					Individual A=null;
					for (Individual ind : pop) {										//find the two individuals
						//System.out.println("Looking for "+i+" and found "+ind.getId());
						if (ind.getId()==i) {
							A = ind;
							//System.out.println("found A at "+i);
							break;
						}
					}
					reproducedElem++;
					offsprings[reproducedElem-1]=A.mutate();
				}
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
			
*/			
			//4. Save results
			int keyOfBestSol = fitnessMap.entrySet().stream().min((entry1, entry2) -> entry1.getValue() > entry2.getValue() ? 1 : -1).get().getKey();	//Find optimal solution

			for (Individual ind : pop) {
				if (ind.getId()==keyOfBestSol) {
					try {
						System.out.println("Printing results to: "+this.outputFile);
						ind.printIndividual(this.outputFile);																							//print it
					} catch (IOException e) {
						System.out.println("FAILED PRINTING RESULTS! R.I.P.");
						e.printStackTrace();
					}
					break;
				}
			}
/*			
			iteratCnt++;
			System.out.println("");
			System.out.println("--------------------");
			System.out.println("");
		}
		
*/		
	}
}
