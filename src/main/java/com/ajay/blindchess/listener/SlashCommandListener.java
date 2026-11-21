package com.ajay.blindchess.listener;

import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class SlashCommandListener extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {

        if (!event.getName().equals("start")) {
            return;
        }

        User challenger = event.getUser();
        User opponent = event.getOption("opponent").getAsUser();

        if (challenger.getId().equals(opponent.getId())) {
            event.reply("You cannot challenge yourself.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        if (opponent.isBot()) {
            event.reply("You cannot challenge a bot.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        String acceptId =
                "challenge:accept:"
                        + challenger.getId()
                        + ":"
                        + opponent.getId();

        String declineId =
                "challenge:decline:"
                        + challenger.getId()
                        + ":"
                        + opponent.getId();

        event.reply(
                        "♟️ **Blind Chess Challenge**\n\n"
                                + challenger.getAsMention()
                                + " challenged "
                                + opponent.getAsMention()
                                + "!"
                )
                .addComponents(
                        ActionRow.of(
                                Button.success(acceptId, "Accept"),
                                Button.danger(declineId, "Decline")
                        )
                )
                .queue();
    }
}