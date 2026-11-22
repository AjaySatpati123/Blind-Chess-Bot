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

    private final List<Integer> whiteShowBoardHistory;
    private final List<Integer> blackShowBoardHistory;

    private boolean active;

    private double whiteScore;
    private double blackScore;

    private String endReason;

    private final long createdAtMillis;
    private long lastActivityMillis;

    // =========================
    // NEW GAME
    // =========================

    public GameSession(
            String channelId,
            String whitePlayerId,
            String blackPlayerId
    ) {

        long now = System.currentTimeMillis();

        this.gameId =
                UUID.randomUUID().toString();

        this.channelId = channelId;

        this.whitePlayerId = whitePlayerId;
        this.blackPlayerId = blackPlayerId;

        this.board = new Board();

        this.moveHistory =
                new ArrayList<>();

        this.whiteShowBoardCount = 0;
        this.blackShowBoardCount = 0;

        this.whiteShowBoardHistory =
                new ArrayList<>();

        this.blackShowBoardHistory =
                new ArrayList<>();

        this.active = true;

        this.whiteScore = 0;
        this.blackScore = 0;

        this.endReason = null;

        this.createdAtMillis = now;
        this.lastActivityMillis = now;
    }

    // =========================
    // RESTORED GAME
    // =========================

    public GameSession(
            String gameId,
            String channelId,
            String whitePlayerId,
            String blackPlayerId,
            Board board,
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

        this.gameId = gameId;
        this.channelId = channelId;

        this.whitePlayerId = whitePlayerId;
        this.blackPlayerId = blackPlayerId;

        this.board = board;

        this.moveHistory =
                new ArrayList<>(moveHistory);

        this.whiteShowBoardCount =
                whiteShowBoardCount;

        this.blackShowBoardCount =
                blackShowBoardCount;

        this.whiteShowBoardHistory =
                new ArrayList<>(whiteShowBoardHistory);

        this.blackShowBoardHistory =
                new ArrayList<>(blackShowBoardHistory);

        this.active = active;

        this.whiteScore = whiteScore;
        this.blackScore = blackScore;

        this.endReason = endReason;

        this.createdAtMillis = createdAtMillis;
        this.lastActivityMillis = lastActivityMillis;
    }

    public String getGameId() {
        return gameId;
    }

    public String getShortGameId() {
        return gameId
                .substring(0, 6)
                .toUpperCase();
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
        return Collections.unmodifiableList(
                moveHistory
        );
    }

    public void addMove(String move) {

        moveHistory.add(move);

        touch();
    }

    public String getFormattedMoveHistory() {

        if (moveHistory.isEmpty()) {
            return "No moves.";
        }

        StringBuilder history =
                new StringBuilder();

        for (
                int i = 0;
                i < moveHistory.size();
                i += 2
        ) {

            int moveNumber =
                    (i / 2) + 1;

            history.append(moveNumber)
                    .append(". ")
                    .append(
                            moveHistory.get(i)
                    );

            if (i + 1 < moveHistory.size()) {

                history.append(" ")
                        .append(
                                moveHistory.get(i + 1)
                        );
            }

            history.append("\n");
        }

        return history.toString();
    }

    public boolean hasPlayer(
            String playerId
    ) {

        return whitePlayerId.equals(playerId)
                || blackPlayerId.equals(playerId);
    }

    public boolean isWhite(
            String playerId
    ) {

        return whitePlayerId.equals(playerId);
    }

    public boolean isBlack(
            String playerId
    ) {

        return blackPlayerId.equals(playerId);
    }

    public boolean isActive() {
        return active;
    }

    public void endGame() {

        active = false;

        touch();
    }

    public void finishGame(
            double whiteScore,
            double blackScore,
            String endReason
    ) {

        this.whiteScore = whiteScore;
        this.blackScore = blackScore;

        this.endReason = endReason;

        this.active = false;

        touch();
    }

    public double getWhiteScore() {
        return whiteScore;
    }

    public double getBlackScore() {
        return blackScore;
    }

    public String getEndReason() {
        return endReason;
    }

    public int getWhiteShowBoardCount() {
        return whiteShowBoardCount;
    }

    public int getBlackShowBoardCount() {
        return blackShowBoardCount;
    }

    public List<Integer> getWhiteShowBoardHistory() {

        return Collections.unmodifiableList(
                whiteShowBoardHistory
        );
    }

    public List<Integer> getBlackShowBoardHistory() {

        return Collections.unmodifiableList(
                blackShowBoardHistory
        );
    }

    public void recordShowBoard(
            String playerId
    ) {

        int currentPly =
                moveHistory.size();

        if (isWhite(playerId)) {

            whiteShowBoardCount++;

            whiteShowBoardHistory.add(
                    currentPly
            );

        } else if (isBlack(playerId)) {

            blackShowBoardCount++;

            blackShowBoardHistory.add(
                    currentPly
            );
        }

        touch();
    }

    public String getFormattedShowBoardHistory(
            String playerId
    ) {

        List<Integer> history;

        if (isWhite(playerId)) {

            history =
                    whiteShowBoardHistory;

        } else if (isBlack(playerId)) {

            history =
                    blackShowBoardHistory;

        } else {

            return "";
        }

        if (history.isEmpty()) {
            return "0 reveals";
        }

        StringBuilder result =
                new StringBuilder();

        result.append(history.size())
                .append(
                        history.size() == 1
                                ? " reveal"
                                : " reveals"
                );

        for (Integer ply : history) {

            result.append("\n• ")
                    .append(
                            getRevealLocation(ply)
                    );
        }

        return result.toString();
    }

    private String getRevealLocation(
            int ply
    ) {

        if (ply == 0) {
            return "Before the first move";
        }

        int moveIndex =
                ply - 1;

        if (moveIndex >= moveHistory.size()) {

            return "After move " + ply;
        }

        String move =
                moveHistory.get(moveIndex);

        int moveNumber =
                (moveIndex / 2) + 1;

        boolean whiteMove =
                moveIndex % 2 == 0;

        if (whiteMove) {

            return "After "
                    + moveNumber
                    + "."
                    + move;
        }

        return "After "
                + moveNumber
                + "..."
                + move;
    }

    public long getCreatedAtMillis() {
        return createdAtMillis;
    }

    public long getLastActivityMillis() {
        return lastActivityMillis;
    }

    public void touch() {

        this.lastActivityMillis =
                System.currentTimeMillis();
    }
}