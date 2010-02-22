package player;
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

//package src.de.tudresden.inf.ggp.basicplayer;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

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

    private HashMap<String, Integer> visitedStates = new HashMap<String, Integer>();
    /**
     * Integer is code for:
     *	propagated_simulation_values < 0 < simulation values < int_max
     *  int_max are terminal goal values or propagated from them
     */
    private HashMap<String, ValuesEntry> values = new HashMap<String, ValuesEntry>();
    private int currentDepthLimit;
    private int nodesVisited;
    private int max = 1;
    private long endTime;
    private int enemyNumber;
    private SimplexSolver solver = new SimplexSolver();
    private long endSearchTime;
    // used for storing the
    private HashMap<String, Integer> propagatedValues;

    public void setFirstEndTime(long endTime) {
        this.endTime = endTime - 600;
    }

    @Override
    public void initMatch(Match initMatch) {
        match = initMatch;
        game = initMatch.getGame();
        playerNumber = game.getRoleIndex(match.getRole());
        enemyNumber = (playerNumber == 0) ? 1 : 0;

        currentDepthLimit = 11;
        endTime = System.currentTimeMillis() + initMatch.getStartTime() * 1000 - 1000;
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
            if (myMoves.length <= 1) {
                max = 0;
            } else if (allMoves[(index + 1) % 2].length > 1) { // obviously we have no turn taking game
                max = 2;
            }

            // for about half the prep time, we simulate the game tree to get some monte carlo heuristics
            // additionally, we refine our understanding of the game (zero-sum, turntaking, ...)
/*			while(System.currentTimeMillis() < endTime - match.getStartTime()*500) {
            simulateGame(root);
            }
            System.out.println("While simulating, I got "+values.size()+" values.");
            System.out.println("Max is set to: "+max+", we are playerIndex "+playerNumber+", thus the opponent: "+((playerNumber+1)%2));
             */
            // with the gained knowledge we start our search routine
            IDS(endTime, root);

        } catch (InterruptedException e) {
        }
    }

    /**
     *
     * @param endSearchTime
     * @param start
     * @return true if search finished, else false
     * @throws java.lang.InterruptedException
     */
    public boolean IDS(long endSearchTime, IGameNode start) throws InterruptedException {
        this.endSearchTime = endSearchTime;
        Boolean finishedSearch = false;

        propagatedValues = new HashMap<String, Integer>();

        try {
            boolean canSearchDeeper = true;
            while (canSearchDeeper) {

                System.out.println("currentDepth " + currentDepthLimit);
                System.out.println("Now: " + System.currentTimeMillis() + ", endTime: " + endSearchTime);
                System.out.println("Visited: " + nodesVisited);

                visitedStates.clear();
                canSearchDeeper = DLS(start, 0, Integer.MIN_VALUE, Integer.MAX_VALUE);
                currentDepthLimit++;
            }

            finishedSearch = true;
        } catch (InterruptedException e) {
            System.out.println("couldnt finish search because of time");
        }

        System.out.println("stopped search and visited " + nodesVisited + " nodes.");
        Integer peter = propagatedValues.get(start.getState().toString());
        System.out.println("found something " + peter);

        return finishedSearch;
    }

    /**
     *
     * also uses:
     *	- currentDepthLimit
     *	- endSearchTime
     *	-
     *	- evaluation()
     *	- visitedStates
     *
     *	values.get(start_node) == max_int if search finished
     *
     * @param node to expand
     * @param depth < depthLimit
     * @return true if it could expand further but depthLimit stopped it
     */
    private Boolean DLS(IGameNode node, int depth, Integer alpha, Integer beta) throws InterruptedException {
        game.regenerateNode(node);
        nodesVisited++;

        if (System.currentTimeMillis() >= endSearchTime) {
//			propagatedValues.put(node.getState().toString(), evaluateNode(node));
            // we dont need to evaluate the state, because good values for the decision
            // should be made in the last iteration
            // if we cant even make the iteration with depthlimit 1 we just suck
            throw new InterruptedException("interrupted by time");
        }

        if (depth >= currentDepthLimit) {
            // reached the fringe -> ask for a evaluation
            propagatedValues.put(node.getState().toString(), evaluateNode(node));
            // we can expand in the next iteration
            return true;
        }

        if (node.isTerminal()) {
            // can also save in constant hash
            values.put(node.getState().toString(), new ValuesEntry(game.getGoalValues(node), Integer.MAX_VALUE));
            propagatedValues.put(node.getState().toString(), evaluateNode(node));
            return false;
        }

        if (visitedStates.containsKey(node.getState().toString())) {
            Integer foundDepth = visitedStates.get(node.getState().toString());
            if (foundDepth <= depth) {
                // have already seen this and therefor evaluated it
                // there cant be the same state twice on one path
                return false;
            }
        }

        visitedStates.put(node.getState().toString(), node.getDepth() - 1);
        // recursion

        // do not work with global variables in a recursive function
        List<IGameNode> children = new LinkedList<IGameNode>();
        List<IMove[]> moves = game.getCombinedMoves(node);
        Boolean expandFurther = false;
        for (IMove[] move : moves) {
            IGameNode child = game.getNextNode(node, move);
            children.add(child);
            if (DLS(child, depth + 1, alpha, beta)) {
                expandFurther = true;
            }
            // MiniMax
            game.regenerateNode(node);
            game.regenerateNode(child);
            Integer val = propagatedValues.get(child.getState().toString());
            // contraint: (val != null) because of DLS(child, ... )

            // alpha: guaranteed value for max player
            //			-> the higher the better for max, worse for min
            //			-> start at positive INFINITY
            // berta: guarantedd value for min player
            //			-> the lower the better for min, worse for max
            //			-> start at negative INFINITY

            if (maximize(node)) {
                if (val >= beta) {
                    // val is greater than what the minimizing player gets in another branch
                    // -> min-player will not go into this branch
                    propagatedValues.put(node.getState().toString(), beta);
                    break;
                }
                if (val > alpha) {
                    alpha = val;
                }
                propagatedValues.put(node.getState().toString(), alpha);
            } else {
                //minimize
                if (val <= alpha) {
                    propagatedValues.put(node.getState().toString(), alpha);
                    break;
                }
                if (val < beta) {
                    beta = val;
                }
                propagatedValues.put(node.getState().toString(), beta);
            }
        }
        return expandFurther;
    }

    public Integer evaluateNode(IGameNode node) {
        Integer val1 = propagatedValues.get(node.getState().toString());
        if (val1 != null) {
            return val1;
        }
        ValuesEntry val = values.get(node.getState().toString());
        if (val != null) {
            // force the game into zero-sum
            // v1' = v1 - v2
            return val.getGoalArray()[playerNumber] - val.getGoalArray()[(playerNumber + 1) % 2];
        } else {
            return 0;
        }
    }

    /**
     * Helper function determining whether we have to maximize ourselves or the opponent.
     * @param depth
     * @return
     */
    private boolean maximize(IGameNode node) throws InterruptedException {
        game.regenerateNode(node);
        //	System.out.println("GUCKE HIER: "+node.getState().toString());
        if (game.getLegalMoves(node)[playerNumber].length > 1) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public IMove getMove(IGameNode current) {
        IGameNode best = null;
        try {
            game.regenerateNode(current);
            //if(!searchFinished) {
            // always try to search cause we could be in a branch we pruned
            endTime = System.currentTimeMillis() + match.getPlayTime() * 1000 - 2000;
            // a little simulation doesn't harm
            while (System.currentTimeMillis() < endTime - match.getPlayTime() * 600) {
                simulateGame(current);
            }
            // do not change the currentDepthLimit
            currentDepthLimit--;

            IDS(endTime, current);
            //if max = 2
            Boolean foundMove = true;
            if (max == 2) {
                //
                // get the probabilities from the simplexSolver
                // no turn-taking -> do this weird simplex-stuff
                IMove[] myMoves = game.getLegalMoves(current)[playerNumber];
                IMove[] enemyMoves = game.getLegalMoves(current)[enemyNumber];

                //init
                List<LinkedList<Float>> averageScore = new LinkedList<LinkedList<Float>>();
                for (int i = 0; i < myMoves.length; i++) {
                    averageScore.add(new LinkedList<Float>());
                }


                LinkedList<LinkedList<Float>> problem = new LinkedList<LinkedList<Float>>();

                // for each move the enemy can do
                for (int i = 0; i < enemyMoves.length; i++) {
                    LinkedList<Float> line = new LinkedList<Float>();
                    //for each move i enemy can do
                    for (int j = 0; j < myMoves.length; j++) {
                        // calculate the child
                        IMove[] move = {myMoves[j], enemyMoves[i]};
                        IGameNode child = game.getNextNode(current, move);
                        game.regenerateNode(child);

                        // if one child has no value
                        // -> let the normal getMove decide
                        Float value;
                        if (propagatedValues.get(child.getState().toString()) == null) {
                            foundMove = false;
                            value = 49f;
                        } else {
                            value = new Float(propagatedValues.get(child.getState().toString()));
                        }

                        /* This doesn't seem to be necessary anymore.

                        if(simulationValues.get(child.getState().toString()) == null){
                        value += 0.49f;
                        } else {
                        value += 0.01f * ((int[]) (simulationValues.get(child.getState().toString()).keySet().toArray()[0]))[playerNumber];
                        }*/

                        //generate the problem
                        if (foundMove == true) {
                            line.add(new Float(propagatedValues.get(child.getState().toString())));
                        }

                        averageScore.get(j).add(value);
                    }
                    problem.add(line);
                }
                if (foundMove == true) {
                    System.out.println("P: " + problem);
                    LinkedList<Float> moves = solver.getMoves(problem);

                    // choose the move randomly
                    int rand = random.nextInt(1000);
                    System.out.println("all moves: " + Arrays.asList(myMoves));
                    Float currentSpace = 0f;
                    IMove move = null;
                    for (int i = 0; i < moves.size(); i++) {
                        if ((moves.get(i) >= 0f) && moves.get(i) <= 1f) {
                            System.out.println("Possible Move: " + myMoves[i] + " when " + currentSpace + " <= " + rand + " < " + (currentSpace + moves.get(i) * 1000));
                            if (move == null && (currentSpace <= rand) && (rand < (currentSpace + moves.get(i) * 1000))) {
                                move = myMoves[i];
                            }
                            currentSpace += moves.get(i) * 1000;
                        } else {
                            System.out.println("Possible Move: " + myMoves[i] + " dont do this");
                        }
                    }

                    if ((move == null)) {
                        // should not happen
                        System.out.println("found no move while solving the problem");
                    } else {
                        return move;
                    }
                } else {
                    // get the best out of average score
                    IMove move = null;
                    Float maximum = null;
                    for (int i = 0; i < averageScore.size(); i++) {
                        LinkedList<Float> row = averageScore.get(i);
                        Float score = 0f;
                        for (Float value : row) {
                            score += value;
                        }
                        score = score / row.size();
                        if ((move == null) || (score > maximum)) {
                            move = myMoves[i];
                            maximum = score;
                        }
                        System.out.println("Possible move : " + myMoves[i] + "   Value: " + score);
                    }
                    return move;
                }
            }
            // max != 2 or (max==2) && wasnt able to calculate a move, hrhrhr
            System.out.println("best we can get: " + evaluateNode(current));
            PriorityQueue<IGameNode> childs = new PriorityQueue<IGameNode>(10, new MoveComparator(this));
            for (IMove[] combMove : game.getCombinedMoves(current)) {
                IGameNode next = game.getNextNode(current, combMove);
                game.regenerateNode(next);

                System.out.print("Possible move: " + combMove[playerNumber]);

                System.out.println("   Value: (" + propagatedValues.get(next.getState().toString()) + ")");
                childs.add(next);
            }
            best = childs.peek();

            if (best == null) { // we didn't find anything.. (actually not possible)
                return game.getRandomMove(current)[playerNumber];
            }
        } catch (InterruptedException e) {
        }

        return best.getMoves()[playerNumber];
    }

    /**
     * dont use this anymore
     * instead use the evaluate function
     * @return
    public HashMap<IGameState, ValuesEntry> getValues(){
    return values;
    }
     */
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
        while (true) {

            // game over?
            if (currentNode.isTerminal()) {
                value = currentNode.getState().getGoalValues();
                break;
            }
            game.regenerateNode(currentNode);

            // try to prove that we have simultaneous moves
            int turns = 0;
            IMove[][] legalMoves = game.getLegalMoves(currentNode);
            for (int i = 0; i < legalMoves.length; i++) {
                if (legalMoves[i].length > 1) {
                    turns++;
                }
            }
            if (max < 2 && turns > 1) {
                max = 2;
            }

            // choose a move
            currentNode = game.getNextNode(currentNode, game.getRandomMove(currentNode));
        }

        // since the game is over, we can now go all the way back and fiddle around with the goals
        ValuesEntry existingValue = values.get(currentNode.getState().toString());

        // if there is no value yet, we put it in
        if (existingValue == null) {
            ValuesEntry temp = new ValuesEntry(value, 1);
            values.put(currentNode.getState().toString(), temp);
        }
        // now we look at the parent of the goal state.
        IGameNode node = currentNode;
        while (node.getParent() != null && node.getParent().getDepth() >= start.getDepth()) {
            game.regenerateNode(node);
            node = node.getParent();
            ValuesEntry entry = values.get(node.getState().toString());
            if (entry == null) { // no value in there yet, so we just set the achieved goal value
                ValuesEntry temp = new ValuesEntry(value, 1);
                values.put(node.getState().toString(), temp);
            } else { // otherwise, we build the average of the existing value and the achieved value in this particular game
                Integer newCount = values.get(node.getState().toString()).getOccurences() + 1;
                int[] tempVal = values.get(node.getState().toString()).getGoalArray();
                for (int i = 0; i < value.length; i++) {
                    tempVal[i] = ((newCount - 1) * tempVal[i] + value[i]) / newCount;
                }
                values.put(node.getState().toString(), new ValuesEntry(tempVal, newCount));
            }
        }
    }

    /*
     * seemingly our destroy function
     */
    @Override
    public void dispose() {
        try {
            FileOutputStream out = new FileOutputStream("values");
            ObjectOutputStream oos = new ObjectOutputStream(out);
            oos.writeObject(values);
            oos.close();
        } catch (IOException ex) {
            Logger.getLogger(TwoPlayerStrategy.class.getName()).log(Level.SEVERE, null, ex);
        }

        this.values.clear();
        this.values = null;
        this.visitedStates.clear();
        this.visitedStates = null;
    }
}
