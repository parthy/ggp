package de.tudresden.inf.ggp.basicplayer;

import org.eclipse.palamedes.gdl.core.model.IGameState;

public class MobilityHeuristic implements IHeuristic {
	
	private int roleIndex;
	
	@Override
	public int calculateHeuristic(IGameState state) {
		int myMoves = state.getLegalMoves()[roleIndex].length;
		int movesOfTheOthers = 0;
		for(int i=0; i<state.getLegalMoves().length; i++) {
			if(i != roleIndex) movesOfTheOthers += state.getLegalMoves()[i].length;
		}
		// substract count of own moves from count of opponents' moves. the value is the lowest when we dominate the most.
		return movesOfTheOthers-myMoves;
	}

}
