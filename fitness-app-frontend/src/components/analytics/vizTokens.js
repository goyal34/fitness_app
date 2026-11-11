import { useLayoutEffect, useRef, useState } from 'react';

// Chart tokens (validated: categorical pair + ordinal blue ramp, light surface).
export const VIZ = {
    blue: '#2a78d6',
    orange: '#eb6834',
    ramp: ['#86b6ef', '#3987e5', '#1c5cab', '#0d366b'], // ordinal, light -> dark
    heat: ['#f0efec', '#b7d3f6', '#6da7ec', '#2a78d6', '#184f95'], // 0, then light -> dark
    ink: '#0b0b0b',
    inkSecondary: '#52514e',
    muted: '#898781',
    grid: '#e1e0d9',
    axis: '#c3c2b7',
    good: '#006300',
    critical: '#d03b3b',
};

/** Width of a container, kept up to date. */
export const useWidth = () => {
    const ref = useRef(null);
    const [width, setWidth] = useState(0);
    useLayoutEffect(() => {
        if (!ref.current) return undefined;
        const observer = new ResizeObserver(([entry]) => setWidth(entry.contentRect.width));
        observer.observe(ref.current);
        setWidth(ref.current.getBoundingClientRect().width);
        return () => observer.disconnect();
    }, []);
    return [ref, width];
};

