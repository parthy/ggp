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
	private HashMap<IGameState, Integer> values = new HashMap<IGameState, Integer>();
	private HashMap<IGameState, HashMap<int[], Integer>> maxNValues = new HashMap<IGameState, HashMap<int[], Integer>>();
	
	private ArrayList<IGameNode> children = new ArrayList<IGameNode>();
	
	private int currentDepthLimit;
	private int nodesVisited;
	private int max=1;
	
	private IGameNode currentNode;
	
	private long endTime;
	private IGameNode node;
	
	public void initMatch(Match initMatch) {
		match = initMatch;
		game = initMatch.getGame();
		playerNumber = game.getRoleIndex(match.getRole());
		
		currentDepthLimit = 5;
		endTime = System.currentTimeMillis() + initMatch.getStartTime()*1000 - 1000;
		try {
			IGameNode root = game.getTree().getRootNode();
			IMove[][] allMoves = game.getLegalMoves(root);
			String role = initMatch.getRole();
			int index = game.getRoleIndex(role);
			IMove[] myMoves = allMoves[index];
			if(myMoves.length <= 1) max=0;
			// new approach: search the game for half the prep time. if we didn't succeed by then, use the remaining time
			// to simulate some matches.
			IDS();
			while(System.currentTimeMillis() < endTime) {
				simulateGame(root);
			}
			System.out.println("While simulating, I got "+maxNValues.size()+" values.");
		} catch (InterruptedException e) {}
	}
	
	public void IDS() throws InterruptedException {
		boolean flag = true;
		while(flag) {
			flag = false;
			
			System.err.println("currentDepth"+currentDepthLimit);
			System.err.println("Now: "+System.currentTimeMillis()+", endTime: "+endTime/2);
			System.err.println("Visited: "+nodesVisited);
			
			visitedStates.clear();
			queue.clear();
			
			currentNode = game.getTree().getRootNode();
			queue.add(currentNode);
			
			while(!queue.isEmpty() && System.currentTimeMillis() < endTime/2) {
				// get next element from queue
				currentNode = queue.remove(0);
				//nodesVisited++;
				//if(nodesVisited % 1000 == 0) System.out.println("visited: "+nodesVisited);
				// regenerate node, if necessary
				if(currentNode.getState() == null)
					try {
						game.regenerateNode(currentNode);
					} catch (InterruptedException e2) {}
				
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
    							Integer foundDepth = visitedStates.get(game.getNextNode(currentNode, combMoves).getState());
    							if(foundDepth == null || foundDepth > currentNode.getDepth()){
	    							//Node temp = new Node(game.getNextNode(currentNode, combMoves));
    								queue.add(0, game.getNextNode(currentNode, combMoves));
    								expanded = true;
    							}
    							children.add(game.getNextNode(currentNode, combMoves));
    						} catch (InterruptedException e) {}
    					}
					} catch (InterruptedException e1) {}
				}
				try {
					if(!currentNode.isTerminal() && game.getCombinedMoves(currentNode).size() > 0 && !expanded && currentNode.getDepth() >= currentDepthLimit )
						flag = true;
				} catch (InterruptedException e) {}
				//currentNode.setChildren(children);
				
				// check if we get a value for the values hash
				if(currentNode.isTerminal()){
					values.put(currentNode.getState(), game.getGoalValues(currentNode)[playerNumber]);
					makeValue(currentNode);
				}
				
				//if(children.size() > 0) gameTree.put(currentNode, -1);
			}
			if(System.currentTimeMillis() > endTime/2){
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
				values.put(node.getState(), game.getGoalValues(node)[playerNumber]);
				makeValue(node);
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
		if(node == null)
			return;
		//we already have a value
		if(values.get(node.getState()) != null){
			makeValue(node.getParent());
			return;
		}
		children.clear();
		List<IMove[]> allMoves = game.getCombinedMoves(node);
		for(IMove[] move : allMoves)
			children.add(game.getNextNode(node, move));
		int value = -1;
		for(IGameNode child : children) {
			if(child.getState() == null) {
				try {
					game.regenerateNode(child);
				} catch (InterruptedException e2) {}
			}
			if(values.get(child.getState()) == null) {
				return;
			}
			// should we maximize or minimize?
			if(max == 1) { // we are max player
				if(node.getDepth()%2 == 0) { // maximize
					if(values.get(child.getState()) > value)
						value = values.get(child.getState());
				} else { // minimize
					if(values.get(child.getState()) < value || value == -1)
						value = values.get(child.getState());
				}
			} else { // we are min player
				if(node.getDepth()%2 == 0) { // minimize
					if(values.get(child.getState()) < value || value == -1)
						value = values.get(child.getState());
				} else { // maximize
					if(values.get(child.getState()) > value)
						value = values.get(child.getState());
				}
			}
		}
		if(value != -1){
			values.put(node.getState(), value);
			makeValue(node.getParent());
		}
	}
	
	@Override
	public IMove getMove(IGameNode arg0) {
		IGameNode best = null;
		try {
			// simulate a bit more, from here.
			long end = System.currentTimeMillis() + match.getPlayTime()*1000 - 800;
			while(System.currentTimeMillis() < end) {
				simulateGame(arg0);
			}
			PriorityQueue<IGameNode> childs = new PriorityQueue<IGameNode>(10, new MoveComparator(this, match, true));
			for(IMove[] combMove : game.getCombinedMoves(arg0)) {
				System.out.print("Possible move: "+combMove[game.getRoleIndex(match.getRole())]);
				System.out.println("   Value: "+values.get(game.getNextNode(arg0, combMove).getState()));
				childs.add(game.getNextNode(arg0, combMove));
				//if((max == 1 && bestValue == 100) || (max == 0 && bestValue == 0)) break;
			}
			best = childs.peek();
			if(best == null) { // we didn't find anything.. (actually not possible)
				return game.getRandomMove(arg0)[game.getRoleIndex(match.getRole())];
			}
		} catch (InterruptedException e) {}
		
		return best.getMoves()[game.getRoleIndex(match.getRole())];
	}

	public HashMap<IGameState, Integer> getValues(){
		return values;
	}
	
	public HashMap<IGameState, HashMap<int[], Integer>> getSimulationValues() {
		return maxNValues;
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
	 * -- deprecated -- for now we just take a random move --
	 * for now we play according to our recently simulated values and possible search values
	 */
	private IMove[] getMoveForSimulation(IGameNode currentNode) throws InterruptedException {
		IGameNode best = null;
		try {
			PriorityQueue<IGameNode> childs = new PriorityQueue<IGameNode>(10, new MoveComparator(this, match, true));
			for(IMove[] combMove : game.getCombinedMoves(currentNode)) {
				childs.add(game.getNextNode(currentNode, combMove));
				//if((max == 1 && bestValue == 100) || (max == 0 && bestValue == 0)) break;
			}
			best = childs.peek();
			if(best == null) { // we didn't find anything.. (actually not possible)
				return game.getRandomMove(currentNode);
			}
		} catch (InterruptedException e) {}
		
		return best.getMoves();
	}
}
