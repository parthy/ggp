package player;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

import org.eclipse.palamedes.gdl.core.model.IGameNode;
import org.eclipse.palamedes.gdl.core.model.IGameState;
import org.eclipse.palamedes.gdl.core.model.IMove;
import org.eclipse.palamedes.gdl.core.simulation.IStrategy;
import org.eclipse.palamedes.gdl.core.simulation.Match;
import org.eclipse.palamedes.gdl.core.simulation.strategies.AbstractStrategy;

public class MultiPlayerStrategy extends AbstractStrategy {

	/* Needed data structures:
	 * - Hash for values(simulation/search/heuristic)
	 * - heuristic: the heuristic object
	 * - visitedStates
	 * - queue: The Search Queue
	 * - root: the game tree root node
	 * - endTime: for setting the end of start/play time
	 * - turn-taking/zero-sum: flags for specialize playing strategy
	 * - some constants
	 */
	// values maps a state to an array constructed as follows:
	//  - [0] contains the actual goal/simulation/heuristic values
	//  - [1] looks like: { 0 => counter for simulator, 1 => source of value, one of the constants}
	private HashMap<IGameState, int[][]> values = new HashMap<IGameState, int[][]>();
	private Set<MemorizeState> memorizeStates = new HashSet<MemorizeState>();
	
	private Evaluator evaluator;

	// maps a state we visited to the depth where we found it 
	private HashMap<IGameState, Integer> visitedStates = new HashMap<IGameState, Integer>();
	
	// the current depth limit for IDS (not needed right now)
	private int currentDepthLimit;
	
	// the root node of our game tree
	IGameNode root;
	
	private long endTime;
	private long endSearchTime;
	private int nodesVisited;
	
	// flags for game properties. by optimistic assumption we set these initially to true and try to refute this in simulation.
	private boolean turntaking = true;
	private boolean zerosum = true;
	private boolean searchFinished = false;
	
	 // constants describing where node evaluations come from
	// SIM means we calculated the values using a monte carlo approach
	public final static int SIM = 0;
	// GOAL means we actually _know_ that this is the value
	public final static int GOAL = 1;
	// HEUR means we calculated the value using a heuristic function
	public final static int HEUR = 2;
	// PROP means we propagated the value using e.g. maxN
	public final static int PROP = 3;
	
	// maximum score sum, to be found out by simulation
	private final static boolean PRUNING_ENABLED = true;
	private String noop_name = "";
	private int max_score = 0;
	
	public void setFirstEndTime(long endTime) {
		this.endTime = endTime - 2500;
	}
	
	public void initMatch(Match arg0) {
            System.out.println("\nINIT: MultiPlayerStrategy.");
		// Initialize game variables
		match = arg0;
		game = match.getGame();
		playerNumber = game.getRoleIndex(match.getRole());
		
		// Set the root node
		root = game.getTree().getRootNode();
		//root.setPreserve(true);
		
		// First, we try to learn something about the game by randomly simulating it
		while(System.currentTimeMillis() < endTime-match.getStartTime()*550) { // simulate for almost the half start time
			try {
				simulateGame(root);
			} catch(InterruptedException e) {}
		}
		System.out.println("While simulating, we got "+values.size()+" values.");
		System.out.println("And we found a maximum score sum of "+max_score);
		
		// And start search
		currentDepthLimit = 1;
		
		// Set up the evaluator
	    this.evaluator = new Evaluator(values, memorizeStates, true);
		
		try {
			IDS(endTime, root);
		} catch(InterruptedException e) {}
	}

	public boolean IDS(long endSearchTime, IGameNode start) throws InterruptedException {
	        this.endSearchTime = endSearchTime;
	        Boolean finishedSearch = false;

	        try {
	            boolean canSearchDeeper = true;
	            while (canSearchDeeper) {

	                System.out.println("currentDepth " + currentDepthLimit);
	                System.out.println("Now: " + System.currentTimeMillis() + ", endTime: " + endSearchTime);
	                System.out.println("Visited: " + nodesVisited);

	                visitedStates.clear();
	                
	                // prepare the lower bounds
	                int[] lower_bounds = new int[game.getRoleCount()];
	                for(int i=0; i<game.getRoleCount(); i++)
	                	lower_bounds[i] = Integer.MIN_VALUE;
	                
	                if(Runtime.getRuntime().freeMemory() < 200*1024*1024) {
	                	return false;
	                }
	                
	                game.regenerateNode(start);
	                if(start == null || start.getState() == null || game.getCombinedMoves(start) == null)
	                	return false;
	                
	                for(IMove[] combMove : game.getCombinedMoves(start)) {
	                	if(System.currentTimeMillis() >= endSearchTime)
	                		return false;
	                	if(combMove == null)
	                		continue;
	                	
	                	IGameNode child = game.getNextNode(start, combMove);
	                	if(child == null)
	                		continue;
	                	canSearchDeeper = DLS(child, child.getDepth(), lower_bounds);
	                }
	                
	                currentDepthLimit++;
	            }

	            finishedSearch = true;
	        } catch (InterruptedException e) {
	            System.out.println("couldnt finish search because of time");
	        }

	        System.out.println("stopped search and visited " + nodesVisited + " nodes.");

	        return finishedSearch;
	    }
	
	/**
	     *
	     * also uses:
	     *	- currentDepthLimit
	     *	- endSearchTime
	     *	-
	     *	- evaluation()
	     *	- visitedStates
	     *
	     *	values.get(start_node) == max_int if search finished
	     *
	     * @param node to expand
	     * @param depth < depthLimit
	     * @return true if it could expand further but depthLimit stopped it
	     */
	private Boolean DLS(IGameNode node, int depth, int[] lower_bounds) throws InterruptedException {
		game.regenerateNode(node);
		nodesVisited++;

		int[] new_bounds = lower_bounds.clone();
		
		if (System.currentTimeMillis() >= endSearchTime) {
			// propagatedHash.put(node.getState(), evaluateNode(node));
			// we don't need to evaluate the state, because good values for the decision
			// should be made in the last iteration
			// if we can't even make the iteration with depth limit 1 we just suck
			return false;
		}

		if (node.isTerminal()) {
			// remember this state as being good to reach -> goal Distance
			if (game.getGoalValues(node)[playerNumber] <= 20 || game.getGoalValues(node)[playerNumber] >= 80) {
				memorizeStates.add(new MemorizeState(node.getState(), game.getGoalValues(node)[playerNumber]));
			}
			// save in hash
			values.put(node.getState(), evaluateNode(node));
			return false;
		}

		if (depth >= currentDepthLimit || Runtime.getRuntime().freeMemory() < 200*1024*1024) {
			// reached the fringe -> ask for a evaluation
			values.put(node.getState(), evaluateNode(node));
			// we can expand in the next iteration
			return true;
		}

		if (visitedStates.containsKey(node.getState())) {
			Integer foundDepth = visitedStates.get(node.getState());
			if (foundDepth <= depth) {
				// have already seen this and therefore evaluated it
				// there can't be the same state twice on one path
				return false;
			}
		}

		visitedStates.put(node.getState(), node.getDepth() - 1);
		// recursion

		// do not work with global variables in a recursive function
		//List<IGameNode> children = new LinkedList<IGameNode>();
		List<IMove[]> moves = game.getCombinedMoves(node);

		int player = whoseTurn(node);
		
		Boolean expandFurther = false;
		int j=1;
		for (IMove[] move : moves) {
			if(System.currentTimeMillis() >= endSearchTime)
        		break;
        	
			IGameNode child = game.getNextNode(node, move);
			//children.add(child);
			if (DLS(child, depth + 1, new_bounds)) {
				expandFurther = true;
			}

			// MaxN
			game.regenerateNode(node);
			game.regenerateNode(child);
			int[][] childVal = values.get(child.getState());
			int[][] parVal = values.get(node.getState());
			
			if(childVal == null)
				continue;

			// propagate values
			if(!turntaking || player == -1) {
				// no turntaking game, we build an average
				if(parVal == null || j == 1) {
					values.put(node.getState(), new int[][]{childVal[0].clone(), {0, PROP}});
				} else if(parVal != null) {
					int[][] before = values.get(node.getState()).clone();
					for(int k=0; k<before[0].length; k++) {
						before[0][k] = Math.round(new Float(((j-1)*before[0][k] + childVal[0][k]))/new Float(j)); 
					}
					before[1] = new int[]{j, PROP};
					values.put(node.getState(), before);
				}
			} else {
				// turntaking, find player in action
				if(childVal[0][player] > new_bounds[player]) {
					new_bounds[player] = childVal[0][player];
				}
				// if all lower bound are present and they add up to more than max_score, cut off
				boolean all_present = true;
				int sum=0;
				for(int i=0; i<new_bounds.length; i++) {
					if(new_bounds[i] == Integer.MIN_VALUE) {
						all_present = false;
						break;
					} else {
						sum += new_bounds[i];
					}
				}
				if(all_present && sum > max_score && PRUNING_ENABLED) {
					values.put(node.getState(), evaluateNode(node));
					return false;
				}
				if(parVal == null || childVal[0][player] > parVal[0][player]) {
					values.put(node.getState(), new int[][]{childVal[0].clone(), {0, PROP}});
				}
			}
		}
		return expandFurther;
	}

	private int[][] evaluateNode(IGameNode node) {
	        // if the node is terminal, return its goal value
		if(node.isTerminal()) 
			return new int[][]{node.getState().getGoalValues(), {0, GOAL}};
		
		return evaluator.evaluateNodeMP(node, game.getRoleCount());
        }
	
	/**
	 * Given a node this function determines which index is the player whose turn it is.
	 * @param node The node
	 * @return index The index in the moves array giving more than 1 move or -1, if not possible.
	 */
	private int whoseTurn(IGameNode node) throws InterruptedException {
		if(!turntaking) return -1;
		int index=-1;
		// regenerate node
		game.regenerateNode(node);
		
		IMove[][] moves = game.getLegalMoves(node);
		for(int i=0; i<moves.length; i++) {
			if(moves[i].length > 1 || (moves[i].length == 1 && !moves[i][0].getMove().equals(this.noop_name))) return i;
		}
		return index;
	}

	@Override
	public IMove getMove(IGameNode arg0) {
		// first search for the time we have
		long realEndTime = System.currentTimeMillis() + match.getPlayTime()*1000 - 3000;
		
		// if we there is the threat of memory exceed coming, clear visitedStates.
		// by the time this happens, it is not a that big problem, anyway.
		if(Runtime.getRuntime().freeMemory() < 200*1024*1024) {
			visitedStates.clear();
			values.clear();
		}
		
		try {
			if(!searchFinished) {
				endTime = realEndTime - match.getPlayTime()*550;
				while(System.currentTimeMillis() < endTime)
					simulateGame(arg0);
				endTime = realEndTime-500;
				currentDepthLimit = arg0.getDepth()+1;
				IDS(endTime, arg0);
			}
			// search finished or end of time, now we have to decide.
			PriorityQueue<IGameNode> childs = new PriorityQueue<IGameNode>(10, new MultiPlayerComparator(values, playerNumber));
			for(IMove[] combMove : game.getCombinedMoves(arg0)) {
				if(System.currentTimeMillis() > endTime+1000) {
					// we have no time left, just return random move
					System.out.println("No time left!");
					break;
				}
				IGameNode next = game.getNextNode(arg0, combMove);
				
				if(next.isTerminal() && next.getState().getGoalValues() != null && next.getState().getGoalValues()[playerNumber] == 100)
                	return next.getMoves()[playerNumber];
				
				// regenerate node
				game.regenerateNode(next);
				
				//System.out.println("Possible move: "+combMove[playerNumber]+", values "+aryToString(values.get(next.getState())));
				childs.add(next);
			}
			if(childs.size() == 0)
				return game.getRandomMove(arg0)[playerNumber];
			
			IGameNode best = childs.peek();
			if(best == null || best.getMoves()[playerNumber].toString().equals(this.noop_name)) { // we didn't find anything.. (actually not possible)
				return game.getRandomMove(arg0)[playerNumber];
			}
			return best.getMoves()[playerNumber];
		} catch(InterruptedException e) {}
		
		return null;
	}
	
	/**
	 * Simulation function: Simulates one game.
	 * @param start The node we should start with
	 */
	private void simulateGame(IGameNode start) throws InterruptedException {
		// first we just play a game
		IGameNode currentNode = start;
		
		// regenerate node
		game.regenerateNode(currentNode);
		
		//System.out.println("Turntaking: "+turntaking+", Noop name is "+noop_name);
		
		int[] value;
		while(true) {
			// regenerate node
			game.regenerateNode(currentNode);
			if(System.currentTimeMillis() >= endTime) return;
			// try to prove that we have simultaneous moves
			int turns = 0;
			String noop = "noop";
			IMove[][] legalMoves = game.getLegalMoves(currentNode);
			for(int i=0; i<legalMoves.length; i++) {
				if(legalMoves[i].length > 1) 
					turns++;
				else if(legalMoves[i].length == 1) 
					noop = legalMoves[i][0].getMove();
			}
			if(turntaking && turns > 1) {
				this.turntaking = false;
				//System.out.println("We found out that the game is not turn-taking!");
			}
			if(turntaking && noop_name.equals("")) {
				System.out.println("Found out that noop has the name "+noop);
				this.noop_name = noop;
			}
			
			// game over?
			if(currentNode.isTerminal()) { 
				value = currentNode.getState().getGoalValues();
				values.put(currentNode.getState(), new int[][]{value, {Integer.MAX_VALUE, GOAL}});
				// try to prove that we don't have zero sum games.
				int sum = 0;
				for(int i=0; i<value.length; i++) {
					sum += value[i];
				}
				if(zerosum && sum != 100) {
					this.zerosum = false;
					//System.out.println("We found out that the game is not zero-sum!");
				}
				// adjust the maximum score
				if(sum > max_score)
					max_score = sum;
				//System.out.println("Played a game and got score "+value[playerNumber]);
				break;
			}
			
			// choose a move
			currentNode = game.getNextNode(currentNode, game.getRandomMove(currentNode));
		}
		
		// since the game is over, we can now go all the way back and fiddle around with the goals
		int[][] existingValue = values.get(currentNode.getState());
		
		//System.out.println("Existing Value in Hash: "+existingValue);
		
		// if there is no value yet, we put it in
		if(existingValue == null) {
			// set the array
			values.put(currentNode.getState(), new int[][]{value, {1, SIM}});
		}
		// now we look at the parents of the goal state.
		IGameNode node = currentNode;
		// regenerate node
		game.regenerateNode(node);
		
		while(node.getParent() != null && node.getParent().getDepth() >= start.getDepth()) {
			if(System.currentTimeMillis() >= endTime) return;

			node = node.getParent();
			// regenerate node
			game.regenerateNode(node);
			
			existingValue = values.get(node.getState());
			int[] tempVal = null;
			if(existingValue != null) {
				tempVal = existingValue[0];
			}
			if(existingValue == null || tempVal == null) { // no value in there yet, so we just set the achieved goal value
				values.put(node.getState(), new int[][]{value, {1, SIM}});
			} else { // otherwise, we build the average of the existing value and the achieved value in this particular game
				Integer newCount = existingValue[1][0]+1;
				for(int i=0; i<tempVal.length; i++) {
					tempVal[i] = Math.round(new Float((tempVal[i]*(newCount-1) + value[i]))/new Float(newCount));
				}
				values.put(node.getState(), new int[][]{tempVal, {newCount, SIM}});
			}
		}
	}
	
	private String aryToString(int[][] ary) {
		if(ary == null) return "{}";
		String str = "{";
		
		for(int i=0; i<ary.length; i++) {
			str += "{ ";
			for(int j=0; j<ary[i].length; j++)
				str += ary[i][j]+" ";
			str += "} ";
		}
		
		str += " }";
		
		return str;
	}
	
	/*
	 * seemingly our destroy function
	 */
	public void dispose() {
		this.values.clear();
		this.values = null;
		this.visitedStates.clear();
		this.visitedStates = null;
		this.root = null;
	}
}
