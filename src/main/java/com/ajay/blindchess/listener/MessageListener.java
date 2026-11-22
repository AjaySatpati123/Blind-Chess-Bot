package com.ajay.blindchess.listener;

import com.ajay.blindchess.chess.MoveResult;
import com.ajay.blindchess.chess.MoveService;
import com.ajay.blindchess.game.GameManager;
import com.ajay.blindchess.game.GameSession;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import java.util.Locale;

import java.util.List;
import java.util.regex.Pattern;

public class MessageListener extends ListenerAdapter {

    private static final Pattern SAN_PATTERN =
            Pattern.compile(
                    "^(?:O-O(?:-O)?|[KQRBN]?[a-h1-8]{0,2}x?[a-h][1-8](?:=[QRBN])?)[+#]?$"
            );

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {

        System.out.println(
                "MESSAGE: "
                        + event.getAuthor().getName()
                        + " -> "
                        + event.getMessage().getContentRaw()
        );

        if (event.getAuthor().isBot()) {
            return;
        }

        String rawMessage =
                event.getMessage()
                        .getContentRaw()
                        .trim();

        String message = normalizeMove(rawMessage);

        System.out.println(
                "NORMALIZED: " + message
        );

        String playerId =
                event.getAuthor().getId();

        String channelId =
                event.getChannel().getId();

        List<GameSession> games =
                GameManager.getInstance()
                        .getActiveGamesForPlayerInChannel(
                                playerId,
                                channelId
                        );

        System.out.println(
                "ACTIVE GAMES FOUND: " + games.size()
        );

        // Player has no active games here
        if (games.isEmpty()) {
            return;
        }

        System.out.println(
                "SAN MATCH: "
                        + SAN_PATTERN.matcher(message).matches()
        );

        // Ignore normal conversation
        if (!SAN_PATTERN.matcher(message).matches()) {
            return;
        }

        // Only one active game
        if (games.size() == 1) {

            GameSession game = games.get(0);

            System.out.println(
                    "PLAYING MOVE "
                            + message
                            + " IN GAME "
                            + game.getShortGameId()
            );

            MoveResult result =
                    MoveService.playMove(
                            game,
                            playerId,
                            message
                    );

            event.getChannel()
                    .sendMessage(result.message())
                    .queue();

            return;
        }

        // Multiple games
        String selectId =
                "game-move:"
                        + playerId
                        + ":"
                        + message;

        StringSelectMenu.Builder menu =
                StringSelectMenu.create(selectId)
                        .setPlaceholder(
                                "Choose which game this move belongs to"
                        );

        StringBuilder gameList =
                new StringBuilder(
                        "♟️ Which game is `"
                                + message
                                + "` for?\n\n"
                );

        int count = 0;

        for (GameSession game : games) {

            // Discord select menu limit
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

            boolean yourTurn =
                    (game.isWhite(playerId)
                            && game.getSideToMove()
                            == com.github.bhlangonijr.chesslib.Side.WHITE)
                            ||
                            (game.isBlack(playerId)
                                    && game.getSideToMove()
                                    == com.github.bhlangonijr.chesslib.Side.BLACK);

            gameList
                    .append("• **")
                    .append(game.getShortGameId())
                    .append("** — vs <@")
                    .append(opponentId)
                    .append("> — ")
                    .append(colour)
                    .append(" — ")
                    .append(
                            yourTurn
                                    ? "Your turn"
                                    : "Opponent's turn"
                    )
                    .append("\n");

            menu.addOption(
                    "Game " + game.getShortGameId(),
                    game.getGameId(),
                    colour
                            + " • "
                            + (
                            yourTurn
                                    ? "Your turn"
                                    : "Opponent's turn"
                    )
            );

            count++;
        }

        event.getChannel()
                .sendMessage(gameList.toString())
                .addComponents(
                        ActionRow.of(menu.build())
                )
                .queue();
    }

    private static String normalizeMove(String input) {

        String move = input.trim();

        // Allow 0-0 / o-o
        move = move.replace('0', 'O');

        if (move.equalsIgnoreCase("o-o")) {
            return "O-O";
        }

        if (move.equalsIgnoreCase("o-o-o")) {
            return "O-O-O";
        }

        move = move.toLowerCase(Locale.ROOT);

        if (move.isEmpty()) {
            return move;
        }

        char first = move.charAt(0);

        // King, Queen, Rook and Knight are unambiguous
        if ("kqrn".indexOf(first) >= 0) {

            move =
                    Character.toUpperCase(first)
                            + move.substring(1);
        }

        /*
         * 'b' is special because it can mean:
         *
         * b4    = pawn move
         * bxc5  = pawn capture
         *
         * bf4   = user meant Bf4
         * bb5   = user meant Bb5
         * bxe5  = user meant Bxe5
         */
        else if (first == 'b' && move.length() >= 2) {

            char second = move.charAt(1);

            boolean pawnMove =
                    Character.isDigit(second);

            boolean pawnCapture = false;

            if (second == 'x' && move.length() >= 3) {

                char destinationFile =
                        move.charAt(2);

                // A pawn on the b-file can only capture a-file or c-file
                pawnCapture =
                        destinationFile == 'a'
                                || destinationFile == 'c';
            }

            if (!pawnMove && !pawnCapture) {

                move =
                        'B'
                                + move.substring(1);
            }
        }

        // Promotion piece must be uppercase
        int promotionIndex =
                move.indexOf('=');

        if (promotionIndex >= 0
                && promotionIndex + 1 < move.length()) {

            char promotionPiece =
                    Character.toUpperCase(
                            move.charAt(promotionIndex + 1)
                    );

            move =
                    move.substring(
                            0,
                            promotionIndex + 1
                    )
                            + promotionPiece
                            + move.substring(
                            promotionIndex + 2
                    );
        }

        return move;
    }
}