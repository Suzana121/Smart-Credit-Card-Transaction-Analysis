from flask import Blueprint, request, jsonify
from flask_bcrypt import Bcrypt
from flask_jwt_extended import create_access_token, jwt_required, get_jwt_identity

# ייבוא מופע ה-Firestore DB ואוסף המשתמשים
from firebase_config import users_ref 

bcrypt = Bcrypt()
auth_bp = Blueprint("auth", __name__)

# --- אין צורך יותר ברשימת users זמנית ---
# users = [] 
# ----------------------------------------

@auth_bp.record_once
def on_load(state):
    bcrypt.init_app(state.app)

# פונקציית עזר למציאת משתמש ב-Firestore
def find_user_by_email(email):
    # Firestore לא מאפשר מציאה ישירה לפי אינדקס (כמו ID) עבור שדה 'email', 
    # לכן משתמשים בשאילתה.
    try:
        query = users_ref.where('email', '==', email).limit(1).get()
        if not query:
            return None
        
        # החזרת המסמך הראשון שנמצא (אם קיים)
        user_data = query[0].to_dict()
        user_data['id'] = query[0].id # הוספת ה-ID של המסמך
        return user_data

    except Exception as e:
        print(f"Database error finding user: {e}")
        return None


@auth_bp.route("/register", methods=["POST"])
def register():
    data = request.get_json()
    username = data.get("username")
    email = data.get("email")
    password = data.get("password")
    phone = data.get("phone") # <--- הוספת השורה הזו כדי לקלוט את הטלפון מהאפליקציה

    # בדיקה אם המשתמש כבר קיים
    if find_user_by_email(email):
        return jsonify({"error": "User already exists"}), 400

    # הצפנת סיסמה
    hashed_pw = bcrypt.generate_password_hash(password).decode("utf-8")

    # יצירת אובייקט המשתמש לשמירה ב-Firestore
    user_doc = {
        "username": username, 
        "email": email, 
        "phone": phone,
        "password": hashed_pw,
    }
    
    try:
        # שמירת המשתמש
        users_ref.add(user_doc)
    except Exception as e:
        print(f"Firestore add error: {e}")
        return jsonify({"error": "Database write failed"}), 500

    return jsonify({"message": "User registered successfully"}), 201

@auth_bp.route("/login", methods=["POST"])
def login():
    data = request.get_json()
    email = data.get("email")
    password = data.get("password")

    # מציאת המשתמש ב-Firestore
    user = find_user_by_email(email) 
    
    # אימות: אם לא נמצא משתמש או שהסיסמה לא תואמת
    if not user or not bcrypt.check_password_hash(user["password"], password):
        return jsonify({"error": "Invalid credentials"}), 401

    # יצירת טוקן JWT
    access_token = create_access_token(identity=user["email"])
    
    # זה ה-JSON שהאפליקציה שלך צריכה כדי לעבור למסך הבא:
    return jsonify({
        "success": True,
        "token": access_token,
        "message": "Login successful",
        "user": {
            "id": user["id"],
            "email": user["email"],
            "name": user.get("username") or user.get("name") or "User" # בדיקה כפולה למניעת null
        }
    }), 200


