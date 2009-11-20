/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.tudresden.inf.ggp.basicplayer;

import java.util.ArrayList;
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
import org.omg.PortableInterceptor.SYSTEM_EXCEPTION;

/**
 *
 * @author konrad
 */
public class TwoPlayerStrategy extends AbstractStrategy {
	
	//private PriorityQueue<Node> queue = new PriorityQueue<Node>();
	private LinkedList<Node> queue = new LinkedList<Node>();
	private HashMap<Integer, Integer> visitedStates = new HashMap<Integer, Integer>();
	private ArrayList<Node> gameTree = new ArrayList<Node>();
	
	private ArrayList<Node> children = new ArrayList<Node>();
	
	private int currentDepthLimit;
	private int nodesVisited;
	private int max;
	
	private Node currentNode;
	private Node currentGameNode;
	private Node startNode=null;
	
	private long startTime;
	private long endTime;
	
	public void initMatch(Match initMatch) {
		match = initMatch;
		game = initMatch.getGame();
		currentNode = new Node(game.getTree().getRootNode());
		queue.add(0, currentNode);
		currentDepthLimit = 3;
		startTime = System.currentTimeMillis();
		endTime = System.currentTimeMillis() + initMatch.getStartTime()*1000 - 5000;
		try {
			IGameNode root = game.getTree().getRootNode();
			IMove[][] allMoves = game.getLegalMoves(root);
			String role = initMatch.getRole();
			int index = game.getRoleIndex(role);
			IMove[] myMoves = allMoves[index];
			if(myMoves.length <= 1) max=0;
		} catch (InterruptedException e) {}
		IDS();
		currentGameNode = startNode;
	}
	
	public void IDS() {
		boolean flag = true;
		while(flag) {
			flag = false;
			
			System.err.println("currentDepth"+currentDepthLimit);
			System.err.println("Now: "+System.currentTimeMillis()+", endTime: "+endTime);
			System.err.println("Visited: "+nodesVisited);
			System.err.println("GameTree: "+gameTree.size());
			
			visitedStates.clear();
			queue.clear();
			gameTree.clear();
			
			currentNode = new Node(game.getTree().getRootNode());
			currentNode.setHeuristic(0);
			queue.add(currentNode);
			
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
				
				if(currentNode.getState().isTerminal()) {
					//System.out.println("Found terminal state with value: "+currentNode.getState().getGoalValue(game.getRoleIndex(match.getRole())));
					currentNode.setValue(currentNode.getState().getGoalValue(game.getRoleIndex(match.getRole())));
					gameTree.add(currentNode);
					continue;
				}
				
				children.clear();
				
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
    							temp.setParentNode(currentNode);
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
				if(currentNode.getState().isTerminal() || children.size() > 0) gameTree.add(currentNode);
				if(startNode == null) 
					startNode = currentNode;
			}
			currentDepthLimit++;
		}
		System.out.println("Search finished, visited Nodes: "+nodesVisited);
		buildStrategy();
	}
	
	public void buildStrategy() {
		// find possible moves in start node. if there's only one, it is likely that we are the min player.
		System.out.println("We assume we are "+((max==0) ? "min" : "max")+" player.");
		setGoalValues();
	}
	
	public int setGoalValues() {
		// find all terminals in gameTree
		queue.clear();
		for(Node node : gameTree) {
			if(node.isTerminal()) {
				System.out.println("Terminal: "+node.getState().getGoalValue(game.getRoleIndex(match.getRole())));
				queue.add(node);
			}
		}
		while(!queue.isEmpty()) {
			Node current = queue.remove(0);
			if(current.getState() == null) {
				try {
					game.regenerateNode(current.getWrapped());
				} catch(InterruptedException ex) {}
			}
			// hooray, a start node
			if(current.getParent() == null) {
				System.out.println("We have a node with depth 0 and the queue has still size: "+queue.size());
				currentGameNode = current;
				continue;
			}
			
			// remove node from gameTree
			gameTree.remove(current);
			
			// adjust the parent's value
			if(current.getDepth()%2 == 1) { // we apply exactly what "max" tells us
				if(max == 0) { // we minimize the parent's value
					if(current.getParentNode().getValue() > current.getValue() || current.getParentNode().getValue() == -1)
						current.getParentNode().setValue(current.getValue());
				} else { // we maximize it
					if(current.getParentNode().getValue() < current.getValue() || current.getParentNode().getValue() == -1)
						current.getParentNode().setValue(current.getValue());
				}
			} else { // we do the opposite of max
				if(max == 0) { // we maximize the parent's value
					if(current.getParentNode().getValue() < current.getValue() || current.getParentNode().getValue() == -1)
						current.getParentNode().setValue(current.getValue());
				} else { // we minimize it
					if(current.getParentNode().getValue() > current.getValue() || current.getParentNode().getValue() == -1)
						current.getParentNode().setValue(current.getValue());
				}
			}
			
			// and add the parent to the queue, at the end, of course
			queue.add(current.getParentNode());
			
			// plus, write back the node to gameTree
			gameTree.add(current);

		}
		return 0;
	}
	
	@Override
	public IMove getMove(IGameNode arg0) {
		// find out our position using the gameTree
		if(arg0.getParent() == null) currentGameNode = startNode;
		else {
			// find child of currentGameNode such that the state matches arg0.getState().getFluents().hashCode()
			for(Node node : gameTree) {
				if(node.getState() == null) {
					int value = node.getValue();
					try {
						node = new Node(game.getNextNode(node.getWrapped().getParent(), node.getWrapped().getMoves()));
						node.setValue(value);
					} catch (InterruptedException e) {}
				}
				if(node.getState().hashCode() == arg0.getState().hashCode()) {
					currentGameNode = node;
					if(currentGameNode.getChildren().size() > 0 || currentGameNode.getState().isTerminal()) break;
				}
			}
		}
		System.out.println("Our position now: "+currentGameNode+", "+currentGameNode.getDepth());
		// find the "best" successor
		Node best = null;
		for(Node child : currentGameNode.getChildren()) {
			System.out.println("Zur Auswahl: "+child.getValue());
			if(best == null) best = child;
			if(best != null && child.getValue() > best.getValue()) best = child;
		}
		if(best == null) { // this shouldn't happen!
			System.err.println("We've been demanded a Move where we think none exists!");
			try {
				return game.getRandomMove(arg0)[playerNumber];
			} catch (InterruptedException e) {}
		}
		int move=0;
		for(int i=0; i<best.getMoves().length; i++) {
			//System.out.println("Move in Array: "+best.getMoves()[i].toString());
			if(best.getMoves()[i].getRole().equals(match.getRole())) {
				//System.out.println("Found move for role "+match.getRole()+" at index "+i);
				move = i;
				break;
			}
		}
		
		return best.getMoves()[move];
	}

}
