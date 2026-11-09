# JTAK Manual — Text and PDF export (F04)

> **TRAINING USE ONLY / 訓練用**
>
> JTAK is a plugin for practicing the JTAC role in milsim and tactical training.
> It is not for real operations, live-fire shooting, or controlling real aircraft. The screen always shows "TRAINING USE ONLY / 訓練用". There is no setting to remove this notice.
>
> **Exported files always contain this notice too.** In text files it is the first and last line; in PDF files it is at the top and bottom of every page. There is no setting to export without the notice.
>
> Weapon values shown by JTAK (the RED screen, F03) are **training sample values**, not real safety distances, risk-estimate distances, or damage radii. The RED circles and table are not exported (chapter 1).

- Date: 2026-11-09
- Applies to: JTAK loaded into ATAK-CIV 5.8.0.9
- Japanese original: [`../F04-export.md`](../F04-export.md) (Japanese)

This manual describes the English screen. JTAK shows the English screen when the device language is not Japanese.
Screen names (buttons, fields) are written in quotation marks ("…") exactly as they appear on the plugin's screen.

## 1. What this function does

- Saves a format made on the "9-line", "Routing", "Check-in", or "Game plan" screen to the device as a text file (`.txt`) or a PDF file (`.pdf`).
- After saving, the Android share screen opens. The user can choose a destination (a files app, email, etc.) and hand the file over.
- The following 4 formats can be exported.

| Screen | Format exported |
|---|---|
| "9-line" | CAS Brief (9-line, Remarks, and Restrictions together) |
| "Routing" | Routing |
| "Check-in" | Check-in |
| "Game plan" | Game plan |

The "RED" screen has no export buttons. The RED circles and table are not exported.

## 2. Where the buttons are, and when they can be pressed

- On each of the 4 screens, "Export text" and "Export PDF" are side by side just below the result field.
- The buttons can be pressed only after a format has been made on that screen.
  - They cannot be pressed when the screen has just been opened and "Make" has not been pressed yet.
  - They also cannot be pressed when the format could not be made and the result field became empty (example: on the "9-line" screen, the Restrictions fields conflicted and the result was cleared).
- What is exported is the **last result that was made**. If you correct the entries and export without pressing "Make", the result from before the correction is exported. To export the corrected content, press "Make" first.
- The exported content is the same as the text shown in the result field. No items are added or removed for the export.

## 3. Exporting

1. On the screen you want to export, fill in the fields and press "Make". The format appears in the result field.
2. Press "Export text" or "Export PDF".
3. The file is saved, and "Exported: <save location>" appears at the bottom of the screen. The save location is shown as the full path on the device (example: `/storage/emulated/0/atak/export/jtak/jtak-cas-brief-20261109-153012.pdf`).
4. Then the share screen (titled "Share the exported file") opens. As described in chapter 6, choose a destination or close it.

Even if you close the share screen, the file remains in the save location.

## 4. File name and save location

### 4.1 Save location

- Files are saved to `atak/export/jtak/` in ATAK's working folder (for example, `/storage/emulated/0/atak/export/jtak/`).
- If the folder does not exist, JTAK creates it the first time you export.

### 4.2 Name

The name has the form `jtak-<format>-<date>-<time>.<txt or pdf>`.

| Format | `<format>` part of the name |
|---|---|
| CAS Brief | `cas-brief` |
| Routing | `routing` |
| Check-in | `check-in` |
| Game plan | `game-plan` |

- Example: `jtak-cas-brief-20261109-153012.txt` (a CAS Brief text exported on 9 November 2026 at 15:30:12).
- The date and time are the time set on the device (local time).
- If a file with the same name exists, it is overwritten. The name includes seconds, so names normally do not collide. Only when the same format is exported twice within 1 second is the previous file overwritten.

## 5. File content

### 5.1 Text (`.txt`)

- The character encoding is UTF-8 (no BOM), and line breaks are LF. It can also be read with a PC text editor such as Notepad.
- **The first line and the last line are both `TRAINING USE ONLY / 訓練用`**. This line appears only once at each place.
- Between them, the lines of the format appear in the same order as in the result field.

Example (part of a check-in. The values are made up):

```
TRAINING USE ONLY / 訓練用
CHECK-IN (JP 3-09.3 2014 Fig. V-4)
OWL 21, this is KESTREL 11
1 Mission number: （未入力）
…
TRAINING USE ONLY / 訓練用
```

The format content is the same in both languages. The "not entered" mark `（未入力）` stays in Japanese even on the English screen.

### 5.2 PDF (`.pdf`)

- A4 portrait. The text is 10-point monospaced, in black.
- **Every page has `TRAINING USE ONLY / 訓練用` in bold at the top and bottom**. Even when there are 2 or more pages, it appears on all of them.
- Long lines wrap at the page width. When the content does not fit on one page, it continues on the next page.
- Japanese text (such as `（未入力）`) also appears in its proper characters (checked on a real device. Chapter 9).

## 6. Sharing

- The share screen opens only when an export button is pressed.
- The user chooses the destination. **JTAK never sends anything to other devices or servers by itself.** It is not sent through ATAK's map sharing (CoT) either.
- If you do not want to send it, just close the share screen. The file remains in the save location.
- Sent files also contain `TRAINING USE ONLY / 訓練用` (chapter 5). The receiver can also see that it is for training.

## 7. Loading an exported text later

Work to add "Load from file" to the F02 screens is in progress. When it is finished, you will be able to load an exported text file and put its values back into the input fields.

- The file format that can be read is the same as the exported text in chapter 4 and 5.1 (chapter 3 of `docs/interfaces/README.md`, Japanese). PDF cannot be read.
- The file selection screen starts at `atak/export/jtak/`.
- As of the date of this manual, it cannot be used yet. How to use it will be written in the F02 manual when it is finished.

## 8. Troubleshooting

| Situation | Text shown / reason | What to do |
|---|---|---|
| The buttons cannot be pressed | "Make" has not been pressed yet. Or the format could not be made and the result field became empty | Fill in the fields and press "Make"; press the button after the format appears in the result field |
| The exported file has the content from before the correction | "Make" was not pressed after the entries were corrected (the last result made is exported) | Press "Make", then export again |
| "Could not export: <reason>" appears | The folder could not be made or written. The device may also be out of free space | Check the free space on the device and ATAK's file permissions, and press again. No partly written file remains, and the share screen does not open |
| "Could not share. The file is in the save folder: <save location>" appears | The PDF could not be handed to the share screen. Or the share screen could not be opened | The file was saved. Open `atak/export/jtak/` in a files app and send it from there |
| When sharing a text, it was sent as message text, not as a file | The text file could not be handed to the share screen, so the same content was handed over as text | The file is in the save location. To send it as a file, send it from a files app |
| The share screen does not show a preview (small picture) of the file | The share screen cannot read the file's content. A known problem (chapter 9) | Choose a destination as it is; the file can still be sent |

In every case, ATAK does not stop.

## 9. What was and was not checked on a real device

On 2026-11-09, exporting a CAS Brief to text and PDF was checked on a real device (ATAK-CIV 5.8.0.9).

Checked:

- Files were saved to `atak/export/jtak/` with the names in chapter 4.
- Choosing a destination on the share screen delivered the file (the PDF was saved to `Download/` with the same size).
- "TRAINING USE ONLY / 訓練用" at the top and bottom of the PDF, and the Japanese `（未入力）`, appeared in the correct characters.
- The text file also had the "TRAINING USE ONLY / 訓練用" line first and last, and the Japanese was not garbled.

Known problem:

- The share screen cannot show a preview of the file to be sent. Sending still works. The fix will be done later.

Not yet checked on a real device:

- Exporting Routing, Check-in, and Game plan (built the same way as CAS Brief).
- How the PDF looks when it has 2 or more pages.
- What is shown when an export fails ("Could not export" and "Could not share" in chapter 8).

## 10. Sources

- Functional specification (Japanese): `docs/functional-spec/F04-export.md`
- Detailed design (Japanese): `docs/detailed-design/05-export.md` chapter 1 (scope), chapter 2 (file names and text format), chapter 3 (save location, PDF, sharing), chapter 4 (buttons, flow, failures), chapter 5 (training use), chapter 6 (results checked on a real device)
- Rules (Japanese): `docs/usage-scope.md` U1 (the TRAINING USE ONLY notice), U8 (no automatic sending)
- English screen design (Japanese): `docs/detailed-design/07-english-ui.md`
- Screen text: `software/atak-plugin/app/src/main/res/values/strings.xml` (English) and `values-ja/strings.xml` (Japanese), `software/atak-plugin/app/src/main/java/jp/jtak/plugin/export/ExportShare.java`, the 4 panels in `jp/jtak/plugin/cas/` (`CasBriefPanel`, `RoutingPanel`, `CheckInPanel`, `GamePlanPanel`)
- Training text: `software/atak-plugin/core/src/main/java/jp/jtak/core/TrainingNotice.java`
- Share API reference: the SDK javadoc (S1 in `docs/references.md`, Japanese)
- Format of the file to load (Japanese): chapter 3 of `docs/interfaces/README.md`
