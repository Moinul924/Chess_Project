package com.chess.account;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// The player's settings as the settings page sends and receives them.
// More settings (show legal moves, engine depth, ...) can be added here later.
public record SettingsData(
    @NotBlank @Size(max = 30) String boardTheme) {}
