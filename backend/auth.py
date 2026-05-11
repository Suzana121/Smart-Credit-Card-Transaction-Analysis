from flask import Blueprint, request, jsonify
from flask_bcrypt import Bcrypt
from flask_jwt_extended import create_access_token, jwt_required, get_jwt_identity
from google.cloud import firestore
from google.cloud.firestore_v1.base_query import FieldFilter
import cloudinary
import cloudinary.uploader
import smtplib
import random
import string
import os
import re
from datetime import datetime, timedelta, timezone
from email.mime.text import MIMEText
from email.mime.multipart import MIMEMultipart

from firebase_config import users_ref, db

bcrypt  = Bcrypt()
auth_bp = Blueprint("auth", __name__)

cloudinary.config(
    cloud_name = os.environ.get("CLOUDINARY_CLOUD_NAME"),
    api_key    = os.environ.get("CLOUDINARY_API_KEY"),
    api_secret = os.environ.get("CLOUDINARY_API_SECRET"),
    secure     = True
)

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

@auth_bp.route("/register", methods=["POST"])
def register():
    data     = request.get_json()
    username = data.get("username") or data.get("userName") or data.get("name")
    email    = data.get("email")
    password = data.get("password")
    phone    = data.get("phone") or data.get("phoneNumber")

    if not password or not username:
        return jsonify({"error": "Missing username or password"}), 400
    if email and find_user_in_db(email, 'email'):
        return jsonify({"error": "This email is already registered"}), 409
    if phone and find_user_in_db(phone, 'phone'):
        return jsonify({"error": "This phone number is already registered"}), 409
    if find_user_in_db(username, 'username'):
        return jsonify({"error": "Username already taken"}), 400

    hashed_pw = bcrypt.generate_password_hash(password).decode("utf-8")
    user_doc  = {
        "username": username, "email": email or "", "phone": phone or "",
        "password": hashed_pw, "profile_image": "", "created_at": firestore.SERVER_TIMESTAMP
    }
    try:
        new_user_ref = users_ref.add(user_doc)
        return jsonify({"message": "User registered successfully", "userId": new_user_ref[1].id}), 201
    except Exception as e:
        return jsonify({"error": "Database write failed"}), 500

@auth_bp.route("/login", methods=["POST"])
def login():
    data     = request.get_json()
    username = data.get("username") or data.get("userName") or data.get("name")
    email    = data.get("email")
    password = data.get("password")
    user     = None

    if username:   user = find_user_in_db(username, 'username')
    elif email:    user = find_user_in_db(email, 'email')
    else:          return jsonify({"error": "Must provide username or email"}), 400

    if not user or not bcrypt.check_password_hash(user["password"], password):
        return jsonify({"error": "Invalid credentials"}), 401

    access_token = create_access_token(identity=user["id"])
    return jsonify({
        "success": True, "token": access_token, "message": "Login successful",
        "user": {
            "id": user["id"], "username": user.get("username"),
            "email": user.get("email"), "profile_image": user.get("profile_image", "")
        }
    }), 200

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
            "id": user_id, "username": user_data.get("username"),
            "email": user_data.get("email"), "phone": user_data.get("phone", ""),
            "profile_image": user_data.get("profile_image", "")
        }), 200
    except Exception as e:
        return jsonify({"error": str(e)}), 500

@auth_bp.route("/update_account", methods=["POST"])
@jwt_required()
def update_account():
    try:
        user_id      = get_jwt_identity()
        data         = request.get_json()
        new_name     = data.get("username", "").strip()
        new_email    = data.get("email", "").strip()
        new_phone    = data.get("phone", "").strip()
        new_pass     = data.get("password")
        new_photo    = data.get("profile_image")

        current_doc  = users_ref.document(user_id).get()
        if not current_doc.exists:
            return jsonify({"error": "User not found"}), 404
        current_data = current_doc.to_dict()
        old_name     = current_data.get("username", "")

        if new_email and new_email != current_data.get("email", ""):
            if users_ref.where("email", "==", new_email).limit(1).get():
                return jsonify({"error": "This email is already in use by another account"}), 409

        if new_phone and new_phone != current_data.get("phone", ""):
            if users_ref.where("phone", "==", new_phone).limit(1).get():
                return jsonify({"error": "This phone number is already in use by another account"}), 409

        update_data = {}
        if new_name:  update_data["username"]      = new_name
        if new_email: update_data["email"]          = new_email
        if new_phone: update_data["phone"]          = new_phone
        if new_photo: update_data["profile_image"]  = new_photo
        if new_pass:  update_data["password"]       = bcrypt.generate_password_hash(new_pass).decode("utf-8")

        if not update_data:
            return jsonify({"message": "Nothing to update"}), 200

        # ✅ עדכון שם המשתמש
        users_ref.document(user_id).update(update_data)

        # ✅ עדכון participantNames בכל הצ'אטים שהמשתמש משתתף בהם
        if new_name and new_name != old_name:
            chats = db.collection('chats') \
                .where(filter=FieldFilter('participants', 'array_contains', user_id)) \
                .stream()
            for chat_doc in chats:
                chat_data = chat_doc.to_dict()
                participant_names = chat_data.get('participantNames', {})
                if user_id in participant_names:
                    chat_doc.reference.update({
                        f'participantNames.{user_id}': new_name
                    })

        return jsonify({"success": True, "message": "Account updated successfully"}), 200
    except Exception as e:
        return jsonify({"success": False, "message": str(e)}), 500

@auth_bp.route("/upload_profile_image", methods=["POST"])
@jwt_required()
def upload_profile_image():
    try:
        if "file" not in request.files:
            return jsonify({"error": "No file provided"}), 400

        file    = request.files["file"]
        user_id = get_jwt_identity()

        result = cloudinary.uploader.upload(
            file,
            folder        = "profile_images",
            public_id     = f"user_{user_id}",
            overwrite     = True,
            resource_type = "image",
            transformation = [
                {"width": 400, "height": 400, "crop": "fill", "gravity": "face"}
            ]
        )

        url = result.get("secure_url")
        users_ref.document(user_id).update({"profile_image": url})
        return jsonify({"url": url}), 200

    except Exception as e:
        print(f"Profile image upload error: {e}")
        return jsonify({"error": str(e)}), 500

@auth_bp.route('/friends', methods=['GET'])
@jwt_required()
def get_all_friendships():
    try:
        current_user_id = get_jwt_identity()
        friends_dict    = {}
        for doc in db.collection('friendships').where('user_id', '==', current_user_id).get():
            f_data = doc.to_dict()
            fd     = users_ref.document(f_data.get('friend_id')).get()
            if fd.exists:
                u = fd.to_dict(); phone = u.get("phone"); status = f_data.get("status")
                friends_dict[phone] = {"name": u.get("username"), "phone": phone,
                                       "status": f"sent_{status}" if status == "pending" else status,
                                       "photoUrl": u.get("profile_image", "")}
        for doc in db.collection('friendships').where('friend_id', '==', current_user_id).get():
            f_data = doc.to_dict()
            sd     = users_ref.document(f_data.get('user_id')).get()
            if sd.exists:
                u = sd.to_dict(); phone = u.get("phone"); status = f_data.get("status")
                friends_dict[phone] = {"name": u.get("username"), "phone": phone,
                                       "status": f"received_{status}" if status == "pending" else status,
                                       "photoUrl": u.get("profile_image", "")}
        return jsonify(list(friends_dict.values())), 200
    except Exception as e:
        return jsonify({"error": str(e)}), 500

@auth_bp.route('/add-friend', methods=['POST'])
@jwt_required()
def add_friend():
    try:
        data = request.get_json(); friend_phone = data.get('phone'); current_user_id = get_jwt_identity()
        fq = users_ref.where('phone', '==', friend_phone).limit(1).get()
        if not fq: return jsonify({"error": "User with this phone not found"}), 404
        friend_id = fq[0].id
        if friend_id == current_user_id: return jsonify({"error": "You cannot add yourself as a friend"}), 400
        if db.collection('friendships').where('user_id','==',current_user_id).where('friend_id','==',friend_id).get():
            return jsonify({"error": "Friend request already exists"}), 400
        db.collection('friendships').add({"user_id": current_user_id, "friend_id": friend_id,
                                          "status": "pending", "created_at": firestore.SERVER_TIMESTAMP})
        return jsonify({"message": "Friend request sent successfully"}), 200
    except Exception as e:
        return jsonify({"error": str(e)}), 500

@auth_bp.route('/confirm-friend', methods=['POST'])
@jwt_required()
def confirm_friend():
    try:
        data = request.get_json(); friend_phone = data.get('phone'); current_user_id = get_jwt_identity()
        fq = users_ref.where('phone', '==', friend_phone).limit(1).get()
        if not fq: return jsonify({"error": "User not found"}), 404
        friend_id = fq[0].id
        for doc in db.collection('friendships').where('user_id','==',friend_id).where('friend_id','==',current_user_id).get():
            doc.reference.update({"status": "approved"})
        return jsonify({"message": "Friend confirmed"}), 200
    except Exception as e:
        return jsonify({"error": str(e)}), 500

@auth_bp.route('/delete-friend-smart', methods=['POST'])
@jwt_required()
def delete_friend_smart():
    data = request.get_json(); friend_phone = data.get('phone')
    delete_sent = data.get('delete_sent', False); delete_received = data.get('delete_received', False)
    current_user_id = get_jwt_identity()
    fq = db.collection('users').where('phone', '==', friend_phone).limit(1).get()
    if not fq: return jsonify({"error": "Not found"}), 404
    friend_id = fq[0].id
    for doc in db.collection('friendships').where('user_id','in',[current_user_id,friend_id]).where('friend_id','in',[current_user_id,friend_id]).get():
        doc.reference.delete()
    if delete_sent:
        for doc in db.collection('shares').where('sender_id','==',current_user_id).where('receiver_id','==',friend_id).get():
            doc.reference.delete()
    if delete_received:
        for doc in db.collection('shares').where('sender_id','==',friend_id).where('receiver_id','==',current_user_id).get():
            doc.reference.delete()
    return jsonify({"success": True}), 200

@auth_bp.route('/search_user/<phone>', methods=['GET'])
@jwt_required()
def search_user(phone):
    try:
        user_docs = users_ref.where('phone', '==', phone).limit(1).get()
        if not user_docs: return jsonify({"error": "User not found"}), 404
        user_data = user_docs[0].to_dict()
        return jsonify({"id": user_docs[0].id, "username": user_data.get('username'),
                        "phone": user_data.get('phone'), "profile_image": user_data.get('profile_image', "")}), 200
    except Exception as e:
        return jsonify({"error": str(e)}), 500

def _generate_otp(length=6) -> str:
    return ''.join(random.choices(string.digits, k=length))

def _send_reset_email(to_email: str, otp: str) -> bool:
    sender = os.environ.get("MAIL_USERNAME"); password = os.environ.get("MAIL_PASSWORD")
    if not sender or not password: raise ValueError("Mail credentials not configured in .env")
    msg = MIMEMultipart("alternative"); msg["Subject"] = "Cardify - Reset Your Password"
    msg["From"] = f"Cardify <{sender}>"; msg["To"] = to_email
    html = f"""<div style="font-family:Arial,sans-serif;max-width:480px;margin:auto;">
        <h2 style="color:#4A90E2;">Reset Your Password</h2>
        <p>Use the code below to reset your Cardify password:</p>
        <div style="font-size:36px;font-weight:bold;letter-spacing:8px;color:#4A90E2;
            padding:20px;text-align:center;background:#f0f4ff;border-radius:8px;">{otp}</div>
        <p style="color:#888;font-size:13px;margin-top:20px;">
            This code expires in 10 minutes.<br>
            If you didn't request this, you can safely ignore this email.</p></div>"""
    msg.attach(MIMEText(html, "html"))
    with smtplib.SMTP("smtp.gmail.com", 587) as server:
        server.starttls(); server.login(sender, password); server.sendmail(sender, to_email, msg.as_string())
    return True

@auth_bp.route('/forgot-password', methods=['POST'])
def forgot_password():
    try:
        data = request.get_json(); email = data.get('email', '').strip().lower()
        if not email or not re.match(r'^[^@]+@[^@]+\.[^@]+$', email):
            return jsonify({"error": "Invalid email address"}), 400
        user_doc = next(iter(users_ref.where('email', '==', email).limit(1).get()), None)
        if not user_doc:
            return jsonify({"success": True, "message": "If this email exists, a code was sent"}), 200
        otp = _generate_otp(); expires_at = datetime.now(timezone.utc) + timedelta(minutes=10)
        db.collection('password_reset_otps').document(email).set(
            {"otp": otp, "expires_at": expires_at, "user_id": user_doc.id, "used": False})
        _send_reset_email(email, otp)
        return jsonify({"success": True, "message": "Reset code sent to your email"}), 200
    except Exception as e:
        return jsonify({"error": "Failed to send reset email"}), 500

@auth_bp.route('/reset-password', methods=['POST'])
def reset_password():
    try:
        data = request.get_json()
        email = data.get('email', '').strip().lower(); otp = data.get('otp', '').strip()
        new_password = data.get('newPassword', '')
        if not all([email, otp, new_password]): return jsonify({"error": "Missing required fields"}), 400
        if len(new_password) < 6: return jsonify({"error": "Password must be at least 6 characters"}), 400
        otp_doc = db.collection('password_reset_otps').document(email).get()
        if not otp_doc.exists: return jsonify({"error": "Invalid or expired reset code"}), 400
        otp_data = otp_doc.to_dict()
        if otp_data.get('used'): return jsonify({"error": "This code has already been used"}), 400
        if otp_data.get('otp') != otp: return jsonify({"error": "Incorrect reset code"}), 400
        expires_at = otp_data.get('expires_at')
        if expires_at:
            now = datetime.now(timezone.utc)
            exp = expires_at if hasattr(expires_at, 'tzinfo') and expires_at.tzinfo else expires_at
            if now > exp: return jsonify({"error": "Reset code has expired"}), 400
        hashed = bcrypt.generate_password_hash(new_password).decode('utf-8')
        users_ref.document(otp_data.get('user_id')).update({"password": hashed})
        db.collection('password_reset_otps').document(email).update({"used": True})
        return jsonify({"success": True, "message": "Password updated successfully"}), 200
    except Exception as e:
        return jsonify({"error": "Failed to reset password"}), 500

@auth_bp.route('/admin/dashboard', methods=['GET'])
@jwt_required()
def admin_dashboard():
    try:
        user_id  = get_jwt_identity()
        user_doc = users_ref.document(user_id).get()
        if not user_doc.exists:
            return jsonify({"error": "User not found"}), 403
        user_data = user_doc.to_dict()
        if not user_data.get("is_admin", False):
            return jsonify({"error": "Unauthorized"}), 403
        total_users        = len(list(users_ref.stream()))
        total_transactions = 0
        for user in users_ref.stream():
            for file_doc in db.collection('users').document(user.id).collection('files').stream():
                total_transactions += file_doc.to_dict().get('transaction_count', 0)
        return jsonify({
            "totalUsers":        total_users,
            "totalTransactions": total_transactions,
        }), 200
    except Exception as e:
        return jsonify({"error": str(e)}), 500