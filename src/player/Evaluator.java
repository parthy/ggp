package player;

import java.util.HashMap;
import java.util.Set;
import org.eclipse.palamedes.gdl.core.model.IGameNode;
import org.eclipse.palamedes.gdl.core.model.IGameState;

public class Evaluator {

	/**
	 * VAR simulationValues The Hash we look for values in.
	 */
	private HashMap<IGameState, ValuesEntry> simulationValues;
	private HashMap<IGameState, int[][]> simulationValuesMultiPlayer;
	private Set<MemorizeState> goodStates;

	private boolean multiplayer = false;

	/**
	 * Constructor. Takes references to the values hash and the goodStates set as parameter.
	 *
	 * @param values The hash of values to use for the evaluation.
	 */
	public Evaluator(HashMap<IGameState, ValuesEntry> values, Set<MemorizeState> goodStates) {
		this.simulationValues = values;
	}
	
	/**
	 * Constructor for multiplayer. Takes references to the values hash and the goodStates set as parameter.
	 *
	 * @param values The hash of values to use for the evaluation.
	 */
	public Evaluator(HashMap<IGameState, int[][]> values, Set<MemorizeState> goodStates, boolean multiplayer) {
		this.simulationValuesMultiPlayer = values;
		this.goodStates = goodStates;
		this.multiplayer = multiplayer;
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

		if (val == null) {
			return 39;
		} else {
			return val;
		}
	}

	public int[][] evaluateNodeMP(IGameNode node, int playerCount) {
		int[][] retVal = new int[2][playerCount];
		
		int[][] entry = simulationValuesMultiPlayer.get(node.getState());
		
		for(int playerNumber=0; playerNumber<playerCount; playerNumber++) {
			Integer val = (entry != null) ? entry[0][playerNumber] : null;
	
			int assumedTruth = -1;
			int boundary = 10;
			int i = 0;
			MemorizeState best = null;
			for (MemorizeState state : goodStates) {
				int truth = RuleOptimizer.calculateTruthDegree(node.getState().toString(), state.getState().toString());
				if (assumedTruth == -1 || truth > assumedTruth) {
					assumedTruth = truth;
					best = state;
				}
	
				if (++i >= boundary) {
					break;
				}
			}
	
			if (assumedTruth >= 90) {
				//System.out.println("\ntruth: "+assumedTruth+"between\n"+node.getState()+"\nand\n"+best.getState());
				retVal[0][playerNumber] = best.getValue();
	
				//		System.out.println("Evaluator val: "+val+ " assumed truth: "+assumedTruth);
	
			}
			if (val == null) {
				if (best != null) {
					retVal[0][playerNumber] = best.getValue();
				} else {
					retVal[0][playerNumber] = 39;
				}
			} else {
				retVal[0][playerNumber] = val;
			}
		}
		
		retVal[1][0] = 0;
		retVal[1][1] = MultiPlayerStrategy.HEUR;
		
		return retVal;
	}
}
