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
 * input as matrix -> the winvalues for maxplayer -> between 0-100
 * rowplayer == maxplayer
 * columnplayer == minplayer
 * a b
 * c d
 * is list of lists:
 * [[a, b], [c, d]]
 *
 * only zero-sum games
 * the output will be 2 vectors
 * if one value in this vector is > 1.0 {
 * another value is exactly 1.0
 * take the value with 1.0 and treat the other (> 1.0) like 0
 * }
 *
 * @author konrad
 */
public class SimplexSolver {

    private final Boolean OUTPUT = false;
    private Integer pivotColumn;
    private Integer pivotRow;
    private Float pivotVal;
    private LinkedList<Integer> VectorColumn;
    private LinkedList<Integer> VectorRow;
    private LinkedList<Float> movesMax;
    private LinkedList<Float> movesMin;
    private List<Float> visitedPivot = new LinkedList<Float>();
    private List<Integer> visitedPivotColumns = new LinkedList<Integer>();
    private LinkedList<Boolean> dominatedX;
    private LinkedList<Boolean> dominatedY;
    private Float gameValue;

    public void test() {

        for (int i = 0; i < 1; i++) {

            if (OUTPUT) {
                System.out.println("\n|||||||||||| new problem |||||||||||||\n");
            }

            LinkedList<LinkedList<Float>> problem = make2PlayerProblem(3, 3);

            if (OUTPUT) {
                System.out.println(problem);
            }
            if (OUTPUT) {
                System.out.println("calculated value " + solve(problem));
            }
        }
    }

    private LinkedList<LinkedList<Float>> make2PlayerProblem(int movesMax, int movesMin) {
        LinkedList<LinkedList<Float>> problem = new LinkedList<LinkedList<Float>>();

        for (int row = 0; row < movesMax; row++) {
            LinkedList<Float> line = new LinkedList<Float>();
            /* for (int column = 0; column < movesMin; column++) {
            line.add(new Float(random.nextInt(101)));
            }
             */
            switch (row) {
                /*     case 0:
                line.add(4f);
                line.add(2f);
                line.add(80f);
                break;
                case 1:
                line.add(2f);
                line.add(3f);
                line.add(100f);
                break;
                case 2:
                line.add(5f);
                line.add(1f);
                line.add(75f);
                break;
                case 3:
                line.add(-12f);
                line.add(-8f);
                line.add(0f);
                break;
                 */ case 0:
                    line.add(5f);
                    line.add(3f);
                    line.add(7f);
                    break;
                case 1:
                    line.add(7f);
                    line.add(9f);
                    line.add(1f);
                    break;
                case 2:
                    line.add(10f);
                    line.add(6f);
                    line.add(2f);
                    break;
                /*  case 0:
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
                 */            }

            problem.add(line);
        }


        return problem;
    }

    public Float solve(LinkedList<LinkedList<Float>> problem) {
// if (OUTPUT) System.out.println(problem);

        // dont forget regenerateDimated at the end
        //      removeDominated(problem);
        // if (OUTPUT) System.out.println("remove dominated: "+problem);

        /*      LinkedList<LinkedList<Float>> matrix = transpose(problem);
        if (OUTPUT) {
        System.out.println("transposed: " + problem);
        }
         */
        LinkedList<LinkedList<Float>> matrix = copy(problem);

        preprocess(matrix);

        if (OUTPUT) {
            System.out.println("preprocessed: " + matrix);
        }
        if (OUTPUT) {
            System.out.println("VectorRow " + VectorRow + " VectorColumn " + VectorColumn);
        }

        //clear for each problem
        visitedPivot.clear();

        while (!isSolved(matrix)) {
            if (OUTPUT) {
                System.out.println("======= next step ===========");
            }

            //set pivotColumn, pivotRow, pivotValue
            choosePivot(matrix);
            if (OUTPUT) {
                System.out.println("pivotRow: " + pivotRow + " pivotColumn: " + pivotColumn + " val: " + pivotVal);
            }

            if (pivotVal == null) {
                return 0f;
            }

            switchVectors(matrix);
            if (OUTPUT) {
                System.out.println("VectorRow " + VectorRow + " VectorColumn " + VectorColumn);
            }
            matrix = oneStep(matrix);
            if (OUTPUT) {
                System.out.println(matrix);
            }
        }

        makeResult(matrix);
        if (OUTPUT) {
            System.out.println("movesMin " + movesMin+" movesMax " + movesMax);
        }

        /*              regenerateDominated(problem);
        if (OUTPUT) {
        System.out.println("regenerated playerI " + movesMax + " playerII " + movesMin);
        }
         */
        return 1/gameValue;
    }

    LinkedList<Float> getMoves(LinkedList<LinkedList<Float>> problem) {
        solve(problem);
        return movesMax;
    }

    private void choosePivot(LinkedList<LinkedList<Float>> problem) {
        //choose column
        pivotColumn = null;

        LinkedList<Float> lastline = problem.get(problem.size() - 1);

        visitedPivotColumns.clear();

        while (pivotColumn == null || pivotRow == null) {
// if (OUTPUT) System.out.println(visitedPivotColumns);

            Float min = null;

            for (int i = 0; i < (lastline.size() - 1); i++) {
                // if (OUTPUT) System.out.println(i+" contains "+visitedPivotColumns.contains(i));
                if (!visitedPivotColumns.contains(i) && (min == null || lastline.get(i) < min)) {
                    pivotColumn = i;
                    min = lastline.get(i);
                }
            }

// if (OUTPUT) System.out.println(pivotColumn);

            if (pivotColumn == null) {
                //cant solve the problem
                if (OUTPUT) {
                    System.out.println("cant solve the problem " + problem);
                }
                pivotVal = null;
                return;
            }

            min = null;
            pivotRow = null;
            //each row
            for (int j = 0; j < (problem.size() - 1); j++) {
                // valY = b*_j / a*_jl
                Float valY = problem.get(j).get(/*length*/problem.get(j).size() - 1) / (problem.get(j).get(pivotColumn));

                if (!visitedPivot.contains(problem.get(j).get(pivotColumn))
                        && (problem.get(j).get(pivotColumn) > 0) && (min == null || valY < min)) {
                    // a*_jl must be greater 0
                    pivotRow = j;
                    min = valY;
                }
            }
            //TODO: what happened when pivotRow is still null
            if (pivotRow == null) {
                // choose another pivotColumn
                visitedPivotColumns.add(pivotColumn);
                pivotColumn = null;
            }
        }

        // calculate it
        pivotVal = problem.get(pivotRow).get(pivotColumn);
        if (pivotVal == null) {
            System.out.println("WARNING: SimplexSolver is exploding");
        }
        visitedPivot.add(pivotVal);
// if (OUTPUT) System.out.println("visited " + visitedPivot);
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
                if (j == pivotColumn) {
                    //is it the pivot ?
                    if (i == pivotRow) {
                        // PE -> -1/
                        newLine.add(1 / element);
                    } else {
                        // PS -> x/-PE
                        newLine.add(-1 * element / pivotVal);
                    }
                } else {
                    if (i == pivotRow) {
                        //PZ -> x/PE
                        newLine.add(element / pivotVal);
                    } else {
                        // rectangle rule
                        newLine.add(element - (line.get(pivotColumn) * problem.get(pivotRow).get(j) / pivotVal));
                    }
                }
            }
            result.add(newLine);
        }
        return result;
    }

    private void switchVectors(LinkedList<LinkedList<Float>> problem) {
        Integer tmp;
        tmp = VectorRow.get(pivotRow);
        VectorRow.set(pivotRow, VectorColumn.get(pivotColumn));
        VectorColumn.set(pivotColumn, tmp);
    }

    private void makeResult(LinkedList<LinkedList<Float>> problem) {
        movesMax = new LinkedList<Float>();
        movesMin = new LinkedList<Float>();



        LinkedList<Float> lastline = problem.get(problem.size() - 1);


        //fill
        for (int i = 0; i < (problem.size() - 1); i++) {
            movesMax.add(0f);
        }

        for (int i = 0; i < (lastline.size() - 1); i++) {
            movesMin.add(0f);
        }




        gameValue = lastline.get(lastline.size() - 1);
        if (OUTPUT) {
            System.out.println("Game Value: " + gameValue);
        }
        if (gameValue == null) {
            System.out.println("WARNING: null game in SimplexSolver");
        }
        // go through VectorRow
        for (int i = 0; i < (problem.size() - 1); i++) {
            Float value = problem.get(i).get(problem.get(i).size() - 1);
            if (VectorRow.get(i) > 0) {
                //add 0 to Min
                movesMin.set(VectorRow.get(i) - 1, 0f);
            } else {
                movesMax.set(-VectorRow.get(i) - 1, value / gameValue);
            }
        }
        // go through Column
        for (int j = 0; j < (lastline.size() - 1); j++) {
            if (VectorColumn.get(j) < 0) {
                // add 0 to Max
                movesMax.set(-VectorColumn.get(j) - 1, 0f);
            } else {
                movesMin.set(VectorColumn.get(j) - 1, lastline.get(j) / gameValue);
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
                // System.err.println(row + " " + column+" "+problem.get(row).get(column)+" "+newLine);
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
    private void preprocess(LinkedList<LinkedList<Float>> problem) {

        Integer length = null;
        VectorColumn = new LinkedList<Integer>();
        VectorRow = new LinkedList<Integer>();

        for (int i = 0; i < problem.size(); i++) {
            //max has positive values
            VectorRow.add(i + 1);
            LinkedList<Float> line = problem.get(i);

            //set length for additional row
            if (length == null) {
                length = line.size();
            }
            // add the last column
            line.add(1f);
        }

        LinkedList line = new LinkedList<Float>();
        for (int i = 0; i < length; i++) {
            //min has negative values
            VectorColumn.add(-i - 1);
            line.add(-1f);
        }
        line.add(0f);
        problem.add(line);
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
                            if (problem.get(row).get(column) < problem.get(otherRow).get(column)) {
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
                        // if (OUTPUT) System.out.println("removeDominated: "+problem);
                        // calculate and save which row it is
                        // check whether something before row was already taken out
                        for (int count = 0; count <= row; count++) {
                            if (count < dominatedX.size() && dominatedX.get(count)) {
                                row++;
                            }
                        }
                        dominatedX.set(row, true);
                        // if (OUTPUT) System.out.println("dominatedX "+dominatedX);
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
                            if (problem.get(row).get(column) > problem.get(row).get(otherColumn)) {
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
                        // if (OUTPUT) System.out.println("removeDominated: "+problem);
                        for (int count = 0; count <= column; count++) {
                            if (count < dominatedY.size() && dominatedY.get(count)) {
                                column++;
                            }
                        }
                        dominatedY.set(column, true);
                        // if (OUTPUT) System.out.println("dominatedY "+dominatedY);
                        break;
                    }
                }
            }
        }
// if (OUTPUT) System.out.println("dominatedX "+dominatedX);
// if (OUTPUT) System.out.println("dominatedY "+dominatedY);


    }

    private void regenerateDominated(LinkedList<LinkedList<Float>> problem) {
        LinkedList<Float> modifiedmovesMax = new LinkedList<Float>();
        LinkedList<Float> modifiedmovesMin = new LinkedList<Float>();
        int count = 0;
        for (int row = 0; row < dominatedX.size(); row++) {
            if (dominatedX.get(row) == true) {
                modifiedmovesMax.add(0f);
            } else {
                modifiedmovesMax.add(movesMax.get(count));
                count++;
            }
        }
        count = 0;
        for (int column = 0; column < dominatedY.size(); column++) {
            if (dominatedY.get(column) == true) {
                modifiedmovesMin.add(0f);
            } else {
                modifiedmovesMin.add(movesMin.get(count));
                count++;
            }
        }
        movesMax = modifiedmovesMax;
        movesMin = modifiedmovesMin;
    }

    private LinkedList<LinkedList<Float>> copy(LinkedList<LinkedList<Float>> problem) {
        LinkedList<LinkedList<Float>> matrix = new LinkedList<LinkedList<Float>>();
        for (int i = 0; i < problem.size(); i++) {
            LinkedList<Float> line = new LinkedList<Float>();
            for (int j = 0; j < problem.get(0).size(); j++) {
                line.add(problem.get(i).get(j));
            }
            matrix.add(line);
        }
        return matrix;
    }
}
