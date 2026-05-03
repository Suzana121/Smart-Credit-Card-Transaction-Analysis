from flask import Blueprint, request, jsonify
from flask_jwt_extended import jwt_required, get_jwt_identity
from firebase_config import db, users_ref
from google.cloud import firestore
from google.cloud.firestore_v1.base_query import FieldFilter

chat_bp = Blueprint('chat', __name__)


def get_user_info(user_id):
    """שליפת פרטי משתמש."""
    doc = users_ref.document(user_id).get()
    if doc.exists:
        d = doc.to_dict()
        return {'id': user_id, 'username': d.get('username', 'Unknown'),
                'phone': d.get('phone', '')}
    return None


def fmt_ts(ts):
    """המרת Firestore timestamp ל-ISO 8601 תקני עם T."""
    if ts is None:
        return ''
    if hasattr(ts, 'isoformat'):
        return ts.isoformat().replace(' ', 'T')
    return ''


# ─────────────────────────────────────────────
# GET /api/chats — רשימת שיחות
# ─────────────────────────────────────────────
@chat_bp.route('/chats', methods=['GET'])
@jwt_required()
def get_chats():
    try:
        user_id = get_jwt_identity()
        docs = (db.collection('chats')
                .where(filter=FieldFilter('participants', 'array_contains', user_id))
                .stream())

        chats = []
        for doc in docs:
            d      = doc.to_dict()
            unread = d.get('unreadCount', {}).get(user_id, 0)
            chats.append({
                'id':               doc.id,
                'participants':     d.get('participants', []),
                'participantNames': d.get('participantNames', {}),
                'isGroup':          d.get('isGroup', False),
                'groupName':        d.get('groupName', ''),
                'lastMessage':      d.get('lastMessage', ''),
                'lastMessageAt':    fmt_ts(d.get('lastMessageAt')),
                'unreadCount':      unread,
            })

        chats.sort(key=lambda x: x.get('lastMessageAt', ''), reverse=True)
        return jsonify(chats), 200
    except Exception as e:
        import traceback; print(traceback.format_exc())
        return jsonify({'error': str(e)}), 500


# ─────────────────────────────────────────────
# POST /api/chats — יצירת שיחה/קבוצה
# ─────────────────────────────────────────────
@chat_bp.route('/chats', methods=['POST'])
@jwt_required()
def create_chat():
    try:
        user_id = get_jwt_identity()
        data    = request.get_json()

        participant_ids  = data.get('participantIds', [])
        group_name       = data.get('groupName', '')
        is_group         = len(participant_ids) > 1
        all_participants = list(set([user_id] + participant_ids))

        if not is_group and len(all_participants) == 2:
            existing = (db.collection('chats')
                        .where(filter=FieldFilter('participants', 'array_contains', user_id))
                        .stream())
            for doc in existing:
                d = doc.to_dict()
                if (not d.get('isGroup') and
                        set(d.get('participants', [])) == set(all_participants)):
                    return jsonify({'id': doc.id, 'existed': True}), 200

        participant_names = {}
        for pid in all_participants:
            info = get_user_info(pid)
            if info:
                participant_names[pid] = info['username']

        chat_ref = db.collection('chats').document()
        chat_ref.set({
            'participants':     all_participants,
            'participantNames': participant_names,
            'isGroup':          is_group,
            'groupName':        group_name if is_group else '',
            'lastMessage':      '',
            'lastMessageAt':    firestore.SERVER_TIMESTAMP,
            'unreadCount':      {pid: 0 for pid in all_participants},
            'createdBy':        user_id,
            'createdAt':        firestore.SERVER_TIMESTAMP,
        })

        return jsonify({'id': chat_ref.id, 'existed': False}), 201
    except Exception as e:
        import traceback; print(traceback.format_exc())
        return jsonify({'error': str(e)}), 500


# ─────────────────────────────────────────────
# GET /api/chats/<id>/messages — הודעות
# ─────────────────────────────────────────────
@chat_bp.route('/chats/<chat_id>/messages', methods=['GET'])
@jwt_required()
def get_messages(chat_id):
    try:
        user_id  = get_jwt_identity()
        chat_doc = db.collection('chats').document(chat_id).get()

        if not chat_doc.exists:
            return jsonify({'error': 'Chat not found'}), 404
        if user_id not in chat_doc.to_dict().get('participants', []):
            return jsonify({'error': 'Unauthorized'}), 403

        db.collection('chats').document(chat_id).update({
            f'unreadCount.{user_id}': 0
        })

        docs = (db.collection('chats').document(chat_id)
                .collection('messages')
                .order_by('timestamp', direction=firestore.Query.ASCENDING)
                .stream())

        messages = []
        for doc in docs:
            d = doc.to_dict()
            messages.append({
                'id':             doc.id,
                'senderId':       d.get('senderId', ''),
                'senderName':     d.get('senderName', ''),
                'text':           d.get('text', ''),
                'transaction':    d.get('transaction'),
                'replyToId':      d.get('replyToId'),
                'replyToMessage': d.get('replyToMessage'),
                'audioUrl':       d.get('audioUrl'),       # ← הודעה קולית
                'audioDuration':  d.get('audioDuration', 0),
                'timestamp':      fmt_ts(d.get('timestamp')),
            })

        return jsonify(messages), 200
    except Exception as e:
        import traceback; print(traceback.format_exc())
        return jsonify({'error': str(e)}), 500


# ─────────────────────────────────────────────
# POST /api/chats/<id>/messages — שליחת הודעה
# ─────────────────────────────────────────────
@chat_bp.route('/chats/<chat_id>/messages', methods=['POST'])
@jwt_required()
def send_message(chat_id):
    try:
        user_id  = get_jwt_identity()
        data     = request.get_json()

        chat_ref = db.collection('chats').document(chat_id)
        chat_doc = chat_ref.get()
        if not chat_doc.exists:
            return jsonify({'error': 'Chat not found'}), 404

        participants = chat_doc.to_dict().get('participants', [])
        if user_id not in participants:
            return jsonify({'error': 'Unauthorized'}), 403

        user_info   = get_user_info(user_id)
        sender_name = user_info['username'] if user_info else 'Unknown'

        text             = data.get('text', '')
        transaction      = data.get('transaction')
        reply_to_id      = data.get('replyToId')
        reply_to_message = data.get('replyToMessage')
        audio_url        = data.get('audioUrl')        # ← הודעה קולית
        audio_duration   = data.get('audioDuration', 0)

        msg_data = {
            'senderId':    user_id,
            'senderName':  sender_name,
            'text':        text,
            'transaction': transaction,
            'timestamp':   firestore.SERVER_TIMESTAMP,
        }

        if reply_to_id:
            msg_data['replyToId'] = reply_to_id
        if reply_to_message:
            msg_data['replyToMessage'] = reply_to_message
        if audio_url:
            msg_data['audioUrl']      = audio_url
            msg_data['audioDuration'] = audio_duration

        msg_ref = chat_ref.collection('messages').document()
        msg_ref.set(msg_data)

        # lastMessage — טקסט או תיאור הודעה קולית
        if audio_url:
            last_msg = '🎤 Voice message'
        elif text:
            last_msg = text
        else:
            last_msg = '📊 Transaction shared'

        update = {
            'lastMessage':   last_msg,
            'lastMessageAt': firestore.SERVER_TIMESTAMP,
        }
        for pid in participants:
            if pid != user_id:
                update[f'unreadCount.{pid}'] = firestore.Increment(1)

        chat_ref.update(update)

        return jsonify({'id': msg_ref.id}), 201
    except Exception as e:
        import traceback; print(traceback.format_exc())
        return jsonify({'error': str(e)}), 500


# ─────────────────────────────────────────────
# GET /api/chats/unread
# ─────────────────────────────────────────────
@chat_bp.route('/chats/unread', methods=['GET'])
@jwt_required()
def get_unread_count():
    try:
        user_id = get_jwt_identity()
        docs    = (db.collection('chats')
                   .where(filter=FieldFilter('participants', 'array_contains', user_id))
                   .stream())

        total_unread = sum(
            doc.to_dict().get('unreadCount', {}).get(user_id, 0)
            for doc in docs
        )
        return jsonify({'unread': total_unread}), 200
    except Exception as e:
        return jsonify({'error': str(e)}), 500