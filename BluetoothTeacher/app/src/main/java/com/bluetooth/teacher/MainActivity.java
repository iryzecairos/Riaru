package com.bluetooth.teacher;

import static com.bluetooth.teacher.utils.SharedPreferencesHelper.clearPreferences;

import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.Manifest;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;

import com.bluetooth.teacher.utils.Tools;
import com.bluetooth.communicator.BluetoothCommunicator;
import com.bluetooth.communicator.Peer;

import java.util.ArrayList;
import java.util.List;
/*
 * Главная активность приложения
 */
public class MainActivity extends AppCompatActivity {
    public static final int NO_PERMISSIONS = -10;
    private static final int REQUEST_CODE_REQUIRED_PERMISSIONS = 2;
    public static final String[] REQUIRED_PERMISSIONS = new String[]{
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_COARSE_LOCATION,
    };

    // Менеджер Bluetooth Communicator
    private BluetoothManager bluetoothManager;

    // Список обратных вызовов для активности
    // Используется для отслеживания состояния подключения
    private ArrayList<Callback> clientsCallbacks = new ArrayList<>();
    private CoordinatorLayout fragmentContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bluetoothManager = (BluetoothManager) getApplication();
        clearPreferences(this);

        // Удаление всех фрагментов из обратного стека и фрагмент менеджера
        FragmentManager fragmentManager = getSupportFragmentManager();
        if (fragmentManager.getBackStackEntryCount() > 0) {
            fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
        List<Fragment> fragmentList = fragmentManager.getFragments();
        for (Fragment fragment : fragmentList) {
            if (fragment != null) {
                fragmentManager.beginTransaction().remove(fragment).commit();
            }
        }

        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        fragmentContainer = findViewById(R.id.fragment_container);

        // Добавление обратных вызовов для BluetoothCommunicator
        bluetoothManager.getBluetoothCommunicator().addCallback(new BluetoothCommunicator.Callback() {
            @Override
            public void onAdvertiseStarted() {
                super.onAdvertiseStarted();
                if (bluetoothManager.getBluetoothCommunicator().isDiscovering()) {
                    notifySearchStarted();
                }
            }

            @Override
            public void onDiscoveryStarted() {
                super.onDiscoveryStarted();
                if (bluetoothManager.getBluetoothCommunicator().isAdvertising()) {
                    notifySearchStarted();
                }
            }

            @Override
            public void onAdvertiseStopped() {
                super.onAdvertiseStopped();
                if (!bluetoothManager.getBluetoothCommunicator().isDiscovering()) {
                    notifySearchStopped();
                }
            }

            @Override
            public void onDiscoveryStopped() {
                super.onDiscoveryStopped();
                if (!bluetoothManager.getBluetoothCommunicator().isAdvertising()) {
                    notifySearchStopped();
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (bluetoothManager.getBluetoothCommunicator().getConnectedPeersList().size() == 0) {
            setFragment();
        }
    }
    // Установка фрагмента в контейнер
    public void setFragment() {
        SearchingFragment paringFragment = new SearchingFragment();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE);
        transaction.replace(R.id.fragment_container, paringFragment);
        transaction.commit();
    }

    @Override
    public void onBackPressed() {
        DialogInterface.OnClickListener confirmExitListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                exitFromConversation();
            }
        };
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (fragment != null) {
            super.onBackPressed();
        }
    }
    // Выход из состояния "отмечания"
    public void exitFromConversation() {
        if (bluetoothManager.getBluetoothCommunicator().getConnectedPeersList().size() > 0) {
            Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
            bluetoothManager.getBluetoothCommunicator().disconnectFromAll();
        } else {
            setFragment();
        }
    }
    // Проверка разрешений для начала поиска устройств Bluetooth
    public int isCanSearchBluetooth() {
        if (bluetoothManager.getBluetoothCommunicator().isBluetoothLeSupported() == BluetoothCommunicator.SUCCESS) {
            if (Tools.hasPermissions(this, REQUIRED_PERMISSIONS)) {
                int advertisingCode = bluetoothManager.getBluetoothCommunicator().startAdvertising();
                int discoveringCode = bluetoothManager.getBluetoothCommunicator().startDiscovery();
                if (advertisingCode == discoveringCode) {
                    return advertisingCode;
                }
                if (advertisingCode == BluetoothCommunicator.BLUETOOTH_LE_NOT_SUPPORTED || discoveringCode == BluetoothCommunicator.BLUETOOTH_LE_NOT_SUPPORTED) {
                    return BluetoothCommunicator.BLUETOOTH_LE_NOT_SUPPORTED;
                }
                if (advertisingCode == BluetoothCommunicator.SUCCESS || discoveringCode == BluetoothCommunicator.SUCCESS) {
                    if (advertisingCode == BluetoothCommunicator.ALREADY_STARTED || discoveringCode == BluetoothCommunicator.ALREADY_STARTED) {
                        return BluetoothCommunicator.SUCCESS;
                    }
                }
                return BluetoothCommunicator.ERROR;
            } else {
                requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_REQUIRED_PERMISSIONS);
                return NO_PERMISSIONS;
            }
        } else {
            return BluetoothCommunicator.BLUETOOTH_LE_NOT_SUPPORTED;
        }
    }
    // Завершение поиска по Bluetooth
    public int stopSearch(boolean tryRestoreBluetoothStatus) {
        int advertisingCode = bluetoothManager.getBluetoothCommunicator().stopAdvertising(tryRestoreBluetoothStatus);
        int discoveringCode = bluetoothManager.getBluetoothCommunicator().stopDiscovery(tryRestoreBluetoothStatus);
        if (advertisingCode == discoveringCode) {
            return advertisingCode;
        }
        if (advertisingCode == BluetoothCommunicator.BLUETOOTH_LE_NOT_SUPPORTED || discoveringCode == BluetoothCommunicator.BLUETOOTH_LE_NOT_SUPPORTED) {
            return BluetoothCommunicator.BLUETOOTH_LE_NOT_SUPPORTED;
        }
        if (advertisingCode == BluetoothCommunicator.SUCCESS || discoveringCode == BluetoothCommunicator.SUCCESS) {
            if (advertisingCode == BluetoothCommunicator.ALREADY_STOPPED || discoveringCode == BluetoothCommunicator.ALREADY_STOPPED) {
                return BluetoothCommunicator.SUCCESS;
            }
        }
        return BluetoothCommunicator.ERROR;
    }

    // Установка подключения к найденному девайсу
    public void connect(Peer peer) {
        stopSearch(false);
        bluetoothManager.getBluetoothCommunicator().connect(peer);
    }

    // Подтверждение полученного запроса на подключение (есть для обоих устройств)
    // И запрос и ответ кидаются обоюдно
    public void acceptConnection(Peer peer) {
        bluetoothManager.getBluetoothCommunicator().acceptConnection(peer);
    }

    public int disconnect(Peer peer) {
        return bluetoothManager.getBluetoothCommunicator().disconnect(peer);
    }

    // Добавление в список обратных вызовов
    public void addCallback(Callback callback) {
        bluetoothManager.getBluetoothCommunicator().addCallback(callback);
        clientsCallbacks.add(callback);
    }

    public void removeCallback(Callback callback) {
        bluetoothManager.getBluetoothCommunicator().removeCallback(callback);
        clientsCallbacks.remove(callback);
    }

    private void notifySearchStarted() {
        for (int i = 0; i < clientsCallbacks.size(); i++) {
            clientsCallbacks.get(i).onSearchStarted();
        }
    }

    private void notifySearchStopped() {
        for (int i = 0; i < clientsCallbacks.size(); i++) {
            clientsCallbacks.get(i).onSearchStopped();
        }
    }

    public static class Callback extends BluetoothCommunicator.Callback {
        public void onSearchStarted() {
        }

        public void onSearchStopped() {
        }

        public void onMissingSearchPermission() {
        }

        public void onSearchPermissionGranted() {
        }
    }

    public CoordinatorLayout getFragmentContainer() {
        return fragmentContainer;
    }
}