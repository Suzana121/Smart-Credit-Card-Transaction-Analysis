import firebase_admin
from firebase_admin import credentials, firestore
import os

SERVICE_ACCOUNT_KEY_PATH = 'serviceAccountKey.json'

# בדיקה אם Firebase כבר אותחל
if not firebase_admin._apps:
    try:
        cred = credentials.Certificate(SERVICE_ACCOUNT_KEY_PATH)
        firebase_admin.initialize_app(cred)
        print("Firebase Admin SDK initialized successfully.")
    except Exception as e:
        print(f"Error initializing Firebase Admin SDK: {e}")

# מופע ה-Firestore DB
db = firestore.client()

# אוסף המשתמשים
users_ref = db.collection('users')