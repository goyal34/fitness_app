import React, { useEffect, useState } from 'react'
import { useParams } from 'react-router'
import { getActivity, getActivityDetail } from '../services/api';
import { Box, Card, CardContent, Divider, Typography } from '@mui/material';

const ActivityDetail = () => {
  const { id } = useParams();
  const [activity, setActivity] = useState(null);
  const [recommendation, setRecommendation] = useState(null);

  useEffect(() => {
    const fetchActivity = async () => {
      try {
        const response = await getActivity(id);
        setActivity(response.data);
      } catch (error) {
        console.error(error);
      }
    }

    const fetchRecommendation = async () => {
      try {
        const response = await getActivityDetail(id);
        setRecommendation(response.data);
      } catch (error) {
        // The AI recommendation is generated asynchronously, so it may not
        // exist yet for a newly created activity.
        console.error(error);
      }
    }

    fetchActivity();
    fetchRecommendation();
  }, [id]);

  if (!activity) {
    return <Typography>Loading...</Typography>
  }
  return (
    <Box sx={{ maxWidth: 800, mx: 'auto', p: 2 }}>
            <Card sx={{ mb: 2 }}>
                <CardContent>
                    <Typography variant="h5" gutterBottom>Activity Details</Typography>
                    <Typography>Type: {activity.type}</Typography>
                    <Typography>Duration: {activity.duration} minutes</Typography>
                    <Typography>Calories Burned: {activity.caloriesBurned}</Typography>
                    <Typography>Date: {new Date(activity.createdAt).toLocaleString()}</Typography>
                </CardContent>
            </Card>

            {recommendation ? (
                <Card>
                    <CardContent>
                        <Typography variant="h5" gutterBottom>AI Recommendation</Typography>
                        <Typography variant="h6">Analysis</Typography>
                        <Typography paragraph>{recommendation.recommendation}</Typography>

                        <Divider sx={{ my: 2 }} />

                        <Typography variant="h6">Improvements</Typography>
                        {recommendation.improvements?.map((improvement, index) => (
                            <Typography key={index} paragraph>• {improvement}</Typography>
                        ))}

                        <Divider sx={{ my: 2 }} />

                        <Typography variant="h6">Suggestions</Typography>
                        {recommendation.suggestions?.map((suggestion, index) => (
                            <Typography key={index} paragraph>• {suggestion}</Typography>
                        ))}

                        <Divider sx={{ my: 2 }} />

                        <Typography variant="h6">Safety Guidelines</Typography>
                        {recommendation.safety?.map((safety, index) => (
                            <Typography key={index} paragraph>• {safety}</Typography>
                        ))}
                    </CardContent>
                </Card>
            ) : (
                <Card>
                    <CardContent>
                        <Typography variant="h5" gutterBottom>AI Recommendation</Typography>
                        <Typography color="text.secondary">
                            No recommendation has been generated for this activity yet.
                        </Typography>
                    </CardContent>
                </Card>
            )}
        </Box>
  )
}

export default ActivityDetail
