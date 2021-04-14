package student_player;

import boardgame.Move;

import pentago_twist.PentagoPlayer;
import pentago_twist.PentagoBoardState;

/**
 * 
 * @author Samuel Morris (dodobird)
 *
 */
public class StudentPlayer extends PentagoPlayer {

	/**
	 * NOTE: Cannot change this function at all.
	 */
    public StudentPlayer() {
        super("260833487");
    }

    /**
     * This is the primary method that you need to implement. The ``boardState``
     * object contains the current state of the game, which your agent must use to
     * make decisions.
     */
    public Move chooseMove(PentagoBoardState boardState) {
        return MyTools.MonteCarloTreeSearch(boardState, false);
    }
}