from flask import Blueprint, request, jsonify
from flask_bcrypt import Bcrypt
from flask_jwt_extended import create_access_token, jwt_required, get_jwt_identity
from google.cloud import firestore

# ייבוא מופע ה-Firestore DB מתוך הקובץ הקיים שלך
from firebase_config import users_ref,db

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


# --- שליפת פרטי המשתמש המחובר ---
@auth_bp.route("/user_details", methods=["GET"])
@jwt_required() # מחייב שהאפליקציה תשלח את ה-Token שהיא קיבלה ב-Login
def get_user_details():
    try:
        # חילוץ ה-ID של המשתמש מתוך ה-Token
        user_id = get_jwt_identity()

        # שליפת המסמך מ-Firestore
        user_doc = users_ref.document(user_id).get()

        if not user_doc.exists:
            return jsonify({"error": "User not found"}), 404

        user_data = user_doc.to_dict()

        # החזרת הנתונים בפורמט שה-Android מצפה לו (לפי המודל שיצרנו)
        return jsonify({
            "id": user_id,
            "username": user_data.get("username"),
            "email": user_data.get("email"),
            "phone": user_data.get("phone", ""),
            "profile_image": user_data.get("profile_image", "")
        }), 200

    except Exception as e:
        print(f"Error fetching user details: {e}")
        return jsonify({"error": str(e)}), 500

# --- עדכון פרטי חשבון ---
@auth_bp.route("/update_account", methods=["POST"])
@jwt_required()
def update_account():
    try:
        user_id = get_jwt_identity()
        data = request.get_json()

        # הכנת המילון לעדכון
        update_data = {
            "username": data.get("username"),
            "email": data.get("email"),
            "phone": data.get("phone")
        }

        # אם נשלחה סיסמה חדשה, נגבב (Hash) אותה ונעדכן
        if data.get("password"):
            hashed_pw = bcrypt.generate_password_hash(data.get("password")).decode("utf-8")
            update_data["password"] = hashed_pw

        # עדכון ב-Firestore
        users_ref.document(user_id).update(update_data)

        return jsonify({"success": True, "message": "Account updated successfully"}), 200

    except Exception as e:
        print(f"Error updating account: {e}")
        return jsonify({"success": False, "message": str(e)}), 500




@auth_bp.route('/friends', methods=['GET'])
@jwt_required()
def get_all_friendships():
    try:
        current_user_id = get_jwt_identity()
        friends_dict = {} # מילון למניעת כפילויות

        # 1. שליפת בקשות שאני שלחתי (אני ה-user_id)
        sent_query = db.collection('friendships').where('user_id', '==', current_user_id).get()
        for doc in sent_query:
            f_data = doc.to_dict()
            friend_id = f_data.get('friend_id')
            friend_doc = users_ref.document(friend_id).get()
            if friend_doc.exists:
                u_info = friend_doc.to_dict()
                phone = u_info.get("phone")
                status = f_data.get("status")

                # אם הסטטוס הוא pending, נסמן שזה "שלחתי"
                final_status = f"sent_{status}" if status == "pending" else status

                friends_dict[phone] = {
                    "name": u_info.get("username"),
                    "phone": phone,
                    "status": final_status,
                    "photoUrl": u_info.get("profile_image")
                }

        # 2. שליפת בקשות שנשלחו אליי (אני ה-friend_id)
        received_query = db.collection('friendships').where('friend_id', '==', current_user_id).get()
        for doc in received_query:
            f_data = doc.to_dict()
            sender_id = f_data.get('user_id')
            sender_doc = users_ref.document(sender_id).get()
            if sender_doc.exists:
                u_info = sender_doc.to_dict()
                phone = u_info.get("phone")
                status = f_data.get("status")

                # אם הסטטוס הוא pending, נסמן שזה "קיבלתי"
                final_status = f"received_{status}" if status == "pending" else status

                # הכנסה למילון (אם כבר קיים כ'sent' ומאושר, זה לא יידרס בצורה שתפריע)
                friends_dict[phone] = {
                    "name": u_info.get("username"),
                    "phone": phone,
                    "status": final_status,
                    "photoUrl": u_info.get("profile_image")
                }

        return jsonify(list(friends_dict.values())), 200
    except Exception as e:
        print(f"Error fetching friends: {e}")
        return jsonify({"error": str(e)}), 500

@auth_bp.route('/add-friend', methods=['POST'])
@jwt_required()
def add_friend():
    try:
        data = request.get_json()
        friend_phone = data.get('phone')
        current_user_id = get_jwt_identity()

        # 1. מציאת המשתמש שאנחנו רוצים להוסיף לפי הטלפון שלו
        friend_query = users_ref.where('phone', '==', friend_phone).limit(1).get()

        if not friend_query:
            return jsonify({"error": "User with this phone not found"}), 404

        friend_id = friend_query[0].id

        # 2. בדיקה: האם המשתמש מנסה להוסיף את עצמו?
        if friend_id == current_user_id:
            return jsonify({"error": "You cannot add yourself as a friend"}), 400

        # 3. בדיקה האם כבר קיימת בקשה (כדי למנוע כפילויות)
        existing_check = db.collection('friendships') \
            .where('user_id', '==', current_user_id) \
            .where('friend_id', '==', friend_id).get()

        if existing_check:
            return jsonify({"error": "Friend request already exists"}), 400

        # 4. יצירת מסמך חברות חדש
        friendship_data = {
            "user_id": current_user_id,
            "friend_id": friend_id,
            "status": "pending",
            "created_at": firestore.SERVER_TIMESTAMP
        }

        db.collection('friendships').add(friendship_data)
        return jsonify({"message": "Friend request sent successfully"}), 200
    except Exception as e:
        return jsonify({"error": str(e)}), 500

@auth_bp.route('/confirm-friend', methods=['POST'])
@jwt_required()
def confirm_friend():
    try:
        data = request.get_json()
        friend_phone = data.get('phone')
        current_user_id = get_jwt_identity()

        # מציאת ה-ID של החבר לפי הטלפון
        friend_query = users_ref.where('phone', '==', friend_phone).limit(1).get()
        if not friend_query:
            return jsonify({"error": "User not found"}), 404

        friend_id = friend_query[0].id

        # עדכון הסטטוס ל-'approved' במסמך החברות הרלוונטי
        friendship_docs = db.collection('friendships') \
            .where('user_id', '==', friend_id) \
            .where('friend_id', '==', current_user_id).get()

        for doc in friendship_docs:
            doc.reference.update({"status": "approved"})

        return jsonify({"message": "Friend confirmed"}), 200
    except Exception as e:
        return jsonify({"error": str(e)}), 500


@auth_bp.route('/delete-friend', methods=['POST'])
@jwt_required()
def delete_friend():
    try:
        data = request.get_json()
        friend_phone = data.get('phone')
        current_user_id = get_jwt_identity()

        # מוצאים את ה-ID של החבר לפי הטלפון
        friend_query = users_ref.where('phone', '==', friend_phone).limit(1).get()
        if not friend_query:
            return jsonify({"error": "User not found"}), 404

        friend_id = friend_query[0].id

        # מחפשים את מסמך החברות (משני הצדדים לביטחון) ומוחקים
        friendship_docs = db.collection('friendships') \
            .where('user_id', 'in', [current_user_id, friend_id]) \
            .where('friend_id', 'in', [current_user_id, friend_id]).get()

        for doc in friendship_docs:
            doc.reference.delete()

        return jsonify({"success": True, "message": "Friend deleted"}), 200
    except Exception as e:
        return jsonify({"error": str(e)}), 500

@auth_bp.route('/update_location', methods=['POST'])
@jwt_required()
def update_location():
    try:
        user_id = get_jwt_identity()
        data = request.get_json()
        latitude = data.get('latitude')
        longitude = data.get('longitude')

        if latitude is None or longitude is None:
            return jsonify({"error": "latitude and longitude are required"}), 400

        users_ref.document(user_id).update({
            "lastLocation": {
                "latitude": latitude,
                "longitude": longitude
            }
        })

        return jsonify({"success": True, "message": "Location updated"}), 200
    except Exception as e:
        print(f"Error updating location: {e}")
        return jsonify({"success": False, "message": str(e)}), 500


@auth_bp.route('/search_user/<phone>', methods=['GET'])
@jwt_required()
def search_user(phone):
    user_doc = db.collection('users').where('phone', '==', phone).limit(1).get()
    if not user_doc:
        return jsonify({"error": "User not found"}), 404

    user_data = user_doc[0].to_dict()
    return jsonify({
        "username": user_data.get('username'),
        "phone": user_data.get('phone')
    }), 200