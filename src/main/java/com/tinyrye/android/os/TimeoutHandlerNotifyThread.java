package com.tinyrye.android.os;

import java.util.concurrent.atomic.AtomicBoolean;

import android.os.Handler;

public class TimeoutHandlerNotifyThread extends Thread
{
	private final AtomicBoolean activeLoopFlag;
	private final Handler timeoutNotify;

	public TimeoutHandlerNotifyThread(AtomicBoolean activeLoopFlag, Handler timeoutNotify) {
		this.activeLoopFlag = activeLoopFlag;
		this.timeoutNotify = timeoutNotify;
	}

	@Override
	public void run() {
		while (activeLoopFlag.get()) {
			timeoutNotify.sendEmptyMessage(0);
			if (! sleep()) return;
		}
	}

	/**
	 * Default is 1 second.
	 * @return false if thread was interrupted implying this thread should exit.
	 */
	public boolean sleep() {
		try { Thread.sleep(1000L); return true; }
		catch (InterruptedException ex) { return false; }
	}
}