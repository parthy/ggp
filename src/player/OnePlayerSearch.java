package player;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.palamedes.gdl.core.model.IGameNode;
import org.eclipse.palamedes.gdl.core.model.IGameState;
import org.eclipse.palamedes.gdl.core.model.IMove;
import org.eclipse.palamedes.gdl.core.model.*;
import org.eclipse.palamedes.gdl.core.model.IGame;
import org.eclipse.palamedes.gdl.core.simulation.Match;
import org.eclipse.palamedes.gdl.core.simulation.strategies.AbstractStrategy;

public class OnePlayerSearch extends AbstractStrategy {
	private List<IGameNode> queue = new ArrayList<IGameNode>();
	private List<IGameNode> currentWay = new ArrayList<IGameNode>();
	private Map<IGameState, Integer> visitedStates = new HashMap<IGameState, Integer>();
	private HashMap<IGameState, HashMap<Integer, Integer>> values = new HashMap<IGameState, HashMap<Integer, Integer>>();
	private PriorityQueue<IGameNode> children;
	private boolean foundSolution = false;
	private int nodesVisited;
	private long endTime;
	IGameNode solution;
	IGameNode node;
	
	private IHeuristic heuristic;

	@Override
	public void initMatch(Match initMatch) {
		super.initMatch(initMatch);
		// initiate solution search
		game = initMatch.getGame();
		//game.getTree().getRootNode().setPreserve(true);
		
		heuristic = new OnePlayerHeuristic(this);
		
		queue.add(game.getTree().getRootNode());
		endTime = System.currentTimeMillis() + initMatch.getStartTime()*1000 - 2000;
		try {
			// simulate for half of the time, then use the experience to search
			while(System.currentTimeMillis() < endTime-initMatch.getStartTime()*450 && !foundSolution) {
				simulateGame(game.getTree().getRootNode());
			}
			Search();
		} catch (InterruptedException ex) {
			Logger.getLogger(OnePlayerSearch.class.getName()).log(Level.SEVERE, null, ex);
		}
		System.out.println("nodes visited: "+nodesVisited);
		System.out.println("values found: "+values.size());
		visitedStates.clear();
		if(foundSolution) fillCurrentWay();
		
	}

    @Override
    public IMove getMove(IGameNode node) {
    	/*
    	 * return the head element of currentWay and remove it from the list
    	 */
    	System.out.println(node);
    	if(!currentWay.isEmpty()) {
    		//System.out.println("Doing move "+currentWay.get(0).getMoves()[0].getMove()+" has value: "+values.get(currentWay.get(0).getState()).toString());
			return currentWay.remove(0).getMoves()[0];
    	}
		else {// if no way is there, just do something
			try {
				// if no way is there, just do something
				// first search a little
				long realEndTime = System.currentTimeMillis() + match.getPlayTime()*1000 - 2500;
				endTime = System.currentTimeMillis() + match.getPlayTime()*500;
				try {
					/*while(System.currentTimeMillis() < endTime) {
						simulateGame(node);
					}*/
					if(foundSolution) {
						fillCurrentWayLimited(node.getDepth());
						return currentWay.remove(0).getMoves()[0];
					} else {
						endTime = realEndTime;
						queue.clear();
						visitedStates.clear();
						queue.add(node);
						Search();
					}
				} catch(InterruptedException ex) {}
				
				List<IMove[]> allMoves = game.getCombinedMoves(node);
				PriorityQueue<IGameNode> children = new PriorityQueue<IGameNode>(10, new OnePlayerComparator(values));
				for(IMove[] move : allMoves) {
					IGameNode next = game.getNextNode(node, move);
					HashMap<Integer, Integer> val = values.get(next.getState());
					String str = "";
					if(val != null) str += values.get(next.getState());
					System.out.println("Possible move "+move[0].getMove()+" has value: "+str);
					if(next.isTerminal()){
						if(game.getGoalValues(next)[0] == 100){
							//if we can reach the goal -> do it
							return next.getMoves()[0];
						} else {
							if(game.getGoalValues(next)[0] == 0){
								//if we can come to a loss -> dont do it
							} else {
								children.add(next);
							}
						}
					}
					children.add(next);
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
    
    public IGame getGame() { return this.game; }
    
    void Search() throws InterruptedException {

		System.out.println("Now: "+System.currentTimeMillis()+", endTime: "+endTime);
		while(!queue.isEmpty() && System.currentTimeMillis() < endTime) {
			node = queue.remove(0);
			game.regenerateNode(node);
			nodesVisited++;
			//System.out.println("Current state: "+node.getState().getFluents().toString());
			// track our way of states so we don't visit states multiple times
			visitedStates.put(node.getState(), node.getDepth());

			// put some value in the values hash, maybe at first just heuristic guesses
			HashMap<Integer, Integer> tmp = new HashMap<Integer, Integer>();
			
			if(node.isTerminal()) {
				// set the goal value as a value you can totally rely on -> treated as it occurred MAX_VALUE times
				tmp.put(game.getGoalValues(node)[0], Integer.MAX_VALUE); 
				this.values.put(node.getState(), tmp);
				if(node.getState().getGoalValue(0) == 100) {
    					foundSolution = true;
    					this.solution = node;
    					return;
    				}
				continue;
			} else {
				// put a value calculated by the heuristic in our hash.
				int occ = -1;
				HashMap<Integer, Integer> entry = values.get(node.getState());
				if(entry != null) {
					if(entry.values().iterator().hasNext())
						occ = entry.values().iterator().next();
				}
				tmp.put(heuristic.calculateHeuristic(node), occ);
				this.values.put(node.getState(), tmp);
			}

			// add successor nodes to queue
			List<IMove[]> allMoves = game.getCombinedMoves(node);
			this.children = new PriorityQueue<IGameNode>(10, new OnePlayerComparator(values));
			for(IMove[] move : allMoves) {
				IGameNode next = game.getNextNode(node, move);
				//next.setPreserve(true);
				game.regenerateNode(next);
				if(!visitedStates.containsKey(next.getState()) || visitedStates.get(next.getState()) > next.getDepth()){
					this.children.add(next);
				}
				if(foundSolution || System.currentTimeMillis() >= endTime){
					this.children.clear();
					System.out.println("break because of time or found solution");
					return;
				}
			}
			while(!children.isEmpty()){
				//System.err.println("value "+values.get(children.peek().getState()));
				queue.add(0, children.poll());
				if(foundSolution || System.currentTimeMillis() >= endTime){
					System.out.println("break because of time or found solution");
					return;
				}
			}
		}
		if(foundSolution || System.currentTimeMillis() >= endTime){
			System.out.println("break because of time or found solution");
			return;
		}
		queue.clear();
    }

	void fillCurrentWay() {
		IGameNode cur = solution;
		while(cur.getParent() != null) {
			System.out.println(cur);
			currentWay.add(0, cur);
			cur = cur.getParent();
		}
	}
	
	void fillCurrentWayLimited(int limit) {
		IGameNode cur = solution;
		while(cur.getParent() != null && cur.getDepth() > limit) {
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
			game.regenerateNode(currentNode);
			// game over?
			if(currentNode.isTerminal()) {
				// watch out for the endTime
				if(System.currentTimeMillis() > endTime){
					System.out.println("stop search because of time");
					return;
				}
				
				value = currentNode.getState().getGoalValues();
				System.out.println("Played a game and got score "+value[playerNumber]);
				if(value[0] == 100) { // we accidentally won.
					foundSolution = true;
					solution = currentNode;
					return;
				}
				break;
			}
			
			// choose a move
			currentNode = game.getNextNode(currentNode, game.getRandomMove(currentNode));
		}
		
		// since the game is over, we can now go all the way back and fiddle around with the goals
		HashMap<Integer, Integer> existingValue = values.get(currentNode.getState());
		
		//System.out.println("Existing Value in Hash: "+existingValue);
		
		// if there is no value yet, we put it in
		if(existingValue == null) {
			HashMap<Integer, Integer> temp = new HashMap<Integer, Integer>();
			temp.put(value[0], 1);
			values.put(currentNode.getState(), temp);
		}
		// now we look at the parent of the goal state.
		IGameNode node = currentNode;

		while(node.getParent() != null && node.getDepth() <= start.getDepth()) {
			// watch out for the endTime
			if(System.currentTimeMillis() > endTime){
				System.out.println("stop search because of time");
				return;
			}
			
			node = node.getParent();
			game.regenerateNode(node);
			HashMap<Integer, Integer> entry = values.get(node.getState());
			Integer tempVal = null;
			if(entry != null) {
				tempVal = (Integer) values.get(node.getState()).keySet().toArray()[0];
			}
			if(entry == null || tempVal == null) { // no value in there yet, so we just set the achieved goal value
				HashMap<Integer, Integer> temp = new HashMap<Integer, Integer>();
				temp.put(value[0], 1);
				values.put(node.getState(), temp);
			} else { // otherwise, we build the average of the existing value and the achieved value in this particular game
				Integer newCount = ((Integer) values.get(node.getState()).values().toArray()[0])+1;
				if(newCount == 0) newCount++;
				tempVal = ((Double) (Math.ceil((double) (((double) ((newCount-1)*tempVal+value[0]))/((double) newCount))))).intValue();
				HashMap<Integer, Integer> temp = new HashMap<Integer, Integer>();
				temp.put(tempVal, newCount);
				values.put(node.getState(), temp);
			}
		}
	}
	
	/*
	 * helping function for game simulation: choose a move according to a heuristic
	 * for now we just take a random move
	 */
	private IMove[] getMoveForSimulation(IGameNode arg0) throws InterruptedException {
		return game.getRandomMove(arg0);
	}
	
	/*
	 * Hand out a value from our Hash
	 */
	public HashMap<Integer, Integer> getValue(IGameState state) {
		return this.values.get(state);
	}
	
	/*
	 * seemingly our destroy function
	 */
	public void dispose() {
		this.queue.clear();
		this.queue = null;
		this.values.clear();
		this.values = null;
		this.visitedStates.clear();
		this.visitedStates = null;
		this.currentWay.clear();
		this.currentWay = null;
		this.node = null;
		this.solution = null;
	}
}
