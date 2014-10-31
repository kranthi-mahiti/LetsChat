package com.mstr.letschat.tasks;

import java.lang.ref.WeakReference;

import android.os.AsyncTask;

import com.mstr.letschat.utils.XMPPUtils;

public class SendChatMessageTask extends AsyncTask<Void, Void, Boolean> {
	
	public static interface SendChatMessageListener {
		public void onChatMessageSent(boolean result);
	}
	
	private String to;
	private String body;
	
	private WeakReference<SendChatMessageListener> listener;
	
	public SendChatMessageTask(SendChatMessageListener listener, String to, String body) {
		this.listener = new WeakReference<SendChatMessageListener>(listener);
		
		this.to = to;
		this.body = body;
	}
	
	@Override
	public Boolean doInBackground(Void... params) {
		return XMPPUtils.sendChatMessage(to, body);
	}
	
	@Override
	public void onPostExecute(Boolean result) {
		SendChatMessageListener l = listener.get();
		
		if (l != null) {
			l.onChatMessageSent(result);
		}
	}
}