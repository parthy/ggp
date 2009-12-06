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
	private HashMap<IGameState, HashMap<int[], Integer>> maxNValues = new HashMap<IGameState, HashMap<int[], Integer>>();
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
		endTime = System.currentTimeMillis() + initMatch.getStartTime()*1000 - 1000L;
		try {
			//Search();
			while(System.currentTimeMillis() < endTime) {
				simulateGame(game.getTree().getRootNode());
			}
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
				// first simulate a little
				long end = System.currentTimeMillis() + match.getPlayTime()*1000 - 800;
				try {
					while(System.currentTimeMillis() < end)
						simulateGame(node);
				} catch(InterruptedException ex) {
					
				}
				List<IMove[]> allMoves = game.getCombinedMoves(node);
				PriorityQueue<IGameNode> children = new PriorityQueue<IGameNode>(10, new SimulationComparator(maxNValues));
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

	/*
	 * new method designed to play a game according to either random choose or some heuristic function
	 * while doing this, we already populate the values hash with some values
	 *  (goal -> goalValue, states before goal -> average goal value achieved from this state
	 */
	private void simulateGame(IGameNode start) throws InterruptedException {
		// first we just play a game
		IGameNode currentNode = start;
		int[] value;
		while(true) {
			// regenerate node, the usual crap
			game.regenerateNode(currentNode);
			
			// game over?
			if(currentNode.isTerminal()) { 
				value = currentNode.getState().getGoalValues();
				System.out.println("Played a game and got score "+value[playerNumber]);
				break;
			}
			
			// choose a move
			currentNode = game.getNextNode(currentNode, getMoveForSimulation(currentNode));
		}
		
		// since the game is over, we can now go all the way back and fiddle around with the goals
		HashMap<int[], Integer> existingValue = maxNValues.get(currentNode.getState());
		
		//System.out.println("Existing Value in Hash: "+existingValue);
		
		// if there is no value yet, we put it in
		if(existingValue == null) {
			HashMap<int[], Integer> temp = new HashMap<int[], Integer>();
			temp.put(value, 1);
			maxNValues.put(currentNode.getState(), temp);
		}
		// now we look at the parent of the goal state.
		IGameNode node = currentNode;
		while(node.getParent() != null) {
			game.regenerateNode(node); // we are a bit paranoid with that, but what the hell.
			node = node.getParent();
			game.regenerateNode(node); // we are a bit paranoid with that, but what the hell.
			HashMap<int[], Integer> entry = maxNValues.get(node.getState());
			int[] tempVal = null;
			if(entry != null) {
				tempVal = (int[]) maxNValues.get(node.getState()).keySet().toArray()[0];
			}
			if(entry == null || tempVal == null) { // no value in there yet, so we just set the achieved goal value
				HashMap<int[], Integer> temp = new HashMap<int[], Integer>();
				temp.put(value, 1);
				maxNValues.put(node.getState(), temp);
			} else { // otherwise, we build the average of the existing value and the achieved value in this particular game
				Integer newCount = ((Integer) maxNValues.get(node.getState()).values().toArray()[0])+1;
				for(int i=0; i<value.length; i++) {
					tempVal[i] = ((newCount-1)*tempVal[i]+value[i])/newCount;
				}
				HashMap<int[], Integer> temp = new HashMap<int[], Integer>();
				temp.put(tempVal, newCount);
				maxNValues.put(node.getState(), temp);
			}
		}
	}
	
	/*
	 * helping function for game simulation: choose a move according to a heuristic
	 * for now we just take a random move
	 */
	private IMove[] getMoveForSimulation(IGameNode arg0) throws InterruptedException {
		IGameNode best = null;
		try {
			PriorityQueue<IGameNode> childs = new PriorityQueue<IGameNode>(10, new HeuristicComparator(this));
			for(IMove[] combMove : game.getCombinedMoves(arg0)) {
				childs.add(game.getNextNode(arg0, combMove));
				//if((max == 1 && bestValue == 100) || (max == 0 && bestValue == 0)) break;
			}
			best = childs.peek();
			if(best == null) { // we didn't find anything.. (actually not possible)
				return game.getRandomMove(arg0);
			}
		} catch (InterruptedException e) {}
		
		return best.getMoves();
	}
}
