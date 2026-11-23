package com.ajay.blindchess.chess;

import com.ajay.blindchess.game.GameManager;
import com.ajay.blindchess.game.GameSession;

import com.ajay.blindchess.game.SanUtils;
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

            // ==================================================
            // GAME ALREADY FINISHED
            // ==================================================

            if (!game.isActive()) {

                return new MoveResult(
                        false,
                        "❌ Game **"
                                + game.getShortGameId()
                                + "** has already finished."
                );
            }

            Board board =
                    game.getBoard();

            // ==================================================
            // IDENTIFY PLAYER COLOUR
            // ==================================================

            Side playerSide;

            if (game.isWhite(
                    playerId
            )) {

                playerSide =
                        Side.WHITE;

            } else if (game.isBlack(
                    playerId
            )) {

                playerSide =
                        Side.BLACK;

            } else {

                return new MoveResult(
                        false,
                        "❌ You are not a player in this game."
                );
            }

            // ==================================================
            // CHECK TURN
            // ==================================================

            if (board.getSideToMove()
                    != playerSide) {

                return new MoveResult(
                        false,
                        "⏳ It is not your turn in Game **"
                                + game.getShortGameId()
                                + "**."
                );
            }

            // ==================================================
            // BASIC SAN VALIDATION
            // ==================================================

            if (!SanUtils.hasValidSanShape(
                    san
            )) {

                return new MoveResult(
                        false,
                        "❌ `"
                                + san
                                + "` is invalid chess notation.\n"
                                + "Examples: `e4`, `Nf3`, `Nbd2`, "
                                + "`R1e2`, `exd5`, `e8=Q`, `O-O`."
                );
            }

            // ==================================================
            // NORMALIZE + PARSE SAN
            // ==================================================

            Move parsedMove = null;

            String acceptedSan = null;

            List<String> candidates =
                    SanUtils.normalizedCandidates(
                            san
                    );

            for (String candidate :
                    candidates) {

                try {

                    MoveList moveList =
                            new MoveList(
                                    board.getFen()
                            );

                    moveList.addSanMove(
                            candidate,
                            true,
                            true
                    );

                    if (moveList.isEmpty()) {
                        continue;
                    }

                    Move candidateMove =
                            moveList.getLast();

                    List<Move> legalMoves =
                            board.legalMoves();

                    if (!legalMoves.contains(
                            candidateMove
                    )) {

                        continue;
                    }

                    parsedMove =
                            candidateMove;

                    acceptedSan =
                            candidate;

                    break;

                } catch (Exception ignored) {

                    /*
                     * Try next possible interpretation.
                     *
                     * Especially important for:
                     *
                     * b4   -> pawn
                     * bf4  -> Bishop
                     * bxc5 -> potentially ambiguous lowercase input
                     */
                }
            }

            // ==================================================
            // ILLEGAL MOVE
            // ==================================================

            if (parsedMove == null) {

                return new MoveResult(
                        false,
                        "🚫 `"
                                + san
                                + "` is not legal in Game **"
                                + game.getShortGameId()
                                + "**.\n"
                                + "Please enter the move again."
                );
            }

            // ==================================================
            // PLAY MOVE
            // ==================================================

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

            // Store normalized SAN
            game.addMove(
                    acceptedSan
            );

            // ==================================================
            // CHECKMATE
            // ==================================================

            if (board.isMated()) {

                if (playerSide
                        == Side.WHITE) {

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

                saveGames();

                return new MoveResult(
                        true,
                        "♟️ **"
                                + acceptedSan
                                + "** played"
                                + " • Game **"
                                + game.getShortGameId()
                                + "**\n\n"

                                + "♚ **CHECKMATE**\n\n"

                                + getResultText(
                                game
                        )

                                + "\n\n"

                                + "**Moves**\n```"
                                + game.getFormattedMoveHistory()
                                + "```\n"

                                + getRevealHistoryText(
                                game
                        )
                );
            }

            // ==================================================
            // STALEMATE
            // ==================================================

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
                                + acceptedSan
                                + "** played"
                                + " • Game **"
                                + game.getShortGameId()
                                + "**\n\n"

                                + "🤝 **DRAW — STALEMATE**\n\n"

                                + getResultText(
                                game
                        )

                                + "\n\n"

                                + "**Moves**\n```"
                                + game.getFormattedMoveHistory()
                                + "```\n"

                                + getRevealHistoryText(
                                game
                        )
                );
            }

            // ==================================================
            // THREEFOLD REPETITION
            // ==================================================

            if (board.isRepetition()) {

                game.finishGame(
                        0.5,
                        0.5,
                        "Threefold Repetition"
                );

                saveGames();

                return new MoveResult(
                        true,
                        "♟️ **"
                                + acceptedSan
                                + "** played"
                                + " • Game **"
                                + game.getShortGameId()
                                + "**\n\n"

                                + "🤝 **DRAW — THREEFOLD REPETITION**\n\n"

                                + getResultText(
                                game
                        )

                                + "\n\n"

                                + "**Moves**\n```"
                                + game.getFormattedMoveHistory()
                                + "```\n"

                                + getRevealHistoryText(
                                game
                        )
                );
            }

            // ==================================================
            // 50-MOVE RULE
            // ==================================================

            /*
             * 50 moves by both players =
             * 100 half-moves.
             *
             * Chesslib resets this counter after
             * a pawn move or capture.
             */
            if (board.getHalfMoveCounter()
                    >= 100) {

                game.finishGame(
                        0.5,
                        0.5,
                        "50-Move Rule"
                );

                saveGames();

                return new MoveResult(
                        true,
                        "♟️ **"
                                + acceptedSan
                                + "** played"
                                + " • Game **"
                                + game.getShortGameId()
                                + "**\n\n"

                                + "🤝 **DRAW — 50-MOVE RULE**\n\n"

                                + getResultText(
                                game
                        )

                                + "\n\n"

                                + "**Moves**\n```"
                                + game.getFormattedMoveHistory()
                                + "```\n"

                                + getRevealHistoryText(
                                game
                        )
                );
            }

            // ==================================================
            // INSUFFICIENT MATERIAL
            // ==================================================

            if (board.isInsufficientMaterial()) {

                game.finishGame(
                        0.5,
                        0.5,
                        "Insufficient Material"
                );

                saveGames();

                return new MoveResult(
                        true,
                        "♟️ **"
                                + acceptedSan
                                + "** played"
                                + " • Game **"
                                + game.getShortGameId()
                                + "**\n\n"

                                + "🤝 **DRAW — INSUFFICIENT MATERIAL**\n\n"

                                + getResultText(
                                game
                        )

                                + "\n\n"

                                + "**Moves**\n```"
                                + game.getFormattedMoveHistory()
                                + "```\n"

                                + getRevealHistoryText(
                                game
                        )
                );
            }

            // ==================================================
            // NORMAL MOVE
            // ==================================================

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

            // ==================================================
            // CHECK
            // ==================================================

            String checkMessage =
                    "";

            if (board.isKingAttacked()) {

                checkMessage =
                        "\n⚠️ **Check!**";
            }

            // ==================================================
            // SAVE SUCCESSFUL MOVE
            // ==================================================

            saveGames();

            // ==================================================
            // RESPONSE
            // ==================================================

            return new MoveResult(
                    true,
                    "♟️ **"
                            + acceptedSan
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