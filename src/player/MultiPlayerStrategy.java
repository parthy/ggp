package player;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;

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
	
	private MultiPlayerHeuristic heuristic;

	// maps a state we visited to the depth where we found it 
	private HashMap<IGameState, Integer> visitedStates = new HashMap<IGameState, Integer>();
	
	// not yet instantiated, because we can specify a domain-specific comparator
	private PriorityQueue<IGameNode> queue;
	
	// the current depth limit for IDS (not needed right now)
	// private int currentDepthLimit;
	
	// the root node of our game tree
	IGameNode root;
	
	private long endTime;
	private int nodesVisited;
	
	// flags for game properties. by optimistic assumption we set these initially to true and try to refute this in simulation.
	private boolean turntaking = true;
	private boolean zerosum = true;
	private boolean searchFinished = false;

	// constants describing where node evaluations come from
	// SIM means we calculated the values using a monte carlo approach
	private static int SIM = 0;
	// GOAL means we actually _know_ that this is the value
	private static int GOAL = 1;
	// HEUR means we calculated the value using a heuristic function
	private static int HEUR = 2;
	// PROP means we propagated the value using e.g. maxN
	private static int PROP = 3;
	
	public void initMatch(Match arg0) {
		// Initialize game variables
		endTime = System.currentTimeMillis() + arg0.getStartTime()*1000 - 1200; // our time stops then
		match = arg0;
		game = match.getGame();
		playerNumber = game.getRoleIndex(match.getRole());
		
		// Set the root node
		root = game.getTree().getRootNode();
		//root.setPreserve(true);
		
		// First, we try to learn something about the game by randomly simulating it
		while(System.currentTimeMillis() < endTime-match.getStartTime()*450) { // simulate for almost the half start time
			try {
				simulateGame(root);
			} catch(InterruptedException e) {}
		}
		System.out.println("While simulating, we got "+values.size()+" values.");
		// Set the heuristic we want to use
		this.heuristic = new MultiPlayerHeuristic(values, playerNumber, game.getRoleCount());
		
		// Set up the queue for search
		this.queue = new PriorityQueue<IGameNode>(50, new MultiPlayerComparator(values, playerNumber));
		
		// And start search
		//currentDepthLimit = 1;
		queue.add(root);
		try {
			DFS(0);
		} catch(InterruptedException e) {}
	}

	/**
	 * Search function for iterative deepening search.
	 * @param makeValueLimit The minimum depth to which we propagate values to.
	 */
	private void DFS(int makeValueLimit) throws InterruptedException {
		IGameNode currentNode;
		List<IGameNode> children = new LinkedList<IGameNode>();
		
		visitedStates.clear();
		
		while(!queue.isEmpty() && System.currentTimeMillis() < endTime) {
			// get next element from queue
			currentNode = queue.poll();
			game.regenerateNode(currentNode);
			nodesVisited++;
			//if(nodesVisited % 1000 == 0) System.out.println("visited: "+nodesVisited);
			
			// check if we get a value for the values hash
			if(currentNode.isTerminal()){
				int[] value = game.getGoalValues(currentNode);
				values.put(currentNode.getState(), new int[][]{value.clone(), {0, GOAL}});
				makeValue(currentNode, makeValueLimit);
				continue;
			}
			// Set the value according to a heuristic.
			//values.put(currentNode.getState(), new int[][]{heuristic.getHeurArray(currentNode), {0, HEUR}});
			//makeValue(currentNode, makeValueLimit);
			// leave tracing infos
			visitedStates.put(currentNode.getState(), currentNode.getDepth()-1);
			
			children.clear();
			
			// find out possible successors
			List<IMove[]> allMoves = game.getCombinedMoves(currentNode);
			IMove[] combMoves;
			
			for(int i=0; i<allMoves.size(); ++i) {
				// watch out for the endTime
				if(System.currentTimeMillis() >= endTime){
					//System.err.println("stop search because of time");
					return;
				}
				
				combMoves = allMoves.get(i);
				IGameNode next = game.getNextNode(currentNode, combMoves);
				//next.setPreserve(true);
				Integer foundDepth = visitedStates.get(next.getState());
				
				if(foundDepth == null || foundDepth > currentNode.getDepth()){
					queue.add(next);
				}
				children.add(next);
			}

		}
		if(System.currentTimeMillis() >= endTime){
			//System.err.println("stop search because of time");
			return;
		}
		if(queue.isEmpty()) searchFinished = true;
		//System.out.println("Search finished, values: "+values.size()+", visited "+nodesVisited+" nodes.");
	}
	
	/**
	 * Make value function for propagating values we found.
	 * Follows the strategy determined by zerosum and turntaking flags.
	 * @param node The node to start with
	 * @param minDepth The lower limit for tree depth
	 */
	private void makeValue(IGameNode node, int minDepth) throws InterruptedException {
		if(node == null || node.getDepth() < minDepth) {
			return;
		}
		
		// watch out for the endTime
		if(System.currentTimeMillis() >= endTime){
			//System.err.println("stop search because of time");
			return;
		}
		
		// regenerate node
		game.regenerateNode(node);
		
		// We already have a value for a terminal
		if(values.get(node.getState()) != null && node.isTerminal()){
			makeValue(node.getParent(), minDepth);
			return;
		}
		
		// If it is no terminal, we adjust the value.
		List<IGameNode> children = new LinkedList<IGameNode>();
		List<IMove[]> allMoves = game.getCombinedMoves(node);
		
		for(IMove[] move : allMoves)
			children.add(game.getNextNode(node, move));
		
		// Determine player whose turn it is, if possible.
		int player = 0;
		if(turntaking)
			player = whoseTurn(node);
		
		int[] value = new int[game.getRoleCount()];
		for(IGameNode child : children) {
			// watch out for the endTime
			if(System.currentTimeMillis() >= endTime){
				//System.err.println("stop search because of time");
				return;
			}
			
			// regenerate node
			game.regenerateNode(child);
			
			// if the value for one child is missing, we can't proceed
			if(values.get(child.getState()) == null) {
				return;
			}
			
			// propagate the child's value up to our current node.
			if(turntaking && zerosum) { // in this case we apply maxN.
				// in this case we maximize the value of the current player
				if(values.get(child.getState())[0][player] > value[player]) {
					value = values.get(child.getState())[0].clone();
				}
			} else { // not turntaking, which is bad. for now we assume the worst for us.
				/*
				 * if(zerosum) {
				 * 	useKonradsSimplexForMorePlayers();
				 * }
				 */
				if(value[0] == -1 || values.get(child.getState())[0][playerNumber] < value[playerNumber]) { // if nothing set yet, just set one value. otherwise minimize our score.
					value = values.get(child.getState())[0].clone();
				}
			}
		}
		
		if(value[0] != -1 && value[1] != -1){
			values.put(node.getState(), new int[][]{value, {0, PROP}});
			if(node.getParent() != null) {
				makeValue(node.getParent(), minDepth);
			}
		}
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
			if(moves[i].length > 1) return i;
		}
		return index;
	}

	@Override
	public IMove getMove(IGameNode arg0) {
		// first search for the time we have
		long realEndTime = System.currentTimeMillis() + match.getPlayTime()*1000 - 1200;
		queue.clear();
		//arg0.setPreserve(true);
		queue.add(arg0);
		try {
			if(!searchFinished) {
				endTime = realEndTime - match.getPlayTime()*450;
				while(System.currentTimeMillis() < endTime)
					simulateGame(arg0);
				endTime = realEndTime;
				DFS(arg0.getDepth());
			}
			// search finished or end of time, now we have to decide.
			PriorityQueue<IGameNode> childs = new PriorityQueue<IGameNode>(10, new MultiPlayerComparator(values, playerNumber));
			for(IMove[] combMove : game.getCombinedMoves(arg0)) {
				if(System.currentTimeMillis() > endTime) {
					// we have no time left, just return random move
					return game.getRandomMove(arg0)[playerNumber];
				}
				IGameNode next = game.getNextNode(arg0, combMove);
				
				// regenerate node
				game.regenerateNode(next);
				
				//System.out.println("Possible move: "+combMove[playerNumber]+", values "+aryToString(values.get(next.getState())));
				childs.add(next);
			}
			IGameNode best = childs.peek();
			if(best == null) { // we didn't find anything.. (actually not possible)
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
		
		int[] value;
		while(true) {
			// regenerate node
			game.regenerateNode(currentNode);
			if(System.currentTimeMillis() >= endTime) return;
			// try to prove that we have simultaneous moves
			int turns = 0;
			IMove[][] legalMoves = game.getLegalMoves(currentNode);
			for(int i=0; i<legalMoves.length; i++) {
				if(legalMoves[i].length > 1) turns++;
			}
			if(turntaking && turns > 1) {
				this.turntaking = false;
				//System.out.println("We found out that the game is not turn-taking!");
			}
			
			// game over?
			if(currentNode.isTerminal()) { 
				value = currentNode.getState().getGoalValues();
				// try to prove that we don't have zero sum games.
				int sum = 0;
				for(int i=0; i<value.length; i++) {
					sum += value[i];
				}
				if(zerosum && sum != 100) {
					this.zerosum = false;
					//System.out.println("We found out that the game is not zero-sum!");
				}
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
					tempVal[i] = (tempVal[i]*(newCount-1) + value[i])/newCount;
				}
				values.put(node.getState(), new int[][]{tempVal, {newCount, SIM}});
			}
		}
	}

	@Override
	public double getHeuristicValue(IGameNode arg0) {
		return heuristic.calculateHeuristic(arg0);
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
		this.queue.clear();
		this.queue = null;
		this.values.clear();
		this.values = null;
		this.visitedStates.clear();
		this.visitedStates = null;
		this.root = null;
	}
}
