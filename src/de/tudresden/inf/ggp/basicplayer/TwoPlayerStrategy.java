/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.tudresden.inf.ggp.basicplayer;

import java.util.HashMap;
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

	private PriorityQueue<Node> queue = new PriorityQueue<Node>();
	private HashMap<Integer, IGameState> visitedStates = new HashMap<Integer, IGameState>();
	
	private Set<Node> terminals = new TreeSet<Node>();
	
	private int currentDepthLimit;
	private int nodesVisited;
	
	private Node currentNode;
	
	private long endTime;
	
	public void initMatch(Match initMatch) {
		game = initMatch.getGame();
		currentNode = new Node(initMatch.getGame().getTree().getRootNode());
		currentNode.setHeuristic(0);
		queue.add(currentNode);
		currentDepthLimit = 1;
		endTime = System.currentTimeMillis() + initMatch.getStartTime()*1000 - 1000L;
		IDS();
		System.out.println("Terminals Size: "+terminals.size());
	}
	
	public void IDS() {
		boolean flag = true;
		while(flag) {
			flag = false;
			
			System.err.println("currentDepth"+currentDepthLimit);
			System.err.println("Now: "+System.currentTimeMillis()+", endTime: "+endTime);
			System.err.println("Terminals Size: "+terminals.size());
			
			while(!queue.isEmpty() && System.currentTimeMillis() < endTime) {
				// get next element from queue
				currentNode = queue.poll();
				nodesVisited++;
				
				// regenerate node, if necessary
				if(currentNode.getState() == null)
					try {
						game.regenerateNode(currentNode.getWrapped());
					} catch (InterruptedException e2) {}
				
				// leave tracing infos
				visitedStates.put(currentNode.getState().hashCode(), null);
				
				// if terminal, add to terminals and continue
				if(currentNode.isTerminal()) {
					terminals.add(currentNode);
					continue;
				}
				
				// find out possible successors
				if(currentNode.getDepth() < currentDepthLimit) {
					try {
    					List<IMove[]> allMoves = game.getCombinedMoves(currentNode.getWrapped());
    					IMove[] combMoves;
    					for(int i=0; i<allMoves.size(); ++i) {
    						try {
    							combMoves = allMoves.get(i);
    							if((!visitedStates.containsKey(game.getNextNode(currentNode.getWrapped(), combMoves).getState().hashCode()))){
    								Node temp = new Node(game.getNextNode(currentNode.getWrapped(), combMoves));
    								temp.setHeuristic(currentNode.getHeuristic()+1+i); // for now we use the order to realise depth-first
    								queue.add(temp);
    							}
    						} catch (InterruptedException e) {}
    					}
    				} catch (InterruptedException e1) {}
				} else {
					flag = true;
				}
				try {
					if(game.getCombinedMoves(currentNode.getWrapped()).size() > 0) flag = true;
				} catch (InterruptedException e) {}
			}
			currentDepthLimit++;
			visitedStates.clear();
			queue.clear();
			
			currentNode = new Node(game.getTree().getRootNode());
			currentNode.setHeuristic(0);
			queue.add(currentNode);
		}
	}
	
	@Override
	public IMove getMove(IGameNode arg0) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

}
