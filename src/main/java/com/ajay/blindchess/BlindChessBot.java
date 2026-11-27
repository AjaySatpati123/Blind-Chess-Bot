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
import com.ajay.blindchess.listener.DrawListener;
import com.ajay.blindchess.game.GameManager;
import net.dv8tion.jda.api.interactions.InteractionContextType;

public class BlindChessBot {

    public static void main(String[] args) throws InterruptedException {

        String token = System.getenv("DISCORD_BOT_TOKEN");

        if (token == null || token.isBlank()) {
            throw new IllegalStateException(
                    "DISCORD_BOT_TOKEN environment variable is not configured."
            );
        }

        GameManager.getInstance().loadFromDisk();

        Runtime.getRuntime()
                .addShutdownHook(
                        new Thread(
                                () ->
                                        GameManager
                                                .getInstance()
                                                .saveNow()
                        )
                );

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
                        new CancelListener(),
                        new DrawListener()
                )
                .setActivity(Activity.playing("Blind Chess"))
                .build();

        jda.awaitReady();

        jda.updateCommands()
                .addCommands(

                        Commands.slash(
                                        "start",
                                        "Challenge another player to Blind Chess"
                                )
                                .addOption(
                                        OptionType.USER,
                                        "opponent",
                                        "Player you want to challenge",
                                        true
                                )
                                .setContexts(
                                        InteractionContextType.GUILD
                                ),

                        Commands.slash(
                                        "showboard",
                                        "Temporarily reveal the board"
                                )
                                .setContexts(
                                        InteractionContextType.GUILD
                                ),

                        Commands.slash(
                                        "resign",
                                        "Resign from an active Blind Chess game"
                                )
                                .setContexts(
                                        InteractionContextType.GUILD
                                ),

                        Commands.slash(
                                        "cancel",
                                        "Request cancellation of an active Blind Chess game"
                                )
                                .setContexts(
                                        InteractionContextType.GUILD
                                ),

                        Commands.slash(
                                        "draw",
                                        "Offer a draw in one of your active Blind Chess games"
                                )
                                .setContexts(
                                        InteractionContextType.GUILD
                                )
                )
                .queue();

        System.out.println("Blind Chess Bot is online!");
    }
}