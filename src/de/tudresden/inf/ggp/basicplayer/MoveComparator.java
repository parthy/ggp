/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.tudresden.inf.ggp.basicplayer;

import java.util.Comparator;
import java.util.HashMap;
import org.eclipse.palamedes.gdl.core.model.IGameNode;
import org.eclipse.palamedes.gdl.core.simulation.Match;

/**
 *
 * @author konrad
 */
public class MoveComparator implements Comparator<IGameNode>{
	private HashMap<Integer, Integer> values;
	private TwoPlayerStrategy strat;
	private Match match;
	
	public MoveComparator(TwoPlayerStrategy strat, Match match){
		this.strat = strat;
		this.match = match;
		this.values = strat.getValues();
	}

	public int compare(IGameNode node1, IGameNode node2) {
		try {
			match.getGame().regenerateNode(node1);
			match.getGame().regenerateNode(node2);
		} catch(InterruptedException e) {}
		
		//the pseudo-49 approach
		int tmpValue1 = 0;
		if(!values.containsKey(node1.getState().hashCode())){
			//we dont have a value
			tmpValue1 = 49;
		} else {
			tmpValue1 = values.get(node1.getState().hashCode());
		}
		int tmpValue2 = 0;
		if(!values.containsKey(node2.getState().hashCode())){
			tmpValue2 = 49;
		} else {
			tmpValue2 = values.get(node2.getState().hashCode());
		}
		//now we really compare
		if(tmpValue1 < tmpValue2){
			return -1;
		} else {
			if(tmpValue1 == tmpValue2){
				//they have the same value
				//here the heuristic decides
				if(MyPlayer.strategy.getHeuristicValue(node1) < MyPlayer.strategy.getHeuristicValue(node2)){
					return -1;
				} else {
					if(MyPlayer.strategy.getHeuristicValue(node1) == MyPlayer.strategy.getHeuristicValue(node2)){
						return 0;
					} else {
						return 1;
					}
				}
			} else {
				return 1;
			}
		}
	}
	
}
