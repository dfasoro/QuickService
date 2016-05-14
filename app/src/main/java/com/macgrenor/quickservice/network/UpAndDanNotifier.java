package com.macgrenor.quickservice.network;

public interface UpAndDanNotifier {
	public void initReady();
	public void newServices(int count);
	public void updatedServices(int count);
}
