package com.ajay.blindchess.board;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.Square;
import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.parser.SVGLoader;
import com.github.weisj.jsvg.view.ViewBox;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BoardRenderer {

    // =========================
    // BOARD SIZE
    // =========================

    private static final int SQUARE_SIZE = 90;

    private static final int BOARD_SIZE =
            SQUARE_SIZE * 8;

    private static final int LABEL_HEIGHT = 65;

    private static final int IMAGE_WIDTH =
            BOARD_SIZE;

    private static final int IMAGE_HEIGHT =
            BOARD_SIZE + (LABEL_HEIGHT * 2);

    // Space between piece and edge of square
    private static final int PIECE_PADDING = 7;

    private static final int PIECE_SIZE =
            SQUARE_SIZE - (PIECE_PADDING * 2);

    // =========================
    // BOARD COLORS
    // =========================

    private static final Color LIGHT_SQUARE =
            new Color(240, 217, 181);

    private static final Color DARK_SQUARE =
            new Color(181, 136, 99);

    private static final Color BACKGROUND =
            new Color(24, 24, 24);

    private static final Color PLAYER_NAME_COLOR =
            Color.WHITE;

    // =========================
    // SVG CACHE
    // =========================

    /*
     * SVG files are loaded only once.
     *
     * After that, the parsed SVGDocument is reused
     * whenever another board is rendered.
     */
    private static final Map<String, SVGDocument> PIECE_CACHE =
            new ConcurrentHashMap<>();

    private BoardRenderer() {
    }

    // ==========================================================
    // PUBLIC METHOD
    // ==========================================================

    public static byte[] renderBoard(
            Board board,
            String blackPlayerName,
            String whitePlayerName
    ) {

        BufferedImage image =
                new BufferedImage(
                        IMAGE_WIDTH,
                        IMAGE_HEIGHT,
                        BufferedImage.TYPE_INT_ARGB
                );

        Graphics2D graphics =
                image.createGraphics();

        try {

            configureGraphics(graphics);

            // Entire background
            graphics.setColor(BACKGROUND);

            graphics.fillRect(
                    0,
                    0,
                    IMAGE_WIDTH,
                    IMAGE_HEIGHT
            );

            // Black player name on top
            drawPlayerName(
                    graphics,
                    blackPlayerName,
                    LABEL_HEIGHT / 2
            );

            // Chess board
            drawChessBoard(
                    graphics,
                    board
            );

            // White player name at bottom
            drawPlayerName(
                    graphics,
                    whitePlayerName,
                    LABEL_HEIGHT
                            + BOARD_SIZE
                            + LABEL_HEIGHT / 2
            );

        } finally {

            graphics.dispose();
        }

        return convertToPng(image);
    }

    // ==========================================================
    // GRAPHICS QUALITY
    // ==========================================================

    private static void configureGraphics(
            Graphics2D graphics
    ) {

        graphics.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
        );

        graphics.setRenderingHint(
                RenderingHints.KEY_STROKE_CONTROL,
                RenderingHints.VALUE_STROKE_PURE
        );

        graphics.setRenderingHint(
                RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY
        );

        graphics.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC
        );

        graphics.setRenderingHint(
                RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON
        );
    }

    // ==========================================================
    // DRAW BOARD
    // ==========================================================

    private static void drawChessBoard(
            Graphics2D graphics,
            Board board
    ) {

        /*
         * row 0 = rank 8
         * row 7 = rank 1
         *
         * Therefore:
         *
         * Black is at the top.
         * White is at the bottom.
         */

        for (int row = 0; row < 8; row++) {

            int rank =
                    8 - row;

            for (int file = 0; file < 8; file++) {

                int squareX =
                        file * SQUARE_SIZE;

                int squareY =
                        LABEL_HEIGHT
                                + row * SQUARE_SIZE;

                // Draw square first
                drawSquare(
                        graphics,
                        row,
                        file,
                        squareX,
                        squareY
                );

                Square square =
                        getSquare(
                                file,
                                rank
                        );

                Piece piece =
                        board.getPiece(square);

                if (piece == null
                        || piece == Piece.NONE) {

                    continue;
                }

                drawPiece(
                        graphics,
                        piece,
                        squareX,
                        squareY
                );
            }
        }
    }

    // ==========================================================
    // DRAW SQUARE
    // ==========================================================

    private static void drawSquare(
            Graphics2D graphics,
            int row,
            int file,
            int x,
            int y
    ) {

        boolean lightSquare =
                (row + file) % 2 == 0;

        graphics.setColor(
                lightSquare
                        ? LIGHT_SQUARE
                        : DARK_SQUARE
        );

        graphics.fillRect(
                x,
                y,
                SQUARE_SIZE,
                SQUARE_SIZE
        );
    }

    // ==========================================================
    // FIND CHESSLIB SQUARE
    // ==========================================================

    private static Square getSquare(
            int file,
            int rank
    ) {

        char fileCharacter =
                (char) ('A' + file);

        String squareName =
                String.valueOf(fileCharacter)
                        + rank;

        return Square.valueOf(
                squareName
        );
    }

    // ==========================================================
    // DRAW SVG PIECE
    // ==========================================================

    private static void drawPiece(
            Graphics2D graphics,
            Piece piece,
            int squareX,
            int squareY
    ) {

        SVGDocument document =
                getPieceDocument(piece);

        int x =
                squareX
                        + PIECE_PADDING;

        int y =
                squareY
                        + PIECE_PADDING;

        /*
         * Render the SVG directly into this specific
         * portion of the board.
         *
         * No intermediate PNG is required.
         */
        ViewBox pieceViewport =
                new ViewBox(
                        x,
                        y,
                        PIECE_SIZE,
                        PIECE_SIZE
                );

        /*
         * Synchronize on the document so simultaneous
         * Discord games cannot try rendering the same
         * cached SVG at the exact same moment.
         */
        synchronized (document) {

            document.render(
                    null,
                    graphics,
                    pieceViewport
            );
        }
    }

    // ==========================================================
    // GET SVG DOCUMENT
    // ==========================================================

    private static SVGDocument getPieceDocument(
            Piece piece
    ) {

        String resourcePath =
                getPieceResourcePath(piece);

        return PIECE_CACHE.computeIfAbsent(
                resourcePath,
                BoardRenderer::loadSvg
        );
    }

    // ==========================================================
    // LOAD SVG
    // ==========================================================

    private static SVGDocument loadSvg(
            String resourcePath
    ) {

        URL svgUrl =
                BoardRenderer.class
                        .getClassLoader()
                        .getResource(resourcePath);

        if (svgUrl == null) {

            throw new IllegalStateException(
                    "Chess piece SVG was not found: "
                            + resourcePath
            );
        }

        /*
         * Create a new loader for each load.
         *
         * JSVG documentation notes that SVGLoader itself
         * is not guaranteed to be thread-safe.
         */
        SVGLoader loader =
                new SVGLoader();

        SVGDocument document =
                loader.load(svgUrl);

        if (document == null) {

            throw new IllegalStateException(
                    "Could not load chess piece SVG: "
                            + resourcePath
            );
        }

        return document;
    }

    // ==========================================================
    // PIECE -> SVG FILE
    // ==========================================================

    private static String getPieceResourcePath(
            Piece piece
    ) {

        return switch (piece) {

            // WHITE
            case WHITE_KING ->
                    "pieces/white-king.svg";

            case WHITE_QUEEN ->
                    "pieces/white-queen.svg";

            case WHITE_ROOK ->
                    "pieces/white-rook.svg";

            case WHITE_BISHOP ->
                    "pieces/white-bishop.svg";

            case WHITE_KNIGHT ->
                    "pieces/white-knight.svg";

            case WHITE_PAWN ->
                    "pieces/white-pawn.svg";


            // BLACK
            case BLACK_KING ->
                    "pieces/black-king.svg";

            case BLACK_QUEEN ->
                    "pieces/black-queen.svg";

            case BLACK_ROOK ->
                    "pieces/black-rook.svg";

            case BLACK_BISHOP ->
                    "pieces/black-bishop.svg";

            case BLACK_KNIGHT ->
                    "pieces/black-knight.svg";

            case BLACK_PAWN ->
                    "pieces/black-pawn.svg";


            default ->
                    throw new IllegalArgumentException(
                            "Unsupported chess piece: "
                                    + piece
                    );
        };
    }

    // ==========================================================
    // PLAYER NAME
    // ==========================================================

    private static void drawPlayerName(
            Graphics2D graphics,
            String playerName,
            int centerY
    ) {

        if (playerName == null
                || playerName.isBlank()) {

            playerName = "Player";
        }

        /*
         * Start at 22 and reduce the font slightly
         * if someone has a very long Discord name.
         */
        int fontSize = 22;

        Font font;

        FontMetrics metrics;

        do {

            font =
                    new Font(
                            Font.SANS_SERIF,
                            Font.BOLD,
                            fontSize
                    );

            graphics.setFont(font);

            metrics =
                    graphics.getFontMetrics();

            if (metrics.stringWidth(playerName)
                    <= IMAGE_WIDTH - 30) {

                break;
            }

            fontSize--;

        } while (fontSize > 12);

        int textWidth =
                metrics.stringWidth(playerName);

        int x =
                (IMAGE_WIDTH - textWidth) / 2;

        int y =
                centerY
                        + (
                        metrics.getAscent()
                                - metrics.getDescent()
                ) / 2;

        graphics.setColor(
                PLAYER_NAME_COLOR
        );

        graphics.drawString(
                playerName,
                x,
                y
        );
    }

    // ==========================================================
    // CONVERT TO PNG
    // ==========================================================

    private static byte[] convertToPng(
            BufferedImage image
    ) {

        try {

            ByteArrayOutputStream output =
                    new ByteArrayOutputStream();

            ImageIO.write(
                    image,
                    "png",
                    output
            );

            return output.toByteArray();

        } catch (IOException e) {

            throw new RuntimeException(
                    "Could not generate chess board PNG.",
                    e
            );
        }
    }
}