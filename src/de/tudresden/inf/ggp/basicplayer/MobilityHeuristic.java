package de.tudresden.inf.ggp.basicplayer;

import org.eclipse.palamedes.gdl.core.model.IGameNode;
import org.eclipse.palamedes.gdl.core.model.IGameState;
import org.eclipse.palamedes.gdl.core.model.IMove;
import org.eclipse.palamedes.gdl.core.simulation.Match;

public class MobilityHeuristic implements IHeuristic {
	private int roleIndex;
	private Match match;
	
	public MobilityHeuristic(Match match) {
		this.match = match;
		this.roleIndex = match.getGame().getRoleIndex(match.getRole());
	}

	@Override
	public double calculateHeuristic(IGameNode node) {
		try {
			match.getGame().regenerateNode(node);
			IGameState state = node.getState();
			IMove[][] legals = match.getGame().getLegalMoves(node);
			IMove[] myLegals = legals[roleIndex];
			
			int myMoves = myLegals.length;
			int movesOfTheOthers = 0;
			for(int i=0; i<node.getState().getLegalMoves().length; i++) {
				if(i != roleIndex) movesOfTheOthers += node.getState().getLegalMoves()[i].length;
			}
			
			// substract count of own moves from count of opponents' moves. the value is the lowest when we dominate the most.
			System.out.println("Heuristic value: "+(movesOfTheOthers-myMoves)+", for Move "+node.getMoves()[roleIndex].getMoveTerm());
			return movesOfTheOthers-myMoves;
		} catch (InterruptedException e) {}
		
		return 0;
	}

}
