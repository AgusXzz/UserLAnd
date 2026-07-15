package com.termux.terminal;

/**
 * Manages escape sequence states for terminal emulation.
 * This class encapsulates the state machine used for processing escape sequences.
 */
public final class EscapeState {
    
    /** Escape processing: Not currently in an escape sequence. */
    public static final int NONE = 0;
    /** Escape processing: Have seen an ESC character - proceed to {@link #doEsc(int)} */
    public static final int ESC = 1;
    /** Escape processing: Have seen ESC POUND */
    public static final int ESC_POUND = 2;
    /** Escape processing: Have seen ESC and a character-set-select ( char */
    public static final int ESC_SELECT_LEFT_PAREN = 3;
    /** Escape processing: Have seen ESC and a character-set-select ) char */
    public static final int ESC_SELECT_RIGHT_PAREN = 4;
    /** Escape processing: "ESC [" or CSI (Control Sequence Introducer). */
    public static final int ESC_CSI = 6;
    /** Escape processing: ESC [ ? */
    public static final int ESC_CSI_QUESTIONMARK = 7;
    /** Escape processing: ESC [ $ */
    public static final int ESC_CSI_DOLLAR = 8;
    /** Escape processing: ESC % */
    public static final int ESC_PERCENT = 9;
    /** Escape processing: ESC ] (AKA OSC - Operating System Controls) */
    public static final int ESC_OSC = 10;
    /** Escape processing: ESC ] (AKA OSC - Operating System Controls) ESC */
    public static final int ESC_OSC_ESC = 11;
    /** Escape processing: ESC [ > */
    public static final int ESC_CSI_BIGGERTHAN = 12;
    /** Escape procession: "ESC P" or Device Control String (DCS) */
    public static final int ESC_P = 13;
    /** Escape processing: CSI > */
    public static final int ESC_CSI_QUESTIONMARK_ARG_DOLLAR = 14;
    /** Escape processing: CSI $ARGS ' ' */
    public static final int ESC_CSI_ARGS_SPACE = 15;
    /** Escape processing: CSI $ARGS '*' */
    public static final int ESC_CSI_ARGS_ASTERIX = 16;
    /** Escape processing: CSI " */
    public static final int ESC_CSI_DOUBLE_QUOTE = 17;
    /** Escape processing: CSI ' */
    public static final int ESC_CSI_SINGLE_QUOTE = 18;
    /** Escape processing: CSI ! */
    public static final int ESC_CSI_EXCLAMATION = 19;

    /** The number of parameter arguments. This name comes from the ANSI standard for terminal escape codes. */
    public static final int MAX_ESCAPE_PARAMETERS = 16;

    /** Needs to be large enough to contain reasonable OSC 52 pastes. */
    public static final int MAX_OSC_STRING_LENGTH = 8192;

    private EscapeState() {
        // Utility class, prevent instantiation
    }
}
