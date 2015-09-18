package com.zhaoshixin.show_as_your_wishes;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.ParcelUuid;
import android.util.Log;

public class BluetoothUtils {
    private static final String TAG = "BluetoothUtils";
    private static final boolean D = true;

    public static ArrayList<ParcelUuid> getDeviceUuids(BluetoothDevice device) {
        ArrayList<ParcelUuid> result = new ArrayList<ParcelUuid>();

        try {
            Class<?>[] cls =null;
            Method method = device.getClass().getMethod("getUuids", cls);
            Object[] objs = null;
            ParcelUuid[] phoneUuids = (ParcelUuid[]) method.invoke(device, objs);
            if (phoneUuids != null) {
                for (ParcelUuid uuid : phoneUuids) {
                    if (D) Log.d(TAG, device.getName() + ": " + uuid.toString());
                    result.add(uuid);
                }
            }
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            if (D) Log.e(TAG, "getDeviceUuids() failed", e);
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            if (D) Log.e(TAG, "getDeviceUuids() failed", e);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            if (D) Log.e(TAG, "getDeviceUuids() failed", e);
        }

        return result;
    }

    public static BluetoothSocket createRfcommSocket(BluetoothDevice device) {
        BluetoothSocket tmp = null;
        try {
            Class class1 = device.getClass();
            Class aclass[] = new Class[1];
            aclass[0] = Integer.TYPE;
            Method method = class1.getMethod("createRfcommSocket", aclass);
            Object aobj[] = new Object[1];
            aobj[0] = Integer.valueOf(1);

            tmp = (BluetoothSocket) method.invoke(device, aobj);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            if (D) Log.e(TAG, "createRfcommSocket() failed", e);
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            if (D) Log.e(TAG, "createRfcommSocket() failed", e);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            if (D) Log.e(TAG, "createRfcommSocket() failed", e);
        }
        return tmp;
    }
}
