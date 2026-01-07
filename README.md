# Smart Credit Card Transaction Analysis

This project is the backend and frontend for a system that analyzes and detects anomalies in credit card transactions using machine learning.

This repository contains a Flask backend and an Android Kotlin frontend (planned/under development).

---

## Project structure

```
Smart-Credit-Card-Transaction-Analysis/
│
├── backend/                      # Flask backend (Python)
│   ├── app.py                    # Main Flask application
│   ├── requirements.txt          # Python dependencies
│   ├── .vscode/                  # VS Code workspace and launch configs (optional)
│   ├── models/                   # Saved/trained ML models (gitignored)
│   └── ...
│
├── frontend/                     # Android Kotlin app (Android Studio)
│   ├── app/                      # Android application module
│   ├── build.gradle
│   └── ...
│
├── data/                         # Example datasets, samples (gitignored)
├── ml/                           # Model training and evaluation notebooks/scripts
├── scripts/                      # Utility scripts (DB migrations, seeders, etc.)
├── docs/                         # Documentation
├── .gitignore
└── README.md
```

---

## Tech stack

- Backend: Flask (Python)
- Database: Firebase
- Frontend: Kotlin (Android Studio)
- Editor / IDE: Visual Studio Code (backend)
- Version Control: Git + GitHub
- Deployment: Render / Heroku (planned)

---

## Setup instructions (backend - VS Code)

1. Clone the repository:
   ```bash
   git clone https://github.com/Suzana121/Smart-Credit-Card-Transaction-Analysis.git
   cd Smart-Credit-Card-Transaction-Analysis/backend
   ```

2. Create a virtual environment:
   ```bash
   python3 -m venv venv
   ```

3. Activate the virtual environment:
   - macOS / Linux:
     ```bash
     source venv/bin/activate
     ```
   - Windows (PowerShell):
     ```powershell
     .\venv\Scripts\Activate.ps1
     ```

4. Install dependencies:
   ```bash
   pip install -r requirements.txt
   ```

5. (Optional) Open the backend in Visual Studio Code:
   ```bash
   code ..
   ```
   - Recommended: add a `.vscode/launch.json` for convenient debugging.

6. Run the Flask server (default port configured in app.py):
   ```bash
   python app.py
   ```

7. Open the app in your browser:
   ```
   http://localhost:5001
   ```

---


## Notes

- Sensitive files (e.g., Firebase credentials, trained models, virtual environments) should be added to `.gitignore` and not committed.
- This README will be kept up-to-date as the project progresses; next steps include integrating a trained ML model into the backend and connecting the Android frontend.

---
