# JTAK Manual — RED (risk-estimate distance) display (F03)

> **TRAINING USE ONLY / 訓練用**
>
> JTAK is a plugin for practicing the JTAC role in milsim and tactical training.
> It is not for real operations, live-fire shooting, or controlling real aircraft. The screen and the descriptions of the circles on the map always show "TRAINING USE ONLY / 訓練用". There is no setting to remove this notice.
>
> **The table values are training sample values, not real safety distances, risk-estimate distances, or damage radii.** The top of the RED screen always shows the following sentence. There is no way to hide it.
>
> `訓練用サンプル値 — 実際の安全距離・危害半径ではない / TRAINING SAMPLE VALUES — NOT REAL SAFETY DATA`

- Date: 2026-11-09
- Applies to: JTAK loaded into ATAK-CIV 5.8.0.9
- Japanese original: [`../F03-red.md`](../F03-red.md) (Japanese)

This manual describes the English screen. JTAK shows the English screen when the device language is not Japanese.
Screen names (buttons, fields) are written in quotation marks ("…") exactly as they appear on the plugin's screen.

## 1. What this function does

- Draws circles around a target point, one for each distance in a table row. In training, the circles are used to practice seeing a rough guide of "it is dangerous if friendlies are inside this range".
- Lists on the screen the friendly points that are inside the circles.
- Table rows can be edited in the JTAK screen. The edited table is saved to a file on the device.

The name "RED" comes from the JP 3-09.3 term risk-estimate distance. Although it is called RED, the table values are not real risk-estimate distances. The values remain training sample values.

## 2. Opening the screen

1. Open JTAK from the ATAK toolbar. The JTAK screen appears on the right side.
2. In the second row of buttons at the top of the screen, press "RED", to the right of "Game plan". The screen switches to RED.
3. The "TRAINING USE ONLY / 訓練用" line at the very top of the screen stays visible on the RED screen too.

Each time you press "RED" to open the screen, JTAK reloads the table file and the list of map points.

## 3. Screen layout

From top to bottom:

1. The training sample values sentence (bold. The same as the sentence in the box at the top of this manual)
2. The title "Risk-estimate distance (RED) (TRAINING)"
3. The table status line (3.1)
4. The "Target point" selection field and the "Refresh point list" button
5. The "Weapon row" selection field and the rings of the selected row (example: "Inner 100 m / Outer 300 m")
6. The "Draw" and "Erase" buttons
7. The result field (chapter 6)
8. The "Edit table" section (chapter 7)

### 3.1 Table status line

Shows which table is in use, as one of the following.

| Text shown | Meaning |
|---|---|
| "Table: default (no file)" | There is no table file. The default table built into the plugin is in use |
| "Table: file (N rows)" | The table file on the device was read and is in use. N is the number of rows |
| "The table file is invalid, so the default table is used" | The table file cannot be read, or its content does not follow the rules. The default table is in use. The reasons appear below (chapter 9) |

### 3.2 Default table

At first, the table has the following 3 rows.

| Row (label) | Ring 1 | Ring 2 |
|---|---|---|
| Sample S | Inner 100 m | Outer 300 m |
| Sample M | Inner 250 m | Outer 600 m |
| Sample L | Inner 500 m | Outer 1000 m |

- These values have no source. Meaningless round numbers were chosen. They are neither values of real weapons nor real safety distances or damage radii.
- The note of all 3 rows is "訓練用サンプル値。実在の兵器の値ではない / TRAINING SAMPLE VALUE — NOT A REAL WEAPON". (The note is the same in both languages. The Japanese manual quotes only the Japanese half.)
- JTAK does not suggest row names.

## 4. Drawing circles

1. In the "Target point" field, select the point to use as the center of the circles.
   - The list shows map points by name. Your own point is also shown. The first value is "(none)".
   - Points placed with the LRF (F01) can also be selected.
   - The list works the same as on the 9-line screen (F01-F02 manual, 3.1. See [`F01-F02-lrf-and-cas.md`](F01-F02-lrf-and-cas.md)). Circles drawn by JTAK are not shown in the list.
   - After adding or moving points, press "Refresh point list".
2. In the "Weapon row" field, select a table row. Below it, the rings of the selected row are shown in the form "Inner 100 m / Outer 300 m".
3. Press "Draw".

The circles that are drawn:

- One circle is drawn for each ring, centered on the position of the selected point. A row has 1 to 3 rings.
- The circles are outlines only, not filled. The line colors are red, orange, and yellow, starting from the inner ring.
- The circle title is "TRAINING {label} {ring name} {radius} m". Example: "TRAINING Sample S Inner 100 m".
- The circle description (remarks) contains the following lines, in this order.
  1. `TRAINING USE ONLY / 訓練用`
  2. `訓練用サンプル値 — 実際の安全距離・危害半径ではない / TRAINING SAMPLE VALUES — NOT REAL SAFETY DATA`
  3. "Target: {name of the target point}"
  4. The row's note (if the note is empty, this line does not appear)
- The circles go into the same group as ATAK's drawing tools (Drawing Objects). When that group does not exist, JTAK makes a group named "JTAK TRAINING RED" and puts them there.
- JTAK does not place a point at the center of the circles.

Cautions:

- Each time you press "Draw", circles are added. Circles drawn before are not erased. To redraw, press "Erase" first.
- If the target point moves later, the circles do not move. Draw them again.
- When you press "Draw", JTAK gets the list of map points again. If the selected point has disappeared from the map, no circles are drawn and "Select a target point" appears.

## 5. Erasing circles

- Press "Erase" to remove all circles drawn by JTAK from the map. The result field shows "Circles erased: N".
- Circles already deleted with ATAK's own operations are not counted in N.
- Other points and shapes the user drew in ATAK are not erased.
- When the JTAK screen is closed (when the plugin is closed), the circles drawn by JTAK are also removed.

## 6. Friendly points inside the RED circles

When you press "Draw", the result field shows the following. The first line is always `TRAINING USE ONLY / 訓練用`.

When friendly points are inside the circles:

```
TRAINING USE ONLY / 訓練用
Friendly points inside the RED circles:
{point name} — {ring name} ({distance} m)
…
```

When no friendly points are inside the circles:

```
TRAINING USE ONLY / 訓練用
Friendly points inside the RED circles: none
```

Rules:

- The points counted as friendly are your own point and points whose type (CoT type) starts with `a-f-`. Hostile points and unknown points are not counted.
- The point selected as the target itself is not counted.
- A point is "inside" when its distance from the target is less than or equal to the radius of the outermost ring. A point whose distance is exactly the radius is also inside.
- The ring name shown is the name of the innermost ring that the point is inside.
- The distance is the distance from the target, in whole m (rounded). Points are listed nearest first.
- Points whose distance could not be calculated are not counted as inside. If there are such points, "Points with unknown distance: N" appears.
- The list is made from the positions at the time "Draw" was pressed. If points move later, the list does not change. Press "Draw" again to remake it with the new positions (circles are also added. See the cautions in chapter 4).
- The text in the result field can be selected and copied.

## 7. Editing the table

In the "Edit table" section, edit the row selected in the "Weapon row" field.

### 7.1 Fields

| Field | What to enter |
|---|---|
| "Label" | Name of the row. 1 to 40 characters |
| "Ring 1 name", "Ring 1 radius (m)" | The innermost ring. The name is up to 20 characters. The radius is a whole number from 1 to 50000 (m) |
| "Ring 2 name (leave empty if not used)", "Ring 2 radius (m)" | The second ring. When not used, leave both the name and the radius empty |
| "Ring 3 name (leave empty if not used)", "Ring 3 radius (m)" | The third ring. When not used, leave both the name and the radius empty |
| "Note" | Text put in the circle description. Up to 200 characters. May be empty |

- Make the radii larger from the inner ring outward. The same value cannot be used twice.
- Enter only half-width digits in the radius. A decimal point or a sign cannot be used.
- An emoji is also counted as 1 character.
- Half-width spaces before and after a name or note are removed. Full-width spaces are not removed.
- We recommend not writing the names of real weapons in row names, and using generic names such as "Sample S". JTAK does not check the content of the names.

### 7.2 Buttons

| Button | What it does |
|---|---|
| "Save row" | Rewrites the selected row with the field values and saves the table to the file. When it succeeds, "Saved" appears |
| "Add row" | Adds a row labeled "New row" with one ring ("Inner", 100 m) at the end of the table and saves it. The added row becomes selected. Cannot be pressed when the table has 50 rows |
| "Delete row" | A confirmation screen shows "Delete {label}". Press "OK" to delete the row and save. Press "Cancel" to do nothing. Cannot be pressed when the table has 1 row |
| "Reset to default table" | A confirmation screen shows "Reset the table to the 3 default rows. The current rows will be removed." Press "OK" to save the default table (3.2) to the file. Press "Cancel" to do nothing |
| "Reload" | Reads the table file again. Values entered in the edit fields are discarded |

- "Reset to default table" writes the default table without deleting the file. Because of this, the table status line after that is "Table: file (3 rows)", not "Table: default (no file)".
- When saved, the "Weapon row" list is also remade from the new table. The new values are used from the next circles drawn. Circles already drawn do not change.
- Values in the edit fields do not go into the file until saved. If you select another row before saving, the fields are replaced with that row's values and the values you entered are lost. The same happens when you reopen the RED screen.

### 7.3 When an entry is wrong

If you press "Save row" and an entry does not follow the rules, nothing is saved and "Fix the entries:" appears with the reasons. Up to 5 reasons are shown, and the rest are shown as "N more". Examples of reasons:

- "Label: use 1 to 40 characters"
- "Ring 2: enter both the name and the distance" (when only one of the name and the radius is entered)
- "Ring 1: the distance must be a whole number from 1 to 50000 (m)"
- "Ring 2: the distance must be larger than the previous ring"
- "Enter at least one ring" (when all ring fields are empty)

The reason texts write "distance" for the field named "radius". Both refer to the same field.

### 7.4 Table file

- It is saved to `tools/jtak/weapons.json` in the ATAK folder.
- While there is no file, the default table is used. The file is created when you first save. If the `tools/jtak/` folder does not exist, it is created at that time.
- The file is JSON and also contains items stating that the values are training sample values (`notice` and `trainingSampleValues`). JTAK rewrites `notice` with a fixed text on every save. The `notice` text is not shown on the screen or on the circles. The training text shown on the screen and on the circles is a fixed text from JTAK.
- The file format is in "2. 兵器の表" (weapon table) of `docs/interfaces/README.md` (Japanese). You can also edit the file by hand, but if it does not follow the rules, it is not loaded (chapter 9).
- If you save while an invalid file exists, the previous file is kept in the same folder under the name `weapons.invalid-YYYYMMDD-HHmmss.json` before the new table is written.

## 8. Circles are not sent and not kept (Q9)

- JTAK does not send circles to other devices or servers.
- To send a circle, the user sends it with ATAK's operations on the circle (the design allows this. Chapter 10). The description of a sent circle also has the TRAINING line and the training sample values line.
- Circles are not kept on the device. By design, circles disappear when ATAK is restarted (chapter 10).

## 9. Troubleshooting

| Situation | Text shown / reason | What to do |
|---|---|---|
| No circles appear after pressing "Draw" | "Select a target point": the target is still "(none)", or the selected point has disappeared from the map | Press "Refresh point list" and select the target point again |
| "The table has no rows" appears when pressing "Draw" or "Save row" | There is no row to select | Press "Reload". If that does not fix it, press "Reset to default table" |
| A point does not appear in the target list | The point was placed after the screen was opened | Press "Refresh point list" |
| Circles piled up | Each press of "Draw" adds circles (chapter 4) | Press "Erase", then draw again |
| The table status line shows "The table file is invalid, so the default table is used" | The table file cannot be read or does not follow the rules. Up to 5 reasons appear below (example: "entries[1].rings[0].radiusM: must be an integer from 1 to 50000"). When the file is too large, "The file is too large (limit … bytes)" appears. When it cannot be read, "Could not read the table file: …" appears | Check the reasons, fix the file, and press "Reload". Or press "Reset to default table" (the previous file is kept under the name in 7.4) |
| "Could not save the table: …" appears | Saving failed, for example because the folder could not be made or the file could not be written. When the previous file was kept under another name, "The previous file is kept as …" appears | Check the free space on the device and whether the ATAK folder can be written. The values you entered remain in the fields, so press "Save row" again |
| "Could not draw the circles: …" appears | The circles could not be made inside ATAK. The circles added that time are removed. Circles drawn before remain | Press "Draw" again |
| "Could not erase the circles: …" appears | Some circles could not be removed | Press "Erase" again. JTAK also tries to remove them when it is closed |
| "Could not get the point list: …" appears | The list of map points could not be obtained from ATAK | Press "Refresh point list" |
| A friendly point does not appear in the result | The point type is not friendly (does not start with `a-f-`), or it is farther than the outer ring | Check the point type in ATAK |

## 10. What was and was not checked on a real device

Checked (2026-11-09, on a real device. Detailed design 04, chapter 4):

- A row was selected from the table and circles were drawn around the target.
- `TRAINING USE ONLY / 訓練用` and the training sample values sentence appeared on the screen and in the circle descriptions.
- "Erase" erased the circles, and other points remained.

Not yet checked:

- Whether a point appears at the center of the circles (and, if it does, that it is not added to the target list).
- That circles have ATAK's send operation. That JTAK does not send circles automatically.
- That circles are gone after restarting ATAK. That circles are gone after restarting even if they were edited in ATAK (color changed, etc.).
- That saving when `tools/jtak/` does not exist creates the folder and the file. That when the file is broken, the reasons appear on the screen and the default table is used.
- That the result text is correct when friendly points are placed inside and outside the circles.
- Whether the circle description (remarks) appears on ATAK's circle screen.
- In which folder on the device the table file is placed (the part before `tools/jtak/weapons.json`).

## 11. Sources

- Functional specification (Japanese): `docs/functional-spec/F03-weapon-effects.md`
- Detailed design (Japanese): `docs/detailed-design/04-weapon-effects.md`
- Table file format (Japanese): `docs/interfaces/README.md` "2. 兵器の表" (weapon table)
- Rules (Japanese): `docs/usage-scope.md` U1, U2, U3, U5, U8, Q9, Q12
- English screen design (Japanese): `docs/detailed-design/07-english-ui.md`
- Screen text: `software/atak-plugin/app/src/main/res/values/strings.xml` (English) and `values-ja/strings.xml` (Japanese), `software/atak-plugin/app/src/main/java/jp/jtak/plugin/weapons/WeaponPanel.java`, `WeaponCircles.java`
- Training text, default table, and entry error text: `software/atak-plugin/core/src/main/java/jp/jtak/core/TrainingNotice.java`, `core/weapons/DefaultWeaponTable.java`, `WeaponEntryForm.java`
- Origin of the name RED: risk-estimate distance in JP 3-09.3, *Close Air Support*, 25 November 2014 (`docs/references.md` S4). The Japanese translation is item (10) of [`../jp3-09-3/ch3-04.md`](../jp3-09-3/ch3-04.md) (Japanese). The RED section of the original has only a definition and a reference to another document; it has no table of distances per weapon. JTAK's table values are not based on figures in this original.
