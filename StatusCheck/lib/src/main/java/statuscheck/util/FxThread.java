package statuscheck.util;

import javafx.application.Platform;

/** Small helpers for the "all state is touched on the JavaFX thread only" rule. */
public final class FxThread {

	private FxThread() {}

	/** Runs inline if already on the FX thread, otherwise queues it. */
	public static void run(Runnable action) {
		if (Platform.isFxApplicationThread()) {
			action.run();
		} else {
			Platform.runLater(action);
		}
	}

	/** Fails fast (assert-style, but always enabled) when called off the FX thread. */
	public static void require() {
		if (!Platform.isFxApplicationThread()) {
			throw new IllegalStateException("Must run on the JavaFX application thread");
		}
	}
}