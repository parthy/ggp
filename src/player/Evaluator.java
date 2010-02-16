package player;

import java.util.HashMap;

import org.eclipse.palamedes.gdl.core.model.IGameNode;
import org.eclipse.palamedes.gdl.core.model.IGameState;

public class Evaluator {
	/**
	 * VAR simulationValues The Hash we look for values in.
	 */
	private HashMap<IGameState, Integer> simulationValues;
	
	/**
	 * Constructor. Takes a reference to the values hash as parameter.
	 * 
	 * @param values The hash of values to use for the evaluation.
	 */
	public Evaluator(HashMap<IGameState, Integer> values) {
		this.simulationValues = values;
	}
	
	/**
	 * The evaluate function. Currently taking just simulation values.
	 * 
	 * @param node The game node to evaluate.
	 * @return The found value, or -1 if none present.
	 */
	public Integer evaluateNode(IGameNode node) {
		Integer val = simulationValues.get(node.getState());
		
		if(val == null) 
			return -1;
		else 
			return val;
	}
}
