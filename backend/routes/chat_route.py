from flask import Blueprint, request, jsonify
from flask_jwt_extended import jwt_required, get_jwt_identity
from firebase_config import db, users_ref
from google.cloud import firestore
from google.cloud.firestore_v1.base_query import FieldFilter

chat_bp = Blueprint('chat', __name__)


def get_user_info(user_id):
    doc = users_ref.document(user_id).get()
    if doc.exists:
        d = doc.to_dict()
        return {
            'id':            user_id,
            'username':      d.get('username', 'Unknown'),
            'phone':         d.get('phone', ''),
            'profile_image': d.get('profile_image', '')
        }
    return None


def get_contact_nickname(user_id, other_phone):
    doc = users_ref.document(user_id) \
        .collection('contacts') \
        .document(other_phone).get()
    if doc.exists:
        return doc.to_dict().get('nickname', '')
    return ''


def fmt_ts(ts):
    if ts is None:
        return ''
    if hasattr(ts, 'isoformat'):
        return ts.isoformat().replace(' ', 'T')
    return ''


def serialize_message(doc):
    d = doc.to_dict()
    if d.get('deleted', False):
        return {
            'id': doc.id, 'senderId': d.get('senderId', ''),
            'senderName': d.get('senderName', ''), 'text': '',
            'deleted': True, 'forwarded': False, 'transaction': None,
            'replyToId': None, 'replyToMessage': None,
            'audioUrl': None, 'audioDuration': 0,
            'reactions': {}, 'timestamp': fmt_ts(d.get('timestamp')),
        }
    return {
        'id':             doc.id,
        'senderId':       d.get('senderId', ''),
        'senderName':     d.get('senderName', ''),
        'text':           d.get('text', ''),
        'deleted':        False,
        'forwarded':      d.get('forwarded', False),
        'transaction':    d.get('transaction'),
        'replyToId':      d.get('replyToId'),
        'replyToMessage': d.get('replyToMessage'),
        'audioUrl':       d.get('audioUrl'),
        'audioDuration':  d.get('audioDuration', 0),
        'reactions':      d.get('reactions', {}),
        'timestamp':      fmt_ts(d.get('timestamp')),
    }


# ─── GET /api/chats ───────────────────────────────────────────
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
            d            = doc.to_dict()
            unread       = d.get('unreadCount', {}).get(user_id, 0)
            participants = d.get('participants', [])

            participant_names  = d.get('participantNames', {})
            display_names      = {}
            participant_photos = {}

            for pid in participants:
                if pid == user_id:
                    continue
                other_info  = get_user_info(pid)
                other_phone = other_info['phone'] if other_info else ''
                nickname    = get_contact_nickname(user_id, other_phone) if other_phone else ''
                display_names[pid] = nickname if nickname else participant_names.get(pid, 'Unknown')

                if other_info and other_info.get('profile_image'):
                    participant_photos[pid] = other_info['profile_image']

            chats.append({
                'id':                doc.id,
                'participants':      participants,
                'participantNames':  participant_names,
                'displayNames':      display_names,
                'participantPhotos': participant_photos,
                'isGroup':           d.get('isGroup', False),
                'groupName':         d.get('groupName', ''),
                'lastMessage':       d.get('lastMessage', ''),
                'lastMessageAt':     fmt_ts(d.get('lastMessageAt')),
                'unreadCount':       unread,
            })
        chats.sort(key=lambda x: x.get('lastMessageAt', ''), reverse=True)
        return jsonify(chats), 200
    except Exception as e:
        import traceback; print(traceback.format_exc())
        return jsonify({'error': str(e)}), 500


# ─── POST /api/chats ──────────────────────────────────────────
@chat_bp.route('/chats', methods=['POST'])
@jwt_required()
def create_chat():
    try:
        user_id          = get_jwt_identity()
        data             = request.get_json()
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


# ─── GET /api/chats/<id>/messages ────────────────────────────
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
        db.collection('chats').document(chat_id).update(
            {f'unreadCount.{user_id}': 0})
        docs = (db.collection('chats').document(chat_id)
                .collection('messages')
                .order_by('timestamp', direction=firestore.Query.ASCENDING)
                .stream())
        return jsonify([serialize_message(doc) for doc in docs]), 200
    except Exception as e:
        import traceback; print(traceback.format_exc())
        return jsonify({'error': str(e)}), 500


# ─── POST /api/chats/<id>/messages ───────────────────────────
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
        audio_url        = data.get('audioUrl')
        audio_duration   = data.get('audioDuration', 0)
        forwarded        = data.get('forwarded', False)

        msg_data = {
            'senderId':    user_id,
            'senderName':  sender_name,
            'text':        text,
            'transaction': transaction,
            'deleted':     False,
            'forwarded':   forwarded,
            'reactions':   {},
            'timestamp':   firestore.SERVER_TIMESTAMP,
        }
        if reply_to_id:      msg_data['replyToId']      = reply_to_id
        if reply_to_message: msg_data['replyToMessage'] = reply_to_message
        if audio_url:
            msg_data['audioUrl']      = audio_url
            msg_data['audioDuration'] = audio_duration

        msg_ref = chat_ref.collection('messages').document()
        msg_ref.set(msg_data)

        if audio_url:   last_msg = '🎤 Voice message'
        elif forwarded: last_msg = '↪ Forwarded message'
        elif text:      last_msg = text
        else:           last_msg = '📊 Transaction shared'

        update = {'lastMessage': last_msg, 'lastMessageAt': firestore.SERVER_TIMESTAMP}
        for pid in participants:
            if pid != user_id:
                update[f'unreadCount.{pid}'] = firestore.Increment(1)
        chat_ref.update(update)
        return jsonify({'id': msg_ref.id}), 201
    except Exception as e:
        import traceback; print(traceback.format_exc())
        return jsonify({'error': str(e)}), 500


# ─── DELETE /api/chats/<id>/messages/<msg_id> ────────────────
@chat_bp.route('/chats/<chat_id>/messages/<message_id>', methods=['DELETE'])
@jwt_required()
def delete_message(chat_id, message_id):
    try:
        user_id  = get_jwt_identity()
        chat_ref = db.collection('chats').document(chat_id)
        chat_doc = chat_ref.get()
        if not chat_doc.exists:
            return jsonify({'error': 'Chat not found'}), 404
        if user_id not in chat_doc.to_dict().get('participants', []):
            return jsonify({'error': 'Unauthorized'}), 403
        msg_ref = chat_ref.collection('messages').document(message_id)
        msg_doc = msg_ref.get()
        if not msg_doc.exists:
            return jsonify({'error': 'Message not found'}), 404
        if msg_doc.to_dict().get('senderId') != user_id:
            return jsonify({'error': 'You can only delete your own messages'}), 403
        msg_ref.update({'deleted': True, 'text': '', 'audioUrl': None,
                        'transaction': None, 'reactions': {}})
        return jsonify({'success': True}), 200
    except Exception as e:
        import traceback; print(traceback.format_exc())
        return jsonify({'error': str(e)}), 500


# ─── POST /api/chats/<id>/messages/<msg_id>/react ────────────
@chat_bp.route('/chats/<chat_id>/messages/<message_id>/react', methods=['POST'])
@jwt_required()
def react_to_message(chat_id, message_id):
    try:
        user_id  = get_jwt_identity()
        data     = request.get_json()
        emoji    = data.get('emoji', '')
        chat_ref = db.collection('chats').document(chat_id)
        chat_doc = chat_ref.get()
        if not chat_doc.exists:
            return jsonify({'error': 'Chat not found'}), 404
        if user_id not in chat_doc.to_dict().get('participants', []):
            return jsonify({'error': 'Unauthorized'}), 403
        msg_ref = chat_ref.collection('messages').document(message_id)
        if not msg_ref.get().exists:
            return jsonify({'error': 'Message not found'}), 404
        if emoji:
            msg_ref.update({f'reactions.{user_id}': emoji})
        else:
            msg_ref.update({f'reactions.{user_id}': firestore.DELETE_FIELD})
        return jsonify({'success': True}), 200
    except Exception as e:
        import traceback; print(traceback.format_exc())
        return jsonify({'error': str(e)}), 500


# ─── POST /api/chats/<targetId>/messages/forward ─────────────
@chat_bp.route('/chats/<target_chat_id>/messages/forward', methods=['POST'])
@jwt_required()
def forward_message(target_chat_id):
    try:
        user_id  = get_jwt_identity()
        data     = request.get_json()
        chat_ref = db.collection('chats').document(target_chat_id)
        chat_doc = chat_ref.get()
        if not chat_doc.exists:
            return jsonify({'error': 'Target chat not found'}), 404
        participants = chat_doc.to_dict().get('participants', [])
        if user_id not in participants:
            return jsonify({'error': 'Unauthorized'}), 403
        user_info   = get_user_info(user_id)
        sender_name = user_info['username'] if user_info else 'Unknown'
        text        = data.get('text', '')
        transaction = data.get('transaction')
        msg_data = {
            'senderId':    user_id, 'senderName':  sender_name,
            'text':        text,    'transaction': transaction,
            'deleted':     False,   'forwarded':   True,
            'reactions':   {},      'timestamp':   firestore.SERVER_TIMESTAMP,
        }
        msg_ref = chat_ref.collection('messages').document()
        msg_ref.set(msg_data)
        update = {'lastMessage': '↪ Forwarded message',
                  'lastMessageAt': firestore.SERVER_TIMESTAMP}
        for pid in participants:
            if pid != user_id:
                update[f'unreadCount.{pid}'] = firestore.Increment(1)
        chat_ref.update(update)
        return jsonify({'id': msg_ref.id}), 201
    except Exception as e:
        import traceback; print(traceback.format_exc())
        return jsonify({'error': str(e)}), 500


# ─── PATCH /api/contacts/<phone>/nickname ────────────────────
@chat_bp.route('/contacts/<phone>/nickname', methods=['PATCH'])
@jwt_required()
def set_global_nickname(phone):
    try:
        user_id  = get_jwt_identity()
        data     = request.get_json()
        nickname = data.get('nickname', '').strip()
        contact_ref = users_ref.document(user_id) \
            .collection('contacts') \
            .document(phone)
        if nickname:
            contact_ref.set({'nickname': nickname}, merge=True)
        else:
            contact_ref.delete()
        return jsonify({'success': True, 'nickname': nickname}), 200
    except Exception as e:
        import traceback; print(traceback.format_exc())
        return jsonify({'error': str(e)}), 500


# ─── GET /api/contacts/nicknames ─────────────────────────────
@chat_bp.route('/contacts/nicknames', methods=['GET'])
@jwt_required()
def get_all_nicknames():
    try:
        user_id = get_jwt_identity()
        docs    = users_ref.document(user_id).collection('contacts').stream()
        result  = {doc.id: doc.to_dict().get('nickname', '') for doc in docs}
        return jsonify(result), 200
    except Exception as e:
        return jsonify({'error': str(e)}), 500


# ─── GET /api/chats/unread ────────────────────────────────────
@chat_bp.route('/chats/unread', methods=['GET'])
@jwt_required()
def get_unread_count():
    try:
        user_id = get_jwt_identity()
        docs    = (db.collection('chats')
                   .where(filter=FieldFilter('participants', 'array_contains', user_id))
                   .stream())
        total_unread = sum(
            doc.to_dict().get('unreadCount', {}).get(user_id, 0) for doc in docs)
        return jsonify({'unread': total_unread}), 200
    except Exception as e:
        return jsonify({'error': str(e)}), 500


# ─── PATCH /api/chats/<chat_id>/group-name ───────────────────
@chat_bp.route('/chats/<chat_id>/group-name', methods=['PATCH'])
@jwt_required()
def update_group_name(chat_id):
    try:
        user_id  = get_jwt_identity()
        data     = request.get_json()
        new_name = data.get('groupName', '').strip()
        if not new_name:
            return jsonify({'error': 'Group name cannot be empty'}), 400
        chat_ref = db.collection('chats').document(chat_id)
        chat_doc = chat_ref.get()
        if not chat_doc.exists:
            return jsonify({'error': 'Chat not found'}), 404
        d = chat_doc.to_dict()
        if user_id not in d.get('participants', []):
            return jsonify({'error': 'Unauthorized'}), 403
        if not d.get('isGroup', False):
            return jsonify({'error': 'Not a group chat'}), 400
        chat_ref.update({'groupName': new_name})
        return jsonify({'success': True, 'groupName': new_name}), 200
    except Exception as e:
        import traceback; print(traceback.format_exc())
        return jsonify({'error': str(e)}), 500