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
import org.eclipse.palamedes.gdl.core.simulation.Match;

/**
 *
 * @author konrad
 */
public class MoveComparator implements Comparator<IGameNode>{
	private HashMap<IGameState, Integer> values;
	private HashMap<IGameState, HashMap<int[], Integer>> simul;
	private TwoPlayerStrategy strat;
	private Match match;
	private boolean useSimul = false;
	
	public MoveComparator(TwoPlayerStrategy strat, Match match){
		this.strat = strat;
		this.match = match;
		this.values = strat.getValues();
		this.simul = strat.getSimulationValues();
	}
	
	public MoveComparator(TwoPlayerStrategy strat, Match match, boolean simul){
		this.strat = strat;
		this.match = match;
		this.values = strat.getValues();
		this.simul = strat.getSimulationValues();
		this.useSimul = simul;
	}

	public int compare(IGameNode node1, IGameNode node2) {
		try {
			if(node1.getState() == null)
				match.getGame().regenerateNode(node1);
			if(node2.getState() == null)
				match.getGame().regenerateNode(node2);
		} catch(InterruptedException e) {}
		
		// shall we use the simulation experience? (only if no real values are found)
		if(this.useSimul && !values.containsKey(node1.getState()) && !values.containsKey(node2.getState())) { 
			if(this.simul.get(node1.getState()) != null && this.simul.get(node2.getState()) != null) {
				HashMap<int[], Integer> val1 = this.simul.get(node1.getState());
				HashMap<int[], Integer> val2 = this.simul.get(node2.getState());
				if(((int[]) val1.keySet().toArray()[0])[strat.getPlayerNumber()] < ((int[]) val2.keySet().toArray()[0])[strat.getPlayerNumber()]) {
					// val1 brings us worse score
					//System.out.println("val1 is worse than val2: "+((int[]) val1.keySet().toArray()[0])[strat.getPlayerNumber()]+", "+((int[]) val2.keySet().toArray()[0])[strat.getPlayerNumber()]);
					return 1;
				}
				if(((int[]) val1.keySet().toArray()[0])[strat.getPlayerNumber()] > ((int[]) val2.keySet().toArray()[0])[strat.getPlayerNumber()]) {
					// val1 brings us better score
					//System.out.println("val1 is better than val2: "+((int[]) val1.keySet().toArray()[0])[strat.getPlayerNumber()]+", "+((int[]) val2.keySet().toArray()[0])[strat.getPlayerNumber()]);
					return -1;
				}
				if(((int[]) val1.keySet().toArray()[0])[strat.getPlayerNumber()] == ((int[]) val2.keySet().toArray()[0])[strat.getPlayerNumber()]) {
					// the values bring us the same score -> equal
					//System.out.println("val1 is equal to val2: "+((int[]) val1.keySet().toArray()[0])[strat.getPlayerNumber()]+", "+((int[]) val2.keySet().toArray()[0])[strat.getPlayerNumber()]);
					return 0;
				}
			}
		}
		
		//the pseudo-49 approach
		int tmpValue1 = 0;
		if(!values.containsKey(node1.getState())){
			//we dont have a value
			tmpValue1 = 49;
		} else {
			tmpValue1 = values.get(node1.getState());
		}
		int tmpValue2 = 0;
		if(!values.containsKey(node2.getState())){
			tmpValue2 = 49;
		} else {
			tmpValue2 = values.get(node2.getState());
		}
		//now we really compare
		if(tmpValue1 < tmpValue2){ // higher value means better node
			return 1;
		} else {
			if(tmpValue1 == tmpValue2){
				//they have the same value
				//here the heuristic decides
				if(MyPlayer.strategy.getHeuristicValue(node1) < MyPlayer.strategy.getHeuristicValue(node2)){ // lower value means better node
					return -1;
				} else {
					if(MyPlayer.strategy.getHeuristicValue(node1) == MyPlayer.strategy.getHeuristicValue(node2)){
						return 0;
					} else {
						return 1;
					}
				}
			} else {
				return -1;
			}
		}
	}
	
}
