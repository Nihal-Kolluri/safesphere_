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
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
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
 * Comprehensive 12-Screen Personal Safety & Emergency Response Orchestrator.
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

    // Root & Navigation
    private View rootView;
    private BottomNavigationView bottomNavigation;
    private Button[] screenChips = new Button[12];
    private View[] screenViews = new View[12];
    private int currentScreenIndex = 2; // Default to Screen 2 (Home Dashboard)
    private boolean isNavigatingProgrammatically = false;

    // Screen 3 Trigger Countdown
    private CountDownTimer triggerCountdownTimer;
    private TextView tvTriggerCountdownNum;

    // Screen 5 Incident Active Timer
    private Handler incidentHandler = new Handler(Looper.getMainLooper());
    private int incidentElapsedSeconds = 154; // Start near 00:02:34 for visual parity
    private TextView tvIncidentActiveTimer;
    private final Runnable incidentTimerRunnable = new Runnable() {
        @Override
        public void run() {
            incidentElapsedSeconds++;
            int mins = incidentElapsedSeconds / 60;
            int secs = incidentElapsedSeconds % 60;
            if (tvIncidentActiveTimer != null) {
                tvIncidentActiveTimer.setText(String.format(Locale.US, "%02d:%02d:%02d", 0, mins, secs));
            }
            incidentHandler.postDelayed(this, 1000);
        }
    };

    // Screen 7 Crash Alert Countdown
    private CountDownTimer crashCountdownTimer;
    private TextView tvCrashCountdown;
    private ProgressBar progressCrashCountdown;

    // Battery & Network
    private int lastBatteryLevel = 85;
    private final BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            if (level >= 0 && scale > 0) {
                lastBatteryLevel = (int) ((level / (float) scale) * 100);
                runOnUiThread(() -> {
                    stateEngine.getCurrentCapsule().getTelemetry().setBatteryLevel(lastBatteryLevel);
                    survivalEngine.updateBatteryLevel(lastBatteryLevel);
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
        setupScreenSwitcherChips();
        setupBottomNavigation();
        setupScreen1Onboarding();
        setupScreen2Home();
        setupScreen3Trigger();
        setupScreen4Types();
        setupScreen5Incident();
        setupScreen6Contacts();
        setupScreen7CrashAlert();
        setupScreen8Evidence();
        setupScreen9Responder();
        setupScreen11Summary();
        setupScreen12Settings();
        wireEventSubscriptions();

        // Show Screen 2 (Home Dashboard) by default
        showScreen(2);
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
            }
        }));
    }

    private void bindViews() {
        rootView = findViewById(R.id.rootView);
        bottomNavigation = findViewById(R.id.bottomNavigation);

        // Screen Views 1 to 12
        screenViews[0] = findViewById(R.id.viewScreen1Onboarding);
        screenViews[1] = findViewById(R.id.viewScreen2Home);
        screenViews[2] = findViewById(R.id.viewScreen3Trigger);
        screenViews[3] = findViewById(R.id.viewScreen4Types);
        screenViews[4] = findViewById(R.id.viewScreen5Incident);
        screenViews[5] = findViewById(R.id.viewScreen6Contacts);
        screenViews[6] = findViewById(R.id.viewScreen7CrashAlert);
        screenViews[7] = findViewById(R.id.viewScreen8Evidence);
        screenViews[8] = findViewById(R.id.viewScreen9Responder);
        screenViews[9] = findViewById(R.id.viewScreen10Timeline);
        screenViews[10] = findViewById(R.id.viewScreen11Summary);
        screenViews[11] = findViewById(R.id.viewScreen12Settings);

        // Top Switcher Chips 1 to 12
        screenChips[0] = findViewById(R.id.chipScreen1);
        screenChips[1] = findViewById(R.id.chipScreen2);
        screenChips[2] = findViewById(R.id.chipScreen3);
        screenChips[3] = findViewById(R.id.chipScreen4);
        screenChips[4] = findViewById(R.id.chipScreen5);
        screenChips[5] = findViewById(R.id.chipScreen6);
        screenChips[6] = findViewById(R.id.chipScreen7);
        screenChips[7] = findViewById(R.id.chipScreen8);
        screenChips[8] = findViewById(R.id.chipScreen9);
        screenChips[9] = findViewById(R.id.chipScreen10);
        screenChips[10] = findViewById(R.id.chipScreen11);
        screenChips[11] = findViewById(R.id.chipScreen12);
    }

    /**
     * Central screen switcher displaying exactly 1 screen out of the 12,
     * updating chip button highlights and bottom navigation state.
     */
    public void showScreen(int screenNumber) {
        currentScreenIndex = screenNumber;
        for (int i = 0; i < 12; i++) {
            if (screenViews[i] != null) {
                screenViews[i].setVisibility(i == (screenNumber - 1) ? View.VISIBLE : View.GONE);
            }
            if (screenChips[i] != null) {
                if (i == (screenNumber - 1)) {
                    screenChips[i].setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#4F46E5")));
                } else {
                    screenChips[i].setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#334155")));
                }
            }
        }

        // Handle Bottom Navigation visibility and selection
        if (bottomNavigation != null) {
            bottomNavigation.setVisibility(View.VISIBLE);
            int targetNavId = -1;
            if (screenNumber == 2) {
                targetNavId = R.id.nav_home;
            } else if (screenNumber == 6) {
                targetNavId = R.id.nav_contacts;
            } else if (screenNumber == 10) {
                targetNavId = R.id.nav_timeline;
            } else if (screenNumber == 12) {
                targetNavId = R.id.nav_more;
            }

            if (targetNavId != -1 && bottomNavigation.getSelectedItemId() != targetNavId) {
                isNavigatingProgrammatically = true;
                try {
                    bottomNavigation.setSelectedItemId(targetNavId);
                } finally {
                    isNavigatingProgrammatically = false;
                }
            }
        }
    }

    private void setupScreenSwitcherChips() {
        for (int i = 0; i < 12; i++) {
            final int screenNum = i + 1;
            if (screenChips[i] != null) {
                screenChips[i].setOnClickListener(v -> showScreen(screenNum));
            }
        }
    }

    private void setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener(item -> {
            if (isNavigatingProgrammatically) {
                return true;
            }
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                showScreen(2);
                return true;
            } else if (id == R.id.nav_contacts) {
                showScreen(6);
                return true;
            } else if (id == R.id.nav_timeline) {
                showScreen(10);
                return true;
            } else if (id == R.id.nav_more) {
                showScreen(12);
                return true;
            }
            return false;
        });
    }

    // ==========================================
    // SCREEN 1: WELCOME / ONBOARDING
    // ==========================================
    private void setupScreen1Onboarding() {
        findViewById(R.id.btnGetStarted).setOnClickListener(v -> {
            Toast.makeText(this, "Welcome to SafeSphere!", Toast.LENGTH_SHORT).show();
            showScreen(2); // Jump to Home Dashboard
        });

        findViewById(R.id.tvAlreadyAccount).setOnClickListener(v -> showScreen(2));
    }

    // ==========================================
    // SCREEN 2: HOME DASHBOARD
    // ==========================================
    private void setupScreen2Home() {
        // SafeSphere Title / Logo: click opens Welcome screen
        findViewById(R.id.tvHomeAppTitle).setOnClickListener(v -> showScreen(1));

        // Bell Icon: opens Crash Alert simulation or notification
        findViewById(R.id.btnNotificationBell).setOnClickListener(v -> {
            Toast.makeText(this, "System Alert: All sensors operational", Toast.LENGTH_SHORT).show();
        });

        // Large Red SOS Pill -> transitions to Screen 3 (Emergency Trigger)
        findViewById(R.id.btnTriggerSosPill).setOnClickListener(v -> {
            triggerVibration(new long[]{0, 200, 100, 200});
            stateEngine.triggerSOS();
            showScreen(3);
            startTriggerCountdown();
        });

        // Safety Check Pill
        findViewById(R.id.btnSafetyCheckPill).setOnClickListener(v -> {
            triggerVibration(new long[]{0, 100});
            Toast.makeText(this, "Safety Check-in active: 30-min auto check scheduled", Toast.LENGTH_SHORT).show();
        });

        // Share Live Location Pill
        findViewById(R.id.btnShareLocationPill).setOnClickListener(v -> {
            triggerVibration(new long[]{0, 100});
            Toast.makeText(this, "Live GPS Location broadcast enabled for trusted contacts", Toast.LENGTH_SHORT).show();
        });
    }

    // ==========================================
    // SCREEN 3: EMERGENCY TRIGGER
    // ==========================================
    private void setupScreen3Trigger() {
        tvTriggerCountdownNum = findViewById(R.id.tvTriggerCountdownNum);

        // Center Pulsing Circle button
        findViewById(R.id.btnPulseCenter).setOnClickListener(v -> {
            if (triggerCountdownTimer != null) triggerCountdownTimer.cancel();
            showScreen(4); // Advance to Emergency Types
        });

        // Cancel circular button
        findViewById(R.id.btnCancelEmergencyTrigger).setOnClickListener(v -> {
            if (triggerCountdownTimer != null) triggerCountdownTimer.cancel();
            stateEngine.cancelFalseAlarm();
            Toast.makeText(this, "Emergency trigger cancelled", Toast.LENGTH_SHORT).show();
            showScreen(2); // Return to Home
        });
    }

    private void startTriggerCountdown() {
        if (triggerCountdownTimer != null) triggerCountdownTimer.cancel();
        triggerCountdownTimer = new CountDownTimer(3500, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int secs = (int) (millisUntilFinished / 1000);
                if (tvTriggerCountdownNum != null) {
                    tvTriggerCountdownNum.setText(String.valueOf(Math.max(1, secs)));
                }
                triggerVibration(new long[]{0, 100});
            }

            @Override
            public void onFinish() {
                if (tvTriggerCountdownNum != null) tvTriggerCountdownNum.setText("0");
                showScreen(4); // Proceed to Emergency Type Selection
            }
        }.start();
    }

    // ==========================================
    // SCREEN 4: EMERGENCY TYPE SELECTION
    // ==========================================
    private void setupScreen4Types() {
        View cardMedical = findViewById(R.id.cardTypeMedical);
        View cardAccident = findViewById(R.id.cardTypeAccident);
        View cardThreat = findViewById(R.id.cardTypeThreat);
        View cardFire = findViewById(R.id.cardTypeFire);
        View cardDisaster = findViewById(R.id.cardTypeDisaster);
        View cardOther = findViewById(R.id.cardTypeOther);

        View.OnClickListener cardClickListener = v -> {
            resetTypeCards(cardMedical, cardAccident, cardThreat, cardFire, cardDisaster, cardOther);
            v.setBackgroundResource(R.drawable.bg_type_card);
            triggerVibration(new long[]{0, 80});
        };

        if (cardMedical != null) cardMedical.setOnClickListener(cardClickListener);
        if (cardAccident != null) cardAccident.setOnClickListener(cardClickListener);
        if (cardThreat != null) cardThreat.setOnClickListener(cardClickListener);
        if (cardFire != null) cardFire.setOnClickListener(cardClickListener);
        if (cardDisaster != null) cardDisaster.setOnClickListener(cardClickListener);
        if (cardOther != null) cardOther.setOnClickListener(cardClickListener);

        // Continue Button -> advances to Screen 5 (Live Incident)
        findViewById(R.id.btnContinueTypeSelection).setOnClickListener(v -> {
            stateEngine.confirmEmergencyImmediately();
            dispatchCapsuleToServer("CONFIRMED_SOS");
            showScreen(5);
            startIncidentTimer();
        });
    }

    private void resetTypeCards(View... cards) {
        for (View card : cards) {
            if (card != null) {
                card.setBackgroundResource(R.drawable.bg_card_white);
            }
        }
    }

    // ==========================================
    // SCREEN 5: LIVE INCIDENT SCREEN
    // ==========================================
    private void setupScreen5Incident() {
        tvIncidentActiveTimer = findViewById(R.id.tvIncidentActiveTimer);

        // End Button -> advances to Screen 11 (Post-Incident Summary)
        findViewById(R.id.btnEndIncident).setOnClickListener(v -> {
            incidentHandler.removeCallbacks(incidentTimerRunnable);
            if (stateEngine.getCurrentState() != FSMState.SAFE && stateEngine.getCurrentState() != FSMState.RESOLVED) {
                try {
                    stateEngine.transitionTo(FSMState.RESOLVED, "Incident resolved by user");
                } catch (Exception ignored) {}
            }
            showScreen(11);
        });

        // View All updates -> jumps to Screen 10 (Live Timeline)
        findViewById(R.id.tvViewAllUpdates).setOnClickListener(v -> showScreen(10));

        // Quick Siren
        findViewById(R.id.btnIncidentSiren).setOnClickListener(v -> {
            if (sirenManager.isPlaying()) {
                sirenManager.stopSiren();
                Toast.makeText(this, "Siren Stopped", Toast.LENGTH_SHORT).show();
            } else {
                sirenManager.startSiren();
                Toast.makeText(this, "Distress Siren Active!", Toast.LENGTH_SHORT).show();
            }
        });

        // Quick Strobe
        findViewById(R.id.btnIncidentStrobe).setOnClickListener(v -> {
            if (flashlightManager.isStrobing()) {
                flashlightManager.stopStrobe();
                Toast.makeText(this, "Strobe Stopped", Toast.LENGTH_SHORT).show();
            } else {
                flashlightManager.startStrobe();
                Toast.makeText(this, "Optical Rescue Strobe Active!", Toast.LENGTH_SHORT).show();
            }
        });

        // Call 112
        findViewById(R.id.btnIncidentCall112).setOnClickListener(v -> dialNumber("112"));
    }

    private void startIncidentTimer() {
        incidentElapsedSeconds = 154; // default demo point
        incidentHandler.removeCallbacks(incidentTimerRunnable);
        incidentHandler.post(incidentTimerRunnable);
    }

    // ==========================================
    // SCREEN 6: CONTACTS & FAMILY
    // ==========================================
    private void setupScreen6Contacts() {
        TextView tabFam = findViewById(R.id.tabFamily);
        TextView tabFri = findViewById(R.id.tabFriends);
        TextView tabVol = findViewById(R.id.tabVolunteers);

        tabFam.setOnClickListener(v -> {
            tabFam.setBackgroundResource(R.drawable.bg_tab_pill_active);
            tabFam.setTextColor(Color.WHITE);
            tabFri.setBackgroundResource(R.drawable.bg_tab_pill_inactive);
            tabFri.setTextColor(Color.parseColor("#64748B"));
            tabVol.setBackgroundResource(R.drawable.bg_tab_pill_inactive);
            tabVol.setTextColor(Color.parseColor("#64748B"));
        });

        tabFri.setOnClickListener(v -> {
            tabFri.setBackgroundResource(R.drawable.bg_tab_pill_active);
            tabFri.setTextColor(Color.WHITE);
            tabFam.setBackgroundResource(R.drawable.bg_tab_pill_inactive);
            tabFam.setTextColor(Color.parseColor("#64748B"));
            tabVol.setBackgroundResource(R.drawable.bg_tab_pill_inactive);
            tabVol.setTextColor(Color.parseColor("#64748B"));
        });

        tabVol.setOnClickListener(v -> {
            tabVol.setBackgroundResource(R.drawable.bg_tab_pill_active);
            tabVol.setTextColor(Color.WHITE);
            tabFam.setBackgroundResource(R.drawable.bg_tab_pill_inactive);
            tabFam.setTextColor(Color.parseColor("#64748B"));
            tabFri.setBackgroundResource(R.drawable.bg_tab_pill_inactive);
            tabFri.setTextColor(Color.parseColor("#64748B"));
        });

        findViewById(R.id.btnAddContact).setOnClickListener(v -> {
            Toast.makeText(this, "Add Contact: Select from address book", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.btnCallPriya).setOnClickListener(v -> dialNumber("+919876543210"));
        findViewById(R.id.btnChatPriya).setOnClickListener(v -> {
            Toast.makeText(this, "SMS / WhatsApp alert sent with live location to Priya Sharma", Toast.LENGTH_SHORT).show();
        });
    }

    // ==========================================
    // SCREEN 7: AUTOMATIC DETECTION ALERT
    // ==========================================
    private void setupScreen7CrashAlert() {
        tvCrashCountdown = findViewById(R.id.tvCrashCountdown);
        progressCrashCountdown = findViewById(R.id.progressCrashCountdown);

        findViewById(R.id.btnCrashImOk).setOnClickListener(v -> {
            if (crashCountdownTimer != null) crashCountdownTimer.cancel();
            stateEngine.cancelFalseAlarm();
            Toast.makeText(this, "False Alarm Cancelled. Status: Safe", Toast.LENGTH_SHORT).show();
            showScreen(2);
        });

        findViewById(R.id.btnCrashNeedHelp).setOnClickListener(v -> {
            if (crashCountdownTimer != null) crashCountdownTimer.cancel();
            stateEngine.confirmEmergencyImmediately();
            dispatchCapsuleToServer("CRASH_CONFIRMED");
            showScreen(5);
            startIncidentTimer();
        });
    }

    public void triggerCrashAlertSimulation() {
        showScreen(7);
        if (crashCountdownTimer != null) crashCountdownTimer.cancel();
        progressCrashCountdown.setMax(20);
        progressCrashCountdown.setProgress(20);

        crashCountdownTimer = new CountDownTimer(20000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int secs = (int) (millisUntilFinished / 1000);
                if (tvCrashCountdown != null) {
                    tvCrashCountdown.setText(String.format(Locale.US, "00:%02d", secs));
                }
                if (progressCrashCountdown != null) {
                    progressCrashCountdown.setProgress(secs);
                }
            }

            @Override
            public void onFinish() {
                // Auto escalate if no response
                stateEngine.confirmEmergencyImmediately();
                dispatchCapsuleToServer("CRASH_AUTO_ESCALATED");
                showScreen(5);
                startIncidentTimer();
            }
        }.start();
    }

    // ==========================================
    // SCREEN 8: EVIDENCE CAPTURE
    // ==========================================
    private void setupScreen8Evidence() {
        findViewById(R.id.btnBackEvidence).setOnClickListener(v -> showScreen(2));
    }

    // ==========================================
    // SCREEN 9: RESPONDER VIEW
    // ==========================================
    private void setupScreen9Responder() {
        findViewById(R.id.btnResponderNavigate).setOnClickListener(v -> {
            Uri gmmIntentUri = Uri.parse("geo:12.9716,77.5946?q=12.9716,77.5946(SafeSphere+Emergency)");
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");
            try {
                startActivity(mapIntent);
            } catch (Exception e) {
                Toast.makeText(this, "Navigating to: 12.9716° N, 77.5946° E (MG Road)", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.btnResponderCall).setOnClickListener(v -> dialNumber("+919876543210"));
    }

    // ==========================================
    // SCREEN 11: POST-INCIDENT SUMMARY
    // ==========================================
    private void setupScreen11Summary() {
        findViewById(R.id.btnBackToDashboard).setOnClickListener(v -> showScreen(2));
        findViewById(R.id.btnViewFullReport).setOnClickListener(v -> showScreen(8));
    }

    // ==========================================
    // SCREEN 12: SETTINGS & PREFERENCES
    // ==========================================
    private void setupScreen12Settings() {
        findViewById(R.id.btnBackSettings).setOnClickListener(v -> showScreen(2));

        findViewById(R.id.rowSettingTriggers).setOnClickListener(v -> {
            Toast.makeText(this, "Emergency Triggers: Crash, Fall & Triple-Press Active", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.rowSettingEscalation).setOnClickListener(v -> showScreen(6));

        findViewById(R.id.rowSettingEvidence).setOnClickListener(v -> showScreen(8));

        findViewById(R.id.rowSettingPrivacy).setOnClickListener(v -> {
            Toast.makeText(this, "Privacy: AES-256 local encrypted storage", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.rowSettingBattery).setOnClickListener(v -> {
            // Toggle extreme survival mode
            SurvivalProfile current = survivalEngine.getCurrentProfile();
            if (current.isExtremeSurvivalActive()) {
                survivalEngine.updateBatteryLevel(85);
                Toast.makeText(this, "Survival Mode: Normal Standard Profile", Toast.LENGTH_SHORT).show();
            } else {
                survivalEngine.updateBatteryLevel(12);
                Toast.makeText(this, "Survival Mode: Low Power Mesh Mode Active", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.rowSettingLanguage).setOnClickListener(v -> {
            Toast.makeText(this, "Language: English (India)", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.rowSettingAbout).setOnClickListener(v -> showScreen(1));
    }

    private void dispatchCapsuleToServer(String triggerHint) {
        EmergencyCapsule capsule = stateEngine.getCurrentCapsule();
        DispatcherHttpClient.dispatchCapsuleAsync(capsule, triggerHint);
    }

    private void dialNumber(String number) {
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:" + number));
        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Dialing: " + number, Toast.LENGTH_SHORT).show();
        }
    }

    private void wireEventSubscriptions() {
        eventBus.subscribe(StateTransitionEvent.class, event -> runOnUiThread(() -> {
            if (event.getToState() == FSMState.EMERGENCY) {
                if (currentScreenIndex != 5) {
                    showScreen(5);
                    startIncidentTimer();
                }
            }
        }));

        survivalEngine.addProfileListener(profile -> runOnUiThread(() -> {
            if (profile.isExtremeSurvivalActive()) {
                rootView.setBackgroundColor(Color.BLACK);
                WindowManager.LayoutParams params = getWindow().getAttributes();
                params.screenBrightness = 0.05f;
                getWindow().setAttributes(params);
            } else {
                rootView.setBackgroundColor(Color.parseColor("#F8FAFC"));
                WindowManager.LayoutParams params = getWindow().getAttributes();
                params.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
                getWindow().setAttributes(params);
            }
        }));
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            double gForce = Math.sqrt(x * x + y * y + z * z) / SensorManager.GRAVITY_EARTH;

            // Real physical high-G spike crash detection
            if (gForce > 4.5 && stateEngine.getCurrentState() == FSMState.SAFE) {
                Telemetry tel = stateEngine.getCurrentCapsule().getTelemetry();
                tel.setCrashImpactG(gForce);
                stateEngine.evaluateSensorSpike(tel, String.format(Locale.getDefault(),
                        "Live Accelerometer Crash Spike (%.1fG)", gForce));
                ThreatResult result = cvService.verifyThreat("Severe vehicular crash collision");
                stateEngine.evaluateThreatResult(result);
                triggerCrashAlertSimulation();
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
                    });
                }

                @Override
                public void onLost(@NonNull Network network) {
                    runOnUiThread(() -> {
                        stateEngine.getCurrentCapsule().getTelemetry().setNetworkQuality(NetworkQuality.OFFLINE);
                        meshNetwork.onNetworkChange(true);
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

        if (triggerCountdownTimer != null) {
            triggerCountdownTimer.cancel();
        }

        if (crashCountdownTimer != null) {
            crashCountdownTimer.cancel();
        }

        incidentHandler.removeCallbacks(incidentTimerRunnable);

        if (connectivityManager != null && networkCallback != null) {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback);
            } catch (Exception ignored) {}
        }
    }
}
