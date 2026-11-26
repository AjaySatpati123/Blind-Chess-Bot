package com.ajay.blindchess.listener;

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

public class CancelListener extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(
            SlashCommandInteractionEvent event
    ) {

        if (!event.getName().equals("cancel")) {
            return;
        }

        String playerId = event.getUser().getId();
        String channelId = event.getChannel().getId();

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

        if (games.size() == 1) {

            sendCancelRequest(
                    event,
                    games.getFirst(),
                    playerId
            );

            return;
        }

        StringSelectMenu.Builder menu =
                StringSelectMenu.create(
                                "cancel-select:" + playerId
                        )
                        .setPlaceholder(
                                "Choose the game you want to cancel"
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

            menu.addOption(
                    "Game " + game.getShortGameId(),
                    game.getGameId(),
                    "Opponent: " + opponentId
            );

            count++;
        }

        event.reply(
                        "❌ Choose the game you want to cancel:"
                )
                .addComponents(
                        ActionRow.of(menu.build())
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

        if (!id.startsWith("cancel-select:")) {
            return;
        }

        String playerId =
                id.substring(
                        "cancel-select:".length()
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

        if (game == null || !game.isActive()) {

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
                        "✅ Cancellation request sent for Game **"
                                + game.getShortGameId()
                                + "**."
                )
                .setComponents()
                .queue();

        postCancelRequest(
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

        if (!id.startsWith("cancel-accept:")
                && !id.startsWith("cancel-reject:")) {

            return;
        }

        String[] parts =
                id.split(":", 4);

        if (parts.length != 4) {
            return;
        }

        String action = parts[0];
        String requesterId = parts[1];
        String opponentId = parts[2];
        String gameId = parts[3];

        // Only opponent can answer
        if (!event.getUser()
                .getId()
                .equals(opponentId)) {

            event.reply(
                            "Only the other player can respond to this cancellation request."
                    )
                    .setEphemeral(true)
                    .queue();

            return;
        }

        GameSession game =
                GameManager.getInstance()
                        .getGame(gameId);

        if (game == null || !game.isActive()) {

            event.editMessage(
                            "❌ This game has already finished."
                    )
                    .setComponents()
                    .queue();

            return;
        }

        if (action.equals("cancel-reject")) {

            event.editMessage(
                            "▶️ <@"
                                    + opponentId
                                    + "> declined the cancellation request.\n"
                                    + "Game **"
                                    + game.getShortGameId()
                                    + "** continues."
                    )
                    .setComponents()
                    .queue();

            return;
        }

        // =========================
        // CANCEL GAME
        // =========================

        boolean cancelled;

        synchronized (game) {

            // Re-check inside the lock
            if (!game.isActive()) {

                cancelled = false;

            } else {

                game.endGame();

                cancelled = true;
            }
        }

        if (!cancelled) {

            event.editMessage(
                            "❌ This game has already finished."
                    )
                    .setComponents()
                    .queue();

            return;
        }

        // Save AFTER releasing game lock
        GameManager.getInstance()
                .saveNow();

        event.editMessage(
                        "❌ **Game "
                                + game.getShortGameId()
                                + " cancelled**\n\n"
                                + "<@" + requesterId + "> and <@"
                                + opponentId
                                + "> agreed to cancel the game.\n\n"
                                + "**Moves**\n```"
                                + game.getFormattedMoveHistory()
                                + "```"
                                + "\n"
                                + getRevealHistoryText(game)
                )
                .setComponents()
                .queue();
    }

    private void sendCancelRequest(
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
                        "✅ Cancellation request sent for Game **"
                                + game.getShortGameId()
                                + "**."
                )
                .setEphemeral(true)
                .queue();

        postCancelRequest(
                event.getChannel(),
                game,
                playerId,
                opponentId
        );
    }

    private void postCancelRequest(
            net.dv8tion.jda.api.entities.channel.middleman.MessageChannel channel,
            GameSession game,
            String requesterId,
            String opponentId
    ) {

        String acceptId =
                "cancel-accept:"
                        + requesterId
                        + ":"
                        + opponentId
                        + ":"
                        + game.getGameId();

        String rejectId =
                "cancel-reject:"
                        + requesterId
                        + ":"
                        + opponentId
                        + ":"
                        + game.getGameId();

        channel.sendMessage(
                        "❌ <@"
                                + requesterId
                                + "> wants to cancel Game **"
                                + game.getShortGameId()
                                + "**.\n\n"
                                + "<@" + opponentId
                                + ">, do you agree?"
                )
                .addComponents(
                        ActionRow.of(
                                Button.danger(
                                        acceptId,
                                        "Accept Cancel"
                                ),
                                Button.secondary(
                                        rejectId,
                                        "Keep Playing"
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

    private String getRevealHistoryText(
            GameSession game
    ) {

        return "**Board Reveals**\n\n"
                + "<@" + game.getWhitePlayerId() + ">\n"
                + game.getFormattedShowBoardHistory(
                game.getWhitePlayerId()
        )
                + "\n\n"
                + "<@" + game.getBlackPlayerId() + ">\n"
                + game.getFormattedShowBoardHistory(
                game.getBlackPlayerId()
        );
    }
}