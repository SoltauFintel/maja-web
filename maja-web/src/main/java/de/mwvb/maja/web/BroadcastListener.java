package de.mwvb.maja.web;

public interface BroadcastListener {

	/**
	 * @param topic
	 * @param data
	 */
	void handle(String topic, String data);
}
