package plus.hahn.speedometer;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentResultListener;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private BluetoothAdapter bluetoothAdapter;
    ActivityResultLauncher<String> requestBluetoothPermissionLauncherForRefresh;
    private boolean failed;
    private String myDeviceAddress;
    private Bundle args = new Bundle();

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
                    connect();
                }
            }
        });

        refresh();

        if (myDeviceAddress != null) {
            cleanupOldFragment();
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

    public void cleanupOldFragment () {
        Fragment oldFragment = getSupportFragmentManager().findFragmentById(R.id.fragment);
        if (oldFragment != null) {
            getSupportFragmentManager().beginTransaction().remove(oldFragment).commit();
        }
    }

    public void connect () {
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
