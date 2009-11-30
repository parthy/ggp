package player;
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

//package src.de.tudresden.inf.ggp.basicplayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.palamedes.gdl.core.model.IGameNode;
import org.eclipse.palamedes.gdl.core.model.IGameState;
import org.eclipse.palamedes.gdl.core.model.IMove;
import org.eclipse.palamedes.gdl.core.simulation.Match;
import org.eclipse.palamedes.gdl.core.simulation.strategies.AbstractStrategy;
import org.omg.PortableInterceptor.SYSTEM_EXCEPTION;

/**
 *
 * @author konrad
 */
public class TwoPlayerStrategy extends AbstractStrategy {
	
	//private PriorityQueue<Node> queue = new PriorityQueue<Node>();
	private LinkedList<IGameNode> queue = new LinkedList<IGameNode>();
	private HashMap<IGameState, Integer> visitedStates = new HashMap<IGameState, Integer>();
	private HashMap<IGameState, Integer> values = new HashMap<IGameState, Integer>();
	
	private ArrayList<IGameNode> children = new ArrayList<IGameNode>();
	
	private int currentDepthLimit;
	private int nodesVisited;
	private int max=1;
	
	private IGameNode currentNode;
	
	private long startTime;
	private long endTime;
	private IGameNode node;
	
	public void initMatch(Match initMatch) {
		match = initMatch;
		game = initMatch.getGame();
		playerNumber = game.getRoleIndex(match.getRole());
		
		currentDepthLimit = 12;
		startTime = System.currentTimeMillis();
		endTime = System.currentTimeMillis() + initMatch.getStartTime()*1000 - 5000;
		try {
			IGameNode root = game.getTree().getRootNode();
			IMove[][] allMoves = game.getLegalMoves(root);
			String role = initMatch.getRole();
			int index = game.getRoleIndex(role);
			IMove[] myMoves = allMoves[index];
			if(myMoves.length <= 1) max=0;
			DFS();
		} catch (InterruptedException e) {}
	}
	
	public void IDS() throws InterruptedException {
		boolean flag = true;
		while(flag) {
			flag = false;
			
			System.err.println("currentDepth"+currentDepthLimit);
			System.err.println("Now: "+System.currentTimeMillis()+", endTime: "+endTime);
			System.err.println("Visited: "+nodesVisited);
			
			visitedStates.clear();
			queue.clear();
			
			currentNode = game.getTree().getRootNode();
			queue.add(currentNode);
			
			while(!queue.isEmpty() && System.currentTimeMillis() < endTime) {
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
			if(System.currentTimeMillis() > endTime){
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

	//not used anymore, work is done by makeValue
	public void buildStrategy() {
		// find possible moves in start node. if there's only one, it is likely that we are the min player.
		System.out.println("We assume we are "+((max==0) ? "min" : "max")+" player.");
		// fill in the last few values starting from startnode
		fillValues(game.getTree().getRootNode());
		System.out.println("Values: "+values.size());
	}

	//not used anymore, work is done by makeValue
	public int fillValues(IGameNode node) {
		if(node.getState() == null) {
			try {
				game.regenerateNode(node);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if(values.get(node.getState()) != null && values.get(node.getState()) != -1) return values.get(node.getState());
		int value = -1;
		try {
			for(IMove[] move : game.getCombinedMoves(node)) {
				IGameNode child = game.getNextNode(node, move);
				// should we maximize or minimize?
				int newValue = fillValues(child);
				//if one of the childs has no value -> we cant tell which value this node has
				if(newValue == -1){
					//do not write anything in the hash
					
				} else {
					if(max == 1) { // we are max player
						if(node.getDepth()%2 == 0) { // maximize
							if(newValue > value) value = newValue;
						} else { // minimize
							if(newValue < value || value == -1) value = newValue;
						}
					} else { // we are min player
						if(node.getDepth()%2 == 0) { // minimize
							if(newValue < value || value == -1) value = newValue;
						} else { // maximize
							if(newValue > value) value = newValue;
						}
					}
				}
			}
		} catch (InterruptedException e) {}
		if(node.getState() == null) {
			try {
				game.regenerateNode(node);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if(node.getState() != null)
			values.put(node.getState(), value);
		return value;
	}
	
	
	@Override
	public IMove getMove(IGameNode arg0) {
		// determine possible next states
		//fillValues(arg0);
		IGameNode best = null;
		try {
			PriorityQueue<IGameNode> childs = new PriorityQueue<IGameNode>(10, new MoveComparator(this, match));
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

}
