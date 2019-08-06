package com.yuanye.bootregister;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.yuanye.bootregister.manager.TcpSocketManager;
import com.yuanye.bootregister.manager.WifiHelper;
import com.yuanye.bootregister.util.Utils;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener, CompoundButton.OnCheckedChangeListener{

    public static final int HANDLER_SOCKET_ERROR = 0;
    public static final int HANDLER_RECEIVE = 1;

    private static final String SaveSuccess = "SAVESUCCESS";
    private static final String SaveNull = "SAVENULL";
    private static final String Repetition = "REPETITION";

    private TextView txtInfo;
    private ListView listViewWifi;
    private Switch swtWifi;
    private Button btnRegister;
    private RelativeLayout relativeLayoutScanning;
    private Context mContext;
    private List<ScanResult> mWifiList;
    private List<String> mWifiStates;
    private WifiHelper wifiHelper;
    private TcpSocketManager tcpSocketManager;
    private BroadcastReceiver receiver;
    private WifiAdapter mWifiAdapter;
    private Dialog dialog;
    private Handler mHandler = new MyHandler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("网络注册");
        setContentView(R.layout.activity_main);
        mContext = this;
        txtInfo = findViewById(R.id.txt_info);
        listViewWifi = findViewById(R.id.lsv_wifi);
        swtWifi = findViewById(R.id.swt_wifi);
        btnRegister = findViewById(R.id.btnRegister);
        relativeLayoutScanning = findViewById(R.id.scanning);
        init();
    }

    private void init() {
        dialog = new Dialog(mContext);
        mWifiList = new ArrayList<>();
        mWifiStates = new ArrayList<>();
        mWifiAdapter = new WifiAdapter(mContext, mWifiList, mWifiStates);
        listViewWifi.setAdapter(mWifiAdapter);
        listViewWifi.setOnItemClickListener(this);
        listViewWifi.setOnItemLongClickListener(this);
        wifiHelper = new WifiHelper(this);
        tcpSocketManager = new TcpSocketManager(this, mHandler);
        swtWifi.setChecked(wifiHelper.isWifiEnabled());
        swtWifi.setOnCheckedChangeListener(this);
        receiver = new WifiBroadcastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        filter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        registerReceiver(receiver, filter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        openWifi();
    }

    private void openWifi(){
        wifiHelper.openWifi();
    }

    private void startScan() {
        wifiHelper.startScan();
        relativeLayoutScanning.setVisibility(View.VISIBLE);
    }

    private void updateScanResult(){
        mWifiList = wifiHelper.getScanResults();
        mWifiStates = wifiHelper.stateChecking(mWifiList);
        mWifiAdapter.update(mWifiList);
        mWifiAdapter.updateState(mWifiStates);
        relativeLayoutScanning.setVisibility(View.GONE);
    }

    private void clearScanresults(){
        mWifiList.clear();
        mWifiStates.clear();
        mWifiAdapter.update(mWifiList);
        mWifiAdapter.updateState(mWifiStates);
    }


    @Override
    protected void onDestroy() {
        closeWifi();
        super.onDestroy();
        if (receiver != null)
            unregisterReceiver(receiver);
    }

    private void closeWifi() {
        if (wifiHelper.isWifiEnabled())
            wifiHelper.closeWifi();
    }

    public void register(View v){
        if (wifiHelper.isNetworkAvailable()){
            tcpSocketManager.register(getSerial());
        }else{
            showToast("没有网络，请检查WIFI状况");
        }
    }

    private String getSerial(){
        String cpuSerial = Utils.getCPUSerial();
        return "GW3326:"+cpuSerial+":0";
    }

    private void registerOK(){
        // remove this activity from the package manager.
        PackageManager pm = getPackageManager();
        ComponentName name = new ComponentName(this, MainActivity.class);
        pm.setComponentEnabledSetting(name, PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);

        // terminate the activity.
        finish();
    }

    private void dealData(String data){
        if(data.contains(SaveSuccess)){
            showToast(SaveSuccess);
			registerOK();
        }else if(data.contains(SaveNull)){
            showToast(SaveNull);
        }else if(data.contains(Repetition)){
            showToast(Repetition);
			registerOK();
        }else{
            showToast("receive error data!");
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        WifiInfo wifiInfo = wifiHelper.getCurrentWifi(); // wifiInfo的ssid前后带引号
        ScanResult scanResult = mWifiList.get(i);// scanResult的ssid不带引号
        if (wifiInfo.getSSID().contains(scanResult.SSID)){
            showDialogWifiInfo(i);
        }else{
            if(wifiHelper.isHistory(scanResult)){
                wifiHelper.connectSavedWifi(scanResult.SSID);
            }else if(wifiHelper.getWifiEncryptTypeStr(scanResult.capabilities).equals("NONE")){
                wifiHelper.connectWifi(scanResult, "");
            }else{
                showDialogConnect(scanResult);
            }
        }

    }

    @Override
    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
        if (mWifiStates.get(i).equals("")) return false;
        showDialogCleardata(i, false);
        return true;
    }


    private void showDialogWifiInfo(int i){
        showDialogCleardata(i, true);
    }

    private void showDialogConnect(final ScanResult scanResult) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.dialog_wifi_connect, null);
        TextView txtName = view.findViewById(R.id.txt_wifi_name);
        final EditText edtPassword = view.findViewById(R.id.edt_wifi_password);
        Button btnCancel = view.findViewById(R.id.btn_cancel);
        Button btnConnect = view.findViewById(R.id.btn_connect);
        txtName.setText(scanResult.SSID);
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                wifiHelper.connectWifi(scanResult, edtPassword.getText().toString());
                dialog.dismiss();
            }
        });
        dialog.setContentView(view);
        dialog.show();

    }

    private void showDialogCleardata(final int i, boolean b){
        final String ssid = mWifiList.get(i).SSID;
        final CleardataCallback callback = new CleardataCallback() {
            @Override
            public void next() {
                mWifiStates.set(i, "");
                mWifiAdapter.updateState(mWifiStates);
            }
        };
        View view = LayoutInflater.from(mContext).inflate(R.layout.dialog_wifi_delete, null);
        TextView textView = view.findViewById(R.id.txt_forget);
        textView.setText(b ? "断开连接" : "取消保存");
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                wifiHelper.clearWifiInfo(ssid, callback);
                dialog.dismiss();
            }
        });
        dialog.setContentView(view);
        dialog.show();
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        if (b){
            openWifi();
        }else{
            closeWifi();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK){
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    class MyHandler extends Handler{
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch(msg.what){
                case HANDLER_SOCKET_ERROR:
                    String strError = msg.obj.toString();
                    showToast("connect error : " + strError);
                    break;
                case HANDLER_RECEIVE:
                    String strRevData = msg.obj.toString();
                    dealData(strRevData);
                    break;
            }
        }
    }

    class WifiAdapter extends BaseAdapter{
        Context context;
        List<ScanResult> list;
        List<String> states;

        public WifiAdapter(Context context, List<ScanResult> list, List<String> states){
            this.context = context;
            this.list = list;
            this.states = states;
        }

        public void update(List<ScanResult> list){
            this.list = list;
            notifyDataSetChanged();
        }

        public void updateState(List<String> states){
            this.states = states;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return list.size();
        }

        @Override
        public ScanResult getItem(int i) {
            return list.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder holder;
            if (view == null){
                view = LayoutInflater.from(context).inflate(R.layout.item_wifi_adapter, null);
                holder = new ViewHolder();
                holder.imageView = view.findViewById(R.id.img_wifi_signal);
                holder.txtName = view.findViewById(R.id.txt_wifi_name);
                holder.txtState = view.findViewById(R.id.txt_wifi_state);
                view.setTag(holder);
            }else{
                holder = (ViewHolder) view.getTag();
            }
            ScanResult scanResult = list.get(i);
            holder.imageView.setImageResource(wifiHelper.translateLevel(scanResult.level));
            holder.txtName.setText(scanResult.SSID);
            holder.txtState.setText(states.get(i));
            return view;
        }

        class ViewHolder{
            ImageView imageView;
            TextView txtName;
            TextView txtState;
        }

    }

    class WifiBroadcastReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()){
                case WifiManager.WIFI_STATE_CHANGED_ACTION:
                    int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_DISABLED);
                    switch (wifiState){
                        case WifiManager.WIFI_STATE_ENABLING:
                            log("wifi enabling");
                            txtInfo.setText("Wifi正在开启..");
                            break;
                        case WifiManager.WIFI_STATE_ENABLED:
                            log("wifi enabled");
                            txtInfo.setText("Wifi已打开");
                            swtWifi.setChecked(true);
                            startScan();
                            break;
                        case WifiManager.WIFI_STATE_DISABLING:
                            log("wifi disabling");
                            txtInfo.setText("Wifi正在关闭..");
                            break;
                        case WifiManager.WIFI_STATE_DISABLED:
                            txtInfo.setText("Wifi已关闭");
                            swtWifi.setChecked(false);
                            clearScanresults();
                            log("wifi disabled");
                            break;
                        case WifiManager.WIFI_STATE_UNKNOWN:
                            log("wifi unknown");
                            txtInfo.setText("Wifi状态未知");
                            break;
                    }
                    break;
                case WifiManager.NETWORK_STATE_CHANGED_ACTION:
                    NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                    if (info.getState().equals(NetworkInfo.State.DISCONNECTED)) {
                        txtInfo.setText("请连接网络进行注册");
                        btnRegister.setVisibility(View.GONE);
                    } else if (info.getState().equals(NetworkInfo.State.CONNECTED)) {
                        final WifiInfo wifiInfo = wifiHelper.getCurrentWifi();
                        txtInfo.setText("已连接到网络:" + wifiInfo.getSSID() + ", 请注册");
                        btnRegister.setVisibility(View.VISIBLE);
                    }else {
                        log("getExtraInfo():"+info.getExtraInfo()); //当前正在连接的WIFI的SSID
                        NetworkInfo.DetailedState state = info.getDetailedState();
                        if (state == state.CONNECTING) {
                            txtInfo.setText("连接中...");
                        } else if (state == state.AUTHENTICATING) {
                            txtInfo.setText("正在验证身份信息...");
                        } else if (state == state.OBTAINING_IPADDR) {
                            txtInfo.setText("正在获取IP地址...");
                        } else if (state == state.FAILED) {
                            txtInfo.setText("连接失败");
                        }
                    }
                    break;
                case WifiManager.SCAN_RESULTS_AVAILABLE_ACTION:
                    log("scan_results_available");
                    updateScanResult();
                    break;
                case WifiManager.SUPPLICANT_STATE_CHANGED_ACTION:
                    break;
            }
        }
    }


    public interface CleardataCallback{
        void next();
    }

    public void log(String str){
        Log.d("===yuanye===", str);
    }

    public void showToast(String str){
        Toast.makeText(mContext, str, Toast.LENGTH_SHORT).show();
    }
}
