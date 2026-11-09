# JTAK Manual — LRF range points (F01) and CAS formats (F02)

> **TRAINING USE ONLY / 訓練用**
>
> JTAK is a plugin for practicing the JTAC role in milsim and tactical training.
> It is not for real operations, live-fire shooting, or controlling real aircraft. The screen, the map points, and the first and last lines of each format always show "TRAINING USE ONLY / 訓練用". There is no setting to remove this notice.
>
> Weapon values shown by JTAK (the RED screen, F03) are **training sample values**, not real safety distances, risk-estimate distances, or damage radii.

- Date: 2026-11-09
- Applies to: JTAK loaded into ATAK-CIV 5.8.0.9
- Japanese original: [`../F01-F02-lrf-and-cas.md`](../F01-F02-lrf-and-cas.md) (Japanese)

This manual describes the English screen. JTAK shows the English screen when the device language is not Japanese.
Screen names (buttons, fields) are written in quotation marks ("…") exactly as they appear on the plugin's screen.

## 1. Opening the screen and switching screens

1. Open JTAK from the ATAK toolbar. The JTAK screen appears on the right side.
2. At the top of the screen are the buttons "LRF", "9-line", "Routing", "Check-in", "Game plan", and "RED". Pressing one switches to that screen.
3. The "TRAINING USE ONLY / 訓練用" line at the very top of the screen stays visible on every screen.

## 2. Placing LRF range points on the map (F01)

JTAK places a target point on the map from the distance and azimuth measured with a laser rangefinder (LRF).

### 2.1 Input paths

| Path | How to use | Status |
|---|---|---|
| ATAK LRF link | Connect a Bluetooth LRF that ATAK supports. Or send lines of the form `1,uid,time,distance,azimuth,elevation[,name]` to UDP port 17211 | Available |
| Manual entry | Type into the "Manual entry" fields on the "LRF" screen (2.4) | Available |
| BLE direct | Connect the LRF directly to JTAK | Not available yet (to be built after the model is decided) |

### 2.2 Auto-marking switch

- Turn it on or off with "Auto-marking (ATAK LRF link)" on the "LRF" screen. It is on at first.
- The chosen state is kept even after restarting ATAK.
- When on: JTAK receives the measurements that arrive through the ATAK LRF link and places points.
- When off: ATAK's standard behavior (a red line and point from your own position) returns. Manual entry can be used even when it is off.
- When the distance field of an ATAK LRF link line starts with `COMPASS`, `RANGE`, or `MAIN`, ATAK shows a notice as a measurement error. JTAK does not place a point.

### 2.3 The point that is placed

- A point is placed at the measured distance and azimuth from your own position.
  - When the azimuth is magnetic, the declination calculated on the device is added to make it true before placing.
  - The curvature of the earth is not included in the calculation.
- The point type is an "unknown" ground point. The name is "JTAK LRF HHmmss" (HHmmss is the device time).
- The point description (remarks) contains the following, in this order. The name of the measuring device is not written.
  1. TRAINING USE ONLY / 訓練用
  2. JTAK LRF range point
  3. Input: one of ATAK LRF link / BLE direct / Manual entry
  4. Distance
  5. Azimuth (for magnetic input, in the form "Azimuth: magnetic 090.0° / declination +7.5° / true 097.5°". For true input, "Azimuth: true …")
  6. Elevation angle
  7. Horizontal distance / Height difference
  8. Target latitude and longitude / Altitude (HAE. "unknown" when not known)
  9. Time (UTC)
  10. TRAINING USE ONLY / 訓練用
- The point is saved on the device and remains after restarting ATAK. JTAK does not delete points. Delete points you no longer need with ATAK's own operations.
- JTAK does not send points to other devices. To send one, send it by hand with ATAK's "send". The description of a sent point also has the TRAINING line.

### 2.4 Manual entry

1. In the "Manual entry" fields on the "LRF" screen, enter:
   - "Distance (m)"
   - "Azimuth (°)" and its reference ("Magnetic" or "True". Magnetic at first)
   - "Elevation angle (°, up is positive)" (0 at first)
2. Press "Place point".
3. If a field cannot be read as a number, a notice "Point not placed: a field is not a number" appears, preceded by the TRAINING line.

### 2.5 When a point is not placed

In the following cases, no point is placed and a notice appears on the screen. Every notice starts with "TRAINING USE ONLY / 訓練用 — ", and the parentheses at the end contain the measured distance.

| Case | Notice on the screen (example) |
|---|---|
| The distance exceeds 20 km (exactly 20,000 m is placed) | "TRAINING USE ONLY / 訓練用 — Point not placed: distance exceeds the 20 km limit (distance 25000.0 m)" |
| The azimuth could not be obtained | "TRAINING USE ONLY / 訓練用 — Point not placed: azimuth unavailable (distance 500.0 m)" |
| Your own position cannot be obtained | "TRAINING USE ONLY / 訓練用 — Point not placed: own position unavailable (distance 500.0 m)" |

### 2.6 Caution when there is no GPS

When GPS is not available, the point is placed from the last position of your own point on the map. Points are placed even when the screen shows NO GPS, so before measuring, check that your own point is in the right position.

## 3. Making CAS formats (F02)

On the "9-line", "Routing", "Check-in", and "Game plan" screens, JTAK makes formats for reading aloud from map points and your input. Common behavior:

- Select map points and press "Fill from map points" to fill the fields that can be calculated.
- Values filled in automatically can also be corrected by hand.
- Press "Make" to show the result on the screen. The result text can be selected and copied.
- The first and last lines of the result are TRAINING lines.
- JTAK never sends a format to other devices by itself. Besides showing it on the screen, you can export it to a text or PDF file (F04. See [`F04-export.md`](F04-export.md)). To send it, the user chooses the destination on the share screen that opens after exporting.
- Field names show the format's item names in English. (On the Japanese screen, a short Japanese explanation is added to them.)
- The first value of each selection field is "(none)".

### 3.1 How to select map points

- The list shows map points by name (callsign). Your own point is also shown by name.
- When two or more points have the same name, they are told apart by adding coordinates (example: `IP1 1 (34.9640, 138.8533)`). Names that differ only in upper and lower case are counted as the same name.
- Points that ATAK uses internally (`TARGET_POINT`) and the end points of range lines (Range & Bearing) are not shown in the list.
- After adding or deleting points, press "Reload point list". "Map points: N" appears above the list.

### 3.2 9-line (CAS brief)

The screen title is "CAS 9-line and Remarks (JP 3-09.3 2014 Fig. V-9)".

1. Select three points.
   - "IP/BP point" and its type ("IP (distance in NM)" or "BP (distance in m)". IP at first)
   - "Target point"
   - "Friendly point"
2. Press "Fill from map points". Values are filled into lines 1, 2, 3, 4, 5, 6, and 8, and "Filled from map points" appears. Lines that could not be filled are shown as "Lines not filled: …".
   - Fields whose values cannot be obtained become empty. Values you entered by hand are also cleared, so enter the hand-entered fields after pressing this button.
3. Enter the remaining fields by hand, and correct the automatically filled values if needed.
4. Press "Make".

Values filled in automatically:

| Line | Content | Example |
|---|---|---|
| 1 IP/BP | Name of the IP/BP point | `IP1` |
| 2 Heading | Magnetic bearing from the IP/BP to the target. A 3-digit integer; 0 is written as 360 | `084` |
| 3 Distance | For IP, in NM with 1 decimal place. For BP, in units of 100 m | `29.7 NM`, `1200 meters` |
| 4 Target elevation | Elevation (MSL) of the target point, in feet. Empty when the point has no height | `1250 feet` |
| 5 Target description | If the target point is a hostile point, a draft is filled in from its type. Empty for non-hostile points or types not in the table | — |
| 6 Target location | MGRS of the target point | `54S UD 08816 72272` |
| 8 Location of friendlies | Direction (from true north) and distance (whole m) of the single nearest friendly, as seen from the target. Empty when a friendly is at the same point as the target | `East 318` |

Fields entered by hand:

- Line 7 "7 Type mark / Terminal guidance" and line 9 "9 Egress". If line 9 does not start with `Egress`, it is added in the result.
- "Offset (left / right)". It appears with line 2 in the form "306, Offset left".
- The five "Remarks" fields: "LTL/PTL", "Ordnance / Effects", "Threat / SEAD", "Additional remarks", "Additional calls". Only the items you entered appear in the result.
- The "Restrictions" fields (3.6).

Order of the result:

- First a TRAINING line, then `CAS 9-LINE (JP 3-09.3 2014 Fig. V-9) / Datum: WGS 84`.
- Sections in the order [1-3], [4-6], [7-9], [Remarks], [Restrictions]. The last line is also a TRAINING line.
- Lines 4 and 6 have the mark `(readback)`, which asks for a readback.
- Lines not entered are shown as `（未入力）` (the "not entered" mark. The format content is the same in both languages, so this mark stays in Japanese), and the screen shows a notice in the form "Lines not entered: 5, 8".
- When lines 1–3 are all empty, [1-3] is not output, and line 4 is output in the form `Elevation …`.

### 3.3 Routing

The screen title is "Routing (JP 3-09.3 2014 Fig. V-3)".

- Enter "Aircraft callsign", "Routing / Holding instruction" (choose from "Proceed … and report established", "Maintain …", "Report passing …"), and "Place (point name, altitude, etc.)".
- Enter the place by hand, or select a map point in "Point for the place" and press "Fill from map points". The point name is entered and "Put the map point name in the place field" appears. If no point is selected, "Select a point" appears.
- Other fields: "Other instructions", "You are the only aircraft on station", "Other AOS (other aircraft)", "Immediate threats", "Significant weather/terrain", "Status calls".
- In "Closing instruction", choose "Send your check-in" or "Hold your check-in". For Hold, you may enter "Hold reason (optional, no period at the end)" (no period is needed at the end of the sentence).
- "You are the only aircraft on station" and "Other AOS (other aircraft)" cannot both be entered. If both are entered, a notice asks you to leave one of them empty.

### 3.4 Check-in

The screen title is "Check-in (JP 3-09.3 2014 Fig. V-4)".

- Enter "JTAC callsign" and "Aircraft callsign".
- In "Format", choose one of "Full (all items)", "As fragged", or "As fragged, with exception".
- The items are "1 Mission number", "2 Number and type of aircraft", "3 Position and altitude", "4 Ordnance", "5 Playtime", "6 Capabilities", "7 Abort code".
- "No abort code (ABORT, ABORT, ABORT)" and "7 Abort code" cannot both be entered.

### 3.5 Game plan

The screen title is "Game plan (JP 3-09.3 2014 Fig. V-9)".

- Enter "Aircraft callsign".
- In "Type of control", choose Type 1 to 3. The explanations shown on the screen (training summary):
  - Type 1: the controller visually acquires the target and the attacking aircraft in the terminal phase and controls each attack
  - Type 2: the controller reduces risk by means other than visual and controls each attack
  - Type 3: the controller reduces risk by means other than visual and clears multiple attacks within one engagement
- In "Method of attack", choose BOT or BOC.
  - BOT: the aircraft acquires the target or aim point itself
  - BOC: the aircraft attacks the coordinates given in the CAS Brief
- Type and BOT/BOC are chosen separately. Any combination is allowed.
- Other fields: "Attack intent", "CDE concerns", "Ordnance", "Desired effects", "Fuzing", "Interval", "Line 6 digits".

### 3.6 Restrictions

These are on the "9-line" screen.

- Fields: "Danger close", "Danger close: commander's initials", "ACAs", "FAHs", "Separation", "RW approval out of BP", "PLA", "TOT format", "TOT time".
- Choose "TOT format" from "TOT (enter a time)", "Standby TOT", "TOT will be issued after correlation", and "Push when ready".
- The initials can be entered only when "Danger close" is set, and "TOT time" only when "TOT format" is a time. If they conflict, a notice appears.
- If a field that must be entered is empty, "Restrictions not entered: …" appears.

### 3.7 When a required field is empty

On Routing, Check-in, and Game plan, if you press "Make" while a required field is empty, "Items not entered: …" appears on the screen.

### 3.8 Not covered

The following have no dedicated fields. If needed, write them in the free-entry fields (Remarks, Other instructions, etc.).

- Situation Update
- Attacks by multiple aircraft, multiple targets
- GEOREF coordinates
- Moving targets

## 4. What was and was not checked on a real device

Checked:

- How LRF range points are placed (chapter 2) was checked with ATAK on a real device.
- The automatic 9-line values on a real device on 2026-11-08 were: line 2 `084`, line 3 `2.7 NM`, line 4 `965 feet`, line 6 `54S UD 08816 72272`, line 8 `East 318`. The notice that the fields were filled from the map points also appeared.

Not yet checked:

- Comparing the line 4 elevation with the map elevation. Comparing the line 8 direction and distance with a separate calculation.
- How the Routing, Check-in, Game plan, and Restrictions screens look on a real device (button layout, screen length).
- How LRF points look when sent to a TAK server or other devices.
- With manual entry and no GPS, whether the point was placed outside the screen.
- How to delete an own position that was placed by hand.

## 5. Sources

- Functional specification (Japanese): `docs/functional-spec/F01-lrf-marking.md`, `F02-cas-products.md`
- Detailed design (Japanese): `docs/detailed-design/01-lrf-marking.md`, `02-cas-brief.md`, `03-cas-other-products.md`
- English screen design (Japanese): `docs/detailed-design/07-english-ui.md`
- Screen text: `software/atak-plugin/app/src/main/res/values/strings.xml` (English) and `values-ja/strings.xml` (Japanese); LRF notices in `software/atak-plugin/core/src/main/java/jp/jtak/core/lrf/LrfMessages.java`
- Source of the formats: JP 3-09.3, *Close Air Support*, 25 November 2014 (`docs/references.md` S4). The Japanese translation is in [`../jp3-09-3/`](../jp3-09-3/README.md) (Japanese).
