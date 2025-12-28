import firebase_admin
from firebase_admin import credentials, firestore
import os

# ******* איתור מפתח השירות *******
# נניח שקובץ המפתח נמצא בתיקייה הראשית של הפרויקט (ליד firebase_config.py)
# או נשתמש בנתיב המלא כפי שצריך להיות:
SERVICE_ACCOUNT_KEY_PATH = 'serviceAccountKey.json' 
# או אם הקובץ יושב בתיקיית backend (לפי מבנה ה-JS שניסינו קודם):
# SERVICE_ACCOUNT_KEY_PATH = 'backend/serviceAccountKey.json' 
# אנא ודאי את הנתיב הנכון לפי המיקום הסופי שבחרת לקובץ serviceAccountKey.json

# בדיקה אם Firebase כבר אותחל
if not firebase_admin._apps:
    try:
        # אתחול עם מפתח השירות
        cred = credentials.Certificate(SERVICE_ACCOUNT_KEY_PATH)
        firebase_admin.initialize_app(cred)
        print("Firebase Admin SDK initialized successfully.")
    except Exception as e:
        print(f"Error initializing Firebase Admin SDK: {e}")
        # אם יש בעיה בנתיב או בקובץ, האפליקציה תיכשל

# מופע ה-Firestore DB
db = firestore.client()

# אוסף המשתמשים
users_ref = db.collection('users')