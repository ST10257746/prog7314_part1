const express = require('express');
const router = express.Router();
const { db } = require('../config/firebase');
const { verifyToken } = require('../middleware/auth');

/**
 * GET /api/goals/:userId
 * Get all goals for a user
 */
router.get('/:userId', verifyToken, async (req, res) => {
  try {
    const { userId } = req.params;
    
    // Verify user can only access their own goals
    if (req.user.uid !== userId) {
      return res.status(403).json({ 
        error: 'Forbidden',
        message: 'You can only access your own goals' 
      });
    }
    
    const snapshot = await db.collection('goals')
      .where('userId', '==', userId)
      .orderBy('createdAt', 'desc')
      .get();
    
    const goals = [];
    snapshot.forEach(doc => {
      goals.push({
        id: doc.id,
        ...doc.data()
      });
    });
    
    res.json({
      count: goals.length,
      goals
    });
    
  } catch (error) {
    console.error('Get goals error:', error);
    res.status(500).json({ 
      error: 'Failed to fetch goals',
      message: error.message 
    });
  }
});

/**
 * GET /api/goals/user/:userId/active
 * Get active goals for a user
 */
router.get('/user/:userId/active', verifyToken, async (req, res) => {
  try {
    const { userId } = req.params;
    
    if (req.user.uid !== userId) {
      return res.status(403).json({ 
        error: 'Forbidden',
        message: 'You can only access your own goals' 
      });
    }
    
    const snapshot = await db.collection('goals')
      .where('userId', '==', userId)
      .where('isActive', '==', true)
      .get();
    
    const goals = [];
    snapshot.forEach(doc => {
      goals.push({
        id: doc.id,
        ...doc.data()
      });
    });
    
    res.json({
      count: goals.length,
      goals
    });
    
  } catch (error) {
    console.error('Get active goals error:', error);
    res.status(500).json({ 
      error: 'Failed to fetch active goals',
      message: error.message 
    });
  }
});

/**
 * POST /api/goals
 * Create a new goal
 */
router.post('/', verifyToken, async (req, res) => {
  try {
    const userId = req.user.uid;
    const {
      goalType,
      targetValue,
      currentValue = 0,
      startDate,
      targetDate,
      description,
      isActive = true
    } = req.body;
    
    // Validation
    if (!goalType || !targetValue) {
      return res.status(400).json({ 
        error: 'Missing required fields',
        required: ['goalType', 'targetValue']
      });
    }
    
    const goalData = {
      userId,
      goalType,
      targetValue,
      currentValue,
      startDate: startDate || Date.now(),
      targetDate: targetDate || null,
      description: description || '',
      isActive,
      isCompleted: false,
      createdAt: Date.now(),
      updatedAt: Date.now()
    };
    
    const docRef = await db.collection('goals').add(goalData);
    
    res.status(201).json({
      message: 'Goal created successfully',
      goal: {
        id: docRef.id,
        ...goalData
      }
    });
    
  } catch (error) {
    console.error('Create goal error:', error);
    res.status(500).json({ 
      error: 'Failed to create goal',
      message: error.message 
    });
  }
});

/**
 * PUT /api/goals/:goalId
 * Update existing goal
 */
router.put('/:goalId', verifyToken, async (req, res) => {
  try {
    const { goalId } = req.params;
    const userId = req.user.uid;
    
    // Verify goal exists and belongs to user
    const doc = await db.collection('goals').doc(goalId).get();
    
    if (!doc.exists) {
      return res.status(404).json({ 
        error: 'Goal not found' 
      });
    }
    
    if (doc.data().userId !== userId) {
      return res.status(403).json({ 
        error: 'Forbidden',
        message: 'You can only update your own goals' 
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
    
    await db.collection('goals').doc(goalId).update(updateData);
    
    const updatedDoc = await db.collection('goals').doc(goalId).get();
    
    res.json({
      message: 'Goal updated successfully',
      goal: {
        id: updatedDoc.id,
        ...updatedDoc.data()
      }
    });
    
  } catch (error) {
    console.error('Update goal error:', error);
    res.status(500).json({ 
      error: 'Failed to update goal',
      message: error.message 
    });
  }
});

/**
 * PUT /api/goals/:goalId/progress
 * Update goal progress
 */
router.put('/:goalId/progress', verifyToken, async (req, res) => {
  try {
    const { goalId } = req.params;
    const userId = req.user.uid;
    const { currentValue } = req.body;
    
    if (currentValue === undefined) {
      return res.status(400).json({ 
        error: 'Missing currentValue' 
      });
    }
    
    // Verify goal exists and belongs to user
    const doc = await db.collection('goals').doc(goalId).get();
    
    if (!doc.exists) {
      return res.status(404).json({ 
        error: 'Goal not found' 
      });
    }
    
    if (doc.data().userId !== userId) {
      return res.status(403).json({ 
        error: 'Forbidden' 
      });
    }
    
    const goalData = doc.data();
    const isCompleted = currentValue >= goalData.targetValue;
    
    await db.collection('goals').doc(goalId).update({
      currentValue,
      isCompleted,
      updatedAt: Date.now(),
      ...(isCompleted && { completedAt: Date.now() })
    });
    
    const updatedDoc = await db.collection('goals').doc(goalId).get();
    
    res.json({
      message: 'Goal progress updated successfully',
      goal: {
        id: updatedDoc.id,
        ...updatedDoc.data()
      }
    });
    
  } catch (error) {
    console.error('Update goal progress error:', error);
    res.status(500).json({ 
      error: 'Failed to update goal progress',
      message: error.message 
    });
  }
});

/**
 * DELETE /api/goals/:goalId
 * Delete goal
 */
router.delete('/:goalId', verifyToken, async (req, res) => {
  try {
    const { goalId } = req.params;
    const userId = req.user.uid;
    
    // Verify goal exists and belongs to user
    const doc = await db.collection('goals').doc(goalId).get();
    
    if (!doc.exists) {
      return res.status(404).json({ 
        error: 'Goal not found' 
      });
    }
    
    if (doc.data().userId !== userId) {
      return res.status(403).json({ 
        error: 'Forbidden',
        message: 'You can only delete your own goals' 
      });
    }
    
    await db.collection('goals').doc(goalId).delete();
    
    res.json({
      message: 'Goal deleted successfully'
    });
    
  } catch (error) {
    console.error('Delete goal error:', error);
    res.status(500).json({ 
      error: 'Failed to delete goal',
      message: error.message 
    });
  }
});

module.exports = router;

