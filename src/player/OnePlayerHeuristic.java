package player;

import java.util.HashMap;

import org.eclipse.palamedes.gdl.core.model.*;

public class OnePlayerHeuristic implements IHeuristic {

	private OnePlayerSearch strategy;
	private IHeuristic helper;
	
	public OnePlayerHeuristic(OnePlayerSearch strategy) {
		this.strategy = strategy;
		this.helper = new NoveltyHeuristic();
	}
	
	@Override
	public int calculateHeuristic(IGameNode node) {
		try {
			strategy.getGame().regenerateNode(node);
		} catch(InterruptedException e) {}

		HashMap<Integer, Integer> existingVal = strategy.getValue(node.getState());
		if(existingVal == null) return helper.calculateHeuristic(node);
		return (Integer) existingVal.keySet().toArray()[0];
	}

}
