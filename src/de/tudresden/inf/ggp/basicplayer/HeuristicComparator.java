package de.tudresden.inf.ggp.basicplayer;

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
		/*
		 * hmm, keine ahnung warum du dies nach strategy unterscheiden willst
		 * ich lass es mal drin
		 * -- konrad
		 *
		if(this.strategy instanceof TwoPlayerStrategy) {
			TwoPlayerStrategy strat = (TwoPlayerStrategy) strategy;
			
			if(strat.heuristic.get(arg0.getState()) == null && strat.heuristic.get(arg1.getState()) == null) return 0;
			if(strat.heuristic.get(arg0.getState()) == null) return 1;
			if(strat.heuristic.get(arg1.getState()) == null) return -1;
			
			if(strat.heuristic.get(arg1.getState()) == strat.heuristic.get(arg0.getState())) return 0;
			return (strat.heuristic.get(arg1.getState()) < strat.heuristic.get(arg0.getState())) ? 1 : -1;
		} else if(this.strategy instanceof IDSStrategy) {
			IDSStrategy strat = (IDSStrategy) strategy;
			
			if(strat.heuristic.get(arg0.getState()) == null && strat.heuristic.get(arg1.getState()) == null) return 0;
			if(strat.heuristic.get(arg0.getState()) == null) return 1;
			if(strat.heuristic.get(arg1.getState()) == null) return -1;
			
			if(strat.heuristic.get(arg1.getState()) == strat.heuristic.get(arg0.getState())) return 0;
			return (strat.heuristic.get(arg1.getState()) < strat.heuristic.get(arg0.getState())) ? 1 : -1;
		} else {
			return 0;
		}
*/	}

}
