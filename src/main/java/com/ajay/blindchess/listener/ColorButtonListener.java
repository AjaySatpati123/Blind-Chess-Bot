package com.ajay.blindchess.listener;

import com.ajay.blindchess.board.BoardRenderer;
import com.ajay.blindchess.game.GameManager;
import com.ajay.blindchess.game.GameSession;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.FileUpload;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

public class ColorButtonListener extends ListenerAdapter {

    @Override
    public void onButtonInteraction(
            ButtonInteractionEvent event
    ) {

        String componentId =
                event.getComponentId();

        // ======================================================
        // NOT A COLOUR BUTTON
        // ======================================================

        if (!componentId.startsWith("colour:")) {
            return;
        }

        String[] parts =
                componentId.split(":");

        if (parts.length != 4) {

            event.reply(
                            "❌ Invalid colour-selection interaction."
                    )
                    .setEphemeral(true)
                    .queue();

            return;
        }

        String colour =
                parts[1];

        String challengerId =
                parts[2];

        String opponentId =
                parts[3];

        // ======================================================
        // ONLY CHALLENGED PLAYER CAN CHOOSE
        // ======================================================

        if (!event.getUser()
                .getId()
                .equals(opponentId)) {

            event.reply(
                            "Only the challenged player can choose the colour."
                    )
                    .setEphemeral(true)
                    .queue();

            return;
        }

        // ======================================================
        // MUST BE INSIDE A SERVER
        // ======================================================

        Guild guild =
                event.getGuild();

        if (guild == null) {

            event.reply(
                            "❌ Blind Chess games can only be started inside a Discord server."
                    )
                    .setEphemeral(true)
                    .queue();

            return;
        }

        // ======================================================
        // DETERMINE COLOURS
        // ======================================================

        String whitePlayerId;
        String blackPlayerId;

        String extraMessage;

        switch (colour) {

            case "white" -> {

                whitePlayerId =
                        opponentId;

                blackPlayerId =
                        challengerId;

                extraMessage =
                        "";
            }

            case "black" -> {

                whitePlayerId =
                        challengerId;

                blackPlayerId =
                        opponentId;

                extraMessage =
                        "";
            }

            case "random" -> {

                boolean opponentGetsWhite =
                        ThreadLocalRandom.current()
                                .nextBoolean();

                if (opponentGetsWhite) {

                    whitePlayerId =
                            opponentId;

                    blackPlayerId =
                            challengerId;

                } else {

                    whitePlayerId =
                            challengerId;

                    blackPlayerId =
                            opponentId;
                }

                extraMessage =
                        "🎲 **Colours were randomly assigned!**\n\n";
            }

            default -> {

                event.reply(
                                "❌ Invalid colour selection."
                        )
                        .setEphemeral(true)
                        .queue();

                return;
            }
        }

        /*
         * IMPORTANT:
         *
         * Discord interactions should be acknowledged
         * immediately.
         *
         * From this point onward we do NOT use:
         *
         * .complete()
         *
         * for member retrieval.
         */
        event.deferEdit()
                .queue(
                        hook -> beginGameCreation(
                                hook,
                                event,
                                guild,
                                whitePlayerId,
                                blackPlayerId,
                                extraMessage
                        ),
                        error ->
                                System.err.println(
                                        "Failed to acknowledge colour interaction: "
                                                + error.getMessage()
                                )
                );
    }

    // ==========================================================
    // BEGIN GAME CREATION
    // ==========================================================

    private void beginGameCreation(
            InteractionHook hook,
            ButtonInteractionEvent event,
            Guild guild,
            String whitePlayerId,
            String blackPlayerId,
            String extraMessage
    ) {

        /*
         * Remove the colour buttons immediately.
         *
         * This also greatly reduces the chance of the
         * challenged player clicking another colour while
         * the board is still being rendered.
         */
        hook.editOriginal(
                        "♟️ Creating Blind Chess game..."
                )
                .setComponents()
                .queue(
                        ignored -> createGame(
                                hook,
                                event,
                                guild,
                                whitePlayerId,
                                blackPlayerId,
                                extraMessage
                        ),
                        error ->
                                System.err.println(
                                        "Failed to clear colour buttons: "
                                                + error.getMessage()
                                )
                );
    }

    // ==========================================================
    // CREATE GAME
    // ==========================================================

    private void createGame(
            InteractionHook hook,
            ButtonInteractionEvent event,
            Guild guild,
            String whitePlayerId,
            String blackPlayerId,
            String extraMessage
    ) {

        String channelId =
                event.getChannel()
                        .getId();

        GameSession gameSession =
                new GameSession(
                        channelId,
                        whitePlayerId,
                        blackPlayerId
                );

        /*
         * addGame() also persists the game.
         */
        GameManager.getInstance()
                .addGame(
                        gameSession
                );

        // ======================================================
        // RETRIEVE PLAYER NAMES ASYNCHRONOUSLY
        // ======================================================

        CompletableFuture<String> whiteNameFuture =
                retrievePlayerName(
                        guild,
                        whitePlayerId,
                        "White Player"
                );

        CompletableFuture<String> blackNameFuture =
                retrievePlayerName(
                        guild,
                        blackPlayerId,
                        "Black Player"
                );

        whiteNameFuture
                .thenCombine(
                        blackNameFuture,
                        PlayerNames::new
                )
                .thenAccept(
                        names ->
                                renderAndSendGame(
                                        hook,
                                        gameSession,
                                        whitePlayerId,
                                        blackPlayerId,
                                        names.whiteName(),
                                        names.blackName(),
                                        extraMessage
                                )
                )
                .exceptionally(error -> {

                    System.err.println(
                            "Failed while preparing game board: "
                                    + error.getMessage()
                    );

                    /*
                     * Even if retrieving the display names fails,
                     * still show the game using fallback names.
                     */
                    renderAndSendGame(
                            hook,
                            gameSession,
                            whitePlayerId,
                            blackPlayerId,
                            "White Player",
                            "Black Player",
                            extraMessage
                    );

                    return null;
                });
    }

    // ==========================================================
    // RETRIEVE PLAYER NAME
    // ==========================================================

    private CompletableFuture<String> retrievePlayerName(
            Guild guild,
            String playerId,
            String fallbackName
    ) {

        /*
         * First check JDA's member cache.
         *
         * This costs no network request.
         */
        Member cachedMember =
                guild.getMemberById(
                        playerId
                );

        if (cachedMember != null) {

            return CompletableFuture.completedFuture(
                    cachedMember.getEffectiveName()
            );
        }

        /*
         * Not cached:
         *
         * retrieve asynchronously.
         *
         * NO .complete()
         */
        return guild.retrieveMemberById(
                        playerId
                )
                .submit()
                .thenApply(
                        Member::getEffectiveName
                )
                .exceptionally(
                        error -> fallbackName
                );
    }

    // ==========================================================
    // RENDER + SEND STARTING BOARD
    // ==========================================================

    private void renderAndSendGame(
            InteractionHook hook,
            GameSession gameSession,
            String whitePlayerId,
            String blackPlayerId,
            String whitePlayerName,
            String blackPlayerName,
            String extraMessage
    ) {

        try {

            byte[] boardImage =
                    BoardRenderer.renderBoard(
                            gameSession.getBoard(),
                            blackPlayerName,
                            whitePlayerName
                    );

            FileUpload file =
                    FileUpload.fromData(
                            boardImage,
                            "board-"
                                    + gameSession.getShortGameId()
                                    + ".png"
                    );

            hook.editOriginal(
                            "♟️ **Blind Chess Game**\n\n"
                                    + "🎮 Game: **"
                                    + gameSession.getShortGameId()
                                    + "**\n\n"
                                    + extraMessage
                                    + "⚫ Black: <@"
                                    + blackPlayerId
                                    + ">\n"
                                    + "⚪ White: <@"
                                    + whitePlayerId
                                    + ">\n\n"
                                    + "⚪ **White to move.**"
                    )
                    .setFiles(
                            file
                    )
                    .setComponents()
                    .queue(
                            ignored -> {

                                System.out.println(
                                        "Game created: "
                                                + gameSession.getShortGameId()
                                                + " | White: "
                                                + whitePlayerId
                                                + " | Black: "
                                                + blackPlayerId
                                                + " | Channel: "
                                                + gameSession.getChannelId()
                                );
                            },
                            error -> {

                                System.err.println(
                                        "Failed to send starting board: "
                                                + error.getMessage()
                                );
                            }
                    );

        } catch (Exception e) {

            e.printStackTrace();

            /*
             * Board rendering failed, but the game itself exists.
             *
             * Don't leave the Discord message stuck at
             * "Creating game...".
             */
            hook.editOriginal(
                            "♟️ **Blind Chess Game**\n\n"
                                    + "🎮 Game: **"
                                    + gameSession.getShortGameId()
                                    + "**\n\n"
                                    + extraMessage
                                    + "⚫ Black: <@"
                                    + blackPlayerId
                                    + ">\n"
                                    + "⚪ White: <@"
                                    + whitePlayerId
                                    + ">\n\n"
                                    + "⚪ **White to move.**\n\n"
                                    + "⚠️ The starting-board image could not be rendered."
                    )
                    .setComponents()
                    .queue();
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