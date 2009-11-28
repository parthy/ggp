/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.tudresden.inf.ggp.basicplayer;

import org.eclipse.palamedes.gdl.core.model.IGameNode;
import org.eclipse.palamedes.gdl.core.model.utils.GameNode;

/**
 *
 * @author konrad
 */
public class NoveltyHeuristic implements IHeuristic {

	//function not tested yet
	public double calculateHeuristic(IGameNode node) {
		int[] diffs = GameNode.getDiffCount(node.getState(), node.getParent().getState());
		int sum = 0;
		for(int i=0; i<diffs.length; i++){
			sum+= diffs[i];
		}
		//we return the lowest value when it differs the most
		return sum*-1;
	}


}
