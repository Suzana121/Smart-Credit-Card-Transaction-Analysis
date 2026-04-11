from flask import Blueprint, request, jsonify
from flask_bcrypt import Bcrypt
from flask_jwt_extended import create_access_token, jwt_required, get_jwt_identity
from google.cloud import firestore
from firebase_config import users_ref, db
from functools import wraps

bcrypt = Bcrypt()
auth_bp = Blueprint("auth", __name__)

@auth_bp.record_once
def on_load(state):
    bcrypt.init_app(state.app)

# --- decorator לבדיקת הרשאות אדמין ---
def admin_required(fn):
    @wraps(fn)
    @jwt_required()
    def wrapper(*args, **kwargs):
        user_id = get_jwt_identity()
        user_doc = users_ref.document(user_id).get()
        if not user_doc.exists or user_doc.to_dict().get("role") != "admin":
            return jsonify({"error": "Admin access required"}), 403
        return fn(*args, **kwargs)
    return wrapper

# --- פונקציית עזר ---
def find_user_in_db(identifier_value, identifier_type='email'):
    try:
        query = users_ref.where(identifier_type, '==', identifier_value).limit(1).get()
        if not query:
            return None
        user_data = query[0].to_dict()
        user_data['id'] = query[0].id
        return user_data
    except Exception as e:
        print(f"Database error finding user: {e}")
        return None

# --- הרשמה ---
@auth_bp.route("/register", methods=["POST"])
def register():
    data = request.get_json()
    print("----- REGISTER REQUEST -----", data)

    username = data.get("username") or data.get("userName") or data.get("name")
    email = data.get("email")
    password = data.get("password")
    phone = data.get("phone") or data.get("phoneNumber")

    if not password or not username:
        return jsonify({"error": "Missing username or password"}), 400

    if email and find_user_in_db(email, 'email'):
        return jsonify({"error": "Email already exists"}), 400

    if find_user_in_db(username, 'username'):
        return jsonify({"error": "Username already taken"}), 400

    hashed_pw = bcrypt.generate_password_hash(password).decode("utf-8")

    user_doc = {
        "username": username,
        "email": email or "",
        "phone": phone or "",
        "password": hashed_pw,
        "profile_image": "https://www.w3schools.com/howto/img_avatar.png",
        "role": "user",  # כל משתמש חדש מקבל role של user
        "created_at": firestore.SERVER_TIMESTAMP
    }

    try:
        new_user_ref = users_ref.add(user_doc)
        print(f"User registered successfully. ID: {new_user_ref[1].id}")
        return jsonify({"message": "User registered successfully", "userId": new_user_ref[1].id}), 201
    except Exception as e:
        print(f"Firestore error: {e}")
        return jsonify({"error": "Database write failed"}), 500

# --- התחברות ---
@auth_bp.route("/login", methods=["POST"])
def login():
    data = request.get_json()
    print("----- LOGIN REQUEST -----", data)

    username = data.get("username") or data.get("userName") or data.get("name")
    email = data.get("email")
    password = data.get("password")

    user = None
    if username:
        user = find_user_in_db(username, 'username')
    elif email:
        user = find_user_in_db(email, 'email')
    else:
        return jsonify({"error": "Must provide username or email"}), 400

    if not user:
        return jsonify({"error": "Invalid credentials"}), 401

    if not bcrypt.check_password_hash(user["password"], password):
        return jsonify({"error": "Invalid credentials"}), 401

    user_id = user["id"]
    access_token = create_access_token(identity=user_id)

    return jsonify({
        "success": True,
        "token": access_token,
        "message": "Login successful",
        "user": {
            "id": user_id,
            "username": user.get("username"),
            "email": user.get("email"),
            "role": user.get("role", "user"),  # מחזירים את ה-role
            "profile_image": user.get("profile_image", "https://www.w3schools.com/howto/img_avatar.png")
        }
    }), 200

# --- פרטי משתמש ---
@auth_bp.route("/user_details", methods=["GET"])
@jwt_required()
def get_user_details():
    try:
        user_id = get_jwt_identity()
        user_doc = users_ref.document(user_id).get()

        if not user_doc.exists:
            return jsonify({"error": "User not found"}), 404

        user_data = user_doc.to_dict()
        return jsonify({
            "id": user_id,
            "username": user_data.get("username"),
            "email": user_data.get("email"),
            "phone": user_data.get("phone", ""),
            "role": user_data.get("role", "user"),  # מחזירים את ה-role
            "profile_image": user_data.get("profile_image", "")
        }), 200

    except Exception as e:
        return jsonify({"error": str(e)}), 500

# --- עדכון פרטי חשבון ---
@auth_bp.route("/update_account", methods=["POST"])
@jwt_required()
def update_account():
    try:
        user_id = get_jwt_identity()
        data = request.get_json()

        update_data = {
            "username": data.get("username"),
            "email": data.get("email"),
            "phone": data.get("phone")
        }

        if data.get("password"):
            hashed_pw = bcrypt.generate_password_hash(data.get("password")).decode("utf-8")
            update_data["password"] = hashed_pw

        users_ref.document(user_id).update(update_data)
        return jsonify({"success": True, "message": "Account updated successfully"}), 200

    except Exception as e:
        return jsonify({"success": False, "message": str(e)}), 500

# ---------------------------------------------------
# ADMIN ENDPOINT - דשבורד סטטיסטיקות של כל המשתמשים
# ---------------------------------------------------
@auth_bp.route("/admin/dashboard", methods=["GET"])
@admin_required
def admin_dashboard():
    try:
        # סטטיסטיקות כלליות
        all_users = users_ref.get()
        total_users = len(all_users)

        all_transactions = db.collection('transactions').get()
        total_transactions = len(all_transactions)

        irregular_transactions = [
            t for t in all_transactions
            if t.to_dict().get('status') == 'IRREGULAR'
        ]
        total_irregular = len(irregular_transactions)

        # עסקים שמדווחים הכי הרבה כחשודים
        suspicious_businesses = {}
        for t in irregular_transactions:
            data = t.to_dict()
            name = data.get('businessName', 'Unknown')
            suspicious_businesses[name] = suspicious_businesses.get(name, 0) + 1

        top_suspicious = sorted(
            [{"businessName": k, "count": v} for k, v in suspicious_businesses.items()],
            key=lambda x: x["count"],
            reverse=True
        )[:5]  # top 5

        # משתמשים עם הכי הרבה עסקאות חשודות
        user_irregular_count = {}
        for t in irregular_transactions:
            uid = t.to_dict().get('user_id', '')
            user_irregular_count[uid] = user_irregular_count.get(uid, 0) + 1

        top_users = []
        for uid, count in sorted(user_irregular_count.items(), key=lambda x: x[1], reverse=True)[:5]:
            user_doc = users_ref.document(uid).get()
            if user_doc.exists:
                username = user_doc.to_dict().get('username', 'Unknown')
                top_users.append({"username": username, "irregularCount": count})

        # התפלגות קטגוריות בכל המערכת
        category_totals = {}
        for t in all_transactions:
            data = t.to_dict()
            cat = data.get('category', 'Other')
            amt = float(data.get('amount', 0))
            category_totals[cat] = category_totals.get(cat, 0) + amt

        categories = [
            {"category": k, "amount": v}
            for k, v in category_totals.items()
        ]

        return jsonify({
            "totalUsers": total_users,
            "totalTransactions": total_transactions,
            "totalIrregular": total_irregular,
            "irregularRate": round((total_irregular / total_transactions * 100), 1) if total_transactions > 0 else 0,
            "topSuspiciousBusinesses": top_suspicious,
            "topUsersWithIrregular": top_users,
            "categoryBreakdown": categories
        }), 200

    except Exception as e:
        print(f"Admin dashboard error: {e}")
        return jsonify({"error": str(e)}), 500

# --- חברים ---
@auth_bp.route('/friends', methods=['GET'])
@jwt_required()
def get_all_friendships():
    try:
        current_user_id = get_jwt_identity()
        friends_dict = {}

        sent_query = db.collection('friendships').where('user_id', '==', current_user_id).get()
        for doc in sent_query:
            f_data = doc.to_dict()
            friend_id = f_data.get('friend_id')
            friend_doc = users_ref.document(friend_id).get()
            if friend_doc.exists:
                u_info = friend_doc.to_dict()
                phone = u_info.get("phone")
                status = f_data.get("status")
                final_status = f"sent_{status}" if status == "pending" else status
                friends_dict[phone] = {
                    "name": u_info.get("username"),
                    "phone": phone,
                    "status": final_status,
                    "photoUrl": u_info.get("profile_image")
                }

        received_query = db.collection('friendships').where('friend_id', '==', current_user_id).get()
        for doc in received_query:
            f_data = doc.to_dict()
            sender_id = f_data.get('user_id')
            sender_doc = users_ref.document(sender_id).get()
            if sender_doc.exists:
                u_info = sender_doc.to_dict()
                phone = u_info.get("phone")
                status = f_data.get("status")
                final_status = f"received_{status}" if status == "pending" else status
                friends_dict[phone] = {
                    "name": u_info.get("username"),
                    "phone": phone,
                    "status": final_status,
                    "photoUrl": u_info.get("profile_image")
                }

        return jsonify(list(friends_dict.values())), 200
    except Exception as e:
        return jsonify({"error": str(e)}), 500

@auth_bp.route('/add-friend', methods=['POST'])
@jwt_required()
def add_friend():
    try:
        data = request.get_json()
        friend_phone = data.get('phone')
        current_user_id = get_jwt_identity()

        friend_query = users_ref.where('phone', '==', friend_phone).limit(1).get()
        if not friend_query:
            return jsonify({"error": "User with this phone not found"}), 404

        friend_id = friend_query[0].id
        if friend_id == current_user_id:
            return jsonify({"error": "You cannot add yourself as a friend"}), 400

        existing_check = db.collection('friendships') \
            .where('user_id', '==', current_user_id) \
            .where('friend_id', '==', friend_id).get()
        if existing_check:
            return jsonify({"error": "Friend request already exists"}), 400

        db.collection('friendships').add({
            "user_id": current_user_id,
            "friend_id": friend_id,
            "status": "pending",
            "created_at": firestore.SERVER_TIMESTAMP
        })
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

        friend_query = users_ref.where('phone', '==', friend_phone).limit(1).get()
        if not friend_query:
            return jsonify({"error": "User not found"}), 404

        friend_id = friend_query[0].id
        friendship_docs = db.collection('friendships') \
            .where('user_id', '==', friend_id) \
            .where('friend_id', '==', current_user_id).get()

        for doc in friendship_docs:
            doc.reference.update({"status": "approved"})

        return jsonify({"message": "Friend confirmed"}), 200
    except Exception as e:
        return jsonify({"error": str(e)}), 500

@auth_bp.route('/delete-friend-smart', methods=['POST'])
@jwt_required()
def delete_friend_smart():
    data = request.get_json()
    friend_phone = data.get('phone')
    delete_sent = data.get('delete_sent', False)
    delete_received = data.get('delete_received', False)
    current_user_id = get_jwt_identity()

    friend_query = db.collection('users').where('phone', '==', friend_phone).limit(1).get()
    if not friend_query:
        return jsonify({"error": "Not found"}), 404
    friend_id = friend_query[0].id

    friendship_docs = db.collection('friendships') \
        .where('user_id', 'in', [current_user_id, friend_id]) \
        .where('friend_id', 'in', [current_user_id, friend_id]).get()
    for doc in friendship_docs:
        doc.reference.delete()

    if delete_sent:
        sent_shares = db.collection('shares').where('sender_id', '==', current_user_id).where('receiver_id', '==', friend_id).get()
        for doc in sent_shares:
            doc.reference.delete()

    if delete_received:
        received_shares = db.collection('shares').where('sender_id', '==', friend_id).where('receiver_id', '==', current_user_id).get()
        for doc in received_shares:
            doc.reference.delete()

    return jsonify({"success": True}), 200

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
            "lastLocation": {"latitude": latitude, "longitude": longitude}
        })
        return jsonify({"success": True, "message": "Location updated"}), 200
    except Exception as e:
        return jsonify({"success": False, "message": str(e)}), 500

@auth_bp.route('/search_user/<phone>', methods=['GET'])
@jwt_required()
def search_user(phone):
    try:
        user_docs = users_ref.where('phone', '==', phone).limit(1).get()
        if not user_docs:
            return jsonify({"error": "User not found"}), 404

        user_data = user_docs[0].to_dict()
        return jsonify({
            "id": user_docs[0].id,
            "username": user_data.get('username'),
            "phone": user_data.get('phone'),
            "profile_image": user_data.get('profile_image', "https://www.w3schools.com/howto/img_avatar.png")
        }), 200
    except Exception as e:
        return jsonify({"error": str(e)}), 500

RAW_CATEGORIES = {
    "Food": ["מסעדה", "מקדונלד", "קפה", "פיצה", "סופר", "ארומה", "וולט", "תן ביס", "שופרסל", "יוחננוף", "wolt", "super", "pizza", "רמי לוי", "קונדיטוריה", "המבורגר", "בורגר"],
    "Health": ["פארם", "pharm", "מרפאה", "כללית", "מכבי", "doctor", "בי", "be", "בית מרקחת", "גוד פארם"],
    "Shopping": ["זארה", "zara", "h&m", "אמזון", "amazon", "ksp", "אייבורי", "עזריאלי", "shein", "שיין", "הלבשה"],
    "Transport": ["דלק", "פז", "סונול", "דור אלון", "paz", "sonol", "רכבת", "אוטובוס", "מונית", "taxi", "gettaxi", "פנגו", "pango", "תחבורה"],
    "Education": ["אוניברסיטה", "university", "college", "טכניון", "לימודים", "המכללה", "קורס"]
}

def classify_transaction(description):
    if not description:
        return "Other"
    desc_lowered = description.lower()
    for category, keywords in RAW_CATEGORIES.items():
        for keyword in keywords:
            if keyword.lower() in desc_lowered:
                return category
    return "Other"

@auth_bp.route('/stats/<month>', methods=['GET'])
@jwt_required()
def get_monthly_stats(month):
    user_id = get_jwt_identity()
    transactions_query = db.collection('transactions').where('user_id', '==', user_id).get()

    category_map = {}
    total_spend = 0

    month_to_num = {
        "Jan": "01", "Feb": "02", "Mar": "03", "Apr": "04",
        "May": "05", "Jun": "06", "Jul": "07", "Aug": "08",
        "Sep": "09", "Oct": "10", "Nov": "11", "Dec": "12"
    }
    target_month = month_to_num.get(month)

    for doc in transactions_query:
        t = doc.to_dict()
        date_str = t.get('date', "")
        if date_str and date_str.split('-')[1] == target_month:
            amt = float(t.get('amount', 0))
            raw_name = t.get('businessName') or t.get('category') or ""
            clean_category = classify_transaction(raw_name)
            total_spend += amt
            category_map[clean_category] = category_map.get(clean_category, 0) + amt

    expenses_by_category = [
        {"category": name, "amount": amt}
        for name, amt in category_map.items()
    ]

    return jsonify({
        "totalSpend": total_spend,
        "expensesByCategory": expenses_by_category
    }), 200