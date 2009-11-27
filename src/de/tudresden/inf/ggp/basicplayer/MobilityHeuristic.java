package de.tudresden.inf.ggp.basicplayer;

import org.eclipse.palamedes.gdl.core.model.IGameNode;

public class MobilityHeuristic implements IHeuristic {
	
	private int roleIndex;
	
	@Override
	public double calculateHeuristic(IGameNode node) {

		int myMoves = node.getState().getLegalMoves()[roleIndex].length;
		int movesOfTheOthers = 0;
		for(int i=0; i<node.getState().getLegalMoves().length; i++) {
			if(i != roleIndex) movesOfTheOthers += node.getState().getLegalMoves()[i].length;
		}
		// substract count of own moves from count of opponents' moves. the value is the lowest when we dominate the most.
		return movesOfTheOthers-myMoves;
	}

}
