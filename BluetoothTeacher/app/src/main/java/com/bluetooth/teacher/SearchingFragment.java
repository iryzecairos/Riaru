package com.bluetooth.teacher;


import static com.bluetooth.teacher.utils.FileHelper.saveNamesToFile;
import static com.bluetooth.teacher.utils.SharedPreferencesHelper.appendStudent;
import static com.bluetooth.teacher.utils.SharedPreferencesHelper.clearPreferences;
import static com.bluetooth.teacher.utils.SharedPreferencesHelper.containsDeviceId;
import static com.bluetooth.teacher.utils.SharedPreferencesHelper.getStudentList;


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;

import com.bluetooth.communicator.Message;
import com.bluetooth.communicator.tools.Timer;
import com.bluetooth.teacher.ui.MessagesAdapter;
import com.bluetooth.communicator.BluetoothCommunicator;
import com.bluetooth.communicator.Peer;

import java.util.Objects;

/*
 * Фрагмент который ждет подключений от учеников Bluetooth
 * Здесь заключен весь дизайн и немного сохранений в SharedPreferences
 */
public class SearchingFragment extends Fragment {
    private ConstraintLayout constraintLayout;
    private Timer connectionTimer;
    private MainActivity.Callback communicatorCallback;
    private Peer connectingPeer;
    protected BluetoothManager bluetoothManager;
    protected MainActivity activity;
    private ProgressBar loading;
    private RecyclerView mRecyclerView;
    private LinearSmoothScroller smoothScroller;
    private MessagesAdapter mAdapter;
    private ConstraintLayout startButtonPlace;
    private ImageView saveStrings;
    private SharedPreferences sharedPreferences;
    private Toolbar toolbar;
    private TextView deviceName;

    public SearchingFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_pairing, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        activity = (MainActivity) requireActivity();
        bluetoothManager = (BluetoothManager) activity.getApplication();

        deviceName.setText(
                String.format("Название устройства - %s",
                bluetoothManager.getBluetoothCommunicator().getBluetoothAdapter().getName())
        );

        LinearLayoutManager layoutManager = new LinearLayoutManager(activity);
        layoutManager.setStackFromEnd(true);

        sharedPreferences = activity.getPreferences(Context.MODE_PRIVATE);

        toolbar = activity.findViewById(R.id.toolbarPairing);
        activity.setActionBar(toolbar);

        String lessonName = sharedPreferences.getString("lessonName", "");

        if (!Objects.requireNonNull(lessonName).isEmpty() && lessonName.length() > 2) {
            toolbar.setTitle(lessonName);
        }

        toolbar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showInputDialog();
            }
        });


        mRecyclerView.setLayoutManager(layoutManager);

        smoothScroller = new LinearSmoothScroller(activity) {
            @Override
            protected int calculateTimeForScrolling(int dx) {
                return 100;
            }
        };

        mAdapter = new MessagesAdapter(bluetoothManager.getBluetoothCommunicator().getUniqueName(), new MessagesAdapter.Callback() {
            @Override
            public void onFirstItemAdded() {
                mRecyclerView.setVisibility(View.VISIBLE);
            }
        });

        mRecyclerView.setAdapter(mAdapter);

        WindowInsets windowInsets = activity.getFragmentContainer().getRootWindowInsets();
        if (windowInsets != null) {

            constraintLayout.dispatchApplyWindowInsets(
                    windowInsets.replaceSystemWindowInsets(
                            windowInsets.getSystemWindowInsetLeft(),
                            windowInsets.getSystemWindowInsetTop(),
                            windowInsets.getSystemWindowInsetRight(), 0
                    ));
        }
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        constraintLayout = view.findViewById(R.id.container);
        startButtonPlace = view.findViewById(R.id.starImageView);
        mRecyclerView = view.findViewById(R.id.recycler_view);
        loading = view.findViewById(R.id.searchingProgressBar);
        saveStrings = view.findViewById(R.id.saveStrings);
        deviceName = view.findViewById(R.id.deviceName);
    }
    protected void startSearch() {
        int result = activity.isCanSearchBluetooth();
        if (result != BluetoothCommunicator.SUCCESS) {
            if (result == BluetoothCommunicator.BLUETOOTH_LE_NOT_SUPPORTED) {
                Toast.makeText(activity, getString(R.string.error_in_search),
                        Toast.LENGTH_SHORT).show();

            } else if (result != MainActivity.NO_PERMISSIONS
                    && result != BluetoothCommunicator.ALREADY_STARTED) {
                Toast.makeText(activity, getString(R.string.error_in_search),
                        Toast.LENGTH_SHORT).show();
            }
        }

        startButtonPlace.setVisibility(View.GONE);
    }

    private void resetConnectionTimer() {
        if (connectionTimer != null) {
            connectionTimer.cancel();
            connectionTimer = null;
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        startButtonPlace.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String lessonName = sharedPreferences.getString("lessonName", "");
                if (!Objects.requireNonNull(lessonName).isEmpty() && lessonName.length() > 2) {
                    loading.setVisibility(View.VISIBLE);
                    startSearch();
                } else {
                    showInputDialog();
                }

            }
        });

        saveStrings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveNamesToFile(activity, sharedPreferences.getString("lessonName", ""));
                clearPreferences(activity);
                mAdapter = new MessagesAdapter(bluetoothManager.getBluetoothCommunicator().getUniqueName(), new MessagesAdapter.Callback() {
                    @Override
                    public void onFirstItemAdded() {
                        mRecyclerView.setVisibility(View.VISIBLE);
                    }
                });
                mRecyclerView.setAdapter(mAdapter);
                mRecyclerView.setVisibility(View.GONE);
                saveStrings.setVisibility(View.GONE);
                startButtonPlace.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        activity.addCallback(communicatorCallback);
    }

    @Override
    public void onPause() {
        super.onPause();
        activity.removeCallback(communicatorCallback);
        activity.stopSearch(connectingPeer == null);

        if (connectingPeer != null) {
            activity.disconnect(connectingPeer);
            connectingPeer = null;
        }
    }

    private void showInputDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Введите название предмета");
        final EditText input = new EditText(activity);
        builder.setView(input);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String text = input.getText().toString();

                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("lessonName", text);
                editor.apply();
                toolbar.setTitle(text);
            }
        });

        builder.setNegativeButton("Отмена", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        communicatorCallback = new MainActivity.Callback() {
            @Override
            public void onConnectionRequest(final Peer peer) {
                super.onConnectionRequest(peer);
                if (peer != null) {
                    activity.acceptConnection(peer);
                }
            }

            @Override
            public void onMessageReceived(Message message, int source) {
                super.onMessageReceived(message, source);

                if (!containsDeviceId(getStudentList(activity), message.getHeader())){
                    mRecyclerView.setVisibility(View.VISIBLE);
                    loading.setVisibility(View.INVISIBLE);
                    saveStrings.setVisibility(View.VISIBLE);

                    appendStudent(activity, message.getHeader(), message.getText());
                    mAdapter.addMessage(message);
                    smoothScroller.setTargetPosition(mAdapter.getItemCount() - 1);
                    mRecyclerView.getLayoutManager().startSmoothScroll(smoothScroller);
                }

                bluetoothManager.getBluetoothCommunicator().disconnectFromAll();
                startSearch();
            }

            @Override
            public void onConnectionSuccess(Peer peer, int source) {
                super.onConnectionSuccess(peer, source);
                connectingPeer = null;
                resetConnectionTimer();
            }

            @Override
            public void onConnectionFailed(Peer peer, int errorCode) {
                super.onConnectionFailed(peer, errorCode);
                if (connectingPeer != null) {
                    if (connectionTimer != null && !connectionTimer.isFinished() && errorCode != BluetoothCommunicator.CONNECTION_REJECTED) {
                        activity.connect(peer);
                    } else {
                        startSearch();
                        connectingPeer = null;
                        if (errorCode == BluetoothCommunicator.CONNECTION_REJECTED) {
                            Toast.makeText(activity, peer.getName() + " refused the connection request", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(activity, "Connection error", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }

            @Override
            public void onPeerUpdated(Peer peer, Peer newPeer) {
                super.onPeerUpdated(peer, newPeer);
                onPeerFound(newPeer);
            }

            @Override
            public void onMissingSearchPermission() {
                super.onMissingSearchPermission();
                Toast.makeText(activity,
                        "Предоставьте разрешения для продолжения работы",
                        Toast.LENGTH_LONG).show();
            }

            @Override
            public void onSearchPermissionGranted() {
                super.onSearchPermissionGranted();
                startSearch();
            }
        };
    }
}