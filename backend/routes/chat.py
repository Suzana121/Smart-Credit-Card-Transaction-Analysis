from flask import Blueprint, request, jsonify
from flask_jwt_extended import jwt_required, get_jwt_identity
from firebase_config import db, users_ref
from google.cloud import firestore

chat_bp = Blueprint('chat', __name__)

# --- שליפת הודעות בין שני משתמשים ---
@chat_bp.route('/messages/<share_id>', methods=['GET'])
@jwt_required()
def get_messages(share_id):
    try:
        messages_ref = db.collection('messages') \
            .where('shareId', '==', share_id) \
            .order_by('timestamp', direction=firestore.Query.ASCENDING) \
            .stream()

        messages = []
        for doc in messages_ref:
            m = doc.to_dict()
            messages.append({
                'id': doc.id,
                'shareId': m.get('shareId', ''),
                'senderId': m.get('senderId', ''),
                'senderName': m.get('senderName', ''),
                'text': m.get('text', ''),
                'timestamp': m.get('timestamp').isoformat() if hasattr(m.get('timestamp'), 'isoformat') else str(m.get('timestamp', ''))
            })

        return jsonify(messages), 200

    except Exception as e:
        print(f"Error fetching messages: {e}")
        return jsonify({"error": str(e)}), 500


# --- שליחת הודעה ---
@chat_bp.route('/messages', methods=['POST'])
@jwt_required()
def send_message():
    try:
        user_id = get_jwt_identity()
        data = request.get_json()

        share_id = data.get('shareId')
        text = data.get('text', '').strip()

        if not share_id or not text:
            return jsonify({"error": "Missing shareId or text"}), 400

        # בדיקה שהמשתמש שייך לשיתוף הזה
        share_doc = db.collection('shares').document(share_id).get()
        if not share_doc.exists:
            return jsonify({"error": "Share not found"}), 404

        share_data = share_doc.to_dict()

        # שליפת הטלפון של המשתמש הנוכחי
        user_doc = users_ref.document(user_id).get()
        if not user_doc.exists:
            return jsonify({"error": "User not found"}), 404

        user_data = user_doc.to_dict()
        my_phone = user_data.get('phone', '')
        sender_name = user_data.get('username', 'Unknown')

        # בדיקה שהמשתמש הוא חלק מהשיתוף
        if my_phone != share_data.get('sharedBy') and my_phone != share_data.get('sharedWith'):
            return jsonify({"error": "Unauthorized"}), 403

        # שמירת ההודעה
        msg_ref = db.collection('messages').document()
        msg_ref.set({
            'shareId': share_id,
            'senderId': user_id,
            'senderName': sender_name,
            'text': text,
            'timestamp': firestore.SERVER_TIMESTAMP
        })

        return jsonify({"success": True, "id": msg_ref.id}), 201

    except Exception as e:
        print(f"Error sending message: {e}")
        return jsonify({"error": str(e)}), 500