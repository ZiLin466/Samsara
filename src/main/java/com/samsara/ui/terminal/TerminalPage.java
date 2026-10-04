package com.samsara.ui.terminal;

/** Limits terminal styling to Samsara's own screens, including their native widgets. */
public interface TerminalPage {
   String terminalTitle();
   String terminalCode();
   String terminalDescription();
   String terminalStatus();
}
