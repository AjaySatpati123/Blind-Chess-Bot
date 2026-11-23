package com.ajay.blindchess.listener;

import com.ajay.blindchess.board.BoardRenderer;
import com.ajay.blindchess.game.GameManager;
import com.ajay.blindchess.game.GameSession;

import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.FileUpload;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class ShowBoardListener extends ListenerAdapter {

    // ==========================================================
    // /showboard
    // ==========================================================

    @Override
    public void onSlashCommandInteraction(
            SlashCommandInteractionEvent event
    ) {

        if (!event.getName().equals("showboard")) {
            return;
        }

        String playerId =
                event.getUser().getId();

        String channelId =
                event.getChannel().getId();

        List<GameSession> games =
                GameManager.getInstance()
                        .getActiveGamesForPlayerInChannel(
                                playerId,
                                channelId
                        );

        // ======================================================
        // NO ACTIVE GAMES
        // ======================================================

        if (games.isEmpty()) {

            event.reply(
                            "❌ You have no active games in this channel."
                    )
                    .setEphemeral(true)
                    .queue();

            return;
        }

        // ======================================================
        // ONLY ONE ACTIVE GAME
        // ======================================================

        if (games.size() == 1) {

            GameSession game =
                    games.getFirst();

            showBoard(
                    event,
                    game,
                    playerId
            );

            return;
        }

        // ======================================================
        // MULTIPLE ACTIVE GAMES
        // ======================================================

        StringSelectMenu.Builder menu =
                StringSelectMenu.create(
                                "showboard:"
                                        + playerId
                        )
                        .setPlaceholder(
                                "Choose the game to reveal"
                        );

        int count = 0;

        for (GameSession game : games) {

            if (count >= 25) {
                break;
            }

            String opponentId =
                    game.isWhite(playerId)
                            ? game.getBlackPlayerId()
                            : game.getWhitePlayerId();

            String colour =
                    game.isWhite(playerId)
                            ? "White"
                            : "Black";

            menu.addOption(
                    "Game "
                            + game.getShortGameId(),
                    game.getGameId(),
                    colour
                            + " • vs player "
                            + opponentId
            );

            count++;
        }

        event.reply(
                        "👁️ Choose which game you want to reveal:"
                )
                .addComponents(
                        ActionRow.of(
                                menu.build()
                        )
                )
                .setEphemeral(true)
                .queue();
    }

    // ==========================================================
    // GAME SELECTOR
    // ==========================================================

    @Override
    public void onStringSelectInteraction(
            StringSelectInteractionEvent event
    ) {

        String componentId =
                event.getComponentId();

        if (!componentId.startsWith(
                "showboard:"
        )) {

            return;
        }

        String[] parts =
                componentId.split(":");

        if (parts.length != 2) {
            return;
        }

        String playerId =
                parts[1];

        // ======================================================
        // ONLY OWNER OF SELECTOR MAY USE IT
        // ======================================================

        if (!event.getUser()
                .getId()
                .equals(playerId)) {

            event.reply(
                            "This selector belongs to another player."
                    )
                    .setEphemeral(true)
                    .queue();

            return;
        }

        String gameId =
                event.getValues()
                        .getFirst();

        GameSession game =
                GameManager.getInstance()
                        .getGame(gameId);

        // ======================================================
        // RE-CHECK GAME
        // ======================================================

        if (game == null
                || !game.isActive()) {

            event.editMessage(
                            "❌ That game has already finished."
                    )
                    .setComponents()
                    .queue();

            return;
        }

        // ======================================================
        // VERIFY PLAYER
        // ======================================================

        if (!game.hasPlayer(
                playerId
        )) {

            event.reply(
                            "❌ You are not a player in this game."
                    )
                    .setEphemeral(true)
                    .queue();

            return;
        }

        // ======================================================
        // VERIFY CHANNEL
        // ======================================================

        if (!game.getChannelId()
                .equals(
                        event.getChannel()
                                .getId()
                )) {

            event.reply(
                            "❌ That game does not belong to this channel."
                    )
                    .setEphemeral(true)
                    .queue();

            return;
        }

        showBoard(
                event,
                game,
                playerId
        );
    }

    // ==========================================================
    // SHOW BOARD
    // SLASH COMMAND VERSION
    // ==========================================================

    private void showBoard(
            SlashCommandInteractionEvent event,
            GameSession game,
            String playerId
    ) {

        /*
         * Acknowledge immediately.
         *
         * Member retrieval + SVG rendering can take some time.
         */
        event.deferReply(true)
                .queue(hook -> {

                    try {

                        // ======================================
                        // CREATE BOARD IMAGE
                        // ======================================

                        byte[] boardImage =
                                createBoardImage(
                                        event.getGuild(),
                                        game,
                                        playerId
                                );

                        // ======================================
                        // RECORD REVEAL
                        // ======================================

                        game.recordShowBoard(
                                playerId
                        );

                        // ======================================
                        // SAVE REVEAL TO JSON
                        // ======================================

                        GameManager.getInstance()
                                .saveNow();

                        // ======================================
                        // REVEAL COUNT
                        // ======================================

                        int count =
                                getRevealCount(
                                        game,
                                        playerId
                                );

                        FileUpload file =
                                FileUpload.fromData(
                                        boardImage,
                                        "board-"
                                                + game.getShortGameId()
                                                + ".png"
                                );

                        // ======================================
                        // PRIVATE BOARD
                        // ======================================

                        hook.editOriginal(
                                        "👁️ **Board Reveal**"
                                                + " • Game **"
                                                + game.getShortGameId()
                                                + "**\n"
                                                + "Reveal #"
                                                + count
                                                + "\n\n"
                                                + "This board will disappear in **30 seconds**."
                                )
                                .setFiles(
                                        file
                                )
                                .queue(
                                        ignored -> {

                                            hook.deleteOriginal()
                                                    .queueAfter(
                                                            30,
                                                            TimeUnit.SECONDS
                                                    );
                                        },
                                        error -> {

                                            System.err.println(
                                                    "Failed to display /showboard image: "
                                                            + error.getMessage()
                                            );
                                        }
                                );

                        // ======================================
                        // PUBLIC NOTIFICATION
                        // ======================================

                        sendPublicRevealNotification(
                                event.getChannel(),
                                game,
                                playerId,
                                count
                        );

                    } catch (Exception e) {

                        e.printStackTrace();

                        hook.editOriginal(
                                        "❌ Failed to render the board for Game **"
                                                + game.getShortGameId()
                                                + "**."
                                )
                                .queue();
                    }
                });
    }

    // ==========================================================
    // SHOW BOARD
    // SELECT MENU VERSION
    // ==========================================================

    private void showBoard(
            StringSelectInteractionEvent event,
            GameSession game,
            String playerId
    ) {

        /*
         * The selector message is already ephemeral.
         *
         * Acknowledge first, then replace the selector
         * with the board image.
         */
        event.deferEdit()
                .queue(hook -> {

                    try {

                        // ======================================
                        // CREATE BOARD IMAGE
                        // ======================================

                        byte[] boardImage =
                                createBoardImage(
                                        event.getGuild(),
                                        game,
                                        playerId
                                );

                        // ======================================
                        // RECORD REVEAL
                        // ======================================

                        game.recordShowBoard(
                                playerId
                        );

                        // ======================================
                        // SAVE REVEAL TO JSON
                        // ======================================

                        GameManager.getInstance()
                                .saveNow();

                        // ======================================
                        // REVEAL COUNT
                        // ======================================

                        int count =
                                getRevealCount(
                                        game,
                                        playerId
                                );

                        FileUpload file =
                                FileUpload.fromData(
                                        boardImage,
                                        "board-"
                                                + game.getShortGameId()
                                                + ".png"
                                );

                        // ======================================
                        // PRIVATE BOARD
                        // ======================================

                        hook.editOriginal(
                                        "👁️ **Board Reveal**"
                                                + " • Game **"
                                                + game.getShortGameId()
                                                + "**\n"
                                                + "Reveal #"
                                                + count
                                                + "\n\n"
                                                + "This board will disappear in **30 seconds**."
                                )
                                .setFiles(
                                        file
                                )
                                .setComponents()
                                .queue(
                                        ignored -> {

                                            hook.deleteOriginal()
                                                    .queueAfter(
                                                            30,
                                                            TimeUnit.SECONDS
                                                    );
                                        },
                                        error -> {

                                            System.err.println(
                                                    "Failed to display selected /showboard image: "
                                                            + error.getMessage()
                                            );
                                        }
                                );

                        // ======================================
                        // PUBLIC NOTIFICATION
                        // ======================================

                        sendPublicRevealNotification(
                                event.getChannel(),
                                game,
                                playerId,
                                count
                        );

                    } catch (Exception e) {

                        e.printStackTrace();

                        hook.editOriginal(
                                        "❌ Failed to render the board for Game **"
                                                + game.getShortGameId()
                                                + "**."
                                )
                                .setComponents()
                                .queue();
                    }
                });
    }

    // ==========================================================
    // CREATE BOARD IMAGE
    // ==========================================================

    private byte[] createBoardImage(
            Guild guild,
            GameSession game,
            String playerId
    ) {

        String whiteName =
                "White Player";

        String blackName =
                "Black Player";

        // ======================================================
        // WHITE PLAYER NAME
        // ======================================================

        if (guild != null) {

            try {

                Member whiteMember =
                        guild.retrieveMemberById(
                                        game.getWhitePlayerId()
                                )
                                .complete();

                if (whiteMember != null) {

                    whiteName =
                            whiteMember.getEffectiveName();
                }

            } catch (Exception ignored) {

            }

            // ==================================================
            // BLACK PLAYER NAME
            // ==================================================

            try {

                Member blackMember =
                        guild.retrieveMemberById(
                                        game.getBlackPlayerId()
                                )
                                .complete();

                if (blackMember != null) {

                    blackName =
                            blackMember.getEffectiveName();
                }

            } catch (Exception ignored) {

            }
        }

        // ======================================================
        // PLAYER-RELATIVE ORIENTATION
        // ======================================================

        /*
         * White player:
         *
         *      Black
         *      board
         *      White
         *
         * Black player:
         *
         *      White
         *      board
         *      Black
         */
        boolean whiteAtBottom =
                game.isWhite(
                        playerId
                );

        return BoardRenderer.renderBoard(
                game.getBoard(),
                blackName,
                whiteName,
                whiteAtBottom
        );
    }

    // ==========================================================
    // GET REVEAL COUNT
    // ==========================================================

    private int getRevealCount(
            GameSession game,
            String playerId
    ) {

        if (game.isWhite(
                playerId
        )) {

            return game
                    .getWhiteShowBoardCount();
        }

        return game
                .getBlackShowBoardCount();
    }

    // ==========================================================
    // PUBLIC REVEAL NOTIFICATION
    // ==========================================================

    private void sendPublicRevealNotification(
            net.dv8tion.jda.api.entities.channel.middleman.MessageChannel channel,
            GameSession game,
            String playerId,
            int count
    ) {

        channel.sendMessage(
                        "👁️ <@"
                                + playerId
                                + "> revealed the board"
                                + " in Game **"
                                + game.getShortGameId()
                                + "**."
                                + " Reveal #"
                                + count
                                + "."
                )
                .queue();
    }
}