package application;

import java.util.InputMismatchException;
import java.util.Scanner;

import chess.ChessException;
import chess.ChessMatch;
import chess.ChessPiece;
import chess.ChessPosition;

public class Program {

	public static void main(String[] args) {	
//		Position pos = new Position(3, 5);
//		System.out.println(pos);
		
//		Board board = new Board(8,8);
		
		Scanner sc = new Scanner(System.in);
		
		ChessMatch chessMath = new ChessMatch();
		while(true) {
			try {
				UI.clearScreen();
				UI.printBoard(chessMath.getPieces());
				System.out.println();
				System.out.print("Origem: ");
				ChessPosition source = UI.readChessPosition(sc);
				
				boolean [][] possibleMove = chessMath.possibleMove(source);
				UI.clearScreen();
				UI.printBoard(chessMath.getPieces(),possibleMove);
				
				System.out.println();
				System.out.print("Destino: ");
				ChessPosition target = UI.readChessPosition(sc);
				
				ChessPiece capturePiece = chessMath.performChessMove(source, target);							
			}
			catch(ChessException e) {
				System.out.println(e.getMessage());
				sc.nextLine();
			}
			catch(InputMismatchException e) {
				System.out.println(e.getMessage());
				sc.nextLine();
			}
		}
	}
}
