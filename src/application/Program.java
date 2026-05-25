package application;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Scanner;

import chess.ChessException;
import chess.ChessMatch;
import chess.ChessPiece;
import chess.ChessPosition;

public class Program {

	public static void main(String[] args) {
		
		Scanner sc = new Scanner(System.in);
		ChessMatch chessMath = new ChessMatch();
		List<ChessPiece> captured = new ArrayList<>();
		
		while(!chessMath.getCheckMate()) {
			try {
				UI.clearScreen();
				UI.printMatch(chessMath, captured);
				System.out.println();
				System.out.print("Origem: ");
				ChessPosition source = UI.readChessPosition(sc);
				
				boolean [][] possibleMove = chessMath.possibleMove(source);
				UI.clearScreen();
				UI.printBoard(chessMath.getPieces(),possibleMove);
				
				System.out.println();
				System.out.print("Destino: ");
				ChessPosition target = UI.readChessPosition(sc);
				
				ChessPiece capturedPiece = chessMath.performChessMove(source, target);
				
				if (capturedPiece != null) {
					captured.add(capturedPiece);
				}
				
				if (chessMath.getPromoted() != null) {
					System.out.print("Entre com a peça a ser promovida (B/C/T/Q): ");
					String type = sc.nextLine().toUpperCase();
					while ((!type.equals("B")) && (!type.equals("C")) && (!type.equals("T")) && !(type.equals("Q"))){
						System.out.print("Valor inválido! Digite uma peça para a promoção (B/C/T/Q):");
						type = sc.nextLine().toUpperCase();
					}
					chessMath.replacePromotedPiece(type);
				}
			}
			catch(ChessException e) {
				System.out.println(e.getMessage());
				//sc.nextLine();
			}
			catch(InputMismatchException e) {
				System.out.println(e.getMessage());
//				sc.nextLine();
			}
			UI.clearScreen();
			UI.printMatch(chessMath, captured);
		}
	}
}
