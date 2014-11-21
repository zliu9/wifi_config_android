package com.intel.www.mugconfig;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.graphics.Color;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;
import android.net.wifi.WifiManager;
import android.widget.ImageView;

import android.view.Window;
import android.view.WindowManager;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Arrays;
import java.io.File;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

public class MugActivity extends Activity {

    private Button btnSubmit = null;
    private Spinner spWifiList = null;
    private TextView tvPreview = null;
    private List<ScanResult> wifiList;
    private WifiManager wm;
    private WifiScanReceiver rec;
    private String wifis[];
    private EditText etSSID = null;
    private EditText etKeyMgmt = null;
    private TextView tvFile = null;
    private EditText etPasswd = null;

    final private String FILE_NAME = "wpa_supplicant.conf";
    final private String DIR_NAME = "smart_mug";
    
    final private String wpaFormat =
            "\nctrl_interface=/var/run/wpa_supplicant\n" +
                    "ctrl_interface_group=0\n" +
                    "update_config=1\n" +
                    "ap_scan=1\n" +
                    "network={\n" +
                    "   ssid=\"%s\"\n" +
                    "   key_mgmt=%s\n" +
                    "   psk=\"%s\"\n" +
                    "}\n";

    List<ScanResult> wifiScanList;


    private static final int STOPSPLASH = 0;
    //time in milliseconds
    private static final long SPLASHTIME = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        //getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_mug);

        // Initialize handlers
        btnSubmit = (Button)findViewById(R.id.button_submit);
        spWifiList = (Spinner)findViewById(R.id.spinner_scan);
        //tvPreview = (TextView)findViewById(R.id.textView5);
        btnSubmit = (Button)findViewById(R.id.button_submit);
        etSSID = (EditText)findViewById(R.id.editText_SSID);
        etKeyMgmt = (EditText)findViewById(R.id.editText_auth);
        tvFile = (TextView)findViewById(R.id.textView_info);
        etPasswd = (EditText)findViewById(R.id.editText_passwd);

        etSSID.addTextChangedListener(new InputWatcher());
        etKeyMgmt.addTextChangedListener(new InputWatcher());
        etPasswd .addTextChangedListener(new InputWatcher());


        rec = new WifiScanReceiver();
        registerReceiver(rec, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        wm = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        wm.startScan();

        spWifiList.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                String sel = spWifiList.getSelectedItem().toString();
                etSSID.setText(sel);

                for(int idx = 0; idx < wifiScanList.size(); idx++){
                    ScanResult result = wifiScanList.get(idx);
                    if(result.SSID.equals(sel)) {
                        etKeyMgmt.setText(result.capabilities);
                        break;
                    }
                }

                genWpaFile();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }

    private void genWpaFile()
    {
        btnSubmit.setEnabled(false);

        String content = String.format(wpaFormat, etSSID.getText(), etKeyMgmt.getText(), etPasswd.getText());
        String keyMgmt = etKeyMgmt.getText().toString();
        String wpaKeyMgmt = null;
        String psk = etPasswd.getText().toString();

        if(keyMgmt.contains("WPA-PSK") || keyMgmt.contains("WPA2-PSK")) {
            wpaKeyMgmt =  "WPA-PSK";
        }

        if(wpaKeyMgmt == null) {
            tvFile.setTextColor(Color.RED);
            tvFile.setText("\nERROR: currently only WPA-PSK/WPA2-PSK are supported\n");
            return;
        }

        if(psk == null || psk.length() == 0) {
            tvFile.setTextColor(Color.RED);
            tvFile.setText("\nERROR: No password\n");
            return;
        }

        tvFile.setTextColor(Color.BLACK);
        tvFile.setText(String.format(wpaFormat, etSSID.getText(), wpaKeyMgmt, etPasswd.getText()));
        btnSubmit.setEnabled(true);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.mug, menu);
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

    class WifiScanReceiver extends BroadcastReceiver {

        public void onReceive(Context c, Intent intent) {
            if(wifiScanList != null)
              wifiScanList.clear();

            wifiScanList = wm.getScanResults();

            wifis = new String[wifiScanList.size()];
            for(int i = 0; i < wifiScanList.size(); i++){
                wifis[i] = ((wifiScanList.get(i)).SSID);
            }

            // remove duplicated SSID.
            wifis = new HashSet<String>(Arrays.asList(wifis)).toArray(new String[0]);

            spWifiList.setAdapter(new ArrayAdapter<String>(getApplicationContext(),
                    android.R.layout.simple_list_item_1, wifis));

            unregisterReceiver(rec);
        }
    }

    class InputWatcher implements TextWatcher {

        public void afterTextChanged(Editable s) {
            genWpaFile();
        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {
            genWpaFile();
        }

        public void beforeTextChanged(CharSequence s, int start, int count, int after){}
    }

    private void ShowMsg(String str)
    {
        Toast.makeText(getApplicationContext(), str, Toast.LENGTH_SHORT).show();
    }

    private void SubmitFile(File f){
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);

        intent.setType("*/*");
        intent.setClassName("com.android.bluetooth", "com.android.bluetooth.opp.BluetoothOppLauncherActivity");
        intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(f));
        startActivity(intent);

        ShowMsg("select mug_**:**:**:**:**:**");
    }

    private void saveAndSubmitFile() {

        try {
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                File sdCardDir = Environment.getExternalStorageDirectory();//获取SDCard目录
                File dir = new File(sdCardDir, DIR_NAME);
                if(!dir.exists()) {
                    dir.mkdir();
                }

                File saveFile = new File(sdCardDir, DIR_NAME + "//" + FILE_NAME);

                FileOutputStream outStream = new FileOutputStream(saveFile);

                byte[] buffer = tvFile.getText().toString().getBytes();
                outStream.write(buffer);
                outStream.close();

                ShowMsg("saved to " + saveFile.toString());

                SubmitFile(saveFile);

            } else {
                ShowMsg("ERROR: no SD card");
                return;
            }
        } catch(Exception e) {
            ShowMsg("ERROR: can not save to " + FILE_NAME);
            e.printStackTrace();
        }
    }

    public void onClickSubmit(View view) {
        saveAndSubmitFile();
    }
}
