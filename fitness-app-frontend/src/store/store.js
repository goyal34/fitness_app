import { configureStore } from "@reduxjs/toolkit";
import authReducer from "./authSlice";
import { fitnessApi } from "../services/fitnessApi";

export const store = configureStore({
    reducer: {
        auth: authReducer,
        [fitnessApi.reducerPath]: fitnessApi.reducer,
    },
    // Enables caching, invalidation and polling for RTK Query.
    middleware: (getDefaultMiddleware) =>
        getDefaultMiddleware().concat(fitnessApi.middleware),
});
