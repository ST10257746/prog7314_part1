const express = require('express');
const router = express.Router();
const { db } = require('../config/firebase');
const { verifyToken } = require('../middleware/auth');
const { sendNotification } = require('../utils/notifications');

/**
 * GET /api/nutrition/:userId
 * Get nutrition entries for a user
 */
router.get('/:userId', verifyToken, async (req, res) => {
  try {
    const { userId } = req.params;
    const { date, mealType, limit = 50 } = req.query;
    
    // Verify user can only access their own nutrition data
    if (req.user.uid !== userId) {
      return res.status(403).json({ 
        error: 'Forbidden',
        message: 'You can only access your own nutrition data' 
      });
    }
    
    let query = db.collection('nutritionEntries')
      .where('userId', '==', userId)
      .orderBy('timestamp', 'desc')
      .limit(parseInt(limit));
    
    if (date) {
      // Filter by specific date
      const startOfDay = new Date(date);
      startOfDay.setHours(0, 0, 0, 0);
      const endOfDay = new Date(date);
      endOfDay.setHours(23, 59, 59, 999);
      
      query = query
        .where('timestamp', '>=', startOfDay.getTime())
        .where('timestamp', '<=', endOfDay.getTime());
    }
    
    if (mealType) {
      query = query.where('mealType', '==', mealType);
    }
    
    const snapshot = await query.get();
    
    const entries = [];
    snapshot.forEach(doc => {
      entries.push({
        id: doc.id,
        ...doc.data()
      });
    });
    
    res.json({
      count: entries.length,
      nutrition: entries
    });
    
  } catch (error) {
    console.error('Get nutrition error:', error);
    res.status(500).json({ 
      error: 'Failed to fetch nutrition entries',
      message: error.message 
    });
  }
});

/**
 * GET /api/nutrition/:userId/daily/:date
 * Get daily nutrition summary for a specific date
 */
router.get('/:userId/daily/:date', verifyToken, async (req, res) => {
  try {
    const { userId, date } = req.params;
    
    if (req.user.uid !== userId) {
      return res.status(403).json({ 
        error: 'Forbidden' 
      });
    }
    
    const startOfDay = new Date(date);
    startOfDay.setHours(0, 0, 0, 0);
    const endOfDay = new Date(date);
    endOfDay.setHours(23, 59, 59, 999);
    
    const snapshot = await db.collection('nutritionEntries')
      .where('userId', '==', userId)
      .where('timestamp', '>=', startOfDay.getTime())
      .where('timestamp', '<=', endOfDay.getTime())
      .get();
    
    let totalCalories = 0;
    let totalProtein = 0;
    let totalCarbs = 0;
    let totalFats = 0;
    const entries = [];
    
    snapshot.forEach(doc => {
      const data = doc.data();
      entries.push({ id: doc.id, ...data });
      totalCalories += data.calories || 0;
      totalProtein += data.proteinG || 0;
      totalCarbs += data.carbsG || 0;
      totalFats += data.fatsG || 0;
    });
    
    res.json({
      date,
      summary: {
        totalCalories,
        totalProtein: parseFloat(totalProtein.toFixed(2)),
        totalCarbs: parseFloat(totalCarbs.toFixed(2)),
        totalFats: parseFloat(totalFats.toFixed(2)),
        mealCount: entries.length
      },
      entries
    });
    
  } catch (error) {
    console.error('Get daily nutrition error:', error);
    res.status(500).json({ 
      error: 'Failed to fetch daily nutrition',
      message: error.message 
    });
  }
});

/**
 * POST /api/nutrition
 * Create a new nutrition entry
 */
router.post('/', verifyToken, async (req, res) => {
  try {
    const userId = req.user.uid;
    const {
      foodName,
      mealType,
      servingSize,
      calories,
      proteinG,
      carbsG,
      fatsG,
      fiberG,
      sugarG,
      notes,
      timestamp
    } = req.body;
    
    // Validation
    if (!foodName || !mealType || !calories) {
      return res.status(400).json({ 
        error: 'Missing required fields',
        required: ['foodName', 'mealType', 'calories']
      });
    }
    
    const nutritionData = {
      userId,
      foodName,
      mealType,
      servingSize: servingSize || '1 serving',
      calories: parseInt(calories),
      proteinG: proteinG || 0,
      carbsG: carbsG || 0,
      fatsG: fatsG || 0,
      fiberG: fiberG || 0,
      sugarG: sugarG || 0,
      notes: notes || '',
      timestamp: timestamp || Date.now(),
      createdAt: Date.now()
    };
    
    const docRef = await db.collection('nutritionEntries').add(nutritionData);
    
    // Send notification
    try {
      await sendNotification(
        userId,
        'Meal logged!',
        `You've logged "${foodName}" for ${mealType}. ${calories} calories added to your daily total.`,
        { type: 'MEAL_LOGGED', nutritionId: docRef.id }
      );
    } catch (notifyError) {
      console.error('Failed to send nutrition notification:', notifyError);
      // Don't fail nutrition entry if notification fails
    }
    
    res.status(201).json({
      message: 'Nutrition entry created successfully',
      nutrition: {
        id: docRef.id,
        ...nutritionData
      }
    });
    
  } catch (error) {
    console.error('Create nutrition error:', error);
    res.status(500).json({ 
      error: 'Failed to create nutrition entry',
      message: error.message 
    });
  }
});

/**
 * PUT /api/nutrition/:nutritionId
 * Update nutrition entry
 */
router.put('/:nutritionId', verifyToken, async (req, res) => {
  try {
    const { nutritionId } = req.params;
    const userId = req.user.uid;
    
    // Verify entry exists and belongs to user
    const doc = await db.collection('nutritionEntries').doc(nutritionId).get();
    
    if (!doc.exists) {
      return res.status(404).json({ 
        error: 'Nutrition entry not found' 
      });
    }
    
    if (doc.data().userId !== userId) {
      return res.status(403).json({ 
        error: 'Forbidden',
        message: 'You can only update your own nutrition entries' 
      });
    }
    
    const updateData = {
      ...req.body,
      updatedAt: Date.now()
    };
    
    // Remove fields that shouldn't be updated
    delete updateData.userId;
    delete updateData.createdAt;
    delete updateData.id;
    
    await db.collection('nutritionEntries').doc(nutritionId).update(updateData);
    
    const updatedDoc = await db.collection('nutritionEntries').doc(nutritionId).get();
    
    res.json({
      message: 'Nutrition entry updated successfully',
      nutrition: {
        id: updatedDoc.id,
        ...updatedDoc.data()
      }
    });
    
  } catch (error) {
    console.error('Update nutrition error:', error);
    res.status(500).json({ 
      error: 'Failed to update nutrition entry',
      message: error.message 
    });
  }
});

/**
 * DELETE /api/nutrition/:nutritionId
 * Delete nutrition entry
 */
router.delete('/:nutritionId', verifyToken, async (req, res) => {
  try {
    const { nutritionId } = req.params;
    const userId = req.user.uid;
    
    // Verify entry exists and belongs to user
    const doc = await db.collection('nutritionEntries').doc(nutritionId).get();
    
    if (!doc.exists) {
      return res.status(404).json({ 
        error: 'Nutrition entry not found' 
      });
    }
    
    if (doc.data().userId !== userId) {
      return res.status(403).json({ 
        error: 'Forbidden',
        message: 'You can only delete your own nutrition entries' 
      });
    }
    
    await db.collection('nutritionEntries').doc(nutritionId).delete();
    
    res.json({
      message: 'Nutrition entry deleted successfully'
    });
    
  } catch (error) {
    console.error('Delete nutrition error:', error);
    res.status(500).json({ 
      error: 'Failed to delete nutrition entry',
      message: error.message 
    });
  }
});

module.exports = router;

