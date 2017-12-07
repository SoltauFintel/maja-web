package de.mwvb.maja.web;

import java.util.ArrayList;
import java.util.List;

import com.google.inject.Singleton;

@Singleton
public class Broadcaster {
	private final List<BroadcastListener> listeners = new ArrayList<>();
	
	public void addListener(BroadcastListener listener) {
		listeners.add(listener);
	}
	
	public void broadcast(String topic, String data) {
		System.out.println(listeners.size() + " listeners: " + topic + " = " + data);
		listeners.forEach(listener -> listener.handle(topic, data));
	}
}
