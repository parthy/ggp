package player;
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

//package src.de.tudresden.inf.ggp.basicplayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;

import org.eclipse.palamedes.gdl.core.model.IGameNode;
import org.eclipse.palamedes.gdl.core.model.IGameState;
import org.eclipse.palamedes.gdl.core.model.IMove;
import org.eclipse.palamedes.gdl.core.simulation.Match;
import org.eclipse.palamedes.gdl.core.simulation.strategies.AbstractStrategy;

/**
 *
 * @author konrad
 */
public class TwoPlayerStrategy extends AbstractStrategy {
	
	private LinkedList<IGameNode> queue = new LinkedList<IGameNode>();
	private HashMap<IGameState, Integer> visitedStates = new HashMap<IGameState, Integer>();
	private HashMap<IGameState, int[]> values = new HashMap<IGameState, int[]>();
	private HashMap<IGameState, HashMap<int[], Integer>> simulationValues = new HashMap<IGameState, HashMap<int[], Integer>>();
	
	private ArrayList<IGameNode> children = new ArrayList<IGameNode>();
	
	private int currentDepthLimit;
	private int nodesVisited;
	private int max=1;
	
	private IGameNode currentNode;
	
	private long endTime;
	private IGameNode node;
	
	private int[] mobilityStatistics = new int[]{0,0,0,0,0,0}; // we count the pro, con and even results for both ourselves and the opponent for mobility while simulating
	private int[] movesAtStart = new int[2];
	
	public static final int MOB_DISABLE_US = 1;
	public static final int MOB_ENABLE_US = 2;
	public static final int MOB_DISABLE_OPP = 4;
	public static final int MOB_ENABLE_OPP = 8;
	
	public static final int MOB_MOBILITY = 6;
	public static final int MOB_INVERSE = 9;
	public static final int MOB_MOBILITY_TEAM = 10;
	public static final int MOB_INV_MOB_TEAM = 5;
	
	public void initMatch(Match initMatch) {
		match = initMatch;
		game = initMatch.getGame();
		playerNumber = game.getRoleIndex(match.getRole());
		
		currentDepthLimit = 5;
		endTime = System.currentTimeMillis() + initMatch.getStartTime()*1000 - 1000;
		try {
			IGameNode root = game.getTree().getRootNode();
			root.setPreserve(true); // we set the node to preserve so the stupid game tree won't forget about the state anymore.
			IMove[][] allMoves = game.getLegalMoves(root);
			String role = initMatch.getRole();
			int index = game.getRoleIndex(role);
			IMove[] myMoves = allMoves[index];
			if(myMoves.length <= 1) max=0;
			else if(allMoves[(index+1)%2].length > 1) { // obviously we have no turn taking game
				max = 2;
			}
			movesAtStart[0] = myMoves.length;
			movesAtStart[1] = allMoves[(index+1)%2].length;
			
			// new approach: search the game for half the prep time. if we didn't succeed by then, use the remaining time
			// to simulate some matches.
			IDS(endTime-initMatch.getStartTime()*500, root, 0);
			while(System.currentTimeMillis() < endTime) {
				simulateGame(root);
			}
			System.out.println("While simulating, I got "+simulationValues.size()+" values.");
			System.out.println("Additionally, I found out the following for mobility: ("
					+mobilityStatistics[0]+", "+mobilityStatistics[3]+") pro, ("
					+mobilityStatistics[1]+", "+mobilityStatistics[4]+") con, ("
					+mobilityStatistics[2]+", "+mobilityStatistics[5]+") even.");
		} catch (InterruptedException e) {}
	}
	
	public void IDS(long endSearchTime, IGameNode start, int makeValueLimit) throws InterruptedException {
		boolean flag = true;
		while(flag) {
			flag = false;
			
			System.err.println("currentDepth"+currentDepthLimit);
			System.err.println("Now: "+System.currentTimeMillis()+", endTime: "+endSearchTime);
			System.err.println("Visited: "+nodesVisited);
			
			visitedStates.clear();
			queue.clear();
			
			queue.add(start);
			
			while(!queue.isEmpty() && System.currentTimeMillis() < endSearchTime) {
				// get next element from queue
				currentNode = queue.remove(0);
				//nodesVisited++;
				//if(nodesVisited % 1000 == 0) System.out.println("visited: "+nodesVisited);
				
				// check if we get a value for the values hash
				if(currentNode.isTerminal()){
					int[] value = game.getGoalValues(currentNode);
					int[] valueInput = new int[3];
					valueInput[0] = value[0];
					valueInput[1] = value[1];
					valueInput[2] = 0;
					values.put(currentNode.getState(), valueInput);
					makeValue(currentNode, makeValueLimit);
					continue;
				}
				makeValue(currentNode, makeValueLimit);
				// leave tracing infos
				visitedStates.put(currentNode.getState(), currentNode.getDepth()-1);
				
				children.clear();
				
				boolean expanded=false;
				// find out possible successors
				if(currentNode.getDepth() < currentDepthLimit) {
					try {
    					List<IMove[]> allMoves = game.getCombinedMoves(currentNode);
    					IMove[] combMoves;
    					
    					for(int i=0; i<allMoves.size(); ++i) {
    						try {
    							combMoves = allMoves.get(i);
    							IGameNode next = game.getNextNode(currentNode, combMoves);
    							next.setPreserve(true);
    							Integer foundDepth = visitedStates.get(next.getState());
    							
    							if(foundDepth == null || foundDepth > currentNode.getDepth()){
    								queue.add(0, next);
    								expanded = true;
    							}
    							children.add(next);
    						} catch (InterruptedException e) {}
    					}
					} catch (InterruptedException e1) {}
				}
				try {
					if(!currentNode.isTerminal() && game.getCombinedMoves(currentNode).size() > 0 && !expanded && currentNode.getDepth() >= currentDepthLimit )
						flag = true;
				} catch (InterruptedException e) {}
				//currentNode.setChildren(children);

				
				//if(children.size() > 0) gameTree.put(currentNode, -1);
			}
			if(System.currentTimeMillis() > endSearchTime){
				System.err.println("stop search because of time");
				return;
			}
			currentDepthLimit++;
		}
		System.out.println("Search finished, values: "+values.size());
		//buildStrategy();
	}

	public void DFS() throws InterruptedException{
	
		queue.add(game.getTree().getRootNode());
		
		while(!queue.isEmpty() && System.currentTimeMillis() < endTime) {
			node = queue.remove(0);
			//System.err.println(node.getDepth());
			nodesVisited++;

			// try to regenerate state information of the node
			game.regenerateNode(node);

			if(node.isTerminal()){
				values.put(node.getState(), game.getGoalValues(node));
				makeValue(node);
				continue;
			}


			// track our way of states so we don't visit states multiple times
			visitedStates.put(node.getState(), null);

			// add successor nodes to queue
			List<IMove[]> allMoves = game.getCombinedMoves(node);
			PriorityQueue<IGameNode> newNodes = new PriorityQueue<IGameNode>(10, new HeuristicComparator(this));
			for(IMove[] move : allMoves) {
				if(!visitedStates.containsKey(game.getNextNode(node, move).getState())){
					newNodes.add(game.getNextNode(node, move));
				} else {
					if(values.containsKey(game.getNextNode(node, move).getState())){
						makeValue(node);
					}
				}
			}
			while(!newNodes.isEmpty()){
				queue.add(0, newNodes.poll());
			}
		}
		if(System.currentTimeMillis() >= endTime)
			System.err.println("break because of time");
		else 
			System.err.println("we searched eveything");
		queue.clear();

	}

	public void makeValue(IGameNode node) throws InterruptedException{
		if(node == null) {
			return;
		}
		//we already have a value
		if(values.get(node.getState()) != null){
			makeValue(node.getParent());
			return;
		}
		children.clear();
		List<IMove[]> allMoves = game.getCombinedMoves(node);
		for(IMove[] move : allMoves)
			children.add(game.getNextNode(node, move));
		int[] value = new int[]{-1, -1, -1};
		for(IGameNode child : children) {
			child.setPreserve(true);
			if(values.get(child.getState()) == null) {
				return;
			}
			// should we maximize or minimize?
			if(max == 1) { // we are max player
				if(node.getDepth()%2 == 0 && values.get(child.getState())[playerNumber] > value[playerNumber]) { // maximize
						value[0] = values.get(child.getState())[0];
						value[1] = values.get(child.getState())[1];
						value[2] = values.get(child.getState())[2]+1;
				} else { // maximize opponent
					if(values.get(child.getState())[(playerNumber+1)%2] > value[(playerNumber+1)%2]) {
						value[0] = values.get(child.getState())[0];
						value[1] = values.get(child.getState())[1];
						value[2] = values.get(child.getState())[2]+1;
					}
				}
			} else if(max == 0) { // we are min player
				if(node.getDepth()%2 == 0) { // maximize opponent
					if(values.get(child.getState())[(playerNumber+1)%2] > value[(playerNumber+1)%2]) {
						value[0] = values.get(child.getState())[0];
						value[1] = values.get(child.getState())[1];
						value[2] = values.get(child.getState())[2]+1;
					}
				} else { // maximize
					if(values.get(child.getState())[playerNumber] > value[playerNumber]) {
						value[0] = values.get(child.getState())[0];
						value[1] = values.get(child.getState())[1];
						value[2] = values.get(child.getState())[2]+1;
					}
				}
			} else { // no turn taking -> no max-min. assume the opponent takes his best move after we moved
				// the above comment means: we take the best value of the opponent's best moves
			}
		}
		
		if(value[0] != -1 && value[1] != -1){
			values.put(node.getState(), value);
			if(node.getParent() != null) {
				node.getParent().setPreserve(true);
				makeValue(node.getParent());
			}
		}
	}
	/*
	 * Limited makeValue function for use in getMove
	 */
	public void makeValue(IGameNode node, int minDepth) throws InterruptedException{
		if(node == null || node.getDepth() < minDepth) {
			return;
		}
		//we already have a value
		if(values.get(node.getState()) != null){
			makeValue(node.getParent(), minDepth);
			return;
		}
		children.clear();
		List<IMove[]> allMoves = game.getCombinedMoves(node);
		for(IMove[] move : allMoves)
			children.add(game.getNextNode(node, move));
		int[] value = new int[]{-1, -1, -1};
		for(IGameNode child : children) {
			child.setPreserve(true);
			if(values.get(child.getState()) == null) {
				return;
			}
			// should we maximize or minimize?
			if(max == 1) { // we are max player
				if(node.getDepth()%2 == 0 && values.get(child.getState())[playerNumber] > value[playerNumber]) { // maximize
						value[0] = values.get(child.getState())[0];
						value[1] = values.get(child.getState())[1];
						value[2] = values.get(child.getState())[2]+1;
				} else { // maximize opponent
					if(values.get(child.getState())[(playerNumber+1)%2] > value[(playerNumber+1)%2]) {
						value[0] = values.get(child.getState())[0];
						value[1] = values.get(child.getState())[1];
						value[2] = values.get(child.getState())[2]+1;
					}
				}
			} else if(max == 0) { // we are min player
				if(node.getDepth()%2 == 0) { // maximize opponent
					if(values.get(child.getState())[(playerNumber+1)%2] > value[(playerNumber+1)%2]) {
						value[0] = values.get(child.getState())[0];
						value[1] = values.get(child.getState())[1];
						value[2] = values.get(child.getState())[2]+1;
					}
				} else { // maximize
					if(values.get(child.getState())[playerNumber] > value[playerNumber]) {
						value[0] = values.get(child.getState())[0];
						value[1] = values.get(child.getState())[1];
						value[2] = values.get(child.getState())[2]+1;
					}
				}
			} else { // no turn taking -> no max-min. assume the opponent takes his best move after we moved
				// the above comment means: we take the best value of the opponent's best moves
			}
		}
		
		if(value[0] != -1 && value[1] != -1){
			values.put(node.getState(), value);
			if(node.getParent() != null) {
				node.getParent().setPreserve(true);
				makeValue(node.getParent(), minDepth);
			}
		}
	}
	
	@Override
	public IMove getMove(IGameNode arg0) {
		IGameNode best = null;
		try {
			// search a bit more, from the node arg0.
			long end = System.currentTimeMillis() + match.getPlayTime()*1000 - 1000;
			currentDepthLimit = arg0.getDepth()+1;
			arg0.setPreserve(true);
			IDS(end, arg0, arg0.getDepth());
			PriorityQueue<IGameNode> childs = new PriorityQueue<IGameNode>(10, new MoveComparator(this, match, true));
			for(IMove[] combMove : game.getCombinedMoves(arg0)) {
				IGameNode next = game.getNextNode(arg0, combMove);
				next.setPreserve(true);
				System.out.print("Possible move: "+combMove[game.getRoleIndex(match.getRole())]);
				int simval = -2;
				int val[] = new int[]{-2, -2};
				try {
					simval = ((int[]) (simulationValues.get(next.getState()).keySet().toArray()[0]))[playerNumber];
				} catch(NullPointerException ex) {}
				val = values.get(next.getState());
				if(val == null) val = new int[]{-2, -2};
				System.out.println("   Value: (["+val[0]+","+val[1]+"], "+simval+")");
				childs.add(next);
				//if((max == 1 && bestValue == 100) || (max == 0 && bestValue == 0)) break;
			}
			best = childs.peek();
			if(best == null) { // we didn't find anything.. (actually not possible)
				return game.getRandomMove(arg0)[game.getRoleIndex(match.getRole())];
			}
		} catch (InterruptedException e) {}
		
		return best.getMoves()[game.getRoleIndex(match.getRole())];
	}

	public HashMap<IGameState, int[]> getValues(){
		return values;
	}
	
	public HashMap<IGameState, HashMap<int[], Integer>> getSimulationValues() {
		return simulationValues;
	}
	
	public int getPlayerNumber() {
		return playerNumber;
	}
	
	/*
	 * new method designed to play a game according to either random choose or some heuristic function
	 * while doing this, we already populate the values hash with some values
	 *  (goal -> goalValue, states before goal -> average goal value achieved from this state
	 */
	private void simulateGame(IGameNode start) throws InterruptedException {
		// first we just play a game
		IGameNode currentNode = start;
		currentNode.setPreserve(true);
		int[] value;
		while(true) {
			
			// game over?
			if(currentNode.isTerminal()) { 
				value = currentNode.getState().getGoalValues();
				System.out.println("Played a game and got score "+value[playerNumber]);
				break;
			}
			
			// choose a move
			currentNode = game.getNextNode(currentNode, getMoveForSimulation(currentNode));
			currentNode.setPreserve(true);
		}
		
		// since the game is over, we can now go all the way back and fiddle around with the goals
		HashMap<int[], Integer> existingValue = simulationValues.get(currentNode.getState());
		
		// check if mobility or inverse mobility would good to apply
		IMove[][] legals = game.getLegalMoves(currentNode.getParent());
		if(legals[playerNumber].length > movesAtStart[0] ^ value[playerNumber] < 50)
		    mobilityStatistics[0]++;
		else if(legals[playerNumber].length < movesAtStart[0] ^ value[playerNumber] >= 50)
			mobilityStatistics[1]++;
		else
			mobilityStatistics[2]++;
		
		if(legals[(playerNumber+1)%2].length < movesAtStart[1] ^ value[(playerNumber+1)%2] >= 50)
			mobilityStatistics[3]++;
		else if(legals[(playerNumber+1)%2].length > movesAtStart[1] ^ value[(playerNumber+1)%2] < 50)
			mobilityStatistics[4]++;
		else
			mobilityStatistics[5]++;
		//System.out.println("Existing Value in Hash: "+existingValue);
		
		// if there is no value yet, we put it in
		if(existingValue == null) {
			HashMap<int[], Integer> temp = new HashMap<int[], Integer>();
			temp.put(value, 1);
			simulationValues.put(currentNode.getState(), temp);
		}
		// now we look at the parent of the goal state.
		IGameNode node = currentNode;
		while(node.getParent() != null) {
			node = node.getParent();
			HashMap<int[], Integer> entry = simulationValues.get(node.getState());
			int[] tempVal = null;
			if(entry != null) {
				tempVal = (int[]) simulationValues.get(node.getState()).keySet().toArray()[0];
			}
			if(entry == null || tempVal == null) { // no value in there yet, so we just set the achieved goal value
				HashMap<int[], Integer> temp = new HashMap<int[], Integer>();
				temp.put(value, 1);
				simulationValues.put(node.getState(), temp);
			} else { // otherwise, we build the average of the existing value and the achieved value in this particular game
				Integer newCount = ((Integer) simulationValues.get(node.getState()).values().toArray()[0])+1;
				for(int i=0; i<value.length; i++) {
					tempVal[i] = ((newCount-1)*tempVal[i]+value[i])/newCount;
				}
				HashMap<int[], Integer> temp = new HashMap<int[], Integer>();
				temp.put(tempVal, newCount);
				simulationValues.put(node.getState(), temp);
			}
		}
	}
	
	/*
	 * helping function for game simulation: choose a move according to a heuristic
	 * for now we just take a random move
	 * 
	 */
	private IMove[] getMoveForSimulation(IGameNode currentNode) throws InterruptedException {
		return game.getRandomMove(currentNode);
		/*
		IGameNode best = null;
		try {
			PriorityQueue<IGameNode> childs = new PriorityQueue<IGameNode>(10, new MoveComparator(this, match, true));
			for(IMove[] combMove : game.getCombinedMoves(currentNode)) {
				childs.add(game.getNextNode(currentNode, combMove));
				//if((max == 1 && bestValue == 100) || (max == 0 && bestValue == 0)) break;
			}
			best = childs.peek();
			if(best == null) { // we didn't find anything.. (actually not possible)
				
			}
		} catch (InterruptedException e) {}
		
		return best.getMoves();*/
	}
	
	public int getMobilityVariant() {
		int sum=0;
		// if the pro's for one variant is overweighing the con's and even's, we can apply that one.
		// conversely, if the con's overweigh the pro's and the even's we can apply the opposite.
		
		if(mobilityStatistics[0] > (mobilityStatistics[1]+mobilityStatistics[2])) sum += MOB_ENABLE_US;
		if((mobilityStatistics[0]+mobilityStatistics[2]) < mobilityStatistics[1]) sum += MOB_DISABLE_US;
		if(mobilityStatistics[3] > (mobilityStatistics[4]+mobilityStatistics[5])) sum += MOB_ENABLE_OPP;
		if((mobilityStatistics[3]+mobilityStatistics[4]) < mobilityStatistics[5]) sum += MOB_DISABLE_OPP;
		
		return sum;
	}
}
