from flask import Blueprint, request, jsonify
from flask_bcrypt import Bcrypt
from flask_jwt_extended import create_access_token, jwt_required, get_jwt_identity
from google.cloud import firestore

# ייבוא מופע ה-Firestore DB מתוך הקובץ הקיים שלך
from firebase_config import users_ref

bcrypt = Bcrypt()
auth_bp = Blueprint("auth", __name__)

@auth_bp.record_once
def on_load(state):
    bcrypt.init_app(state.app)

# --- פונקציית עזר גמישה: מוצאת משתמש לפי אימייל או שם משתמש ---
def find_user_in_db(identifier_value, identifier_type='email'):
    try:
        # identifier_type יהיה 'email' או 'username'
        query = users_ref.where(identifier_type, '==', identifier_value).limit(1).get()
        if not query:
            return None

        user_data = query[0].to_dict()
        user_data['id'] = query[0].id # חילוץ ה-ID הייחודי של המסמך
        return user_data

    except Exception as e:
        print(f"Database error finding user: {e}")
        return None

# --- הרשמה (Register) ---
@auth_bp.route("/register", methods=["POST"])
def register():
    data = request.get_json()
    print("----- REGISTER REQUEST -----", data)

    # קליטת נתונים
    username = data.get("username") or data.get("userName") or data.get("name")
    email = data.get("email")
    password = data.get("password")
    phone = data.get("phone") or data.get("phoneNumber")

    if not password or not username:
        return jsonify({"error": "Missing username or password"}), 400

    # בדיקה שהאימייל לא תפוס
    if email and find_user_in_db(email, 'email'):
        return jsonify({"error": "Email already exists"}), 400

    # בדיקה שהשם משתמש לא תפוס
    if find_user_in_db(username, 'username'):
        return jsonify({"error": "Username already taken"}), 400

    hashed_pw = bcrypt.generate_password_hash(password).decode("utf-8")

    # יצירת אובייקט משתמש כולל שדה תמונה ומזהה
    user_doc = {
        "username": username,
        "email": email or "",
        "phone": phone or "",
        "password": hashed_pw,
        "profile_image": "https://www.w3schools.com/howto/img_avatar.png", # תמונת ברירת מחדל
        "created_at": firestore.SERVER_TIMESTAMP # חותמת זמן של שרת פיירבייס
    }

    try:
        # שמירה ב-Firestore
        new_user_ref = users_ref.add(user_doc)
        print(f"User registered successfully. ID: {new_user_ref[1].id}")
        return jsonify({"message": "User registered successfully", "userId": new_user_ref[1].id}), 201
    except Exception as e:
        print(f"Firestore error: {e}")
        return jsonify({"error": "Database write failed"}), 500


# --- התחברות (Login) ---
@auth_bp.route("/login", methods=["POST"])
def login():
    data = request.get_json()
    print("----- LOGIN REQUEST -----", data)

    username = data.get("username") or data.get("userName") or data.get("name")
    email = data.get("email")
    password = data.get("password")

    user = None

    if username:
        print(f"Searching user by username: {username}")
        user = find_user_in_db(username, 'username')
    elif email:
        print(f"Searching user by email: {email}")
        user = find_user_in_db(email, 'email')
    else:
        return jsonify({"error": "Must provide username or email"}), 400

    # בדיקת סיסמה
    if not user:
        print("User not found in DB")
        return jsonify({"error": "Invalid credentials"}), 401

    if not bcrypt.check_password_hash(user["password"], password):
        print("Wrong password")
        return jsonify({"error": "Invalid credentials"}), 401

    # יצירת טוקן - משתמשים ב-ID הייחודי של המשתמש כזהות!
    user_id = user["id"]
    access_token = create_access_token(identity=user_id)

    print(f"Login successful for user: {user_id}")
    return jsonify({
        "success": True,
        "token": access_token,
        "message": "Login successful",
        "user": {
            "id": user_id,
            "username": user.get("username"),
            "email": user.get("email"),
            "profile_image": user.get("profile_image", "https://www.w3schools.com/howto/img_avatar.png")
        }
    }), 200