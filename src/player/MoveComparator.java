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
	private HashMap<IGameState, HashMap<int[], Integer>> values;
	private TwoPlayerStrategy strat;
	
	public MoveComparator(TwoPlayerStrategy strat){
		this.strat = strat;
		this.values = strat.getValues();
	}

	@Override
	public int compare(IGameNode arg0, IGameNode arg1) {
		// arg0 is better than arg1, if the value in the hash is either better, or more reliable.
		HashMap<int[], Integer> hash1 = values.get(arg0.getState());
		HashMap<int[], Integer> hash2 = values.get(arg1.getState());
		
		int[] val1 = (hash1 != null) ? (int[]) hash1.keySet().toArray()[0] : new int[]{49, 49};
		int[] val2 = (hash2 != null) ? (int[]) hash2.keySet().toArray()[0] : new int[]{49, 49};
		
		Integer rel1 = (hash1 != null) ? (Integer) hash1.values().toArray()[0] : 1;
		Integer rel2 = (hash2 != null) ? (Integer) hash2.values().toArray()[0] : 1;
		
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
	}

	
}
