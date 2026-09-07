export const CITIES = ['Seattle', 'Tacoma', 'Bainbridge Island'] as const;

export type City = (typeof CITIES)[number];
