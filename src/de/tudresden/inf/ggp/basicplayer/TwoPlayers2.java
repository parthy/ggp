/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.tudresden.inf.ggp.basicplayer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import org.eclipse.palamedes.gdl.core.model.IGameNode;
import org.eclipse.palamedes.gdl.core.model.IMove;
import org.eclipse.palamedes.gdl.core.simulation.Match;
import org.eclipse.palamedes.gdl.core.simulation.strategies.AbstractStrategy;

/**
 *
 * @author konrad
 */
public class TwoPlayers2 extends AbstractStrategy {
	private List<IGameNode> queue = new LinkedList<IGameNode>();
	private Map<Integer, Integer> hash = new HashMap<Integer, Integer>();
	//to save the value for each Node
	private Map<Integer, Integer> values = new HashMap<Integer, Integer>();

	private long endTime;
	private int currentDepthLimit;
	private int expandedNodes;
	private long start;
	private boolean gotFirstMove;

	@Override
	public void initMatch(Match initMatch) {
		super.initMatch(initMatch);


		// initiate solution search
		hash.clear();
		game = initMatch.getGame();
		match = initMatch;
		endTime = System.currentTimeMillis() + initMatch.getStartTime()*1000 - 2000L;
		try {
			whoIsStarting(game.getTree().getRootNode());
			IDS(game.getTree().getRootNode(), 1);
		} catch (InterruptedException ex) {
		}
		System.err.println(" we are player "+game.getRoleNames()[playerNumber]);
		System.err.println("did we get the first move ? "+gotFirstMove);
		hash.clear();
		//if(foundSolution) fillCurrentWay();
	}

	@Override
	public IMove getMove(IGameNode currentNode) {
		try {
			currentNode = game.getNextNode(currentNode.getParent(), currentNode.getMoves());
		} catch (Exception e2) {}
		IMove lastOption = null;
		//calc last option if we get interrupted (shouldnt happen)
		try {
			lastOption = game.getRandomMove(currentNode)[playerNumber];
		} catch (InterruptedException ex) {
	//		Logger.getLogger(TwoPlayers2.class.getName()).info(Level.SEVERE, null, ex);
		}

		try {
			/**
			 * simulate the game for a few seconds...
			 */
			endTime = System.currentTimeMillis() + match.getPlayTime()*1000-2000L;
			queue.clear();
			queue.add(currentNode);
			try {
				IDS(currentNode, 1+currentNode.getDepth());
			} catch (InterruptedException ex) {
			}

			/**
			 * calcuate all children and compare the values (choose highest)
			 */
			List<IMove[]> moves = game.getCombinedMoves(currentNode);
			PriorityQueue<Node2> children = new PriorityQueue<Node2>();
			for (IMove[] move : moves) {
				IGameNode child = game.getNextNode(currentNode, move);
				if(values.containsKey(child.getState().hashCode())){
					children.add(new Node2(game, child, values.get(child.getState().hashCode())));
					System.err.println("child "+Arrays.asList(child.getMoves())+" with value "+values.get(child.getState().hashCode()));
				} else {
					if(game.isTerminal(child)){
						children.add(new Node2(game, child, game.getGoalValues(child)[playerNumber]));
						System.err.println("child "+Arrays.asList(child.getMoves())+" with value "+game.getGoalValues(child)[playerNumber]);
					} else {
						children.add(new Node2(game, child));
						System.err.println("child "+Arrays.asList(child.getMoves())+" without value");
					}
				}
			}
			//choose the one with the higest value
			IGameNode child = children.peek().getWrapped();
			System.err.println("we took the child "+Arrays.asList(child.getMoves())+" with value "+children.peek().getValue());
			return child.getMoves()[playerNumber];
		} catch (InterruptedException ex) {
			return lastOption;
		}
	}

	void IDS(IGameNode startNode, int depthLimit) throws InterruptedException {
		queue.clear();
		queue.add(startNode);
		currentDepthLimit = depthLimit;
		expandedNodes = 0;
		start = System.currentTimeMillis();
		IGameNode node;
		hash.clear();

	//	Boolean foundSomethingNew = true;
		int oldSize = -1;

    	while(oldSize < hash.size()) {
			oldSize = hash.size();
			hash.clear();

			System.err.println("\ncurrentDepth: "+currentDepthLimit);
			//System.err.println("current size of hash: "+hash.size());
			System.err.println("current amount of known values "+values.size());
			System.err.println("expanded "+expandedNodes+" Nodes in "+(System.currentTimeMillis()-start)+" ms");
    		while(!queue.isEmpty() && (System.currentTimeMillis() < endTime)) {
    			node = queue.remove(0);

				try {
					node = game.getNextNode(node.getParent(), node.getMoves());
				} catch (Exception e2) {}

				hash.put(node.getState().hashCode(), /*currentDepthLimit-node.getDepth()*/ null );

				/**
				 * here comes the evaluation of the nodes
				 */
				if(game.isTerminal(node)){
					//put the values and propagate until nothing changes or root reached
					evaluateStates(node, game.getGoalValues(node)[playerNumber]);
				} else {

					if(node.getDepth() < this.currentDepthLimit) {
						expandedNodes++;
						// add successor nodes to queue
						List<IMove[]> moves = game.getCombinedMoves(node);
						for(IMove[] move: moves) {
							if((game==null) || (node==null)){
								System.err.println("WWWWWWWWWWWWWWW");
							}
							IGameNode newNode = game.getNextNode(node, move);
							if(!hash.containsKey(newNode.getState().hashCode())) {
								queue.add(newNode);
				//				foundSomethingNew = true;
							} else {
								if(values.containsKey(newNode.getState().hashCode())){
									evaluateStates(node, values.get(newNode.getState().hashCode()));
								}
							
				/*				if((currentDepthLimit-newNode.getDepth()) > hash.get(newNode.getState().hashCode())){
									queue.add(newNode);
								} else {
	//								foundSomethingNew = true;
								}	*/
							}	
						}
					}
				}
    		}
    		if(System.currentTimeMillis() >= endTime){
				System.err.println("break because of time");
				System.err.println("currentDepth: "+currentDepthLimit);
				System.err.println("current size of hash: "+hash.size());
				System.err.println("current amount of known values "+values.size());
				System.err.println("expanded "+expandedNodes+" Nodes in "+(System.currentTimeMillis()-start)+" seconds.");

				return;
			}
    		queue.clear();
    		queue.add( startNode);
    		currentDepthLimit++;
		//	hash.clear();
    	}
    }

	private void evaluateStates(IGameNode node, int value) throws InterruptedException {
					Boolean changedSomething = true;

					while(changedSomething && (node != null)){
						changedSomething = false;

						try {
							node = game.getNextNode(node.getParent(), node.getMoves());
						} catch (Exception e3) {}
						//if no value is known yet
						if(!values.containsKey(node.getState().hashCode())){
							values.put(node.getState().hashCode(), value);
							changedSomething = true;
						} else {
							int oldValue = values.get(node.getState().hashCode());

							//do we have to minimize or maximize the parent ?
							//if(gotFirstMove _XOR_ ((node.getDepth() % 2)==1) )
							if(gotFirstMove ^ ((node.getDepth() % 2)==1) ){
								//System.err.println("at level "+node.getDepth()+" do minimize");
								//we maximize
								if(value > oldValue){
									values.put(node.getState().hashCode(), value);
									changedSomething = true;
								}
							} else {
								//System.err.println("at level "+node.getDepth()+" do maximize");
								//minimize
								if(value < oldValue){
									values.put(node.getState().hashCode(), value);
									changedSomething = true;
								}
							}
						}
						node = node.getParent();
					}
				//evaluation finished
	}

	private void whoIsStarting(IGameNode node) throws InterruptedException {
		List<IMove[]> allMoves = game.getCombinedMoves(node);
		if(allMoves.size() == 1){
			//just one move possible, we cannot know who is the starter
			whoIsStarting(game.getNextNode(node, allMoves.remove(0)));
		} else {
			//someone has more than one move
			//save the first one
			IMove firstMove = allMoves.get(0)[playerNumber];
			gotFirstMove = false;
			for(IMove[] move : allMoves){
				//check if we have another possible move
				if(firstMove != move[playerNumber]){
					gotFirstMove = true;
				}
			}
		}
	}


}
