package org.openremote.manager.server.notification;

public class Notification {

	String body;
	boolean mutable_content;
	String click_action = "blok61Notification";

	public Notification(String body, boolean mutable_content) {
		super();
		this.body = body;
		this.mutable_content = mutable_content;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public boolean getMutable_content() {
		return mutable_content;
	}

	public void setMutable_content(boolean mutable_content) {
		this.mutable_content = mutable_content;
	}

	public String getClick_action() {
		return click_action;
	}
}
