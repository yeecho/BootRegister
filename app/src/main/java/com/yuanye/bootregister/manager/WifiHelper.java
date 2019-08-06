package com.yuanye.bootregister.manager;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.text.TextUtils;
import android.util.Log;

import com.yuanye.bootregister.MainActivity;
import com.yuanye.bootregister.R;

import java.util.ArrayList;
import java.util.List;

public class WifiHelper {

    public String TAG = "WifiHelper";

    WifiManager wifiManager;
    Context context;

    public WifiHelper(Context context){
        this.context = context;
        wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    }

    public void openWifi(){
        if (!isWifiEnabled())
            wifiManager.setWifiEnabled(true);
    }

    public void startScan() {
        wifiManager.startScan();
    }

    public List<ScanResult> getScanResults(){
        return wifiManager.getScanResults();
    }

    public void closeWifi(){
        if (isWifiEnabled())
            wifiManager.setWifiEnabled(false);
    }

    public boolean isWifiEnabled(){
        return wifiManager.isWifiEnabled();
    }

    public boolean isHistory(ScanResult scanResult){
        String ssid = "\"" + scanResult.SSID + "\"";
        for (WifiConfiguration wc : wifiManager.getConfiguredNetworks()) {
            if(wc.SSID.equals(ssid)) {
                return true;
            }
        }
        return false;
    }

    public WifiInfo getCurrentWifi () {
        return wifiManager.getConnectionInfo();
    }

    /**
     * @des 连接已经保存过配置的wifi
     * @param ssid
     */
    public void connectSavedWifi (String ssid) {

        Log.d(TAG, "connectWifi: 去连接wifi: " + ssid);

        if (!isWifiEnabled()) return;

        WifiConfiguration configuration = getWifiConfig(ssid);
        if (configuration != null) {
            wifiManager.enableNetwork(configuration.networkId, true);
        }

    }

    public void connectWifi(ScanResult scanResult, String password){
        String ssid = scanResult.SSID;
        int encryptType = getWifiEncryptType(scanResult.capabilities);
        connectWifi(ssid, password, encryptType);
    }

    /**
     * @des 连接没有配置过的wifi
     * @param ssid
     * @param password
     * @param encryptType
     */
    public void connectWifi (String ssid, String password, int encryptType) {

        Log.d(TAG, "connectWifi: 去连接wifi: " + ssid);

        if (!isWifiEnabled()) return;

        WifiConfiguration wc = new WifiConfiguration();
        wc.allowedAuthAlgorithms.clear();
        wc.allowedGroupCiphers.clear();
        wc.allowedKeyManagement.clear();
        wc.allowedPairwiseCiphers.clear();
        wc.allowedProtocols.clear();

        wc.SSID = "\"" + ssid + "\"";

        WifiConfiguration configuration = getWifiConfig(ssid);
        if (configuration != null) {
            wifiManager.removeNetwork(configuration.networkId);
        }

        switch (encryptType) {
            case 4://不加密
                wc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                break;

            case 3://wep加密
                wc.hiddenSSID = true;
                wc.wepKeys[0] = "\"" + password +"\"";
                wc.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
                wc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
                wc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
                wc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
                wc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
                wc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);

                break;
            case 0: //wpa/wap2加密
            case 1: //wpa2加密
            case 2: //wpa加密

                wc.preSharedKey = "\"" + password + "\"";
                wc.hiddenSSID = true;
                wc.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
                wc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
                wc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
                wc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                wc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                wc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
                wc.status = WifiConfiguration.Status.ENABLED;
                break;
        }

        int network = wifiManager.addNetwork(wc);
        wifiManager.enableNetwork(network, true);
    }

    public void clearWifiInfo(String ssid, MainActivity.CleardataCallback callback) {

        Log.d(TAG, "clearWifiInfo: 清除WIFI配置信息: " + ssid);

        String newSSID;

        if (!(ssid.startsWith("\"") && ssid.endsWith("\""))) {
            newSSID = "\"" + ssid + "\"";
        } else {
            newSSID = ssid;
        }

        WifiConfiguration configuration = getWifiConfig(newSSID);
        configuration.allowedAuthAlgorithms.clear();
        configuration.allowedGroupCiphers.clear();
        configuration.allowedKeyManagement.clear();
        configuration.allowedPairwiseCiphers.clear();
        configuration.allowedProtocols.clear();

        if (configuration != null) {

            wifiManager.removeNetwork(configuration.networkId);
            wifiManager.saveConfiguration();
        }

        callback.next();
    }

    public WifiConfiguration getWifiConfig (String ssid) {

        Log.d(TAG, "getWifiConfig: 获取wifi配置信息: " + ssid);

        if (TextUtils.isEmpty(ssid)) return null;

        String newSSID;

        if (!(ssid.startsWith("\"") && ssid.endsWith("\""))) {
            newSSID = "\"" + ssid + "\"";
        } else {
            newSSID = ssid;
        }

        List<WifiConfiguration> configuredNetworks = wifiManager.getConfiguredNetworks();

        for (WifiConfiguration configuration : configuredNetworks) {
            if (newSSID.equalsIgnoreCase(configuration.SSID)) {
                return configuration;
            }
        }

        return null;
    }

    public int translateLevel(int level){
        int resId;
        if (level <= 0 && level >= -50) {
            resId = R.drawable.ic_wifi_signal_4_dark;
        } else if (level < -50 && level >= -70) {
            resId = R.drawable.ic_wifi_signal_3_dark;
        } else if (level < -70 && level >= -80) {
            resId = R.drawable.ic_wifi_signal_2_dark;
        } else if (level < -80 && level >= -100) {
            resId = R.drawable.ic_wifi_signal_1_dark;
        } else {
            resId = R.drawable.ic_wifi_signal_1_dark;
        }
        return resId;
    }


    public String getWifiEncryptTypeStr (String capabilitie) {
        if (TextUtils.isEmpty(capabilitie)) return null;

        String encryptType;

        if (capabilitie.contains("WPA") && capabilitie.contains("WPA2")) {
            encryptType = "WPA/WPA2 PSK";
        } else if (capabilitie.contains("WPA2")) {
            encryptType = "WPA2 PSK";
        } else if (capabilitie.contains("WPA")) {
            encryptType = "WPA PSK";
        } else if (capabilitie.contains("WEP")) {
            encryptType = "WEP";
        } else {
            encryptType = "NONE";
        }

        return encryptType;
    }

    /**
     * wifi加密方式有5种
     * 0 - WPA/WPA2 PSK
     * 1 - WPA2 PSK
     * 2 - WPA PSK
     * 3 - WEP
     * 4 - NONE
     * @param capabilitie
     * @return
     */
    public int getWifiEncryptType (String capabilitie) {
        if (TextUtils.isEmpty(capabilitie)) return -1;

        int encryptType;

        if (capabilitie.contains("WPA") && capabilitie.contains("WPA2")) {
            encryptType = 0;
        } else if (capabilitie.contains("WPA2")) {
            encryptType = 1;
        } else if (capabilitie.contains("WPA")) {
            encryptType = 2;
        } else if (capabilitie.contains("WEP")) {
            encryptType = 3;
        } else {
            encryptType = 4;
        }

        return encryptType;
    }

    public List<String> stateChecking(List<ScanResult> mWifiList) {
        List<String> list = new ArrayList<>();
        for (ScanResult scanResult : mWifiList){
            if (isHistory(scanResult)){
                list.add("已保存");
            }else{
                list.add("");
            }
        }
        return list;
    }

    public boolean isNetworkAvailable(){
        // 获取手机所有连接管理对象（包括对wi-fi,net等连接的管理）
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null){
            return false;
        }else{
            NetworkInfo[] networkInfo = connectivityManager.getAllNetworkInfo();
            if (networkInfo != null && networkInfo.length > 0){
                for (int i = 0; i < networkInfo.length; i++){
                    if (networkInfo[i].getState() == NetworkInfo.State.CONNECTED){
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
