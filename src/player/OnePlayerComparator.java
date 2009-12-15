package player;

import java.util.Comparator;
import java.util.HashMap;

import org.eclipse.palamedes.gdl.core.model.IGameNode;
import org.eclipse.palamedes.gdl.core.model.IGameState;

public class OnePlayerComparator implements Comparator<IGameNode> {

	private HashMap<IGameState, HashMap<Integer, Integer>> values;
	private boolean reverse = false;
	
	public OnePlayerComparator(HashMap<IGameState, HashMap<Integer, Integer>> values, boolean reverse) {
		this.values = values;
		this.reverse = reverse;
	}
	
	@Override
	public int compare(IGameNode arg0, IGameNode arg1) {
		if(values.get(arg0.getState()) != null && values.get(arg1.getState()) != null) {
			HashMap<Integer, Integer> val1 = this.values.get(arg0.getState());
			HashMap<Integer, Integer> val2 = this.values.get(arg1.getState());
			if(((Integer) val1.keySet().toArray()[0]) < ((Integer) val2.keySet().toArray()[0])) {
				// val1 brings us worse score
				//System.out.println("val1 is worse than val2: "+((int[]) val1.keySet().toArray()[0])[0]+", "+((int[]) val2.keySet().toArray()[0])[0]);
				return (reverse) ? -1 : 1;
			}
			if(((Integer) val1.keySet().toArray()[0]) > ((Integer) val2.keySet().toArray()[0])) {
				// val1 brings us better score
				//System.out.println("val1 is better than val2: "+((int[]) val1.keySet().toArray()[0])[0]+", "+((int[]) val2.keySet().toArray()[0])[0]);
				return (reverse) ? 1 : -1;
			}
			if(((Integer) val1.keySet().toArray()[0]) == ((Integer) val2.keySet().toArray()[0])) {
				// the values bring us the same score -> equal
				//System.out.println("val1 is equal to val2: "+((int[]) val1.keySet().toArray()[0])[0]+", "+((int[]) val2.keySet().toArray()[0])[0]);
				return 0;
			}
		}
		if(values.get(arg0.getState()) != null && values.get(arg1.getState()) == null) return (reverse) ? 1 : -1;
		if(values.get(arg0.getState()) == null && values.get(arg1.getState()) != null) return (reverse) ? -1 : 1;
		return 0;
	}

}
