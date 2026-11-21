package com.ajay.blindchess.chess;

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

            if (!game.isActive()) {

                return new MoveResult(
                        false,
                        "❌ This game has already finished."
                );
            }

            Board board = game.getBoard();

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

            // Check turn
            if (board.getSideToMove() != playerSide) {

                return new MoveResult(
                        false,
                        "⏳ It is not your turn in Game **"
                                + game.getShortGameId()
                                + "**."
                );
            }

            Move parsedMove;

            // Parse SAN
            try {

                MoveList moveList =
                        new MoveList(board.getFen());

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
                                + "` is invalid notation or cannot be played here.\n"
                                + "Please enter the move again."
                );
            }

            // Check legal moves
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

            // Play move
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

                return new MoveResult(
                        true,
                        "♟️ **" + san + "** played"
                                + " • Game **"
                                + game.getShortGameId()
                                + "**\n\n"
                                + "♚ **CHECKMATE**\n\n"
                                + getResultText(game)
                                + "\n\n"
                                + "**Moves**\n```"
                                + game.getFormattedMoveHistory()
                                + "```"
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

                return new MoveResult(
                        true,
                        "♟️ **" + san + "** played"
                                + " • Game **"
                                + game.getShortGameId()
                                + "**\n\n"
                                + "🤝 **STALEMATE**\n\n"
                                + getResultText(game)
                                + "\n\n"
                                + "**Moves**\n```"
                                + game.getFormattedMoveHistory()
                                + "```"
                );
            }

            // =========================
            // OTHER DRAW
            // =========================

            if (board.isDraw()) {

                game.finishGame(
                        0.5,
                        0.5,
                        "Draw"
                );

                return new MoveResult(
                        true,
                        "♟️ **" + san + "** played"
                                + " • Game **"
                                + game.getShortGameId()
                                + "**\n\n"
                                + "🤝 **DRAW**\n\n"
                                + getResultText(game)
                                + "\n\n"
                                + "**Moves**\n```"
                                + game.getFormattedMoveHistory()
                                + "```"
                );
            }

            // =========================
            // NORMAL MOVE
            // =========================

            String nextPlayer;

            if (board.getSideToMove() == Side.WHITE) {

                nextPlayer =
                        "<@" + game.getWhitePlayerId() + ">";

            } else {

                nextPlayer =
                        "<@" + game.getBlackPlayerId() + ">";
            }

            String checkMessage = "";

            if (board.isKingAttacked()) {

                checkMessage =
                        "\n⚠️ **Check!**";
            }

            return new MoveResult(
                    true,
                    "♟️ **" + san + "** played"
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

    private static String getResultText(
            GameSession game
    ) {

        return "<@" + game.getWhitePlayerId() + "> : "
                + formatScore(game.getWhiteScore())
                + "\n"
                + "<@" + game.getBlackPlayerId() + "> : "
                + formatScore(game.getBlackScore());
    }

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