package player;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.eclipse.palamedes.gdl.core.model.IGameNode;
import org.eclipse.palamedes.gdl.core.model.IGameState;
import org.eclipse.palamedes.gdl.core.resolver.prologprover.GameProlog;

public class Evaluator {
	/**
	 * VAR simulationValues The Hash we look for values in.
	 */
	private HashMap<IGameState, ValuesEntry> simulationValues;
	private Set<IGameState> goodStates;
	
	/**
	 * Constructor. Takes references to the values hash and the goodStates set as parameter.
	 * 
	 * @param values The hash of values to use for the evaluation.
	 */
	public Evaluator(HashMap<IGameState, ValuesEntry> values, Set<IGameState> goodStates) {
		this.simulationValues = values;
		this.goodStates = goodStates;
	}
	
	/**
	 * The evaluate function.
	 * 
	 * @param node The game node to evaluate.
	 * @return The found value, or -1 if none present.
	 */
	public Integer evaluateNode(IGameNode node, int playerNumber) {
		ValuesEntry entry = simulationValues.get(node.getState());
		Integer val = (entry != null) ? simulationValues.get(node.getState()).getGoalArray()[playerNumber] : null;
		
		int assumedTruth = -1;
		int boundary = 10;
		int i=0;
		for(IGameState state : goodStates) {
			int truth = RuleOptimizer.calculateTruthDegree(node.getState().toString(), state.toString());
			if(assumedTruth == -1 || truth > assumedTruth)
				assumedTruth = truth;
			
			if(++i >= boundary) 
				break;
		}
		
		if(val == null) 
			return assumedTruth;
		else 
			return (int) 0.8*val+(int) 0.2*assumedTruth;
	}
}
