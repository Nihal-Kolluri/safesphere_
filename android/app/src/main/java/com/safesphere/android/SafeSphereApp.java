package com.safesphere.android;

import android.app.Application;
import com.safesphere.crypto.EvidenceVault;
import com.safesphere.cv.ThreatVerificationService;
import com.safesphere.fsm.EmergencyStateEngine;
import com.safesphere.survival.MeshStoreAndForward;
import com.safesphere.survival.SurvivalEngine;

/**
 * Android Application entry point maintaining singleton engine instances.
 */
public class SafeSphereApp extends Application {
    private static SafeSphereApp instance;

    private EmergencyStateEngine stateEngine;
    private SurvivalEngine survivalEngine;
    private EvidenceVault evidenceVault;
    private MeshStoreAndForward meshNetwork;
    private ThreatVerificationService cvService;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        // Initialize core engines
        this.stateEngine = new EmergencyStateEngine();
        this.survivalEngine = new SurvivalEngine();
        this.evidenceVault = new EvidenceVault();
        this.meshNetwork = new MeshStoreAndForward();
        this.cvService = new ThreatVerificationService();
    }

    public static SafeSphereApp getInstance() {
        return instance;
    }

    public EmergencyStateEngine getStateEngine() {
        return stateEngine;
    }

    public SurvivalEngine getSurvivalEngine() {
        return survivalEngine;
    }

    public EvidenceVault getEvidenceVault() {
        return evidenceVault;
    }

    public MeshStoreAndForward getMeshNetwork() {
        return meshNetwork;
    }

    public ThreatVerificationService getCvService() {
        return cvService;
    }
}
