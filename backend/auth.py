from flask import Blueprint, request, jsonify
from flask_bcrypt import Bcrypt
from flask_jwt_extended import create_access_token, jwt_required, get_jwt_identity

# ייבוא מופע ה-Firestore DB
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
        user_data['id'] = query[0].id
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

    # אם יש אימייל, נבדוק שהוא לא תפוס (אופציונלי אם אתם לא משתמשים בו)
    if email and find_user_in_db(email, 'email'):
        return jsonify({"error": "Email already exists"}), 400

    # בדיקה אם השם משתמש כבר תפוס
    if find_user_in_db(username, 'username'):
        return jsonify({"error": "Username already taken"}), 400

    hashed_pw = bcrypt.generate_password_hash(password).decode("utf-8")

    user_doc = {
        "username": username,
        "email": email or "", # שומרים ריק אם אין אימייל
        "phone": phone or "",
        "password": hashed_pw,
    }

    try:
        users_ref.add(user_doc)
        print("User registered successfully")
        return jsonify({"message": "User registered successfully"}), 201
    except Exception as e:
        print(f"Firestore error: {e}")
        return jsonify({"error": "Database write failed"}), 500


# --- התחברות (Login) - תומך כעת בשם משתמש! ---
@auth_bp.route("/login", methods=["POST"])
def login():
    data = request.get_json()
    print("----- LOGIN REQUEST -----", data)

    # ננסה לקחת שם משתמש, ואם אין אז ננסה אימייל
    username = data.get("username") or data.get("userName") or data.get("name")
    email = data.get("email")
    password = data.get("password")

    user = None

    # לוגיקה: אם יש שם משתמש, נחפש לפיו. אחרת נחפש לפי אימייל.
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

    # יצירת טוקן
    # משתמשים בשם המשתמש בתור הזהות בטוקן
    identity = user.get("username") or user.get("email")
    access_token = create_access_token(identity=identity)

    print("Login successful!")
    return jsonify({
        "success": True,
        "token": access_token,
        "message": "Login successful",
        "user": {
            "id": user["id"],
            "username": user.get("username"),
            "email": user.get("email")
        }
    }), 200