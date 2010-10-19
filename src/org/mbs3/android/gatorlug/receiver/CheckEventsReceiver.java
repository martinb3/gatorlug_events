package org.mbs3.android.gatorlug.receiver;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.ComponentList;
import net.fortuna.ical4j.model.Date;
import net.fortuna.ical4j.model.component.VEvent;

import org.apache.http.*;
import org.apache.http.client.methods.*;
import org.apache.http.impl.client.*;
import org.mbs3.android.gatorlug.R;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.text.format.DateFormat;
import android.util.Log;

public class CheckEventsReceiver extends BroadcastReceiver {

	private int NOTFICATION_ID = 0;
	private final String NOTIFICATION_CLICKED = "org.mbs3.android.gatorlug.intent.NOTIFICATION_CLICKED";
	private final String URL_EVENTS = "http://www.google.com/calendar/ical/mbs3.org_qkoalps94abcijs3ooiba15f40%40group.calendar.google.com/public/basic.ics";
	
	private static final String PREFS_NAME = "org.mbs3.android.gatorlug.receiver.CheckEventsReceiver";
	private static final String UID_PREF_NAME = "UID_PREF_NAME";
	
	@Override 
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		Log.i("cer", "("+action+") " + intent.toString());
		
		if (action != null && action.equals(NOTIFICATION_CLICKED)) {
			Log.i("cer", "no internet, not going further");
			return;
		}
		
		if(!haveInternet(context)) {
			Log.i("cer", "no internet, not going further");
			return;
		}

	    SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
	      
		
		// get the highest feed UID we last saw (possibly 0),
	    // and track the highest we see this time (order is wrong sometimes)
	    final long lastSeenUid = settings.getLong(UID_PREF_NAME, 0);
	    long highestUidPref = lastSeenUid;
	    
		Calendar cal = getEvents("Meeting"); // filter by meetings
		ComponentList comps = cal.getComponents();
		for(int i = 0; i < comps.size(); i++ ) {
			VEvent ei = (VEvent)comps.get(i);

			if(ei.getLastModified() == null)
				continue;

			long thisUid = ei.getLastModified().getDateTime().getTime();
			
			if(thisUid > lastSeenUid) {
				// we haven't seen this one before
				Log.i("cer", "Haven't seen this UID before: " + thisUid);
				if(thisUid > highestUidPref) {
					highestUidPref = thisUid;
				}
				notifyNewEvent(context, ei);
			}
			
		}
		
		if(highestUidPref > lastSeenUid) {
			SharedPreferences.Editor editor = settings.edit();
			editor.putLong(UID_PREF_NAME, highestUidPref);
			editor.commit();
		}		
	}
	
	private void notifyNewEvent(Context context, VEvent event) {
		NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		Notification notification = new Notification(R.drawable.icon, "New or updated event", System.currentTimeMillis());
		notification.flags |= Notification.FLAG_AUTO_CANCEL;
		
		NOTFICATION_ID++;
		CharSequence contentTitle = "";
		if(event.getSummary() != null && event.getSummary().getValue() != null)
			contentTitle = event.getSummary().getValue();
		
		CharSequence contentText = "";
		if(event.getStartDate() != null && event.getStartDate().getDate() != null) {
			java.text.DateFormat dateFormatter = DateFormat.getDateFormat(context);
			java.text.DateFormat timeFormatter = DateFormat.getTimeFormat(context);
			
			Date date = event.getStartDate().getDate();
			contentText = "Starts on " + dateFormatter.format(date) + " @ " + timeFormatter.format(date);
		}
		else if(event.getDescription() != null && event.getDescription().getValue() != null) {
			contentText = event.getDescription().getValue();
		}
		

		// this won't do anything, since this activity/intent doesn't exist
		// unless someone wants to create their own activity and listen for it
		Intent notificationIntent = new Intent(NOTIFICATION_CLICKED);
		PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);

		//Log.i("cer",event);
		mNotificationManager.notify(NOTFICATION_ID, notification);
	}

	private boolean haveInternet(Context mContext){
		ConnectivityManager cm = (ConnectivityManager)mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
		     NetworkInfo info=cm.getActiveNetworkInfo();  
		     if(info==null || !info.isConnected()){  
		         return false;  
		     }  
		     if(info.isRoaming()){  
		         //here is the roaming option you can change it if you want to disable internet while roaming, just return false  
		         return true;  
		     }  
		     return true;  
		 }  


	private Calendar getEvents(String categoryFilter) {
		try {
			DefaultHttpClient httpclient = new DefaultHttpClient();

			HttpGet httpget = new HttpGet(URL_EVENTS);

			HttpResponse response = httpclient.execute(httpget);
			HttpEntity entity = response.getEntity();


			CalendarBuilder builder = new CalendarBuilder();
			Calendar calendar = builder.build(entity.getContent());
			
			// When HttpClient instance is no longer needed, 
			// shut down the connection manager to ensure
			// immediate deallocation of all system resources
			httpclient.getConnectionManager().shutdown();
			return calendar;
		} catch (Exception ex)
		{
			throw new RuntimeException(ex);
		}
	}

	
}
