package player;
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

import org.eclipse.palamedes.gdl.core.model.IGameNode;
import org.eclipse.palamedes.gdl.core.model.utils.GameNode;

/**
 *
 * @author konrad
 */
public class NoveltyHeuristic implements IHeuristic {

	//function not tested yet
	public int calculateHeuristic(IGameNode node) {
		if(node.getParent() == null || node.getState() == null || node.getParent().getState() == null) return 0;
		int[] diffs = GameNode.getDiffCount(node.getState(), node.getParent().getState());
		
		// diffs is an array of {count aOnly, count bOnly, count overlap}
		// we sum up the differences and divide them by aOnly+bOnly+overlap/2.
		// This gives us a rate < 1 describing the ratio of different fluents compared to all fluents.
		double ratio = (double) (diffs[0]+diffs[1]) / (double) (diffs[0]+diffs[1]+(double) (diffs[2]/2));
		//we return the lowest value when it differs the most
		//System.out.println("Got a novelty ratio of "+ratio);
		return (int) ratio*100;
	}


}
