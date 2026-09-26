package com.safesphere.android;

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
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.safesphere.cv.ThreatResult;
import com.safesphere.cv.ThreatVerificationService;
import com.safesphere.event.CapsuleUpdatedEvent;
import com.safesphere.event.SafeSphereEventBus;
import com.safesphere.event.StateTransitionEvent;
import com.safesphere.event.SurvivalModeChangedEvent;
import com.safesphere.fsm.EmergencyStateEngine;
import com.safesphere.model.FSMState;
import com.safesphere.model.NetworkQuality;
import com.safesphere.model.Telemetry;
import com.safesphere.survival.MeshStoreAndForward;
import com.safesphere.survival.SurvivalEngine;
import com.safesphere.survival.SurvivalProfile;

import java.util.Locale;

/**
 * SafeSphere Android Native Client.
 * Hooks directly into real Android OS hardware:
 * - BatteryManager (Survival Mode dynamic decay)
 * - SensorManager Accelerometer (real high-G crash impact detection)
 * - ConnectivityManager (real network link monitoring & Store-and-Forward Mesh)
 * - OLED Zero-Emission Extreme Survival Mode
 */
public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private EmergencyStateEngine stateEngine;
    private SurvivalEngine survivalEngine;
    private ThreatVerificationService cvService;
    private MeshStoreAndForward meshNetwork;
    private final SafeSphereEventBus eventBus = SafeSphereEventBus.getInstance();

    // Android OS Hardware Managers
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private ConnectivityManager connectivityManager;
    private Vibrator vibrator;

    // UI Components
    private ScrollView rootScrollView;
    private LinearLayout mainContainer;
    private TextView tvSystemStatus;
    private TextView tvTelemetryQuick;
    private LinearLayout bannerSurvival;
    private LinearLayout cardCountdown;
    private TextView tvCountdownSeconds;
    private ProgressBar progressCountdown;
    private Button btnCancelCountdown;
    private Button btnConfirmCountdown;
    private Button btnSOS;
    private LinearLayout cardQuestionnaire;
    private TextView tvTriageFeedback;
    private LinearLayout cardHardware;
    private TextView tvLiveSensorReadout;
    private TextView tvLiveBatteryReadout;
    private TextView tvLiveNetworkReadout;
    private TextView tvLiveGpsReadout;

    private int lastBatteryLevel = 85;
    private boolean isExtremeSurvivalActive = false;

    // BroadcastReceiver for Live Battery Telemetry
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

    // Network Callback for Live Connectivity Monitoring
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

        initHardwareSensors();
        bindViews();
        setupListeners();
        wireEventSubscriptions();
    }

    private void initHardwareSensors() {
        // Accelerometer for Real Crash Detection
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }

        // Connectivity Manager for Carrier/Wi-Fi Monitoring
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        // Vibrator for Haptic Emergency Alerts
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
    }

    private void bindViews() {
        rootScrollView = findViewById(R.id.rootScrollView);
        mainContainer = findViewById(R.id.mainContainer);
        tvSystemStatus = findViewById(R.id.tvSystemStatus);
        tvTelemetryQuick = findViewById(R.id.tvTelemetryQuick);
        bannerSurvival = findViewById(R.id.bannerSurvival);
        cardCountdown = findViewById(R.id.cardCountdown);
        tvCountdownSeconds = findViewById(R.id.tvCountdownSeconds);
        progressCountdown = findViewById(R.id.progressCountdown);
        btnCancelCountdown = findViewById(R.id.btnCancelCountdown);
        btnConfirmCountdown = findViewById(R.id.btnConfirmCountdown);
        btnSOS = findViewById(R.id.btnSOS);
        cardQuestionnaire = findViewById(R.id.cardQuestionnaire);
        tvTriageFeedback = findViewById(R.id.tvTriageFeedback);
        cardHardware = findViewById(R.id.cardHardware);
        tvLiveSensorReadout = findViewById(R.id.tvLiveSensorReadout);
        tvLiveBatteryReadout = findViewById(R.id.tvLiveBatteryReadout);
        tvLiveNetworkReadout = findViewById(R.id.tvLiveNetworkReadout);
        tvLiveGpsReadout = findViewById(R.id.tvLiveGpsReadout);
    }

    private void setupListeners() {
        // Primary SOS Button
        btnSOS.setOnClickListener(v -> {
            triggerVibration(new long[]{0, 200, 100, 200});
            stateEngine.triggerSOS();
        });

        // Silent Confirmation Buttons
        btnCancelCountdown.setOnClickListener(v -> stateEngine.cancelFalseAlarm());
        btnConfirmCountdown.setOnClickListener(v -> stateEngine.confirmEmergencyImmediately());

        // "I Can't Speak" Questionnaire Buttons
        findViewById(R.id.btnInjuredYes).setOnClickListener(v -> updateTriage("Injured: YES"));
        findViewById(R.id.btnInjuredNo).setOnClickListener(v -> updateTriage("Injured: NO"));
        findViewById(R.id.btnThreatYes).setOnClickListener(v -> updateTriage("Threat Nearby: YES"));
        findViewById(R.id.btnThreatNo).setOnClickListener(v -> updateTriage("Threat Nearby: NO"));

        // Diagnostic / Test Triggers
        findViewById(R.id.btnSimulateCrash).setOnClickListener(v -> {
            Toast.makeText(this, "Simulating Severe 8.6G Crash Spike...", Toast.LENGTH_SHORT).show();
            triggerVibration(new long[]{0, 400, 100, 400});
            Telemetry tel = stateEngine.getCurrentCapsule().getTelemetry();
            tel.setCrashImpactG(8.6);
            stateEngine.evaluateSensorSpike(tel, "Simulated Severe Deceleration Crash Spike (8.6G)");
            ThreatResult result = cvService.verifyThreat("Severe vehicle crash impact");
            stateEngine.evaluateThreatResult(result);
        });

        findViewById(R.id.btnSimulateThreat).setOnClickListener(v -> {
            Toast.makeText(this, "Simulating YOLOv8 Weapon/Fire Detection...", Toast.LENGTH_SHORT).show();
            if (stateEngine.getCurrentState() == FSMState.SAFE) {
                stateEngine.triggerSOS();
            }
            ThreatResult result = cvService.verifyThreat("optical fire and weapon detected");
            stateEngine.evaluateThreatResult(result);
        });

        findViewById(R.id.btnSimulateLowBattery).setOnClickListener(v -> {
            Toast.makeText(this, "Simulating Critical 12% Battery (Survival Mode)...", Toast.LENGTH_SHORT).show();
            survivalEngine.updateBatteryLevel(12);
        });
    }

    private void wireEventSubscriptions() {
        // Countdown timer updates
        stateEngine.addCountdownListener(seconds -> runOnUiThread(() -> {
            tvCountdownSeconds.setText(String.format(Locale.getDefault(), "%ds", seconds));
            progressCountdown.setProgress(seconds);
        }));

        // FSM State transitions
        eventBus.subscribe(StateTransitionEvent.class, event -> runOnUiThread(() -> {
            updateFsmUIState(event.getToState());
        }));

        // Survival Mode profile changes
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
                triggerVibration(new long[]{0, 500, 200, 500, 200, 500});
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

    /**
     * Requirement: If battery drops below 15%, engage Extreme Survival Mode.
     * Snaps Activity background to Pitch Black (OLED conservation), lowers brightness,
     * hides non-essential UI views, and throttles GPS to 45s heartbeat.
     */
    private void applySurvivalProfileUI(SurvivalProfile profile) {
        boolean extreme = profile.isExtremeSurvivalActive();
        this.isExtremeSurvivalActive = extreme;

        updateTelemetryHeader();
        tvLiveBatteryReadout.setText(String.format(Locale.getDefault(),
                "Battery: %d%% (%s Profile)", survivalEngine.getCurrentBatteryPercent(), profile.getName()));
        tvLiveGpsReadout.setText(String.format(Locale.getDefault(),
                "GPS Interval: %ds (%s)", profile.getGpsPollingIntervalSeconds(), profile.getEvidenceCaptureMode()));

        if (extreme) {
            // Apply Pitch Black (OLED zero-emission)
            rootScrollView.setBackgroundColor(Color.BLACK);
            mainContainer.setBackgroundColor(Color.BLACK);
            bannerSurvival.setVisibility(View.VISIBLE);

            // Hide non-essential triage & simulation cards to conserve render power
            cardQuestionnaire.setVisibility(View.GONE);
            cardHardware.setVisibility(View.GONE);

            // Dim screen brightness to save OLED power
            WindowManager.LayoutParams params = getWindow().getAttributes();
            params.screenBrightness = 0.05f; // lowest readable brightness
            getWindow().setAttributes(params);
        } else {
            // Restore standard UI
            rootScrollView.setBackgroundColor(getResources().getColor(R.color.bg_dark));
            mainContainer.setBackgroundColor(getResources().getColor(R.color.bg_dark));
            bannerSurvival.setVisibility(View.GONE);

            cardQuestionnaire.setVisibility(View.VISIBLE);
            cardHardware.setVisibility(View.VISIBLE);

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

    private void updateTriage(String answer) {
        stateEngine.getCurrentCapsule().setQuestionnaireStatus(answer);
        tvTriageFeedback.setText("Triage updated: " + answer);
        Toast.makeText(this, "Packaged into capsule: " + answer, Toast.LENGTH_SHORT).show();
    }

    // ---------------------------------------------------------
    // Real Android Hardware Sensor Listeners
    // ---------------------------------------------------------

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            // Compute total G-force magnitude: sqrt(x^2 + y^2 + z^2) / g
            double gForce = Math.sqrt(x * x + y * y + z * z) / SensorManager.GRAVITY_EARTH;

            if (tvLiveSensorReadout != null) {
                tvLiveSensorReadout.setText(String.format(Locale.getDefault(),
                        "Accelerometer: %.2fG (Live OS Sensor)", gForce));
            }

            // Real Crash Detection Trigger: If impact > 4.5G, trigger sensor anomaly
            if (gForce > 4.5 && stateEngine.getCurrentState() == FSMState.SAFE) {
                Telemetry tel = stateEngine.getCurrentCapsule().getTelemetry();
                tel.setCrashImpactG(gForce);
                stateEngine.evaluateSensorSpike(tel, String.format(Locale.getDefault(),
                        "Live Accelerometer Crash Spike (%.1fG)", gForce));
                ThreatResult result = cvService.verifyThreat("Severe vehicular crash impact");
                stateEngine.evaluateThreatResult(result);
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
        // Register Live Battery Receiver
        registerReceiver(batteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        // Register Live Accelerometer
        if (sensorManager != null && accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        }

        // Register Network Callback
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

        if (connectivityManager != null && networkCallback != null) {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback);
            } catch (Exception ignored) {}
        }
    }
}
