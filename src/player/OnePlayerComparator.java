package player;

import java.util.Comparator;
import java.util.HashMap;

import org.eclipse.palamedes.gdl.core.model.IGameNode;
import org.eclipse.palamedes.gdl.core.model.IGameState;

public class OnePlayerComparator implements Comparator<IGameNode> {

    private HashMap<String, ValuesEntry> values;
    private boolean reverse = false;
    private OnePlayerSearch strategy;

    public OnePlayerComparator(HashMap<String, ValuesEntry> values, boolean reverse, OnePlayerSearch strategy) {
        this.values = values;
        this.reverse = reverse;
        this.strategy = strategy;
    }

    @Override
    public int compare(IGameNode arg0, IGameNode arg1) {
        try {
            strategy.getGame().regenerateNode(arg0);
            strategy.getGame().regenerateNode(arg1);
        } catch (InterruptedException e) {
        }

        ValuesEntry val1 = this.values.get(strategy.makeKeyString(arg0.getState()));
        ValuesEntry val2 = this.values.get(strategy.makeKeyString(arg1.getState()));
        if (val1 != null && val2 != null) {

            if ((val1.getGoalArray()[0]) < (val2.getGoalArray()[0])) {
                // val1 brings us worse score
                //System.out.println("val1 is worse than val2: "+((int[]) val1.keySet().toArray()[0])[0]+", "+((int[]) val2.keySet().toArray()[0])[0]);
                return (reverse) ? -1 : 1;
            }
            if ((val1.getGoalArray()[0]) > (val2.getGoalArray()[0])) {
                // val1 brings us better score
                //System.out.println("val1 is better than val2: "+((int[]) val1.keySet().toArray()[0])[0]+", "+((int[]) val2.keySet().toArray()[0])[0]);
                return (reverse) ? 1 : -1;
            }
            if ((val1.getGoalArray()[0]) == (val2.getGoalArray()[0])) {
                // the values bring us the same score -> equal
                // take the one with more occurences
                //System.out.println("val1 is equal to val2: "+((int[]) val1.keySet().toArray()[0])[0]+", "+((int[]) val2.keySet().toArray()[0])[0]);
                if (val1.getOccurences() > val2.getOccurences()) {
                    return (reverse) ? -1 : 1;
                }
                if (val1.getOccurences() < val2.getOccurences()) {
                    return (reverse) ? 1 : -1;
                }

                return 0;
            }
        }
        if (val1 != null && val2 == null) {
            return (reverse) ? 1 : -1;
        }
        if (val1 == null && val2 != null) {
            return (reverse) ? -1 : 1;
        }
        return 0;
    }
}
