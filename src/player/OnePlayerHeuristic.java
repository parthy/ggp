package player;

import java.util.HashMap;

import org.eclipse.palamedes.gdl.core.model.*;

public class OnePlayerHeuristic implements IHeuristic {

	private OnePlayerSearch strategy;
	private GoalDistanceHeuristic helper;
	
	public OnePlayerHeuristic(OnePlayerSearch strategy) {
		this.strategy = strategy;
		this.helper = new GoalDistanceHeuristic(strategy.getGame().getSourceGDL());
	}
	
	@Override
	public int calculateHeuristic(IGameNode node) {
		return calculateHeuristic(node, "");
	}
	
	public int calculateHeuristic(IGameNode node, String stepcounter) {
		try {
			strategy.getGame().regenerateNode(node);
		} catch(InterruptedException e) {}
		
		// try to merge with existing simulation values
		ValuesEntry val = strategy.getValue(node.getState());
		if(val == null) // don't trust our heuristics too much!
			return Math.round(new Float(0.6*helper.calculateHeuristic(node, stepcounter)));
		else
			return Math.round(new Float(0.6*helper.calculateHeuristic(node, stepcounter) + 0.4*val.getGoalArray()[0]));
	}

}
