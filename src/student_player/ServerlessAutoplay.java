package student_player;

import pentago_twist.PentagoBoard;
import pentago_twist.PentagoBoardState;
import pentago_twist.PentagoMove;
import pentago_twist.PentagoPlayer;
import pentago_twist.RandomPentagoPlayer;

public class ServerlessAutoplay {
	
	private static int NUMBER_OF_GAMES = 10;
	
	public static void main(String[] args) {
		new ServerlessAutoplay(new StudentPlayer(), new RandomPentagoPlayer(), NUMBER_OF_GAMES);
	}
	
	public ServerlessAutoplay(PentagoPlayer p1, PentagoPlayer p2, int gamesToPlay) {
		
		PentagoBoardState board;
		int p1Wins = 0, p2Wins = 0;
		for (int i = 0; i < gamesToPlay; i++) {
			System.out.println("Playing Game " + i + "...");
			board = new PentagoBoardState();
			while(board.getWinner() == PentagoBoard.NOBODY) {
				if (board.getTurnPlayer() == 0) {
					board.processMove((PentagoMove) p1.chooseMove(board));
				}
				else {
					board.processMove((PentagoMove) p2.chooseMove(board));
				}
			}
			int winner = board.getWinner();
			if (winner == PentagoBoard.DRAW) continue;
			if (winner == 0) {
				p1Wins++;
			}
			else {
				p2Wins++;
			}
		}
		
		System.out.println(p1.getName() + " wins: " + p1Wins);
		System.out.println(p2.getName() + " wins: " + p2Wins);
	}
}
