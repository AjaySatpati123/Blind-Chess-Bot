package com.ajay.blindchess.listener;

import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import com.ajay.blindchess.game.GameManager;
import com.ajay.blindchess.game.GameSession;
import java.util.concurrent.ThreadLocalRandom;
import com.ajay.blindchess.board.BoardRenderer;

public class ColorButtonListener extends ListenerAdapter {

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {

        String componentId = event.getComponentId();

        if (!componentId.startsWith("colour:")) {
            return;
        }

        String[] parts = componentId.split(":");

        if (parts.length != 4) {
            return;
        }

        String colour = parts[1];
        String challengerId = parts[2];
        String opponentId = parts[3];

        // Only the player who accepted the challenge can choose
        if (!event.getUser().getId().equals(opponentId)) {

            event.reply("Only the challenged player can choose the colour.")
                    .setEphemeral(true)
                    .queue();

            return;
        }

        String whitePlayerId;
        String blackPlayerId;
        String extraMessage = "";

        switch (colour) {

            case "white" -> {
                whitePlayerId = opponentId;
                blackPlayerId = challengerId;
            }

            case "black" -> {
                whitePlayerId = challengerId;
                blackPlayerId = opponentId;
            }

            case "random" -> {

                boolean opponentGetsWhite =
                        ThreadLocalRandom.current().nextBoolean();

                if (opponentGetsWhite) {

                    whitePlayerId = opponentId;
                    blackPlayerId = challengerId;

                } else {

                    whitePlayerId = challengerId;
                    blackPlayerId = opponentId;
                }

                extraMessage = "🎲 **Colours were randomly assigned!**\n\n";
            }

            default -> {
                return;
            }
        }

        String channelId = event.getMessage().getChannelId();

        GameSession gameSession = new GameSession(
                channelId,
                whitePlayerId,
                blackPlayerId
        );

        GameManager.getInstance().addGame(gameSession);

        System.out.println(
                "Game created: "
                        + gameSession.getShortGameId()
                        + " | White: "
                        + whitePlayerId
                        + " | Black: "
                        + blackPlayerId
                        + " | Channel: "
                        + channelId
        );

        event.editMessage(
                        "♟️ **Blind Chess Game**\n\n"
                                + "🎮 Game: **"
                                + gameSession.getShortGameId()
                                + "**\n\n"
                                + extraMessage
                                + "⚪ White: <@" + whitePlayerId + ">\n"
                                + "⚫ Black: <@" + blackPlayerId + ">\n\n"
                                + BoardRenderer.renderInitialBoard(
                                        whitePlayerId,
                                        blackPlayerId
                                )
                                + "\n\n"
                                + "⚪ **White to move.**"
                )
                .setComponents()
                .queue();
    }
}