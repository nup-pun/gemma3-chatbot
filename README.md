# Offline Chatbot with Gemma 3-1B

This is an offline Android chatbot app built with [Google's Gemma 3B model (INT4)](https://ai.google.dev/gemma) and powered by [LiteRT](https://github.com/google/mediapipe/tree/master/mediapipe/tasks/genai). It runs completely offline — no internet or external API calls needed.

> ⚠️ Note: The model file (`.task`) is not included in this repo due to GitHub's file size limits. See instructions below to run the app locally.

## Features

-  Simple Chat interface built with Jetpack Compose
-  Runs locally using MediaPipe LLM Inference + Gemma-3-1B model
-  No internet required after installation
-  Fully private, all inference is offline

## How to Run Locally

1. **Clone the repo:**

```bash
git clone https://github.com/nup-pun/gemma3-chatbot.git
cd gemma3-chatbot
```

2. **Download the model from kaggle**
Download link: https://www.kaggle.com/models/google/gemma-3/tfLite

3. **Put the model to your project**
Put the downloaded '.task' file into a app/src/main/assets/ folder (You have to manually create assets folder)

4. **Run the Project in Android Studio**
Run in actual physical device as it may not work in the emulator.
