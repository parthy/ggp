package player;
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

//package src.de.tudresden.inf.ggp.basicplayer;

import java.util.ArrayList;
import java.util.Arrays;
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
	private HashMap<IGameState, HashMap<int[], Integer>> values = new HashMap<IGameState, HashMap<int[], Integer>>();
	
	private ArrayList<IGameNode> children = new ArrayList<IGameNode>();
	
	private int currentDepthLimit;
	private int nodesVisited;
	private int max=1;
	
	private IGameNode currentNode;
	
	private long endTime;
	private boolean searchFinished=false;
	
	private int enemyNumber;

	private SimplexSolver solver = new SimplexSolver();
	
	public void initMatch(Match initMatch) {
		match = initMatch;
		game = initMatch.getGame();
		playerNumber = game.getRoleIndex(match.getRole());
		enemyNumber = (playerNumber == 0) ? 1 : 0;
		
		currentDepthLimit = 1;
		endTime = System.currentTimeMillis() + initMatch.getStartTime()*1000 - 1000;
		try {
			// set the game root node
			IGameNode root = game.getTree().getRootNode();
			
			// find out some stuff about the moves in the root node
			IMove[][] allMoves = game.getLegalMoves(root);
			String role = initMatch.getRole();
			int index = game.getRoleIndex(role);
			IMove[] myMoves = allMoves[index];
			
			// before we know anything in more detail, we assume turn-taking zero-sum. 
			// thus we determine the active player in the root by the amount of possible moves
			if(myMoves.length <= 1) max=0;
			else if(allMoves[(index+1)%2].length > 1) { // obviously we have no turn taking game
				max = 2;
			}

			// for about half the prep time, we simulate the game tree to get some monte carlo heuristics
			// additionally, we refine our understanding of the game (zero-sum, turntaking, ...)
			while(System.currentTimeMillis() < endTime - match.getStartTime()*500) {
				simulateGame(root);
			}
			System.out.println("While simulating, I got "+values.size()+" values.");
			System.out.println("Max is set to: "+max+", we are playerIndex "+playerNumber+", thus the opponent: "+((playerNumber+1)%2));
			
			// with the gained knowledge we start our search routine
			searchFinished = IDS(endTime, root);
			
		} catch (InterruptedException e) {}
	}

	/**
	 *
	 * @param endSearchTime
	 * @param start
	 * @param makeValueLimit
	 * @return true if search finished, else false
	 * @throws java.lang.InterruptedException
	 */
	public boolean IDS(long endSearchTime, IGameNode start) throws InterruptedException {
		boolean flag = true;
		while(flag) {
			flag = false;
			
			System.out.println("currentDepth"+currentDepthLimit);
			System.out.println("Now: "+System.currentTimeMillis()+", endTime: "+endSearchTime);
			System.out.println("Visited: "+nodesVisited);
			
			visitedStates.clear();
			queue.clear();
			
			queue.add(start);
			
			while(!queue.isEmpty() && System.currentTimeMillis() < endSearchTime) {
				// get next element from queue
				currentNode = queue.remove(0);
				game.regenerateNode(currentNode);
				nodesVisited++;
				
				// check if we get a value for the values hash
				// this updates the value in the hash and sets the number of occurrences to MAX_VALUE.
				// if there are simulation values for this node, they will be overwritten.
				if(currentNode.isTerminal()) {
					int[] value = game.getGoalValues(currentNode);
					
					HashMap<int[], Integer> valueH = new HashMap<int[], Integer>();
					valueH.put(value, Integer.MAX_VALUE);
					
					values.put(currentNode.getState(), valueH);
					
					continue;
				}
			
				// leave tracing infos
				visitedStates.put(currentNode.getState(), currentNode.getDepth()-1);
				
				children.clear();
				
				// find out possible successors
				if(currentNode.getDepth() < currentDepthLimit) {
					List<IMove[]> allMoves = game.getCombinedMoves(currentNode);
					IMove[] combMoves;
					
					// fetch existing value in hash. this is gonna be updated now.
					HashMap<int[], Integer> tempValues = values.get(currentNode.getState());
					Integer occ = (Integer) tempValues.values().toArray()[0];
					
					// we now walk through all moves and calculate the successor nodes
					// while doing so, we automatically check for existence of values
					for(int i=0; i<allMoves.size(); ++i) {
						combMoves = allMoves.get(i);
						IGameNode next = game.getNextNode(currentNode, combMoves);
						game.regenerateNode(next);
						Integer foundDepth = visitedStates.get(next.getState());
						
						HashMap<int[], Integer> exValueH = values.get(next.getState()); 
						if(exValueH != null) {
							int[] exValues = (int[]) exValueH.keySet().toArray()[0];

							// we use our brand new updateValues function to do an encapsulated calculation (min/max/simplex/...)
							tempValues = updateValues(tempValues, exValues, currentNode.getDepth(), occ);
						}
						
						if(foundDepth == null || foundDepth > currentNode.getDepth()) {
							children.add(0, next);
						}
					}
					
					// if we could not calculate all successor values, we add the current node again to the queue
					// otherwise, we write the now completely calculated values in the hash
					values.put(currentNode.getState(), tempValues);
					children.add(currentNode);
					
					// and finally add all newly expanded nodes to the queue
					queue.addAll(0, children);
					
				} else if(game.getCombinedMoves(currentNode).size() > 0) {
					flag = true;
				}
			}
			if(System.currentTimeMillis() >= endSearchTime){
				System.out.println("stop search because of time, visited Nodes: "+nodesVisited);
				return false;
			}
			currentDepthLimit++;
		}
		System.out.println("Search finished, values: "+values.size());
		System.out.println("Expanded Nodes: "+nodesVisited);
		return true;
	}

	/**
	 * This function updates values of a game node according to the current policy.
	 * 
	 * @param exValues The values before the update
	 * @param newValues The values to update with
	 * @param depth The depth we are currently 
	 * @param occurrences The number of times we refined these values.
	 * @return The newly calculated values.
	 */
	private HashMap<int[], Integer> updateValues(HashMap<int[], Integer> exValues, int[] newValues, int depth, int occurrences) {
		int newOcc = (occurrences == Integer.MAX_VALUE) ? occurrences : occurrences+1;
		int[] exValuesInt = (int[]) exValues.keySet().toArray()[0];
		
		// we have actually three cases:
		// (1) zero-sum, simultaneous moves => simplex
		if(max == 2) {
			// TODO: Make simplex update the values. Is this actually possible this way?
			return exValues;
		}
		// (2) zero-sum, turn-taking, maximize us
		else if(maximize(depth)) {
			if(newValues[playerNumber] > exValuesInt[playerNumber])
				exValues.put(newValues, newOcc);
		}
		// (3) zero-sum, turn-taking, maximize opponent
		else if(!maximize(depth)) {
			if(newValues[enemyNumber] > exValuesInt[enemyNumber])
				exValues.put(newValues, newOcc);
		}
		
		return exValues;
	}

	/**
	 * Helper function determining whether we have to maximize ourselves or the opponent.
	 * @param depth
	 * @return
	 */
	private boolean maximize(int depth) {
		return (max < 2 && (max == 1 ^ depth%2 == 1));
	}

	@Override
	public IMove getMove(IGameNode current) {
		IGameNode best = null;
		try {
			game.regenerateNode(current);
			if(!searchFinished) {
				endTime = System.currentTimeMillis() + match.getPlayTime()*1000 - 2000;
				// a little simulation doesn't harm
				while(System.currentTimeMillis() < endTime - match.getPlayTime()*600)
					simulateGame(current);
				// search a bit more, from the node arg0.
				currentDepthLimit = current.getDepth()+1;
				
				IDS(endTime, current);
			}
			//if max = 2
			Boolean foundMove = true;
			if(max == 2){
				//
				// get the probabilities from the simplexSolver
				// no turn-taking -> do this weird simplex-stuff
				IMove[] myMoves = game.getLegalMoves(current)[playerNumber];
				IMove[] enemyMoves = game.getLegalMoves(current)[enemyNumber];

				//init
				List<LinkedList<Float>> averageScore = new LinkedList<LinkedList<Float>>();
				for(int i=0; i<myMoves.length; i++){
					averageScore.add(new LinkedList<Float>());
				}


				LinkedList<LinkedList<Float>> problem = new LinkedList<LinkedList<Float>>();

				// for each move the enemy can do
				for(int i=0; i<enemyMoves.length; i++){
					LinkedList<Float> line = new LinkedList<Float>();
					//for each move i enemy can do
					for(int j=0; j<myMoves.length; j++){
						// calculate the child
						IMove[] move = {myMoves[j], enemyMoves[i]};
						IGameNode child = game.getNextNode(current, move);
						game.regenerateNode(child);
						
						// if one child has no value
						// -> let the normal getMove decide
						Float value;
						if(values.get(child.getState()) == null){
							foundMove = false;
							value = 49f;
						} else {
							int val = ((int[]) (values.get(child.getState()).keySet().toArray()[0]))[playerNumber];
							value = new Float(val);
						}
						
						/* This doesn't seem to be necessary anymore.
						
						if(simulationValues.get(child.getState()) == null){
							value += 0.49f;
						} else {
							value += 0.01f * ((int[]) (simulationValues.get(child.getState()).keySet().toArray()[0]))[playerNumber];
						}*/
						
						//generate the problem
						if(foundMove == true){
							line.add(new Float(((int[]) (values.get(child.getState()).keySet().toArray()[0]))[playerNumber]));
						}

						averageScore.get(j).add(value);
					}
					problem.add(line);
				}
				if(foundMove == true){
					System.out.println("P: "+problem);
					LinkedList<Float> moves = solver.getMoves(problem);
					
					// choose the move randomly
					int rand = random.nextInt(1000);
					System.out.println("all moves: "+Arrays.asList(myMoves));
					Float currentSpace = 0f;
					IMove move = null;
					for(int i=0; i<moves.size(); i++){
						if((moves.get(i) >= 0f) && moves.get(i) <= 1f){
								System.out.println("Possible Move: "+myMoves[i]+" when "+currentSpace+" <= "+rand+" < "+(currentSpace+moves.get(i)*1000));
								if(move == null && (currentSpace <= rand) && (rand < (currentSpace+moves.get(i)*1000))){
									move = myMoves[i];
								}
								currentSpace += moves.get(i)*1000;
						} else {
								System.out.println("Possible Move: "+myMoves[i]+" dont do this");
						}
					}

					if((move == null)) {
						// should not happen
						System.out.println("found no move while solving the problem");
					} else {
						return move;
					}
				} else {
					// get the best out of average score
					IMove move = null;
					Float maximum = null;
					for(int i=0; i<averageScore.size(); i++){
						LinkedList<Float> row = averageScore.get(i);
						Float score =  0f;
						for(Float value : row){
							score += value;
						}
						score = score / row.size();
						if((move == null) || (score > maximum)){
							move = myMoves[i];
							maximum = score;
						}
						System.out.println("Possible move : "+myMoves[i]+"   Value: "+score);
					}
					return move;
				}
			}
			// max != 2 or (max==2) wasnt able to calculate a move, hrhrhr
			PriorityQueue<IGameNode> childs = new PriorityQueue<IGameNode>(10, new MoveComparator(this));
			for(IMove[] combMove : game.getCombinedMoves(current)) {
				IGameNode next = game.getNextNode(current, combMove);
				game.regenerateNode(next);
				
				System.out.print("Possible move: "+combMove[game.getRoleIndex(match.getRole())]);
				
				int[] val = (int[]) (values.get(next.getState()).keySet().toArray()[0]);
				if(val == null) val = new int[]{-2, -2};
				System.out.println("   Value: (["+val[0]+","+val[1]+"])");
				childs.add(next);
			}
			best = childs.peek();
			
			if(best == null) { // we didn't find anything.. (actually not possible)
				return game.getRandomMove(current)[playerNumber];
			}
		} catch (InterruptedException e) {}
		
		return best.getMoves()[playerNumber];
	}

	public HashMap<IGameState, HashMap<int[], Integer>> getValues(){
		return values;
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
		//currentNode.setPreserve(true);
		game.regenerateNode(currentNode);
		int[] value;
		while(true) {
			
			// game over?
			if(currentNode.isTerminal()) { 
				value = currentNode.getState().getGoalValues();
				break;
			}
			game.regenerateNode(currentNode);
			
			// try to prove that we have simultaneous moves
			int turns = 0;
			IMove[][] legalMoves = game.getLegalMoves(currentNode);
			for(int i=0; i<legalMoves.length; i++) {
				if(legalMoves[i].length > 1) turns++;
			}
			if(max < 2 && turns > 1) {
				max = 2;
			}
			
			// choose a move
			currentNode = game.getNextNode(currentNode, game.getRandomMove(currentNode));
		}
		
		// since the game is over, we can now go all the way back and fiddle around with the goals
		HashMap<int[], Integer> existingValue = values.get(currentNode.getState());
		
		// if there is no value yet, we put it in
		if(existingValue == null) {
			HashMap<int[], Integer> temp = new HashMap<int[], Integer>();
			temp.put(value, 1);
			values.put(currentNode.getState(), temp);
		}
		// now we look at the parent of the goal state.
		IGameNode node = currentNode;
		while(node.getParent() != null && node.getParent().getDepth() >= start.getDepth()) {
			game.regenerateNode(node);
			node = node.getParent();
			HashMap<int[], Integer> entry = values.get(node.getState());
			int[] tempVal = null;
			if(entry != null) {
				tempVal = (int[]) values.get(node.getState()).keySet().toArray()[0];
			}
			if(entry == null || tempVal == null) { // no value in there yet, so we just set the achieved goal value
				HashMap<int[], Integer> temp = new HashMap<int[], Integer>();
				temp.put(value, 1);
				values.put(node.getState(), temp);
			} else { // otherwise, we build the average of the existing value and the achieved value in this particular game
				Integer newCount = ((Integer) values.get(node.getState()).values().toArray()[0])+1;
				for(int i=0; i<value.length; i++) {
					tempVal[i] = ((newCount-1)*tempVal[i]+value[i])/newCount;
				}
				HashMap<int[], Integer> temp = new HashMap<int[], Integer>();
				temp.put(tempVal, newCount);
				values.put(node.getState(), temp);
			}
		}
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
		this.currentNode = null;
	}
}
