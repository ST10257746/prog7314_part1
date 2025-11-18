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
      
      // Log detailed error information
      response.responses.forEach((resp, idx) => {
        if (!resp.success) {
          const errorCode = resp.error?.code;
          const errorMessage = resp.error?.message;
          console.log(`❌ Token ${idx} failed: ${errorCode} - ${errorMessage}`);
          console.log(`   Token: ${tokens[idx].substring(0, 30)}...`);
        }
      });
      
      // Remove only permanently invalid tokens (not transient errors)
      const invalidTokens = [];
      response.responses.forEach((resp, idx) => {
        if (!resp.success) {
          const errorCode = resp.error?.code;
          // Only remove tokens that are permanently invalid
          // Don't remove for "not-registered" as it might be a timing issue
          if (errorCode === 'messaging/invalid-registration-token' ||
              errorCode === 'messaging/invalid-argument') {
            invalidTokens.push(tokens[idx]);
            console.log(`🗑️ Marking token for removal: ${tokens[idx].substring(0, 20)}... (${errorCode})`);
          } else if (errorCode === 'messaging/registration-token-not-registered') {
            // Log but don't remove - might be transient or emulator token
            console.log(`⚠️ Token not registered (might be emulator/test): ${tokens[idx].substring(0, 20)}...`);
          }
        }
      });
      
      // Remove invalid tokens from Firestore
      if (invalidTokens.length > 0) {
        const userRef = db.collection('users').doc(userId);
        await userRef.update({
          fcmTokens: admin.firestore.FieldValue.arrayRemove(...invalidTokens)
        });
        console.log(`🗑️ Removed ${invalidTokens.length} permanently invalid FCM token(s) for user ${userId}`);
      }
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

