from flask import Flask, jsonify
from flask_jwt_extended import JWTManager
from routes.upload import upload_bp
from routes.chat import chat_bp
from auth import auth_bp

app = Flask(__name__)

# Secret key for JWT
app.config["JWT_SECRET_KEY"] = "YOUR_SUPER_SECRET_KEY"

# אתחול JWT
jwt = JWTManager(app)

# רישום ה-blueprints
app.register_blueprint(auth_bp, url_prefix="/auth")
app.register_blueprint(upload_bp, url_prefix="/api")
app.register_blueprint(chat_bp, url_prefix="/api")

@app.route("/")
def home():
    return jsonify({"message": "Welcome to Smart Credit Card Transaction Analysis API"})

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5001, debug=True)