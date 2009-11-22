/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.tudresden.inf.ggp.basicplayer;

import org.eclipse.palamedes.gdl.core.model.IGame;
import org.eclipse.palamedes.gdl.core.model.IGameNode;
import org.eclipse.palamedes.gdl.core.model.IGameState;
import org.eclipse.palamedes.gdl.core.model.IMove;
import org.eclipse.palamedes.gdl.core.model.utils.Game;

/**
 *
 * @author konrad
 */
public class Node2 implements IGameNode, Comparable {

	private IGameNode wrapped;
	private int value;
	private IGame game;

	public Node2(IGame game, IGameNode wrappee) {
		this.game = game;
		this.wrapped = wrappee;
		this.value = -1;
	}

	public Node2(IGame game, IGameNode wrappee, int value) {
		this.game = game;
		this.wrapped = wrappee;
		this.value = value;
	}

	public IGameNode getWrapped() {
		return this.wrapped;
	}

	@Override
	public int getDepth() {
		return wrapped.getDepth();
	}

	@Override
	public IMove[] getMoves() {
		return wrapped.getMoves();
	}

	@Override
	public IGameNode getParent() {
		return wrapped.getParent();
	}

	@Override
	public IGameState getState() {
		try {
			game.getNextNode(wrapped.getParent(), wrapped.getMoves());
		} catch(Exception e) {}

		return wrapped.getState();
	}

	@Override
	public boolean isPreserved() {
		return wrapped.isPreserved();
	}

	@Override
	public boolean isTerminal() {
		return wrapped.isTerminal();
	}

	@Override
	public void setPreserve(boolean arg0) {
		wrapped.setPreserve(arg0);
	}

	@Override
	public void setState(IGameState arg0) {
		wrapped.setState(arg0);
	}

	public int getValue() {
		return this.value;
	}

	public void setValue(int arg0) {
		this.value = arg0;
	}

	public int compareTo(Object obj) {
		Node2 o = (Node2) obj;
		if(value < o.getValue()){
			return 1;
		} else {
			if(value == o.getValue()){
				return 0;
			} else {
				return -1;
			}
		}


	}

}