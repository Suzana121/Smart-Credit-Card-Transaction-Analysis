// קובץ: backend/routes/auth-routes.js

const express = require('express');
const router = express.Router();
const userService = require('../services/user-service');
const jwt = require('jsonwebtoken'); 
// const bcrypt = require('bcrypt'); // צריך לייבא אם משתמשים בו כאן

// מפתח סודי ל-JWT (צריך להיות ב-Environment Variable אמיתי!)
const JWT_SECRET = 'YOUR_SUPER_SECRET_KEY'; 

// POST /api/auth/register
router.post('/register', async (req, res) => {
  const { email, password } = req.body;
  
  if (await userService.findUserByEmail(email)) {
    return res.status(409).send({ message: 'User already exists.' });
  }

  try {
    // 1. הצפנת סיסמה
    // const hashedPassword = await bcrypt.hash(password, 10);
    const hashedPassword = password + "_hashed"; // זמני

    // 2. שמירה ב-Firestore
    const userId = await userService.createUser(email, hashedPassword);

    res.status(201).send({ message: 'User registered successfully', userId });
  } catch (error) {
    console.error('Registration error:', error);
    res.status(500).send({ message: 'Registration failed' });
  }
});

// POST /api/auth/login
router.post('/login', async (req, res) => {
  const { email, password } = req.body;

  try {
    // 1. אימות המשתמש מול Firestore
    const user = await userService.authenticateUser(email, password);

    if (!user) {
      return res.status(401).send({ message: 'Invalid credentials.' });
    }

    // 2. יצירת JWT (טוקן)
    const token = jwt.sign(
      { userId: user.id, email: user.email },
      JWT_SECRET,
      { expiresIn: '1h' } // תוקף הטוקן
    );

    res.send({ token, userId: user.id });

  } catch (error) {
    console.error('Login error:', error);
    res.status(500).send({ message: 'Login failed' });
  }
});

module.exports = router;