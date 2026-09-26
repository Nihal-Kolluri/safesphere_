#!/usr/bin/env python3
"""
SafeSphere Tactical Map Visualizer (M5).
Uses Pandas and Folium to generate an interactive HTML map for Safe Journeys,
route deviations, and verified volunteer capability overlays.
"""

import sys
import os

def generate_map(victim_lat=17.3850, victim_lon=78.4867, output_path="safesphere_live_map.html"):
    try:
        import folium
        import pandas as pd
    except ImportError:
        print("[MapVisualizer] folium or pandas not installed; skipping generation.")
        return

    # Base Map centered around victim
    m = folium.Map(location=[victim_lat, victim_lon], zoom_start=15, tiles="CartoDB dark_matter")

    # Safe Journey Planned Corridor (Green Polyline)
    safe_corridor = [
        [victim_lat - 0.0050, victim_lon - 0.0050],
        [victim_lat - 0.0020, victim_lon - 0.0020],
        [victim_lat, victim_lon]
    ]
    folium.PolyLine(safe_corridor, color="#22c55e", weight=4, opacity=0.8, tooltip="Planned Safe Corridor").addTo(m)

    # Route Deviation Vector (Red Dashed Line)
    deviation_vector = [
        [victim_lat, victim_lon],
        [victim_lat + 0.0035, victim_lon + 0.0040]
    ]
    folium.PolyLine(deviation_vector, color="#ef4444", weight=3, dash_array="6, 6", tooltip="Critical Route Deviation (+420m)").addTo(m)

    # Incident Center Marker
    folium.CircleMarker(
        location=[victim_lat, victim_lon],
        radius=14,
        color="#ffffff",
        weight=2,
        fill=True,
        fill_color="#dc2626",
        fill_opacity=0.9,
        tooltip="VICTIM INCIDENT LOCATION (CR-8924)",
        popup="<b>Victim Location (CR-8924)</b><br>State: ACTIVE_EMERGENCY<br>Battery: 12% (Extreme Survival)"
    ).addTo(m)

    # Responder data
    volunteers = [
        {"id": "VOL-101", "name": "Ramesh Sharma", "lat": 17.3870, "lon": 78.4890, "dist": 0.40, "cpr": False, "score": 1.00},
        {"id": "VOL-204", "name": "Dr. Ananya Reddy", "lat": 17.3910, "lon": 78.4920, "dist": 0.80, "cpr": True, "score": 1.10},
        {"id": "VOL-305", "name": "Vikram Singh", "lat": 17.3860, "lon": 78.4875, "dist": 0.25, "cpr": True, "score": 2.20},
        {"id": "VOL-512", "name": "Kiran Kumar", "lat": 17.3950, "lon": 78.4960, "dist": 1.20, "cpr": True, "score": 0.93}
    ]

    for v in volunteers:
        color = "#2563eb" if v["cpr"] else "#64748b"
        popup_html = f"<b>{v['id']} - {v['name']}</b><br>Dist: {v['dist']} km<br>CPR: {'YES' if v['cpr'] else 'NO'}<br>Score: {v['score']}"
        folium.CircleMarker(
            location=[v["lat"], v["lon"]],
            radius=9,
            color="#ffffff",
            weight=1.5,
            fill=True,
            fill_color=color,
            fill_opacity=0.85,
            tooltip=f"{v['id']} ({v['name']})",
            popup=popup_html
        ).addTo(m)

    m.save(output_path)
    print(f"[MapVisualizer] Interactive HTML map saved to {output_path}")

if __name__ == "__main__":
    generate_map()
