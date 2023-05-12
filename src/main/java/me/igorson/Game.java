package me.igorson;

import javax.swing.*;
import java.util.*;
import java.util.stream.IntStream;

public class Game implements Cloneable {

    int winner, currentMove;
    private ArrayList<int[]> gamePositionHistory = new ArrayList<>();

    private Stack<Move> moveHistory = new Stack<>();
    private Stack<boolean[]> castleRightsHistory = new Stack<>();

    private int movesFromLastMoveOrPawnCapture;

    private ChessBoardWithColumnsAndRows chessboardDisplay;

    private int[] squares;

    public Game(String startingFen, int startingSite, boolean wK, boolean wQ, boolean bK, boolean bQ) {
        squares = new int[64];
        loadBoardFromFen(startingFen);

        currentMove = startingSite;
        movesFromLastMoveOrPawnCapture = 0;
        castleRightsHistory.push(new boolean[]{wK, wQ, bK, bQ});

        this.chessboardDisplay = new ChessBoardWithColumnsAndRows(this);

    }

    void start() {

        while (winner == 0) {

            SwingUtilities.invokeLater(() -> chessboardDisplay.reloadGui());

            Move move = null;

            do {

                if (move != null && !checkIfMoveIsPossible(move)) {

                    System.out.println("This move is not possible! Try other one.");
                }

                move = readInputMove();

            } while (!checkIfMoveIsPossible(move));

            if (getPiece(squares[move.from]) != 1 || getPiece(squares[move.to]) == 0) {
                movesFromLastMoveOrPawnCapture += 1;
            } else {
                movesFromLastMoveOrPawnCapture = 0;
            }

            if (checkIfIsPromotion(move)) {
                makeMove(new Move(move.from, move.to, 2), false);
            } else {
                makeMove(move, false);
            }

            if (checkFiftyMoveRule()) {
                winner = 1;

                System.out.println("Draw !");

                SwingUtilities.invokeLater(() -> chessboardDisplay.reloadGui());

                continue;
            }

            if (checkStaleMate()) {

                winner = 1;

                System.out.println("Stalemate!");

                SwingUtilities.invokeLater(() -> chessboardDisplay.reloadGui());

                continue;
            }


            if (checkCheckMate()) {

                winner = currentMove == 8 ? 16 : 8;

                SwingUtilities.invokeLater(() -> chessboardDisplay.reloadGui());

                System.out.println((winner == 8 ? "White" : "Black") + " has won the game!");
            }

        }

    }

    private boolean checkIfIsPromotion(Move move) {

        for (Move m : calculateAllPossibleMoves(currentMove, false)) {
            if (m.pieceToPromote != 0) {
                return true;
            }
        }
        return false;
    }

    private boolean checkIfMoveIsPossible(Move move) {

        for (Move m : calculateAllPossibleMoves(currentMove, false)) {
            if (m.from == move.from && m.to == move.to && !isKingChecked(move, false)) {
                return true;
            }
        }
        return false;
    }

    private boolean checkFiftyMoveRule() {

        return movesFromLastMoveOrPawnCapture == 50;
    }

    private boolean checkStaleMate() {
        if (checkCheckMate()) return false;
        for (Move m : calculateAllPossibleMoves(currentMove, false)) {
            if (!isKingChecked(m, false)) {
                return false;
            }
        }
        return true;
    }

    private boolean checkCheckMate() {

        for (int i = 0; i <= 63; i++) {
            if (getPiece(squares[i]) == 6) {
                if (!isKingChecked(new Move(i, i), false)) {
                    return false;
                }
            }
        }

        for (Move m : calculateAllPossibleMoves(currentMove, false)) {
            if (!isKingChecked(m, false)) {
                return false;
            }
        }
        return true;
    }

    long perft(int depth) {

        if (depth == 0) {
            return 1;
        }

        long nodes = 0;

        List<Move> moves = calculateAllPossibleMoves(currentMove, false);

        for (Move move : moves) {

            if (isKingChecked(move, false)) continue;

            makeMove(move, true);

            nodes += perft(depth - 1);

            undoMove();
        }

        return nodes;
    }


    private boolean isKingChecked(Move m, boolean skipKing) {

        makeMove(m, true);

        for (Move move : calculateAllPossibleMoves(currentMove, skipKing)) {

            if (getPiece(squares[move.to]) == Piece.King) {
                undoMove();
                return true;
            }
        }
        undoMove();
        return false;
    }

    Move readInputMove() {
        char[] letters = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H'};

        String input, from, to;
        int fromSquare, toSquare;

        while (true) {

            System.out.println("Enter move. (Example: A1 A2).");
            Scanner sc = new Scanner(System.in);
            input = sc.nextLine();
            if (!input.matches("([A-H][1-8]) ([A-H][1-8])")) {
                System.out.println("Entered incorrect move. Try again.");
                continue;
            }
            from = input.split(" ")[0];
            to = input.split(" ")[1];
            break;
        }

        fromSquare = (8 - (Integer.parseInt(String.valueOf(from.charAt(1))))) * 8 + indexOf(letters, from.charAt(0));
        toSquare = (8 - (Integer.parseInt(String.valueOf(to.charAt(1))))) * 8 + indexOf(letters, to.charAt(0));

        return new Move(fromSquare, toSquare);
    }


    private void undoMove() {

        int[] gameToUndo = gamePositionHistory.get(gamePositionHistory.size() - 1);
        gamePositionHistory.remove(gamePositionHistory.size() - 1);

        for (int i = 0; i <= 63; i++) {

            squares[i] = gameToUndo[i];

        }

        currentMove = currentMove == 8 ? 16 : 8;

        moveHistory.pop();
        castleRightsHistory.pop();
    }

    private void makeMove(Move move, boolean isPerfTest) {

        boolean whiteKingsideCastlingRights, whiteQueensideCastlingRights, blackKingsideCastlingRights, blackQueensideCastlingRights;

        whiteKingsideCastlingRights = castleRightsHistory.peek()[0];
        whiteQueensideCastlingRights = castleRightsHistory.peek()[1];
        blackKingsideCastlingRights = castleRightsHistory.peek()[2];
        blackQueensideCastlingRights = castleRightsHistory.peek()[3];


        int[] lastBoardPosition = new int[64];

        for (int i = 0; i <= 63; i++) {
            lastBoardPosition[i] = squares[i];
        }
        gamePositionHistory.add(lastBoardPosition);

        int multiplayer = currentMove == 8 ? 1 : -1;

        if (getPiece(squares[move.from]) == Piece.Pawn) {
            if ((float) Math.abs(move.from - move.to) % 8 != 0 && squares[move.to] == 0) {
                squares[move.to + 8 * multiplayer] = 0;
            }
        }

        if (getPiece(squares[move.from]) == Piece.King) {

            if (currentMove == Piece.White) {
                whiteKingsideCastlingRights = false;
                whiteQueensideCastlingRights = false;


            } else {
                blackKingsideCastlingRights = false;
                blackQueensideCastlingRights = false;

            }

            if (move.to - move.from == 2) {

                squares[move.from + 1] = 4 + currentMove;
                squares[move.from + 3] = 0;

            } else {
                if (move.from - move.to == 2) {

                    squares[move.from - 1] = 4 + currentMove;
                    squares[move.from - 4] = 0;
                }
            }
        }

        if (getPiece(squares[move.from]) == Piece.Rook) {

            int file = move.from + 8 % 8 + 1;

            if (currentMove == Piece.White) {
                if (file == 1) {
                    whiteQueensideCastlingRights = false;
                }
                if (file == 8) {
                    whiteKingsideCastlingRights = false;
                }

            } else {
                if (file == 1) {
                    blackQueensideCastlingRights = false;
                }
                if (file == 8) {
                    blackKingsideCastlingRights = false;
                }
            }
        }

        if (getPiece(squares[move.to]) == Piece.Rook) {

            int file = move.to + 8 % 8 + 1;

            if (currentMove == Piece.Black) {
                if (file == 1) {
                    whiteQueensideCastlingRights = false;
                }
                if (file == 8) {
                    whiteKingsideCastlingRights = false;
                }

            }

            if (currentMove == Piece.White) {

                if (file == 1) {
                    blackQueensideCastlingRights = false;
                }
                if (file == 8) {
                    blackKingsideCastlingRights = false;
                }


            }
        }

        boolean promotion = move.pieceToPromote != 0;

        if (isPerfTest) {
            if (promotion) {
                squares[move.to] = move.pieceToPromote + currentMove;
            } else {
                squares[move.to] = squares[move.from];
            }
        } else {
            if (promotion) {
                int chosenPiece = 0;

                while (chosenPiece == 0) {
                    System.out.println("Enter piece you want promote to. " + (currentMove == 8 ? "(N, B, R, Q)" : "(n, b, r, q)"));
                    Scanner sc = new Scanner(System.in);
                    String input = sc.nextLine();
                    input = input.toLowerCase();
                    if (input.equals("n") || input.equals("b") || input.equals("r") || input.equals("q")) {
                        char inputChar = input.charAt(0);
                        switch (inputChar) {
                            case 'n' -> chosenPiece = Piece.Knight;
                            case 'b' -> chosenPiece = Piece.Bishop;
                            case 'r' -> chosenPiece = Piece.Rook;
                            case 'q' -> chosenPiece = Piece.Queen;
                        }
                    }
                }

                squares[move.to] = chosenPiece + currentMove;
            } else {
                squares[move.to] = squares[move.from];

            }

        }

        squares[move.from] = 0;

        if (move.from == move.to) {
            squares[move.from] = 6 + currentMove;
        }

        currentMove = currentMove == 8 ? 16 : 8;

        moveHistory.push(move);

        castleRightsHistory.push(new boolean[]{whiteKingsideCastlingRights, whiteQueensideCastlingRights, blackKingsideCastlingRights, blackQueensideCastlingRights});
    }

    Move getLastMove() {
        if (moveHistory.empty()) {
            return null;
        }
        return moveHistory.peek();
    }

    ArrayList<Move> calculateAllPossibleMoves(int site, boolean skipKing) {

        ArrayList<Move> listOfAllPossibleMoves = new ArrayList<>();

        for (int i = 0; i <= 63; i++) {

            if (getColor(squares[i]) != site || squares[i] == 0) continue;

            int color = getColor(squares[i]);
            int piece = squares[i] - color;

            int multiplayer = color == 8 ? 1 : -1;
            int add = color == 8 ? 0 : 1;

            int rank = i / 8 + 1;

            int distanceFromRight = rank * 8 - 1 - i;
            int distanceFromLeft = i - ((rank - 1) * 8);

            int distanceFromTop = i / 8;
            int distanceFromDown = 7 - distanceFromTop;

            int file, destinationFile;

            file = (i + 8) % 8 + 1;

            switch (piece) {
                case Piece.Pawn -> {

                    ArrayList<Move> movesToAdd = new ArrayList<>();

                    if (rank != 8 && rank != 1) {

                        if (squares[i - (8 * multiplayer)] == 0) {
                            movesToAdd.add(new Move(i, i - (8 * multiplayer)));

                            if (8 - i / 8 == color - 8 + add + (2 * multiplayer)) {
                                if (squares[i - (16 * multiplayer)] == 0) {
                                    movesToAdd.add(new Move(i, i - (16 * multiplayer)));
                                }
                            }
                        }

                        if (i - (9 * multiplayer) >= 0 && i - (9 * multiplayer) < 64) {
                            if (squares[i - (9 * multiplayer)] != 0 && getColor(squares[i - (9 * multiplayer)]) != currentMove) {


                                destinationFile = (i - (9 * multiplayer) + 8) % 8 + 1;
                                if (Math.abs(destinationFile - file) <= 2) {
                                    movesToAdd.add(new Move(i, i - (9 * multiplayer)));

                                }
                            }
                        }

                        if (i - (7 * multiplayer) >= 0 && i - (7 * multiplayer) < 64) {
                            if (squares[i - (7 * multiplayer)] != 0 && getColor(squares[i - (7 * multiplayer)]) != currentMove) {

                                destinationFile = (i - (7 * multiplayer) + 8) % 8 + 1;
                                if (Math.abs(destinationFile - file) <= 2) {

                                    movesToAdd.add(new Move(i, i - (7 * multiplayer)));

                                }
                            }
                        }


                        if (getLastMove() != null) {
                            if (8 - i / 8 == color - 8 + add + (5 * multiplayer)) {
                                if (getLastMove().to == i - 1) {
                                    if (Math.abs(getLastMove().to - getLastMove().from) == 16 && getPiece(squares[getLastMove().to]) == Piece.Pawn) {
                                        if (currentMove == 8) {

                                            destinationFile = ((i - 9) + 8) % 8 + 1;
                                            if (Math.abs(destinationFile - file) <= 2) {

                                                movesToAdd.add(new Move(i, i - 9));
                                            }
                                        } else {
                                            destinationFile = ((i + 7) + 8) % 8 + 1;
                                            if (Math.abs(destinationFile - file) <= 2) {

                                                movesToAdd.add(new Move(i, i + 7));
                                            }
                                        }
                                    }
                                }

                                if (getLastMove().to == i + 1) {
                                    if (Math.abs(getLastMove().to - getLastMove().from) == 16 && getPiece(squares[getLastMove().to]) == Piece.Pawn) {
                                        if (currentMove == 8) {

                                            destinationFile = ((i - 7) + 8) % 8 + 1;
                                            if (Math.abs(destinationFile - file) <= 2) {
                                                movesToAdd.add(new Move(i, i - 7));
                                            }

                                        } else {
                                            destinationFile = ((i + 9) + 8) % 8 + 1;
                                            if (Math.abs(destinationFile - file) <= 2) {
                                                movesToAdd.add(new Move(i, i + 9));
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        for (Move move : movesToAdd) {

                            int destinationRank = move.to / 8 + 1;

                            if (destinationRank == 1 || destinationRank == 8) {

                                for (int b = 2; b <= 5; b++) {
                                    listOfAllPossibleMoves.add(new Move(move.from, move.to, b));
                                }
                            } else {
                                listOfAllPossibleMoves.add(move);
                            }
                        }
                    }
                }
                case Piece.Bishop, Piece.Rook, Piece.Queen -> {
                    int startingIndex = 0, endingIndex = 7;
                    endingIndex = piece == 2 ? 3 : endingIndex;
                    startingIndex = piece == 4 ? 4 : startingIndex;
                    int[] offsets = {7, -9, -7, 9, 1, -1, 8, -8};
                    for (int o = startingIndex; o <= endingIndex; o++) {
                        int offset = offsets[o];
                        int distance = switch (offset) {
                            case -7, 9 ->
                                    offset == -7 ? Math.min(distanceFromRight, distanceFromTop) : Math.min(distanceFromRight, distanceFromDown);
                            case 7, -9 ->
                                    offset == 7 ? Math.min(distanceFromLeft, distanceFromDown) : Math.min(distanceFromLeft, distanceFromTop);
                            case 1 -> distanceFromRight;
                            case -1 -> distanceFromLeft;
                            case 8 -> distanceFromDown;
                            case -8 -> distanceFromTop;
                            default -> 0;
                        };

                        for (int n = 1; n <= distance; n++) {

                            if (i + offset * n > 63 || i + offset * n < 0) continue;

                            if (squares[i + offset * n] == 0) {
                                listOfAllPossibleMoves.add(new Move(i, i + offset * n));
                                continue;
                            }
                            if (getColor(squares[i + offset * n]) == currentMove) {
                                break;
                            }
                            if (getColor(squares[i + offset * n]) != currentMove) {
                                listOfAllPossibleMoves.add(new Move(i, i + offset * n));
                                break;
                            }

                        }
                    }
                }
                case Piece.Knight -> {
                    int[] knightOffsets = {-10, -17, -15, -6, 10, 17, 15, 6};
                    for (int offset : knightOffsets) {

                        destinationFile = ((i + offset) + 8) % 8 + 1;

                        if (i + offset > 63 || i + offset < 0) continue;

                        if (getColor(squares[i + offset]) == currentMove && squares[i + offset] != 0) continue;

                        if (Math.abs(destinationFile - file) > 2) continue;

                        listOfAllPossibleMoves.add(new Move(i, i + offset));
                    }
                }
                case Piece.King -> {
                    if (skipKing) continue;
                    int[] kingOffsets = {-1, 1, -8, 8, 7, -7, 9, -9};
                    for (int offset : kingOffsets) {

                        destinationFile = ((i + offset) + 8) % 8 + 1;

                        if (i + offset > 63 || i + offset < 0) continue;

                        if (getPiece(squares[i + offset]) == Piece.King) continue;

                        if (Math.abs(destinationFile - file) > 2) continue;

                        if (squares[i + offset] == 0) {
                            listOfAllPossibleMoves.add(new Move(i, i + offset));
                            continue;
                        }
                        if (getColor(squares[i + offset]) == currentMove) {
                            continue;
                        } else {
                            listOfAllPossibleMoves.add(new Move(i, i + offset));
                        }
                    }
                    if (!isKingChecked(new Move(i, i), true)) {

                        if (getColor(squares[i]) == Piece.White) {
                            if (castleRightsHistory.peek()[0]) {

                                if (squares[i + 1] == 0 && squares[i + 2] == 0) {
                                    if (!isKingChecked(new Move(i, i + 1), false) && !isKingChecked(new Move(i, i + 2), false)) {
                                        listOfAllPossibleMoves.add(new Move(i, i + 2));
                                    }
                                }
                            }

                            if (castleRightsHistory.peek()[1]) {
                                if (squares[i - 1] == 0 && squares[i - 2] == 0 && squares[i - 3] == 0) {
                                    if (!isKingChecked(new Move(i, i - 1), false) && !isKingChecked(new Move(i, i - 2), false)) {
                                        listOfAllPossibleMoves.add(new Move(i, i - 2));
                                    }
                                }
                            }
                        } else {
                            if (castleRightsHistory.peek()[2]) {
                                if (squares[i + 1] == 0 && squares[i + 2] == 0) {
                                    if (!isKingChecked(new Move(i, i + 1), false) && !isKingChecked(new Move(i, i + 2), false)) {
                                        listOfAllPossibleMoves.add(new Move(i, i + 2));
                                    }
                                }
                            }

                            if (castleRightsHistory.peek()[3]) {
                                if (squares[i - 1] == 0 && squares[i - 2] == 0 && squares[i - 3] == 0) {
                                    if (!isKingChecked(new Move(i, i - 1), false) && !isKingChecked(new Move(i, i - 2), false)) {
                                        listOfAllPossibleMoves.add(new Move(i, i - 2));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        ArrayList<Move> listOfAllPossibleMovesAfterCheck = new ArrayList<>();

        int[] kingOffsets = {-1, 1, -8, 8, 7, -7, 9, -9};

        for (Move move : listOfAllPossibleMoves) {

            if (getPiece(squares[move.from]) != Piece.King) {
                listOfAllPossibleMovesAfterCheck.add(move);
                continue;
            }

            makeMove(move, true);

            boolean isKingTouchingKing = false;

            int file = (move.to + 8) % 8 + 1;

            for (int i : kingOffsets) {

                int destinationFile = (move.to + i + 8) % 8 + 1;

                if (move.to + i < 0 || move.to + i > 63) continue;
                if (Math.abs(destinationFile - file) >= 7) continue;
                if (getPiece(squares[move.to + i]) == Piece.King) {
                    isKingTouchingKing = true;
                }

            }

            undoMove();

            if (!isKingTouchingKing) {
                listOfAllPossibleMovesAfterCheck.add(move);
            }

        }

        return listOfAllPossibleMovesAfterCheck;
    }


    private void loadBoardFromFen(String fen) {

        int squareToSet = 0;

        fen = fen.replaceAll("/", "");
        for (int i = 0; i < fen.length(); i++) {

            if (Character.isDigit(fen.charAt(i))) {
                squareToSet = squareToSet + Character.getNumericValue(fen.charAt(i));
                continue;
            }
            int color = Character.isLowerCase(fen.charAt(i)) ? Piece.Black : Piece.White;

            switch (Character.toLowerCase(fen.charAt(i))) {
                case 'p' -> squares[squareToSet] = Piece.Pawn + color;
                case 'n' -> squares[squareToSet] = Piece.Knight + color;
                case 'b' -> squares[squareToSet] = Piece.Bishop + color;
                case 'r' -> squares[squareToSet] = Piece.Rook + color;
                case 'q' -> squares[squareToSet] = Piece.Queen + color;
                case 'k' -> squares[squareToSet] = Piece.King + color;
            }
            squareToSet += 1;
        }


    }

    private static String generateFen(int[] board) {

        StringBuilder result = new StringBuilder();

        if (board.length != 64) return null;

        for (int i = 0; i <= 63; i++) {

            char addToFen = 'A';

            int piece = board[i] - ((float) board[i] / 8 > 2 ? Piece.Black : Piece.White);

            if (board[i] == 0) piece = 0;


            switch (piece) {
                case Piece.Pawn -> addToFen = 'p';
                case Piece.Knight -> addToFen = 'n';
                case Piece.Bishop -> addToFen = 'b';
                case Piece.Rook -> addToFen = 'r';
                case Piece.Queen -> addToFen = 'q';
                case Piece.King -> addToFen = 'k';
                case 0 -> addToFen = 'M';
            }

            addToFen = ((float) board[i] / 8 > 2 ? Piece.Black : Piece.White) == Piece.White ? Character.toUpperCase(addToFen) : addToFen;

            result.append(addToFen);

            if ((float) (i + 1) % 8 == 0.0f && i != 63) result.append("/");

        }


        for (String a : generateFen(result.toString()).split("M")) {

            if (Objects.equals(a, "")) continue;

            int b = Integer.parseInt(a);

            StringBuilder c = new StringBuilder();

            c.append("M".repeat(Math.max(0, b)));

            result = new StringBuilder(result.toString().replaceFirst(c.toString(), String.valueOf(c.length())));

        }

        return result.toString();
    }

    int getColor(int p) {
        return p != 0 ? (float) p / 8 > 2 ? Piece.Black : Piece.White : currentMove;
    }

    int getPiece(int p) {
        return p - getColor(p);
    }


    private static int indexOf(char[] arr, char val) {
        return IntStream.range(0, arr.length).filter(i -> arr[i] == val).findFirst().orElse(-1);
    }

    private static String generateFen(String s) {

        StringBuilder result = new StringBuilder();


        for (int i = 0; i < s.length(); i++) {

            // Counting occurrences of s[i]
            int count = 1;
            while (i + 1 < s.length()
                    && s.charAt(i)
                    == s.charAt(i + 1)) {
                i++;
                count++;
            }

            if (s.charAt(i) == 'M') {
                result.append(s.charAt(i)).append(count);
            }
        }

        return result.toString();

    }

    int[] getSquares() {
        return this.squares;
    }

}
