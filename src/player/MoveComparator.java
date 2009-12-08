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
import org.eclipse.palamedes.gdl.core.model.IMove;
import org.eclipse.palamedes.gdl.core.simulation.Match;

/**
 *
 * @author konrad
 */
public class MoveComparator implements Comparator<IGameNode>{
	private HashMap<IGameState, int[]> values;
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
					// the simulation values don't distinguish. try mobility approach
					int mobility = strat.getMobilityVariant();
					switch(mobility) {
						case 0: return 0;
						default: 
							try {
								return compareMobility(mobility, match.getGame().getLegalMoves(node1), match.getGame().getLegalMoves(node2));
							} catch (InterruptedException e) {}
					}
				}
			// the following covers the cases where values are not known. something is better than not known here.
			} else if(this.simul.get(node1.getState()) == null && this.simul.get(node2.getState()) != null) {
				HashMap<int[], Integer> val2 = this.simul.get(node2.getState());
				if( ((int[]) val2.keySet().toArray()[0])[strat.getPlayerNumber()] == 0) // if the known value is zero, this is bad
					return -1;
				return 1;
			} else if(this.simul.get(node1.getState()) != null && this.simul.get(node2.getState()) == null ) {
				HashMap<int[], Integer> val1 = this.simul.get(node1.getState());
				if( ((int[]) val1.keySet().toArray()[0])[strat.getPlayerNumber()] == 0) // if the known value is zero, this is bad
					return 1;
				return -1;
			} else {
				// the simulation values don't distinguish. try mobility approach
				int mobility = strat.getMobilityVariant();
				switch(mobility) {
					case 0: return 0;
					default: 
						try {
							return compareMobility(mobility, match.getGame().getLegalMoves(node1), match.getGame().getLegalMoves(node2));
						} catch (InterruptedException e) {}
				}
			}
		}
		
		//the pseudo-49 approach
		int tmpValue1 = 0;
		if(!values.containsKey(node1.getState())){
			//we dont have a value
			tmpValue1 = 49;
		} else {
			tmpValue1 = values.get(node1.getState())[strat.getPlayerNumber()];
		}
		int tmpValue2 = 0;
		if(!values.containsKey(node2.getState())){
			tmpValue2 = 49;
		} else {
			tmpValue2 = values.get(node2.getState())[strat.getPlayerNumber()];
		}
		//now we really compare
		if(tmpValue1 < tmpValue2){ // higher value means better node
			return 1;
		} else {
			if(tmpValue1 == tmpValue2){
				//they have the same value
				// first try to distinguish using the distance field
				int dist1 = values.get(node1.getState())[2];
				int dist2 = values.get(node2.getState())[2];
				if(dist1 != -1 && dist2 != -1) {
					if(dist1 < dist2) return -1;
					if(dist1 == dist2) return 0;
					if(dist1 > dist2) return 1;
				}
				//here the heuristic decides
				// the values don't distinguish. try mobility approach
				int mobility = strat.getMobilityVariant();
				switch(mobility) {
					case 0: return 0;
					default: 
						try {
							return compareMobility(mobility, match.getGame().getLegalMoves(node1), match.getGame().getLegalMoves(node2));
						} catch (InterruptedException e) {return 0;}
				}
			} else {
				return -1;
			}
		}
	}
	
	
	private int compareMobility(int mobilityVariant, IMove[][] allMoves1, IMove[][] allMoves2) {
		switch(mobilityVariant) {
		case TwoPlayerStrategy.MOB_DISABLE_OPP:
			if(allMoves1[(strat.getPlayerNumber()+1)%2].length < allMoves2[(strat.getPlayerNumber()+1)%2].length)
				return -1;
			if(allMoves1[(strat.getPlayerNumber()+1)%2].length > allMoves2[(strat.getPlayerNumber()+1)%2].length)
				return 1;
			return 0;
			
		case TwoPlayerStrategy.MOB_DISABLE_US:
			if(allMoves1[(strat.getPlayerNumber()+0)%2].length < allMoves2[(strat.getPlayerNumber()+0)%2].length)
				return -1;
			if(allMoves1[(strat.getPlayerNumber()+0)%2].length > allMoves2[(strat.getPlayerNumber()+0)%2].length)
				return 1;
			return 0;
			
		case TwoPlayerStrategy.MOB_ENABLE_OPP:
			if(allMoves1[(strat.getPlayerNumber()+1)%2].length > allMoves2[(strat.getPlayerNumber()+1)%2].length)
				return -1;
			if(allMoves1[(strat.getPlayerNumber()+1)%2].length < allMoves2[(strat.getPlayerNumber()+1)%2].length)
				return 1;
			return 0;
			
		case TwoPlayerStrategy.MOB_ENABLE_US:
			if(allMoves1[(strat.getPlayerNumber()+0)%2].length > allMoves2[(strat.getPlayerNumber()+0)%2].length)
				return -1;
			if(allMoves1[(strat.getPlayerNumber()+0)%2].length < allMoves2[(strat.getPlayerNumber()+0)%2].length)
				return 1;
			return 0;
			
		case TwoPlayerStrategy.MOB_MOBILITY:
			if(allMoves1[(strat.getPlayerNumber()+0)%2].length > allMoves2[(strat.getPlayerNumber()+0)%2].length)
				return -1;
			if(allMoves1[(strat.getPlayerNumber()+0)%2].length < allMoves2[(strat.getPlayerNumber()+0)%2].length)
				return 1;
			if(allMoves1[(strat.getPlayerNumber()+1)%2].length < allMoves2[(strat.getPlayerNumber()+1)%2].length)
				return -1;
			if(allMoves1[(strat.getPlayerNumber()+1)%2].length > allMoves2[(strat.getPlayerNumber()+1)%2].length)
				return 1;
			return 0;
			
		case TwoPlayerStrategy.MOB_INVERSE:
			if(allMoves1[(strat.getPlayerNumber()+0)%2].length < allMoves2[(strat.getPlayerNumber()+0)%2].length)
				return -1;
			if(allMoves1[(strat.getPlayerNumber()+0)%2].length > allMoves2[(strat.getPlayerNumber()+0)%2].length)
				return 1;
			if(allMoves1[(strat.getPlayerNumber()+1)%2].length > allMoves2[(strat.getPlayerNumber()+1)%2].length)
				return -1;
			if(allMoves1[(strat.getPlayerNumber()+1)%2].length < allMoves2[(strat.getPlayerNumber()+1)%2].length)
				return 1;
			return 0;
		
		case TwoPlayerStrategy.MOB_MOBILITY_TEAM:
			if(allMoves1[(strat.getPlayerNumber()+0)%2].length > allMoves2[(strat.getPlayerNumber()+0)%2].length)
				return -1;
			if(allMoves1[(strat.getPlayerNumber()+0)%2].length < allMoves2[(strat.getPlayerNumber()+0)%2].length)
				return 1;
			if(allMoves1[(strat.getPlayerNumber()+1)%2].length > allMoves2[(strat.getPlayerNumber()+1)%2].length)
				return -1;
			if(allMoves1[(strat.getPlayerNumber()+1)%2].length < allMoves2[(strat.getPlayerNumber()+1)%2].length)
				return 1;
			return 0;
			
		case TwoPlayerStrategy.MOB_INV_MOB_TEAM:
			if(allMoves1[(strat.getPlayerNumber()+0)%2].length < allMoves2[(strat.getPlayerNumber()+0)%2].length)
				return -1;
			if(allMoves1[(strat.getPlayerNumber()+0)%2].length > allMoves2[(strat.getPlayerNumber()+0)%2].length)
				return 1;
			if(allMoves1[(strat.getPlayerNumber()+1)%2].length < allMoves2[(strat.getPlayerNumber()+1)%2].length)
				return -1;
			if(allMoves1[(strat.getPlayerNumber()+1)%2].length > allMoves2[(strat.getPlayerNumber()+1)%2].length)
				return 1;
			return 0;
			
		default: return 0;
		}
	}
}
