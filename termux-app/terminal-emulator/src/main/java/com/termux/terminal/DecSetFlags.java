package com.termux.terminal;

/**
 * Manages DECSET (DEC Private Mode Set) flags for terminal emulation.
 * This class encapsulates the bit flags used for various terminal modes.
 */
public final class DecSetFlags {
    
    /** DECSET 1 - application cursor keys. */
    public static final int APPLICATION_CURSOR_KEYS = 1;
    public static final int REVERSE_VIDEO = 1 << 1;
    /**
     * http://www.vt100.net/docs/vt510-rm/DECOM: "When DECOM is set, the home cursor position is at the upper-left
     * corner of the screen, within the margins. The starting point for line numbers depends on the current top margin
     * setting. The cursor cannot move outside of the margins. When DECOM is reset, the home cursor position is at the
     * upper-left corner of the screen. The starting point for line numbers is independent of the margins. The cursor
     * can move outside of the margins."
     */
    public static final int ORIGIN_MODE = 1 << 2;
    /**
     * http://www.vt100.net/docs/vt510-rm/DECAWM: "If the DECAWM function is set, then graphic characters received when
     * the cursor is at the right border of the page appear at the beginning of the next line. Any text on the page
     * scrolls up if the cursor is at the end of the scrolling region. If the DECAWM function is reset, then graphic
     * characters received when the cursor is at the right border of the page replace characters already on the page."
     */
    public static final int AUTOWRAP = 1 << 3;
    /** DECSET 25 - if the cursor should be visible, {@link #isShowingCursor()}. */
    public static final int SHOWING_CURSOR = 1 << 4;
    public static final int APPLICATION_KEYPAD = 1 << 5;
    /** DECSET 1000 - if to report mouse press&release events. */
    public static final int MOUSE_TRACKING_PRESS_RELEASE = 1 << 6;
    /** DECSET 1002 - like 1000, but report moving mouse while pressed. */
    public static final int MOUSE_TRACKING_BUTTON_EVENT = 1 << 7;
    /** DECSET 1004 - NOT implemented. */
    public static final int SEND_FOCUS_EVENTS = 1 << 8;
    /** DECSET 1006 - SGR-like mouse protocol (the modern sane choice). */
    public static final int MOUSE_PROTOCOL_SGR = 1 << 9;
    /** DECSET 2004 - see {@link TerminalEmulator#paste(String)} */
    public static final int BRACKETED_PASTE_MODE = 1 << 10;
    /** Toggled with DECLRMM - http://www.vt100.net/docs/vt510-rm/DECLRMM */
    public static final int LEFTRIGHT_MARGIN_MODE = 1 << 11;
    /** Not really DECSET bit... - http://www.vt100.net/docs/vt510-rm/DECSACE */
    public static final int RECTANGULAR_CHANGEATTRIBUTE = 1 << 12;

    private DecSetFlags() {
        // Utility class, prevent instantiation
    }
    
    /**
     * Maps a DECSET bit number to its internal representation.
     * @param decsetBit The DECSET bit number
     * @return The internal bit flag, or -1 if not recognized
     */
    public static int mapDecSetBitToInternalBit(int decsetBit) {
        switch (decsetBit) {
            case 1: return APPLICATION_CURSOR_KEYS;
            case 2: return REVERSE_VIDEO;
            case 3: return ORIGIN_MODE;
            case 4: return AUTOWRAP;
            case 5: return SHOWING_CURSOR;
            case 6: return APPLICATION_KEYPAD;
            case 1000: return MOUSE_TRACKING_PRESS_RELEASE;
            case 1002: return MOUSE_TRACKING_BUTTON_EVENT;
            case 1004: return SEND_FOCUS_EVENTS;
            case 1006: return MOUSE_PROTOCOL_SGR;
            case 2004: return BRACKETED_PASTE_MODE;
            case 69: return LEFTRIGHT_MARGIN_MODE; // DECLRMM
            default: return -1;
        }
    }
}
