package chess;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import boardgame.Board;
import boardgame.Piece;
import boardgame.Position;
import chess.pieces.Bishop;
import chess.pieces.King;
import chess.pieces.Knight;
import chess.pieces.Pawn;
import chess.pieces.Queen;
import chess.pieces.Rook;

public class ChessMatch {
	private int turn;
	private Color currentPlayer;
	private Board board;
	private boolean check;
	private boolean checkMate;
	private ChessPiece enPassantVulnerable;
	private ChessPiece promoted;
	
	private List<Piece> pieceOnTheBoard = new ArrayList<>();
	private List<Piece> capturedPieces = new ArrayList<>();
	
	public ChessMatch() {
		board = new Board(8,8);
		turn = 1;
		currentPlayer = Color.BRANCA;
		initialSetup();
		check = false;
	}
	
	public int getTurn() {
		return this.turn;
	}
	
	public Color getCurrentPlayer() {
		return this.currentPlayer;
	}
	
	public boolean getCheck() {
		return check;
	}
	
	public boolean getCheckMate() {
		return checkMate;
	}
	
	public ChessPiece getEnPassantVulnerable() {
		return enPassantVulnerable;
	}
	
	public ChessPiece getPromoted() {
		return promoted;
	}
				
	public ChessPiece[][] getPieces(){
		ChessPiece[][] mat = new ChessPiece[board.getRows()][board.getColumns()];
		for (int i = 0; i < board.getRows(); i++) {
			for (int j = 0; j < board.getColumns(); j++) {
				mat[i][j] = (ChessPiece) board.piece(i,j);
			}
		}
		return mat;
	}
	
	public boolean[][] possibleMove(ChessPosition sourcePosition) {
		Position position = sourcePosition.toPosition();
		validateSourcePosition(position);
		return board.piece(position).possibleMoves();
	}
	
	public ChessPiece performChessMove(ChessPosition sourcePosition, ChessPosition targetPosition) {
		Position source = sourcePosition.toPosition();
		Position target = targetPosition.toPosition();
		validateSourcePosition(source);
		validateTargetPosition(source, target);
		Piece capturedPiece = makeMove(source, target);
		
		if (testCheck(currentPlayer)) {
			undoMove(source, target, capturedPiece);
			throw new ChessException("O seu Rei está em check, não é possível executar esse movimento. ");
		}
		
		ChessPiece movePiece = (ChessPiece) board.piece(target);
		
		// special move promotion
		promoted = null;
		if (movePiece instanceof Pawn) {
			if ((movePiece.getColor() == Color.BRANCA && target.getRow() == 0) || (movePiece.getColor() == Color.PRETA && target.getRow() == 7)) {
				promoted = (ChessPiece)board.piece(target);
				promoted = replacePromotedPiece("Q");
			}			
		}
		
		check = (testCheck(opponent(currentPlayer))) ? true : false;
						
		if (testCheckMate(opponent(currentPlayer))) {
			checkMate = true;
		}
		else {
			nextTurn();			
		}
		
		// special move en passant
		if (movePiece instanceof Pawn && (target.getRow() == source.getRow() - 2 || target.getRow() == source.getRow() + 2)) {
			enPassantVulnerable = movePiece;
		}
		else {
			enPassantVulnerable = null;
		}
		
		return (ChessPiece)capturedPiece;
	}
	
	public ChessPiece replacePromotedPiece(String type) {
		if (promoted == null) {
			throw new IllegalStateException("Não existe peça para ser promovida.");
		}
		if ((!type.equals("B")) && (!type.equals("C")) && (!type.equals("T")) && !(type.equals("Q"))){
			throw new InvalidParameterException("Tipo inválido para promotion.");
		}
		Position pos = promoted.getChessPosition().toPosition();
		Piece p = board.removePiece(pos);
		pieceOnTheBoard.remove(p);
		
		ChessPiece newPiece = newPiece(type, promoted.getColor());
		board.placePiece(newPiece, pos);
		pieceOnTheBoard.add(newPiece);
		
		return newPiece;
	}
	
	private ChessPiece newPiece(String type,Color color) {
		if (type.equals("B")) return new Bishop(board, color);
		if (type.equals("C")) return new Knight(board, color);
		if (type.equals("Q")) return new Queen(board, color);
		return new Rook(board, color);
	}
	
	private Piece makeMove(Position source,Position target) {
		ChessPiece p = (ChessPiece)board.removePiece(source);
		p.increaseMoveCount();
		Piece capturedPiece = board.removePiece(target);
		board.placePiece(p, target);
		
		if(capturedPiece != null) {
			pieceOnTheBoard.remove(capturedPiece);
			capturedPieces.add(capturedPiece);
		}
		
		//movimento especial ROQUE PEQUENO
		if (p instanceof King && target.getColumn() == source.getColumn() + 2) {
			Position sourceT = new Position(source.getRow(), source.getColumn() + 3);
			Position targetT = new Position(source.getRow(), source.getColumn() + 1);
			ChessPiece rook = (ChessPiece)board.removePiece(sourceT);
			board.placePiece(rook, targetT);
			rook.increaseMoveCount();
		}
		
		//movimento especial ROQUE GRANDE
		if (p instanceof King && target.getColumn() == source.getColumn() - 2) {
			Position sourceT = new Position(source.getRow(), source.getColumn() - 4);
			Position targetT = new Position(source.getRow(), source.getColumn() - 1);
			ChessPiece rook = (ChessPiece)board.removePiece(sourceT);
			board.placePiece(rook, targetT);
			rook.increaseMoveCount();
		}
		
		//special move en passant
		if (p instanceof Pawn) {
			if (source.getColumn() != target.getColumn() && capturedPiece == null) {
				Position pawnPosition;
				if (p.getColor() == Color.BRANCA) {
					pawnPosition = new Position(target.getRow() + 1, target.getColumn());
				}
				else {
					pawnPosition = new Position(target.getRow() - 1, target.getColumn());
				}
				capturedPiece = board.removePiece(pawnPosition);
				capturedPieces.add(capturedPiece);
				pieceOnTheBoard.remove(capturedPiece);
			}
		}
				
		return capturedPiece;
	}
	
	private void undoMove(Position source,Position target, Piece capturedPiece) {
		ChessPiece p = (ChessPiece)board.removePiece(target);
		p.decreaseMoveCount();
		board.placePiece(p, source);
		if (capturedPiece != null) {
			board.placePiece(capturedPiece, target);
			capturedPieces.remove(capturedPiece);
			pieceOnTheBoard.add(capturedPiece);
		}
		
		//movimento especial ROQUE PEQUENO
		if (p instanceof King && target.getColumn() == source.getColumn() + 2) {
			Position sourceT = new Position(source.getRow(), source.getColumn() + 3);
			Position targetT = new Position(source.getRow(), source.getColumn() + 1);
			ChessPiece rook = (ChessPiece)board.removePiece(targetT);
			board.placePiece(rook, sourceT);
			rook.decreaseMoveCount();
		}
		
		//movimento especial ROQUE GRANDE
		if (p instanceof King && target.getColumn() == source.getColumn() - 2) {
			Position sourceT = new Position(source.getRow(), source.getColumn() - 4);
			Position targetT = new Position(source.getRow(), source.getColumn() - 1);
			ChessPiece rook = (ChessPiece)board.removePiece(targetT);
			board.placePiece(rook, sourceT);
			rook.decreaseMoveCount();
		}
		
		//special move en passant
		if (p instanceof Pawn) {
			if (source.getColumn() != target.getColumn() && capturedPiece == enPassantVulnerable) {
				ChessPiece pawn = (ChessPiece)board.removePiece(target);
				Position pawnPosition;
				if (p.getColor() == Color.BRANCA) {
					pawnPosition = new Position(3, target.getColumn());
				}
				else {
					pawnPosition = new Position(4, target.getColumn());
				}
				board.placePiece(pawn, pawnPosition);
			}
		}
		
	}
	
	private void validateSourcePosition(Position position) {
		if(!board.thereIsAPiece(position)) {
			throw new ChessException("Não existe peça na posição de origem.");
		}
		if (currentPlayer != ((ChessPiece) board.piece(position)).getColor()) {
			throw new ChessException("A peça escolhida é do adversário.");
		}
		if (!board.piece(position).isThereAnyPossibleMove()) {
			throw new ChessException("Não existem movimentos possiveis para a peça escolhida.");
		}
	}
	
	private void validateTargetPosition(Position source, Position target) {
		if(!board.piece(source).possibleMove(target)) {
			throw new ChessException("A peça escolhida não pode se mover para a posição de destino.");
		}
	}
	
	private void nextTurn() {
		turn++;
		currentPlayer = (currentPlayer == Color.BRANCA) ? Color.PRETA : Color.BRANCA; 
	}
	
	private Color opponent(Color color) {
		return (color == Color.BRANCA) ? Color.PRETA : Color.BRANCA;
	}
	
	private ChessPiece king(Color color) {
		List<Piece> list = pieceOnTheBoard.stream().filter(x -> ((ChessPiece)x).getColor() == color).collect(Collectors.toList());
		for(Piece p : list) {
			if (p instanceof King) {
				return (ChessPiece)p;
			}
		}
		throw new IllegalStateException("Não existe o rei da cor " + color + " no tabuleiro.");
	}
	
	public boolean testPositionForCheck(Color color,Position position) {
		List<Piece> opponentPieces = pieceOnTheBoard.stream().filter(x -> ((ChessPiece)x).getColor() == opponent(color)).collect(Collectors.toList());
		for(Piece p : opponentPieces) {
			if (!(p instanceof King)) {
				boolean[][] mat = p.possibleMoves();
				if (mat[position.getRow()][position.getColumn()]) {
					return true;
				}				
			}
		}
		return false;		
	}
	
	private boolean testCheck(Color color) {
		Position kingPosition = king(color).getChessPosition().toPosition();
		List<Piece> opponentPieces = pieceOnTheBoard.stream().filter(x -> ((ChessPiece)x).getColor() == opponent(color)).collect(Collectors.toList());
		for(Piece p : opponentPieces) {
			boolean[][] mat = p.possibleMoves();
			if (mat[kingPosition.getRow()][kingPosition.getColumn()]) {
				return true;
			}
		}
		return false;
	}
		
	private boolean testCheckMate(Color color) {
		if(!testCheck(color)) {
			return false;
		}
		List<Piece> list = pieceOnTheBoard.stream().filter(x -> ((ChessPiece)x).getColor() == color).collect(Collectors.toList());
		for(Piece p : list) {
			boolean[][] mat = p.possibleMoves();
			for (int i=0; i<board.getRows();i++) {
				for (int j=0; j<board.getColumns();j++) {
					if (mat[i][j]) {
						Position source = ((ChessPiece)p).getChessPosition().toPosition();
						Position target = new Position(i,j);
						Piece capturedPiece = makeMove(source,target);
						boolean testCheck = testCheck(color);
						undoMove(source, target, capturedPiece);
						if(!testCheck) {
							return false;
						}
					}
				}
			}
		}
		return true;
	}
	
	private void placeNewPiece(char column,int row, ChessPiece piece) {
		board.placePiece(piece, new ChessPosition(column, row).toPosition());
		pieceOnTheBoard.add(piece);
	}
		
	private void initialSetup() {        
        placeNewPiece('a', 1, new Rook(board, Color.BRANCA));
        placeNewPiece('b', 1, new Knight(board, Color.BRANCA));
        placeNewPiece('c', 1, new Bishop(board, Color.BRANCA));
        placeNewPiece('d', 1, new Queen(board, Color.BRANCA));
        placeNewPiece('e', 1, new King(board, Color.BRANCA, this));
        placeNewPiece('f', 1, new Bishop(board, Color.BRANCA));        
        placeNewPiece('g', 1, new Knight(board, Color.BRANCA));
        placeNewPiece('h', 1, new Rook(board, Color.BRANCA));
        placeNewPiece('a', 2, new Pawn(board, Color.BRANCA, this));
        placeNewPiece('b', 2, new Pawn(board, Color.BRANCA, this));
        placeNewPiece('c', 2, new Pawn(board, Color.BRANCA, this));
        placeNewPiece('d', 2, new Pawn(board, Color.BRANCA, this));
        placeNewPiece('e', 2, new Pawn(board, Color.BRANCA, this));
        placeNewPiece('f', 2, new Pawn(board, Color.BRANCA, this));
        placeNewPiece('g', 2, new Pawn(board, Color.BRANCA, this));
        placeNewPiece('h', 2, new Pawn(board, Color.BRANCA, this));
        
        placeNewPiece('a', 8, new Rook(board, Color.PRETA));
        placeNewPiece('b', 8, new Knight(board, Color.PRETA));
        placeNewPiece('c', 8, new Bishop(board, Color.PRETA));
        placeNewPiece('d', 8, new Queen(board, Color.PRETA));
        placeNewPiece('e', 8, new King(board, Color.PRETA, this));
        placeNewPiece('f', 8, new Bishop(board, Color.PRETA));
        placeNewPiece('g', 8, new Knight(board, Color.PRETA));
        placeNewPiece('h', 8, new Rook(board, Color.PRETA));
        placeNewPiece('a', 7, new Pawn(board, Color.PRETA, this));
        placeNewPiece('b', 7, new Pawn(board, Color.PRETA, this));
        placeNewPiece('c', 7, new Pawn(board, Color.PRETA, this));
        placeNewPiece('d', 7, new Pawn(board, Color.PRETA, this));
        placeNewPiece('e', 7, new Pawn(board, Color.PRETA, this));
        placeNewPiece('f', 7, new Pawn(board, Color.PRETA, this));
        placeNewPiece('g', 7, new Pawn(board, Color.PRETA, this));
        placeNewPiece('h', 7, new Pawn(board, Color.PRETA, this));        
    }
}
