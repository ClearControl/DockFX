package org.dockfx.dock;

import javafx.scene.control.Button;

/**
 * Base class for a dock indicator button that allows it to be displayed during
 * a dock event and continue to receive input.
 *
 * @since DockFX 0.1
 */
public class DockPosButton extends Button {

	/**
	 * Whether this dock indicator button is used for docking a node relative to the
	 * root of the dock pane.
	 */
	private final boolean dockRoot;
	/**
	 * The docking position indicated by this button.
	 */
	private final DockPos dockPos ;

	/**
	 * Creates a new dock indicator button.
	 */
	public DockPosButton(boolean dockRoot, DockPos dockPos) {
		this.dockRoot = dockRoot;
		this.dockPos = dockPos;
	}

	
	/**
	 * The docking position indicated by this button.
	 *
	 * @return The docking position indicated by this button.
	 */
	public DockPos getDockPos() {
		return dockPos;
	}

	/**
	 * Whether this dock indicator button is used for docking a node relative to the
	 * root of the dock pane.
	 *
	 * @return Whether this indicator button is used for docking a node relative to
	 *         the root of the dock pane.
	 */
	public boolean isDockRoot() {
		return dockRoot;
	}
}