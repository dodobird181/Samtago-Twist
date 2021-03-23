package student_player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import boardgame.Move;
import pentago_twist.PentagoBoardState;
import pentago_twist.PentagoMove;

public class MyTools {
	
	/**
	 * FOR TESTING PURPOSES TODO: Delete this before submitting!
	 */
	public static void main(String[] args) {
		
	}
	
	private static final int MOVE_TIME_LIMIT = 1900; // 2 second time limit minus a buffer of 100 ms just in case ;)
	
	/**
	 * Performs a Monte Carlo Tree Search, starting from the given boardState.
	 * @param boardState is the board state to start the MCTS from.
	 * @return a Move determined to be the best move by the MCTS.
	 */
	public static Move MonteCarloTreeSearch(PentagoBoardState board) {
		int startTime = (int) System.currentTimeMillis();
		
		Tree<MCTSInfo> searchTree = new Tree<MCTSInfo>(new MCTSInfo(board));
		Node<MCTSInfo> currentNode = searchTree.rootNode;
		
		while((int) System.currentTimeMillis() - startTime < MOVE_TIME_LIMIT) {
			if (currentNode.isLeaf()) {
				
			}
			else { // Use the "Tree Policy" to navigate towards a leaf node.
				
				double maxUCB = 0;
				Node<MCTSInfo> maxNode = null;
				List<Node<MCTSInfo>> children = currentNode.getUnmodifiableChildren();
				
				// Iterate through the current node's children to calculate their UCBs
				for(Node<MCTSInfo> child : children) {
					
					// Calculate UCB
					float childWinRate = child.value().winCount / child.value().visitCount;
					int childVisitCount = child.value().visitCount;
					int currentVisitCount = currentNode.value().visitCount;
					double childUCB = childWinRate + Math.sqrt((2*Math.log(currentVisitCount)) / childVisitCount);
				
					// Replace max if a new highest UCB has been found
					if (maxUCB < childUCB) {
						maxUCB = childUCB;
						maxNode = child;
					}
				}
 			}
		}
		
		Move m = board.getRandomMove();
		return m;
	}
}

/**
 * Datatype of information to store inside each node for the MCTS.
 * @author Samuel Morris (dodobird)
 */
class MCTSInfo{
	public int visitCount = 0;
	public int winCount = 0;
	public PentagoBoardState board;
	
	public MCTSInfo(PentagoBoardState board) {
		this.board = board;
	}
}










/**
 * A simple implementation of a tree in Java.
 * @author Samuel Morris (dodobird)
 * @param <T> The datatype to store inside each tree node.
 */
class Tree<T>{

	public Node<T> rootNode;
	
	/**
	 * Construct a new tree by passing a value for the root node.
	 * @param rootValue is the value that the root of the tree will have
	 * when the tree is initilized.
	 */
	public Tree(T rootValue) {
		assert rootValue != null;
		this.rootNode = new Node<T>(rootValue, null);
	}
}

/**
 * Implementation of a tree node, to be used in conjunction with the Tree class above.
 * @author Samuel Morris (dodobird)
 * @param <T> The type of value contained inside the node.
 */
class Node<T>{
	
	private T value;// immutable value stored inside the tree node
	private Optional<Node<T>> parent;// immutable option for a parent node
	private ArrayList<Node<T>> children;// mutable list of children
	
	/**
	 * Construct a node without any children.
	 * @param value of the node.
	 * @param parent of the node.
	 */
	public Node(T value, Node<T> parent) {
		this.value = value;
		this.parent = Optional.ofNullable(parent);
		children = new ArrayList<Node<T>>();
	}
	
	/**
	 * @return the ArrayList of this node's children
	 */
	public List<Node<T>> getChildren() {
		return children;
	}

	/**
	 * Construct a node with an ARrayList of children.
	 * @param value of the node.
	 * @param parent of the node.
	 * @param children is an ArrayList of child nodes for this node.
	 */
	public Node(T value, Node<T> parent, ArrayList<Node<T>> children) {
		this(value, parent);
		this.children = children;
	}
	
	/**
	 * Add a new child node to this node's list of children. 
	 * The child's parent is set to be this node, and the child's children
	 * ArrayList is empty.
	 * 
	 * @param value of the child node.
	 */
	public void addChild(T value) {
		Node<T> child = new Node<T>(value, this);
		children.add(child);
	}
	
	/**
	 * Return true if this node's children ArrayList contains the given child.
	 * @param child is the node to check if it is the child of this node.
	 */
	public boolean hasChild(Node<T> child) {
		return children.contains(child);
	}
	
	/**
	 * Removes a child from this node's children ArrayList if 
	 * one exists matching the given child.
	 * @param child node to remove.
	 */
	public void removeChild(Node<T> child) {
		children.remove(child);
	}
	
	/**
	 * @return An unmodifiable list of this node's children.
	 */
	public List<Node<T>> getUnmodifiableChildren() {
		return Collections.unmodifiableList(children);
	}
	
	/**
	 * @return True if this node has at least one child.
	 */
	public boolean hasChildren() {
		return !children.isEmpty();
	}
	
	/**
	 * @return True if this node is a root node (i.e. has no parent).
	 */
	public boolean isRoot() {
		return parent.isEmpty();
	}
	
	/**
	 * @return True if this node has no children, false otherwise.
	 */
	public boolean isLeaf() {
		return !hasChildren();
	}
	
	/**
	 * @return The value of this node.
	 */
	public T value() {
		return value;
	}
	
	/**
	 * @return The parent of this node.
	 * @throws UnsupportedOperationException if this is a root node.
	 */
	public Node<T> parent(){
		return parent.orElseThrow(() -> 
		new UnsupportedOperationException(
				"Cannot access the parent of a root node!"
				+ "Please check .isRoot() before accessing."));
	}
	
	/**
	 * Nodes are considered equal if they contain the same value and have the same parents.
	 */
	@Override
	public boolean equals(Object o) {
		if (o == null) return false;
		if (!(o instanceof Node<?>)) return false;
		Node<?> n = (Node<?>)o;
		if (n.value == this.value && n.parent.equals(this.parent)) {
			return true;
		}
		return false;
	}
}


