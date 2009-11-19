/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.tudresden.inf.ggp.basicplayer;

import java.util.TreeSet;

import org.eclipse.palamedes.gdl.core.model.IGameNode;
import org.eclipse.palamedes.gdl.core.model.IGameState;
import org.eclipse.palamedes.gdl.core.model.IMove;

/**
 *
 * @author konrad
 */
public class Node implements IGameNode, Comparable<Node> {

	private IGameNode wrapped;
	private TreeSet<Node> children;
	private int value;
	private int heuristic;

	public Node(IGameNode wrappee) {
		this.wrapped = wrappee;
		this.value = -1;
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
	
	public int getHeuristic() {
		return this.heuristic;
	}

	public void setHeuristic(int arg0) {
		this.heuristic = arg0;
	}
	
	public TreeSet<Node> getChildren() {
		return this.children;
	}

	public void setChildren(TreeSet<Node> children) {
		this.children = children;
	}

	@Override
	public int compareTo(Node arg0) {
		return (this.heuristic > arg0.heuristic) ? 1 : -1;
	}

}
