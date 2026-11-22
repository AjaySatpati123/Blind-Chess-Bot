package com.ajay.blindchess.persistence;

import com.ajay.blindchess.game.GameSession;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.move.Move;
import com.github.bhlangonijr.chesslib.move.MoveList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class GamePersistenceService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(
                    GamePersistenceService.class
            );

    private static final Path DATA_DIRECTORY =
            Path.of("data");

    private static final Path GAME_FILE =
            DATA_DIRECTORY.resolve(
                    "games.json"
            );

    private static final Path TEMP_FILE =
            DATA_DIRECTORY.resolve(
                    "games.tmp.json"
            );

    private final ObjectMapper objectMapper;

    public GamePersistenceService() {

        objectMapper =
                new ObjectMapper();

        objectMapper.enable(
                SerializationFeature.INDENT_OUTPUT
        );
    }

    // ==========================================================
    // SAVE
    // ==========================================================

    public synchronized void save(
            Collection<GameSession> games
    ) {

        try {

            Files.createDirectories(
                    DATA_DIRECTORY
            );

            List<GameSnapshot> snapshots =
                    games.stream()
                            .map(GameSnapshot::from)
                            .toList();

            objectMapper.writeValue(
                    TEMP_FILE.toFile(),
                    snapshots
            );

            try {

                Files.move(
                        TEMP_FILE,
                        GAME_FILE,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE
                );

            } catch (IOException atomicMoveFailed) {

                Files.move(
                        TEMP_FILE,
                        GAME_FILE,
                        StandardCopyOption.REPLACE_EXISTING
                );
            }

        } catch (Exception e) {

            LOGGER.error(
                    "Could not save games.",
                    e
            );
        }
    }

    // ==========================================================
    // LOAD
    // ==========================================================

    public synchronized List<GameSession> load() {

        if (!Files.exists(GAME_FILE)) {

            LOGGER.info(
                    "No games.json found. Starting with no saved games."
            );

            return new ArrayList<>();
        }

        try {

            List<GameSnapshot> snapshots =
                    objectMapper.readValue(
                            GAME_FILE.toFile(),
                            new TypeReference<>() {
                            }
                    );

            List<GameSession> games =
                    new ArrayList<>();

            for (GameSnapshot snapshot : snapshots) {

                try {

                    GameSession game =
                            restoreGame(snapshot);

                    games.add(game);

                    LOGGER.info(
                            "Restored game {}",
                            game.getShortGameId()
                    );

                } catch (Exception e) {

                    LOGGER.error(
                            "Could not restore game {}",
                            snapshot.gameId(),
                            e
                    );
                }
            }

            return games;

        } catch (Exception e) {

            LOGGER.error(
                    "Could not read games.json.",
                    e
            );

            return new ArrayList<>();
        }
    }

    // ==========================================================
    // RESTORE ONE GAME
    // ==========================================================

    private GameSession restoreGame(
            GameSnapshot snapshot
    ) {

        Board board =
                rebuildBoard(
                        snapshot.moveHistory()
                );

        /*
         * Verify that replaying the moves produced
         * exactly the same board that was saved.
         */
        if (snapshot.fen() != null
                && !snapshot.fen().equals(
                board.getFen()
        )) {

            throw new IllegalStateException(
                    "Saved FEN does not match replayed moves.\n"
                            + "Saved: "
                            + snapshot.fen()
                            + "\nRestored: "
                            + board.getFen()
            );
        }

        return new GameSession(

                snapshot.gameId(),

                snapshot.channelId(),

                snapshot.whitePlayerId(),
                snapshot.blackPlayerId(),

                board,

                snapshot.moveHistory(),

                snapshot.whiteShowBoardCount(),
                snapshot.blackShowBoardCount(),

                snapshot.whiteShowBoardHistory(),
                snapshot.blackShowBoardHistory(),

                snapshot.active(),

                snapshot.whiteScore(),
                snapshot.blackScore(),

                snapshot.endReason(),

                snapshot.createdAtMillis(),
                snapshot.lastActivityMillis()
        );
    }

    // ==========================================================
    // REPLAY MOVES
    // ==========================================================

    private Board rebuildBoard(
            List<String> moves
    ) {

        Board board =
                new Board();

        for (String san : moves) {

            MoveList moveList =
                    new MoveList(
                            board.getFen()
                    );

            moveList.addSanMove(
                    san,
                    true,
                    true
            );

            Move move =
                    moveList.get(
                            moveList.size() - 1
                    );

            if (!board.legalMoves()
                    .contains(move)) {

                throw new IllegalStateException(
                        "Illegal saved move: "
                                + san
                );
            }

            boolean success =
                    board.doMove(
                            move,
                            true
                    );

            if (!success) {

                throw new IllegalStateException(
                        "Could not restore move: "
                                + san
                );
            }
        }

        return board;
    }
}