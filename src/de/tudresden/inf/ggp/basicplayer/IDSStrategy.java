package de.tudresden.inf.ggp.basicplayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.eclipse.palamedes.gdl.core.model.IFluent;
import org.eclipse.palamedes.gdl.core.model.IGameNode;
import org.eclipse.palamedes.gdl.core.model.IGameState;
import org.eclipse.palamedes.gdl.core.model.IMove;
import org.eclipse.palamedes.gdl.core.simulation.Match;
import org.eclipse.palamedes.gdl.core.simulation.strategies.AbstractStrategy;


public class IDSStrategy extends AbstractStrategy {
	private List<IGameNode> queue = new ArrayList<IGameNode>();
	private List<IGameNode> currentWay = new ArrayList<IGameNode>();
	private Map<Integer, Integer> hash = new HashMap<Integer, Integer>();
	private Map<Integer, List<IFluent>> visitedStates_playing = new HashMap<Integer, List<IFluent>>();
	private boolean foundSolution = false;
	private int currentDepthLimit;
	private int nodesVisited;
	private long endTime;
	IGameNode solution;
	IGameNode node;
	IGameNode sol;
	int expandedNodes;
	private long start;
	
	@Override
	public void initMatch(Match initMatch) {
		super.initMatch(initMatch);
		// initiate solution search
		game = initMatch.getGame();
		queue.add(game.getTree().getRootNode());
		endTime = System.currentTimeMillis() + initMatch.getStartTime()*1000 - 5000L;
		IDS(1);
		hash.clear();
		if(foundSolution) fillCurrentWay();
	}

    @Override
    public IMove getMove(IGameNode currentNode) {
    	/*
    	 * return the head element of currentWay and remove it from the list
    	 */
    	System.out.println(currentNode);
    	long endOfPlayTime = System.currentTimeMillis() + match.getPlayTime()*1000 - 500L;
    	if(!currentWay.isEmpty()) return currentWay.remove(0).getMoves()[0];
		else {// if no way is there, just do something.
			visitedStates_playing.put(currentNode.getState().getFluents().hashCode(), null); // remember we were here
			try {
				// try to return a move that will not lead us back somewhere we were already. if not possible, take random shot.
				IMove[] movesToReturn;
				IMove moveToReturn;
				do {
					movesToReturn = game.getRandomMove(currentNode);
					moveToReturn = movesToReturn[0];
				} while(System.currentTimeMillis() < endOfPlayTime && visitedStates_playing.containsKey(game.getNextNode(currentNode, movesToReturn).getState().getFluents().hashCode()));
				 
				return moveToReturn;
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return currentNode.getMoves()[0]; // should not happen
		}
    }
    
    void IDS(int depthLimit) {
    	currentDepthLimit = depthLimit;
		expandedNodes = 0;
		start = System.currentTimeMillis();

    	
    	while(!foundSolution) {
			System.err.println("currentDepth: "+currentDepthLimit);
			System.err.println("current size of hash: "+hash.size());
			System.err.println("expanded "+expandedNodes+" Nodes in "+(System.currentTimeMillis()-start)+" seconds.");
    		while(!queue.isEmpty() && System.currentTimeMillis() < endTime) {
    			node = queue.remove(0);
    			
    			// try to regenerate state information of the node
    			
				try {
					node = game.getNextNode(node.getParent(), node.getMoves());
				} catch (Exception e2) {}
    			// track our way of states so we don't visit states multiple times
    			hash.put(node.getState().hashCode(), currentDepthLimit-node.getDepth());
    			
    			if(node.getState().getGoalValue(0) == 100) {
    				foundSolution = true;
    				this.solution = node;
    			}

				if(node.getState().isTerminal()){
					break;
				}
    			
    			if(node.getDepth() < this.currentDepthLimit) {
					expandedNodes++;
    				// add successor nodes to queue
    				try {
    					List<IMove[]> allMoves = game.getCombinedMoves(node);
    					IMove[] combMoves;
    					for(int i=0; i<allMoves.size(); ++i) {
    						try {
    							combMoves = allMoves.get(i);
								IGameNode newNode = game.getNextNode(node, combMoves);
    							if((!hash.containsKey(newNode.getState().hashCode()))
									|| ((currentDepthLimit-newNode.getDepth()) > hash.get(newNode.getState().hashCode()))){
    								queue.add(0, newNode);
    							}
    						} catch (InterruptedException e) {}
    					}
    				} catch (InterruptedException e1) {}
    				
    			}
    		}
    		if(foundSolution || System.currentTimeMillis() >= endTime) return;
    		queue.clear();
    		queue.add(game.getTree().getRootNode());
    		currentDepthLimit++;
    	}
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
