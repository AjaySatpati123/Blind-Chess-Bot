package com.ajay.blindchess.persistence;

import com.ajay.blindchess.game.GameSession;

import java.util.List;

public record GameSnapshot(

        String gameId,

        String channelId,

        String whitePlayerId,
        String blackPlayerId,

        String fen,

        List<String> moveHistory,

        int whiteShowBoardCount,
        int blackShowBoardCount,

        List<Integer> whiteShowBoardHistory,
        List<Integer> blackShowBoardHistory,

        boolean active,

        double whiteScore,
        double blackScore,

        String endReason,

        long createdAtMillis,
        long lastActivityMillis

) {

    public static GameSnapshot from(
            GameSession game
    ) {

        synchronized (game) {

            return new GameSnapshot(

                    game.getGameId(),

                    game.getChannelId(),

                    game.getWhitePlayerId(),
                    game.getBlackPlayerId(),

                    game.getBoard().getFen(),

                    List.copyOf(
                            game.getMoveHistory()
                    ),

                    game.getWhiteShowBoardCount(),
                    game.getBlackShowBoardCount(),

                    List.copyOf(
                            game.getWhiteShowBoardHistory()
                    ),

                    List.copyOf(
                            game.getBlackShowBoardHistory()
                    ),

                    game.isActive(),

                    game.getWhiteScore(),
                    game.getBlackScore(),

                    game.getEndReason(),

                    game.getCreatedAtMillis(),
                    game.getLastActivityMillis()
            );
        }
    }
}