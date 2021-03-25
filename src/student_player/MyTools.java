package student_player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.sun.jdi.Value;

import boardgame.Move;
import pentago_twist.PentagoBoard;
import pentago_twist.PentagoBoardState;
import pentago_twist.PentagoMove;

public class MyTools {
	
	/**
	 * FOR TESTING PURPOSES TODO: Delete this before submitting!
	 */
	public static void main(String[] args) {
		Tree<Integer> tree = new Tree<Integer>(3);
		tree.rootNode.addChild(5);
		tree.rootNode.getChildren().get(0).addChild(6);
	}
	
	private static final int MOVE_TIME_LIMIT = 1800; // 2 second time limit minus a buffer of 200 ms for the rest of the code to terminate
	
	/**
	 * Performs a Monte Carlo Tree Search, starting from the given boardState.
	 * @param boardState is the board state to start the MCTS from.
	 * @return a Move determined to be the best move by the MCTS.
	 */
	public static int numRollouts = 0;
	public static int numChildrenCreated = 0;
	public static Move MonteCarloTreeSearch(PentagoBoardState board) {
		int startTime = (int) System.currentTimeMillis();
		
		MonteCarloData rootDatum = new MonteCarloData(board, null); // Root contains null-move
		Tree<MonteCarloData> searchTree = new Tree<MonteCarloData>(rootDatum); // Init search tree
		Node<MonteCarloData> currentNode = searchTree.rootNode;
		
		// Start search loop
		while((int) System.currentTimeMillis() - startTime < MOVE_TIME_LIMIT) {
			if (currentNode.isLeaf()) // LEAF
			{
				// Perform a rollout if the leaf node has zero visits so far
				if (currentNode.data.visitCount == 0) 
				{
					rolloutWithUpdate(currentNode);
					currentNode = searchTree.rootNode;
				}
				else 
				{
					// Generate children if the leaf node has been visited before
					ArrayList<PentagoMove> childGeneratingMoves = currentNode.data.board.getAllLegalMoves();
					for(PentagoMove move : childGeneratingMoves)
					{
						numChildrenCreated++;
						PentagoBoardState newBoard = (PentagoBoardState) currentNode.data.board.clone();
						newBoard.processMove(move); // Apply move to cloned board
						currentNode.addChild(new MonteCarloData(newBoard, move));// Add child to Monte Carlo Tree
					}
					
					// Perform a rollout on the first child of the former leaf node
					Node<MonteCarloData> firstChild = currentNode.getChildren().get(0);
					rolloutWithUpdate(firstChild);
					currentNode = searchTree.rootNode;
				}
			}
			else // NOT A LEAF
			{
				// Use the "Tree Policy" to navigate towards a leaf node.
				double maxUCB = 0;
				Node<MonteCarloData> maxChild = null;
				List<Node<MonteCarloData>> children = currentNode.getChildren();
				
				System.out.println("Calculating the UCB for all child nodes...");
				// Iterate through the current node's children to calculate their UCBs
				for(Node<MonteCarloData> child : children) {
					
					System.out.println("Visit count for UCB calculation: " + child.data.visitCount);
					System.out.println("Win count for UCB calculation: " + child.data.winCount);
					
					// Win rate is either zero or the child's wins / visits.
					int visits = child.data.visitCount;
					int wins = child.data.winCount;
					float childWinRate;
					if (visits == 0) {
						childWinRate = 0;
					}
					else{
						childWinRate = wins / visits;
					}
					
					int currentVisitCount = currentNode.data.visitCount;
					double childUCB = childWinRate + Math.sqrt((2*Math.log(currentVisitCount)) / visits);
				
					// Replace max if a new highest UCB has been found
					if (maxUCB < childUCB) {
						maxUCB = childUCB;
						maxChild = child;
					}
				}
				// Set the current node to the child with highest UCB and restart the loop
				currentNode = maxChild;
 			}
		}// POST SEARCH
		
		// Find the highest scoring root child
		float bestWinRate = 0;
		Node<MonteCarloData> bestChild = null;
		for(Node<MonteCarloData> child : searchTree.rootNode.getChildren()) {
			float childWinRate = child.data.visitCount;
			if (childWinRate == 0) continue;
			else {
				childWinRate = child.data.winCount / child.data.visitCount;
				if (childWinRate > bestWinRate) {
					bestWinRate = childWinRate;
					bestChild = child;
				}
			}
		}
		
		PentagoMove bestMove = null;
		ArrayList<PentagoMove> possibleMoves = searchTree.rootNode.data.board.getAllLegalMoves();
		for(PentagoMove move : possibleMoves) {
			PentagoBoardState moveBoard = (PentagoBoardState) searchTree.rootNode.data.board.clone();
			moveBoard.processMove(move);
			if (moveBoard.equals(bestChild.data.board)) {
				bestMove = move;
				break;
			}
		}
		
		//Print MCTS stats
		System.out.println("Finished MCTS with " + numRollouts + " rollouts and " + numChildrenCreated + " child nodes.");
		numRollouts = 0;
		numChildrenCreated = 0;
		
		if (bestMove == null) throw new NullPointerException("BEST MOVE IS NULL WHATTTTTT THE FUCK");
		return bestMove;
	}
	
	/**
	 * Performs a rollout on the currentNode and updates the visited and win counts for
	 * the currentNode and each of its parents.
	 * @param currentNode is the node that the rollout is performed on.
	 */
	private static void rolloutWithUpdate(Node<MonteCarloData> currentNode) {
		
		numRollouts++;
		// Perform rollout
		int result = stochasticRollout(currentNode.data.board);
		System.out.println("result = " + result);
		
		// Update win and visit counts all the way up the tree
		System.out.println("Updating values after rollout...");
		boolean isUpdating = true;
		while(isUpdating) {
			currentNode.data.winCount += result;
			currentNode.data.visitCount += 1;
			System.out.println("Wincount: " + currentNode.data.winCount);
			System.out.println("Visitcount: " + currentNode.data.visitCount);
			if (currentNode.isRoot()) {
				isUpdating = false;
			}
			else {
				currentNode = currentNode.parent();
			}
		}
	}

	/**
	 * Simulates a game rollout from a given boardState.
	 * @param boardState the boardState to rollout from.
	 * @return 1 if the player whose turn it was in the initial board state
	 * won the game, 0 otherwise.
	 */
	private static int stochasticRollout(PentagoBoardState boardState) {
		
		// Deep copy of the board state so we don't affect the board state in the tree
		PentagoBoardState currentState = (PentagoBoardState) boardState.clone();
		
		int AI_player_number = boardState.getTurnPlayer();
		
		// Process random moves for each player until someone wins.
		while(currentState.gameOver() == false) {
			PentagoMove randomMove = (PentagoMove) currentState.getRandomMove();
			currentState.processMove(randomMove);
		}
		
		// Return 1 if the AI agent won, 0 otherwise
		int winner = currentState.getWinner();
		if (winner == AI_player_number) {
			return 1;
		}
		return 0;
	}
	
}

/**
 * Datatype of information to store inside each node for the MCTS.
 * @author Samuel Morris (dodobird)
 */
class MonteCarloData{
	
	public int visitCount = 0;
	public int winCount = 0;
	public PentagoBoardState board;
	public PentagoMove move;
	
	public MonteCarloData(PentagoBoardState board, PentagoMove move) {
		this.board = board;
		this.move = move;
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
	
	public T data;
	private Optional<Node<T>> parent;// immutable option for a parent node
	private ArrayList<Node<T>> children;// mutable list of children
	
	/**
	 * Construct a node without any children.
	 * @param value of the node.
	 * @param parent of the node.
	 */
	public Node(T value, Node<T> parent) {
		this.data = value;
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
		if (n.data == this.data && n.parent.equals(this.parent)) {
			return true;
		}
		return false;
	}
}


