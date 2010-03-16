package player;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.palamedes.gdl.core.model.*;
import org.eclipse.palamedes.gdl.core.simulation.Match;
import org.eclipse.palamedes.gdl.core.simulation.strategies.AbstractStrategy;
import org.eclipse.palamedes.gdl.core.resolver.prologprover.*;

import com.parctechnologies.eclipse.Atom;

public class OnePlayerSearch extends AbstractStrategy {
	private List<IGameNode> queue = new ArrayList<IGameNode>();
	private List<IGameNode> currentWay = new ArrayList<IGameNode>();
	private Map<String, Integer> visitedStates = new HashMap<String, Integer>();
	private HashMap<String, HashMap<Integer, Integer>> values = new HashMap<String, HashMap<Integer, Integer>>();
	private PriorityQueue<IGameNode> children;
	private boolean foundSolution = false;
	private int nodesVisited;
	private long endTime;
	IGameNode solution;
	IGameNode node;
	
	private HashMap<String, List<String>> domains = new HashMap<String, List<String>>();
	private HashMap<String, Integer> domainSizes = new HashMap<String, Integer>();
	private int maxDepth = 0;
	private String stepcounter = "";
	private boolean REMOVE_STEPCOUNTER = true;
	
	private OnePlayerHeuristic heuristic;

	public void setFirstEndTime(long endTime) {
		this.endTime = endTime - 600;
	}
	
	public String getStepCounter() {
		return this.stepcounter;
	}
	
	@Override
	public void initMatch(Match initMatch) {
		super.initMatch(initMatch);
		// initiate solution search
		game = initMatch.getGame();
		
		heuristic = new OnePlayerHeuristic(this);
		
		queue.add(game.getTree().getRootNode());
	
		try {
			// simulate for half of the time, then use the experience to search
			while(System.currentTimeMillis() < endTime-initMatch.getStartTime()*450 && !foundSolution) {
				simulateGame(game.getTree().getRootNode());
			}
			for(String fluent : domainSizes.keySet()) {
				if(domainSizes.get(fluent) != null && domainSizes.get(fluent) == this.maxDepth) {
					this.stepcounter = fluent;
					break;
				}
			}
			System.out.println("Done simulating. I found out that "+stepcounter+" must be the stepcounter.");
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
				long realEndTime = System.currentTimeMillis() + match.getPlayTime()*1000 - 1200;
				endTime = System.currentTimeMillis() + match.getPlayTime()*500;
				try {
					while(System.currentTimeMillis() < endTime) {
						simulateGame(node);
					}
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
				PriorityQueue<IGameNode> children = new PriorityQueue<IGameNode>(10, new OnePlayerComparator(values, false, this));
				for(IMove[] move : allMoves) {
					IGameNode next = game.getNextNode(node, move);
					String k = makeKeyString(next.getState());
					HashMap<Integer, Integer> val = values.get(k);
					String str = "";
					if(val != null) str += val;
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
				if(!children.isEmpty()) {
					IGameNode next = game.getNextNode(node, children.peek().getMoves());
					game.regenerateNode(next);
					HashMap<Integer, Integer> val = values.get(makeKeyString(next.getState()));
					if(val != null && (Integer) val.keySet().toArray()[0] == 0) {
						// random is better than loss
						return game.getRandomMove(node)[0];
					}
					return children.poll().getMoves()[0];
				}
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
			
			visitedStates.put(node.getState().toString(), node.getDepth());
			

			// put some value in the values hash, maybe at first just heuristic guesses
			HashMap<Integer, Integer> tmp = new HashMap<Integer, Integer>();
			
			if(node.isTerminal()) {
				// set the goal value as a value you can totally rely on -> treated as it occurred MAX_VALUE times
				tmp.put(game.getGoalValues(node)[0], Integer.MAX_VALUE); 
				this.values.put(makeKeyString(node.getState()), tmp);
				if(node.getState().getGoalValue(0) == 100) {
    					foundSolution = true;
    					this.solution = node;
    					return;
    				}
				continue;
			} else {
				// put a value calculated by the heuristic in our hash.
				int occ = -1;
				HashMap<Integer, Integer> entry = values.get(makeKeyString(node.getState()));
				if(entry != null) {
					if(entry.values().iterator().hasNext())
						occ = entry.values().iterator().next();
				}

				tmp.put(heuristic.calculateHeuristic(node, stepcounter), occ);
				this.values.put(makeKeyString(node.getState()), tmp);
			}

			// add successor nodes to queue
			List<IMove[]> allMoves = game.getCombinedMoves(node);
			children = new PriorityQueue<IGameNode>(10, new OnePlayerComparator(values, true, this));
			for(IMove[] move : allMoves) {
				IGameNode next = game.getNextNode(node, move);
				//next.setPreserve(true);
				game.regenerateNode(next);
				
				String key = next.getState().toString();
				
				if(!visitedStates.containsKey(key) || visitedStates.get(key) > next.getDepth()){
					children.add(next);
				}
				if(foundSolution || System.currentTimeMillis() >= endTime){
					this.children.clear();
					System.out.println("break because of time or found solution");
					return;
				}
			}

			while(!children.isEmpty()) {
				IGameNode n = children.poll();
				queue.add(0, n);
			}
			//System.out.println("Queue size: "+queue.size());
			children.clear();
		}
		if(foundSolution || System.currentTimeMillis() >= endTime){
			System.out.println("break because of time or found solution");
			return;
		}
		queue.clear();
    }

	public String makeKeyString(IGameState state) {
		String key = "";
		if(!stepcounter.equals("") && REMOVE_STEPCOUNTER) {
			List<IFluent> fluents = state.getFluents();
			Iterator<IFluent> it = fluents.iterator();
			while(it.hasNext()) {
				IFluent f = it.next();
				if(f.getName().equals(stepcounter)) {
					fluents.remove(f);
					break;
				}
			}
			for(IFluent f : fluents) {
				key += f+" ";
			}
		} else {
			key = state.toString();
		}
		
		return key;
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
			
			// remember for each fluent, how many different occurrences it had
			List<IFluent> fluents = currentNode.getState().getFluents();
			for(IFluent f : fluents) {
				// get current entries in hashes
				Integer size = domainSizes.get(f.getName());
				if(size == null)
					size = 0;
				
				List<String> domain = domains.get(f.getName());
				if(domain == null)
					domain = new ArrayList<String>();
				
				String valueThisTime = "";
				if((((FluentAdapter) f).getNativeTerm().arg(1)) instanceof Atom)
					valueThisTime = ((Atom) (((FluentAdapter) f).getNativeTerm().arg(1))).functor();
				else if((((FluentAdapter) f).getNativeTerm().arg(1)) instanceof Integer)
					valueThisTime = ((Integer) (((FluentAdapter) f).getNativeTerm().arg(1))).toString();
				else
					throw new InterruptedException("Fluent value not found!");
				
				if(!domain.contains(valueThisTime)) {
					domain.add(valueThisTime);
					size++;
				}
				domainSizes.put(f.getName(), size);
				domains.put(f.getName(), domain);
			}
			
			// game over?
			if(currentNode.isTerminal()) {
				value = currentNode.getState().getGoalValues();
				
				if(currentNode.getDepth()+1 > maxDepth)
					maxDepth = currentNode.getDepth()+1;
				
				//System.out.println("Played a game and got score "+value[playerNumber]);
				if(value[0] == 100) { // we accidentally won.
					foundSolution = true;
					solution = currentNode;
					return;
				}
				
				// watch out for the endTime
				if(System.currentTimeMillis() > endTime){
					System.out.println("stop sim because of time");
					return;
				}
				break;
			}
			
			// fill the domainSizes hash
			
			// choose a move
			currentNode = game.getNextNode(currentNode, game.getRandomMove(currentNode));
		}
		
		// since the game is over, we can now go all the way back and fiddle around with the goals
		HashMap<Integer, Integer> existingValue = values.get(makeKeyString(currentNode.getState()));
		
		//System.out.println("Existing Value in Hash: "+existingValue);
		
		// if there is no value yet, we put it in
		if(existingValue == null) {
			HashMap<Integer, Integer> temp = new HashMap<Integer, Integer>();
			temp.put(value[0], 1);
			values.put(makeKeyString(currentNode.getState()), temp);
		}
		// now we look at the parent of the goal state.
		IGameNode node = currentNode;

		while(node.getParent() != null && node.getParent().getDepth() >= start.getDepth()) {
			// watch out for the endTime
			if(System.currentTimeMillis() > endTime){
				System.out.println("stop search because of time");
				return;
			}
			
			node = node.getParent();
			game.regenerateNode(node);
			HashMap<Integer, Integer> entry = values.get(makeKeyString(node.getState()));
			Integer tempVal = null;
			if(entry != null) {
				tempVal = (Integer) values.get(makeKeyString(node.getState())).keySet().toArray()[0];
			}
			if(entry == null || tempVal == null) { // no value in there yet, so we just set the achieved goal value
				HashMap<Integer, Integer> temp = new HashMap<Integer, Integer>();
				temp.put(value[0], 1);
				values.put(makeKeyString(node.getState()), temp);
			} else { // otherwise, we build the average of the existing value and the achieved value in this particular game
				Integer newCount = ((Integer) values.get(makeKeyString(node.getState())).values().toArray()[0])+1;
				if(newCount == 0) newCount++;
				tempVal = Math.round((new Float(((newCount-1)*tempVal+value[0]))/new Float(newCount)));
				HashMap<Integer, Integer> temp = new HashMap<Integer, Integer>();
				temp.put(tempVal, newCount);
				values.put(makeKeyString(node.getState()), temp);
			}
		}
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
