package student_player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import boardgame.Move;
import pentago_twist.PentagoBoardState;
import pentago_twist.PentagoBoardState.Piece;
import pentago_twist.PentagoCoord;
import pentago_twist.PentagoMove;

public class MyTools {
	
	private static final int MOVE_TIME_LIMIT = 1800; // 2 second time limit minus a buffer of 200 ms for the rest of the code to terminate
	
	/**
	 * Performs a Monte Carlo Tree Search, starting from the given boardState.
	 * @param boardState is the board state to start the MCTS from.
	 * @return a Move determined to be the best move by the MCTS.
	 */
	public static int numRollouts = 0;
	public static int numChildrenCreated = 0;
	public static Move MonteCarloTreeSearch(PentagoBoardState board, boolean isAdversarialRAVE) {
		int startTime = (int) System.currentTimeMillis();
		
		NodeBoard rootBoard = new NodeBoard(board, null); // root contains null-move
		Tree<String, NodeBoard> tree = new Tree<String, NodeBoard>(rootBoard); // Init search tree
		Node<String, NodeBoard> currentNode = tree.root; // Set initial node to root
		
		// Immediately check to see if a winning move exists
		ArrayList<PentagoMove> moves = currentNode.data.board.getAllLegalMoves();
		int AI_PLAYER_NUMBER = currentNode.data.board.getTurnPlayer();
		for(PentagoMove move : moves) {
			PentagoBoardState newClone = (PentagoBoardState) currentNode.data.board.clone();
			newClone.processMove(move);
			if (newClone.getWinner() == AI_PLAYER_NUMBER) {
				return move; //return if a winning move exists
			}
		}
		
		// Generate children for the root node prior to starting search
		generateChildren(tree.root);
		
		int timeLimit = MOVE_TIME_LIMIT;
		if (board.getTurnNumber() == 0) {
			timeLimit = 25000;
		}
		// Start search loop
		while((int) System.currentTimeMillis() - startTime < timeLimit) {
			if (currentNode.isLeaf()) // LEAF
			{
				// Perform a rollout if the leaf node has zero visits so far
				if (currentNode.data.visitCount == 0) 
				{
					rolloutWithUpdate(currentNode);
					currentNode = tree.root;
				}
				else 
				{
					generateChildren(currentNode);
					
					// Perform a rollout on the first child of the former leaf node
					Optional<Node<String, NodeBoard>> firstChildOpt = currentNode.childMap().values().stream().findFirst();
					firstChildOpt.ifPresent(firstChild -> {
						rolloutWithUpdate(firstChild);
					});
					currentNode = tree.root;
				}
			}
			else // NOT A LEAF
			{
				// Use the "Tree Policy" to navigate towards a leaf node.
				double maxUCB = 0;
				Node<String, NodeBoard> maxChild = currentNode.childMap().values().stream().findFirst().orElseThrow();
				HashMap<String, Node<String, NodeBoard>> children = currentNode.childMap();
				
				//System.out.println("Calculating the UCB for all child nodes...");
				// Iterate through the current node's children to calculate their UCBs
				for(Node<String, NodeBoard> child : children.values()) {
					
					//System.out.println("Visit count for UCB calculation: " + child.data.visitCount);
					//System.out.println("Win count for UCB calculation: " + child.data.winCount);
					
					// Win rate is either zero or the child's wins / visits.
					int visits = child.data.visitCount;
					float childWinRate = child.data.winRate();
					
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
			
		 //System.out.println("Size of child map inside while loop: " + tree.root.childMap().size());
		}// POST SEARCH
		
		// Find the node with the highest win rate
		ArrayList<Node<String, NodeBoard>> rootChildrenList = new ArrayList<>(tree.root.childMap().values());
		rootChildrenList.sort(NodeBoard.byHighestWinrate());
		Move bestMove = rootChildrenList.get(0).data.move;
		
		// Print sorted list and best winRate
		//rootChildrenList.forEach(child -> {
		//	System.out.println(child.data.winRate());
		//});
		//System.out.println("BestMove winRate is " + rootChildrenList.get(0).data.winRate());
		
		
		//Print MCTS stats
		//System.out.println("Finished MCTS with " + numRollouts + " rollouts and " + numChildrenCreated + " child nodes.");
		numRollouts = 0;
		numChildrenCreated = 0;
		
		// Print move string
		//System.out.println("MCTS chose the move: " + bestMove.toPrettyString());
		
		return bestMove;
	}
	
	/**
	 * Generates children for the current node. If the current node is the root, then 
	 * initilizes the children using RootNodeBoard instead of NodeBoard.
	 * @param currentNode is the node to generate children for.
	 */
	public static void generateChildren(Node<String, NodeBoard> currentNode) {
		
		// Generate children if the leaf node has been visited before
		for(PentagoMove move : currentNode.data.board.getAllLegalMoves())
		{
			numChildrenCreated++;
			PentagoBoardState newBoard = (PentagoBoardState) currentNode.data.board.clone();
			newBoard.processMove(move); // Apply move to cloned board
			
			NodeBoard possibleChild = new NodeBoard(newBoard, move);
			//if (filterBoardsByTouching().test(possibleChild)) {
				currentNode.addChild(move.toPrettyString(), possibleChild);// Add child to Monte Carlo Tree
			//}
		}
		//System.out.println("Legal moves size: " + currentNode.data.board.getAllLegalMoves().size());
		//System.out.println("Filtered moves size: " + currentNode.childMap().size());
	}

	/**
	 * Performs a rollout on the currentNode and updates the visited and win counts for
	 * the currentNode and each of its parents.
	 * @param currentNode is the node that the rollout is performed on.
	 */
	public static void rolloutWithUpdate(Node<String, NodeBoard> currentNode) {
		
		numRollouts++;
		// Perform rollout
		int result = stochasticRollout(currentNode.data.board);
		//System.out.println("result = " + result);
		
		// Update win and visit counts all the way up the tree
		//System.out.println("Updating values after rollout...");
		boolean isUpdating = true;
		while(isUpdating) {
			currentNode.data.winCount += result;
			currentNode.data.visitCount += 1;
			//System.out.println("Wincount: " + currentNode.data.winCount);
			//System.out.println("Visitcount: " + currentNode.data.visitCount);
			if (currentNode.isRoot()) {
				isUpdating = false;
			}
			else {
				currentNode = currentNode.parent();
			}
		}
	}
	
	public static void adversarialRAVE(Node<String, NodeBoard> currentNode) {
		numRollouts++;
		
		// Get siblings of the current node to possibly update after rollout
		HashMap<String, Node<String, NodeBoard>> siblings = new HashMap<String, Node<String, NodeBoard>>();
		if (currentNode.isRoot() == false) {
			siblings = currentNode.parent().childMap();
		}
		
		// Deep copy of the board state so we don't affect the board state in the tree
		PentagoBoardState currentState = (PentagoBoardState) currentNode.data.board.clone();
		int AI_player_number = currentState.getTurnPlayer();
		
		// Process random moves for each player until someone wins.
		// and keep track of the siblings whose moves were played in this rollout
		HashMap<String, Node<String, NodeBoard>> siblingsInRollout = new HashMap<String, Node<String, NodeBoard>>();
		while(currentState.gameOver() == false) {
			
			PentagoMove bestMove = bestMoveAccordingToEval(currentState);
			//PentagoMove bestMove = (PentagoMove) currentState.getRandomMove();
			
			// If it's the AI's turn and the move key returns a sibling, add it to "siblingsInRollout"
			Node<String, NodeBoard> siblingPlayed = siblings.get(bestMove.toPrettyString());
			if (siblingPlayed != null) {
				siblingsInRollout.put(bestMove.toPrettyString(), siblingPlayed);// place the sibling
				//System.out.println("Adding to Siblings In Rollout, total size is: " + siblingsInRollout.size());
			}
			
			currentState.processMove(bestMove);
		}
		
		// result is 1 if the AI agent won, 0 otherwise
		int rolloutResult;
		int winner = currentState.getWinner();
		if (winner == AI_player_number) {
			rolloutResult = 1;
		}
		else {
			rolloutResult = 0;
		}
		//### End of rollout
		
		// normal update of win and visit counts all the way up the tree
		boolean isUpdating = true;
		while(isUpdating) {
			currentNode.data.winCount += rolloutResult;
			currentNode.data.visitCount += 1;
			//System.out.println("Wincount: " + currentNode.data.winCount);
			//System.out.println("Visitcount: " + currentNode.data.visitCount);
			if (currentNode.isRoot()) {
				isUpdating = false;
			}
			else {
				currentNode = currentNode.parent();
			}
		}
		
		// additionally update any siblings whose moves may have been played during the rollout
		if (rolloutResult == 0) return; //optimization
		for(Node<String, NodeBoard> sibling : siblingsInRollout.values()) {
			sibling.data.winCount += 1;
			sibling.data.visitCount += 1;
		}
	}
	
	/**
	 * Performs a single-move lookahead and ranks possible moves according to the 
	 * board evaluation function, returning the best move.
	 * @param state to perform the lookahead on.
	 * @return the best move for the state according to the evaluation function (works for either player).
	 */
	private static PentagoMove bestMoveAccordingToEval(PentagoBoardState state) {
		
		int bestVal = Integer.MIN_VALUE;
		ArrayList<PentagoMove> legalMoves = state.getAllLegalMoves();
		if (legalMoves.size() == 0) {
			throw new IllegalStateException("ERROR: bestMoveAccordingToEval has been "
					+ "called on a PentagoBoardState which produced 0 possible"
					+ " legal moves! This should not happen!!!");
		}
		PentagoMove bestMove = legalMoves.get(0);
		for(PentagoMove move : legalMoves) {
			PentagoBoardState clone = (PentagoBoardState) state.clone();// clone board to not affect it
			clone.processMove(move);// process possible best move
			
			int cloneVal = boardEvaluationFn(clone);
			if(cloneVal > bestVal) {
				bestVal = cloneVal;
				bestMove = move;
			}
		}
		return bestMove;
	}

	public static Comparator<PentagoBoardState> sortByHighestEvalFunction(){
		return new Comparator<PentagoBoardState>() {

			@Override
			public int compare(PentagoBoardState s1, PentagoBoardState s2) {
				int e1 = boardEvaluationFn(s1);
				int e2 = boardEvaluationFn(s2);
				if (e1 > e2) {
					return -1;
				}
				else if (e2 > e1) {
					return 1;
				}
				return 0;
			}
			
		};
	}
	
	/**
	 * Return an estimate of how good the board is for the current player.
	 * @param b the PentagoBoardState to evaluate
	 * @return an integer estimate of the board's value
	 */
	private static int boardEvaluationFn(PentagoBoardState b) {
		
		Piece AI_PIECE = b.getTurnPlayer() == 0 ? Piece.WHITE : Piece.BLACK;
		
		int points = 0;
		int POWER = 10;
		
		// check rows
		for(int row = 0; row < 6; row++) {
			for(int col = 0; col < 6; col++) {
				int streakAi = 0;
				int streakOpp = 0;
				int bestAi = 0;
				int bestOpp = 0;
				Piece curRow = b.getPieceAt(row, col);
				if (curRow != AI_PIECE) {
					streakAi++;
					if (streakAi > bestAi) {
						bestAi = streakAi;
					}
					streakOpp = 0;
				}
				else {
					streakOpp++;
					if (streakOpp > bestOpp) {
						bestOpp = streakOpp;
					}
					streakAi = 0;
				}
				points += Math.pow(POWER, bestAi);
				points -= Math.pow(POWER, bestOpp);
			}
		}
		
		// check columns
		for(int row = 0; row < 6; row++) {
			for(int col = 0; col < 6; col++) {
				int streakAi = 0;
				int streakOpp = 0;
				int bestAi = 0;
				int bestOpp = 0;
				Piece curRow = b.getPieceAt(col, row);
				if (curRow != AI_PIECE) {
					streakAi++;
					if (streakAi > bestAi) {
						bestAi = streakAi;
					}
					streakOpp = 0;
				}
				else {
					streakOpp++;
					if (streakOpp > bestOpp) {
						bestOpp = streakOpp;
					}
					streakAi = 0;
				}
				points += Math.pow(POWER, bestAi);
				points -= Math.pow(POWER, bestOpp);
			}
		}
		return points;
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
class NodeBoard{
	
	public int visitCount = 0;
	public int winCount = 0;
	public PentagoBoardState board;
	public Move move;
	
	public NodeBoard(PentagoBoardState board, Move move) {
		this.board = board;
		this.move = move;
	}
	
	/**
	 * Returns the win rate of the node.
	 */
	public float winRate() {
		if (visitCount == 0) {
			return 0;
		}
		else {
			return  (float)winCount / (float)visitCount;
		}
	}
	
	/**
	 * A comparator that sorts MonteCarloData nodes by highest winrate first.
	 */
	public static Comparator<Node<String, NodeBoard>> byHighestWinrate(){
		return new Comparator<Node<String, NodeBoard>>() {

			@Override
			public int compare(Node<String, NodeBoard> n1, Node<String, NodeBoard> n2) {
				
				if (n1.data.winRate() > n2.data.winRate()) return -1;
				else if (n1.data.winRate() < n2.data.winRate()) return 1;
				return 0;
			}
		};
	}
}

/**
 * A generically-typed implementation of a tree in Java, using a HashMap.
 * @author Samuel Morris (dodobird)
 */
class Tree<K, V>{

	public Node<K, V> root;
	
	/**
	 * Construct a new tree by passing a value for the root node.
	 * @param rootValue is the value that the root of the tree will have
	 * when the tree is initilized.
	 */
	public Tree(V rootValue) {
		assert rootValue != null;
		this.root = new Node<K, V>(rootValue, null);// Parent of the root node is null.
	}
}

/**
 * A generically typed node to be used in a tree-like datastructure. Children are stored in
 * a hashmap for quick access.
 * @author Samuel Morris (dodobird)
 * @param <K> The type of the key for accessing the hashmap of children.
 * @param <V> The type of the datum stored inside this node and all its children.
 */
class Node<K, V>{
	
	public V data;
	private Optional<Node<K, V>> parent;// immutable option for a parent node
	private HashMap<K, Node<K, V>> children;// mutable map of children, unique for each possible move
	
	/**
	 * Construct a node without any children.
	 * @param value of the node.
	 * @param parent of the node.
	 */
	public Node(V value, Node<K, V> parent) {
		this.data = value;
		this.parent = Optional.ofNullable(parent);
		children = new HashMap<K, Node<K, V>>();
	}
	
	/**
	 * Construct a node with a pre-defined HashMap of children.
	 * @param value of the node.
	 * @param parent of this node. 
	 * @param children is the HashMap of children for this node.
	 */
	public Node(V value, Node<K, V> parent, HashMap<K, Node<K, V>> children) {
		this(value, parent);
		this.children = children;
	}
	
	/**
	 * @return a HashMap of children, with possible moves from this node's board-state as keys.
	 */
	public HashMap<K, Node<K, V>> childMap() {
		return children;
	}
	
	/**
	 * Add a new child node to this node's list of children. 
	 * The child's parent is set to be this node, and the child's children
	 * ArrayList is empty.
	 * 
	 * @param value of the child node.
	 */
	public void addChild(K key, V value) {
		Node<K, V> child = new Node<K, V>(value, this);
		children.put(key, child);
	}
	
	/**
	 * Return true if this node's children ArrayList contains the given child.
	 * @param child is the node to check if it is the child of this node.
	 */
	public boolean hasChild(K key) {
		return children.containsKey(key);
	}
	
	/**
	 * Removes a child from this node's children ArrayList if 
	 * one exists matching the given child.
	 * @param child node to remove.
	 */
	public void removeChild(K key) {
		children.remove(key);
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
	public Node<K, V> parent(){
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
		if (!(o instanceof Node<?, ?>)) return false;
		Node<?, ?> n = (Node<?, ?>)o;
		if (n.data == this.data && n.parent.equals(this.parent)) {
			return true;
		}
		return false;
	}
}


