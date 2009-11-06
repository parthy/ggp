package de.tudresden.inf.ggp.basicplayer;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.palamedes.gdl.core.model.IGameNode;
import org.eclipse.palamedes.gdl.core.model.IMove;
import org.eclipse.palamedes.gdl.core.simulation.Match;
import org.eclipse.palamedes.gdl.core.simulation.strategies.AbstractStrategy;


public class IDSStrategy extends AbstractStrategy {
	private List<IGameNode> queue;
	private List<IGameNode> currentWay;
	private boolean foundSolution = false;
	IGameNode solution;
	private long endOfStartTime;
	
	@Override
	public void initMatch(Match initMatch) {
		super.initMatch(initMatch);
		//generate the game tree
		game = initMatch.getGame();
		currentWay = new ArrayList<IGameNode>();
		queue = new ArrayList<IGameNode>();
		queue.add(game.getTree().getRootNode());
		solution = IDS(1);
		System.out.println(solution);
		fillCurrentWay();
		System.out.println(currentWay);
	}

    @Override
    public IMove getMove(IGameNode currentNode) {
    	/*
    	 * return the head element of currentWay and remove it from the list
    	 */
    	return currentWay.remove(0).getMoves()[0];
    }
    
    IGameNode IDS(int depthLimit) {
    	int currentDepthLimit = depthLimit;
    	IGameNode sol;
    	
    	while(!foundSolution) {
    		sol = DLS(currentDepthLimit); 
    		if(foundSolution) return sol;
    		queue.clear();
    		queue.add(game.getTree().getRootNode());
    		currentDepthLimit++;
    	}
    	return null;
    }

	IGameNode DLS(int depth) {
		if(queue.isEmpty()) return null;
		IGameNode node = queue.remove(0);
		System.out.println(node);
		if(node.getState().isTerminal() && node.getState().getGoalValue(0) == 100) {
			foundSolution = true;
			return node;
		}
		
		if(node.getDepth() < depth) {
			List<IMove[]> legalMoves = new ArrayList<IMove[]>();
			// add successor nodes to queue
			try {
				legalMoves = game.getCombinedMoves(node);
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			for(IMove[] combMoves : legalMoves) {
				try {
					queue.add(0, game.getNextNode(node, combMoves));
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			// and start over again
			return DLS(depth);
		} else {
			return DLS(depth); // continue search
		}
	}
    
	void fillCurrentWay() {
		IGameNode cur = solution;
		while(cur.getParent() != null) {
			System.out.println(cur);
			currentWay.add(0, cur);
			cur = cur.getParent();
		}
	}
    
}
