# Design system

## Design intent

The player is an immersive dark stage; learning surfaces sit above it only when useful. Home/Settings remain calm, readable and responsive. Warm amber marks the learning language, indigo identifies interactive cards/actions, and Persian text is shaped/directed independently from English text. Use motion for control feedback and popups, not for decoration.

## Tokens

`core/design/SubLearnTheme.kt` is the source of truth for:

- `SubLearnColors`: light, dark, AMOLED, player stage, learning/native accents, semantic surfaces.
- `SubLearnSpacing`: touch targets, screen gutters, controls, cards, and gesture edge guard.
- `SubLearnRadii`, `SubLearnElevation`, `SubLearnStroke`: shared geometry and mark/card elevations.
- `SubLearnMotion`: popup and feedback motion timings.
- Material 3 `Typography`/`Shapes`, configured in `SubLearnTheme`.

Features should consume these tokens and `MaterialTheme.colorScheme`, not add screen-local literals. `dp`/`sp`/duration values in settings are user-facing controls and are clamped in `AppSettings.normalized()`.

## Typography and direction

- Settings keys are per surface *and* per language role: `menu.app`, `subtitle.learning`, `subtitle.native`, `popup.learning`, `popup.native`, `word-card.learning`, `word-card.native`, `ai.answer`.
- Each `SurfaceFontSettings` supports system/serif/monospace family, size, weight, optional color and AUTO/LTR/RTL direction. Only system-provided font families are shipped.
- Subtitle text direction is set per `TextStyle` using explicit language-role settings, not inferred from the whole app's layout direction. App UI direction follows the chosen UI locale (English LTR, Persian RTL).
- Word-card font role follows the actual source/target BCP-47 language, so a Persian-to-English lookup does not inherit English styling.

## Player layout

- Player surface is full-bleed; top/bottom scrims improve legibility while controls are visible.
- Controls respect status/navigation insets; edge gestures have a side guard to avoid system back/edge navigation.
- Learning/native subtitle positions are independently adjustable; their text hit-target is reserved for translation/layout adjustment rather than whole-surface seeks.
- Controls use 48dp+ targets and expose semantics/content descriptions; list cards and settings remain scrollable at large font scales.

## Motion and feedback

- Controls fade in/out after the configured idle interval (default 3 seconds).
- Learning popup stack enters with short vertical slide + fade.
- Brightness/volume/speed and loading states use concise, localized feedback. Avoid delaying direct user response.

## Accessibility and review checklist

- Do not encode state by color alone; use selected state, labels and descriptions.
- Verify contrast in light/dark/AMOLED and theme dynamic colors on API 31+.
- Verify EN/FA layout, mixed bidi text selection, TalkBack descriptions, font scaling, switch/slider touch targets, and portrait/landscape list behavior.
- No screenshot or device capture is published until a real build has been run; no mock/sample video is shipped.
