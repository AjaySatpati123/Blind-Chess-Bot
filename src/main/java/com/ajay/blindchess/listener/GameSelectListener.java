package com.ajay.blindchess.listener;

import com.ajay.blindchess.chess.MoveResult;
import com.ajay.blindchess.chess.MoveService;
import com.ajay.blindchess.game.GameManager;
import com.ajay.blindchess.game.GameSession;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import com.ajay.blindchess.board.FinalBoardService;

public class GameSelectListener extends ListenerAdapter {

    @Override
    public void onStringSelectInteraction(
            StringSelectInteractionEvent event
    ) {

        String componentId =
                event.getComponentId();

        if (!componentId.startsWith("game-move:")) {
            return;
        }

        String[] parts =
                componentId.split(":", 3);

        if (parts.length != 3) {
            return;
        }

        String playerId = parts[1];
        String move = parts[2];

        // Only the player who typed the move
        // can use this selector
        if (!event.getUser().getId().equals(playerId)) {

            event.reply(
                            "This game selector belongs to another player."
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

        // Re-check game state at selection time
        if (game == null || !game.isActive()) {

            event.editMessage(
                            "❌ That game has already finished or no longer exists."
                    )
                    .setComponents()
                    .queue();

            return;
        }

        // Security check
        if (!game.hasPlayer(playerId)) {

            event.reply(
                            "You are not a player in that game."
                    )
                    .setEphemeral(true)
                    .queue();

            return;
        }

        // Make sure this game is actually from this channel
        if (!game.getChannelId()
                .equals(event.getChannel().getId())) {

            event.reply(
                            "That game does not belong to this channel."
                    )
                    .setEphemeral(true)
                    .queue();

            return;
        }

        MoveResult result =
                MoveService.playMove(
                        game,
                        playerId,
                        move
                );

        event.editMessage(
                        result.message()
                )
                .setComponents()
                .queue(message -> {

                    if (!game.isActive()) {

                        FinalBoardService.sendFinalBoard(
                                event.getChannel(),
                                event.getGuild(),
                                game
                        );
                    }
                });
    }
}