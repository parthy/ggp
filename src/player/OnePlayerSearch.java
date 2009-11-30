package player;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import java.util.PriorityQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.palamedes.gdl.core.model.IFluent;
import org.eclipse.palamedes.gdl.core.model.IGameNode;
import org.eclipse.palamedes.gdl.core.model.IGameState;
import org.eclipse.palamedes.gdl.core.model.IMove;
import org.eclipse.palamedes.gdl.core.simulation.Match;
import org.eclipse.palamedes.gdl.core.simulation.strategies.AbstractStrategy;

public class OnePlayerSearch extends AbstractStrategy {
	private List<IGameNode> queue = new ArrayList<IGameNode>();
	private List<IGameNode> currentWay = new ArrayList<IGameNode>();
	private Map<Integer, IGameState> visitedStates = new HashMap<Integer, IGameState>();
	private boolean foundSolution = false;
	private int nodesVisited;
	private long endTime;
	IGameNode solution;
	IGameNode node;

	@Override
	public void initMatch(Match initMatch) {
		super.initMatch(initMatch);
		// initiate solution search
		game = initMatch.getGame();
		queue.add(game.getTree().getRootNode());
		endTime = System.currentTimeMillis() + initMatch.getStartTime()*1000 - 5000L;
		try {
			Search();
		} catch (InterruptedException ex) {
			Logger.getLogger(OnePlayerSearch.class.getName()).log(Level.SEVERE, null, ex);
		}
		System.err.println("nodes visited: "+nodesVisited);
		visitedStates.clear();
		if(foundSolution) fillCurrentWay();
	}

    @Override
    public IMove getMove(IGameNode node) {
    	/*
    	 * return the head element of currentWay and remove it from the list
    	 */
    	System.out.println(node);
    	if(!currentWay.isEmpty())
			return currentWay.remove(0).getMoves()[0];
		else {// if no way is there, just do something
			try {
				// if no way is there, just do something
				List<IMove[]> allMoves = game.getCombinedMoves(node);
				PriorityQueue<IGameNode> children = new PriorityQueue<IGameNode>(10, new HeuristicComparator(this));
				for(IMove[] move : allMoves) {
					if(game.getNextNode(node, move).isTerminal()){
						if(game.getGoalValues(game.getNextNode(node, move))[0] == 100){
							//if we can reach the goal -> do it
							return game.getNextNode(node, move).getMoves()[0];
						} else {
							if(game.getGoalValues(game.getNextNode(node, move))[0] == 0){
								//if we can come to a loss -> dont do it
							} else {
								children.add(game.getNextNode(node, move));
							}
						}
					}
					children.add(game.getNextNode(node, move));
				}
				if(!children.isEmpty())
						return children.poll().getMoves()[0];
				return game.getRandomMove(node)[0]; // should not happen
			} catch (InterruptedException ex) {
				Logger.getLogger(OnePlayerSearch.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
		return null;
    }

    void Search() throws InterruptedException {

    	while(!foundSolution) {
			System.err.println("Now: "+System.currentTimeMillis()+", endTime: "+endTime);
    		while(!queue.isEmpty() && System.currentTimeMillis() < endTime) {
    			node = queue.remove(0);
				nodesVisited++;

    			// try to regenerate state information of the node
				game.regenerateNode(node);

				// track our way of states so we don't visit states multiple times
    			visitedStates.put(node.getState().hashCode(), null);

    			if(node.getState().getGoalValue(0) == 100) {
    				foundSolution = true;
    				this.solution = node;
    			}

   				// add successor nodes to queue
				List<IMove[]> allMoves = game.getCombinedMoves(node);
				PriorityQueue<IGameNode> children = new PriorityQueue<IGameNode>(10, new HeuristicComparator(this));
				for(IMove[] move : allMoves) {
					if(!visitedStates.containsKey(game.getNextNode(node, move).getState().hashCode())){
						children.add(game.getNextNode(node, move));
					}
				}
				while(!children.isEmpty()){
					System.err.println("value "+MyPlayer.strategy.getHeuristicValue(children.peek()));
					queue.add(0, children.poll());
				}
    		}
    		if(foundSolution || System.currentTimeMillis() >= endTime){
				System.err.println("break because of time");
				return;
			}
    		queue.clear();
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
