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

public class ResignListener extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(
            SlashCommandInteractionEvent event
    ) {

        if (!event.getName().equals("resign")) {
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

        // Only one game
        if (games.size() == 1) {

            showConfirmation(
                    event,
                    games.get(0),
                    playerId
            );

            return;
        }

        // Multiple games
        StringSelectMenu.Builder menu =
                StringSelectMenu.create(
                                "resign-select:" + playerId
                        )
                        .setPlaceholder(
                                "Choose the game you want to resign"
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
                    "Game " + game.getShortGameId(),
                    game.getGameId(),
                    colour + " • Opponent: " + opponentId
            );

            count++;
        }

        event.reply(
                        "🏳️ Choose the game you want to resign:"
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

        if (!event.getComponentId()
                .startsWith("resign-select:")) {
            return;
        }

        String playerId =
                event.getComponentId()
                        .substring(
                                "resign-select:".length()
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
                event.getValues().get(0);

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

        String confirmId =
                "resign-confirm:"
                        + playerId
                        + ":"
                        + game.getGameId();

        String cancelId =
                "resign-cancel:"
                        + playerId
                        + ":"
                        + game.getGameId();

        event.editMessage(
                        "🏳️ Are you sure you want to resign Game **"
                                + game.getShortGameId()
                                + "**?"
                )
                .setComponents(
                        ActionRow.of(
                                Button.danger(
                                        confirmId,
                                        "Resign"
                                ),
                                Button.secondary(
                                        cancelId,
                                        "Keep Playing"
                                )
                        )
                )
                .queue();
    }

    @Override
    public void onButtonInteraction(
            ButtonInteractionEvent event
    ) {

        String id =
                event.getComponentId();

        if (!id.startsWith("resign-confirm:")
                && !id.startsWith("resign-cancel:")) {

            return;
        }

        String[] parts =
                id.split(":", 3);

        if (parts.length != 3) {
            return;
        }

        String action = parts[0];
        String playerId = parts[1];
        String gameId = parts[2];

        if (!event.getUser()
                .getId()
                .equals(playerId)) {

            event.reply(
                            "This resignation belongs to another player."
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
                            "❌ That game has already finished."
                    )
                    .setComponents()
                    .queue();

            return;
        }

        if (action.equals("resign-cancel")) {

            event.editMessage(
                            "✅ Resignation cancelled. Game **"
                                    + game.getShortGameId()
                                    + "** continues."
                    )
                    .setComponents()
                    .queue();

            return;
        }

        resignGame(
                event,
                game,
                playerId
        );
    }

    private void showConfirmation(
            SlashCommandInteractionEvent event,
            GameSession game,
            String playerId
    ) {

        String confirmId =
                "resign-confirm:"
                        + playerId
                        + ":"
                        + game.getGameId();

        String cancelId =
                "resign-cancel:"
                        + playerId
                        + ":"
                        + game.getGameId();

        event.reply(
                        "🏳️ Are you sure you want to resign Game **"
                                + game.getShortGameId()
                                + "**?"
                )
                .addComponents(
                        ActionRow.of(
                                Button.danger(
                                        confirmId,
                                        "Resign"
                                ),
                                Button.secondary(
                                        cancelId,
                                        "Keep Playing"
                                )
                        )
                )
                .setEphemeral(true)
                .queue();
    }

    private void resignGame(
            ButtonInteractionEvent event,
            GameSession game,
            String playerId
    ) {

        if (game.isWhite(playerId)) {

            game.finishGame(
                    0,
                    1,
                    "Resignation"
            );

        } else {

            game.finishGame(
                    1,
                    0,
                    "Resignation"
            );
        }

        event.editMessage(
                        "🏳️ You resigned Game **"
                                + game.getShortGameId()
                                + "**."
                )
                .setComponents()
                .queue();

        event.getChannel()
                .sendMessage(
                        "🏳️ **Game "
                                + game.getShortGameId()
                                + " — Resignation**\n\n"
                                + getResultText(game)
                                + "\n\n"
                                + "**Moves**\n```"
                                + game.getFormattedMoveHistory()
                                + "```\n"
                                + getRevealHistoryText(game)
                )
                .queue();
    }

    private String getResultText(
            GameSession game
    ) {

        return "<@" + game.getWhitePlayerId() + "> : "
                + formatScore(game.getWhiteScore())
                + "\n"
                + "<@" + game.getBlackPlayerId() + "> : "
                + formatScore(game.getBlackScore());
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