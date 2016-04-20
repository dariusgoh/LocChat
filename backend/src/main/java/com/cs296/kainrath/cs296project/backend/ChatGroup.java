package com.cs296.kainrath.cs296project.backend;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kainrath on 4/19/16.
 */

// Latidude and Longitude represent the center of the Group
public class ChatGroup {
    private int chatId;
    private List<String> user_ids;
    private List<String> user_tokens;
    private double latitude;
    private double longitude;
    private String interest;
    private int groupSize;

    public ChatGroup(String interest, int chatId, int groupSize, double latitude, double longitude) {
        this.interest = interest;
        this.chatId = chatId;
        this.groupSize = groupSize;
        this.longitude = longitude;
        this.latitude = latitude;
    }

    /*
    public ChatGroup(String interest, String user_id, String user_token, double latitude, double longitude) {
        this.interest = interest;
        this.latitude = latitude;
        this.longitude = longitude;
        user_ids = new ArrayList<String>();
        user_tokens = new ArrayList<String>();
        user_ids.add(user_id);
        user_tokens.add(user_token);
    }

    public ChatGroup(String interest, List<String> user_ids, List<String> user_tokens, double latitude, double longitude) {
        this.interest = interest;
        this.user_tokens = user_tokens;
        this.user_ids = user_ids;
        this.latitude = latitude;
        this.longitude = longitude;
    }*/

    public int getChatId() {
        return chatId;
    }

    public int getGroupSize() {
        return groupSize;
    }

    public void moveMember(double oldLat, double oldLong, double newLat, double newLong) {
        this.latitude = (this.latitude * groupSize - oldLat + newLat) / groupSize;
        this.longitude = (this.longitude * groupSize - oldLong + newLong) / groupSize;

    }

    // USE FOR ADDING A CURRENT MEMBER, WILL NOT INCREASE GROUPSIZE
    public void putCurrMember(String user_id, String user_token) {
        this.user_ids.add(user_id);
        this.user_tokens.add(user_token);
    }

    // USE FOR ADDING A NEW MEMBER, WILL INCREMENT GROUPSIZE
    public void addUserToGroup(String user_id, String user_token, double latitude, double longitude) {
        this.latitude = (this.latitude * groupSize + latitude) / (groupSize + 1);
        this.longitude = (this.longitude * groupSize + longitude) / (groupSize + 1);
        ++groupSize;
        user_ids.add(user_id);
        user_tokens.add(user_token);
    }

    public void removeUserFromGroup(String user_id, String user_token, double latitude, double longitude) {
        if (groupSize > 1) {
            this.latitude = (this.latitude * groupSize - latitude) / (groupSize - 1);
            this.longitude = (this.longitude * groupSize - longitude) / (groupSize - 1);
            --groupSize;
            user_ids.remove(user_id);
            user_tokens.remove(user_token);
        } else {
            this.latitude = 0;
            this.longitude = 0;
            groupSize = 0;
            user_ids.clear();
            user_tokens.clear();
        }
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public List<String> getUserIds() {
        return user_ids;
    }

    public List<String> getUserTokens() {
        return user_tokens;
    }
}
