package pack;

public class Population {
	private Integer popSize;
	private Individual[] pop;
	
	public Population(Integer popSize, Instance instance) {
		this.popSize = popSize;
		pop = new Individual[popSize];
		for(int i = 0; i < popSize; i++) {
			pop[i] = new Individual(instance);
		}
	}

	public Integer getPopSize() {
		return popSize;
	}

	public Individual[] getPopulation() {
		return pop;
	}
}
