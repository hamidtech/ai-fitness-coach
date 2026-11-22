import { ExerciseCategory, ResistanceBandType } from '../data/exercises';

export type GenderOption = 'male' | 'female' | 'neutral';

export interface UserProfile {
  name: string;
  gender: GenderOption;
  age: number | '';
  heightCm: number | '';
  weightKg: number | '';
}

export interface LoggedSet {
  exerciseId: string;
  category: ExerciseCategory;
  setNumber: number;
  reps: number;
  holdSec: number;
  weightValue?: number;
  weightMode?: 'PER_DUMBBELL' | 'TOTAL_LOAD';
  bandType?: ResistanceBandType;
  bandLevel?: string;
  bandColor?: string;
  bandBrand?: string;
  observations: string[];
}

export interface LoggedWorkoutSession {
  id: string;
  titleEn: string;
  titleFa: string;
  dateIso: string;
  durationMin: number;
  sets: LoggedSet[];
  syncedToHealthConnect: boolean;
}
