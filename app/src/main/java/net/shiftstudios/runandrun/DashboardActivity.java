package net.shiftstudios.runandrun;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.hoho.android.usbserial.driver.FtdiSerialDriver;
import com.hoho.android.usbserial.driver.ProbeTable;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.HexDump;

import net.shiftstudios.runandrun.view.Speedometer;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class DashboardActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private final String TAG = DashboardActivity.class.getSimpleName();

    public Speedometer speedometer;

    private List<UsbSerialPort> mEntries = new ArrayList<UsbSerialPort>();

    private UsbManager mUsbManager;
    private static UsbSerialPort sPort = null;

    public String tempData = "";
    public String currData = "[0,0,0]";

    double rpm = 0, w = 0, ws = 0;
    double diameter = 0.508; //m
    double weight = 77.7; // km

    long tempTime;
    long startTime;
    long ranMeters = 0;
    double calories = 0;
    double watt = 0;

    TextView uiDebugText, uiCalories, uiWatt, uiCarbonDioxide, uiCaution;
    ScrollView mainScrollView;
    RelativeLayout layout;
    RelativeLayout.LayoutParams params;

    public Thread t = new Thread(new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "Start reading...");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mainScrollView.smoothScrollTo(0, 200);
                }
            });

            while (true) try {
                byte buffer[] = new byte[256];
                int numBytesRead = sPort.read(buffer, 128);

                try {
                    updateReceivedData(buffer);
                } catch (Exception e) {
                }

                Thread.sleep(100);

            } catch (IOException e) {
                // Deal with error.
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        startTime = System.currentTimeMillis();

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        speedometer = (Speedometer) findViewById(R.id.view2);
        speedometer.setHandTarget(0);

        uiDebugText = (TextView) findViewById(R.id.uiDebugText);
        uiCalories = (TextView) findViewById(R.id.uiCalories);
        uiWatt = (TextView) findViewById(R.id.uiWatt);
        uiCarbonDioxide = (TextView) findViewById(R.id.uiCarbonDioxide);
        uiCaution = (TextView) findViewById(R.id.textView6);

        mainScrollView = (ScrollView) findViewById(R.id.scrollView);

        layout = (RelativeLayout) findViewById(R.id.layout);
        params = (RelativeLayout.LayoutParams) speedometer.getLayoutParams();

        mainScrollView.getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener() {

            @Override
            public void onScrollChanged() {
                int scrollPos = mainScrollView.getScrollY();
                Log.d(TAG, scrollPos + "");
                params.setMargins(0, -scrollPos / 2, 0, 0);
                speedometer.setLayoutParams(params);

                uiCaution.setAlpha((float) (150 - scrollPos) / (float) (150));
            }
        });

        refreshDeviceList();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.dashboard, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_mail) {
            // Handle the camera action
        } else if (id == R.id.nav_dashboard) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void refreshDeviceList() {
        new AsyncTask<Void, Void, List<UsbSerialPort>>() {
            @Override
            protected List<UsbSerialPort> doInBackground(Void... params) {
                SystemClock.sleep(1000);

                ProbeTable customTable = UsbSerialProber.getDefaultProbeTable();
                customTable.addProduct(0x046D, 0xC52F, FtdiSerialDriver.class);

                UsbSerialProber prober = new UsbSerialProber(customTable);
                final List<UsbSerialDriver> drivers = prober.findAllDrivers(mUsbManager);

                final List<UsbSerialPort> result = new ArrayList<UsbSerialPort>();
                for (final UsbSerialDriver driver : drivers) {
                    final List<UsbSerialPort> ports = driver.getPorts();

                    result.addAll(ports);
                }

                return result;
            }

            @Override
            protected void onPostExecute(List<UsbSerialPort> result) {
                mEntries.clear();
                mEntries.addAll(result);

                if (mEntries.size() > 0) {
                    final UsbSerialPort port = mEntries.get(0);
                    final UsbSerialDriver driver = port.getDriver();
                    final UsbDevice device = driver.getDevice();

                    sPort = port;
                    startIoManager();

                    final String title = String.format("Listening to device: Vendor 0x%s Product 0x%s",
                            HexDump.toHexString((short) device.getVendorId()),
                            HexDump.toHexString((short) device.getProductId()));
                }
            }

        }.execute((Void) null);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sPort != null) {
            try {
                sPort.close();
            } catch (IOException e) {
                // Ignore.
            }
            sPort = null;
        }
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "Resumed, port=" + sPort);
        if (sPort == null) {
        } else {
            startIoManager();
        }
        onDeviceStateChange();
    }

    private void startIoManager() {
        if (sPort != null) {
            final UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
            //usbManager.requestPermission(sPort.getDriver().getDevice(), null);

            UsbDeviceConnection connection = usbManager.openDevice(sPort.getDriver().getDevice());
            if (connection == null) {
                return;
            }

            try {
                Log.d(TAG, "Connection...");
                sPort.open(connection);
                sPort.setParameters(9600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);

                t.start();

            } catch (IOException e) {
                Log.e(TAG, "Error setting up device: " + e.getMessage(), e);
                try {
                    sPort.close();
                } catch (IOException e2) {
                    // Ignore.
                }
                sPort = null;
                return;
            }
        } else {
            Log.d(TAG, "Port null ??");
        }
    }

    private void onDeviceStateChange() {
        startIoManager();
    }

    private void updateReceivedData(byte[] bytedata) {
        final String message = new String(bytedata, 0, bytedata.length).replaceAll("[^(\\p{Digit}|\\p{Punct})]", "");;

        if (message.length() != 0) {
            tempData += message;
        }

        String data = "";

        boolean parseStart = false;
        char[] chars = tempData.toCharArray();
        for (char c : chars) {
            if (c == '[') parseStart = true;
            if (parseStart) data += String.valueOf(c);
            if (c == ']') break;
        }

        if (data.contains("]")) {
            currData = data;
            Log.d(TAG, currData);

            parseCurrData();

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    uiDebugText.setText(currData);
                }
            });

            tempData = tempData.replace(currData, "");
        }
    }

    public void parseCurrData(){
        String[] datas = currData.replace("[", "").replace("]", "").split("[,]");

        rpm = Double.parseDouble(datas[0]);
        w = Double.parseDouble(datas[1]);
        ws = Double.parseDouble(datas[2]);

        double speed = rpm * (diameter * 3.1415926535897932384) / 50 * 3;
        speedometer.setHandTarget((float) speed);

        if (tempTime != 0) {
            long deltaTime = System.currentTimeMillis() - tempTime;
            ranMeters += (long) (speed * (double) deltaTime / 3600d);
            calories = weight * 0.31 * (double) ranMeters / 1000d;
            watt = ws;
            DecimalFormat df = new DecimalFormat("#,##0.00");
            uiCalories.setText(df.format(calories));
            uiWatt.setText(df.format(watt));
            uiCarbonDioxide.setText(df.format(ranMeters / 1000d * 210d));
        }

        tempTime = System.currentTimeMillis();
    }

}
