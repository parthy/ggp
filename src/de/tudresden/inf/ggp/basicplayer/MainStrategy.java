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

	@Override
	public IMove getMove(IGameNode arg0) {
		return this.myStrategy.getMove(arg0);
	}

	public void initMatch(Match initMatch) {
		if(initMatch.getGame().getRoleCount() == 1){
			this.myStrategy = new IDSStrategy();
		} else {
			if(initMatch.getGame().getRoleCount() == 2){
				this.myStrategy = new TwoPlayerStrategy();
			} else {
				throw new UnsupportedOperationException("i dont know how to play this game");
			}
		}
		myStrategy.initMatch(initMatch);
	}

	public double getHeuristicValue(IGameNode arg0) {
		return this.myStrategy.getHeuristicValue(arg0);
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