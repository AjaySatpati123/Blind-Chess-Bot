package com.ajay.blindchess.board;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.Square;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.parser.SVGLoader;
import com.github.weisj.jsvg.view.ViewBox;

import javax.imageio.ImageIO;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import java.awt.image.BufferedImage;

import java.io.ByteArrayOutputStream;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class BoardRenderer {

    private BoardRenderer() {
    }

    private static final int SQUARE_SIZE = 90;

    private static final int BOARD_SIZE =
            SQUARE_SIZE * 8;

    private static final int COORDINATE_MARGIN = 30;

    private static final int PLAYER_PANEL_HEIGHT = 72;

    private static final int IMAGE_WIDTH =
            BOARD_SIZE
                    + (COORDINATE_MARGIN * 2);

    private static final int IMAGE_HEIGHT =
            BOARD_SIZE
                    + (COORDINATE_MARGIN * 2)
                    + (PLAYER_PANEL_HEIGHT * 2);

    private static final int BOARD_X =
            COORDINATE_MARGIN;

    private static final int BOARD_Y =
            PLAYER_PANEL_HEIGHT
                    + COORDINATE_MARGIN;

    private static final int PIECE_PADDING = 7;

    private static final int PIECE_SIZE =
            SQUARE_SIZE
                    - (PIECE_PADDING * 2);

    private static final Color LIGHT_SQUARE =
            new Color(
                    240,
                    217,
                    181
            );

    private static final Color DARK_SQUARE =
            new Color(
                    181,
                    136,
                    99
            );

    private static final Color BACKGROUND =
            new Color(
                    24,
                    24,
                    24
            );

    private static final Color PANEL =
            new Color(
                    31,
                    31,
                    31
            );

    private static final Color TEXT =
            new Color(
                    240,
                    240,
                    240
            );

    private static final Color SECONDARY_TEXT =
            new Color(
                    170,
                    170,
                    170
            );

    private static final Color BORDER =
            new Color(
                    70,
                    70,
                    70
            );

    private static final Map<String, SVGDocument>
            PIECE_CACHE =
            new ConcurrentHashMap<>();

    // ==========================================================
    // STANDARD BOARD
    // White at bottom
    // ==========================================================

    public static byte[] renderBoard(
            Board board,
            String blackPlayerName,
            String whitePlayerName
    ) {

        return renderBoard(
                board,
                blackPlayerName,
                whitePlayerName,
                true
        );
    }

    // ==========================================================
    // ORIENTED BOARD
    // ==========================================================

    public static byte[] renderBoard(
            Board board,
            String blackPlayerName,
            String whitePlayerName,
            boolean whiteAtBottom
    ) {

        try {

            BufferedImage image =
                    new BufferedImage(
                            IMAGE_WIDTH,
                            IMAGE_HEIGHT,
                            BufferedImage.TYPE_INT_ARGB
                    );

            Graphics2D graphics =
                    image.createGraphics();

            configureGraphics(
                    graphics
            );

            // Background
            graphics.setColor(
                    BACKGROUND
            );

            graphics.fillRect(
                    0,
                    0,
                    IMAGE_WIDTH,
                    IMAGE_HEIGHT
            );

            // Player panels
            graphics.setColor(
                    PANEL
            );

            graphics.fillRect(
                    0,
                    0,
                    IMAGE_WIDTH,
                    PLAYER_PANEL_HEIGHT
            );

            graphics.fillRect(
                    0,
                    IMAGE_HEIGHT
                            - PLAYER_PANEL_HEIGHT,
                    IMAGE_WIDTH,
                    PLAYER_PANEL_HEIGHT
            );

            String topName =
                    whiteAtBottom
                            ? blackPlayerName
                            : whitePlayerName;

            String bottomName =
                    whiteAtBottom
                            ? whitePlayerName
                            : blackPlayerName;

            String topColour =
                    whiteAtBottom
                            ? "BLACK"
                            : "WHITE";

            String bottomColour =
                    whiteAtBottom
                            ? "WHITE"
                            : "BLACK";

            drawPlayerPanel(
                    graphics,
                    topName,
                    topColour,
                    true
            );

            drawPlayerPanel(
                    graphics,
                    bottomName,
                    bottomColour,
                    false
            );

            drawBoardSquares(
                    graphics,
                    board,
                    whiteAtBottom
            );

            drawCoordinates(
                    graphics,
                    whiteAtBottom
            );

            // Border
            graphics.setColor(
                    BORDER
            );

            graphics.setStroke(
                    new BasicStroke(2)
            );

            graphics.drawRect(
                    BOARD_X - 1,
                    BOARD_Y - 1,
                    BOARD_SIZE + 1,
                    BOARD_SIZE + 1
            );

            graphics.dispose();

            ByteArrayOutputStream output =
                    new ByteArrayOutputStream();

            ImageIO.write(
                    image,
                    "png",
                    output
            );

            return output.toByteArray();

        } catch (Exception e) {

            throw new RuntimeException(
                    "Could not render chess board.",
                    e
            );
        }
    }

    private static void configureGraphics(
            Graphics2D graphics
    ) {

        graphics.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
        );

        graphics.setRenderingHint(
                RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON
        );

        graphics.setRenderingHint(
                RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY
        );

        graphics.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC
        );
    }

    // ==========================================================
    // BOARD
    // ==========================================================

    private static void drawBoardSquares(
            Graphics2D graphics,
            Board board,
            boolean whiteAtBottom
    ) {

        for (
                int displayRow = 0;
                displayRow < 8;
                displayRow++
        ) {

            for (
                    int displayColumn = 0;
                    displayColumn < 8;
                    displayColumn++
            ) {

                int fileIndex;
                int rankIndex;

                if (whiteAtBottom) {

                    fileIndex =
                            displayColumn;

                    rankIndex =
                            7 - displayRow;

                } else {

                    fileIndex =
                            7 - displayColumn;

                    rankIndex =
                            displayRow;
                }

                int x =
                        BOARD_X
                                + displayColumn
                                * SQUARE_SIZE;

                int y =
                        BOARD_Y
                                + displayRow
                                * SQUARE_SIZE;

                boolean light =
                        (fileIndex + rankIndex)
                                % 2
                                != 0;

                graphics.setColor(
                        light
                                ? LIGHT_SQUARE
                                : DARK_SQUARE
                );

                graphics.fillRect(
                        x,
                        y,
                        SQUARE_SIZE,
                        SQUARE_SIZE
                );

                Square square =
                        getSquare(
                                fileIndex,
                                rankIndex
                        );

                Piece piece =
                        board.getPiece(
                                square
                        );

                if (piece
                        != Piece.NONE) {

                    drawPiece(
                            graphics,
                            piece,
                            x + PIECE_PADDING,
                            y + PIECE_PADDING
                    );
                }
            }
        }
    }

    private static Square getSquare(
            int fileIndex,
            int rankIndex
    ) {

        char file =
                (char) (
                        'A'
                                + fileIndex
                );

        int rank =
                rankIndex + 1;

        return Square.valueOf(
                String.valueOf(file)
                        + rank
        );
    }

    // ==========================================================
    // COORDINATES
    // ==========================================================

    private static void drawCoordinates(
            Graphics2D graphics,
            boolean whiteAtBottom
    ) {

        graphics.setFont(
                new Font(
                        "SansSerif",
                        Font.BOLD,
                        14
                )
        );

        graphics.setColor(
                SECONDARY_TEXT
        );

        FontMetrics metrics =
                graphics.getFontMetrics();

        for (
                int displayColumn = 0;
                displayColumn < 8;
                displayColumn++
        ) {

            int fileIndex =
                    whiteAtBottom
                            ? displayColumn
                            : 7 - displayColumn;

            String file =
                    String.valueOf(
                            (char) (
                                    'a'
                                            + fileIndex
                            )
                    );

            int squareCenter =
                    BOARD_X
                            + displayColumn
                            * SQUARE_SIZE
                            + SQUARE_SIZE / 2;

            int textX =
                    squareCenter
                            - metrics.stringWidth(file)
                            / 2;

            // Top coordinate
            graphics.drawString(
                    file,
                    textX,
                    BOARD_Y - 9
            );

            // Bottom coordinate
            graphics.drawString(
                    file,
                    textX,
                    BOARD_Y
                            + BOARD_SIZE
                            + 20
            );
        }

        for (
                int displayRow = 0;
                displayRow < 8;
                displayRow++
        ) {

            int rankIndex =
                    whiteAtBottom
                            ? 7 - displayRow
                            : displayRow;

            String rank =
                    String.valueOf(
                            rankIndex + 1
                    );

            int squareCenter =
                    BOARD_Y
                            + displayRow
                            * SQUARE_SIZE
                            + SQUARE_SIZE / 2;

            int textY =
                    squareCenter
                            + metrics.getAscent()
                            / 2
                            - 2;

            // Left
            graphics.drawString(
                    rank,
                    9,
                    textY
            );

            // Right
            graphics.drawString(
                    rank,
                    BOARD_X
                            + BOARD_SIZE
                            + 10,
                    textY
            );
        }
    }

    // ==========================================================
    // PLAYER PANEL
    // ==========================================================

    private static void drawPlayerPanel(
            Graphics2D graphics,
            String playerName,
            String colour,
            boolean top
    ) {

        if (playerName == null
                || playerName.isBlank()) {

            playerName =
                    colour.equals("WHITE")
                            ? "White Player"
                            : "Black Player";
        }

        int panelStartY =
                top
                        ? 0
                        : IMAGE_HEIGHT
                        - PLAYER_PANEL_HEIGHT;

        graphics.setColor(
                SECONDARY_TEXT
        );

        graphics.setFont(
                new Font(
                        "SansSerif",
                        Font.BOLD,
                        13
                )
        );

        graphics.drawString(
                colour,
                24,
                panelStartY + 24
        );

        Font playerFont =
                fitFont(
                        graphics,
                        playerName,
                        IMAGE_WIDTH - 48,
                        28
                );

        graphics.setFont(
                playerFont
        );

        graphics.setColor(
                TEXT
        );

        graphics.drawString(
                playerName,
                24,
                panelStartY + 54
        );
    }

    private static Font fitFont(
            Graphics2D graphics,
            String text,
            int maxWidth,
            int maximumSize
    ) {

        int size =
                maximumSize;

        while (size > 14) {

            Font font =
                    new Font(
                            "SansSerif",
                            Font.BOLD,
                            size
                    );

            FontMetrics metrics =
                    graphics.getFontMetrics(
                            font
                    );

            if (metrics.stringWidth(text)
                    <= maxWidth) {

                return font;
            }

            size--;
        }

        return new Font(
                "SansSerif",
                Font.BOLD,
                14
        );
    }

    // ==========================================================
    // SVG PIECES
    // ==========================================================

    private static void drawPiece(
            Graphics2D graphics,
            Piece piece,
            int x,
            int y
    ) {

        SVGDocument document =
                getPieceDocument(
                        piece
                );

        if (document == null) {
            return;
        }

        synchronized (document) {

            document.render(
                    null,
                    graphics,
                    new ViewBox(
                            x,
                            y,
                            PIECE_SIZE,
                            PIECE_SIZE
                    )
            );
        }
    }

    private static SVGDocument getPieceDocument(
            Piece piece
    ) {

        String resourceName =
                piece.name()
                        .toLowerCase(
                                Locale.ROOT
                        )
                        .replace(
                                '_',
                                '-'
                        )
                        + ".svg";

        return PIECE_CACHE.computeIfAbsent(
                resourceName,
                BoardRenderer::loadSvg
        );
    }

    private static SVGDocument loadSvg(
            String file
    ) {

        String path =
                "pieces/" + file;

        try {

            java.net.URL url =
                    BoardRenderer.class
                            .getClassLoader()
                            .getResource(
                                    path
                            );

            if (url == null) {

                throw new IllegalStateException(
                        "Missing SVG resource: "
                                + path
                );
            }

            SVGLoader loader =
                    new SVGLoader();

            SVGDocument document =
                    loader.load(
                            url
                    );

            if (document == null) {

                throw new IllegalStateException(
                        "Could not parse SVG: "
                                + path
                );
            }

            return document;

        } catch (Exception e) {

            throw new RuntimeException(
                    "Could not load SVG: "
                            + path,
                    e
            );
        }
    }
}