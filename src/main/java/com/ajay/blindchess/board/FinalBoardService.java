package com.ajay.blindchess.board;

import com.ajay.blindchess.game.GameSession;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.utils.FileUpload;

import java.util.concurrent.CompletableFuture;

public final class FinalBoardService {

    private FinalBoardService() {
    }

    // ==========================================================
    // PUBLIC ENTRY POINT
    // ==========================================================

    public static void sendFinalBoard(
            MessageChannel channel,
            Guild guild,
            GameSession game
    ) {

        /*
         * Even if guild information is unavailable,
         * we can still render the chess board using
         * fallback player names.
         */
        if (guild == null) {

            sendBoard(
                    channel,
                    game,
                    "White Player",
                    "Black Player"
            );

            return;
        }

        // ======================================================
        // GET PLAYER NAMES
        // ======================================================

        CompletableFuture<String> whiteNameFuture =
                retrievePlayerName(
                        guild,
                        game.getWhitePlayerId(),
                        "White Player"
                );

        CompletableFuture<String> blackNameFuture =
                retrievePlayerName(
                        guild,
                        game.getBlackPlayerId(),
                        "Black Player"
                );

        /*
         * Both futures always return a name because
         * retrievePlayerName() has fallback handling.
         */
        whiteNameFuture
                .thenCombine(
                        blackNameFuture,
                        PlayerNames::new
                )
                .thenAccept(
                        names ->
                                sendBoard(
                                        channel,
                                        game,
                                        names.whiteName(),
                                        names.blackName()
                                )
                )
                .exceptionally(error -> {

                    /*
                     * Final safety net.
                     *
                     * Even if something unexpected happens
                     * while retrieving names, still send
                     * the final board.
                     */
                    sendBoard(
                            channel,
                            game,
                            "White Player",
                            "Black Player"
                    );

                    return null;
                });
    }

    // ==========================================================
    // RETRIEVE PLAYER NAME
    // ==========================================================

    private static CompletableFuture<String> retrievePlayerName(
            Guild guild,
            String playerId,
            String fallbackName
    ) {

        // ======================================================
        // FIRST: MEMBER CACHE
        // ======================================================

        Member cachedMember =
                guild.getMemberById(
                        playerId
                );

        if (cachedMember != null) {

            return CompletableFuture.completedFuture(
                    cachedMember.getEffectiveName()
            );
        }

        // ======================================================
        // SECOND: USER CACHE
        // ======================================================

        /*
         * The player might have left the server,
         * but JDA may still know the Discord user.
         */
        User cachedUser =
                guild.getJDA()
                        .getUserById(
                                playerId
                        );

        String cachedFallback =
                cachedUser != null
                        ? cachedUser.getName()
                        : fallbackName;

        // ======================================================
        // THIRD: ASYNC MEMBER RETRIEVAL
        // ======================================================

        return guild.retrieveMemberById(
                        playerId
                )
                .submit()
                .thenApply(
                        Member::getEffectiveName
                )
                .exceptionally(
                        error ->
                                cachedFallback
                );
    }

    // ==========================================================
    // SEND FINAL BOARD
    // ==========================================================

    private static void sendBoard(
            MessageChannel channel,
            GameSession game,
            String whitePlayerName,
            String blackPlayerName
    ) {

        try {

            byte[] boardImage =
                    BoardRenderer.renderBoard(
                            game.getBoard(),
                            blackPlayerName,
                            whitePlayerName
                    );

            FileUpload file =
                    FileUpload.fromData(
                            boardImage,
                            "final-board-"
                                    + game.getShortGameId()
                                    + ".png"
                    );

            channel.sendMessage(
                            "🏁 **Final Position**"
                                    + " • Game **"
                                    + game.getShortGameId()
                                    + "**"
                    )
                    .addFiles(
                            file
                    )
                    .queue(
                            ignored -> {
                                // Successfully sent.
                            },
                            error -> {

                                /*
                                 * There isn't much we can do here
                                 * if Discord itself rejects the
                                 * final message.
                                 *
                                 * Proper SLF4J logging will be
                                 * added in the logging cleanup.
                                 */
                                System.err.println(
                                        "Failed to send final board for Game "
                                                + game.getShortGameId()
                                                + ": "
                                                + error.getMessage()
                                );
                            }
                    );

        } catch (Exception e) {

            /*
             * If SVG/image rendering fails, still tell
             * the channel that the game ended and that
             * the board image could not be rendered.
             */
            channel.sendMessage(
                            "🏁 **Final Position**"
                                    + " • Game **"
                                    + game.getShortGameId()
                                    + "**\n"
                                    + "⚠️ The final-board image could not be rendered."
                    )
                    .queue();

            System.err.println(
                    "Failed to render final board for Game "
                            + game.getShortGameId()
                            + ": "
                            + e.getMessage()
            );
        }
    }

    // ==========================================================
    // PLAYER NAME PAIR
    // ==========================================================

    private record PlayerNames(
            String whiteName,
            String blackName
    ) {
    }
}