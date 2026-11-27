package com.ajay.blindchess.listener;

import com.ajay.blindchess.board.FinalBoardService;
import com.ajay.blindchess.game.GameManager;
import com.ajay.blindchess.game.GameSession;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.List;

public class DrawListener extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(
            SlashCommandInteractionEvent event
    ) {

        if (!event.getName().equals("draw")) {
            return;
        }

        if (!event.isFromGuild()) {

            event.reply(
                            "❌ `/draw` can only be used inside a Discord server."
                    )
                    .setEphemeral(true)
                    .queue();

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

        if (games.isEmpty()) {

            event.reply(
                            "❌ You have no active games in this channel."
                    )
                    .setEphemeral(true)
                    .queue();

            return;
        }

        // Only one active game
        if (games.size() == 1) {

            GameSession game =
                    games.getFirst();

            sendDrawOffer(
                    event,
                    game,
                    playerId
            );

            return;
        }

        // Multiple active games
        StringSelectMenu.Builder menu =
                StringSelectMenu.create(
                                "draw-select:" + playerId
                        )
                        .setPlaceholder(
                                "Choose the game"
                        );

        int count = 0;

        for (GameSession game : games) {

            if (count >= 25) {
                break;
            }

            String opponentId =
                    getOpponentId(
                            game,
                            playerId
                    );

            String colour =
                    game.isWhite(playerId)
                            ? "White"
                            : "Black";

            menu.addOption(
                    "Game "
                            + game.getShortGameId(),
                    game.getGameId(),
                    colour
                            + " • Opponent: "
                            + opponentId
            );

            count++;
        }

        event.reply(
                        "🤝 Choose the game where you want to offer a draw:"
                )
                .addComponents(
                        ActionRow.of(
                                menu.build()
                        )
                )
                .setEphemeral(true)
                .queue();
    }

    @Override
    public void onStringSelectInteraction(
            StringSelectInteractionEvent event
    ) {

        String id =
                event.getComponentId();

        if (!id.startsWith("draw-select:")) {
            return;
        }

        String playerId =
                id.substring(
                        "draw-select:".length()
                );

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
                event.getValues().getFirst();

        GameSession game =
                GameManager.getInstance()
                        .getGame(gameId);

        if (game == null
                || !game.isActive()) {

            event.editMessage(
                            "❌ That game has already finished."
                    )
                    .setComponents()
                    .queue();

            return;
        }

        if (!game.hasPlayer(playerId)) {

            event.reply(
                            "❌ You are not a player in this game."
                    )
                    .setEphemeral(true)
                    .queue();

            return;
        }

        String opponentId =
                getOpponentId(
                        game,
                        playerId
                );

        event.editMessage(
                        "✅ Draw offer sent for Game **"
                                + game.getShortGameId()
                                + "**."
                )
                .setComponents()
                .queue();

        postDrawOffer(
                event.getChannel(),
                game,
                playerId,
                opponentId
        );
    }

    @Override
    public void onButtonInteraction(
            ButtonInteractionEvent event
    ) {

        String id =
                event.getComponentId();

        if (!id.startsWith("draw-accept:")
                && !id.startsWith("draw-decline:")) {

            return;
        }

        String[] parts =
                id.split(":", 4);

        if (parts.length != 4) {
            return;
        }

        String action =
                parts[0];

        String requesterId =
                parts[1];

        String opponentId =
                parts[2];

        String gameId =
                parts[3];

        // Only opponent can respond
        if (!event.getUser()
                .getId()
                .equals(opponentId)) {

            event.reply(
                            "Only the other player can respond to this draw offer."
                    )
                    .setEphemeral(true)
                    .queue();

            return;
        }

        GameSession game =
                GameManager.getInstance()
                        .getGame(gameId);

        // Re-check current game state
        if (game == null
                || !game.isActive()) {

            event.editMessage(
                            "❌ This game has already finished."
                    )
                    .setComponents()
                    .queue();

            return;
        }

        if (!game.hasPlayer(requesterId)
                || !game.hasPlayer(opponentId)) {

            event.editMessage(
                            "❌ This draw offer is no longer valid."
                    )
                    .setComponents()
                    .queue();

            return;
        }

        // =========================
        // DECLINE DRAW
        // =========================

        if (action.equals("draw-decline")) {

            event.editMessage(
                            "▶️ <@"
                                    + opponentId
                                    + "> declined the draw offer.\n"
                                    + "Game **"
                                    + game.getShortGameId()
                                    + "** continues."
                    )
                    .setComponents()
                    .queue();

            return;
        }

        // =========================
        // ACCEPT DRAW
        // =========================

        boolean drawAccepted;

        synchronized (game) {

            // Re-check INSIDE the lock
            if (!game.isActive()) {

                drawAccepted = false;

            } else {

                game.finishGame(
                        0.5,
                        0.5,
                        "Draw Agreement"
                );

                drawAccepted = true;
            }
        }

        if (!drawAccepted) {

            event.editMessage(
                            "❌ This game has already finished."
                    )
                    .setComponents()
                    .queue();

            return;
        }

        // Save AFTER releasing the game lock
        GameManager.getInstance()
                .saveNow();

        event.editMessage(
                        "🤝 **Game "
                                + game.getShortGameId()
                                + " — Draw Agreed**\n\n"
                                + getResultText(game)
                                + "\n\n"
                                + "**Moves**\n```"
                                + game.getFormattedMoveHistory()
                                + "```\n"
                                + getRevealHistoryText(game)
                )
                .setComponents()
                .queue(ignored -> {

                    FinalBoardService.sendFinalBoard(
                            event.getChannel(),
                            event.getGuild(),
                            game
                    );
                });
    }

    private void sendDrawOffer(
            SlashCommandInteractionEvent event,
            GameSession game,
            String playerId
    ) {

        String opponentId =
                getOpponentId(
                        game,
                        playerId
                );

        event.reply(
                        "✅ Draw offer sent for Game **"
                                + game.getShortGameId()
                                + "**."
                )
                .setEphemeral(true)
                .queue();

        postDrawOffer(
                event.getChannel(),
                game,
                playerId,
                opponentId
        );
    }

    private void postDrawOffer(
            net.dv8tion.jda.api.entities.channel.middleman.MessageChannel channel,
            GameSession game,
            String requesterId,
            String opponentId
    ) {

        String acceptId =
                "draw-accept:"
                        + requesterId
                        + ":"
                        + opponentId
                        + ":"
                        + game.getGameId();

        String declineId =
                "draw-decline:"
                        + requesterId
                        + ":"
                        + opponentId
                        + ":"
                        + game.getGameId();

        channel.sendMessage(
                        "🤝 <@"
                                + requesterId
                                + "> offered a draw in Game **"
                                + game.getShortGameId()
                                + "**.\n\n"
                                + "<@"
                                + opponentId
                                + ">, do you accept?"
                )
                .addComponents(
                        ActionRow.of(
                                Button.success(
                                        acceptId,
                                        "Accept Draw"
                                ),
                                Button.secondary(
                                        declineId,
                                        "Decline Draw"
                                )
                        )
                )
                .queue();
    }

    private String getOpponentId(
            GameSession game,
            String playerId
    ) {

        return game.isWhite(playerId)
                ? game.getBlackPlayerId()
                : game.getWhitePlayerId();
    }

    private String getResultText(
            GameSession game
    ) {

        return "<@"
                + game.getWhitePlayerId()
                + "> : "
                + formatScore(
                game.getWhiteScore()
        )
                + "\n"
                + "<@"
                + game.getBlackPlayerId()
                + "> : "
                + formatScore(
                game.getBlackScore()
        );
    }

    private String getRevealHistoryText(
            GameSession game
    ) {

        return "**Board Reveals**\n\n"
                + "<@"
                + game.getWhitePlayerId()
                + ">\n"
                + game.getFormattedShowBoardHistory(
                game.getWhitePlayerId()
        )
                + "\n\n"
                + "<@"
                + game.getBlackPlayerId()
                + ">\n"
                + game.getFormattedShowBoardHistory(
                game.getBlackPlayerId()
        );
    }

    private String formatScore(
            double score
    ) {

        if (score == 1) {
            return "1";
        }

        if (score == 0) {
            return "0";
        }

        return "0.5";
    }
}