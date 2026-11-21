package com.ajay.blindchess.board;

public class BoardRenderer {

    private static final String WHITE_SQUARE = "⬜";
    private static final String BLACK_SQUARE = "⬛";

    private BoardRenderer() {
    }

    public static String renderInitialBoard(
            String whitePlayerId,
            String blackPlayerId
    ) {

        StringBuilder board = new StringBuilder();

        board.append("<@")
                .append(blackPlayerId)
                .append(">\n");

        board.append("♜♞♝♛♚♝♞♜\n");
        board.append("♟♟♟♟♟♟♟♟\n");

        for (int rank = 6; rank >= 3; rank--) {

            for (int file = 0; file < 8; file++) {

                boolean lightSquare =
                        (rank + file) % 2 == 0;

                board.append(
                        lightSquare
                                ? WHITE_SQUARE
                                : BLACK_SQUARE
                );
            }

            board.append("\n");
        }

        board.append("♙♙♙♙♙♙♙♙\n");
        board.append("♖♘♗♕♔♗♘♖\n");

        board.append("<@")
                .append(whitePlayerId)
                .append(">");

        return board.toString();
    }

    public static String renderHiddenBoard() {

        StringBuilder board = new StringBuilder();

        for (int rank = 8; rank >= 1; rank--) {

            for (int file = 0; file < 8; file++) {

                boolean lightSquare =
                        (rank + file) % 2 == 0;

                board.append(
                        lightSquare
                                ? WHITE_SQUARE
                                : BLACK_SQUARE
                );
            }

            board.append("\n");
        }

        return board.toString();
    }
}