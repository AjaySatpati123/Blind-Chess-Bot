package com.ajay.blindchess.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public final class SanUtils {

    private SanUtils() {
    }

    /*
     * Supported SAN shapes:
     *
     * e4
     * e8=Q
     * exd5
     * exd8=Q+
     *
     * Nf3
     * Nbd2
     * N1d2
     * R1e2
     * Bxe5
     *
     * O-O
     * O-O-O
     *
     * Optional:
     * +
     * #
     */
    private static final Pattern SAN_PATTERN =
            Pattern.compile(
                    "^(?:"
                            + "O-O(?:-O)?"
                            + "|"
                            + "[KQRBN](?:[a-h]|[1-8]|[a-h][1-8])?x?[a-h][1-8]"
                            + "|"
                            + "[a-h]x[a-h][1-8](?:=[QRBN])?"
                            + "|"
                            + "[a-h][1-8](?:=[QRBN])?"
                            + ")[+#]?$"
            );

    // ==========================================================
    // DOES THIS LOOK LIKE A CHESS MOVE?
    // ==========================================================

    public static boolean isPotentialMoveInput(
            String input
    ) {

        if (input == null) {
            return false;
        }

        String move =
                input.trim();

        if (move.isEmpty()) {
            return false;
        }

        if (move.length() > 12) {
            return false;
        }

        if (move.contains(" ")) {
            return false;
        }

        // Castling
        if (move.equalsIgnoreCase("O-O")
                || move.equalsIgnoreCase("0-0")
                || move.equalsIgnoreCase("O-O-O")
                || move.equalsIgnoreCase("0-0-0")) {

            return true;
        }

        char first =
                Character.toLowerCase(
                        move.charAt(0)
                );

        if ("abcdefghkqrbn".indexOf(first) < 0) {
            return false;
        }

        /*
         * Prevent normal Discord chat such as:
         *
         * hello
         * nice
         * bro
         *
         * from being interpreted as chess moves.
         *
         * Normal SAN moves always contain a rank digit,
         * except castling, which was already handled above.
         */
        return move.chars()
                .anyMatch(
                        Character::isDigit
                );
    }

    // ==========================================================
    // BASIC SAN SHAPE VALIDATION
    // ==========================================================

    public static boolean hasValidSanShape(
            String input
    ) {

        List<String> candidates =
                normalizedCandidates(
                        input
                );

        for (String candidate : candidates) {

            if (SAN_PATTERN
                    .matcher(candidate)
                    .matches()) {

                return true;
            }
        }

        return false;
    }

    // ==========================================================
    // NORMALIZATION
    // ==========================================================

    public static List<String> normalizedCandidates(
            String input
    ) {

        List<String> candidates =
                new ArrayList<>();

        if (input == null) {
            return candidates;
        }

        String originalMove =
                input.trim();

        if (originalMove.isEmpty()) {
            return candidates;
        }

        /*
         * IMPORTANT:
         *
         * Preserve whether the user explicitly typed
         * uppercase B BEFORE converting anything to lowercase.
         *
         * Bxc5 = definitely Bishop
         * bxc5 = potentially b-pawn OR lowercase Bishop input
         */
        boolean explicitlyUppercaseB =
                originalMove.charAt(0) == 'B';

        String move =
                originalMove;

        // ======================================================
        // CASTLING
        // ======================================================

        /*
         * Allow:
         *
         * O-O
         * o-o
         * 0-0
         *
         * O-O-O
         * o-o-o
         * 0-0-0
         */
        move =
                move.replace(
                        '0',
                        'O'
                );

        if (move.equalsIgnoreCase(
                "O-O"
        )) {

            candidates.add(
                    "O-O"
            );

            return candidates;
        }

        if (move.equalsIgnoreCase(
                "O-O-O"
        )) {

            candidates.add(
                    "O-O-O"
            );

            return candidates;
        }

        // ======================================================
        // NORMALIZE CASE
        // ======================================================

        move =
                move.toLowerCase(
                        Locale.ROOT
                );

        // ======================================================
        // PROMOTION
        // ======================================================

        int promotionIndex =
                move.indexOf('=');

        if (promotionIndex >= 0
                && promotionIndex + 1
                < move.length()) {

            char promotionPiece =
                    Character.toUpperCase(
                            move.charAt(
                                    promotionIndex + 1
                            )
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

        char first =
                move.charAt(0);

        // ======================================================
        // KING / QUEEN / ROOK / KNIGHT
        // ======================================================

        if (first == 'k'
                || first == 'q'
                || first == 'r'
                || first == 'n') {

            candidates.add(
                    Character.toUpperCase(first)
                            + move.substring(1)
            );

            return candidates;
        }

        // ======================================================
        // BISHOP / B-PAWN AMBIGUITY
        // ======================================================

        if (first == 'b') {

            /*
             * User explicitly typed uppercase B.
             *
             * Examples:
             *
             * Bf4
             * Bxc5
             * Bbd2
             * B1d2
             *
             * These MUST always mean Bishop.
             */
            if (explicitlyUppercaseB) {

                candidates.add(
                        "B"
                                + move.substring(1)
                );

                return candidates;
            }

            /*
             * Lowercase b is ambiguous because we allow
             * users to type piece moves in lowercase.
             *
             * Examples:
             *
             * b4
             *      -> b-pawn move
             *
             * bxc5
             *      -> could be b-pawn capture
             *      -> could mean Bxc5 typed lowercase
             *
             * bf4
             *      -> user probably means Bf4
             *
             * Therefore:
             *
             * 1. Try pawn interpretation
             * 2. Try Bishop interpretation
             *
             * MoveService will choose whichever is actually
             * legal in the current board position.
             */

            candidates.add(
                    move
            );

            candidates.add(
                    "B"
                            + move.substring(1)
            );

            return candidates;
        }

        // ======================================================
        // PAWN MOVE
        // ======================================================

        candidates.add(
                move
        );

        return candidates;
    }
}