package player;

import java.util.HashMap;

import org.eclipse.palamedes.gdl.core.model.IGameNode;
import org.eclipse.palamedes.gdl.core.model.IGameState;

public class MultiPlayerHeuristic implements IHeuristic {

	private HashMap<IGameState, int[][]> values;
	private int playerNumber;
	private int players;
	private IHeuristic helper;
	
	public MultiPlayerHeuristic(HashMap<IGameState, int[][]> values, int playerNumber, int players) {
		this.values = values;
		this.playerNumber = playerNumber;
		this.players = players;
		this.helper = new NoveltyHeuristic();
	}
	
	public int calculateHeuristic(IGameNode node) {
		int[][] existingVal = values.get(node.getState());
		if(existingVal == null) return helper.calculateHeuristic(node);
		return (existingVal[0][playerNumber]+helper.calculateHeuristic(node))/2;
	}

	public int[] getHeurArray(IGameNode node) {
		int[] ret = new int[players];
		int[][] existingVal = values.get(node.getState());
		for(int i=0; i<players; i++) {
			ret[i] = (existingVal == null) ? helper.calculateHeuristic(node) : (existingVal[0][playerNumber]+helper.calculateHeuristic(node))/2; 
		}
		
		return ret;
	}
}
