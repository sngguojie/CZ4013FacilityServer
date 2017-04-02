package com.cz4013.server;

import java.util.HashMap;

/**
 * Created by melvynsng on 3/29/17.
 */
public class Confirmation {

    public static HashMap<Integer, Confirmation> confirmationHashMap = new HashMap<Integer, Confirmation>();

    public Booking booking;

    public Confirmation (Booking booking) {
        this.booking = booking;
    }

    /**
     * To store the confirmation of the booking with a unique ID
     * @return
     */
    public int save() {
        int i = 0;
        while (confirmationHashMap.containsKey(i)) {
            i++;
        }
        confirmationHashMap.put(i, this);
        return i;
    }
}
