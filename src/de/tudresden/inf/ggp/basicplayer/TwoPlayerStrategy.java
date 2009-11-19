/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.tudresden.inf.ggp.basicplayer;

import java.util.HashMap;
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

/**
 *
 * @author konrad
 */
public class TwoPlayerStrategy extends AbstractStrategy {

	//private PriorityQueue<Node> queue = new PriorityQueue<Node>();
	private LinkedList<Node> queue = new LinkedList<Node>();
	private HashMap<Integer, Integer> visitedStates = new HashMap<Integer, Integer>();
	
	private HashMap<Integer, Node> gameNodes = new HashMap<Integer, Node>();
	
	private int currentDepthLimit;
	private int nodesVisited;
	
	private Node currentNode;
	private Node currentGameNode;
	private Node startNode;
	
	private long endTime;
	
	public void initMatch(Match initMatch) {
		game = initMatch.getGame();
		currentNode = new Node(initMatch.getGame().getTree().getRootNode());
		queue.add(0, currentNode);
		currentDepthLimit = 9;
		endTime = System.currentTimeMillis() + initMatch.getStartTime()*1000 - 1000L;
		IDS();
		System.out.println("Search finished, visited Nodes: "+nodesVisited);
		buildStrategy();
		currentGameNode = startNode;
	}
	
	public void IDS() {
		boolean flag = true;
		while(flag) {
			flag = false;
			
			System.err.println("currentDepth"+currentDepthLimit);
			System.err.println("Now: "+System.currentTimeMillis()+", endTime: "+endTime);
			System.err.println("Visited: "+nodesVisited);
			
			while(!queue.isEmpty() && System.currentTimeMillis() < endTime) {
				// get next element from queue
				currentNode = queue.remove(0);
				nodesVisited++;
				if(nodesVisited % 1000 == 0) System.out.println("visited: "+nodesVisited);
				// regenerate node, if necessary
				if(currentNode.getState() == null)
					try {
						game.regenerateNode(currentNode.getWrapped());
					} catch (InterruptedException e2) {}
				
				// leave tracing infos
				visitedStates.put(currentNode.getState().hashCode(), currentNode.getDepth()-1);
				
				TreeSet<Node> children = new TreeSet<Node>();
				
				boolean expanded=false;
				// find out possible successors
				if(currentNode.getDepth() < currentDepthLimit) {
					try {
    					List<IMove[]> allMoves = game.getCombinedMoves(currentNode.getWrapped());
    					IMove[] combMoves;
    					
    					for(int i=0; i<allMoves.size(); ++i) {
    						try {
    							combMoves = allMoves.get(i);
    							boolean doIt = false;
    							Integer foundDepth = visitedStates.get(game.getNextNode(currentNode.getWrapped(), combMoves).getState().hashCode());
    							if(foundDepth == null || foundDepth > currentNode.getDepth()) doIt = true;
    							Node temp = new Node(game.getNextNode(currentNode.getWrapped(), combMoves));
    							if(doIt){
    								queue.add(0, temp);
    								expanded = true;
    							}
    							children.add(temp);
    						} catch (InterruptedException e) {}
    					}
					} catch (InterruptedException e1) {}
				}
				try {
					if(!currentNode.isTerminal() && game.getCombinedMoves(currentNode.getWrapped()).size() > 0 && !expanded && currentNode.getDepth() >= currentDepthLimit ) flag = true;
				} catch (InterruptedException e) {}
				currentNode.setChildren(children);
				if(currentNode.getParent() == null) 
					startNode = currentNode;
			}
			currentDepthLimit++;
			visitedStates.clear();
			queue.clear();
			
			currentNode = new Node(game.getTree().getRootNode());
			currentNode.setHeuristic(0);
			queue.add(currentNode);
			gameNodes.put(currentNode.getWrapped().getState().getFluents().hashCode(), currentNode);
		}
	}
	
	public void buildStrategy() {
		int best = setGoalValues(startNode, 0);
		for(Node child : startNode.getChildren()) {
			System.out.println("Child with value "+child.getValue());
		}
	}
	
	public int setGoalValues(Node node, int max) {
		try {
                        game.regenerateNode(node.getWrapped());
                } catch (InterruptedException e) {}
                
		if(node.getChildren() == null || node.getChildren().isEmpty()) {
			node.setValue(node.getState().getGoalValue(playerNumber));			
			return node.getState().getGoalValue(playerNumber);
		}
		else {
			int value = (max == 0) ? 100 : 0;
			if(max == 0) {
				for(Node child : node.getChildren()) {
					int childValue = setGoalValues(child, 1);
					if(childValue < value && childValue != -1) value = childValue;
				}
			} else {
				for(Node child : node.getChildren()) {
					int childValue = setGoalValues(child, 0);
					if(childValue > value) value = childValue;
				}
			}
			node.setValue(value);
			return value;
		}
	}
	
	@Override
	public IMove getMove(IGameNode arg0) {
		// find out our position using the gameNodes hash
		if(arg0.getDepth() == 0) currentGameNode = startNode;
		else {
			// find child of currentGameNode such that the state matches arg0.getState().getFluents().hashCode()
			for(Node child : currentGameNode.getChildren()) {
				try {
					game.regenerateNode(child.getWrapped());
				} catch (InterruptedException e) {}
				
				if(child.getState().getFluents().hashCode() == arg0.getState().getFluents().hashCode()) {
					currentGameNode = child;
					System.out.println("Found successor node.");
					break;
				}
			}
		}
		System.out.println("Our position now: "+currentGameNode+", "+currentGameNode.getDepth());
		// find the "best" successor
		Node best = null;
		for(Node child : currentGameNode.getChildren()) {
			if(best == null) best = child;
			if(best != null && child.getValue() > best.getValue()) best = child;
		}
		if(best == null) { // this shouldn't happen!
			System.err.println("We've been demanded a Move where we think none exists!");
			try {
				return game.getRandomMove(arg0)[playerNumber];
			} catch (InterruptedException e) {}
		}
		return best.getMoves()[this.playerNumber];
	}

}
