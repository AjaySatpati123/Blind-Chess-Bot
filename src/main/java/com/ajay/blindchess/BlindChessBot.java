package com.ajay.blindchess;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import com.ajay.blindchess.listener.MessageListener;
import com.ajay.blindchess.listener.SlashCommandListener;
import com.ajay.blindchess.listener.ChallengeButtonListener;
import com.ajay.blindchess.listener.ColorButtonListener;
import com.ajay.blindchess.listener.GameSelectListener;
import com.ajay.blindchess.listener.ShowBoardListener;
import com.ajay.blindchess.listener.ResignListener;
import com.ajay.blindchess.listener.CancelListener;

public class BlindChessBot {

    public static void main(String[] args) throws InterruptedException {

        String token = System.getenv("DISCORD_BOT_TOKEN");

        if (token == null || token.isBlank()) {
            throw new IllegalStateException(
                    "DISCORD_BOT_TOKEN environment variable is not configured."
            );
        }

        JDA jda = JDABuilder.createDefault(token)
                .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                .addEventListeners(
                        new MessageListener(),
                        new SlashCommandListener(),
                        new ChallengeButtonListener(),
                        new ColorButtonListener(),
                        new GameSelectListener(),
                        new ShowBoardListener(),
                        new ResignListener(),
                        new CancelListener()
                )
                .setActivity(Activity.playing("Blind Chess"))
                .build();

        jda.awaitReady();

        jda.updateCommands()
                .addCommands(
                        Commands.slash(
                                        "start",
                                        "Challenge someone to a Blind Chess game"
                                )
                                .addOption(
                                        OptionType.USER,
                                        "opponent",
                                        "The player you want to challenge",
                                        true
                                ),

                        Commands.slash(
                                "showboard",
                                "Temporarily reveal your current chess position"
                        ),
                        Commands.slash(
                                "resign",
                                "Resign one of your active Blind Chess games"
                        ),
                        Commands.slash(
                                "cancel",
                                "Request cancellation of an active Blind Chess game"
                        )
                )
                .queue();

        System.out.println("Blind Chess Bot is online!");
    }
}