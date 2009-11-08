package de.tudresden.inf.ggp.basicplayer;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

import org.eclipse.palamedes.gdl.core.model.IGameNode;
import org.eclipse.palamedes.gdl.core.model.IGameState;
import org.eclipse.palamedes.gdl.core.model.IMove;
import org.eclipse.palamedes.gdl.core.simulation.Match;
import org.eclipse.palamedes.gdl.core.simulation.strategies.AbstractStrategy;


public class IDSStrategy extends AbstractStrategy {
	private List<IGameNode> queue;
	private List<IGameNode> currentWay;
	private Set<IGameState> visitedStates;
	private boolean foundSolution = false;
	private int currentDepthLimit;
	IGameNode solution;
	IGameNode node;
	IGameNode sol;
	
	@Override
	public void initMatch(Match initMatch) {
		super.initMatch(initMatch);
		// initiate solution search
		game = initMatch.getGame();
		currentWay = new ArrayList<IGameNode>();
		queue = new ArrayList<IGameNode>();
		visitedStates = new HashSet<IGameState>();
		queue.add(game.getTree().getRootNode());
		System.out.println(new Date());
		IDS(1);
		System.out.println(new Date());
		System.out.println("Solution found:"+solution);
		fillCurrentWay();
		System.out.println(currentWay);
	}

    @Override
    public IMove getMove(IGameNode currentNode) {
    	/*
    	 * return the head element of currentWay and remove it from the list
    	 */
    	if(!currentWay.isEmpty()) return currentWay.remove(0).getMoves()[0];
		else // if no way is there, just do something.
			try {
				return game.getRandomMove(currentNode)[0]; 
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return currentNode.getMoves()[0]; // should not happen
    }
    
    void IDS(int depthLimit) {
    	currentDepthLimit = depthLimit;
    	
    	while(!foundSolution) {
    		while(!queue.isEmpty()) {
    			node = queue.remove(0);
    			
    			// try to regenerate state information of the node
    			if(node.getState() == null) {
    				try {
    					game.regenerateNode(node);
    				} catch (InterruptedException e2) {}
    			}
    			visitedStates.add(node.getState());
    			//System.out.println(node);
    			
    			if(node.getState().getGoalValue(0) == 100) {
    				foundSolution = true;
    				this.solution = node;
    			}
    			
    			if(node.getDepth() < this.currentDepthLimit) {
    				// add successor nodes to queue
    				try {
    					for(IMove[] combMoves : game.getCombinedMoves(node)) {
    						try {
    							if(!visitedStates.contains(game.getNextNode(node, combMoves).getState()))
    								queue.add(0, game.getNextNode(node, combMoves));
    						} catch (InterruptedException e) {}
    					}
    				} catch (InterruptedException e1) {}
    				
    			}
    		}
    		if(foundSolution) return;
    		queue.clear();
    		visitedStates.clear();
    		queue.add(game.getTree().getRootNode());
    		currentDepthLimit++;
    	}
    	return;
    }

	void DLS() {
		
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
