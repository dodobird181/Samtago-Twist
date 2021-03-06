package student_player;

import pentago_twist.PentagoBoard;
import pentago_twist.PentagoBoardState;
import pentago_twist.PentagoMove;
import pentago_twist.PentagoPlayer;
import pentago_twist.RandomPentagoPlayer;

public class ServerlessAutoplay {
	
	private static int NUMBER_OF_GAMES = 100;
	
	public static void main(String[] args) {
		new ServerlessAutoplay(new StudentPlayer(), new RandomPentagoPlayer(), NUMBER_OF_GAMES);
	}
	
	public ServerlessAutoplay(PentagoPlayer p1, PentagoPlayer p2, int gamesToPlay) {
		
		PentagoBoardState board;
		int p1Wins = 0, p2Wins = 0;
		for (int i = 0; i < gamesToPlay; i++) {
			board = new PentagoBoardState();
			while(board.getWinner() == PentagoBoard.NOBODY) {
				if (board.getTurnPlayer() == 0) {
					if (i % 2 == 0) {
						board.processMove((PentagoMove) p1.chooseMove(board));
					}
					else {
						board.processMove((PentagoMove) p2.chooseMove(board));
					}
				}
				else {
					if (i % 2 == 0) {
						board.processMove((PentagoMove) p2.chooseMove(board));
					}
					else {
						board.processMove((PentagoMove) p1.chooseMove(board));
					}
				}
			}
			int winner = board.getWinner();
			if (winner == PentagoBoard.DRAW) {
				System.out.println("Game " + i + "is a draw");
				continue;
			}
			else {
				if (i % 2 == 0) {
					if (winner == 0) {
						p1Wins++;
						System.out.println(p1.getName() + " wins game " + i);
					}
					else {
						p2Wins++;
						System.out.println(p2.getName() + " wins game " + i);
					}
				}
				else {
					if (winner == 1) {
						p1Wins++;
						System.out.println(p1.getName() + " wins game " + i);
					}
					else {
						p2Wins++;
						System.out.println(p2.getName() + " wins game " + i);
					}
				}
			}
		}
		
		System.out.println(p1.getName() + " wins: " + p1Wins);
		System.out.println(p2.getName() + " wins: " + p2Wins);
	}
}
