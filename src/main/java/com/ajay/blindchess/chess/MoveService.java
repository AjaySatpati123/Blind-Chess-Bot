package com.ajay.blindchess.chess;

import com.ajay.blindchess.game.GameManager;
import com.ajay.blindchess.game.GameSession;
import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.move.Move;
import com.github.bhlangonijr.chesslib.move.MoveList;

import java.util.List;

public class MoveService {

    public static MoveResult playMove(
            GameSession game,
            String playerId,
            String san
    ) {

        synchronized (game) {

            // =========================
            // GAME ALREADY FINISHED
            // =========================

            if (!game.isActive()) {

                return new MoveResult(
                        false,
                        "❌ This game has already finished."
                );
            }

            Board board = game.getBoard();

            // =========================
            // IDENTIFY PLAYER COLOUR
            // =========================

            Side playerSide;

            if (game.isWhite(playerId)) {

                playerSide = Side.WHITE;

            } else if (game.isBlack(playerId)) {

                playerSide = Side.BLACK;

            } else {

                return new MoveResult(
                        false,
                        "❌ You are not a player in this game."
                );
            }

            // =========================
            // CHECK TURN
            // =========================

            if (board.getSideToMove() != playerSide) {

                return new MoveResult(
                        false,
                        "⏳ It is not your turn in Game **"
                                + game.getShortGameId()
                                + "**."
                );
            }

            // =========================
            // PARSE SAN
            // =========================

            Move parsedMove;

            try {

                MoveList moveList =
                        new MoveList(
                                board.getFen()
                        );

                moveList.addSanMove(
                        san,
                        true,
                        true
                );

                parsedMove =
                        moveList.get(
                                moveList.size() - 1
                        );

            } catch (Exception e) {

                return new MoveResult(
                        false,
                        "❌ `" + san
                                + "` is invalid chess notation.\n"
                                + "Please enter the move again."
                );
            }

            // =========================
            // VALIDATE LEGAL MOVE
            // =========================

            List<Move> legalMoves =
                    board.legalMoves();

            if (!legalMoves.contains(parsedMove)) {

                return new MoveResult(
                        false,
                        "❌ `" + san
                                + "` is illegal in Game **"
                                + game.getShortGameId()
                                + "**.\n"
                                + "Please enter the move again."
                );
            }

            // =========================
            // PLAY MOVE
            // =========================

            boolean moved =
                    board.doMove(
                            parsedMove,
                            true
                    );

            if (!moved) {

                return new MoveResult(
                        false,
                        "❌ That move could not be played."
                );
            }

            // Store SAN in move history
            game.addMove(san);

            // =========================
            // CHECKMATE
            // =========================

            if (board.isMated()) {

                if (playerSide == Side.WHITE) {

                    game.finishGame(
                            1,
                            0,
                            "Checkmate"
                    );

                } else {

                    game.finishGame(
                            0,
                            1,
                            "Checkmate"
                    );
                }

                // Save completed state
                saveGames();

                return new MoveResult(
                        true,
                        "♟️ **"
                                + san
                                + "** played"
                                + " • Game **"
                                + game.getShortGameId()
                                + "**\n\n"
                                + "♚ **CHECKMATE**\n\n"
                                + getResultText(game)
                                + "\n\n"
                                + "**Moves**\n```"
                                + game.getFormattedMoveHistory()
                                + "```\n"
                                + getRevealHistoryText(game)
                );
            }

            // =========================
            // STALEMATE
            // =========================

            if (board.isStaleMate()) {

                game.finishGame(
                        0.5,
                        0.5,
                        "Stalemate"
                );

                saveGames();

                return new MoveResult(
                        true,
                        "♟️ **"
                                + san
                                + "** played"
                                + " • Game **"
                                + game.getShortGameId()
                                + "**\n\n"
                                + "🤝 **STALEMATE**\n\n"
                                + getResultText(game)
                                + "\n\n"
                                + "**Moves**\n```"
                                + game.getFormattedMoveHistory()
                                + "```\n"
                                + getRevealHistoryText(game)
                );
            }

            // =========================
            // OTHER AUTOMATIC DRAW
            // =========================

            /*
             * Chesslib's isDraw() also handles states such as:
             *
             * - Threefold repetition
             * - 50-move rule
             * - Insufficient material
             *
             * Stalemate was already handled above so that
             * we can display a specific message.
             */

            if (board.isDraw()) {

                game.finishGame(
                        0.5,
                        0.5,
                        "Draw"
                );

                saveGames();

                return new MoveResult(
                        true,
                        "♟️ **"
                                + san
                                + "** played"
                                + " • Game **"
                                + game.getShortGameId()
                                + "**\n\n"
                                + "🤝 **DRAW**\n\n"
                                + getResultText(game)
                                + "\n\n"
                                + "**Moves**\n```"
                                + game.getFormattedMoveHistory()
                                + "```\n"
                                + getRevealHistoryText(game)
                );
            }

            // =========================
            // NORMAL MOVE
            // =========================

            String nextPlayer;

            if (board.getSideToMove()
                    == Side.WHITE) {

                nextPlayer =
                        "<@"
                                + game.getWhitePlayerId()
                                + ">";

            } else {

                nextPlayer =
                        "<@"
                                + game.getBlackPlayerId()
                                + ">";
            }

            String checkMessage = "";

            if (board.isKingAttacked()) {

                checkMessage =
                        "\n⚠️ **Check!**";
            }

            // Persist successful normal move
            saveGames();

            return new MoveResult(
                    true,
                    "♟️ **"
                            + san
                            + "** played"
                            + " • Game **"
                            + game.getShortGameId()
                            + "**"
                            + checkMessage
                            + "\n"
                            + nextPlayer
                            + " to move."
            );
        }
    }

    // ==========================================================
    // SAVE GAME STATE
    // ==========================================================

    private static void saveGames() {

        GameManager.getInstance()
                .saveNow();
    }

    // ==========================================================
    // RESULT TEXT
    // ==========================================================

    private static String getResultText(
            GameSession game
    ) {

        return "<@"
                + game.getWhitePlayerId()
                + "> : "
                + formatScore(
                game.getWhiteScore()
        )
                + "\n"
                + "<@"
                + game.getBlackPlayerId()
                + "> : "
                + formatScore(
                game.getBlackScore()
        );
    }

    // ==========================================================
    // SHOW BOARD HISTORY
    // ==========================================================

    private static String getRevealHistoryText(
            GameSession game
    ) {

        return "**Board Reveals**\n\n"
                + "<@"
                + game.getWhitePlayerId()
                + ">\n"
                + game.getFormattedShowBoardHistory(
                game.getWhitePlayerId()
        )
                + "\n\n"
                + "<@"
                + game.getBlackPlayerId()
                + ">\n"
                + game.getFormattedShowBoardHistory(
                game.getBlackPlayerId()
        );
    }

    // ==========================================================
    // SCORE FORMAT
    // ==========================================================

    private static String formatScore(
            double score
    ) {

        if (score == 1) {
            return "1";
        }

        if (score == 0) {
            return "0";
        }

        return "0.5";
    }
}