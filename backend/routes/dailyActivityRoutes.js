const express = require('express');
const router = express.Router();
const { db } = require('../config/firebase');
const { verifyToken } = require('../middleware/auth');

/**
 * GET /api/daily-activity/:userId/:date
 * Get daily activity for a specific date
 */
router.get('/:userId/:date', verifyToken, async (req, res) => {
  try {
    const { userId, date } = req.params;
    
    // Verify user can only access their own data
    if (req.user.uid !== userId) {
      return res.status(403).json({ 
        error: 'Forbidden',
        message: 'You can only access your own activity data' 
      });
    }
    
    const doc = await db.collection('dailyActivities').doc(`${userId}_${date}`).get();
    
    if (!doc.exists) {
      return res.json({
        userId,
        date,
        steps: 0,
        waterGlasses: 0,
        caloriesBurned: 0,
        activeMinutes: 0,
        distance: 0,
        lastUpdated: Date.now()
      });
    }
    
    res.json({
      ...doc.data(),
      id: doc.id
    });
    
  } catch (error) {
    console.error('Get daily activity error:', error);
    res.status(500).json({ 
      error: 'Failed to fetch daily activity',
      message: error.message 
    });
  }
});

/**
 * PUT /api/daily-activity/:userId/:date
 * Update daily activity for a specific date
 */
router.put('/:userId/:date', verifyToken, async (req, res) => {
  try {
    const { userId, date } = req.params;
    const { steps, waterGlasses, caloriesBurned, activeMinutes, distance } = req.body;
    
    // Verify user can only update their own data
    if (req.user.uid !== userId) {
      return res.status(403).json({ 
        error: 'Forbidden',
        message: 'You can only update your own activity data' 
      });
    }
    
    const docId = `${userId}_${date}`;
    const docRef = db.collection('dailyActivities').doc(docId);
    
    // Get existing data or create new
    const doc = await docRef.get();
    const existingData = doc.exists ? doc.data() : {
      userId,
      date,
      steps: 0,
      waterGlasses: 0,
      caloriesBurned: 0,
      activeMinutes: 0,
      distance: 0
    };
    
    // Update only provided fields
    const updateData = {
      ...existingData,
      ...(steps !== undefined && { steps: parseInt(steps) }),
      ...(waterGlasses !== undefined && { waterGlasses: parseInt(waterGlasses) }),
      ...(caloriesBurned !== undefined && { caloriesBurned: parseInt(caloriesBurned) }),
      ...(activeMinutes !== undefined && { activeMinutes: parseInt(activeMinutes) }),
      ...(distance !== undefined && { distance: parseFloat(distance) }),
      lastUpdated: Date.now()
    };
    
    await docRef.set(updateData, { merge: true });
    
    res.json({
      message: 'Daily activity updated successfully',
      activity: updateData
    });
    
  } catch (error) {
    console.error('Update daily activity error:', error);
    res.status(500).json({ 
      error: 'Failed to update daily activity',
      message: error.message 
    });
  }
});

/**
 * POST /api/daily-activity/:userId/:date/water
 * Add or remove water glasses
 */
router.post('/:userId/:date/water', verifyToken, async (req, res) => {
  try {
    const { userId, date } = req.params;
    const { amount } = req.body; // Can be positive (add) or negative (remove)
    
    // Verify user
    if (req.user.uid !== userId) {
      return res.status(403).json({ 
        error: 'Forbidden' 
      });
    }
    
    const docId = `${userId}_${date}`;
    const docRef = db.collection('dailyActivities').doc(docId);
    
    // Get current data
    const doc = await docRef.get();
    const currentWater = doc.exists ? (doc.data().waterGlasses || 0) : 0;
    const newWater = Math.max(0, currentWater + parseInt(amount)); // Don't go below 0
    
    const updateData = {
      userId,
      date,
      waterGlasses: newWater,
      lastUpdated: Date.now()
    };
    
    await docRef.set(updateData, { merge: true });
    
    res.json({
      message: 'Water intake updated successfully',
      waterGlasses: newWater
    });
    
  } catch (error) {
    console.error('Update water error:', error);
    res.status(500).json({ 
      error: 'Failed to update water intake',
      message: error.message 
    });
  }
});

module.exports = router;

