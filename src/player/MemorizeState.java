/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package player;

import org.eclipse.palamedes.gdl.core.model.IGameState;

/**
 *
 * @author s5677658
 */
public class MemorizeState {
    private IGameState state;
    private final int value;
    private int activity;

    public MemorizeState(IGameState state, int value){
        this.state = state;
        this.value = value;
        this.activity = 0;
    }

    public IGameState getState(){
        return state;
    }

    public int getValue(){
        return value;
    }

    public int getActivity(){
        return activity;
    }

    public void raiseActivity(){
        activity++;
    }

    public void dispose(){
        state = null;
    }

}
