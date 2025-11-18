const express = require('express');
const router = express.Router();
const { db, admin } = require('../config/firebase');
const { verifyToken } = require('../middleware/auth');

router.post('/register-token', verifyToken, async (req, res) => {
  try {
    const userId = req.user.uid;
    const { token } = req.body;

    if (!token) {
      return res.status(400).json({
        error: 'Missing token'
      });
    }

    const userRef = db.collection('users').doc(userId);
    const userDoc = await userRef.get();
    
    if (!userDoc.exists) {
      return res.status(404).json({
        error: 'User not found',
        message: 'User profile does not exist. Please register first.'
      });
    }

    const userData = userDoc.data();
    const existingTokens = userData.fcmTokens || [];
    
    // Check if token already exists
    if (existingTokens.includes(token)) {
      console.log(`Token already registered for user ${userId}`);
      return res.json({
        message: 'Token already registered'
      });
    }

    // Add token to array (initialize if doesn't exist)
    await userRef.update({
      fcmTokens: admin.firestore.FieldValue.arrayUnion(token),
      updatedAt: Date.now()
    });

    console.log(`✅ FCM token registered for user ${userId}: ${token.substring(0, 20)}...`);

    res.json({
      message: 'Token registered'
    });
  } catch (error) {
    console.error('Register token error:', error);
    res.status(500).json({
      error: 'Failed to register token',
      message: error.message
    });
  }
});

router.get('/', verifyToken, async (req, res) => {
  try {
    const userId = req.user.uid;

    const snapshot = await db.collection('notifications')
      .where('userId', '==', userId)
      .orderBy('createdAt', 'desc')
      .limit(50)
      .get();

    const notifications = [];
    snapshot.forEach(doc => {
      notifications.push({
        id: doc.id,
        ...doc.data()
      });
    });

    res.json({
      count: notifications.length,
      notifications
    });
  } catch (error) {
    console.error('Get notifications error:', error);
    res.status(500).json({
      error: 'Failed to fetch notifications',
      message: error.message
    });
  }
});

router.put('/:notificationId/read', verifyToken, async (req, res) => {
  try {
    const userId = req.user.uid;
    const { notificationId } = req.params;

    const docRef = db.collection('notifications').doc(notificationId);
    const doc = await docRef.get();

    if (!doc.exists) {
      return res.status(404).json({
        error: 'Notification not found'
      });
    }

    if (doc.data().userId !== userId) {
      return res.status(403).json({
        error: 'Forbidden',
        message: 'You can only update your own notifications'
      });
    }

    await docRef.update({
      read: true,
      readAt: Date.now()
    });

    const updated = await docRef.get();

    res.json({
      message: 'Notification marked as read',
      notification: {
        id: updated.id,
        ...updated.data()
      }
    });
  } catch (error) {
    console.error('Update notification error:', error);
    res.status(500).json({
      error: 'Failed to update notification',
      message: error.message
    });
  }
});

router.post('/send-test', verifyToken, async (req, res) => {
  try {
    const userId = req.user.uid;
    const { title, body, data } = req.body;

    const userDoc = await db.collection('users').doc(userId).get();

    if (!userDoc.exists) {
      return res.status(404).json({
        error: 'User not found'
      });
    }

    const tokens = userDoc.data().fcmTokens || [];

    if (!tokens.length) {
      return res.status(400).json({
        error: 'No FCM tokens registered for user'
      });
    }

    const message = {
      tokens,
      notification: {
        title: title || 'FitTrackr notification',
        body: body || 'This is a test notification'
      },
      data: data || {}
    };

    const response = await admin.messaging().sendEachForMulticast(message);

    const notificationData = {
      userId,
      title: message.notification.title,
      body: message.notification.body,
      data: message.data,
      createdAt: Date.now(),
      read: false
    };

    const docRef = await db.collection('notifications').add(notificationData);

    res.json({
      message: 'Notification sent',
      sendResult: {
        successCount: response.successCount,
        failureCount: response.failureCount
      },
      notification: {
        id: docRef.id,
        ...notificationData
      }
    });
  } catch (error) {
    console.error('Send test notification error:', error);
    res.status(500).json({
      error: 'Failed to send notification',
      message: error.message
    });
  }
});

module.exports = router;
