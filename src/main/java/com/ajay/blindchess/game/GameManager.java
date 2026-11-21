package com.ajay.blindchess.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GameManager {

    private static final GameManager INSTANCE = new GameManager();

    private final Map<String, GameSession> games;

    private GameManager() {
        games = new ConcurrentHashMap<>();
    }

    public static GameManager getInstance() {
        return INSTANCE;
    }

    public void addGame(GameSession gameSession) {

        games.put(
                gameSession.getGameId(),
                gameSession
        );
    }

    public GameSession getGame(String gameId) {
        return games.get(gameId);
    }

    public List<GameSession> getActiveGamesForPlayerInChannel(
            String playerId,
            String channelId
    ) {

        List<GameSession> result = new ArrayList<>();

        for (GameSession game : games.values()) {

            if (!game.isActive()) {
                continue;
            }

            if (!game.getChannelId().equals(channelId)) {
                continue;
            }

            if (!game.hasPlayer(playerId)) {
                continue;
            }

            result.add(game);
        }

        return result;
    }

    public void removeGame(String gameId) {
        games.remove(gameId);
    }
}