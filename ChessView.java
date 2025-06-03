package com.chess.of.god;

import android.content.Context;
import android.graphics.*;
import android.media.MediaPlayer;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class ChessView extends View {
	private static final int ROWS = 8;
	private static final int COLS = 8;
	private float cellWidth, cellHeight;
	
	private Piece[][] board = new Piece[ROWS][COLS];
	private int selectedRow = -1, selectedCol = -1;
	private boolean whiteTurn = true;
	
	private Paint whitePaint = new Paint();
	private Paint blackPaint = new Paint();
	private Paint highlightPaint = new Paint();
	private Paint killablePaint = new Paint();
	
	private Vibrator vibrator;
	private MediaPlayer mediaPlayer;
	private Context context;
	
	private Bitmap pieceSpriteSheet;
	private int pieceWidth, pieceHeight;
	
	
	public interface OnGameOverListener {
		void onGameOver(String winningColor);
	}
	
	private OnGameOverListener gameOverListener;
	
	public void setGameOverListener(OnGameOverListener listener) {
		this.gameOverListener = listener;
	}
	
	private void notifyGameOver(String winner) {
		if (gameOverListener != null) {
			gameOverListener.onGameOver(winner);
		}
	}
	
	public ChessView(Context context) {
		super(context);
		this.context = context;
		init(context);
	}
	
	public ChessView(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.context = context;
		init(context);
	}
	
	private void init(Context context) {
		vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
		
		whitePaint.setColor(Color.LTGRAY);
		blackPaint.setColor(Color.DKGRAY);
		highlightPaint.setColor(Color.YELLOW);
		highlightPaint.setAlpha(100);
		killablePaint.setColor(Color.RED);
		killablePaint.setAlpha(100);
		
		pieceSpriteSheet = BitmapFactory.decodeResource(getResources(), R.drawable.chess_pieces);
		pieceWidth = pieceSpriteSheet.getWidth() / 6;
		pieceHeight = pieceSpriteSheet.getHeight() / 2;
		
		mediaPlayer = MediaPlayer.create(context, R.raw.win_sound);
		initBoard();
	}
	
	private void initBoard() {
		// Clear board first
		for (int r = 0; r < ROWS; r++) {
			for (int c = 0; c < COLS; c++) {
				board[r][c] = null;
			}
		}
		
		// Black pieces on top (row 0 and 1)
		board[0][0] = new Piece("Rook", false, 5);
		board[0][1] = new Piece("Knight", false, 4);
		board[0][2] = new Piece("Bishop", false, 4);
		board[0][3] = new Piece("Queen", false, 5);
		board[0][4] = new Piece("King", false, 10);
		board[0][5] = new Piece("Bishop", false, 4);
		board[0][6] = new Piece("Knight", false, 4);
		board[0][7] = new Piece("Rook", false, 5);
		for (int c = 0; c < COLS; c++) {
			board[1][c] = new Piece("Pawn", false, 3);
		}
		
		// White pieces on bottom (row 7 and 6)
		board[7][0] = new Piece("Rook", true, 5);
		board[7][1] = new Piece("Knight", true, 4);
		board[7][2] = new Piece("Bishop", true, 4);
		board[7][3] = new Piece("Queen", true, 5);
		board[7][4] = new Piece("King", true, 10);
		board[7][5] = new Piece("Bishop", true, 4);
		board[7][6] = new Piece("Knight", true, 4);
		board[7][7] = new Piece("Rook", true, 5);
		for (int c = 0; c < COLS; c++) {
			board[6][c] = new Piece("Pawn", true, 3);
		}
	}
	
	
	
	
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		
		cellWidth = getWidth() / (float) COLS;
		cellHeight = getHeight() / (float) ROWS;
		
		for (int r = 0; r < ROWS; r++) {
			for (int c = 0; c < COLS; c++) {
				Paint paint = ((r + c) % 2 == 0) ? whitePaint : blackPaint;
				canvas.drawRect(c * cellWidth, r * cellHeight,
				(c + 1) * cellWidth, (r + 1) * cellHeight, paint);
			}
		}
		
		if (selectedRow != -1) {
			Piece selected = board[selectedRow][selectedCol];
			for (int r = 0; r < ROWS; r++) {
				for (int c = 0; c < COLS; c++) {
					if (isValidMove(selectedRow, selectedCol, r, c, selected)) {
						Piece target = board[r][c];
						Paint paint = (target == null) ? highlightPaint : killablePaint;
						canvas.drawRect(c * cellWidth, r * cellHeight,
						(c + 1) * cellWidth, (r + 1) * cellHeight, paint);
					}
				}
			}
		}
		
		for (int r = 0; r < ROWS; r++) {
			for (int c = 0; c < COLS; c++) {
				Piece piece = board[r][c];
				if (piece != null) {
					drawPiece(canvas, r, c, piece);
					drawHealthBorder(canvas, r, c, piece);
				}
			}
		}
	}
	private void drawPiece(Canvas canvas, int r, int c, Piece piece) {
		if (pieceSpriteSheet == null) return;
		
		int pieceWidth = pieceSpriteSheet.getWidth() / 6;  // 6 columns
		int pieceHeight = pieceSpriteSheet.getHeight() / 2; // 2 rows
		
		// White pieces are at top row (0), dark pieces at bottom row (1)
		int row = piece.isWhite ? 0 : 1;
		
		int col;
		if (piece.isWhite) {
			// White pieces order as in sprite sheet: pawn, rook, knight, bishop, queen, king
			switch (piece.type) {
				case "Pawn":   col = 0; break;
				case "Rook":   col = 1; break;
				case "Knight": col = 2; break;
				case "Bishop": col = 3; break;
				case "Queen":  col = 4; break;
				case "King":   col = 5; break;
				default:       col = 0; break;
			}
		} else {
			// Dark pieces order reversed: king, queen, bishop, knight, rook, pawn
			switch (piece.type) {
				case "King":   col = 0; break;
				case "Queen":  col = 1; break;
				case "Bishop": col = 2; break;
				case "Knight": col = 3; break;
				case "Rook":   col = 4; break;
				case "Pawn":   col = 5; break;
				default:       col = 0; break;
			}
		}
		
		Rect src = new Rect(
		col * pieceWidth,
		row * pieceHeight,
		(col + 1) * pieceWidth,
		(row + 1) * pieceHeight
		);
		
		Rect dest = new Rect(
		(int)(c * cellWidth),
		(int)(r * cellHeight),
		(int)((c + 1) * cellWidth),
		(int)((r + 1) * cellHeight)
		);
		
		canvas.drawBitmap(pieceSpriteSheet, src, dest, null);
	}
	
	
	
	
	private int getPieceRow(boolean isWhite) {
		return isWhite ? 0 : 1;
	}
	
	private int getPieceColumn(String type, boolean isWhite) {
		if (isWhite) {
			switch (type) {
				case "Pawn": return 0;
				case "Rook": return 1;
				case "Knight": return 2;
				case "Bishop": return 3;
				case "Queen": return 4;
				case "King": return 5;
			}
		} else {
			// Black pieces row mapping
			switch (type) {
				case "King": return 5;
				case "Queen": return 4;
				case "Bishop": return 3;
				case "Knight": return 2;
				case "Rook": return 1;
				case "Pawn": return 0;
			}
		}
		return 0;
	}
	
	
	private void drawHealthBorder(Canvas canvas, int r, int c, Piece piece) {
		Paint paint = new Paint();
		paint.setStyle(Paint.Style.STROKE);
		float healthRatio = (float) piece.currentHealth / piece.maxHealth;
		
		if (healthRatio >= 0.75f) {
			paint.setColor(Color.GREEN);
			paint.setStrokeWidth(6);
			canvas.drawRect(c * cellWidth, r * cellHeight,
			(c + 1) * cellWidth, (r + 1) * cellHeight, paint);
		} else if (healthRatio >= 0.4f) {
			paint.setColor(Color.YELLOW);
			paint.setStrokeWidth(8);
			canvas.drawLine(c * cellWidth, r * cellHeight,
			c * cellWidth, (r + 1) * cellHeight, paint);
		} else {
			paint.setColor(Color.RED);
			paint.setStrokeWidth(3);
			canvas.drawRect(c * cellWidth, r * cellHeight,
			(c + 1) * cellWidth, (r + 1) * cellHeight, paint);
		}
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (event.getAction() != MotionEvent.ACTION_DOWN) return false;
		
		int c = (int) (event.getX() / cellWidth);
		int r = (int) (event.getY() / cellHeight);
		if (r < 0 || r >= ROWS || c < 0 || c >= COLS) return false;
		
		Piece clicked = board[r][c];
		
		if (selectedRow == -1 && clicked != null && clicked.isWhite == whiteTurn) {
			selectedRow = r;
			selectedCol = c;
			invalidate();
			return true;
		}
		
		if (selectedRow >= 0) {
			Piece selectedPiece = board[selectedRow][selectedCol];
			if (isValidMove(selectedRow, selectedCol, r, c, selectedPiece)) {
				Piece target = board[r][c];
				if (target != null && target.isWhite != selectedPiece.isWhite) {
					target.currentHealth -= 1;
					vibrate();
					if (target.currentHealth <= 0) {
						if (target.type.equals("King")) {
							board[r][c] = selectedPiece;
							board[selectedRow][selectedCol] = null;
							invalidate();
							checkForGameOver();
							return true;
						}
						board[r][c] = selectedPiece;
						board[selectedRow][selectedCol] = null;
					}
				} else if (target == null) {
					board[r][c] = selectedPiece;
					board[selectedRow][selectedCol] = null;
				}
				whiteTurn = !whiteTurn;
				selectedRow = -1;
				selectedCol = -1;
				invalidate();
				return true;
			} else {
				selectedRow = -1;
				selectedCol = -1;
				invalidate();
				return true;
			}
		}
		return false;
	}
	
	private void vibrate() {
		if (vibrator != null && vibrator.hasVibrator()) {
			vibrator.vibrate(100);
		}
	}
	
	private void checkForGameOver() {
		boolean whiteKingAlive = false;
		boolean blackKingAlive = false;
		
		for (Piece[] row : board) {
			for (Piece p : row) {
				if (p != null && p.type.equals("King")) {
					if (p.isWhite) whiteKingAlive = true;
					else blackKingAlive = true;
				}
			}
		}
		
		if (!whiteKingAlive || !blackKingAlive) {
			String winner = whiteKingAlive ? "White" : "Black";
			if (mediaPlayer != null) mediaPlayer.start();
			if (gameOverListener != null) gameOverListener.onGameOver(winner);
		}
	}
	
	private boolean isValidMove(int fromRow, int fromCol, int toRow, int toCol, Piece piece) {
		if (piece == null || fromRow == toRow && fromCol == toCol ||
		toRow < 0 || toRow >= ROWS || toCol < 0 || toCol >= COLS)
		return false;
		
		int dRow = toRow - fromRow, dCol = toCol - fromCol;
		Piece target = board[toRow][toCol];
		
		switch (piece.type) {
			case "Pawn":
			int dir = piece.isWhite ? -1 : 1;
			if (dCol == 0 && dRow == dir && target == null) return true;
			if (dCol == 0 && dRow == 2 * dir && target == null &&
			board[fromRow + dir][fromCol] == null &&
			fromRow == (piece.isWhite ? 6 : 1)) return true;
			if (Math.abs(dCol) == 1 && dRow == dir && target != null && target.isWhite != piece.isWhite) return true;
			break;
			case "Rook":
			if (dRow == 0 || dCol == 0)
			return isPathClear(fromRow, fromCol, toRow, toCol) &&
			(target == null || target.isWhite != piece.isWhite);
			break;
			case "Knight":
			if ((Math.abs(dRow) == 2 && Math.abs(dCol) == 1) ||
			(Math.abs(dRow) == 1 && Math.abs(dCol) == 2))
			return target == null || target.isWhite != piece.isWhite;
			break;
			case "Bishop":
			if (Math.abs(dRow) == Math.abs(dCol))
			return isPathClear(fromRow, fromCol, toRow, toCol) &&
			(target == null || target.isWhite != piece.isWhite);
			break;
			case "Queen":
			if (Math.abs(dRow) == Math.abs(dCol) || dRow == 0 || dCol == 0)
			return isPathClear(fromRow, fromCol, toRow, toCol) &&
			(target == null || target.isWhite != piece.isWhite);
			break;
			case "King":
			if (Math.abs(dRow) <= 1 && Math.abs(dCol) <= 1)
			return target == null || target.isWhite != piece.isWhite;
			break;
		}
		return false;
	}
	
	private boolean isPathClear(int fromRow, int fromCol, int toRow, int toCol) {
		int dRow = Integer.compare(toRow, fromRow);
		int dCol = Integer.compare(toCol, fromCol);
		for (int r = fromRow + dRow, c = fromCol + dCol;
		r != toRow || c != toCol;
		r += dRow, c += dCol) {
			if (board[r][c] != null) return false;
		}
		return true;
	}
	
	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		if (mediaPlayer != null) {
			mediaPlayer.release();
			mediaPlayer = null;
		}
	}
	
	private static class Piece {
		String type;
		boolean isWhite;
		int maxHealth;
		int currentHealth;
		
		Piece(String type, boolean isWhite, int health) {
			this.type = type;
			this.isWhite = isWhite;
			this.maxHealth = health;
			this.currentHealth = health;
		}
	}
}
