# ChoufWIE Hal 🔮

Burnout-risk companion for students and young graduates. Inspired by *Choufli Hal*: Jannet the fortune-teller
pops up whenever the app predicts or "reads" something. She reads your DATA, not the stars.

## 1. Put YOUR images here
Folder: `app/src/main/res/drawable/`

| File name (exact, lowercase)           | Used for                                                    |
|----------------------------------------|-------------------------------------------------------------|
| `jannet.png`                           | Jannet popup, login screen, register screen                 |
| `meme_1.png`, `meme_2.png`, `meme_3.png`, ... | Home-screen memes ("Another one" button cycles them) |

* Number the memes without gaps (1, 2, 3...). Add as many as you want, no code change.
* .jpg / .webp also work (`meme_4.jpg`), but never use the same name twice with two extensions.
* Names: lowercase letters, digits and underscores only. No spaces, no dashes.
* Keep each image under ~1 MB (about 800 px wide is plenty).
* Missing image = a purple placeholder shows, the app never crashes.

## 2. Run the AI (Ollama) - optional but recommended
```
ollama pull llama3.2:3b
```
* Emulator: works out of the box (host `http://10.0.2.2:11434`).
* Real phone: start Ollama with `OLLAMA_HOST=0.0.0.0`, then in the app: Home > Settings > host = `http://<PC-LAN-IP>:11434` > Test connection.
* No Ollama? The app still works: Jannet answers with built-in messages.

## 3. Build
Open this folder in Android Studio (Koala or newer), let Gradle sync, press Run.
minSdk 26, Java 17. Demo: Home > "Load demo data" (simulated data), or Settings.

## Privacy / safety
* Everything stays on the phone. Check-in text is AES-256-GCM encrypted (key in Android Keystore).
* The burnout score is a transparent heuristic, not a medical tool.
* Crisis keywords (EN/FR/AR/arabizi) are detected in plain code, never by the LLM, and show a supportive dialog.
  Check the emergency number in `res/values/strings.xml` (`crisis_text`) for your country.
