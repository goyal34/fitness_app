import { createApi, fetchBaseQuery } from '@reduxjs/toolkit/query/react';

const API_URL = 'http://localhost:8080/api';

export const fitnessApi = createApi({
    reducerPath: 'fitnessApi',
    baseQuery: fetchBaseQuery({
        baseUrl: API_URL,
        prepareHeaders: (headers) => {
            const token = localStorage.getItem('token');
            if (token) {
                headers.set('Authorization', `Bearer ${token}`);
            }
            return headers;
        },
    }),
    tagTypes: ['Activity', 'Analytics', 'Recommendation'],
    endpoints: (builder) => ({
        getActivities: builder.query({
            query: () => '/activities',
            providesTags: (result) => (
                result
                    ? [...result.map(({ id }) => ({ type: 'Activity', id })), { type: 'Activity', id: 'LIST' }]
                    : [{ type: 'Activity', id: 'LIST' }]
            ),
        }),

        getActivity: builder.query({
            query: (id) => `/activities/${id}`,
            providesTags: (result, error, id) => [{ type: 'Activity', id }],
        }),

        addActivity: builder.mutation({
            query: (activity) => ({
                url: '/activities',
                method: 'POST',
                body: activity,
            }),
            invalidatesTags: [
                { type: 'Activity', id: 'LIST' },
                'Analytics',
            ],
        }),

        deleteActivity: builder.mutation({
            query: (id) => ({
                url: `/activities/${id}`,
                method: 'DELETE',
            }),
            invalidatesTags: (result, error, id) => [
                { type: 'Activity', id },
                { type: 'Activity', id: 'LIST' },
                { type: 'Recommendation', id },
                'Analytics',
            ],
            async onQueryStarted(id, { dispatch, queryFulfilled }) {
                const patchResult = dispatch(
                    fitnessApi.util.updateQueryData('getActivities', undefined, (draft) => {
                        const index = draft.findIndex((activity) => activity.id === id);
                        if (index !== -1) draft.splice(index, 1);
                    })
                );
                try {
                    await queryFulfilled;
                } catch {
                    patchResult.undo();
                }
            },
        }),

        getActivityRecommendation: builder.query({
            query: (id) => `/recommendations/activity/${id}`,
            providesTags: (result, error, id) => [{ type: 'Recommendation', id }],
        }),

        getAnalyticsDashboard: builder.query({
            query: () => '/activities/analytics/dashboard',
            providesTags: ['Analytics'],
        }),
    }),
});

export const {
    useGetActivitiesQuery,
    useGetActivityQuery,
    useAddActivityMutation,
    useDeleteActivityMutation,
    useGetActivityRecommendationQuery,
    useGetAnalyticsDashboardQuery,
} = fitnessApi;
