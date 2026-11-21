import { configureStore } from "@reduxjs/toolkit";
import authReducer from "./authSlice";
import { fitnessApi } from "../services/fitnessApi";

export const store = configureStore({
    reducer: {
        auth: authReducer,
        [fitnessApi.reducerPath]: fitnessApi.reducer,
    },
    middleware: (getDefaultMiddleware) =>
        getDefaultMiddleware().concat(fitnessApi.middleware),
});
