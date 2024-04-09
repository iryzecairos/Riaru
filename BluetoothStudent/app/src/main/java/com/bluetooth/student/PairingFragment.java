package com.bluetooth.student;

import static com.bluetooth.student.utils.DeviceInfoHelper.getDeviceUniqueId;

import android.animation.Animator;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import com.bluetooth.communicator.Message;
import com.bluetooth.communicator.tools.Timer;
import com.bluetooth.student.ui.ButtonSearch;
import com.bluetooth.student.ui.CustomAnimator;
import com.bluetooth.student.ui.GuiTools;
import com.bluetooth.student.ui.PeerListAdapter;
import com.bluetooth.student.ui.RequestDialog;
import com.bluetooth.student.utils.BluetoothHelper;
import com.bluetooth.student.utils.Tools;
import com.bluetooth.communicator.BluetoothCommunicator;
import com.bluetooth.communicator.Peer;

import java.util.ArrayList;
import java.util.Objects;

public class PairingFragment extends Fragment {
    public static final int CONNECTION_TIMEOUT = 5000;
    private ConstraintLayout constraintLayout;
    private Peer confirmConnectionPeer;
    private ListView listViewGui;
    private Timer connectionTimer;
    @Nullable
    private PeerListAdapter listView;
    private TextView discoveryDescription;
    private TextView noDevices;
    private TextView noPermissions;
    private TextView noBluetoothLe;
    private final Object lock = new Object();
    private MainActivity.Callback communicatorCallback;
    private RequestDialog connectionConfirmDialog;
    private CustomAnimator animator = new CustomAnimator();
    private Peer connectingPeer;
    protected BluetoothHelper global;
    protected MainActivity activity;
    private static final float LOADING_SIZE_DP = 24;
    protected boolean isLoadingVisible = false;
    private boolean appearSearchButton = false;
    protected boolean isLoadingAnimating;
    protected ButtonSearch buttonSearch;
    private ProgressBar loading;
    private SharedPreferences sharedPreferences;
    private Toolbar toolbar;
    private String deviceId;
    private ArrayList<CustomAnimator.EndListener> listeners = new ArrayList<>();
    private ConstraintLayout restoreLayout;


    public PairingFragment() {
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_pairing, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        constraintLayout = view.findViewById(R.id.container);
        listViewGui = view.findViewById(R.id.list_view);
        discoveryDescription = view.findViewById(R.id.discoveryDescription);
        noDevices = view.findViewById(R.id.noDevices);
        noPermissions = view.findViewById(R.id.noPermission);
        noBluetoothLe = view.findViewById(R.id.noBluetoothLe);
        buttonSearch = view.findViewById(R.id.searchButton);
        loading = view.findViewById(R.id.progressBar2);
        restoreLayout = view.findViewById(R.id.restorebutton);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        activity = (MainActivity) requireActivity();
        global = (BluetoothHelper) activity.getApplication();

        deviceId = getDeviceUniqueId(activity);

        toolbar = activity.findViewById(R.id.toolbarPairing);
        activity.setActionBar(toolbar);

        sharedPreferences = activity.getPreferences(Context.MODE_PRIVATE);
        toolbar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showInputDialog();
            }
        });

        String savedName = sharedPreferences.getString("name", "");

        if (!Objects.requireNonNull(savedName).isEmpty() && savedName.length() > 5) {
            toolbar.setTitle(savedName);
        }

        WindowInsets windowInsets = activity.getFragmentContainer().getRootWindowInsets();
        if (windowInsets != null) {
            constraintLayout.dispatchApplyWindowInsets(windowInsets.replaceSystemWindowInsets(windowInsets.getSystemWindowInsetLeft(), windowInsets.getSystemWindowInsetTop(), windowInsets.getSystemWindowInsetRight(), 0));
        }


        restoreLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startSearch();
            }
        });
        initializePeerList();
        listViewGui.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                synchronized (lock) {
                    if (listView != null) {
                        if (listView.isClickable()) {
                            String savedName = sharedPreferences.getString("name", "");
                            if (!Objects.requireNonNull(savedName).isEmpty() && savedName.length() > 5) {
                                Peer item = listView.get(i);
                                connect(item);
                            } else {
                                showInputDialog();
                            }

                        } else {
                            listView.getCallback().onClickNotAllowed(listView.getShowToast());
                        }
                    }
                }
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();

        activateInputs();

        disappearLoading(true, null);

        if (!Tools.hasPermissions(activity, MainActivity.REQUIRED_PERMISSIONS)) {
            startSearch();
        }

        buttonSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (activity.isSearching()) {
                    activity.stopSearch(false);
                    clearFoundPeers();
                } else {
                    startSearch();
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        clearFoundPeers();
        activity.addCallback(communicatorCallback);
        if (Tools.hasPermissions(activity, MainActivity.REQUIRED_PERMISSIONS)) {
            startSearch();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        activity.removeCallback(communicatorCallback);
        stopSearch();
        if (connectingPeer != null) {
            activity.disconnect(connectingPeer);
            connectingPeer = null;
        }
    }

    private void connect(final Peer peer) {
        connectingPeer = peer;
        confirmConnectionPeer = peer;
        deactivateInputs();
        appearLoading(null);
        activity.connect(peer);
        startConnectionTimer();
    }

    protected void startSearch() {
        int result = activity.startSearch();
        if (result != BluetoothCommunicator.SUCCESS) {
            if (result == BluetoothCommunicator.BLUETOOTH_LE_NOT_SUPPORTED && noBluetoothLe.getVisibility() != View.VISIBLE) {
                listViewGui.setVisibility(View.GONE);
                noDevices.setVisibility(View.GONE);
                discoveryDescription.setVisibility(View.GONE);

                noBluetoothLe.setVisibility(View.VISIBLE);
                restoreLayout.setVisibility(View.VISIBLE);
            } else if (result != MainActivity.NO_PERMISSIONS && result != BluetoothCommunicator.ALREADY_STARTED) {
                Toast.makeText(activity, "Ошибка начала поиска", Toast.LENGTH_SHORT).show();
            }
        } else {
            noDevices.setVisibility(View.VISIBLE);
            discoveryDescription.setVisibility(View.VISIBLE);
            restoreLayout.setVisibility(View.GONE);
            noBluetoothLe.setVisibility(View.GONE);
        }
    }

    private void stopSearch() {
        activity.stopSearch(connectingPeer == null);
    }

    private void activateInputs() {
        setListViewClickable(true, true);
    }

    private void deactivateInputs() {
        setListViewClickable(false, true);
    }

    public Peer getConfirmConnectionPeer() {
        return confirmConnectionPeer;
    }

    public RequestDialog getConnectionConfirmDialog() {
        return connectionConfirmDialog;
    }

    private void startConnectionTimer() {
        connectionTimer = new Timer(CONNECTION_TIMEOUT);
        connectionTimer.start();
    }

    private void resetConnectionTimer() {
        if (connectionTimer != null) {
            connectionTimer.cancel();
            connectionTimer = null;
        }
    }

    private void initializePeerList() {
        final PeerListAdapter.Callback callback = new PeerListAdapter.Callback() {
            @Override
            public void onFirstItemAdded() {
                super.onFirstItemAdded();
                discoveryDescription.setVisibility(View.GONE);
                noDevices.setVisibility(View.GONE);
                listViewGui.setVisibility(View.VISIBLE);
            }

            @Override
            public void onLastItemRemoved() {
                super.onLastItemRemoved();
                listViewGui.setVisibility(View.GONE);
                if (noPermissions.getVisibility() != View.VISIBLE) {
                    discoveryDescription.setVisibility(View.VISIBLE);
                    noDevices.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onClickNotAllowed(boolean showToast) {
                super.onClickNotAllowed(showToast);
                Toast.makeText(activity, "Вы уже отмечались", Toast.LENGTH_SHORT).show();
            }
        };

        listView = new PeerListAdapter(activity, new ArrayList<Peer>(), callback);
        listViewGui.setAdapter(listView);
    }

    public void clearFoundPeers() {
        if (listView != null) {
            listView.clear();
        }
    }

    public void setListViewClickable(boolean isClickable, boolean showToast) {
        if (listView != null) {
            listView.setClickable(isClickable, showToast);
        }
    }

    public void appearLoading(@Nullable CustomAnimator.EndListener responseListener) {
        if (responseListener != null) {
            listeners.add(responseListener);
        }
        isLoadingVisible = true;
        if (!isLoadingAnimating) {
            if (loading.getVisibility() != View.VISIBLE) {
                isLoadingAnimating = true;
                buttonSearch.setVisible(false, new CustomAnimator.EndListener() {
                    @Override
                    public void onAnimationEnd() {
                        int loadingSizePx = GuiTools.convertDpToPixels(activity, LOADING_SIZE_DP);
                        Animator animation = animator.createAnimatorSize(loading, 1, 1, loadingSizePx, loadingSizePx, getResources().getInteger(R.integer.durationShort));
                        animation.addListener(new Animator.AnimatorListener() {
                            @Override
                            public void onAnimationStart(Animator animation) {
                                loading.setVisibility(View.VISIBLE);
                            }

                            @Override
                            public void onAnimationEnd(Animator animation) {
                                isLoadingAnimating = false;
                                if (!isLoadingVisible) {
                                    disappearLoading(appearSearchButton, null);
                                } else {
                                    notifyLoadingAnimationEnd();
                                }
                            }

                            @Override
                            public void onAnimationCancel(Animator animation) {
                            }
                            @Override
                            public void onAnimationRepeat(Animator animation) {

                            }
                        });
                        animation.start();

                    }
                });
            } else {
                notifyLoadingAnimationEnd();
            }
        }
    }

    public void disappearLoading(final boolean appearSearchButton, @Nullable CustomAnimator.EndListener responseListener) {
        if (responseListener != null) {
            listeners.add(responseListener);
        }
        this.isLoadingVisible = false;
        this.appearSearchButton = appearSearchButton;
        if (!isLoadingAnimating) {
            if (loading.getVisibility() != View.GONE) {
                isLoadingAnimating = true;
                int loadingSizePx = GuiTools.convertDpToPixels(activity, LOADING_SIZE_DP);
                Animator animation = animator.createAnimatorSize(loading, loadingSizePx, loadingSizePx, 1, 1, getResources().getInteger(R.integer.durationShort));
                animation.addListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        loading.setVisibility(View.GONE);
                        CustomAnimator.EndListener listener = new CustomAnimator.EndListener() {
                            @Override
                            public void onAnimationEnd() {
                                isLoadingAnimating = false;
                                if (isLoadingVisible) {
                                    appearLoading(null);
                                } else {
                                    notifyLoadingAnimationEnd();
                                }
                            }
                        };
                        if (appearSearchButton) {
                            buttonSearch.setVisible(true, listener);
                        } else {
                            listener.onAnimationEnd();
                        }
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {
                    }
                });
                animation.start();
            } else {
                notifyLoadingAnimationEnd();
            }
        }
    }

    private void notifyLoadingAnimationEnd() {
        while (listeners.size() > 0) {
            listeners.remove(0).onAnimationEnd();
        }
    }

    private void showInputDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Введите фамилию и имя");
        final EditText input = new EditText(activity);
        builder.setView(input);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String text = input.getText().toString();

                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("name", text);
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
            public void onSearchStarted() {
                buttonSearch.setSearching(true, animator);
            }

            @Override
            public void onSearchStopped() {
                buttonSearch.setSearching(false, animator);
            }

            @Override
            public void onConnectionRequest(final Peer peer) {
                super.onConnectionRequest(peer);
                if (peer != null) {
                    activity.acceptConnection(peer);
                }
            }

            @Override
            public void onConnectionSuccess(Peer peer, int source) {
                super.onConnectionSuccess(peer, source);
                connectingPeer = null;
                resetConnectionTimer();

                if (global.getBluetoothCommunicator().getConnectedPeersList().size() > 0) {

                    Message message = new Message(global, deviceId, sharedPreferences.getString("name", ""),
                            global.getBluetoothCommunicator().getConnectedPeersList().get(0));

                    global.getBluetoothCommunicator().sendMessage(message);
                    Toast.makeText(activity, "Отмечен(а)", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onConnectionFailed(Peer peer, int errorCode) {
                super.onConnectionFailed(peer, errorCode);
                if (connectingPeer != null) {
                    if (connectionTimer != null && !connectionTimer.isFinished() && errorCode != BluetoothCommunicator.CONNECTION_REJECTED) {
                        activity.connect(peer);
                    } else {
                        clearFoundPeers();
                        startSearch();
                        activateInputs();
                        disappearLoading(true, null);

                        connectingPeer = null;
                        if (errorCode == BluetoothCommunicator.CONNECTION_REJECTED) {
                            Toast.makeText(activity, peer.getName() + " отказал в подключении", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(activity, "Ошибка подключения", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }

            @Override
            public void onPeerFound(Peer peer) {
                super.onPeerFound(peer);
                synchronized (lock) {
                    if (listView != null) {
                        BluetoothAdapter bluetoothAdapter = global.getBluetoothCommunicator().getBluetoothAdapter();
                        int index = listView.indexOfPeer(peer.getUniqueName());

                        if (index == -1) {
                            listView.add(peer);
                        } else {
                            Peer peer1 = listView.get(index);
                            if (peer.isBonded(bluetoothAdapter)) {
                                listView.set(index, peer);
                            } else if (peer1.isBonded(bluetoothAdapter)) {
                                listView.set(index, listView.get(index));
                            } else {
                                listView.set(index, peer);
                            }
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
            public void onPeerLost(Peer peer) {
                synchronized (lock) {
                    if (listView != null) {
                        listView.remove(peer);
                        if (peer.equals(getConfirmConnectionPeer())) {
                            RequestDialog requestDialog = getConnectionConfirmDialog();
                            if (requestDialog != null) {
                                requestDialog.cancel();
                            }
                        }
                    }
                }
            }

            @Override
            public void onBluetoothLeNotSupported() {

            }

            @Override
            public void onMissingSearchPermission() {
                super.onMissingSearchPermission();
                clearFoundPeers();
                if (noPermissions.getVisibility() != View.VISIBLE) {
                    listViewGui.setVisibility(View.GONE);
                    noDevices.setVisibility(View.GONE);
                    discoveryDescription.setVisibility(View.GONE);
                    noPermissions.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onSearchPermissionGranted() {
                super.onSearchPermissionGranted();
                if (noPermissions.getVisibility() == View.VISIBLE) {
                    noPermissions.setVisibility(View.GONE);
                    noDevices.setVisibility(View.VISIBLE);
                    discoveryDescription.setVisibility(View.VISIBLE);
                    initializePeerList();
                } else {
                    clearFoundPeers();
                }
                startSearch();
            }
        };
    }
}