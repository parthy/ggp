package player;
/*package de.tudresden.inf.ggp.basicplayer;

import org.eclipse.palamedes.gdl.core.model.IGameState;
import org.eclipse.palamedes.gdl.core.simulation.IStrategy;

public class DFSHeuristic implements IHeuristic {

	IStrategy strategy;
	
	public DFSHeuristic(IStrategy strat) {
		this.strategy = strat;
	}
	
	@Override
	public int calculateHeuristic(IGameState state) {
		if(this.strategy instanceof IDSStrategy) {
			int min = 0;
			for(Integer val : ((IDSStrategy) strategy).heuristic.values()) {
				if (val < min) min = val;
			}
			return min-1;
		} else if(this.strategy instanceof TwoPlayerStrategy) {
			int min = 0;
			for(Integer val : ((TwoPlayerStrategy) strategy).heuristic.values()) {
				if (val < min) min = val;
			}
			return min-1;
		}
		return 0;
	}

}
*/