package statuscheck.ui;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public final class AppState {

	private final BooleanProperty scanning = new SimpleBooleanProperty(false);
	private final StringProperty notice = new SimpleStringProperty("");

	public BooleanProperty scanningProperty() { return scanning; }
	public boolean isScanning() { return scanning.get(); }
	public void setScanning(boolean value) { scanning.set(value); }

	public StringProperty noticeProperty() { return notice; }
	public String getNotice() { return notice.get(); }
	public void setNotice(String value) { notice.set(value); }
}