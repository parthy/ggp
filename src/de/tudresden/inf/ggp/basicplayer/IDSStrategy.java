package de.tudresden.inf.ggp.basicplayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.PriorityQueue;

import org.eclipse.palamedes.gdl.core.model.IFluent;
import org.eclipse.palamedes.gdl.core.model.IGameNode;
import org.eclipse.palamedes.gdl.core.model.IGameState;
import org.eclipse.palamedes.gdl.core.model.IMove;
import org.eclipse.palamedes.gdl.core.simulation.Match;
import org.eclipse.palamedes.gdl.core.simulation.strategies.AbstractStrategy;

public class IDSStrategy extends AbstractStrategy {
	private PriorityQueue<Node> queue;
	private List<Node> currentWay = new ArrayList<Node>();
	private Map<Integer, IGameState> visitedStates = new HashMap<Integer, IGameState>();
	private Map<Integer, List<IFluent>> visitedStates_playing = new HashMap<Integer, List<IFluent>>();
	private Map<Integer, IGameState> unusefulStates = new HashMap<Integer, IGameState>();
	public HashMap<IGameState, Integer> heuristic = new HashMap<IGameState, Integer>();
	private IHeuristic heurCalc;
	private boolean foundSolution = false;
	private int currentDepthLimit;
	private int nodesVisited;
	private long endTime;
	Node solution;
	Node node;
	Node sol;
	
	@Override
	public void initMatch(Match initMatch) {
		super.initMatch(initMatch);
		// initiate solution search
		game = initMatch.getGame();
		
		// set comparator
		queue = new PriorityQueue<Node>(100, new HeuristicComparator(this));
		queue.add(new Node(game.getTree().getRootNode()));
		
		// set heuristic
		heurCalc = new DFSHeuristic(this);
		
		endTime = System.currentTimeMillis() + initMatch.getStartTime()*1000 - 5000L;
		IDS(1);
		visitedStates.clear();
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
    	
    	while(!foundSolution) {
			System.err.println("currentDepth"+currentDepthLimit);
			System.err.println("unusefulStates:"+unusefulStates.size());
			System.err.println("Now: "+System.currentTimeMillis()+", endTime: "+endTime);
    		while(!queue.isEmpty() && System.currentTimeMillis() < endTime) {
    			node = queue.poll();
    			
    			// try to regenerate state information of the node
    			
				try {
					node = new Node(game.getNextNode(node.getParent(), node.getMoves()));
				} catch (Exception e2) {}
    			// track our way of states so we don't visit states multiple times
    			visitedStates.put(node.getState().hashCode(), null);
    			
    			if(node.getState().getGoalValue(0) == 100) {
    				foundSolution = true;
    				this.solution = node;
    			}
    			
    			if(node.isTerminal()) {
    				unusefulStates.put(node.getState().hashCode(), null);
    				continue;
    			}
    			
    			if(node.getDepth() < this.currentDepthLimit) {
    				// add successor nodes to queue
    				try {
    					List<IMove[]> allMoves = game.getCombinedMoves(node.getWrapped());
    					IMove[] combMoves;
						Boolean noSuccesor = true;
    					for(int i=0; i<allMoves.size(); ++i) {
    						try {
    							combMoves = allMoves.get(i);
    							if((!visitedStates.containsKey(game.getNextNode(node.getWrapped(), combMoves).getState().hashCode()))
									&& !unusefulStates.containsKey(game.getNextNode(node.getWrapped(), combMoves).getState().hashCode())){
    								heuristic.put(game.getNextNode(node.getWrapped(), combMoves).getState(), value)
    								queue.add(new Node(game.getNextNode(node.getWrapped(), combMoves)));
									noSuccesor = false;
    							}
    						} catch (InterruptedException e) {}
    					}
						if(noSuccesor){
							if(node.getState() == null)
								game.regenerateNode(node.getWrapped());
							unusefulStates.put(node.getState().hashCode(), null);
						}
    				} catch (InterruptedException e1) {}
    				
    			}
    		}
    		if(foundSolution || System.currentTimeMillis() >= endTime) return;
    		queue.clear();
    		visitedStates.clear();
    		queue.add(new Node(game.getTree().getRootNode()));
    		currentDepthLimit++;
    	}
    }

	void DLS() {
		
	}
    
	void fillCurrentWay() {
		Node cur = solution;
		while(cur.getParent() != null) {
			System.out.println(cur);
			currentWay.add(0, cur);
			cur = new Node(cur.getParent());
		}
	}
    
}
