package com.zhaoshixin.show_as_your_wishes;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.InputFilter;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.TextView;
import android.util.Log;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import com.fortysevendeg.swipelistview.BaseSwipeListViewListener;
import com.fortysevendeg.swipelistview.SwipeListView;


public final class DeviceControlActivity extends BaseActivity {
    private static final String DEVICE_NAME = "DEVICE_NAME";
    private static final String LOG = "LOG";

    private static final SimpleDateFormat timeformat = new SimpleDateFormat("HH:mm:ss.SSS");

    private static String MSG_NOT_CONNECTED;
    private static String MSG_CONNECTING;
    private static String MSG_CONNECTED;

    private static DeviceConnector connector;
    private static BluetoothResponseHandler mHandler;

    private TextView logTextView;
    private EditText commandEditText;

    private String deviceName;

    private SwipeListView swipeListView;
    private SavedMessageAdapter adapter;
    private List<String> data;

    private MySQLiteHelper myDBHelper;
    private String mConnectDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mConnectDevice = "";
        super.onCreate(savedInstanceState);
        PreferenceManager.setDefaultValues(this, R.layout.settings_activity, false);

        if (mHandler == null) mHandler = new BluetoothResponseHandler(this);
        else mHandler.setTarget(this);

        MSG_NOT_CONNECTED = getString(R.string.msg_not_connected);
        MSG_CONNECTING = getString(R.string.msg_connecting);
        MSG_CONNECTED = getString(R.string.msg_connected);

        setContentView(R.layout.main_activity);
        if (isConnected() && (savedInstanceState != null)) {
            setDeviceName(savedInstanceState.getString(DEVICE_NAME));
        } else getSupportActionBar().setSubtitle(MSG_NOT_CONNECTED);


        data = new ArrayList<String>();
        myDBHelper = new MySQLiteHelper(this, "my.db", null, 1);

        adapter = new SavedMessageAdapter(this, data, swipeListView, myDBHelper);
        swipeListView = (SwipeListView) findViewById(R.id.saved_message);
        //swipeListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);CHOICE_MODE_SINGLE
        swipeListView.setSwipeListViewListener(new BaseSwipeListViewListener() {
            @Override
            public void onOpened(int position, boolean toRight) {
            }

            @Override
            public void onClosed(int position, boolean fromRight) {
            }

            @Override
            public void onListChanged() {
            }

            @Override
            public void onMove(int position, float x) {
            }

            @Override
            public void onStartOpen(int position, int action, boolean right) {
                Log.d("swipe", String.format("onStartOpen %d - action %d", position, action));
            }

            @Override
            public void onStartClose(int position, boolean right) {
                Log.d("swipe", String.format("onStartClose %d", position));
            }

            @Override
            public void onClickFrontView(int position) {
                String str = adapter.getItem(position);
                realSendCommand(str);
                Log.d("swipe", String.format("onClickFrontView %d", position));
            }

            @Override
            public void onClickBackView(int position) {
                Log.d("swipe", String.format("onClickBackView %d", position));
            }

            @Override
            public void onDismiss(int[] reverseSortedPositions) {
                for (int position : reverseSortedPositions) {
                    data.remove(position);
                }
                adapter.notifyDataSetChanged();
            }
        });
        swipeListView.setAdapter(adapter);

        String str = "";
        //获得数据库对象
        SQLiteDatabase db = myDBHelper.getReadableDatabase();
        //查询表中的数据
        Cursor cursor = db.query("normal_save_message", null, null, null, null, null, "id asc");
        //获取message列的索引
        int messageIndex = cursor.getColumnIndex("normalMessage");
        for (cursor.moveToFirst();!(cursor.isAfterLast());cursor.moveToNext()) {
            str = cursor.getString(messageIndex);
            data.add(str);
        }
        cursor.close();//关闭结果集
        db.close();//关闭数据库对象
        adapter.notifyDataSetChanged();

        this.commandEditText = (EditText) findViewById(R.id.command_edittext);

        //尝试自动连接蓝牙设备
        SQLiteDatabase db1 = myDBHelper.getReadableDatabase();
        //查询表中的数据
        Cursor cursor1 = db1.query("connectDevice", null, null, null, null, null, "id asc");
        if (cursor1.getCount() != 0){
            int index = cursor1.getColumnIndex("address");
            cursor1.moveToFirst();
            String address = cursor1.getString(index);
            BluetoothDevice device = btAdapter.getRemoteDevice(address);
            if (super.isAdapterReady() && (connector == null))
                setupConnector(device);
        }
        //获取message列的索引

        cursor1.close();//关闭结果集
        db1.close();//关闭数据库对象


        // soft-keyboard send button
        this.commandEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    sendCommand(null);
                    return true;
                }
                return false;
            }
        });
        // hardware Enter button
        this.commandEditText.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    switch (keyCode) {
                        case KeyEvent.KEYCODE_ENTER:
                            sendCommand(null);
                            return true;
                        default:
                            break;
                    }
                }
                return false;
            }
        });
    }
    // ==========================================================================

    @Override
    public void onResume() {
        super.onResume();
        if (mConnectDevice != ""){
            if (super.isAdapterReady() && (connector == null)){
                BluetoothDevice device = btAdapter.getRemoteDevice(mConnectDevice);
                setupConnector(device);
            }
        }
    }

    // ==========================================================================

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(DEVICE_NAME, deviceName);
        if (logTextView != null) {
            final String log = logTextView.getText().toString();
            outState.putString(LOG, log);
        }
    }
    // ============================================================================

    private boolean isConnected() {
        return (connector != null) && (connector.getState() == DeviceConnector.STATE_CONNECTED);
    }
    // ==========================================================================

    private void stopConnection() {
        if (connector != null) {
            connector.stop();
            connector = null;
            deviceName = null;
        }
    }
    // ==========================================================================

    private final AdapterView.OnItemClickListener mSavedMessageClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
            // Get the device MAC address, which is the last 17 chars in the View
            CharSequence info = ((TextView) v).getText();
            if (info != null) {
                realSendCommand(info.toString());
            }
        }
    };

    private void startDeviceListActivity() {
        stopConnection();
        Intent serverIntent = new Intent(this, DeviceListActivity.class);
        startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
    }
    // ============================================================================


    @Override
    public boolean onSearchRequested() {
        if (super.isAdapterReady()) startDeviceListActivity();
        return false;
    }
    // ==========================================================================


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.layout.menu, menu);
        return true;
    }
    // ============================================================================


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.menu_search:
                if (super.isAdapterReady()) {
                    if (isConnected()) stopConnection();
                    else startDeviceListActivity();
                } else {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                }
                return true;

            case R.id.menu_settings:
                final Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }
    // ============================================================================


    @Override
    public void onStart() {
        super.onStart();

        commandEditText.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        commandEditText.setFilters(new InputFilter[]{});
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    String address = data.getStringExtra(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    BluetoothDevice device = btAdapter.getRemoteDevice(address);
                    if (super.isAdapterReady() && (connector == null)) setupConnector(device);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                super.pendingRequestEnableBt = false;
                if (resultCode != Activity.RESULT_OK) {
                    Log.i("随心显", "BT not enabled");
                }
                break;
        }
    }
    // ==========================================================================

    private void setupConnector(BluetoothDevice connectedDevice) {
        stopConnection();
        try {
            String emptyName = getString(R.string.empty_device_name);
            DeviceData data = new DeviceData(connectedDevice, emptyName);
            connector = new DeviceConnector(data, mHandler);
            mConnectDevice = connectedDevice.toString();
            connector.connect();
        } catch (IllegalArgumentException e) {
            Log.i("随心显", "setupConnector failed: " + e.getMessage());
        }
    }
    // ==========================================================================

    public void sendCommand(View view) {
        if (commandEditText != null) {
            String commandString = commandEditText.getText().toString();
            realSendCommand(commandString);
            commandEditText.selectAll();
        }
    }

    public void saveCommand(View view) {
        if (commandEditText != null) {
            String commandString = commandEditText.getText().toString();

            if (commandString.length() == 0)
                return;
            if (data.contains(commandString))
                return;
            swipeListView.setEnabled(true);
            adapter.add(commandString);
            adapter.notifyDataSetChanged();
            commandEditText.selectAll();
            //获取数据库对象
            SQLiteDatabase db = myDBHelper.getWritableDatabase();
            //使用execSQL方法向表中插入数据
            String sql = "insert into normal_save_message(normalMessage) values('"+ commandString + "')";
            db.execSQL(sql);
            //关闭SQLiteDatabase对象
            db.close();
        }
    }
    // ==========================================================================

    private void realSendCommand(String commandString) {
            byte[] command = null;
            try {
                command = commandString.getBytes("gb2312");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            int length=command.length;
            int total = 0;
            byte i;

            byte[] realCommand=new byte[length+7];
            realCommand[0] = 0x1;  //帧同步
            realCommand[1] = 0x0;  //485 addr
            realCommand[2] = (byte)(length+5); //length
            realCommand[3] = 0x0; //clear everytime
            realCommand[4] = 0x2; //color  1 red, 2 green, 3 orange
            for (i = 0; i < length; i++)
                realCommand[5+i] = command[i];
            for (i = 0; i < realCommand[2]; i++)
                total+=(int)(realCommand[i] & 0xFF);

            realCommand[length+5] = (byte)(total>>8);
            realCommand[length+6] = (byte)(total & 0x00FF);

            if (isConnected()) {
                connector.write(realCommand);
            }
    }

    // =========================================================================

    void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
        getSupportActionBar().setSubtitle(deviceName);
    }
    // ==========================================================================

    private static class BluetoothResponseHandler extends Handler {
        private WeakReference<DeviceControlActivity> mActivity;

        public BluetoothResponseHandler(DeviceControlActivity activity) {
            mActivity = new WeakReference<DeviceControlActivity>(activity);
        }

        public void setTarget(DeviceControlActivity target) {
            mActivity.clear();
            mActivity = new WeakReference<DeviceControlActivity>(target);
        }

        @Override
        public void handleMessage(Message msg) {
            DeviceControlActivity activity = mActivity.get();
            if (activity != null) {
                switch (msg.what) {
                    case MESSAGE_STATE_CHANGE:
                        Log.i("随心显", "MESSAGE_STATE_CHANGE: " + msg.arg1);
                        final ActionBar bar = activity.getSupportActionBar();
                        switch (msg.arg1) {
                            case DeviceConnector.STATE_CONNECTED:
                                //将本次连接的设备号存入数据库
                                //获取数据库对象
                                SQLiteDatabase db = activity.myDBHelper.getWritableDatabase();
                                //查询表中的数据
                                Cursor cursor = db.query("connectDevice", null, null, null, null, null, "id asc");
                                String sql = null;
                                if (cursor.getCount() != 0){
                                    //使用execSQL方法，删除原来保存的设备号
                                    sql = "delete from connectDevice where 1=1";
                                    db.execSQL(sql);
                                }
                                //使用execSQL方法向表中插入数据
                                sql = "insert into connectDevice(address) values('"+ activity.mConnectDevice + "')";
                                db.execSQL(sql);
                                //关闭SQLiteDatabase对象
                                cursor.close();//关闭结果集
                                db.close();

                                bar.setSubtitle(MSG_CONNECTED);
                                break;
                            case DeviceConnector.STATE_CONNECTING:
                                bar.setSubtitle(MSG_CONNECTING);
                                break;
                            case DeviceConnector.STATE_NONE:
                                bar.setSubtitle(MSG_NOT_CONNECTED);
                                break;
                        }
                        break;

                    case MESSAGE_READ:
                        break;

                    case MESSAGE_DEVICE_NAME:
                        activity.setDeviceName((String) msg.obj);
                        break;

                    case MESSAGE_WRITE:
                        // stub
                        break;

                    case MESSAGE_TOAST:
                        // stub
                        break;
                }
            }
        }
    }
    // ==========================================================================
}