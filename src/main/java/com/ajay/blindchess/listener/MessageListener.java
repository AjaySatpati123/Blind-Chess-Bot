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

        if (event.getAuthor().isBot()) {
            return;
        }

        String rawMessage =
                event.getMessage()
                        .getContentRaw()
                        .trim();

        String message = normalizeMove(rawMessage);

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

        // Player has no active games here
        if (games.isEmpty()) {
            return;
        }

        // Ignore normal conversation
        if (!SAN_PATTERN.matcher(message).matches()) {
            return;
        }

        // Only one active game
        if (games.size() == 1) {

            GameSession game = games.get(0);

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

        // Allow 0-0 / o-o as castling
        move = move.replace('0', 'O');

        if (move.equalsIgnoreCase("o-o")) {
            return "O-O";
        }

        if (move.equalsIgnoreCase("o-o-o")) {
            return "O-O-O";
        }

        move = move.toLowerCase(Locale.ROOT);

        // Piece letters must be uppercase in SAN
        if (!move.isEmpty()
                && "kqrbn".indexOf(move.charAt(0)) >= 0) {

            move =
                    Character.toUpperCase(move.charAt(0))
                            + move.substring(1);
        }

        // Promotion piece must also be uppercase
        int promotionIndex = move.indexOf('=');

        if (promotionIndex >= 0
                && promotionIndex + 1 < move.length()) {

            char promotionPiece =
                    Character.toUpperCase(
                            move.charAt(promotionIndex + 1)
                    );

            move =
                    move.substring(0, promotionIndex + 1)
                            + promotionPiece
                            + move.substring(promotionIndex + 2);
        }

        return move;
    }
}