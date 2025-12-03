# Smart Credit Card Transaction Analysis

This project is the backend for a system that analyzes and detects anomalies in credit card transactions using machine learning.

This is the initial setup stage, built with Flask.  
In later stages, it will be connected to a React frontend and a trained ML model.

---

## Project Structure

```
Smart-Credit-Card-Transaction-Analysis/
│
├── backend/
│   ├── app.py                  # Main Flask application
│   ├── requirements.txt        # Dependencies
│   ├── venv/                   # Virtual environment (not pushed to Git)
│   └── ...
│
└── README.md
```

---

## Tech Stack

- Backend: Flask (Python)
- Database: MongoDB (planned for later phase)
- Frontend: React (planned)
- Version Control: Git + GitHub
- Deployment: Render / Heroku (planned)

---

## Setup Instructions

1. Clone the repository:
   ```bash
   git clone https://github.com/YOUR_USERNAME/Smart-Credit-Card-Transaction-Analysis.git
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
   - Windows:
     ```bash
     venv\Scripts\activate
     ```

4. Install dependencies:
   ```bash
   pip install -r requirements.txt
   ```

5. Run the Flask server:
   ```bash
   python app.py
   ```

6. Open the app in your browser:
   ```
   http://localhost:5000
   ```

---

## Notes

- This is the first stage of the backend setup.
- Future steps will include:
  - Authentication system (login/register)
  - Database integration
  - ML model connection
  - API endpoints for file upload and analysis
````
