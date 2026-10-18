# ChoufWIE Hal (شوفوي حل)

> *"Choufli hal"* means "find me a solution". **Jannet**, a warm Tunisian fortune-teller, reads your **data, not the future**, afuturend helps you see burnout coming before it hits.

An AI-powered Android app that supports the emotional and mental well-being of young women during their university and early professional journey in Tunisia. Built for the **HERwin Hackathon** (IEEE WIE, École Polytechnique de Sousse).

**Team:** Butterflies tasnim ladhari-Nawres helaoui

<!-- Add 2-3 screenshots or a demo GIF here, e.g. ![Home](docs/home.png) -->

---

## 1. Problem

Young female students and recent graduates in Tunisia face a lot at once: exam and thesis pressure, career uncertainty, social expectations and the move into independence.

- **Burnout and anxiety peak** during exams, thesis preparation and job hunting.
- **Stigma around therapy** keeps many students from seeking support early.
- **Daily habits collapse** under workload: sleep, exercise, time management.
- **Few tools are personal, private and made for student life.**

Figures that show the scale (see the notes below the list):

- 80.6% of 643 Tunisian medical students reported a high level of perceived stress, 69.1% had poor sleep quality and 25.3% showed anxiety-depressive symptoms (Kammoun et al., *European Psychiatry*, 2026; survey run Oct-Nov 2024).
- Women's unemployment in Tunisia was 20.7% versus 12.3% for men in Q1 2026, and 32% of women with a higher-education degree were unemployed versus 14.2% of men (INS, Q1 2026, as reported by Tunisie Numérique).

*Notes: the student stress figures come from medical students and are not split by sex; we did not find a Tunisian dataset covering all women students. Unemployment figures for Q2 2026 vary slightly between outlets, so we quote Q1.*

## 2. Solution overview

ChoufWIE Hal is built around a **30-second daily check-in**. A few free words (English, French, Arabic or Tunisian arabizi), hours of sleep and an energy level become:

- a **burnout-risk score** (0-100) with a **7-day forecast**,
- an **adaptive weekly plan**, and
- a short, kind explanation from **Jannet** with one tiny action for today.

**Two modes**

| Student | Graduate |
|---|---|
| Exam deadlines with coefficients and difficulty, study plan, grade simulator | Job-search strain, daily cap on applications, interview and test prep, a rest day |

**Five tabs:** Home · Plan · Check-in · Insights · Toolkit

**Toolkit:** breathing exercise (4-4-6), 25-minute focus timer, brain dump sorter, reframing helper, grade simulator, small-wins streak, learn cards, grounding exercise and a support circle (trusted contact).

**Safety:** a keyword-based crisis detector shows a supportive message with emergency guidance (SAMU 190 in Tunisia) and never depends on the AI. The app is **non-diagnostic**; the score is a transparent heuristic, not a clinical tool.

## 3. AI integration

AI is a core part of the pipeline, but it does not make the decisions. **Code computes, the LLM explains.**

| Stage | What happens | Who does it |
|---|---|---|
| 1. Understand | Free text becomes stress (1-10), emotion, stressor and event (e.g. rejection, no response) | LLM returning JSON, with a keyword fallback |
| 2. Predict | Burnout score and 7-day forecast: `100 × (0.35 stress + 0.25 sleep debt + 0.30 pressure + 0.10 trend)` | Plain code (`RiskEngine`) |
| 3. Plan | Greedy priority planner; lightens study load and caps applications when risk is high | Plain code (`Planner`) |
| 4. Explain | Jannet phrases the computed numbers in the user's language, max about 90 words, one action | LLM, with a deterministic fallback message |

Also AI-assisted: weekly **insights** (patterns are computed in code, the LLM only phrases them), **brain dump** sorting and **reframing**.

**Design choices**

- **Local LLM via [Ollama](https://ollama.com)** (default model `llama3.2:3b`): check-in text is not sent to a cloud AI service.
- **Grounded output:** Jannet receives the facts computed by the app and is told to use only those numbers, never diagnose and never mention medication.
- **Never an error screen:** if the model is offline or slow, every AI feature falls back to a built-in answer, and the risk score and plan keep working.
- **Privacy:** check-in text is stored encrypted (AES-256-GCM, key in the Android Keystore); passwords use PBKDF2-HMAC-SHA256 with a per-user salt.

## 4. How to run the project

### Prerequisites

- Android Studio and an emulator or an Android phone
- [Ollama](https://ollama.com) installed on a computer

### Steps

1. **Start the local model** on your computer:
   ```bash
   ollama pull llama3.2:3b
   ollama serve
   ```
2. **Open the project in Android Studio** and run it on an emulator or device. This repository contains the `main` source set (`java/`, `res/`, `assets/`, `AndroidManifest.xml`); use it as `app/src/main` in a standard Android Studio Java project that includes the AndroidX AppCompat, Material 3 and Room libraries with view binding enabled. [Add minSdk and Gradle details here if you publish the full Gradle files.]
3. **Connect the app to Ollama** (Settings screen):
   - **Emulator:** the default host `http://10.0.2.2:11434` already points to your computer.
   - **Real phone:** start Ollama with `OLLAMA_HOST=0.0.0.0 ollama serve`, then enter `http://<your-computer-LAN-IP>:11434` as the host. Phone and computer must be on the same network.
   - Use the connection test in Settings to check that Ollama is reachable.
4. **Register an account**, choose Student or Graduate mode and make your first check-in.
5. **Demo:** *Settings → Load demo data* fills a week of **simulated** check-ins and deadlines so you can see the forecast, plan and insights right away. Demo data is not real.

> The app allows cleartext HTTP so it can reach a local Ollama server on your own network. Do not expose Ollama to the public internet.

## 5. Tech stack

- **Platform:** Android (Java), Material 3 UI, Room database, Android Keystore
- **AI:** local LLM served by Ollama (`llama3.2:3b` by default) over its HTTP API; keyword and rule-based fallbacks
- **Engine:** custom risk model, forecast and adaptive planner (no external services)
- **Security:** AES-256-GCM, PBKDF2-HMAC-SHA256

## Project structure

```
java/com/choufwie/hal/
  ai/        AiService, OllamaProvider, CrisisDetector, Analysis
  engine/    RiskEngine, Planner, Insights, GradeSimulator, Snapshot, DemoSeeder
  data/      Room database, entities and DAOs
  ui/        Home, Plan, Check-in, Insights, Toolkit, Jannet dialog, login, settings
  util/      Prefs, Security, Utils
res/         layouts, strings, colors, icon
```

## Limitations and responsible use

- ChoufWIE Hal is **not a medical device** and does not diagnose. The burnout score is a heuristic built to prompt early self-care.
- It does not replace a friend, a counselor or a doctor. If you are in danger or thinking about hurting yourself, talk to someone you trust and call your local emergency number (Tunisia: SAMU 190).
- The risk formula and weights have not been clinically validated.

## Future improvements

Pilot with students at Polytechnique de Sousse · voice check-ins in Tunisian Arabic · a smaller model running fully on the phone · review of the risk model with psychologists · opt-in links to campus counseling · iOS and wearable sleep data.
