package com.ajay.blindchess.game;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Side;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class GameSession {

    private final String gameId;
    private final String channelId;

    private final String whitePlayerId;
    private final String blackPlayerId;

    private final Board board;
    private final List<String> moveHistory;

    private int whiteShowBoardCount;
    private int blackShowBoardCount;

    private boolean active;

    public GameSession(
            String channelId,
            String whitePlayerId,
            String blackPlayerId
    ) {
        this.gameId = UUID.randomUUID().toString();

        this.channelId = channelId;
        this.whitePlayerId = whitePlayerId;
        this.blackPlayerId = blackPlayerId;

        this.board = new Board();
        this.moveHistory = new ArrayList<>();

        this.whiteShowBoardCount = 0;
        this.blackShowBoardCount = 0;

        this.active = true;
    }

    public String getGameId() {
        return gameId;
    }

    public String getShortGameId() {
        return gameId.substring(0, 6).toUpperCase();
    }

    public String getChannelId() {
        return channelId;
    }

    public String getWhitePlayerId() {
        return whitePlayerId;
    }

    public String getBlackPlayerId() {
        return blackPlayerId;
    }

    public Board getBoard() {
        return board;
    }

    public Side getSideToMove() {
        return board.getSideToMove();
    }

    public List<String> getMoveHistory() {
        return Collections.unmodifiableList(moveHistory);
    }

    public void addMove(String move) {
        moveHistory.add(move);
    }

    public int getWhiteShowBoardCount() {
        return whiteShowBoardCount;
    }

    public int getBlackShowBoardCount() {
        return blackShowBoardCount;
    }

    public void incrementWhiteShowBoardCount() {
        whiteShowBoardCount++;
    }

    public void incrementBlackShowBoardCount() {
        blackShowBoardCount++;
    }

    public boolean isActive() {
        return active;
    }

    public void endGame() {
        active = false;
    }

    public boolean hasPlayer(String playerId) {
        return whitePlayerId.equals(playerId)
                || blackPlayerId.equals(playerId);
    }

    public boolean isWhite(String playerId) {
        return whitePlayerId.equals(playerId);
    }

    public boolean isBlack(String playerId) {
        return blackPlayerId.equals(playerId);
    }
}