package com.ajay.blindchess.board;

import com.ajay.blindchess.game.GameSession;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.utils.FileUpload;

public class FinalBoardService {

    private FinalBoardService() {
    }

    public static void sendFinalBoard(
            MessageChannel channel,
            Guild guild,
            GameSession game
    ) {

        if (guild == null) {
            return;
        }

        guild.retrieveMemberById(game.getWhitePlayerId())
                .queue(
                        whiteMember ->
                                guild.retrieveMemberById(game.getBlackPlayerId())
                                        .queue(
                                                blackMember ->
                                                        sendBoard(
                                                                channel,
                                                                game,
                                                                whiteMember,
                                                                blackMember
                                                        )
                                        )
                );
    }

    private static void sendBoard(
            MessageChannel channel,
            GameSession game,
            Member whiteMember,
            Member blackMember
    ) {

        byte[] boardImage =
                BoardRenderer.renderBoard(
                        game.getBoard(),
                        blackMember.getEffectiveName(),
                        whiteMember.getEffectiveName()
                );

        channel.sendMessage(
                        "🏁 **Final Position** • Game **"
                                + game.getShortGameId()
                                + "**"
                )
                .addFiles(
                        FileUpload.fromData(
                                boardImage,
                                "final-board-"
                                        + game.getShortGameId()
                                        + ".png"
                        )
                )
                .queue();
    }
}