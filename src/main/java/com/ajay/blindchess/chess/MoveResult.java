package com.ajay.blindchess.chess;

public record MoveResult(
        boolean success,
        String message
) {
}