package adam.learn.testvideo;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.MediaRouteActionProvider;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.cast.ApplicationMetadata;
import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

public class MainActivity extends ActionBarActivity {

    private String RECEIVER_APP_ID = "B4CE97D6";
    private boolean mWaitingForReconnect = true;

    private MediaRouter mMediaRouter;
    private MediaRouteSelector mMediaRouteSelector;
    private CastDevice mSelectedDevice;
    private MyMediaRouterCallback mMediaRouterCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }
        mMediaRouter = MediaRouter.getInstance(getApplicationContext());
        mMediaRouteSelector = new MediaRouteSelector.Builder()
                .addControlCategory(CastMediaControlIntent.categoryForCast(RECEIVER_APP_ID))
                .build();

        mMediaRouterCallback = new MyMediaRouterCallback();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);

        MenuItem mediaRouteMenuItem = menu.findItem(R.id.media_route_menu_item);
        MediaRouteActionProvider mediaRouteActionProvider =
                (MediaRouteActionProvider) MenuItemCompat.getActionProvider(mediaRouteMenuItem);
        mediaRouteActionProvider.setRouteSelector(mMediaRouteSelector);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMediaRouter.addCallback(mMediaRouteSelector, mMediaRouterCallback, MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN);
    }

    @Override
    protected void onPause() {
        if(isFinishing()) {
            mMediaRouter.removeCallback(mMediaRouterCallback);
        }
        super.onPause();
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            return rootView;
        }
    }

    private class MyMediaRouterCallback extends MediaRouter.Callback {

        private GoogleApiClient mApiClient;
        private CastListener mCastClientListener;
        private ConnectionCallbacks mConnectionCallbacks;
        private ConnectionFailedListener mConnectionFailedListener;

        @Override
        public void onRouteSelected(MediaRouter router, MediaRouter.RouteInfo info) {
            mSelectedDevice = CastDevice.getFromBundle(info.getExtras());
            String routeId = info.getId();

            mCastClientListener = new CastListener();

            Cast.CastOptions.Builder apiOptionsBuilder = Cast.CastOptions.builder(mSelectedDevice, mCastClientListener);

            mApiClient = new GoogleApiClient.Builder(getBaseContext())
                    .addApi(Cast.API, apiOptionsBuilder.build())
                    .addConnectionCallbacks(mConnectionCallbacks)
                    .addOnConnectionFailedListener(mConnectionFailedListener)
                    .build();

            mApiClient.connect();

        }

        @Override
        public void onRouteUnselected(MediaRouter router, MediaRouter.RouteInfo info) {
            //teardown();
            mCastClientListener = null;
            mSelectedDevice = null;
        }

        private class CastListener extends Cast.Listener {
            @Override
            public void onApplicationStatusChanged() {
                if (mApiClient != null) {
                    Log.d("", "onApplicationStatusChanged: " + Cast.CastApi.getApplicationStatus(mApiClient));
                }
            }

            @Override
            public void onVolumeChanged() {
                if (mApiClient != null) {
                    Log.d("", "onVolumeChanged: " + Cast.CastApi.getVolume(mApiClient));
                }
            }

            @Override
            public void onApplicationDisconnected(int errorCode) {
                //teardown();
            }
        }

        private class ConnectionCallbacks implements GoogleApiClient.ConnectionCallbacks {

            @Override
            public void onConnected(Bundle connectionHint) {
                if(mWaitingForReconnect) {
                    mWaitingForReconnect = false;
                    //reconnectChannels();
                } else {
                    try {
                        Cast.CastApi.launchApplication(mApiClient, RECEIVER_APP_ID, false)
                                .setResultCallback(
                                    new ResultCallback<Cast.ApplicationConnectionResult>() {
                                      @Override
                                    public void onResult(Cast.ApplicationConnectionResult result) {
                                          Status status = result.getStatus();
                                          if(status.isSuccess()) {
                                              ApplicationMetadata applicationMetadata = result.getApplicationMetadata();
                                              String sessionId = result.getSessionId();
                                              String applicationStatus = result.getApplicationStatus();
                                              boolean wasLaunched = result.getWasLaunched();
                                              // ...
                                          } else {
                                              //teardown();
                                          }
                                      }
                                    });
                    } catch (Exception e) {
                        Log.e("", "Failed to launch application");
                    }
                }
            }

            @Override
            public void onConnectionSuspended(int cause) {
                mWaitingForReconnect = true;
            }
        }

        private class ConnectionFailedListener implements GoogleApiClient.OnConnectionFailedListener {
            @Override
            public void onConnectionFailed(ConnectionResult result) {
                //teardown();
            }
        }
    }

}
