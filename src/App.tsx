import { useEffect, useState } from 'react';
import { AnalyzerLab } from './components/AnalyzerLab';
import { AndroidCodeExplorer } from './components/AndroidCodeExplorer';
import { PhonePreview } from './components/PhonePreview';
import { ExerciseItem, ResistanceBandType } from './data/exercises';
import { LoggedWorkoutSession, UserProfile } from './types/profile';

const workspaceSections = [
  { id: 'preview', en: 'Web Preview', fa: 'شبیه‌ساز وب' },
  { id: 'analyzer', en: 'Analyzer Lab', fa: 'آزمایشگاه تحلیل' },
  { id: 'code', en: 'Android Code', fa: 'کد اندروید' },
] as const;

type WorkspaceSection = (typeof workspaceSections)[number]['id'];

function sectionFromHash(): WorkspaceSection {
  const id = window.location.hash.slice(1);
  return workspaceSections.find((item) => item.id === id)?.id ?? 'preview';
}

const PROFILE_KEY = 'aifc_profile_v1';
const SESSIONS_KEY = 'aifc_sessions_v1';
const LANG_KEY = 'aifc_lang_v1';
const DARK_KEY = 'aifc_dark_v1';

const DEFAULT_PROFILE: UserProfile = {
  name: '',
  gender: 'neutral',
  age: '',
  heightCm: '',
  weightKg: '',
};

export default function App() {
  const [section, setSection] = useState<WorkspaceSection>(sectionFromHash);
  const [isFa, setIsFa] = useState<boolean>(() => {
    const saved = localStorage.getItem(LANG_KEY);
    if (saved === 'fa') return true;
    if (saved === 'en') return false;
    return typeof navigator !== 'undefined' && navigator.language.toLowerCase().startsWith('fa');
  });
  const [darkMode, setDarkMode] = useState<boolean>(() => localStorage.getItem(DARK_KEY) === 'true');
  const [profile, setProfile] = useState<UserProfile>(() => {
    try {
      const raw = localStorage.getItem(PROFILE_KEY);
      return raw ? { ...DEFAULT_PROFILE, ...JSON.parse(raw) } : DEFAULT_PROFILE;
    } catch {
      return DEFAULT_PROFILE;
    }
  });
  const [sessions, setSessions] = useState<LoggedWorkoutSession[]>(() => {
    try {
      const raw = localStorage.getItem(SESSIONS_KEY);
      return raw ? JSON.parse(raw) : [];
    } catch {
      return [];
    }
  });

  useEffect(() => {
    const syncLocation = () => setSection(sectionFromHash());
    window.addEventListener('hashchange', syncLocation);
    return () => window.removeEventListener('hashchange', syncLocation);
  }, []);

  useEffect(() => {
    const root = document.documentElement;
    root.setAttribute('data-gender', profile.gender);
    root.setAttribute('dir', isFa ? 'rtl' : 'ltr');
    root.setAttribute('lang', isFa ? 'fa' : 'en');
    if (darkMode) root.classList.add('dark');
    else root.classList.remove('dark');
  }, [profile.gender, isFa, darkMode]);

  useEffect(() => {
    localStorage.setItem(PROFILE_KEY, JSON.stringify(profile));
  }, [profile]);

  useEffect(() => {
    localStorage.setItem(SESSIONS_KEY, JSON.stringify(sessions));
  }, [sessions]);

  useEffect(() => {
    localStorage.setItem(LANG_KEY, isFa ? 'fa' : 'en');
  }, [isFa]);

  useEffect(() => {
    localStorage.setItem(DARK_KEY, String(darkMode));
  }, [darkMode]);

  const handleAddQuickSession = (
    ex: ExerciseItem,
    bandType?: ResistanceBandType,
    bandColor?: string,
    bandLevel?: string,
    weightKg?: number
  ) => {
    const newSession: LoggedWorkoutSession = {
      id: `${Date.now()}`,
      titleEn: ex.nameEn,
      titleFa: ex.nameFa,
      dateIso: new Date().toISOString(),
      durationMin: 12,
      syncedToHealthConnect: false,
      sets: [
        {
          exerciseId: ex.id,
          category: ex.category,
          setNumber: 1,
          reps: ex.mode === 'TIMED_HOLD' ? 0 : 10,
          holdSec: ex.mode === 'TIMED_HOLD' ? 35 : 0,
          weightValue: weightKg,
          weightMode: weightKg ? 'PER_DUMBBELL' : undefined,
          bandType,
          bandColor,
          bandLevel,
          observations: [],
        },
      ],
    };
    setSessions((prev) => [newSession, ...prev]);
  };

  return (
    <div className="min-h-screen text-slate-900 dark:text-slate-100 transition-colors">
      <header className="sticky top-0 z-30 border-b border-slate-200 bg-white dark:border-slate-800 dark:bg-slate-950">
        <div className="mx-auto flex max-w-7xl flex-wrap items-center justify-between gap-2 px-4 py-2 sm:px-6">
          <a href="#preview" className="py-2 text-lg font-bold tracking-tight">
            {isFa ? 'مربی هوشمند تناسب اندام' : 'AI Fitness Coach'}
          </a>
          <div className="flex shrink-0 items-center gap-1 md:order-3">
            <button type="button" aria-pressed={darkMode} onClick={() => setDarkMode((value) => !value)}
              className="min-h-12 workspace-control rounded-lg px-3 text-sm font-medium hover:bg-slate-100 focus-visible:ring-2 dark:hover:bg-slate-800">
              {darkMode ? (isFa ? 'روشن' : 'Light') : (isFa ? 'تیره' : 'Dark')}
            </button>
            <button type="button" onClick={() => setIsFa((value) => !value)}
              className="min-h-12 workspace-control rounded-lg px-3 text-sm font-semibold hover:bg-slate-100 focus-visible:ring-2 dark:hover:bg-slate-800">
              {isFa ? 'English' : 'فارسی'}
            </button>
          </div>
          <nav aria-label={isFa ? 'بخش‌های سایت' : 'Workspace navigation'}
            className="order-3 flex w-full flex-wrap gap-1 text-sm md:order-2 md:w-auto">
            {workspaceSections.map((item) => (
              <a key={item.id} href={`#${item.id}`}
                aria-current={section === item.id ? 'page' : undefined}
                className={`workspace-control inline-flex min-h-12 items-center rounded-lg px-3 font-medium focus-visible:ring-2 ${section === item.id
                  ? 'bg-slate-900 text-white dark:bg-slate-100 dark:text-slate-900'
                  : 'hover:bg-slate-100 dark:hover:bg-slate-800'}`}>
                {isFa ? item.fa : item.en}
              </a>
            ))}
          </nav>
        </div>
      </header>

      {/* Hero Summary */}
      <div className="border-b border-slate-200 dark:border-slate-800 bg-white/60 dark:bg-slate-950/60">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 py-7 flex flex-col lg:flex-row lg:items-center justify-between gap-6">
          <div className="max-w-2xl space-y-2">
            <div className="text-xs text-slate-500 dark:text-slate-400">
              <span>{isFa ? 'پروژه واقعی اندروید: /android-app (Kotlin / Compose)' : 'Real Native Android App: /android-app (Kotlin / Compose)'}</span>
              <span aria-hidden="true" className="mx-2">·</span>
              <span>{isFa ? 'این صفحه: شبیه‌ساز و مرورگر کد در وب' : 'This Page: React Browser Companion'}</span>
            </div>
            <h1 className="text-2xl sm:text-3xl font-bold tracking-tight text-slate-950 dark:text-white text-balance">
              {isFa
                ? 'مربی هوشمند تناسب اندام با تحلیل حرکت روی دستگاه'
                : 'AI Fitness Coach — On-Device Pose Analysis & Gender-Adaptive Material 3'}
            </h1>
            <p className="text-sm text-slate-600 dark:text-slate-400 leading-relaxed">
              {isFa
                ? 'اپلیکیشن واقعی اندروید (Kotlin، Jetpack Compose، CameraX، MediaPipe Pose Landmarker با مدل pose_landmarker_lite.task، Room، DataStore و Health Connect با مجوزهای تفکیک‌شده) در پوشه android-app قرار دارد. این رابط React صرفاً یک شبیه‌ساز مرورگر برای بررسی جریان کار و مطالعه فایل‌های اندروید است (APK در محیط مرورگر ساخته نمی‌شود).'
                : 'The real native Android application (Kotlin, Jetpack Compose, CameraX, MediaPipe Pose Landmarker with bundled pose_landmarker_lite.task, Room, DataStore, and per-feature Health Connect) lives in /android-app. This React interface is a browser-based preview & source inspector—not the Android runtime or an APK builder.'}
            </p>
          </div>

          <div className="grid grid-cols-3 gap-6 shrink-0 text-start">
            <div>
              <div className="text-2xl font-bold tabular-nums" style={{ color: 'var(--accent)' }}>39</div>
              <div className="text-xs text-slate-500 dark:text-slate-400">
                {isFa ? 'حرکت در ۴ دسته' : 'Exercises (4 Categories)'}
              </div>
            </div>
            <div>
              <div className="text-2xl font-bold tabular-nums" style={{ color: 'var(--accent)' }}>4</div>
              <div className="text-xs text-slate-500 dark:text-slate-400">
                {isFa ? 'تحلیلگر فعال' : 'Working Analyzers'}
              </div>
            </div>
            <div>
              <div className="text-2xl font-bold tabular-nums" style={{ color: 'var(--accent)' }}>3</div>
              <div className="text-xs text-slate-500 dark:text-slate-400">
                {isFa ? 'تم جنسیتی M3' : 'Gender M3 Themes'}
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Main Content */}
      <main key={section} className="workspace-panel">
        {section === 'preview' && (
          <PhonePreview
            isFa={isFa}
            darkMode={darkMode}
            profile={profile}
            onUpdateProfile={setProfile}
            onToggleLanguage={() => setIsFa((f) => !f)}
            onToggleDarkMode={() => setDarkMode((d) => !d)}
            sessions={sessions}
            onAddQuickSession={handleAddQuickSession}
          />
        )}
        {section === 'analyzer' && <AnalyzerLab isFa={isFa} />}
        {section === 'code' && <AndroidCodeExplorer isFa={isFa} />}
      </main>
    </div>
  );
}
