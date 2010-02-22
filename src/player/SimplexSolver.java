
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
 * the output will be 2 vectors
 * if one value in this vector is > 1.0 {
 *        another value is exactly 1.0
 *        take the value with 1.0 and treat the other (> 1.0) like 0
 * }
 *
 * @author konrad
 */
public class SimplexSolver {

    private Integer pivotX;
    private Integer pivotY;
    private Float pivotVal;
    private LinkedList<Integer> vectorX;
    private LinkedList<Integer> vectorY;
    private LinkedList<Float> movesI;
    private LinkedList<Float> movesII;
    private List<Float> visitedPivot = new LinkedList<Float>();
    private List<Integer> visitedPivotX = new LinkedList<Integer>();
    private LinkedList<Boolean> dominatedX;
    private LinkedList<Boolean> dominatedY;

    public void test() {

        for (int i = 0; i < 1; i++) {

            System.out.println("\n|||||||||||| new problem |||||||||||||\n");

            LinkedList<LinkedList<Float>> problem = make2PlayerProblem(5, 4);

            System.out.println(problem);
            System.out.println("calculated value " + solve(problem));
        }
    }

    public LinkedList<LinkedList<Float>> make2PlayerProblem(int movesI, int movesII) {
        LinkedList<LinkedList<Float>> problem = new LinkedList<LinkedList<Float>>();

        for (int row = 0; row < movesI; row++) {
            LinkedList<Float> line = new LinkedList<Float>();
            /*	for (int column = 0; column < movesII; column++) {
            line.add(new Float(random.nextInt(101)));
            }
             */
            switch (row) {
                case 0:
                    line.add(10f);
                    line.add(10f);
                    line.add(9f);
                    line.add(2f);
                    break;
                case 1:
                    line.add(7f);
                    line.add(4f);
                    line.add(7f);
                    line.add(6f);
                    break;
                case 2:
                    line.add(6f);
                    line.add(6f);
                    line.add(9f);
                    line.add(4f);
                    break;
                case 3:
                    line.add(9f);
                    line.add(9f);
                    line.add(8f);
                    line.add(3f);
                    break;
                case 4:
                    line.add(8f);
                    line.add(5f);
                    line.add(8f);
                    line.add(7f);
                    break;
            }

            problem.add(line);
        }

        return problem;
    }

    public Float solve(LinkedList<LinkedList<Float>> problem) {
//		System.out.println(problem);

        // dont forget regenerateDimated at the end
        //           removeDominated(problem);
        //              System.out.println("remove dominated: "+problem);

        LinkedList<LinkedList<Float>> matrix = transpose(problem);
//		System.out.println("transposed: "+problem);
        matrix = preprocess(matrix);

//		System.out.println("preprocessed: "+problem);
//		System.out.println("vectorY " + vectorY + " vectorX " + vectorX);

        //clear for each problem
        visitedPivot.clear();

        while (!isSolved(matrix)) {
//			System.out.println("======= next step ===========");

            //set pivotX, pivotY, pivotValue
            choosePivot(matrix);
//			System.out.println("x: " + pivotX + " y: " + pivotY + " val: " + pivotVal);

            if (pivotVal == null) {
                return 0f;
            }

            switchVectors(matrix);
//			System.out.println("vectorY " + vectorY + " vectorX " + vectorX);
            matrix = oneStep(matrix);
//			System.out.println(matrix);
        }

        makeResult(matrix);
        System.out.println("playerI " + movesI + " playerII " + movesII);

        Float result = calcValue(problem);

        //           regenerateDominated(problem);
        //	System.out.println("playerI " + movesI + " playerII " + movesII);
        return result;
    }

    LinkedList<Float> getMoves(LinkedList<LinkedList<Float>> problem) {
        solve(problem);
        return movesI;
    }

    /**
     *
     * the value of the parent node
     *
     * @param problem
     * @return
     */
    private Float calcValue(LinkedList<LinkedList<Float>> problem) {
        Float value = 0f;
        for (int j = 0; j < movesI.size(); j++) {
            for (int k = 0; k < movesII.size(); k++) {
                if ((movesI.get(j) < 1f) && (movesII.get(k) < 1f)
                        && (movesI.get(j) >= 0f) && (movesII.get(k) >= 0f)) {
                    value += movesI.get(j) * movesII.get(k) * problem.get(k).get(j);
                }
            }
        }
        return value;
    }

    private void choosePivot(LinkedList<LinkedList<Float>> problem) {
        //choose column
        pivotX = null;

        LinkedList<Float> lastline = problem.get(problem.size() - 1);

        visitedPivotX.clear();

        while (pivotX == null || pivotY == null) {
//			System.out.println(visitedPivotX);

            Float min = null;

            for (int i = 0; i < (lastline.size() - 1); i++) {
                //                System.out.println(i+" contains "+visitedPivotX.contains(i));
                if (!visitedPivotX.contains(i) && (min == null || lastline.get(i) < min)) {
                    pivotX = i;
                    min = lastline.get(i);
                }
            }

//			System.out.println(pivotX);
            if (pivotX == null) {
                //cant solve the problem
                System.out.println("cant solve the problem " + problem);
                pivotVal = null;
                return;
            }

            min = null;
            pivotY = null;
            //each row
            for (int j = 0; j < (problem.size() - 1); j++) {
                Float valY = problem.get(j).get(problem.get(j).size() - 1) / (-problem.get(j).get(pivotX));
//				System.out.println("valY " + valY + " " + visitedPivot.contains(problem.get(j).get(pivotX)));
                if (!visitedPivot.contains(problem.get(j).get(pivotX)) && (valY > 0) && (min == null || valY < min)) {
                    pivotY = j;
                    min = valY;
                }
            }
            //TODO: what happened when pivotY is still null
            if (pivotY == null) {
                // choose another pivotX
                visitedPivotX.add(pivotX);
                pivotX = null;
            }
        }

        // calculate it
        pivotVal = problem.get(pivotY).get(pivotX);
        visitedPivot.add(pivotVal);
//		System.out.println("visited " + visitedPivot);
    }

    private boolean isSolved(LinkedList<LinkedList<Float>> problem) {

        LinkedList<Float> lastline = problem.get(problem.size() - 1);
        for (int i = 0; i < (lastline.size() - 1); i++) {
            if (lastline.get(i) < 0) {
                return false;
            }
        }
        return true;
    }

    private LinkedList<LinkedList<Float>> oneStep(LinkedList<LinkedList<Float>> problem) {
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

    private void switchVectors(LinkedList<LinkedList<Float>> problem) {
        Integer tmp;
        tmp = vectorX.get(pivotX);
        vectorX.set(pivotX, vectorY.get(pivotY));
        vectorY.set(pivotY, tmp);
    }

    private void makeResult(LinkedList<LinkedList<Float>> problem) {
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

    private LinkedList<LinkedList<Float>> transpose(LinkedList<LinkedList<Float>> problem) {
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
     * +1 to every value to avoid zero
     * -> a constant addition does not affect the optimization-values
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
                line.set(j, -1 * line.get(j) - 1);
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

    private void removeDominated(LinkedList<LinkedList<Float>> problem) {
        dominatedX = new LinkedList<Boolean>();
        dominatedY = new LinkedList<Boolean>();
        for (int row = 0; row < problem.size(); row++) {
            dominatedX.add(false);
        }
        for (int column = 0; column < problem.get(0).size(); column++) {
            dominatedY.add(false);
        }


        //repeat as long as something changes
        Boolean somethingChanged = true;
        while (somethingChanged) {
            somethingChanged = false;
            // check each row
            // if all values are <= all values from another row -> it is dominated
            for (int row = 0; row < problem.size(); row++) {
                for (int otherRow = 0; otherRow < problem.size(); otherRow++) {
                    Boolean isDominated = true;
                    if (row != otherRow) {
                        for (int column = 0; column < problem.get(0).size(); column++) {
                            if (problem.get(row).get(column) > problem.get(otherRow).get(column)) {
                                isDominated = false;
                                break;
                            }
                        }
                    } else {
                        // dont be dominated by itself
                        isDominated = false;
                    }
                    if (isDominated == true) {
                        // remove this row
                        problem.remove(row);
                        somethingChanged = true;
                        //                       System.out.println("removeDominated: "+problem);
                        // calculate and save which row it is
                        // check whether something before row was already taken out
                        for (int count = 0; count <= row; count++) {
                            if (count < dominatedX.size() && dominatedX.get(count)) {
                                row++;
                            }
                        }
                        dominatedX.set(row, true);
                        //                     System.out.println("dominatedX "+dominatedX);
                        break;
                    }
                }
            }
            // now the same for columns
            for (int column = 0; column < problem.get(0).size(); column++) {
                for (int otherColumn = 0; otherColumn < problem.get(0).size(); otherColumn++) {
                    Boolean isDominated = true;
                    if (column != otherColumn) {
                        for (int row = 0; row < problem.size(); row++) {
                            // this time its dominated if all values are >=
                            if (problem.get(row).get(column) < problem.get(row).get(otherColumn)) {
                                isDominated = false;
                                break;
                            }
                        }
                    } else {
                        isDominated = false;
                    }
                    if (isDominated == true) {
                        somethingChanged = true;
                        // remove the column
                        for (int row = 0; row < problem.size(); row++) {
                            problem.get(row).remove(column);
                        }
                        //                    System.out.println("removeDominated: "+problem);
                        for (int count = 0; count <= column; count++) {
                            if (count < dominatedY.size() && dominatedY.get(count)) {
                                column++;
                            }
                        }
                        dominatedY.set(column, true);
                        //                      System.out.println("dominatedY "+dominatedY);
                        break;
                    }
                }
            }
        }
//        System.out.println("dominatedX "+dominatedX);
//        System.out.println("dominatedY "+dominatedY);


    }

    private void regenerateDominated(LinkedList<LinkedList<Float>> problem) {
        LinkedList<Float> modifiedMovesI = new LinkedList<Float>();
        LinkedList<Float> modifiedMovesII = new LinkedList<Float>();
        int count = 0;
        for (int row = 0; row < dominatedX.size(); row++) {
            if (dominatedX.get(row) == true) {
                modifiedMovesI.add(0f);
            } else {
                modifiedMovesI.add(movesI.get(count));
                count++;
            }
        }
        count = 0;
        for (int column = 0; column < dominatedY.size(); column++) {
            if (dominatedY.get(column) == true) {
                modifiedMovesII.add(0f);
            } else {
                modifiedMovesII.add(movesII.get(count));
                count++;
            }
        }
        movesI = modifiedMovesI;
        movesII = modifiedMovesII;
    }
}
