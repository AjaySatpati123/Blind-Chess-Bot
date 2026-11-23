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

        if ("abcdefghkqrbn".indexOf(first)
                < 0) {

            return false;
        }

        /*
         * Prevent normal chat such as:
         *
         * hello
         * nice
         * bro
         *
         * from being interpreted as moves.
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

        String move =
                input.trim();

        if (move.isEmpty()) {
            return candidates;
        }

        /*
         * Allow:
         *
         * 0-0
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

        /*
         * Normalize user input first.
         */
        move =
                move.toLowerCase(
                        Locale.ROOT
                );

        // =========================
        // PROMOTION
        // =========================

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

        // =========================
        // KING
        // QUEEN
        // ROOK
        // KNIGHT
        // =========================

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

        // =========================
        // B IS SPECIAL
        // =========================

        /*
         * "b" can mean:
         *
         * b4      -> b-pawn
         * bxc5    -> b-pawn capture
         *
         * bf4     -> user intended Bf4
         * bxe5    -> possibly Bxe5
         * bbd2    -> Bbd2
         * b1d2    -> B1d2
         *
         * We try pawn interpretation FIRST.
         *
         * If that isn't legal, MoveService tries
         * the Bishop interpretation.
         */
        if (first == 'b') {

            candidates.add(
                    move
            );

            String bishopCandidate =
                    "B"
                            + move.substring(1);

            if (!bishopCandidate.equals(
                    move
            )) {

                candidates.add(
                        bishopCandidate
                );
            }

            return candidates;
        }

        // =========================
        // PAWN MOVE
        // =========================

        candidates.add(
                move
        );

        return candidates;
    }
}