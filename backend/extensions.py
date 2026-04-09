 from flask_mail import Mail

# Created separately to avoid circular imports between app.py and auth.py.
# app.py calls mail.init_app(app); auth.py imports mail from here.
mail = Mail()
