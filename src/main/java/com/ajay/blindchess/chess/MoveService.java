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

            if (board.getSideToMove() != playerSide) {

                return new MoveResult(
                        false,
                        "⏳ It is not your turn in Game **"
                                + game.getShortGameId()
                                + "**."
                );
            }

            Move parsedMove;

            try {

                MoveList moveList =
                        new MoveList(board.getFen());

                moveList.addSanMove(
                        san,
                        true,
                        true
                );

                parsedMove = moveList.removeLast();

            } catch (Exception e) {

                return new MoveResult(
                        false,
                        "❌ `" + san
                                + "` is invalid notation or cannot be played here.\n"
                                + "Please enter the move again."
                );
            }

            List<Move> legalMoves = board.legalMoves();

            if (!legalMoves.contains(parsedMove)) {

                return new MoveResult(
                        false,
                        "❌ `" + san
                                + "` is illegal in Game **"
                                + game.getShortGameId()
                                + "**.\nPlease enter the move again."
                );
            }

            boolean moved =
                    board.doMove(parsedMove, true);

            if (!moved) {

                return new MoveResult(
                        false,
                        "❌ That move could not be played."
                );
            }

            game.addMove(san);

            String nextPlayer =
                    board.getSideToMove() == Side.WHITE
                            ? "<@" + game.getWhitePlayerId() + ">"
                            : "<@" + game.getBlackPlayerId() + ">";

            return new MoveResult(
                    true,
                    "♟️ **" + san + "** played"
                            + " • Game **"
                            + game.getShortGameId()
                            + "**\n"
                            + nextPlayer
                            + " to move."
            );
        }
    }
}