package com.ajay.blindchess.listener;

import com.ajay.blindchess.board.FinalBoardService;
import com.ajay.blindchess.chess.MoveResult;
import com.ajay.blindchess.chess.MoveService;
import com.ajay.blindchess.game.GameManager;
import com.ajay.blindchess.game.GameSession;
import com.ajay.blindchess.game.SanUtils;

import com.github.bhlangonijr.chesslib.Side;

import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.List;

public class MessageListener extends ListenerAdapter {

    @Override
    public void onMessageReceived(
            MessageReceivedEvent event
    ) {

        // ======================================================
        // IGNORE BOT MESSAGES
        // ======================================================

        if (event.getAuthor().isBot()) {
            return;
        }

        // ======================================================
        // ONLY SERVER CHANNELS
        // ======================================================

        if (!event.isFromGuild()) {
            return;
        }

        String rawMessage =
                event.getMessage()
                        .getContentRaw()
                        .trim();

        if (rawMessage.isEmpty()) {
            return;
        }

        String playerId =
                event.getAuthor().getId();

        String channelId =
                event.getChannel().getId();

        // ======================================================
        // FIND ACTIVE GAMES
        // ======================================================

        List<GameSession> games =
                GameManager.getInstance()
                        .getActiveGamesForPlayerInChannel(
                                playerId,
                                channelId
                        );

        /*
         * No active games for this user in this channel.
         *
         * Do absolutely nothing so normal Discord chat
         * remains unaffected.
         */
        if (games.isEmpty()) {
            return;
        }

        // ======================================================
        // DOES MESSAGE LOOK LIKE A CHESS MOVE?
        // ======================================================

        if (!SanUtils.isPotentialMoveInput(
                rawMessage
        )) {

            /*
             * Examples:
             *
             * hello
             * nice
             * gg
             * bro
             *
             * Ignore ordinary conversation.
             */
            return;
        }

        // ======================================================
        // INVALID SAN SHAPE
        // ======================================================

        if (!SanUtils.hasValidSanShape(
                rawMessage
        )) {

            event.getChannel()
                    .sendMessage(
                            "❌ `"
                                    + rawMessage
                                    + "` is invalid chess notation.\n"
                                    + "Examples: `e4`, `Nf3`, `Nbd2`, "
                                    + "`R1e2`, `exd5`, `e8=Q`, `O-O`."
                    )
                    .queue();

            return;
        }

        // ======================================================
        // ONLY ONE ACTIVE GAME
        // ======================================================

        if (games.size() == 1) {

            GameSession game =
                    games.get(0);

            playMove(
                    event,
                    game,
                    playerId,
                    rawMessage
            );

            return;
        }

        // ======================================================
        // MULTIPLE ACTIVE GAMES
        // ======================================================

        showGameSelector(
                event,
                games,
                playerId,
                rawMessage
        );
    }

    // ==========================================================
    // PLAY MOVE DIRECTLY
    // ==========================================================

    private void playMove(
            MessageReceivedEvent event,
            GameSession game,
            String playerId,
            String rawMove
    ) {

        MoveResult result =
                MoveService.playMove(
                        game,
                        playerId,
                        rawMove
                );

        event.getChannel()
                .sendMessage(
                        result.message()
                )
                .queue(ignored -> {

                    /*
                     * MoveService marks the game inactive
                     * when the move causes:
                     *
                     * - Checkmate
                     * - Stalemate
                     * - Threefold repetition
                     * - 50-move rule
                     * - Insufficient material
                     *
                     * In those cases show the final board.
                     */
                    if (result.success()
                            && !game.isActive()) {

                        FinalBoardService.sendFinalBoard(
                                event.getChannel(),
                                event.getGuild(),
                                game
                        );
                    }
                });
    }

    // ==========================================================
    // MULTIPLE-GAME SELECTOR
    // ==========================================================

    private void showGameSelector(
            MessageReceivedEvent event,
            List<GameSession> games,
            String playerId,
            String rawMove
    ) {

        String componentId =
                "game-move:"
                        + playerId
                        + ":"
                        + rawMove;

        StringSelectMenu.Builder menu =
                StringSelectMenu.create(
                                componentId
                        )
                        .setPlaceholder(
                                "Choose the game for "
                                        + rawMove
                        );

        int count = 0;

        for (GameSession game : games) {

            /*
             * Discord select menus allow
             * a maximum of 25 options.
             */
            if (count >= 25) {
                break;
            }

            String colour =
                    game.isWhite(playerId)
                            ? "White"
                            : "Black";

            String opponentId =
                    game.isWhite(playerId)
                            ? game.getBlackPlayerId()
                            : game.getWhitePlayerId();

            boolean yourTurn =
                    isPlayersTurn(
                            game,
                            playerId
                    );

            String turnText =
                    yourTurn
                            ? "Your turn"
                            : "Opponent's turn";

            String label =
                    "Game "
                            + game.getShortGameId()
                            + " • "
                            + colour;

            String description =
                    "Opponent: "
                            + opponentId
                            + " • "
                            + turnText;

            /*
             * Discord limits select option descriptions.
             * Keep it safely below the limit.
             */
            if (description.length() > 100) {

                description =
                        description.substring(
                                0,
                                100
                        );
            }

            menu.addOption(
                    label,
                    game.getGameId(),
                    description
            );

            count++;
        }

        event.getChannel()
                .sendMessage(
                        "♟️ You have multiple active games.\n"
                                + "Choose which game should receive **"
                                + rawMove
                                + "**:"
                )
                .addComponents(
                        ActionRow.of(
                                menu.build()
                        )
                )
                .queue();
    }

    // ==========================================================
    // TURN CHECK FOR SELECTOR DISPLAY
    // ==========================================================

    private boolean isPlayersTurn(
            GameSession game,
            String playerId
    ) {

        if (game.isWhite(playerId)) {

            return game.getSideToMove()
                    == Side.WHITE;
        }

        if (game.isBlack(playerId)) {

            return game.getSideToMove()
                    == Side.BLACK;
        }

        return false;
    }
}