package player;
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

//package de.tudresden.inf.ggp.basicplayer;

import java.util.Comparator;
import java.util.HashMap;

import org.eclipse.palamedes.gdl.core.model.IGameNode;
import org.eclipse.palamedes.gdl.core.model.IGameState;

/**
 *
 * @author konrad
 */
public class MoveComparator implements Comparator<IGameNode>{
	private TwoPlayerStrategy strat;
	
	public MoveComparator(TwoPlayerStrategy strat){
		this.strat = strat;
	}

	@Override
	public int compare(IGameNode arg0, IGameNode arg1) {
		// arg0 is better than arg1, if the value in the hash is either better, or more reliable.
/*		ValuesEntry entry1 = values.get(arg0.getState());
		ValuesEntry entry2 = values.get(arg1.getState());
		
		int[] val1 = (entry1 != null) ? (int[]) entry1.getGoalArray() : new int[]{49, 49};
		int[] val2 = (entry2 != null) ? (int[]) entry2.getGoalArray() : new int[]{49, 49};
		
		Integer rel1 = (entry1 != null) ? entry1.getOccurences() : 1;
		Integer rel2 = (entry2 != null) ? entry2.getOccurences() : 1;
		
		int player = strat.getPlayerNumber();
		
		if(val1[player] == val2[player]) {
			if(rel1 > rel2) 
				return -1;
			else if(rel1 < rel2)
				return 1;
			else
				return 0;
		} else if(val1[player] > val2[player]) {
			return -1;
		} else {
			return 1;
		}
*/
	Integer val0 = strat.evaluateNode(arg0);
	Integer val1 = strat.evaluateNode(arg1);
	
	// if it cant be evaluated set it to 49
	val0 = (val0 == null || val0 == -1) ? 49 : val0;
	val1 = (val1 == null || val1 == -1) ? 49 : val1;
	
	if(val0 == val1) {
		ValuesEntry v0 = strat.getValues().get(arg0.getState());
		ValuesEntry v1 = strat.getValues().get(arg1.getState());
		
		if(v0 == null || v1 == null) {
			return 0;
		} else {
			if(v0.getGoalArray()[strat.getPlayerNumber()] > v1.getGoalArray()[strat.getPlayerNumber()])
				return -1;
			if(v0.getGoalArray()[strat.getPlayerNumber()] < v1.getGoalArray()[strat.getPlayerNumber()])
				return 1;
			else
				return 0;
		}
	}

	return -val0.compareTo(val1);
	}

	
}
