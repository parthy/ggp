package player;

import java.util.Comparator;
import java.util.HashMap;

import org.eclipse.palamedes.gdl.core.model.IGameNode;
import org.eclipse.palamedes.gdl.core.model.IGameState;

public class MultiPlayerComparator implements Comparator<IGameNode> {
	private HashMap<IGameState, int[][]> values;
	private int playerNumber;
	
	public MultiPlayerComparator(HashMap<IGameState, int[][]> values, int playerNumber) {
		this.values = values;
		this.playerNumber = playerNumber;
	}
	
	@Override
	public int compare(IGameNode arg0, IGameNode arg1) {
		if(values.get(arg0.getState()) == null && values.get(arg1.getState()) == null)
			return 0;
		if(values.get(arg0.getState()) != null && values.get(arg1.getState()) == null)
			return (values.get(arg0.getState())[0][playerNumber] != 0) ? -1 : 1;
		if(values.get(arg0.getState()) == null && values.get(arg1.getState()) != null)
			return (values.get(arg1.getState())[0][playerNumber] != 0) ? 1 : -1;
		
		if(values.get(arg0.getState())[0][playerNumber] > values.get(arg1.getState())[0][playerNumber])
			return -1;
		if(values.get(arg0.getState())[0][playerNumber] < values.get(arg1.getState())[0][playerNumber])
			return 1;
		if(values.get(arg0.getState())[0][playerNumber] == values.get(arg1.getState())[0][playerNumber]) {
			if(values.get(arg0.getState())[1][0] > values.get(arg1.getState())[1][0]) return -1;
			if(values.get(arg0.getState())[1][0] < values.get(arg1.getState())[1][0]) return 1;
			return 0;
		}
		
		return 0;
	}

}
