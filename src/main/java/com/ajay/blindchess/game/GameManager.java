package com.ajay.blindchess.game;

import com.ajay.blindchess.persistence.GamePersistenceService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GameManager {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(
                    GameManager.class
            );

    private static final GameManager INSTANCE =
            new GameManager();

    private final Map<String, GameSession> games;

    private final GamePersistenceService persistenceService;

    private GameManager() {

        games =
                new ConcurrentHashMap<>();

        persistenceService =
                new GamePersistenceService();
    }

    public static GameManager getInstance() {
        return INSTANCE;
    }

    public void addGame(
            GameSession gameSession
    ) {

        games.put(
                gameSession.getGameId(),
                gameSession
        );

        saveNow();
    }

    public GameSession getGame(
            String gameId
    ) {

        return games.get(gameId);
    }

    public List<GameSession>
    getActiveGamesForPlayerInChannel(
            String playerId,
            String channelId
    ) {

        List<GameSession> result =
                new ArrayList<>();

        for (GameSession game : games.values()) {

            if (!game.isActive()) {
                continue;
            }

            if (!game.getChannelId()
                    .equals(channelId)) {
                continue;
            }

            if (!game.hasPlayer(playerId)) {
                continue;
            }

            result.add(game);
        }

        return result;
    }

    public List<GameSession> getAllGames() {

        return new ArrayList<>(
                games.values()
        );
    }

    public void removeGame(
            String gameId
    ) {

        games.remove(gameId);

        saveNow();
    }

    // ==========================================================
    // SAVE
    // ==========================================================

    public void saveNow() {

        persistenceService.save(
                games.values()
        );
    }

    // ==========================================================
    // LOAD
    // ==========================================================

    public void loadFromDisk() {

        List<GameSession> restored =
                persistenceService.load();

        games.clear();

        for (GameSession game : restored) {

            games.put(
                    game.getGameId(),
                    game
            );
        }

        long activeCount =
                games.values()
                        .stream()
                        .filter(GameSession::isActive)
                        .count();

        LOGGER.info(
                "Loaded {} games, {} active.",
                games.size(),
                activeCount
        );
    }
}