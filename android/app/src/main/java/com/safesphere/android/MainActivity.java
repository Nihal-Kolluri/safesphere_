package com.safesphere.android;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.safesphere.android.hardware.FlashlightManager;
import com.safesphere.android.hardware.GpsLocationTracker;
import com.safesphere.android.hardware.SirenManager;
import com.safesphere.android.network.DispatcherHttpClient;
import com.safesphere.cv.ThreatResult;
import com.safesphere.cv.ThreatVerificationService;
import com.safesphere.event.SafeSphereEventBus;
import com.safesphere.event.StateTransitionEvent;
import com.safesphere.fsm.EmergencyStateEngine;
import com.safesphere.model.EmergencyCapsule;
import com.safesphere.model.FSMState;
import com.safesphere.model.NetworkQuality;
import com.safesphere.model.Telemetry;
import com.safesphere.survival.MeshStoreAndForward;
import com.safesphere.survival.SurvivalEngine;
import com.safesphere.survival.SurvivalProfile;

import java.util.Locale;

/**
 * SafeSphere Mobile Application Controller.
 * Comprehensive multi-tab personal safety orchestrator with real hardware sensors,
 * acoustic distress siren, optical strobe, live tactical GIS map, and 112 command hub sync.
 */
public class MainActivity extends AppCompatActivity implements SensorEventListener {

    // Core Business Engines
    private EmergencyStateEngine stateEngine;
    private SurvivalEngine survivalEngine;
    private ThreatVerificationService cvService;
    private MeshStoreAndForward meshNetwork;
    private final SafeSphereEventBus eventBus = SafeSphereEventBus.getInstance();

    // Hardware Managers
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private ConnectivityManager connectivityManager;
    private Vibrator vibrator;
    private SirenManager sirenManager;
    private FlashlightManager flashlightManager;
    private GpsLocationTracker gpsTracker;

    // View Containers (4 Tabs)
    private View rootView;
    private ScrollView tabShield;
    private LinearLayout tabMap;
    private ScrollView tabMedical;
    private ScrollView tabSync;
    private BottomNavigationView bottomNavigation;

    // Persistent Header & Banners
    private TextView tvSystemStatus;
    private TextView tvTelemetryQuick;
    private LinearLayout bannerSurvival;

    // Shield Tab Views
    private LinearLayout cardCountdown;
    private TextView tvCountdownSeconds;
    private ProgressBar progressCountdown;
    private Button btnCancelCountdown;
    private Button btnConfirmCountdown;
    private Button btnSOS;
    private Button btnToggleSiren;
    private Button btnToggleStrobe;
    private TextView tvTriageFeedback;

    // Map Tab Views
    private WebView mapWebView;
    private TextView tvJourneyStatus;

    // Sync Tab Views
    private EditText etServerHost;
    private TextView tvConnectionStatus;
    private TextView tvLiveSensorReadout;
    private TextView tvLiveBatteryReadout;
    private TextView tvLiveNetworkReadout;
    private TextView tvLiveGpsReadout;
    private TextView tvLiveMeshReadout;

    private int lastBatteryLevel = 85;
    private boolean isRouteDeviated = false;

    // Battery Broadcast Receiver
    private final BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            if (level >= 0 && scale > 0) {
                int batteryPercent = (int) ((level / (float) scale) * 100);
                lastBatteryLevel = batteryPercent;
                runOnUiThread(() -> {
                    stateEngine.getCurrentCapsule().getTelemetry().setBatteryLevel(batteryPercent);
                    survivalEngine.updateBatteryLevel(batteryPercent);
                    updateTelemetryHeader();
                });
            }
        }
    };

    private ConnectivityManager.NetworkCallback networkCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SafeSphereApp app = (SafeSphereApp) getApplication();
        this.stateEngine = app.getStateEngine();
        this.survivalEngine = app.getSurvivalEngine();
        this.cvService = app.getCvService();
        this.meshNetwork = app.getMeshNetwork();

        initHardwareManagers();
        bindViews();
        setupNavigation();
        setupInteractiveMap();
        setupShieldActions();
        setupSyncActions();
        wireEventSubscriptions();
    }

    private void initHardwareManagers() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        sirenManager = new SirenManager();
        flashlightManager = new FlashlightManager(this);
        gpsTracker = new GpsLocationTracker(this);

        gpsTracker.addLocationListener(loc -> runOnUiThread(() -> {
            if (loc != null) {
                stateEngine.getCurrentCapsule().getTelemetry().setLatitude(loc.getLatitude());
                stateEngine.getCurrentCapsule().getTelemetry().setLongitude(loc.getLongitude());
                if (mapWebView != null) {
                    mapWebView.evaluateJavascript(String.format(Locale.US,
                            "updateLocation(%.4f, %.4f, 'Live GPS Position');",
                            loc.getLatitude(), loc.getLongitude()), null);
                }
            }
        }));
    }

    private void bindViews() {
        rootView = findViewById(R.id.rootView);
        tabShield = findViewById(R.id.tabShield);
        tabMap = findViewById(R.id.tabMap);
        tabMedical = findViewById(R.id.tabMedical);
        tabSync = findViewById(R.id.tabSync);
        bottomNavigation = findViewById(R.id.bottomNavigation);

        tvSystemStatus = findViewById(R.id.tvSystemStatus);
        tvTelemetryQuick = findViewById(R.id.tvTelemetryQuick);
        bannerSurvival = findViewById(R.id.bannerSurvival);

        cardCountdown = findViewById(R.id.cardCountdown);
        tvCountdownSeconds = findViewById(R.id.tvCountdownSeconds);
        progressCountdown = findViewById(R.id.progressCountdown);
        btnCancelCountdown = findViewById(R.id.btnCancelCountdown);
        btnConfirmCountdown = findViewById(R.id.btnConfirmCountdown);
        btnSOS = findViewById(R.id.btnSOS);
        btnToggleSiren = findViewById(R.id.btnToggleSiren);
        btnToggleStrobe = findViewById(R.id.btnToggleStrobe);
        tvTriageFeedback = findViewById(R.id.tvTriageFeedback);

        mapWebView = findViewById(R.id.mapWebView);
        tvJourneyStatus = findViewById(R.id.tvJourneyStatus);

        etServerHost = findViewById(R.id.etServerHost);
        tvConnectionStatus = findViewById(R.id.tvConnectionStatus);
        tvLiveSensorReadout = findViewById(R.id.tvLiveSensorReadout);
        tvLiveBatteryReadout = findViewById(R.id.tvLiveBatteryReadout);
        tvLiveNetworkReadout = findViewById(R.id.tvLiveNetworkReadout);
        tvLiveGpsReadout = findViewById(R.id.tvLiveGpsReadout);
        tvLiveMeshReadout = findViewById(R.id.tvLiveMeshReadout);
    }

    private void setupNavigation() {
        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            tabShield.setVisibility(id == R.id.nav_shield ? View.VISIBLE : View.GONE);
            tabMap.setVisibility(id == R.id.nav_map ? View.VISIBLE : View.GONE);
            tabMedical.setVisibility(id == R.id.nav_medical ? View.VISIBLE : View.GONE);
            tabSync.setVisibility(id == R.id.nav_sync ? View.VISIBLE : View.GONE);
            return true;
        });
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupInteractiveMap() {
        WebSettings settings = mapWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        mapWebView.setWebViewClient(new WebViewClient());
        mapWebView.loadUrl("file:///android_asset/leaflet_map.html");

        findViewById(R.id.btnCenterLocation).setOnClickListener(v -> {
            double lat = stateEngine.getCurrentCapsule().getTelemetry().getLatitude();
            double lon = stateEngine.getCurrentCapsule().getTelemetry().getLongitude();
            mapWebView.evaluateJavascript(String.format(Locale.US,
                    "updateLocation(%.4f, %.4f, 'Center Pin');", lat, lon), null);
            Toast.makeText(this, "Map centered on current location", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.btnSimulateDeviation).setOnClickListener(v -> {
            isRouteDeviated = !isRouteDeviated;
            stateEngine.getCurrentCapsule().getTelemetry().setRouteDeviated(isRouteDeviated);
            if (isRouteDeviated) {
                mapWebView.evaluateJavascript("setRouteDeviation(true, 17.3895, 78.4930);", null);
                tvJourneyStatus.setText("⚠ CRITICAL ROUTE DEVIATION (+420m off corridor)");
                tvJourneyStatus.setTextColor(getResources().getColor(R.color.primary_red));
                stateEngine.evaluateSensorSpike(stateEngine.getCurrentCapsule().getTelemetry(),
                        "Safe Corridor Route Deviation Anomaly (+420m)");
                triggerVibration(new long[]{0, 300, 100, 300});
            } else {
                mapWebView.evaluateJavascript("setRouteDeviation(false);", null);
                tvJourneyStatus.setText("Safe Corridor Active · Corridor: Cyber Towers → Jubilee Hills");
                tvJourneyStatus.setTextColor(getResources().getColor(R.color.accent_green));
            }
        });
    }

    private void setupShieldActions() {
        btnSOS.setOnClickListener(v -> {
            triggerVibration(new long[]{0, 250, 100, 250});
            stateEngine.triggerSOS();
            dispatchCapsuleToServer("MANUAL_SOS");
        });

        btnCancelCountdown.setOnClickListener(v -> stateEngine.cancelFalseAlarm());
        btnConfirmCountdown.setOnClickListener(v -> {
            stateEngine.confirmEmergencyImmediately();
            dispatchCapsuleToServer("CONFIRMED_SOS");
        });

        // Acoustic Siren Toggle
        btnToggleSiren.setOnClickListener(v -> {
            if (sirenManager.isPlaying()) {
                sirenManager.stopSiren();
                btnToggleSiren.setText("🔊 Loud Siren: OFF");
                btnToggleSiren.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#1E293B")));
            } else {
                sirenManager.startSiren();
                btnToggleSiren.setText("🔊 Siren Active: ON");
                btnToggleSiren.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#DC2626")));
            }
        });

        // Flashlight Strobe Toggle
        btnToggleStrobe.setOnClickListener(v -> {
            if (flashlightManager.isStrobing()) {
                flashlightManager.stopStrobe();
                btnToggleStrobe.setText("⚡ Strobe Light: OFF");
                btnToggleStrobe.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#1E293B")));
            } else {
                flashlightManager.startStrobe();
                btnToggleStrobe.setText("⚡ Strobe Active: ON");
                btnToggleStrobe.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#DC2626")));
            }
        });

        // Emergency Speed Dialers
        findViewById(R.id.btnCallPolice).setOnClickListener(v -> dialNumber("112"));
        findViewById(R.id.btnCallAmbulance).setOnClickListener(v -> dialNumber("108"));
        findViewById(R.id.btnCallFire).setOnClickListener(v -> dialNumber("101"));

        // Silent Triage
        findViewById(R.id.btnInjuredYes).setOnClickListener(v -> updateTriage("Injured: YES"));
        findViewById(R.id.btnInjuredNo).setOnClickListener(v -> updateTriage("Injured: NO"));
        findViewById(R.id.btnThreatYes).setOnClickListener(v -> updateTriage("Threat Nearby: YES"));
        findViewById(R.id.btnThreatNo).setOnClickListener(v -> updateTriage("Threat Nearby: NO"));

        // SMS Simulation
        findViewById(R.id.btnSimulateSms).setOnClickListener(v -> {
            String msg = String.format(Locale.US,
                    "EMERGENCY ALERT: SafeSphere detected crisis at https://maps.google.com/?q=%.4f,%.4f. Immediate assistance needed!",
                    stateEngine.getCurrentCapsule().getTelemetry().getLatitude(),
                    stateEngine.getCurrentCapsule().getTelemetry().getLongitude());
            Toast.makeText(this, "Simulated SMS broadcast sent to 3 trusted contacts:\n" + msg, Toast.LENGTH_LONG).show();
        });
    }

    private void setupSyncActions() {
        findViewById(R.id.btnTestConnection).setOnClickListener(v -> {
            String host = etServerHost.getText().toString().trim();
            if (!host.isEmpty()) {
                DispatcherHttpClient.setServerHost(host);
            }
            tvConnectionStatus.setText("Pinging 112 command hub...");
            DispatcherHttpClient.testConnectionAsync().thenAccept(success -> runOnUiThread(() -> {
                if (success) {
                    tvConnectionStatus.setText("● CONNECTED · 112 Dispatch Hub Online (HTTP 200)");
                    tvConnectionStatus.setTextColor(getResources().getColor(R.color.accent_green));
                    Toast.makeText(this, "Connected to SafeSphere 112 Command Desk!", Toast.LENGTH_SHORT).show();
                } else {
                    tvConnectionStatus.setText("✕ UNREACHABLE · Check IP / Port");
                    tvConnectionStatus.setTextColor(getResources().getColor(R.color.primary_red));
                }
            }));
        });

        findViewById(R.id.btnSimulateCrash).setOnClickListener(v -> {
            Telemetry tel = stateEngine.getCurrentCapsule().getTelemetry();
            tel.setCrashImpactG(8.6);
            stateEngine.evaluateSensorSpike(tel, "Severe Deceleration Crash Spike (8.6G)");
            ThreatResult result = cvService.verifyThreat("Severe vehicle collision");
            stateEngine.evaluateThreatResult(result);
            dispatchCapsuleToServer("CRASH_ACCELEROMETER_8.6G");
            triggerVibration(new long[]{0, 500, 100, 500});
        });

        findViewById(R.id.btnSimulateThreat).setOnClickListener(v -> {
            if (stateEngine.getCurrentState() == FSMState.SAFE) {
                stateEngine.triggerSOS();
            }
            ThreatResult result = cvService.verifyThreat("optical fire and weapon detected");
            stateEngine.evaluateThreatResult(result);
            dispatchCapsuleToServer("CV_HAZARD_VERIFIED");
        });

        findViewById(R.id.btnSimulateLowBattery).setOnClickListener(v -> {
            survivalEngine.updateBatteryLevel(12);
        });
    }

    private void dispatchCapsuleToServer(String triggerHint) {
        String host = etServerHost.getText().toString().trim();
        if (!host.isEmpty()) {
            DispatcherHttpClient.setServerHost(host);
        }
        EmergencyCapsule capsule = stateEngine.getCurrentCapsule();
        DispatcherHttpClient.dispatchCapsuleAsync(capsule, triggerHint).thenAccept(res -> runOnUiThread(() -> {
            tvLiveMeshReadout.setText("Network: " + res);
        }));
    }

    private void dialNumber(String number) {
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:" + number));
        startActivity(intent);
    }

    private void updateTriage(String answer) {
        stateEngine.getCurrentCapsule().setQuestionnaireStatus(answer);
        tvTriageFeedback.setText("Triage updated: " + answer);
        dispatchCapsuleToServer("TRIAGE_" + answer);
        Toast.makeText(this, "Packaged into capsule: " + answer, Toast.LENGTH_SHORT).show();
    }

    private void wireEventSubscriptions() {
        stateEngine.addCountdownListener(seconds -> runOnUiThread(() -> {
            tvCountdownSeconds.setText(String.format(Locale.getDefault(), "%ds", seconds));
            progressCountdown.setProgress(seconds);
        }));

        eventBus.subscribe(StateTransitionEvent.class, event -> runOnUiThread(() -> {
            updateFsmUIState(event.getToState());
        }));

        survivalEngine.addProfileListener(profile -> runOnUiThread(() -> {
            applySurvivalProfileUI(profile);
        }));
    }

    private void updateFsmUIState(FSMState state) {
        switch (state) {
            case SAFE -> {
                tvSystemStatus.setText("● SYSTEM SAFE · MONITORING");
                tvSystemStatus.setTextColor(getResources().getColor(R.color.accent_green));
                cardCountdown.setVisibility(View.GONE);
                btnSOS.setEnabled(true);
            }
            case SUSPICIOUS, CHECKING -> {
                tvSystemStatus.setText("● " + state.name() + " · SILENT WINDOW");
                tvSystemStatus.setTextColor(getResources().getColor(R.color.accent_amber));
                cardCountdown.setVisibility(View.VISIBLE);
            }
            case EMERGENCY -> {
                tvSystemStatus.setText("● ACTIVE EMERGENCY · BROADCASTING");
                tvSystemStatus.setTextColor(getResources().getColor(R.color.primary_red));
                cardCountdown.setVisibility(View.GONE);
                triggerVibration(new long[]{0, 500, 200, 500});
            }
            case ESCALATING -> {
                tvSystemStatus.setText("● ESCALATING TO 112 ERSS");
                tvSystemStatus.setTextColor(getResources().getColor(R.color.primary_red));
            }
            case RESPONDER_ASSIGNED -> {
                tvSystemStatus.setText("● RESPONDER EN ROUTE (" + stateEngine.getCurrentCapsule().getAssignedResponderId() + ")");
                tvSystemStatus.setTextColor(getResources().getColor(R.color.accent_blue));
            }
            case RESOLVED -> {
                tvSystemStatus.setText("● INCIDENT RESOLVED");
                tvSystemStatus.setTextColor(getResources().getColor(R.color.accent_green));
                cardCountdown.setVisibility(View.GONE);
            }
        }
    }

    private void applySurvivalProfileUI(SurvivalProfile profile) {
        boolean extreme = profile.isExtremeSurvivalActive();
        updateTelemetryHeader();
        gpsTracker.updateInterval(profile.getGpsPollingIntervalSeconds() * 1000L);

        tvLiveBatteryReadout.setText(String.format(Locale.getDefault(),
                "Battery: %d%% (%s Profile)", survivalEngine.getCurrentBatteryPercent(), profile.getName()));
        tvLiveGpsReadout.setText(String.format(Locale.getDefault(),
                "GPS Interval: %ds (%s)", profile.getGpsPollingIntervalSeconds(), profile.getEvidenceCaptureMode()));

        if (extreme) {
            // OLED zero-emission pitch black
            rootView.setBackgroundColor(Color.BLACK);
            bannerSurvival.setVisibility(View.VISIBLE);
            findViewById(R.id.cardQuestionnaire).setVisibility(View.GONE);
            findViewById(R.id.speedDialBar).setVisibility(View.GONE);

            WindowManager.LayoutParams params = getWindow().getAttributes();
            params.screenBrightness = 0.05f;
            getWindow().setAttributes(params);
        } else {
            rootView.setBackgroundColor(getResources().getColor(R.color.bg_dark));
            bannerSurvival.setVisibility(View.GONE);
            findViewById(R.id.cardQuestionnaire).setVisibility(View.VISIBLE);
            findViewById(R.id.speedDialBar).setVisibility(View.VISIBLE);

            WindowManager.LayoutParams params = getWindow().getAttributes();
            params.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
            getWindow().setAttributes(params);
        }
    }

    private void updateTelemetryHeader() {
        NetworkQuality nq = stateEngine.getCurrentCapsule().getTelemetry().getNetworkQuality();
        tvTelemetryQuick.setText(String.format(Locale.getDefault(),
                "⚡ %d%%  📶 %s", lastBatteryLevel, nq != null ? nq.name() : "OK"));
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            double gForce = Math.sqrt(x * x + y * y + z * z) / SensorManager.GRAVITY_EARTH;

            if (tvLiveSensorReadout != null) {
                tvLiveSensorReadout.setText(String.format(Locale.getDefault(),
                        "Accelerometer: %.2fG (Live Sensor)", gForce));
            }

            // Real physical high-G spike crash detection
            if (gForce > 4.5 && stateEngine.getCurrentState() == FSMState.SAFE) {
                Telemetry tel = stateEngine.getCurrentCapsule().getTelemetry();
                tel.setCrashImpactG(gForce);
                stateEngine.evaluateSensorSpike(tel, String.format(Locale.getDefault(),
                        "Live Accelerometer Crash Spike (%.1fG)", gForce));
                ThreatResult result = cvService.verifyThreat("Severe vehicular crash collision");
                stateEngine.evaluateThreatResult(result);
                dispatchCapsuleToServer("LIVE_ACCELEROMETER_IMPACT_" + String.format(Locale.US, "%.1fG", gForce));
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    private void triggerVibration(long[] pattern) {
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1));
            } else {
                vibrator.vibrate(pattern, -1);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(batteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        if (sensorManager != null && accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        }

        gpsTracker.startTracking(survivalEngine.getCurrentProfile().getGpsPollingIntervalSeconds() * 1000L);

        if (connectivityManager != null) {
            NetworkRequest request = new NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build();
            networkCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(@NonNull Network network) {
                    runOnUiThread(() -> {
                        stateEngine.getCurrentCapsule().getTelemetry().setNetworkQuality(NetworkQuality.STRONG);
                        meshNetwork.onNetworkChange(false);
                        tvLiveNetworkReadout.setText("Network: CONNECTED (High-Speed Link)");
                        updateTelemetryHeader();
                    });
                }

                @Override
                public void onLost(@NonNull Network network) {
                    runOnUiThread(() -> {
                        stateEngine.getCurrentCapsule().getTelemetry().setNetworkQuality(NetworkQuality.OFFLINE);
                        meshNetwork.onNetworkChange(true);
                        tvLiveNetworkReadout.setText("Network: OFFLINE (Store-and-Forward Mesh Active)");
                        updateTelemetryHeader();
                    });
                }
            };
            connectivityManager.registerNetworkCallback(request, networkCallback);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            unregisterReceiver(batteryReceiver);
        } catch (Exception ignored) {}

        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }

        gpsTracker.stopTracking();

        if (sirenManager != null && sirenManager.isPlaying()) {
            sirenManager.stopSiren();
        }

        if (flashlightManager != null && flashlightManager.isStrobing()) {
            flashlightManager.stopStrobe();
        }

        if (connectivityManager != null && networkCallback != null) {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback);
            } catch (Exception ignored) {}
        }
    }
}
