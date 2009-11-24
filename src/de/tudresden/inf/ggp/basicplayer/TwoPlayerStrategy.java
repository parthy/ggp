/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.tudresden.inf.ggp.basicplayer;

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
	private LinkedList<Node> queue = new LinkedList<Node>();
	private HashMap<Integer, Integer> visitedStates = new HashMap<Integer, Integer>();
	private HashMap<Node, Integer> gameTree = new HashMap<Node, Integer>();
	private HashMap<Integer, Node> parents = new HashMap<Integer, Node>();
	private HashMap<Integer, Integer> values = new HashMap<Integer, Integer>();
	
	private ArrayList<Node> children = new ArrayList<Node>();
	
	private int currentDepthLimit;
	private int nodesVisited;
	private int max=1;
	
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
		currentDepthLimit = 9;
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
				//parents.put(currentNode.hashCode(), currentNode);
				
				if(currentNode.getState().isTerminal()) {
					//System.out.println("Found terminal state with value: "+currentNode.getState().getGoalValue(game.getRoleIndex(match.getRole())));
					currentNode.setValue(currentNode.getState().getGoalValue(game.getRoleIndex(match.getRole())));
					gameTree.put(currentNode, currentNode.getValue());
					values.put(currentNode.getState().hashCode(), currentNode.getValue());
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
    							temp.setParentNode(currentNode.hashCode());
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
				
				// check if we get a value for the values hash
				int value = -1;
				boolean insert = true;
				for(Node child : children) {
					if(values.get(child.getState().hashCode()) == null || values.get(child.getState().hashCode()) == -1) {
						insert = false;
						break;
					}
					// should we maximize or minimize?
					if(max == 1) { // we are max player
						if(currentNode.getDepth()%2 == 0) { // maximize
							if(values.get(child.getState().hashCode()) > value) value = values.get(child.getState().hashCode());
						} else { // minimize
							if(values.get(child.getState().hashCode()) < value || value == -1) value = values.get(child.getState().hashCode());
						}
					} else { // we are min player
						if(currentNode.getDepth()%2 == 0) { // minimize
							if(values.get(child.getState().hashCode()) < value || value == -1) value = values.get(child.getState().hashCode());
						} else { // maximize
							if(values.get(child.getState().hashCode()) > value) value = values.get(child.getState().hashCode());
						}
					}
				}
				if(value != -1 && insert) values.put(currentNode.getState().hashCode(), value);
				
				//if(children.size() > 0) gameTree.put(currentNode, -1);
				if(startNode == null) {
					startNode = currentNode;
					startNode.setChildren(currentNode.getChildren());
					System.out.println("");
				}
			}
			currentDepthLimit++;
		}
		System.out.println("Search finished, visited Nodes: "+nodesVisited+", visitedStates: "+visitedStates.size()+", values: "+values.size());
		buildStrategy();
	}
	
	public void buildStrategy() {
		// find possible moves in start node. if there's only one, it is likely that we are the min player.
		System.out.println("We assume we are "+((max==0) ? "min" : "max")+" player.");
		// fill in the last few values starting from startnode
		fillValues(game.getTree().getRootNode());
		System.out.println("Values: "+values.size());
	}
	
	public int fillValues(IGameNode node) {
		if(node.getState() == null) {
			try {
				game.regenerateNode(node);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if(values.get(node.getState().hashCode()) != null && values.get(node.getState().hashCode()) != -1) return values.get(node.getState().hashCode());
		int value = -1;
		try {
			for(IMove[] move : game.getCombinedMoves(node)) {
				IGameNode child = game.getNextNode(node, move);
				// should we maximize or minimize?
				int newValue = fillValues(child);
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
			values.put(node.getState().hashCode(), value);
		return value;
	}
	
	
	@Override
	public IMove getMove(IGameNode arg0) {
		// determine possible next states
		fillValues(arg0);
		IMove best = null; 
		try {
			int bestValue = -1;
			for(IMove[] combMove : game.getCombinedMoves(arg0)) {
				System.out.println("Possible move: "+combMove[game.getRoleIndex(match.getRole())]);
				System.out.println("Value: "+values.get(game.getNextNode(arg0, combMove).getState().hashCode()));
				if(best == null) { 
					best = combMove[game.getRoleIndex(match.getRole())];
					if(values.get(game.getNextNode(arg0, combMove).getState().hashCode()) != null) {
						bestValue = values.get(game.getNextNode(arg0, combMove).getState().hashCode());
						System.out.println("New best value: "+bestValue);
					}
					continue; 
				}
				if(values.get(game.getNextNode(arg0, combMove).getState().hashCode()) != null && values.get(game.getNextNode(arg0, combMove).getState().hashCode()) > bestValue) {
					best = combMove[game.getRoleIndex(match.getRole())];
					bestValue = values.get(game.getNextNode(arg0, combMove).getState().hashCode());
				}
				//if((max == 1 && bestValue == 100) || (max == 0 && bestValue == 0)) break;
			}
			if(best == null) { // we didn't find anything.. (actually not possible)
				return game.getRandomMove(arg0)[game.getRoleIndex(match.getRole())];
			}
		} catch (InterruptedException e) {}
		
		return best;
	}

}
