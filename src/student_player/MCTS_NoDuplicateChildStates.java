package student_player;

import java.util.List;

import boardgame.Move;

import pentago_twist.PentagoPlayer;
import pentago_twist.PentagoBoardState;

/**
 * 
 * @author Samuel Morris (dodobird)
 *
 */
public class MCTS_NoDuplicateChildStates extends PentagoPlayer {

	/**
	 * NOTE: Cannot change this function at all.
	 */
    public MCTS_NoDuplicateChildStates() {
        super("MCTS_NoDuplicateChildStates");
    }
    
    private static final int MOVE_TIME_LIMIT = 500;

    /**
     * This is the primary method that you need to implement. The ``boardState``
     * object contains the current state of the game, which your agent must use to
     * make decisions.
     */
    public Move chooseMove(PentagoBoardState boardState) {
    	int numRollouts = 0;
    	int numChildrenCreated = 0;
    	int startTime = (int) System.currentTimeMillis();
		
		MonteCarloData rootDatum = new MonteCarloData(boardState, null); // Root contains null-move
		Tree<MonteCarloData> searchTree = new Tree<MonteCarloData>(rootDatum); // Init search tree
		Node<MonteCarloData> currentNode = searchTree.rootNode;
		
		// Start search loop
		while((int) System.currentTimeMillis() - startTime < MOVE_TIME_LIMIT) {
			if (currentNode.isLeaf()) // LEAF
			{
				// Perform a rollout if the leaf node has zero visits so far
				if (currentNode.data.visitCount == 0) 
				{
					MyTools.rolloutWithUpdate(currentNode);
					currentNode = searchTree.rootNode;
				}
				else 
				{
					// KEY FEATURE
					MyTools.generateChildrenNoDuplicateBoardStates(currentNode);
					
					// Perform a rollout on the first child of the former leaf node
					Node<MonteCarloData> firstChild = currentNode.getChildren().get(0);
					MyTools.rolloutWithUpdate(firstChild);
					currentNode = searchTree.rootNode;
				}
			}
			else // NOT A LEAF
			{
				// Use the "Tree Policy" to navigate towards a leaf node.
				double maxUCB = 0;
				Node<MonteCarloData> maxChild = null;
				List<Node<MonteCarloData>> children = currentNode.getChildren();
				
				//System.out.println("Calculating the UCB for all child nodes...");
				// Iterate through the current node's children to calculate their UCBs
				for(Node<MonteCarloData> child : children) {
					
					//System.out.println("Visit count for UCB calculation: " + child.data.visitCount);
					//System.out.println("Win count for UCB calculation: " + child.data.winCount);
					
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
		
		// Find the move associated with the node that has the highest winRate
		searchTree.rootNode.getChildren().sort(MonteCarloData.highestWinrate());
		
		//Print MCTS stats
		System.out.println("Finished MCTS with " + numRollouts + " rollouts and " + numChildrenCreated + " child nodes.");
		numRollouts = 0;
		numChildrenCreated = 0;
		
		return searchTree.rootNode.getChildren().get(0).data.move;
    }
    	
}