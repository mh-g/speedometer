package plus.hahn.speedometer;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentResultListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;


public class MainActivity extends AppCompatActivity {

    private BluetoothAdapter bluetoothAdapter;
    ActivityResultLauncher<String> requestBluetoothPermissionLauncherForRefresh;
    private boolean failed;
    private String myDeviceAddress;
    private Bundle args = new Bundle();
    private File logFile;
    public FileOutputStream logFileStream = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportFragmentManager().setFragmentResultListener("requestKey", this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle bundle) {
                String result = bundle.getString("bundleKey");
                if (result.compareTo("disconnected") == 0) {
                    // try to reconnect immediately
                    cleanupOldFragment();
                    closeFile();
                    connect();
                }
            }
        });

        refresh();

        if (myDeviceAddress != null) {
            cleanupOldFragment();
            closeFile();
            connect();
        }
    }

    void refresh() {
        // check if bluetooth is available and working
        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH))
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            args.putString("error", "<Bluetooth not supported>");
            failed = true;
        } else if (!bluetoothAdapter.isEnabled()) {
            args.putString("error", "<Bluetooth is disabled>");
            failed = true;
        }

        for (BluetoothDevice device : bluetoothAdapter.getBondedDevices()) {
            if (device.getType() != BluetoothDevice.DEVICE_TYPE_LE) {
                if (device.getName().compareTo("ESP32speedometer") == 0) {
                    if (myDeviceAddress != null) {
                        args.putString("error", "<Multiple ESP32speedometers found>");
                        failed = true;
                    }
                    myDeviceAddress = device.getAddress();
                }
            }
        }
    }

    private void closeFile() {
        if (logFileStream != null) {
            try {
                logFileStream.close();
            } catch (IOException e) {
                args.putString("error", "<Could not close file>");
            }
            logFileStream = null;
        }
    }

    private static boolean isExternalStorageReadOnly() {
        String extStorageState = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(extStorageState)) {
            return true;
        }
        return false;
    }

    private static boolean isExternalStorageAvailable() {
        String extStorageState = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(extStorageState)) {
            return true;
        }
        return false;
    }

    public void cleanupOldFragment () {
        Fragment oldFragment = getSupportFragmentManager().findFragmentById(R.id.fragment);
        if (oldFragment != null) {
            getSupportFragmentManager().beginTransaction().remove(oldFragment).commit();
        }
    }

    public void connect () {
        if (isExternalStorageAvailable() && !isExternalStorageReadOnly()) {
            closeFile();
            logFile = new File (getExternalFilesDir("speedometer"),
                    "log-" + new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new java.util.Date()) + ".txt");
            try {
                logFileStream = new FileOutputStream(logFile);
            } catch (IOException e) {
                logFile = null;
                logFileStream = null;
                args.putString("error", "<Could not open file>");
            }
        }

        args.putString("device", myDeviceAddress);
        Fragment fragment = new TerminalFragment();
        fragment.setArguments(args);
        getSupportFragmentManager().beginTransaction().add(R.id.fragment, fragment, "terminal").commit();
    }

    private ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (!isGranted) {
                    args.putString("error", "<Permission not granted>");
                }
            });

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
