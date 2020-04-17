package com.magdy.calendarex;

import android.util.Log;

import com.google.api.client.util.DateTime;
import com.microsoft.graph.authentication.IAuthenticationProvider;
import com.microsoft.graph.concurrency.ICallback;
import com.microsoft.graph.core.ClientException;
import com.microsoft.graph.http.IHttpRequest;
import com.microsoft.graph.models.extensions.Attendee;
import com.microsoft.graph.models.extensions.DateOnly;
import com.microsoft.graph.models.extensions.DateTimeTimeZone;
import com.microsoft.graph.models.extensions.EmailAddress;
import com.microsoft.graph.models.extensions.Event;
import com.microsoft.graph.models.extensions.IGraphServiceClient;
import com.microsoft.graph.models.extensions.ItemBody;
import com.microsoft.graph.models.extensions.PatternedRecurrence;
import com.microsoft.graph.models.extensions.RecurrencePattern;
import com.microsoft.graph.models.extensions.RecurrenceRange;
import com.microsoft.graph.models.extensions.User;
import com.microsoft.graph.models.generated.AttendeeType;
import com.microsoft.graph.models.generated.BodyType;
import com.microsoft.graph.models.generated.RecurrencePatternType;
import com.microsoft.graph.models.generated.RecurrenceRangeType;
import com.microsoft.graph.requests.extensions.GraphServiceClient;

import java.util.LinkedList;
import java.util.TimeZone;

// Singleton class - the app only needs a single instance
// of the Graph client
public class GraphHelper implements IAuthenticationProvider {
    private static GraphHelper INSTANCE = null;
    private IGraphServiceClient mClient = null;
    private String mAccessToken = null;

    private GraphHelper() {
        mClient = GraphServiceClient.builder()
                .authenticationProvider(this).buildClient();
    }

    public static synchronized GraphHelper getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new GraphHelper();
        }

        return INSTANCE;
    }

    // Part of the Graph IAuthenticationProvider interface
    // This method is called before sending the HTTP request
    @Override
    public void authenticateRequest(IHttpRequest request) {
        // Add the access token in the Authorization header
        request.addHeader("Authorization", "Bearer " + mAccessToken);
    }

    public void getUser(String accessToken, ICallback<User> callback) {
        mAccessToken = accessToken;

        // GET /me (logged in user)
        mClient.me().buildRequest().get(callback);
    }

    public void addEvent(String summary,
                         String des,
                         DateTime startDate,
                         DateTime endDate) {


      /*  LinkedList<Option> requestOptions = new LinkedList<Option>();
        requestOptions.add(new HeaderOption("Prefer", "outlook.timezone=\"Pacific Standard Time\""));
*/
        Event event = new Event();
        event.subject = summary;
        ItemBody body = new ItemBody();
        body.contentType = BodyType.HTML;
        body.content =  des;
        event.body = body;
        DateTimeTimeZone start = new DateTimeTimeZone();
        start.dateTime = startDate.toString();
        start.timeZone = TimeZone.getDefault().getID();
        event.start = start;
        DateTimeTimeZone end = new DateTimeTimeZone();
        end.dateTime = endDate.toString();
        end.timeZone = TimeZone.getDefault().getID();
        event.end = end;
        PatternedRecurrence patternedRecurrence = new PatternedRecurrence();
        patternedRecurrence.pattern = new RecurrencePattern();
        patternedRecurrence.pattern.interval=1;
        patternedRecurrence.pattern.type = RecurrencePatternType.DAILY;
        patternedRecurrence.range = new RecurrenceRange();
        patternedRecurrence.range.type = RecurrenceRangeType.NUMBERED;
        patternedRecurrence.range.startDate = new DateOnly(2020,4,16);
        patternedRecurrence.range.numberOfOccurrences = 3;
        event.recurrence = patternedRecurrence;

        mClient.me().events().buildRequest().post(event, new ICallback<Event>() {
            @Override
            public void success(Event event) {
                Log.e("Calendar", "doneeee");
            }

            @Override
            public void failure(ClientException ex) {
                Log.e("Calendar", "errorrr");
            }
        });
    }
}