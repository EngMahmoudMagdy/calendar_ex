package com.magdy.calendarex

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.os.AsyncTask
import android.os.Bundle
import android.provider.CalendarContract
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse
import com.google.api.client.googleapis.batch.json.JsonBatchCallback
import com.google.api.client.googleapis.json.GoogleJsonError
import com.google.api.client.http.HttpHeaders
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.DateTime
import com.google.api.services.calendar.CalendarScopes
import com.google.api.services.calendar.model.Event
import com.google.api.services.calendar.model.EventDateTime
import com.google.api.services.calendar.model.EventReminder
import com.microsoft.graph.concurrency.ICallback
import com.microsoft.graph.core.ClientException
import com.microsoft.graph.models.extensions.User
import com.microsoft.identity.client.AuthenticationCallback
import com.microsoft.identity.client.IAuthenticationResult
import com.microsoft.identity.client.exception.MsalClientException
import com.microsoft.identity.client.exception.MsalException
import com.microsoft.identity.client.exception.MsalServiceException
import com.microsoft.identity.client.exception.MsalUiRequiredException
import java.io.IOException
import java.util.*

class MainActivity : AppCompatActivity() {
    private val CODE_GOOGLE_SIGN_IN = 145
    //1040413787303-rgvs4vit06os0kiulef5mm4qc7alobdn.apps.googleusercontent.com
    private var alarmMgr: AlarmManager? = null
    private lateinit var alarmIntent: PendingIntent
    private var mService: com.google.api.services.calendar.Calendar? = null
    private val SCOPES = Collections.singletonList(CalendarScopes.CALENDAR)
    private var mAuthHelper: AuthenticationHelper? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        /* alarmMgr = getSystemService(Context.ALARM_SERVICE) as AlarmManager
         alarmIntent = Intent(baseContext, AlarmReceiver::class.java).let { intent ->
             PendingIntent.getBroadcast(baseContext, 0, intent, 0)
         }

         val time = Calendar.getInstance()
         time.timeInMillis = System.currentTimeMillis()
         time.add(Calendar.SECOND, 5)
         alarmMgr?.setRepeating(
             AlarmManager.RTC_WAKEUP, time.timeInMillis,10000,
             alarmIntent
         )*/
// Get the authentication helper
        mAuthHelper = AuthenticationHelper.getInstance(applicationContext)
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(CalendarScopes.CALENDAR))
            .requestId()
            .requestServerAuthCode(BuildConfig.CLIENT_ID)
            .requestIdToken(BuildConfig.CLIENT_ID)
            .build()
        doSilentSignIn()
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)
        /*
        val signInIntent = mGoogleSignInClient.signInIntent
        startActivityForResult(signInIntent, CODE_GOOGLE_SIGN_IN)*/
        //addEvent()

    }

    // Silently sign in - used if there is already a
// user account in the MSAL cache
    private fun doSilentSignIn() {
        mAuthHelper!!.acquireTokenSilently(getAuthCallback())
    }

    // Prompt the user to sign in
    private fun doInteractiveSignIn() {
        mAuthHelper!!.acquireTokenInteractively(this, getAuthCallback())
    }

    // Handles the authentication result
    public fun getAuthCallback(): AuthenticationCallback {
        return object : AuthenticationCallback {

            override fun onSuccess(authenticationResult: IAuthenticationResult) {
                // Log the token for debug purposes
                val accessToken = authenticationResult.accessToken
                Log.d("AUTH", String.format("Access token: %s", accessToken))
                var graphHelper = GraphHelper.getInstance()
                graphHelper.getUser(
                    accessToken,
                    getUserCallback()
                )

                graphHelper.addEvent(
                    "1 Zuhr salah", "it's salah now",
                    DateTime(Calendar.getInstance().timeInMillis + 1 * 60000 + 3600000),
                    DateTime(Calendar.getInstance().timeInMillis + 1 * 60000 + 7000000)
                )

            }

            override fun onError(exception: MsalException) {
                // Check the type of exception and handle appropriately
                if (exception is MsalUiRequiredException) {
                    Log.d("AUTH", "Interactive login required")
                    doInteractiveSignIn()

                } else if (exception is MsalClientException) {
                    if (exception.getErrorCode() === "no_current_account") {
                        Log.d("AUTH", "No current account, interactive login required")
                        doInteractiveSignIn()
                    } else {
                        // Exception inside MSAL, more info inside MsalError.java
                        Log.e("AUTH", "Client error authenticating", exception)
                    }
                } else if (exception is MsalServiceException) {
                    // Exception when communicating with the auth server, likely config issue
                    Log.e("AUTH", "Service error authenticating", exception)
                }
            }

            override fun onCancel() {
                // User canceled the authentication
                Log.d("AUTH", "Authentication canceled")
            }
        }
    }

    private fun getUserCallback(): ICallback<User> {
        return object : ICallback<User> {
            override fun success(user: User) {
                Log.d("AUTH", "User: " + user.displayName)
                /*   mUserName = user.displayName
                   mUserEmail = if (user.mail == null) user.userPrincipalName else user.mail

                   runOnUiThread {
                       hideProgressBar()

                       setSignedInState(true)
                       openHomeFragment(mUserName)
                   }
   */
            }

            override fun failure(ex: ClientException) {
                Log.e("AUTH", "Error getting /me", ex)
                runOnUiThread {
                    /*     hideProgressBar()

                         setSignedInState(true)
                         openHomeFragment(mUserName)*/
                }
            }
        }
    }

    lateinit var mGoogleSignInClient: GoogleSignInClient

    fun processSigninRequestResult(resultData: Intent?) {
        val task = GoogleSignIn.getSignedInAccountFromIntent(resultData)
        try {
            val account = task.getResult(ApiException::class.java)
            Log.i("example", "Authentication successful, writing auth code to model")
            insetNow(account!!.serverAuthCode!!)
        } catch (e: ApiException) {
            // Google Sign In failed, update UI appropriately
            Log.w("example", "Google sign in failed", e)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CODE_GOOGLE_SIGN_IN) {
            processSigninRequestResult(data)

        }
    }

    fun insetNow(authCode: String) {
        object : AsyncTask<Void, Void, GoogleTokenResponse>() {
            override fun doInBackground(vararg params: Void?): GoogleTokenResponse {
                val tokenResponse = GoogleAuthorizationCodeTokenRequest(
                    NetHttpTransport(),
                    JacksonFactory(),
                    "https://www.googleapis.com/oauth2/v4/token",
                    BuildConfig.CLIENT_ID,
                    BuildConfig.CLIENT_SECRET,
                    authCode,
                    ""
                )
                    .execute()
                return tokenResponse
            }

            override fun onPostExecute(result: GoogleTokenResponse?) {
                super.onPostExecute(result)
                startAddingEvents(result)

            }
        }.execute()


    }

    private fun startAddingEvents(result: GoogleTokenResponse?) {
        val mGoogleCredential = GoogleCredential.Builder()
            .setJsonFactory(JacksonFactory())
            .setTransport(NetHttpTransport())
            .setClientSecrets(
                BuildConfig.CLIENT_ID,
                BuildConfig.CLIENT_SECRET
            ).build().setFromTokenResponse(result)

        val transport = AndroidHttp.newCompatibleTransport()
        val jsonFactory = JacksonFactory.getDefaultInstance()
        mService = com.google.api.services.calendar.Calendar.Builder(
            transport, jsonFactory, mGoogleCredential
        )
            .setApplicationName(getString(R.string.app_name))
            .build()
        val batch = mService!!.batch()
        val callback: JsonBatchCallback<Event> = object : JsonBatchCallback<Event>() {

            override fun onSuccess(event: Event, responseHeaders: HttpHeaders) {

            }

            override fun onFailure(e: GoogleJsonError, responseHeaders: HttpHeaders) {
                println("Error Message: " + e.message)
            }
        }

        for (i in 0..2) {

            val event = insertEvent(
                "$i Zuhr salah", "it's salah now",
                DateTime(Calendar.getInstance().timeInMillis + i * 60000 + 60000),
                DateTime(Calendar.getInstance().timeInMillis + i * 60000 + 120000)
            )
            mService!!.events().insert("primary", event).setSendNotifications(true)
                .queue(batch, callback)
            /*
                batch.queue(
                    event,
                    Calendar::class.java,
                    GoogleJsonErrorContainer::class.java,
                    callback
                )*/
        }
        if (mService != null) {
            object : AsyncTask<Void, Void, Void>() {
                override fun doInBackground(vararg params: Void?): Void? {
                    batch.execute()
                    return null
                }

                override fun onPostExecute(result: Void?) {
                    super.onPostExecute(result)
                    Toast.makeText(baseContext, "Successsss ", Toast.LENGTH_LONG).show()

                }
            }.execute()
        }
    }

    @Throws(IOException::class)
    fun insertEvent(
        summary: String,
        des: String,
        startDate: DateTime,
        endDate: DateTime
    ): Event? {
        val event = Event()
            .setSummary(summary)
//            .setLocation(location)
            .setDescription(des)
        val start = EventDateTime()
            .setDateTime(startDate)
            .setTimeZone(TimeZone.getDefault().id)
        event.start = start
        val end = EventDateTime()
            .setDateTime(endDate)
            .setTimeZone(TimeZone.getDefault().id)
        event.end = end
        val recurrence = arrayOf("RRULE:FREQ=DAILY;COUNT=30")
        event.recurrence = recurrence.toMutableList()
        val reminderOverrides = arrayOf(
            EventReminder().setMethod("email").setMinutes(24 * 60),
            EventReminder().setMethod("popup").setMinutes(10)
        )
        val reminders = Event.Reminders()
            .setUseDefault(false)
            .setOverrides(reminderOverrides.toMutableList())
        event.reminders = reminders
        return event
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 4) {

            if (grantResults.isNotEmpty()
                && grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {
                addEvent()

            }
        }
    }

    val CALENDAR_QUERY_COLUMNS = arrayOf(
        CalendarContract.Calendars._ID,
        CalendarContract.Calendars.NAME,
        CalendarContract.Calendars.VISIBLE,
        CalendarContract.Calendars.OWNER_ACCOUNT
    )

    fun addEvent() {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CALENDAR)
            != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_CALENDAR
            )
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_CALENDAR, Manifest.permission.READ_CALENDAR),
                4
            )
        } else {

            val cr = contentResolver
            val cursor = cr.query(
                CalendarContract.Calendars.CONTENT_URI,
                CALENDAR_QUERY_COLUMNS, null, null, null
            )
            while (cursor!!.moveToNext()) {
                val _id = cursor.getString(0)
                val displayName = cursor.getString(1)
                val selected = cursor.getString(2) != "0"
                val accountName = cursor.getString(3)
                Log.d("example", "Found calendar $accountName")
//        	calendarList.append(
//        			"Calendar: Id: " + _id + " Display Name: " + displayName + " Selected: " + selected + " Name " + accountName)
            }

            val start2 = Calendar.getInstance().timeInMillis // 2011-02-12 12h00
            val end2 =
                Calendar.getInstance().timeInMillis + (4 * 60 * 60 * 1000)   // 2011-02-12 13h00

            val title = "This is my demo test with alaram with 5 minutes"

            val cvEvent = ContentValues()
            cvEvent.put(CalendarContract.Events.CALENDAR_ID, 1)
            cvEvent.put(CalendarContract.Events.TITLE, title)

            cvEvent.put(CalendarContract.Events.DESCRIPTION, (start2).toString())
//                cvEvent.put(CalendarContract.Events.EVENT_LOCATION, "Bhatar,Surat")
            cvEvent.put("hasAlarm", 1)
            cvEvent.put(CalendarContract.Events.DTSTART, start2)
            cvEvent.put(CalendarContract.Events.DTEND, end2)
            cvEvent.put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)


            val uri = contentResolver.insert(
                CalendarContract.Events.CONTENT_URI,
                cvEvent
            )

            val id = uri!!.lastPathSegment.toString()
            Toast.makeText(baseContext, "Created Calendar Event $id", Toast.LENGTH_SHORT)
                .show()

            // get the event ID that is the last element in the Uri
/*
                val eventID = java.lang.Long.parseLong(uri!!.lastPathSegment!!)


                val values = ContentValues()

                values.put(CalendarContract.Reminders.MINUTES, 2)
                values.put(CalendarContract.Reminders.EVENT_ID, eventID)
                values.put(
                    CalendarContract.Reminders.METHOD,
                    CalendarContract.Reminders.METHOD_ALARM
                )
                calEvent.put(CalendarContract.Events.CALENDAR_ID, 1) // XXX pick)
                calEvent.put(CalendarContract.Events.TITLE, title)
                calEvent.put(CalendarContract.Events.DTSTART, start.getTimeInMillis())
                calEvent.put(CalendarContract.Events.DTEND, end.getTimeInMillis())
                calEvent.put(CalendarContract.Events.EVENT_TIMEZONE, "Canada/Eastern")
                val uri2 = cr.insert(CalendarContract.Reminders.CONTENT_URI, values)*/
            //Uri uri = cr.insert(CalendarContract.Reminders.CONTENT_URI, values)
            Toast.makeText(applicationContext, "done", Toast.LENGTH_SHORT).show()

            try {


            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(
                    applicationContext,
                    e.printStackTrace().toString(),
                    Toast.LENGTH_SHORT
                ).show()
            }


        }

    }

    /*
    *
	final static String[] CALENDAR_QUERY_COLUMNS = {
			CalendarContract.Calendars._ID,
			CalendarContract.Calendars.NAME,
			CalendarContract.Calendars.VISIBLE,
			CalendarContract.Calendars.OWNER_ACCOUNT
	}

	public void addEvent(Context ctx, String title, Calendar start, Calendar end) {
		Log.d(TAG, "AddUsingContentProvider.addEvent()")


		// Get list of Calendars (after Jim Blackler, http://jimblackler.net/blog/?p=151)
		ContentResolver contentResolver = ctx.getContentResolver()
		Log.d(TAG, "URI = " + CalendarContract.Calendars.CONTENT_URI)
		if (checkSelfPermission(ctx,Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
			// TODO: Consider calling
			//    Activity#requestPermissions
			// here to request the missing permissions, and then overriding
			//   public void onRequestPermissionsResult(int requestCode, String[] permissions,
			//                                          int[] grantResults)
			// to handle the case where the user grants the permission. See the documentation
			// for Activity#requestPermissions for more details.
			return
		}
		final Cursor cursor = contentResolver.query(CalendarContract.Calendars.CONTENT_URI,
				CALENDAR_QUERY_COLUMNS, null, null, null)
        Log.d(TAG, "cursor = " + cursor)
        while (cursor.moveToNext()) {
        	final String _id = cursor.getString(0)
        	final String displayName = cursor.getString(1)
        	final Boolean selected = !cursor.getString(2).equals("0")
        	final String accountName = cursor.getString(3)
        	Log.d(TAG, "Found calendar " + accountName)
//        	calendarList.append(
//        			"Calendar: Id: " + _id + " Display Name: " + displayName + " Selected: " + selected + " Name " + accountName)
        }

        ContentValues calEvent = new ContentValues()
        calEvent.put(CalendarContract.Events.CALENDAR_ID, 1) // XXX pick)
        calEvent.put(CalendarContract.Events.TITLE, title)
        calEvent.put(CalendarContract.Events.DTSTART, start.getTimeInMillis())
        calEvent.put(CalendarContract.Events.DTEND, end.getTimeInMillis())
        calEvent.put(CalendarContract.Events.EVENT_TIMEZONE, "Canada/Eastern")
        Uri uri = contentResolver.insert(CalendarContract.Events.CONTENT_URI, calEvent)

        // The returned Uri contains the content-retriever URI for the newly-inserted event, including its id
        int id = Integer.parseInt(uri.getLastPathSegment())
        Toast.makeText(ctx, "Created Calendar Event " + id, Toast.LENGTH_SHORT).show()
    }
    * */
}
