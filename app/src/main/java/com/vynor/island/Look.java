package com.vynor.island;

/** Shared color data for the app UI themes and the island colors. */
final class Look {
    static final String[] THEME_NAMES = {"Aurora", "Sunset", "Ocean", "Forest", "Midnight"};
    // 5 page colors per theme (one per page, blended while swiping)
    static final int[][] THEMES = {
            {0xFF3F51B5, 0xFFD81B60, 0xFF0277BD, 0xFF00897B, 0xFF6A1B9A},
            {0xFFE65100, 0xFFC2185B, 0xFFAD1457, 0xFF6A1B9A, 0xFFBF360C},
            {0xFF006064, 0xFF0277BD, 0xFF1565C0, 0xFF283593, 0xFF00838F},
            {0xFF1B5E20, 0xFF2E7D32, 0xFF00695C, 0xFF33691E, 0xFF004D40},
            {0xFF0D0D12, 0xFF1A1033, 0xFF0B1F33, 0xFF0E2A22, 0xFF1F1020}};
    // Black, Graphite, Indigo, Rose, White (index 4 uses dark text)
    static final int[] ISLAND = {0xFF000000, 0xFF2B2B30, 0xFF3F51B5, 0xFFD81B60, 0xFFFFFFFF};
}
