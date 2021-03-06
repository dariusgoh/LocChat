package com.cs296.kainrath.cs296project;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.LocationManager;
import android.provider.Settings;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.cs296.kainrath.cs296project.backend.locationApi.model.ChatGroup;
import com.cs296.kainrath.cs296project.backend.userApi.model.User;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final String USER_ID = "USER_ID";

    private static ChatGroupAdaptor chatAdaptor = null;
    private List<ChatGroup> chatGroups;

    private User user;
    private Button activate;
    private Button disp_ints;
    private Button deactivate;
    private ListView chat_list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            ((GlobalVars) this.getApplication()).restoreState(savedInstanceState);
        }

        if (GlobalVars.getFailed()) {
            System.exit(1);
        }
        user = GlobalVars.getUser();
        if (user == null) {
            startActivity(new Intent(this, CreateUser.class));
        }
        setContentView(R.layout.activity_main);

        activate = (Button) findViewById(R.id.button_activate);
        deactivate = (Button) findViewById(R.id.button_deactivate);
        disp_ints = (Button) findViewById(R.id.button_display_interests);
        chat_list = (ListView) findViewById(R.id.chat_list);

        boolean has_interests = GlobalVars.hasInterests();

        if (LocationTrackerService.isInstanceCreated()) {
            activate.setEnabled(false);
            disp_ints.setEnabled(false); // Don't allow user to modify interests while activated
            //dislayChatGroups();
            //registerOnClick();
            // Make chat list visible
        } else {
            deactivate.setEnabled(false);
            // Make chat list invisible
        }
        if (!has_interests) { // Don't allow user to "activate" if they don't have interests
            activate.setEnabled(false);
        }
    }

    // For updating list if user joined/left a group
    private BroadcastReceiver notificationReceiver = new BroadcastReceiver() {
        private String TAG = "BroadCastRec";
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Recieved broadcast");
            // Updating UI must be done on Main thread
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (chatAdaptor != null) {
                        Log.d(TAG, "Running on UI thread");
                        chatAdaptor.notifyDataSetChanged();

                    }
                }
            });
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        // Activate broadcast receiver
        this.registerReceiver(notificationReceiver, new IntentFilter("ChatUpdate"));
        if (chatAdaptor != null) { // Possibility that chatGroups were updated while in a different activity
            chatAdaptor.notifyDataSetChanged();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Pause broadcast receiver
        this.unregisterReceiver(notificationReceiver);
    }

    // For accessing Logout button
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_actionbar_layout, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        ((GlobalVars) this.getApplication()).saveState(savedInstanceState);
    }

    public void onClickMyInterests(View view) {
        startActivity(new Intent(this, DisplayInterests.class));
    }

    public void onClickActivate(View view) {
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        if (PackageManager.PERMISSION_GRANTED == permissionCheck) {

            // Make sure gps is enabled
            Criteria trackerCriteria = new Criteria();
            trackerCriteria.setAccuracy(Criteria.ACCURACY_FINE);
            trackerCriteria.setAltitudeRequired(false);
            trackerCriteria.setBearingRequired(false);
            trackerCriteria.setCostAllowed(true);
            trackerCriteria.setPowerRequirement(Criteria.NO_REQUIREMENT);
            LocationManager locManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            String provider = locManager.getBestProvider(trackerCriteria, true);
            if (provider != null) {
                Log.d(TAG, "provider: " + provider);
                chat_list.setEnabled(true);
                chat_list.setVisibility(View.VISIBLE);
                registerOnClick();
                disp_ints.setEnabled(false);
                activate.setEnabled(false);
                deactivate.setEnabled(true);
                Intent intent = new Intent(this, LocationTrackerService.class);
                intent.putExtra("PROVIDER", provider);
                intent.putExtra(USER_ID, user.getId());
                startService(intent);
                dislayChatGroups();
            } else {
                Toast.makeText(getBaseContext(), "Please enable GPS for this app",
                        Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }

        } else {
            Toast.makeText(this, "Please enable location services for this app", Toast.LENGTH_LONG).show();
        }
    }

    public void onClickDeactivate(View view) {

        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        if (PackageManager.PERMISSION_GRANTED == permissionCheck) {
            stopService(new Intent(this, LocationTrackerService.class));
            deactivate.setEnabled(false);
            activate.setEnabled(true);
            disp_ints.setEnabled(true);
            chat_list.setVisibility(View.INVISIBLE);
            chat_list.setEnabled(false);
        }
        else {
            Toast.makeText(this, "Please enable location services for this app", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menu) {
        Log.d(TAG, "clicked options menu");
        switch (menu.getItemId()) {
            case R.id.logout_button:
                Log.d(TAG, "clicked logout");
                logout();
                return true;
            default:
                Log.d(TAG, "clicked default");
                return super.onOptionsItemSelected(menu);
        }
    }

    private void logout() {
        if (LocationTrackerService.isInstanceCreated()) {
            Toast.makeText(this, "Please Deactivate before logging out", Toast.LENGTH_SHORT).show();
            return;
        }
        Log.d(TAG, "starting logout");
        GlobalVars.setUser(null);
        GlobalVars.emptyChatGroup();
        GlobalVars.setLatLong(0,0);
        if (LocationTrackerService.isInstanceCreated()) {
            Log.d(TAG, "stopping location service");
            stopService(new Intent(this, LocationTrackerService.class));
        } else {
            Log.d(TAG, "location service was not running");
        }
        startActivity(new Intent(this, CreateUser.class));
    }

    private void dislayChatGroups() {
        chatGroups = GlobalVars.chatGroups;
        //chatGroups = GlobalVars.getChatGroups();
        chatAdaptor = new ChatGroupAdaptor(this, R.layout.group_item, chatGroups);
        chat_list.setAdapter(chatAdaptor);
    }

    private void registerOnClick() {
        chat_list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view_clicked, int position, long id) {
                ChatGroup group = chatGroups.get(position);
                Intent intent = new Intent(MainActivity.this, ChatGroupActivity.class);
                intent.putExtra("ChatId", group.getChatId());
                startActivity(intent);
            }
        });
    }
}

class ChatGroupAdaptor extends ArrayAdapter<ChatGroup> {
    private static String TAG = "ChatGroupAdaptor";
    List<ChatGroup> chatGroups;

    private static LayoutInflater inflater;

    public ChatGroupAdaptor(Context context, int textResId, List<ChatGroup> chatGroups) {
        super(context, textResId, chatGroups);
        this.chatGroups = chatGroups;
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        Log.d(TAG, "Getting count, " + chatGroups.size());
        return chatGroups.size();
    }

    @Override
    public ChatGroup getItem(int position) {
        Log.d(TAG, "Getting chatGroup at position " + position);
        return chatGroups.get(position);
    }

    @Override
    public long getItemId(int position) {
        Log.d(TAG, "Getting item id at position " + position);
        return position;
    }

    @Override
    public View getView(int position, View rowView, ViewGroup parent) {
        Log.d(TAG, "Updating list view position " + position);
        if (rowView == null) {
            rowView = inflater.inflate(R.layout.group_item, null);
        }

        TextView groupId =(TextView) rowView.findViewById(R.id.group_name);
        groupId.setText(chatGroups.get(position).getInterest());
        TextView groupSize = (TextView) rowView.findViewById(R.id.group_size);
        String groupSizeText = chatGroups.get(position).getGroupSize() + " User(s)";
        groupSize.setText(groupSizeText);
        return rowView;
    }

}



