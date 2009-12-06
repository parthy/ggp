package player;


import java.util.Comparator;

import org.eclipse.palamedes.gdl.core.model.IGameNode;
import org.eclipse.palamedes.gdl.core.simulation.IStrategy;

public class HeuristicComparator implements Comparator<IGameNode> {
	private IStrategy strat;
	
	public HeuristicComparator(IStrategy strat) {
		this.strat = strat;
	}

	@Override
	public int compare(IGameNode arg0, IGameNode arg1) {
		//the decision for the heuristic falls in MainStrategy
		Double value1 = strat.getHeuristicValue(arg0);
		Double value2 = strat.getHeuristicValue(arg1);

		return value1.compareTo(value2);
	}

}
