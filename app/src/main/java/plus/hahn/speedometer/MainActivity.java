package plus.hahn.speedometer;

import android.bluetooth.BluetoothAdapter;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class MainActivity extends AppCompatActivity implements FragmentManager.OnBackStackChangedListener {

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
        getSupportFragmentManager().addOnBackStackChangedListener(this);
        if (savedInstanceState == null)
            getSupportFragmentManager().beginTransaction().add(R.id.fragment, new DevicesFragment(), "devices").commit();
        else
            onBackStackChanged();
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
    public void onBackStackChanged() {
        getSupportActionBar().setDisplayHomeAsUpEnabled(getSupportFragmentManager().getBackStackEntryCount()>0);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
