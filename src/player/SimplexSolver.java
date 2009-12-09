/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package player;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 *
 * ok, this is game theory
 * input as matrix -> the winvalues for player1 -> between 0-100
 * only zero-sum games
 * the out put will be 2 vectors
 * if one value in this vector is > 1.0 {
 *        another value is exactly 1.0
 *        take the value with 1.0 and treat the other (> 1.0) like 0
 * }
 *
 * @author konrad
 */
public class SimplexSolver {

	private static Integer pivotX;
	private static Integer pivotY;
	private static Float pivotVal;
	private static SimplexSolver solver;
	private static LinkedList<Integer> vectorX;
	private static LinkedList<Integer> vectorY;
	private static LinkedList<Float> movesI;
	private static LinkedList<Float> movesII;
	private	static Random random = new Random();
        private static List<Float> visitedPivot = new LinkedList<Float>();
        private static List<Integer> visitedPivotX = new LinkedList<Integer>();

	public static void main(String[] args) {

		solver = new SimplexSolver();


		for (int i = 0; i < 100; i++) {
                    
			System.out.println("\n|||||||||||| new problem |||||||||||||\n");

			LinkedList<LinkedList<Float>> problem = make2PlayerProblem(3, 3);

			solve(problem);
		}
	}

        public static LinkedList<LinkedList<Float>> make2PlayerProblem(int movesI, int movesII){
            LinkedList<LinkedList<Float>> problem = new LinkedList<LinkedList<Float>>();

            for(int row=0; row<movesI; row++){
                LinkedList<Float> line = new LinkedList<Float>();
                for(int column=0; column<movesII; column++){
                    line.add(new Float(random.nextInt(101)));
                }
                problem.add(line);
            }

            return problem;
        }

	public static void solve(LinkedList<LinkedList<Float>> problem) {
		System.out.println(problem);

		problem = transpose(problem);
		System.out.println(problem);
		problem = solver.preprocess(problem);

		System.out.println(problem);
		System.out.println("vectorY " + vectorY + " vectorX " + vectorX);

                //clear for each problem
                visitedPivot.clear();

		while (!isSolved(problem)) {
			System.out.println("======= next step ===========");

			//set pivotX, pivotY, pivotValue
			choosePivot(problem);
			System.out.println("x: " + pivotX + " y: " + pivotY + " val: " + pivotVal);

			switchVectors(problem);
			System.out.println("vectorY " + vectorY + " vectorX " + vectorX);

			problem = oneStep(problem);
			System.out.println(problem);
		}

		makeResult(problem);
		System.out.println("playerI " + movesI + " playerII " + movesII);
	}

	private static void choosePivot(LinkedList<LinkedList<Float>> problem) {
		//choose column
		pivotX = null;

		LinkedList<Float> lastline = problem.get(problem.size() - 1);

                visitedPivotX.clear();

                while(pivotX == null || pivotY == null){
                    System.out.println(visitedPivotX);

                    Float min = null;
                    
                    for (int i = 0; i < (lastline.size() - 1); i++) {
        //                System.out.println(i+" contains "+visitedPivotX.contains(i));
                            if (!visitedPivotX.contains(i) && (min == null || lastline.get(i) < min)) {
                                    pivotX = i;
                                    min = lastline.get(i);
                            }
                    }

                    System.out.println(pivotX);

                    min = null;
                    pivotY = null;
                    //each row
                    for (int j = 0; j < (problem.size() - 1); j++) {
                            Float valY = problem.get(j).get(problem.get(j).size() - 1) / (-problem.get(j).get(pivotX));
                            System.out.println("valY "+valY+" "+visitedPivot.contains(problem.get(j).get(pivotX)));
                            if (!visitedPivot.contains(problem.get(j).get(pivotX)) && (valY>0) && (min == null || valY < min)) {
                                    pivotY = j;
                                    min = valY;
                            }
                    }
                    //TODO: what happened when pivotY is still null
                    if(pivotY == null){
                        // choose another pivotX
                        visitedPivotX.add(pivotX);
                        pivotX = null;
                    }
                }

		// calculate it
		pivotVal = problem.get(pivotY).get(pivotX);
                visitedPivot.add(pivotVal);
                System.out.println("visited "+visitedPivot);
	}

	private static boolean isSolved(LinkedList<LinkedList<Float>> problem) {

		LinkedList<Float> lastline = problem.get(problem.size() - 1);
		for (int i = 0; i < (lastline.size() - 1); i++) {
			if (lastline.get(i) < 0) {
				return false;
			}
		}
		return true;
	}

	private static LinkedList<LinkedList<Float>> oneStep(LinkedList<LinkedList<Float>> problem) {
		LinkedList<LinkedList<Float>> result = new LinkedList<LinkedList<Float>>();
		//each row
		for (int i = 0; i < problem.size(); i++) {
			LinkedList<Float> line = problem.get(i);
			LinkedList<Float> newLine = new LinkedList<Float>();

			//each element
			for (int j = 0; j < line.size(); j++) {
				Float element = line.get(j);
				//check which rule to apply

				// same column as pivot
				if (j == pivotX) {
					//is it the pivot ?
					if (i == pivotY) {
						// PE -> 1/
						newLine.add(1 / element);
					} else {
						// PS -> x/PE
						newLine.add(element / pivotVal);
					}
				} else {
					if (i == pivotY) {
						//PZ -> x/-PE
						newLine.add(-1 * element / pivotVal);
					} else {
						// rectangle rule
						newLine.add(element - (line.get(pivotX) * problem.get(pivotY).get(j) / pivotVal));
					}
				}
			}
			result.add(newLine);
		}
		return result;
	}

	private static void switchVectors(LinkedList<LinkedList<Float>> problem) {
		Integer tmp;
		tmp = vectorX.get(pivotX);
		vectorX.set(pivotX, vectorY.get(pivotY));
		vectorY.set(pivotY, tmp);
	}

	private static void makeResult(LinkedList<LinkedList<Float>> problem) {
		movesI = new LinkedList<Float>();
		movesII = new LinkedList<Float>();



		LinkedList<Float> lastline = problem.get(problem.size() - 1);


		//fill
		for (int i = 0; i < (problem.size() - 1); i++) {
			movesI.add(0f);
		}

		for (int i = 0; i < (lastline.size() - 1); i++) {
			movesII.add(0f);
		}




		Float gameValue = -lastline.get(lastline.size() - 1);
		for (int i = 0; i < (problem.size() - 1); i++) {
			Float value = problem.get(i).get(problem.get(i).size() - 1);
			//System.out.println(value/gameValue);
			if (vectorY.get(i) < 0) {
				//add to player I
				movesI.set(-vectorY.get(i) - 1, value / gameValue);
			} else {
				movesII.set(vectorY.get(i) - 1, value / gameValue);
			}
		}
		for (int j = 0; j < (lastline.size() - 1); j++) {
			if (vectorX.get(j) < 0) {
				//add to player I
				//System.out.println(vectorX.get(j));
				movesI.set(-vectorX.get(j) - 1, lastline.get(j) / gameValue);
			} else {
				movesII.set(vectorX.get(j) - 1, lastline.get(j) / gameValue);
			}
		}
	}

	private static LinkedList<LinkedList<Float>> transpose(LinkedList<LinkedList<Float>> problem) {
		LinkedList<LinkedList<Float>> result = new LinkedList<LinkedList<Float>>();

		//each column
		for (int column = 0; column < problem.get(0).size(); column++) {
			//go through all rows
			LinkedList<Float> newLine = new LinkedList<Float>();
			for (int row = 0; row < problem.size(); row++) {

				newLine.add(problem.get(row).get(column));
		//		System.err.println(row + " " + column+" "+problem.get(row).get(column)+" "+newLine);
			}
			result.add(newLine);
		}
		return result;
	}

	/**
	 * make the tableau complete
	 * also make 2 lines for the vectors
	 * -> player I is negative values
	 * -> player 2 is positive values
	 *
	 * @param problem
	 * @return
	 */
	private LinkedList<LinkedList<Float>> preprocess(LinkedList<LinkedList<Float>> problem) {

		Integer length = null;
		vectorX = new LinkedList<Integer>();
		vectorY = new LinkedList<Integer>();

		for (int i = 0; i < problem.size(); i++) {
			vectorY.add(-i - 1);
			LinkedList<Float> line = problem.get(i);

			for (int j = 0; j < line.size(); j++) {
//				System.err.println(line);
				//invert every element
				line.set(j, -1 * line.get(j));
				//only one time
				if (i == 0) {
					vectorX.add(j + 1);
				}
			}
			//set length for additional row
			if (length == null) {
				length = line.size();
			}
			// add the last column
			line.add(1f);
		}

		LinkedList line = new LinkedList<Float>();
		for (int i = 0; i < length; i++) {
			line.add(-1f);
		}
		line.add(0f);
		problem.add(line);

		return problem;
	}
}