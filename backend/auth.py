from flask import Blueprint, request, jsonify
from flask_bcrypt import Bcrypt
from flask_jwt_extended import create_access_token, jwt_required, get_jwt_identity
from google.cloud import firestore
import smtplib
import random
import string
import os
import re
from datetime import datetime, timedelta, timezone
from email.mime.text import MIMEText
from email.mime.multipart import MIMEMultipart

from firebase_config import users_ref, db

bcrypt = Bcrypt()
auth_bp = Blueprint("auth", __name__)

@auth_bp.record_once
def on_load(state):
    bcrypt.init_app(state.app)

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


# ─────────────────────────────────────────────
# הרשמה (Register)
# ─────────────────────────────────────────────
@auth_bp.route("/register", methods=["POST"])
def register():
    data = request.get_json()
    print("----- REGISTER REQUEST -----", data)

    username = data.get("username") or data.get("userName") or data.get("name")
    email    = data.get("email")
    password = data.get("password")
    phone    = data.get("phone") or data.get("phoneNumber")

    if not password or not username:
        return jsonify({"error": "Missing username or password"}), 400

    if email and find_user_in_db(email, 'email'):
        return jsonify({"error": "Email already exists"}), 400

    if find_user_in_db(username, 'username'):
        return jsonify({"error": "Username already taken"}), 400

    hashed_pw = bcrypt.generate_password_hash(password).decode("utf-8")

    user_doc = {
        "username":      username,
        "email":         email or "",
        "phone":         phone or "",
        "password":      hashed_pw,
        "profile_image": "https://www.w3schools.com/howto/img_avatar.png",
        "created_at":    firestore.SERVER_TIMESTAMP
    }

    try:
        new_user_ref = users_ref.add(user_doc)
        print(f"User registered successfully. ID: {new_user_ref[1].id}")
        return jsonify({"message": "User registered successfully", "userId": new_user_ref[1].id}), 201
    except Exception as e:
        print(f"Firestore error: {e}")
        return jsonify({"error": "Database write failed"}), 500


# ─────────────────────────────────────────────
# התחברות (Login)
# ─────────────────────────────────────────────
@auth_bp.route("/login", methods=["POST"])
def login():
    data = request.get_json()
    print("----- LOGIN REQUEST -----", data)

    username = data.get("username") or data.get("userName") or data.get("name")
    email    = data.get("email")
    password = data.get("password")
    user     = None

    if username:
        print(f"Searching user by username: {username}")
        user = find_user_in_db(username, 'username')
    elif email:
        print(f"Searching user by email: {email}")
        user = find_user_in_db(email, 'email')
    else:
        return jsonify({"error": "Must provide username or email"}), 400

    if not user:
        print("User not found in DB")
        return jsonify({"error": "Invalid credentials"}), 401

    if not bcrypt.check_password_hash(user["password"], password):
        print("Wrong password")
        return jsonify({"error": "Invalid credentials"}), 401

    user_id      = user["id"]
    access_token = create_access_token(identity=user_id)

    print(f"Login successful for user: {user_id}")
    return jsonify({
        "success": True,
        "token":   access_token,
        "message": "Login successful",
        "user": {
            "id":            user_id,
            "username":      user.get("username"),
            "email":         user.get("email"),
            "profile_image": user.get("profile_image", "https://www.w3schools.com/howto/img_avatar.png")
        }
    }), 200


# ─────────────────────────────────────────────
# שליפת פרטי המשתמש המחובר
# ─────────────────────────────────────────────
@auth_bp.route("/user_details", methods=["GET"])
@jwt_required()
def get_user_details():
    try:
        user_id  = get_jwt_identity()
        user_doc = users_ref.document(user_id).get()

        if not user_doc.exists:
            return jsonify({"error": "User not found"}), 404

        user_data = user_doc.to_dict()
        return jsonify({
            "id":            user_id,
            "username":      user_data.get("username"),
            "email":         user_data.get("email"),
            "phone":         user_data.get("phone", ""),
            "profile_image": user_data.get("profile_image", "")
        }), 200
    except Exception as e:
        print(f"Error fetching user details: {e}")
        return jsonify({"error": str(e)}), 500


# ─────────────────────────────────────────────
# עדכון פרטי חשבון
# ─────────────────────────────────────────────
@auth_bp.route("/update_account", methods=["POST"])
@jwt_required()
def update_account():
    try:
        user_id     = get_jwt_identity()
        data        = request.get_json()
        update_data = {
            "username": data.get("username"),
            "email":    data.get("email"),
            "phone":    data.get("phone")
        }

        if data.get("password"):
            hashed_pw               = bcrypt.generate_password_hash(data.get("password")).decode("utf-8")
            update_data["password"] = hashed_pw

        users_ref.document(user_id).update(update_data)
        return jsonify({"success": True, "message": "Account updated successfully"}), 200
    except Exception as e:
        print(f"Error updating account: {e}")
        return jsonify({"success": False, "message": str(e)}), 500


# ─────────────────────────────────────────────
# חברים (Friends)
# ─────────────────────────────────────────────
@auth_bp.route('/friends', methods=['GET'])
@jwt_required()
def get_all_friendships():
    try:
        current_user_id = get_jwt_identity()
        friends_dict    = {}

        sent_query = db.collection('friendships').where('user_id', '==', current_user_id).get()
        for doc in sent_query:
            f_data     = doc.to_dict()
            friend_id  = f_data.get('friend_id')
            friend_doc = users_ref.document(friend_id).get()
            if friend_doc.exists:
                u_info       = friend_doc.to_dict()
                phone        = u_info.get("phone")
                status       = f_data.get("status")
                final_status = f"sent_{status}" if status == "pending" else status
                friends_dict[phone] = {
                    "name":     u_info.get("username"),
                    "phone":    phone,
                    "status":   final_status,
                    "photoUrl": u_info.get("profile_image")
                }

        received_query = db.collection('friendships').where('friend_id', '==', current_user_id).get()
        for doc in received_query:
            f_data     = doc.to_dict()
            sender_id  = f_data.get('user_id')
            sender_doc = users_ref.document(sender_id).get()
            if sender_doc.exists:
                u_info       = sender_doc.to_dict()
                phone        = u_info.get("phone")
                status       = f_data.get("status")
                final_status = f"received_{status}" if status == "pending" else status
                friends_dict[phone] = {
                    "name":     u_info.get("username"),
                    "phone":    phone,
                    "status":   final_status,
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
        data            = request.get_json()
        friend_phone    = data.get('phone')
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
            "user_id":    current_user_id,
            "friend_id":  friend_id,
            "status":     "pending",
            "created_at": firestore.SERVER_TIMESTAMP
        })
        return jsonify({"message": "Friend request sent successfully"}), 200
    except Exception as e:
        return jsonify({"error": str(e)}), 500


@auth_bp.route('/confirm-friend', methods=['POST'])
@jwt_required()
def confirm_friend():
    try:
        data            = request.get_json()
        friend_phone    = data.get('phone')
        current_user_id = get_jwt_identity()

        friend_query = users_ref.where('phone', '==', friend_phone).limit(1).get()
        if not friend_query:
            return jsonify({"error": "User not found"}), 404

        friend_id       = friend_query[0].id
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
    data            = request.get_json()
    friend_phone    = data.get('phone')
    delete_chat     = data.get('delete_chat', False)
    current_user_id = get_jwt_identity()

    # שליפת הטלפון שלי כדי למצוא שיתופים
    user_doc = users_ref.document(current_user_id).get()
    user_phone = user_doc.to_dict().get('phone')

    friend_query = db.collection('users').where('phone', '==', friend_phone).limit(1).get()
    if not friend_query:
        return jsonify({"error": "Not found"}), 404
    friend_id = friend_query[0].id

    # 1. מחיקת החברות (זה תמיד קורה)
    friendship_docs = db.collection('friendships') \
        .where('user_id', 'in', [current_user_id, friend_id]) \
        .where('friend_id', 'in', [current_user_id, friend_id]).get()
    for doc in friendship_docs:
        doc.reference.delete()

    # 2. אם המשתמש בחר למחוק את הצאט (הסתרה עבורי)
    if delete_chat:
        # הסתרת הצאט
        chats = db.collection('chats').where('participants', 'array_contains', current_user_id).get()
        for chat in chats:
            participants = chat.to_dict().get('participants', [])
            if friend_id in participants and not chat.to_dict().get('isGroup', False):
                chat.reference.update({
                    "hidden_for": firestore.ArrayUnion([current_user_id])
                })

        # הסתרת השיתופים ב-SharedInfoScreen (עבורי בלבד)
        all_shares = db.collection('shares').get() # אפשר לייעל עם query, אבל זה הכי בטוח
        for s in all_shares:
            d = s.to_dict()
            # אם אני הצד ששלח או קיבל והצד השני הוא החבר שנמחק
            if (d.get('sharedBy') == user_phone and d.get('sharedWith') == friend_phone) or \
                    (d.get('sharedBy') == friend_phone and d.get('sharedWith') == user_phone):
                s.reference.update({
                    "hidden_for": firestore.ArrayUnion([current_user_id])
                })

    return jsonify({"success": True}), 200


@auth_bp.route('/search_user/<phone>', methods=['GET'])
@jwt_required()
def search_user(phone):
    try:
        user_docs = users_ref.where('phone', '==', phone).limit(1).get()

        if not user_docs:
            return jsonify({"error": "User not found"}), 404

        user_data = user_docs[0].to_dict()
        user_id   = user_docs[0].id

        return jsonify({
            "id":            user_id,
            "username":      user_data.get('username'),
            "phone":         user_data.get('phone'),
            "profile_image": user_data.get('profile_image', "https://www.w3schools.com/howto/img_avatar.png")
        }), 200
    except Exception as e:
        return jsonify({"error": str(e)}), 500


# ─────────────────────────────────────────────
# שירות מייל + routes לאיפוס סיסמה
# ─────────────────────────────────────────────

def _generate_otp(length=6) -> str:
    return ''.join(random.choices(string.digits, k=length))


def _send_reset_email(to_email: str, otp: str) -> bool:
    sender   = os.environ.get("MAIL_USERNAME")
    password = os.environ.get("MAIL_PASSWORD")

    if not sender or not password:
        raise ValueError("Mail credentials not configured in .env")

    msg            = MIMEMultipart("alternative")
    msg["Subject"] = "Cardify - Reset Your Password"
    msg["From"]    = f"Cardify <{sender}>"
    msg["To"]      = to_email

    html = f"""
    <div style="font-family: Arial, sans-serif; max-width: 480px; margin: auto;">
        <h2 style="color: #4A90E2;">Reset Your Password</h2>
        <p>Use the code below to reset your Cardify password:</p>
        <div style="font-size: 36px; font-weight: bold; letter-spacing: 8px;
                    color: #4A90E2; padding: 20px; text-align: center;
                    background: #f0f4ff; border-radius: 8px;">
            {otp}
        </div>
        <p style="color: #888; font-size: 13px; margin-top: 20px;">
            This code expires in 10 minutes.<br>
            If you didn't request this, you can safely ignore this email.
        </p>
    </div>
    """
    msg.attach(MIMEText(html, "html"))

    with smtplib.SMTP("smtp.gmail.com", 587) as server:
        server.starttls()
        server.login(sender, password)
        server.sendmail(sender, to_email, msg.as_string())
    return True


@auth_bp.route('/forgot-password', methods=['POST'])
def forgot_password():
    try:
        data  = request.get_json()
        email = data.get('email', '').strip().lower()

        if not email or not re.match(r'^[^@]+@[^@]+\.[^@]+$', email):
            return jsonify({"error": "Invalid email address"}), 400

        users    = users_ref.where('email', '==', email).limit(1).get()
        user_doc = next(iter(users), None)

        if not user_doc:
            return jsonify({"success": True, "message": "If this email exists, a code was sent"}), 200

        otp        = _generate_otp()
        expires_at = datetime.now(timezone.utc) + timedelta(minutes=10)

        db.collection('password_reset_otps').document(email).set({
            "otp":        otp,
            "expires_at": expires_at,
            "user_id":    user_doc.id,
            "used":       False
        })

        _send_reset_email(email, otp)

        return jsonify({"success": True, "message": "Reset code sent to your email"}), 200

    except Exception as e:
        print(f"Forgot password error: {e}")
        return jsonify({"error": "Failed to send reset email"}), 500


@auth_bp.route('/reset-password', methods=['POST'])
def reset_password():
    try:
        data         = request.get_json()
        email        = data.get('email', '').strip().lower()
        otp          = data.get('otp', '').strip()
        new_password = data.get('newPassword', '')

        if not all([email, otp, new_password]):
            return jsonify({"error": "Missing required fields"}), 400

        if len(new_password) < 6:
            return jsonify({"error": "Password must be at least 6 characters"}), 400

        otp_doc = db.collection('password_reset_otps').document(email).get()

        if not otp_doc.exists:
            return jsonify({"error": "Invalid or expired reset code"}), 400

        otp_data = otp_doc.to_dict()

        if otp_data.get('used'):
            return jsonify({"error": "This code has already been used"}), 400

        if otp_data.get('otp') != otp:
            return jsonify({"error": "Incorrect reset code"}), 400

        # תיקון השוואת תאריכים עם timezone
        expires_at = otp_data.get('expires_at')
        if expires_at is not None:
            now = datetime.now(timezone.utc)
            if hasattr(expires_at, 'tzinfo') and expires_at.tzinfo is not None:
                if now > expires_at:
                    return jsonify({"error": "Reset code has expired"}), 400
            else:
                if datetime.utcnow() > expires_at:
                    return jsonify({"error": "Reset code has expired"}), 400

        user_id = otp_data.get('user_id')
        hashed  = bcrypt.generate_password_hash(new_password).decode('utf-8')
        users_ref.document(user_id).update({"password": hashed})

        db.collection('password_reset_otps').document(email).update({"used": True})

        return jsonify({"success": True, "message": "Password updated successfully"}), 200

    except Exception as e:
        print(f"Reset password error: {e}")
        return jsonify({"error": "Failed to reset password"}), 500