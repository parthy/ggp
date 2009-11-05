package de.tudresden.inf.ggp.basicplayer;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.palamedes.gdl.core.model.IGameNode;
import org.eclipse.palamedes.gdl.core.model.IMove;
import org.eclipse.palamedes.gdl.core.simulation.Match;
import org.eclipse.palamedes.gdl.core.simulation.strategies.AbstractStrategy;


public class IDSStrategy extends AbstractStrategy {
	private List<IGameNode> currentWay;
	private int depthLimit;


	@Override
	public void initMatch(Match initMatch) {
		super.initMatch(initMatch);
		//generate the game tree
		game = initMatch.getGame();
		currentWay = new ArrayList<IGameNode>();
		depthLimit = 1;

	}

    @Override
    public IMove getMove(IGameNode currentNode) {

    	/** XXX: All strategy relevant code goes in here. */
        try {
            List<IMove[]> moves = match.getGame().getCombinedMoves(currentNode);
			System.err.println(game.getTree());
            return moves.get( random.nextInt( moves.size() ) )[playerNumber];

        }
        catch (InterruptedException e) {
            System.out.println("getMove() stopped by time.");
        }

        return null;
    }
}
