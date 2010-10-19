package org.mbs3.android.gatorlug.receiver;

import java.util.concurrent.atomic.AtomicBoolean;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootCompletedReceiver extends BroadcastReceiver {
	
	final static AtomicBoolean startedAlarms = new AtomicBoolean(false);
	
	final int REQUEST_CODE = 0;
	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		Log.i("bcr", "("+action+") " + intent.toString());
		
		if (
				action.equals(Intent.ACTION_BOOT_COMPLETED) ||
				action.equals(Intent.ACTION_USER_PRESENT)
				) {
			startAlarms(context);
		}
	}

	private void startAlarms(Context context) {
		boolean started = startedAlarms.getAndSet(true);
		if(!started) {
			
			AlarmManager mAlarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
			
			Intent p2 = new Intent(context, CheckEventsReceiver.class);
			PendingIntent pendingIntent = PendingIntent.getBroadcast(context, REQUEST_CODE, p2, PendingIntent.FLAG_UPDATE_CURRENT);
			
			mAlarmManager.cancel(pendingIntent);
			mAlarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + (5 * 1000), 30 * 1000, pendingIntent);
			Log.i("bcr", "Started alarms for events receiver");
		}
	}
	
}
