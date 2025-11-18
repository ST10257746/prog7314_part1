const express = require('express');
const router = express.Router();
const { db, auth } = require('../config/firebase');
const { verifyToken } = require('../middleware/auth');
const { sendNotification } = require('../utils/notifications');

/**
 * POST /api/users/register
 * Complete user registration by creating Firestore profile
 * (Auth user already created by client)
 */
router.post('/register', verifyToken, async (req, res) => {
  try {
    const { email, displayName, age, weightKg, heightCm } = req.body;
    const userId = req.user.uid; // From JWT token
    
    // Validation
    if (!email || !displayName) {
      return res.status(400).json({ 
        error: 'Missing required fields',
        required: ['email', 'displayName']
      });
    }
    
    // Check if user profile already exists
    const existingUser = await db.collection('users').doc(userId).get();
    if (existingUser.exists) {
      return res.status(400).json({
        error: 'Registration failed',
        message: 'User profile already exists'
      });
    }
    
    // Create Firestore user document
    const userData = {
      userId: userId,
      email,
      displayName,
      age: age || 0,
      weightKg: weightKg || 0,
      heightCm: heightCm || null,
      profileImageUrl: null,
      dailyStepGoal: null,
      dailyCalorieGoal: null,
      dailyWaterGoal: null,
      weeklyWorkoutGoal: null,
      proteinGoalG: null,
      carbsGoalG: null,
      fatsGoalG: null,
      fcmTokens: [], // Initialize empty array for FCM tokens
      createdAt: Date.now(),
      updatedAt: Date.now()
    };
    
    await db.collection('users').doc(userId).set(userData);
    
    // Note: Welcome notification will be sent by the app after FCM token is registered
    // This prevents race condition where notification is sent before token exists
    
    res.status(201).json({
      message: 'User registered successfully',
      user: userData
    });
    
  } catch (error) {
    console.error('Registration error:', error);
    res.status(400).json({ 
      error: 'Registration failed',
      message: error.message 
    });
  }
});

/**
 * POST /api/users/login
 * Verify user credentials (Firebase handles actual auth on client)
 * This endpoint mainly fetches user data after successful auth
 */
router.post('/login', verifyToken, async (req, res) => {
  try {
    const userId = req.user.uid;
    
    // Fetch user data from Firestore
    const userDoc = await db.collection('users').doc(userId).get();
    
    if (!userDoc.exists) {
      return res.status(404).json({ 
        error: 'User not found',
        message: 'User profile does not exist' 
      });
    }
    
    const userData = userDoc.data();
    
    // Note: Login notification will be sent by the app after FCM token is registered
    // This prevents race condition where notification is sent before token exists
    
    res.json({
      message: 'Login successful',
      user: userData
    });
    
  } catch (error) {
    console.error('Login error:', error);
    res.status(500).json({ 
      error: 'Login failed',
      message: error.message 
    });
  }
});

/**
 * GET /api/users/:userId
 * Get user profile by ID
 */
router.get('/:userId', verifyToken, async (req, res) => {
  try {
    const { userId } = req.params;
    
    // Verify user can only access their own data
    if (req.user.uid !== userId) {
      return res.status(403).json({ 
        error: 'Forbidden',
        message: 'You can only access your own profile' 
      });
    }
    
    const userDoc = await db.collection('users').doc(userId).get();
    
    if (!userDoc.exists) {
      return res.status(404).json({ 
        error: 'User not found' 
      });
    }
    
    res.json({
      user: userDoc.data()
    });
    
  } catch (error) {
    console.error('Get user error:', error);
    res.status(500).json({ 
      error: 'Failed to fetch user',
      message: error.message 
    });
  }
});

/**
 * PUT /api/users/:userId
 * Update user profile
 */
router.put('/:userId', verifyToken, async (req, res) => {
  try {
    const { userId } = req.params;
    
    // Verify user can only update their own data
    if (req.user.uid !== userId) {
      return res.status(403).json({ 
        error: 'Forbidden',
        message: 'You can only update your own profile' 
      });
    }
    
    const updateData = {
      ...req.body,
      updatedAt: Date.now()
    };
    
    // Remove fields that shouldn't be updated via this endpoint
    delete updateData.userId;
    delete updateData.email;
    delete updateData.createdAt;
    
    await db.collection('users').doc(userId).update(updateData);
    
    const updatedDoc = await db.collection('users').doc(userId).get();
    
    res.json({
      message: 'User updated successfully',
      user: updatedDoc.data()
    });
    
  } catch (error) {
    console.error('Update user error:', error);
    res.status(500).json({ 
      error: 'Failed to update user',
      message: error.message 
    });
  }
});

/**
 * DELETE /api/users/:userId
 * Delete user account (soft delete or full delete)
 */
router.delete('/:userId', verifyToken, async (req, res) => {
  try {
    const { userId } = req.params;
    
    // Verify user can only delete their own account
    if (req.user.uid !== userId) {
      return res.status(403).json({ 
        error: 'Forbidden',
        message: 'You can only delete your own account' 
      });
    }
    
    // Delete from Firestore
    await db.collection('users').doc(userId).delete();
    
    // Delete from Firebase Auth
    await auth.deleteUser(userId);
    
    res.json({
      message: 'User account deleted successfully'
    });
    
  } catch (error) {
    console.error('Delete user error:', error);
    res.status(500).json({ 
      error: 'Failed to delete user',
      message: error.message 
    });
  }
});

module.exports = router;

