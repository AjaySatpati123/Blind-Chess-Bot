package com.ajay.blindchess.listener;

import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class ChallengeButtonListener extends ListenerAdapter {

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {

        String componentId = event.getComponentId();

        if (!componentId.startsWith("challenge:")) {
            return;
        }

        String[] parts = componentId.split(":");

        if (parts.length != 4) {
            return;
        }

        String action = parts[1];
        String challengerId = parts[2];
        String opponentId = parts[3];

        // Only challenged player can accept or decline
        if (!event.getUser().getId().equals(opponentId)) {

            event.reply("Only the challenged player can respond to this challenge.")
                    .setEphemeral(true)
                    .queue();

            return;
        }

        // DECLINE
        if (action.equals("decline")) {

            event.editMessage(
                            "❌ <@" + opponentId + "> declined the Blind Chess challenge from <@"
                                    + challengerId + ">."
                    )
                    .setComponents()
                    .queue();

            return;
        }

        // ACCEPT
        if (action.equals("accept")) {

            String whiteId =
                    "colour:white:"
                            + challengerId
                            + ":"
                            + opponentId;

            String blackId =
                    "colour:black:"
                            + challengerId
                            + ":"
                            + opponentId;

            String randomId =
                    "colour:random:"
                            + challengerId
                            + ":"
                            + opponentId;

            event.editMessage(
                            "✅ <@" + opponentId + "> accepted the challenge!\n\n"
                                    + "🎨 <@" + opponentId + ">, choose your colour:"
                    )
                    .setComponents(
                            ActionRow.of(
                                    Button.secondary(whiteId, "⚪ White"),
                                    Button.secondary(blackId, "⚫ Black"),
                                    Button.secondary(randomId, "🎲 Random")
                            )
                    )
                    .queue();
        }
    }
}