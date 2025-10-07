const express = require('express');
const router = express.Router();
const { db } = require('../config/firebase');
const { verifyToken } = require('../middleware/auth');

/**
 * GET /api/workouts
 * Get all workout sessions for authenticated user
 */
router.get('/', verifyToken, async (req, res) => {
  try {
    const userId = req.user.uid;
    const { limit = 50, status } = req.query;
    
    let query = db.collection('workoutSessions')
      .where('userId', '==', userId)
      .orderBy('startTime', 'desc')
      .limit(parseInt(limit));
    
    // Optional status filter
    if (status) {
      query = query.where('status', '==', status);
    }
    
    const snapshot = await query.get();
    
    const workouts = [];
    snapshot.forEach(doc => {
      workouts.push({
        id: doc.id,
        ...doc.data()
      });
    });
    
    res.json({
      count: workouts.length,
      workouts
    });
    
  } catch (error) {
    console.error('Get workouts error:', error);
    res.status(500).json({ 
      error: 'Failed to fetch workouts',
      message: error.message 
    });
  }
});

/**
 * GET /api/workouts/:workoutId
 * Get specific workout session by ID
 */
router.get('/:workoutId', verifyToken, async (req, res) => {
  try {
    const { workoutId } = req.params;
    const userId = req.user.uid;
    
    const doc = await db.collection('workoutSessions').doc(workoutId).get();
    
    if (!doc.exists) {
      return res.status(404).json({ 
        error: 'Workout not found' 
      });
    }
    
    const workoutData = doc.data();
    
    // Verify user owns this workout
    if (workoutData.userId !== userId) {
      return res.status(403).json({ 
        error: 'Forbidden',
        message: 'You can only access your own workouts' 
      });
    }
    
    res.json({
      workout: {
        id: doc.id,
        ...workoutData
      }
    });
    
  } catch (error) {
    console.error('Get workout error:', error);
    res.status(500).json({ 
      error: 'Failed to fetch workout',
      message: error.message 
    });
  }
});

/**
 * POST /api/workouts
 * Create a new workout session
 */
router.post('/', verifyToken, async (req, res) => {
  try {
    const userId = req.user.uid;
    const { 
      workoutName, 
      workoutType,
      startTime, 
      endTime, 
      durationSeconds,
      caloriesBurned,
      distanceKm,
      notes,
      status = 'COMPLETED'
    } = req.body;
    
    // Validation
    if (!workoutName || !startTime) {
      return res.status(400).json({ 
        error: 'Missing required fields',
        required: ['workoutName', 'startTime']
      });
    }
    
    const workoutData = {
      userId,
      workoutName,
      workoutType: workoutType || 'OTHER',
      startTime: parseInt(startTime),
      endTime: endTime ? parseInt(endTime) : null,
      durationSeconds: durationSeconds || 0,
      caloriesBurned: caloriesBurned || 0,
      distanceKm: distanceKm || 0,
      notes: notes || '',
      status,
      createdAt: Date.now(),
      isSynced: true
    };
    
    const docRef = await db.collection('workoutSessions').add(workoutData);
    
    res.status(201).json({
      message: 'Workout created successfully',
      workout: {
        id: docRef.id,
        ...workoutData
      }
    });
    
  } catch (error) {
    console.error('Create workout error:', error);
    res.status(500).json({ 
      error: 'Failed to create workout',
      message: error.message 
    });
  }
});

/**
 * PUT /api/workouts/:workoutId
 * Update existing workout session
 */
router.put('/:workoutId', verifyToken, async (req, res) => {
  try {
    const { workoutId } = req.params;
    const userId = req.user.uid;
    
    // Verify workout exists and belongs to user
    const doc = await db.collection('workoutSessions').doc(workoutId).get();
    
    if (!doc.exists) {
      return res.status(404).json({ 
        error: 'Workout not found' 
      });
    }
    
    if (doc.data().userId !== userId) {
      return res.status(403).json({ 
        error: 'Forbidden',
        message: 'You can only update your own workouts' 
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
    
    await db.collection('workoutSessions').doc(workoutId).update(updateData);
    
    const updatedDoc = await db.collection('workoutSessions').doc(workoutId).get();
    
    res.json({
      message: 'Workout updated successfully',
      workout: {
        id: updatedDoc.id,
        ...updatedDoc.data()
      }
    });
    
  } catch (error) {
    console.error('Update workout error:', error);
    res.status(500).json({ 
      error: 'Failed to update workout',
      message: error.message 
    });
  }
});

/**
 * DELETE /api/workouts/:workoutId
 * Delete workout session
 */
router.delete('/:workoutId', verifyToken, async (req, res) => {
  try {
    const { workoutId } = req.params;
    const userId = req.user.uid;
    
    // Verify workout exists and belongs to user
    const doc = await db.collection('workoutSessions').doc(workoutId).get();
    
    if (!doc.exists) {
      return res.status(404).json({ 
        error: 'Workout not found' 
      });
    }
    
    if (doc.data().userId !== userId) {
      return res.status(403).json({ 
        error: 'Forbidden',
        message: 'You can only delete your own workouts' 
      });
    }
    
    await db.collection('workoutSessions').doc(workoutId).delete();
    
    res.json({
      message: 'Workout deleted successfully'
    });
    
  } catch (error) {
    console.error('Delete workout error:', error);
    res.status(500).json({ 
      error: 'Failed to delete workout',
      message: error.message 
    });
  }
});

/**
 * GET /api/workouts/stats/summary
 * Get workout statistics summary
 */
router.get('/stats/summary', verifyToken, async (req, res) => {
  try {
    const userId = req.user.uid;
    const { startDate, endDate } = req.query;
    
    let query = db.collection('workoutSessions')
      .where('userId', '==', userId)
      .where('status', '==', 'COMPLETED');
    
    if (startDate) {
      query = query.where('startTime', '>=', parseInt(startDate));
    }
    if (endDate) {
      query = query.where('startTime', '<=', parseInt(endDate));
    }
    
    const snapshot = await query.get();
    
    let totalWorkouts = 0;
    let totalDuration = 0;
    let totalCalories = 0;
    let totalDistance = 0;
    
    snapshot.forEach(doc => {
      const data = doc.data();
      totalWorkouts++;
      totalDuration += data.durationSeconds || 0;
      totalCalories += data.caloriesBurned || 0;
      totalDistance += data.distanceKm || 0;
    });
    
    res.json({
      summary: {
        totalWorkouts,
        totalDurationMinutes: Math.round(totalDuration / 60),
        totalCaloriesBurned: totalCalories,
        totalDistanceKm: parseFloat(totalDistance.toFixed(2)),
        averageDurationMinutes: totalWorkouts > 0 ? Math.round(totalDuration / 60 / totalWorkouts) : 0
      }
    });
    
  } catch (error) {
    console.error('Get stats error:', error);
    res.status(500).json({ 
      error: 'Failed to fetch statistics',
      message: error.message 
    });
  }
});

module.exports = router;

