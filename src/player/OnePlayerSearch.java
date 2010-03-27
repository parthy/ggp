package player;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.palamedes.gdl.core.model.IFluent;
import org.eclipse.palamedes.gdl.core.model.IGame;
import org.eclipse.palamedes.gdl.core.model.IGameNode;
import org.eclipse.palamedes.gdl.core.model.IGameState;
import org.eclipse.palamedes.gdl.core.model.IMove;
import org.eclipse.palamedes.gdl.core.resolver.prologprover.FluentAdapter;
import org.eclipse.palamedes.gdl.core.simulation.Match;
import org.eclipse.palamedes.gdl.core.simulation.strategies.AbstractStrategy;

import com.parctechnologies.eclipse.Atom;

public class OnePlayerSearch extends AbstractStrategy {
	private List<IGameNode> currentWay = new ArrayList<IGameNode>();
	private Map<String, Integer> visitedStates = new HashMap<String, Integer>();
	private HashMap<String, ValuesEntry> values = new HashMap<String, ValuesEntry>();
	private boolean foundSolution = false;
	private int nodesVisited;
	private long endTime;
	IGameNode solution;
	Integer curMaxGoal = 0;
	IGameNode node;
	
	private int currentDepthLimit;

	private HashMap<String, List<String>> domains = new HashMap<String, List<String>>();
	private HashMap<String, Integer> domainSizes = new HashMap<String, Integer>();
	private int maxDepth = 0;
	private String stepcounter = "";
	private boolean REMOVE_STEPCOUNTER = false;

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

		try {
			// simulate for half of the time, then use the experience to search
			while(System.currentTimeMillis() < endTime-initMatch.getStartTime()*600 && !foundSolution) {
				simulateGame(game.getTree().getRootNode());
			}
			for(String fluent : domainSizes.keySet()) {
				if(domainSizes.get(fluent) != null && domainSizes.get(fluent) == this.maxDepth) {
					this.stepcounter = fluent;
					break;
				}
			}
			System.out.println("Done simulating. I found out that "+stepcounter+" must be the stepcounter.");
			
			currentDepthLimit = maxDepth+5;
			
			IDS(game.getTree().getRootNode());
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
		if(Runtime.getRuntime().freeMemory() < 100*1024*1024) {
			visitedStates.clear();
			values.clear();
		}
		System.out.println(node);
		if(foundSolution && !currentWay.isEmpty()) {
			//System.out.println("Doing move "+currentWay.get(0).getMoves()[0].getMove()+" has value: "+values.get(currentWay.get(0).getState()).toString());
			return currentWay.remove(0).getMoves()[0];
		}
		else {// if no way is there, just do something
			try {
				// if no way is there, just do something
				// first search a little
				long realEndTime = System.currentTimeMillis() + match.getPlayTime()*1000 - 800;
				endTime = System.currentTimeMillis() + match.getPlayTime()*400;
				try {
					while(System.currentTimeMillis() < endTime) {
						simulateGame(node);
					}
					// fill the current way with the best we've got
					fillCurrentWayLimited(node.getDepth());
					if(foundSolution) {
						return currentWay.remove(0).getMoves()[0];
					} else {
						endTime = realEndTime;
						
						visitedStates.clear();
						currentDepthLimit = maxDepth+5;
						
						IDS(node);
					}
					// fill the current way with the best we've got
					fillCurrentWayLimited(node.getDepth());
					if(foundSolution) {
						return currentWay.remove(0).getMoves()[0];
					}
				} catch(InterruptedException ex) {}

				List<IMove[]> allMoves = new LinkedList<IMove[]>(game.getCombinedMoves(node));
				
				// ensure that we take random moves if all are equal
				Collections.shuffle(allMoves);
				
				if(curMaxGoal > 0)
					System.out.println("Currently best solution yields "+curMaxGoal+" using move "+currentWay.get(0).getMoves()[0]);
				
				Integer min_occs=Integer.MAX_VALUE;

				PriorityQueue<IGameNode> children = new PriorityQueue<IGameNode>(10, new OnePlayerComparator(values, false, this));
				for(IMove[] move : allMoves) {
					IGameNode next = game.getNextNode(node, move);
					String k = makeKeyString(next.getState());
					ValuesEntry val = values.get(k);
					if(val != null && val.getOccurences() < min_occs) {
						min_occs = val.getOccurences();
					}
					System.out.println("Possible move "+move[0].getMove()+" has value: "+val);
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
					IGameNode next = children.poll();
					game.regenerateNode(next);
					ValuesEntry val = values.get(makeKeyString(next.getState()));
					if(val.getGoalArray()[0] <= curMaxGoal && currentWay.size() > 0) {
						System.out.println("Found nothing better, playing according to currently best solution.");
						if(min_occs == Integer.MAX_VALUE) {
							// We can not get any better. Stop looking for it. 
							foundSolution = true;
						}
						return currentWay.remove(0).getMoves()[0];
					}
					
					if(val != null && val.getGoalArray()[0] == 0 && val.getOccurences() == Integer.MAX_VALUE) {
						// we take the 0 with the least certainty
						IMove last = null;
						while(children.isEmpty())
							last = children.poll().getMoves()[0];
						
						return last;
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

	public IGame getGame() { 
		return this.game; 
	}

	private void IDS(IGameNode start) throws InterruptedException {
	        try {
	            boolean canSearchDeeper = true;
	            while (canSearchDeeper) {

	                System.out.println("currentDepth " + currentDepthLimit);
	                System.out.println("Now: " + System.currentTimeMillis() + ", endTime: " + endTime);
	                System.out.println("Visited: " + nodesVisited);

	                visitedStates.clear();
	                
	                canSearchDeeper = DLS(start, start.getDepth());
	                currentDepthLimit++;
	            }
	        } catch (InterruptedException e) {
	            System.out.println("couldnt finish search because of time");
	        }
	}
	
	private boolean DLS(IGameNode node, int depth) throws InterruptedException {
		if(foundSolution)
			return false;
		
		game.regenerateNode(node);
		
		nodesVisited++;

		
		if (System.currentTimeMillis() >= endTime || Runtime.getRuntime().freeMemory() < 200*1024*1024) {
			visitedStates.clear();
			if(values.get(makeKeyString(node.getState())) == null)
				values.put(makeKeyString(node.getState()), new ValuesEntry(new int[]{heuristic.calculateHeuristic(node, stepcounter)}, -1));
			return false;
		}

		if (node.isTerminal()) {
			
			if(node.getState().getGoalValues()[0] == 100) {
				foundSolution = true;
				solution = node;
			} else if(node.getState().getGoalValues()[0] > curMaxGoal) {
				curMaxGoal = node.getState().getGoalValues()[0];
				solution = node;
			}
			// save in hash
			values.put(makeKeyString(node.getState()), new ValuesEntry(node.getState().getGoalValues(), Integer.MAX_VALUE));
			return false;
		}
		
        if(Runtime.getRuntime().freeMemory() < 200*1024*1024){
            System.out.println("WARNING: memory full");
            visitedStates.clear();
            return false;
        }

        if (depth >= currentDepthLimit) {
			// reached the fringe -> ask for a evaluation
			
			// if we have a certain value in there, don't destroy it.
			ValuesEntry prev = values.get(makeKeyString(node.getState()));
			if(prev != null && prev.getOccurences() == Integer.MAX_VALUE)
				return true;
			
			values.put(makeKeyString(node.getState()), new ValuesEntry(new int[]{heuristic.calculateHeuristic(node, stepcounter)}, 1));
			
			
			// we can expand in the next iteration
			return true;
		}
                


		if (visitedStates.containsKey(node.getState().toString())) {
			Integer foundDepth = visitedStates.get(node.getState().toString());
			if (foundDepth <= depth) {
				// have already seen this and therefore evaluated it
				// there can't be the same state twice on one path
				return false;
			}
		}

		visitedStates.put(node.getState().toString(), node.getDepth() - 1);
		// recursion

		// do not work with global variables in a recursive function
		PriorityQueue<IGameNode> children = new PriorityQueue<IGameNode>(10, new OnePlayerComparator(values, false, this));
		List<IMove[]> moves = game.getCombinedMoves(node);
		IGameNode child;
		ValuesEntry childVal;
		ValuesEntry parVal = values.get(makeKeyString(node.getState()));
		
		// we already examined this node fully. cut search here.
		if(parVal != null && parVal.getOccurences() == Integer.MAX_VALUE) {
			//System.out.println("Found a node we already have fully expanded. Cutting search.");
			return false;
		}
		
		Boolean expandFurther = false;
		for (IMove[] move : moves) {
			child = game.getNextNode(node, move);
			
			childVal = values.get(makeKeyString(child.getState()));
			if(childVal == null)
				values.put(makeKeyString(child.getState()), new ValuesEntry(new int[]{heuristic.calculateHeuristic(child, stepcounter)}, -1));
			children.add(child);
		}

		Integer min_occurences=Integer.MAX_VALUE;
		
		while(!children.isEmpty()) {
			child = children.poll();
			
			// Max
			game.regenerateNode(node);
			game.regenerateNode(child);	
			
			
			
			if (DLS(child, depth + 1)) {
				expandFurther = true;
			}
			
			childVal = values.get(makeKeyString(child.getState()));
			
			if(childVal.getOccurences() < min_occurences)
				min_occurences = childVal.getOccurences();
			
			if(values.get(makeKeyString(child.getState())) == null)
				values.put(makeKeyString(child.getState()), new ValuesEntry(new int[]{heuristic.calculateHeuristic(child, stepcounter)}, -1));
			
			// propagate values, if:
			// (1) the parent has no value yet, or
			// (2) the parent has a value that is smaller, or
			// (3) the parent has a value that is less reliable, i.e. the child has occ INTMAX, but the parent doesn't.
			if((parVal == null && childVal != null) || (childVal != null && parVal != null && (childVal.getGoalArray()[0] > parVal.getGoalArray()[0] || (childVal.getOccurences() == Integer.MAX_VALUE && parVal.getOccurences() < Integer.MAX_VALUE)))) {
				parVal = new ValuesEntry(new int[]{childVal.getGoalArray()[0]}, childVal.getOccurences());
			}
		}
		
		values.put(makeKeyString(node.getState()), new ValuesEntry(parVal.getGoalArray(), min_occurences));
		
		
		return expandFurther;
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
				} else {
					key += f+" ";
				}
			}
		} else {
			key = state.toString();
		}

		return key;
	}

	void fillCurrentWay() {
		IGameNode cur = solution;
		while(cur.getParent() != null) {
			//System.out.println(cur);
			currentWay.add(0, cur);
			cur = cur.getParent();
		}
	}

	void fillCurrentWayLimited(int limit) {
		if(solution == null)
			return;
		IGameNode cur = solution;
		currentWay.clear();
		while(cur.getParent() != null && cur.getDepth() > limit) {
			//System.out.println(cur);
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
            try{
		if(Runtime.getRuntime().freeMemory() < 100*1024*1024) {
			domainSizes.clear();
			domains.clear();
			throw new InterruptedException("No memory left!");
		}
		// first we just play a game
		IGameNode currentNode = start;
		int[] value;
		while(true) {
			// watch out for the endTime
			if(System.currentTimeMillis() > endTime){
				System.out.println("stop sim because of time");
				return;
			}
			
			game.regenerateNode(currentNode);

			// remember for each fluent, how many different occurrences it had
			List<IFluent> fluents = currentNode.getState().getFluents();
			for(IFluent f : fluents) {
				if(((FluentAdapter) f).getNativeTerm().arity() == 0)
					continue;
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
				//System.out.println("values "+values.size());
                if(value[0] == 100) { // we accidentally won.
					foundSolution = true;
					solution = currentNode;
					return;
				} else {
					if(value[0] > curMaxGoal) {
						// we remember the best score we got so that we can weigh the moves according to that solution
						solution = currentNode;
						curMaxGoal = value[0];
					}
				}
                
				break;
			}

			// choose a move
			currentNode = game.getNextNode(currentNode, game.getRandomMove(currentNode));
		}

		// since the game is over, we can now go all the way back and fiddle around with the goals
		values.put(makeKeyString(currentNode.getState()), new ValuesEntry(value, Integer.MAX_VALUE));
		
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
			ValuesEntry entry = values.get(makeKeyString(node.getState()));
			Integer tempVal = null;
			if(entry != null) {
				tempVal = (Integer) values.get(makeKeyString(node.getState())).getGoalArray()[0];
			}
			if(entry == null || tempVal == null) { // no value in there yet, so we just set the achieved goal value
				values.put(makeKeyString(node.getState()), new ValuesEntry(value, 1));
			} else { // otherwise, we build the average of the existing value and the achieved value in this particular game
				ValuesEntry old = values.get(makeKeyString(node.getState()));
				Integer newCount = old.getOccurences();
				
				if(old.getGoalArray()[0] < value[0]) {
					// If there was a lower value in the hash, this is obviously wrong. what we can get in single player, we can definitely get.
					values.put(makeKeyString(node.getState()), new ValuesEntry(value, newCount++));
				} else if(newCount < Integer.MAX_VALUE && value[0] > old.getGoalArray()[0]) {
					// only if we have a value greater than or equal to value, build the average.
					newCount++;
					int[] goalValues;
					if(newCount == 0) {
						// means the value was heuristic, count a bit more on that one
						goalValues = new int[]{ Math.round((new Float((6*tempVal+4*value[0])*0.1))) };
						newCount = -1;
					} else {
						goalValues = new int[]{ Math.round((new Float(((newCount-1)*tempVal+value[0]))/new Float(newCount))) };
					}
					values.put(makeKeyString(node.getState()), new ValuesEntry(goalValues, newCount));
				}
			}
		}
            }catch(Exception e){
            }
	}

	/*
	 * Hand out a value from our Hash
	 */
	public ValuesEntry getValue(IGameState state) {
		return this.values.get(makeKeyString(state));
	}

	/*
	 * seemingly our destroy function
	 */
	public void dispose() {
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
