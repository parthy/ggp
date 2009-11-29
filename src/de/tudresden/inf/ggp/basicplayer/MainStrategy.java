/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.tudresden.inf.ggp.basicplayer;

import org.eclipse.palamedes.gdl.core.model.IGameNode;
import org.eclipse.palamedes.gdl.core.model.IMove;
import org.eclipse.palamedes.gdl.core.simulation.IStrategy;
import org.eclipse.palamedes.gdl.core.simulation.Match;

/**
 *
 * @author konrad
 */
public class MainStrategy implements IStrategy {

	private IStrategy myStrategy;
	private IHeuristic heuristic;

	public void initMatch(Match initMatch) {
		if(initMatch.getGame().getRoleCount() == 1){
			this.myStrategy = new OnePlayerSearch();
		} else {
			if(initMatch.getGame().getRoleCount() == 2){
				this.myStrategy = new TwoPlayerStrategy();
				this.heuristic = new MobilityHeuristic(initMatch);
			} else {
				throw new UnsupportedOperationException("i dont know how to play this game");
			}
		}
		myStrategy.initMatch(initMatch);
	}

	public double getHeuristicValue(IGameNode arg0) {
		if(heuristic == null){
			return 0;
		} else {
			return heuristic.calculateHeuristic(arg0);
		}
	}

	public void setHeuristic(IHeuristic heuristic){
		this.heuristic = heuristic;
	}
	
	@Override
	public IMove getMove(IGameNode arg0) {
		return this.myStrategy.getMove(arg0);
	}

	public void dispose() {
		this.myStrategy.dispose();
		myStrategy = null;
	}

	public double getReliabilityValue() {
		return this.getReliabilityValue();
	}

	public double getExpectedGoalValue() {
		return this.getExpectedGoalValue();
	}

}
