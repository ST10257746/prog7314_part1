const { admin, db } = require('../config/firebase');

/**
 * Send FCM notification to user
 * @param {string} userId - User ID
 * @param {string} title - Notification title
 * @param {string} body - Notification body
 * @param {object} data - Additional data payload
 * @returns {Promise<void>}
 */
async function sendNotification(userId, title, body, data = {}) {
  try {
    const userDoc = await db.collection('users').doc(userId).get();
    
    if (!userDoc.exists) {
      console.log(`User ${userId} not found, skipping notification`);
      return;
    }
    
    const tokens = userDoc.data().fcmTokens || [];
    
    if (tokens.length === 0) {
      console.log(`No FCM tokens for user ${userId}, skipping notification`);
      return;
    }
    
    const message = {
      tokens,
      notification: {
        title,
        body
      },
      data: {
        type: data.type || 'GENERAL',
        ...data
      }
    };
    
    const response = await admin.messaging().sendEachForMulticast(message);
    
    console.log(`✅ Notification sent to ${response.successCount} device(s) for user ${userId}`);
    
    if (response.failureCount > 0) {
      console.warn(`⚠️ Failed to send to ${response.failureCount} device(s)`);
    }
    
    // Save notification to Firestore
    const notificationData = {
      userId,
      title,
      body,
      data: message.data,
      createdAt: Date.now(),
      read: false
    };
    
    await db.collection('notifications').add(notificationData);
    
  } catch (error) {
    console.error('Error sending notification:', error);
    // Don't throw - notifications are non-critical
  }
}

module.exports = { sendNotification };

