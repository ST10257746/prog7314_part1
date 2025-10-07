const express = require('express');
const router = express.Router();
const { db } = require('../config/firebase');
const { verifyToken } = require('../middleware/auth');

/**
 * GET /api/progress/:userId
 * Get progress entries for a user
 */
router.get('/:userId', verifyToken, async (req, res) => {
  try {
    const { userId } = req.params;
    const { startDate, endDate, limit = 30 } = req.query;
    
    // Verify user can only access their own progress
    if (req.user.uid !== userId) {
      return res.status(403).json({ 
        error: 'Forbidden',
        message: 'You can only access your own progress' 
      });
    }
    
    let query = db.collection('progressTracking')
      .where('userId', '==', userId)
      .orderBy('date', 'desc')
      .limit(parseInt(limit));
    
    if (startDate) {
      query = query.where('date', '>=', parseInt(startDate));
    }
    if (endDate) {
      query = query.where('date', '<=', parseInt(endDate));
    }
    
    const snapshot = await query.get();
    
    const progressEntries = [];
    snapshot.forEach(doc => {
      progressEntries.push({
        id: doc.id,
        ...doc.data()
      });
    });
    
    res.json({
      count: progressEntries.length,
      progress: progressEntries
    });
    
  } catch (error) {
    console.error('Get progress error:', error);
    res.status(500).json({ 
      error: 'Failed to fetch progress',
      message: error.message 
    });
  }
});

/**
 * GET /api/progress/:userId/latest
 * Get latest progress entry for a user
 */
router.get('/:userId/latest', verifyToken, async (req, res) => {
  try {
    const { userId } = req.params;
    
    if (req.user.uid !== userId) {
      return res.status(403).json({ 
        error: 'Forbidden' 
      });
    }
    
    const snapshot = await db.collection('progressTracking')
      .where('userId', '==', userId)
      .orderBy('date', 'desc')
      .limit(1)
      .get();
    
    if (snapshot.empty) {
      return res.status(404).json({ 
        error: 'No progress entries found' 
      });
    }
    
    const doc = snapshot.docs[0];
    res.json({
      progress: {
        id: doc.id,
        ...doc.data()
      }
    });
    
  } catch (error) {
    console.error('Get latest progress error:', error);
    res.status(500).json({ 
      error: 'Failed to fetch latest progress',
      message: error.message 
    });
  }
});

/**
 * POST /api/progress
 * Create a new progress entry
 */
router.post('/', verifyToken, async (req, res) => {
  try {
    const userId = req.user.uid;
    const {
      date,
      weightKg,
      bmi,
      bodyFatPercentage,
      muscleMassKg,
      notes
    } = req.body;
    
    // Validation
    if (!date) {
      return res.status(400).json({ 
        error: 'Missing required field: date' 
      });
    }
    
    const progressData = {
      userId,
      date: parseInt(date),
      weightKg: weightKg || null,
      bmi: bmi || null,
      bodyFatPercentage: bodyFatPercentage || null,
      muscleMassKg: muscleMassKg || null,
      notes: notes || '',
      createdAt: Date.now()
    };
    
    const docRef = await db.collection('progressTracking').add(progressData);
    
    res.status(201).json({
      message: 'Progress entry created successfully',
      progress: {
        id: docRef.id,
        ...progressData
      }
    });
    
  } catch (error) {
    console.error('Create progress error:', error);
    res.status(500).json({ 
      error: 'Failed to create progress entry',
      message: error.message 
    });
  }
});

/**
 * PUT /api/progress/:progressId
 * Update progress entry
 */
router.put('/:progressId', verifyToken, async (req, res) => {
  try {
    const { progressId } = req.params;
    const userId = req.user.uid;
    
    // Verify progress entry exists and belongs to user
    const doc = await db.collection('progressTracking').doc(progressId).get();
    
    if (!doc.exists) {
      return res.status(404).json({ 
        error: 'Progress entry not found' 
      });
    }
    
    if (doc.data().userId !== userId) {
      return res.status(403).json({ 
        error: 'Forbidden',
        message: 'You can only update your own progress entries' 
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
    
    await db.collection('progressTracking').doc(progressId).update(updateData);
    
    const updatedDoc = await db.collection('progressTracking').doc(progressId).get();
    
    res.json({
      message: 'Progress entry updated successfully',
      progress: {
        id: updatedDoc.id,
        ...updatedDoc.data()
      }
    });
    
  } catch (error) {
    console.error('Update progress error:', error);
    res.status(500).json({ 
      error: 'Failed to update progress entry',
      message: error.message 
    });
  }
});

/**
 * DELETE /api/progress/:progressId
 * Delete progress entry
 */
router.delete('/:progressId', verifyToken, async (req, res) => {
  try {
    const { progressId } = req.params;
    const userId = req.user.uid;
    
    // Verify progress entry exists and belongs to user
    const doc = await db.collection('progressTracking').doc(progressId).get();
    
    if (!doc.exists) {
      return res.status(404).json({ 
        error: 'Progress entry not found' 
      });
    }
    
    if (doc.data().userId !== userId) {
      return res.status(403).json({ 
        error: 'Forbidden',
        message: 'You can only delete your own progress entries' 
      });
    }
    
    await db.collection('progressTracking').doc(progressId).delete();
    
    res.json({
      message: 'Progress entry deleted successfully'
    });
    
  } catch (error) {
    console.error('Delete progress error:', error);
    res.status(500).json({ 
      error: 'Failed to delete progress entry',
      message: error.message 
    });
  }
});

/**
 * GET /api/progress/:userId/summary
 * Get progress summary statistics
 */
router.get('/:userId/summary', verifyToken, async (req, res) => {
  try {
    const { userId } = req.params;
    
    if (req.user.uid !== userId) {
      return res.status(403).json({ 
        error: 'Forbidden' 
      });
    }
    
    const snapshot = await db.collection('progressTracking')
      .where('userId', '==', userId)
      .orderBy('date', 'asc')
      .get();
    
    if (snapshot.empty) {
      return res.json({
        summary: {
          totalEntries: 0,
          weightChange: null,
          averageBmi: null
        }
      });
    }
    
    const entries = [];
    snapshot.forEach(doc => {
      entries.push(doc.data());
    });
    
    const firstEntry = entries[0];
    const lastEntry = entries[entries.length - 1];
    
    const weightChange = (lastEntry.weightKg && firstEntry.weightKg) 
      ? (lastEntry.weightKg - firstEntry.weightKg).toFixed(2)
      : null;
    
    const bmiValues = entries.filter(e => e.bmi).map(e => e.bmi);
    const averageBmi = bmiValues.length > 0
      ? (bmiValues.reduce((a, b) => a + b, 0) / bmiValues.length).toFixed(2)
      : null;
    
    res.json({
      summary: {
        totalEntries: entries.length,
        weightChange: weightChange ? parseFloat(weightChange) : null,
        averageBmi: averageBmi ? parseFloat(averageBmi) : null,
        firstEntry: {
          date: firstEntry.date,
          weightKg: firstEntry.weightKg
        },
        latestEntry: {
          date: lastEntry.date,
          weightKg: lastEntry.weightKg
        }
      }
    });
    
  } catch (error) {
    console.error('Get progress summary error:', error);
    res.status(500).json({ 
      error: 'Failed to fetch progress summary',
      message: error.message 
    });
  }
});

module.exports = router;

