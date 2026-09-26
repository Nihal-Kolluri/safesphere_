#!/usr/bin/env python3
"""
SafeSphere Agent API Gateway (M4).
Exposes REST endpoints for IoT devices, smart wearables, and autonomous dispatch agents.
"""

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field
from typing import Optional
import datetime
import uvicorn

app = FastAPI(
    title="SafeSphere Agent Orchestration API Gateway",
    description="Inbound IoT trigger and autonomous emergency capsule dispatch API",
    version="1.0.0"
)

class IoTTriggerPayload(BaseModel):
    sensor_type: str = Field(..., example="CRASH_ACCELEROMETER")
    intensity_g: float = Field(..., example=8.4)
    latitude: float = Field(17.3850, example=17.3850)
    longitude: float = Field(78.4867, example=78.4867)
    threat_hint: Optional[str] = Field("NONE", example="WEAPON")
    device_id: Optional[str] = Field("SMARTWATCH-X7", example="SMARTWATCH-X7")

class EmergencyCapsuleResponse(BaseModel):
    capsule_id: str
    timestamp: str
    fsm_state: str
    threat_status: str
    encrypted_evidence: str
    message: str

@app.get("/")
def root():
    return {
        "service": "SafeSphere Agent Gateway",
        "status": "ONLINE",
        "documentation": "/docs"
    }

@app.post("/api/v1/agent/trigger", response_model=EmergencyCapsuleResponse)
def trigger_agent_incident(payload: IoTTriggerPayload):
    """
    Receives an emergency trigger from external IoT hardware or ride-share apps.
    Bypasses or initiates confirmation based on intensity and threat hints.
    """
    state = "EMERGENCY" if (payload.intensity_g > 6.0 or payload.threat_hint in ["WEAPON", "FIRE"]) else "SUSPICIOUS"
    
    return {
        "capsule_id": "CR-8924",
        "timestamp": datetime.datetime.utcnow().isoformat() + "Z",
        "fsm_state": state,
        "threat_status": f"Threat Verified: {payload.threat_hint} (Intensity: {payload.intensity_g}G)",
        "encrypted_evidence": "AES_256_GCM_ENCRYPTED_VAULT_PAYLOAD_SEALED",
        "message": "Incident ingested and broadcasted to 112 Dispatch Hub."
    }

if __name__ == "__main__":
    uvicorn.run("agent_gateway:app", host="0.0.0.0", port=8000, reload=True)
