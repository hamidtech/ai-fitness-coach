import { useMemo, useState } from 'react';
import {
  Activity,
  Dumbbell,
  HeartPulse,
  History,
  Home,
  Search,
  Settings,
  User,
} from 'lucide-react';
import {
  BAND_TYPE_LABELS,
  CAMERA_VIEW_LABELS,
  CATEGORY_LABELS,
  EXERCISES,
  ExerciseCategory,
  ExerciseItem,
  ResistanceBandType,
  STATUS_LABELS,
} from '../data/exercises';
import { GenderOption, LoggedWorkoutSession, UserProfile } from '../types/profile';

type PhoneTab = 'home' | 'exercises' | 'history' | 'health' | 'settings';

interface PhonePreviewProps {
  isFa: boolean;
  darkMode: boolean;
  profile: UserProfile;
  onUpdateProfile: (next: UserProfile) => void;
  onToggleLanguage: () => void;
  onToggleDarkMode: () => void;
  sessions: LoggedWorkoutSession[];
  onAddQuickSession: (ex: ExerciseItem, bandType?: ResistanceBandType, bandColor?: string, bandLevel?: string, weightKg?: number) => void;
}

export function PhonePreview({
  isFa,
  darkMode,
  profile,
  onUpdateProfile,
  onToggleLanguage,
  onToggleDarkMode,
  sessions,
  onAddQuickSession,
}: PhonePreviewProps) {
  const [tab, setTab] = useState<PhoneTab>('home');
  const [categoryFilter, setCategoryFilter] = useState<ExerciseCategory | 'ALL'>('ALL');
  const [search, setSearch] = useState('');
  const [detailEx, setDetailEx] = useState<ExerciseItem | null>(null);

  // Quick equipment logging state inside Exercise Detail
  const [weightKg, setWeightKg] = useState(8);
  const [bandType, setBandType] = useState<ResistanceBandType>('LONG_LOOP');
  const [bandLevel, setBandLevel] = useState('Medium');
  const [bandColor, setBandColor] = useState('');
  const [healthEnabled, setHealthEnabled] = useState(false);
  const [hcWriteWorkouts, setHcWriteWorkouts] = useState(false);
  const [hcReadWorkouts, setHcReadWorkouts] = useState(false);
  const [hcReadSteps, setHcReadSteps] = useState(false);
  const [hcReadSleep, setHcReadSleep] = useState(false);
  const [hcReadHeartRate, setHcReadHeartRate] = useState(false);

  const bmi = useMemo(() => {
    const h = Number(profile.heightCm);
    const w = Number(profile.weightKg);
    if (!h || !w || h <= 0 || w <= 0) return null;
    return (w / Math.pow(h / 100, 2)).toFixed(1);
  }, [profile.heightCm, profile.weightKg]);

  const filteredExercises = useMemo(() => {
    const q = search.trim().toLowerCase();
    return EXERCISES.filter((ex) => {
      const catOk = categoryFilter === 'ALL' || ex.category === categoryFilter;
      const qOk =
        !q ||
        ex.nameEn.toLowerCase().includes(q) ||
        ex.nameFa.toLowerCase().includes(q) ||
        ex.targetMusclesEn.some((m) => m.toLowerCase().includes(q)) ||
        ex.targetMusclesFa.some((m) => m.toLowerCase().includes(q));
      return catOk && qOk;
    });
  }, [categoryFilter, search]);

  const genderThemeName = useMemo(() => {
    if (profile.gender === 'male') return isFa ? 'تم آبی ورزشی (مردانه)' : 'Athletic Cobalt Theme (Male)';
    if (profile.gender === 'female') return isFa ? 'تم رز و آلبالویی استودیویی (زنانه)' : 'Studio Rose-Plum Theme (Female)';
    return isFa ? 'تم زمردی متعادل (خنثی)' : 'Performance Emerald Theme (Neutral)';
  }, [profile.gender, isFa]);

  return (
    <section className="max-w-7xl mx-auto px-4 sm:px-6 py-8">
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
        {/* Left: Profile & Gender-Adaptive Theme Control Panel */}
        <div className="lg:col-span-5 rounded-2xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 p-6 space-y-5">
          <div>
            <h2 className="text-xl font-bold text-slate-900 dark:text-slate-100">
              {isFa ? 'پروفایل ورزشکار و تم پویا بر اساس جنسیت' : 'Athlete Profile & Gender-Adaptive Theme'}
            </h2>
            <p className="text-sm text-slate-600 dark:text-slate-400 mt-1">
              {isFa
                ? 'با تغییر جنسیت، پالت رنگی Material 3 در نسخه اندروید و پیش‌نمایش زنده فوراً تغییر می‌کند.'
                : 'Entering Name, Sex/Gender, Age, Height (cm), and Weight (kg) dynamically switches the Material 3 theme palette across the entire application.'}
            </p>
          </div>

          <div className="space-y-4">
            <div>
              <label className="block text-xs font-medium text-slate-600 dark:text-slate-400 mb-1">
                {isFa ? 'نام و نام خانوادگی' : 'Person Name'}
              </label>
              <input
                type="text"
                value={profile.name}
                onChange={(e) => onUpdateProfile({ ...profile, name: e.target.value })}
                placeholder={isFa ? 'مثلاً: سارا محمدی' : 'e.g. Sara Mohammadi'}
                className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-950 text-sm text-slate-900 dark:text-slate-100 focus:outline-none focus:ring-2"
              />
            </div>

            <div>
              <label className="block text-xs font-medium text-slate-600 dark:text-slate-400 mb-1.5">
                {isFa ? 'جنسیت (تعیین‌کننده تم)' : 'Sex / Gender (Drives App Theme)'}
              </label>
              <div className="grid grid-cols-3 gap-2">
                {([
                  { id: 'male', en: 'Male', fa: 'مرد' },
                  { id: 'female', en: 'Female', fa: 'زن' },
                  { id: 'neutral', en: 'Neutral', fa: 'خنثی' },
                ] as { id: GenderOption; en: string; fa: string }[]).map((g) => {
                  const active = profile.gender === g.id;
                  return (
                    <button
                      key={g.id}
                      type="button"
                      onClick={() => onUpdateProfile({ ...profile, gender: g.id })}
                      className={`py-2.5 px-3 rounded-xl text-sm font-medium border transition-colors whitespace-nowrap shrink-0 ${
                        active
                          ? 'text-white border-transparent'
                          : 'bg-slate-50 dark:bg-slate-950 text-slate-700 dark:text-slate-300 border-slate-200 dark:border-slate-700'
                      }`}
                      style={active ? { backgroundColor: 'var(--accent)' } : undefined}
                    >
                      {isFa ? g.fa : g.en}
                    </button>
                  );
                })}
              </div>
              <p className="text-xs mt-1.5 font-medium" style={{ color: 'var(--accent)' }}>
                {genderThemeName}
              </p>
            </div>

            <div className="grid grid-cols-3 gap-3">
              <div>
                <label className="block text-xs font-medium text-slate-600 dark:text-slate-400 mb-1">
                  {isFa ? 'سن' : 'Age'}
                </label>
                <input
                  type="number"
                  min={10}
                  max={110}
                  value={profile.age}
                  onChange={(e) =>
                    onUpdateProfile({
                      ...profile,
                      age: e.target.value === '' ? '' : Number(e.target.value),
                    })
                  }
                  placeholder="28"
                  className="w-full px-3 py-2 rounded-xl border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-950 text-sm tabular-nums text-slate-900 dark:text-slate-100"
                />
              </div>
              <div>
                <label className="block text-xs font-medium text-slate-600 dark:text-slate-400 mb-1">
                  {isFa ? 'قد (cm)' : 'Height (cm)'}
                </label>
                <input
                  type="number"
                  min={90}
                  max={250}
                  value={profile.heightCm}
                  onChange={(e) =>
                    onUpdateProfile({
                      ...profile,
                      heightCm: e.target.value === '' ? '' : Number(e.target.value),
                    })
                  }
                  placeholder="170"
                  className="w-full px-3 py-2 rounded-xl border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-950 text-sm tabular-nums text-slate-900 dark:text-slate-100"
                />
              </div>
              <div>
                <label className="block text-xs font-medium text-slate-600 dark:text-slate-400 mb-1">
                  {isFa ? 'وزن (kg)' : 'Weight (kg)'}
                </label>
                <input
                  type="number"
                  min={25}
                  max={300}
                  value={profile.weightKg}
                  onChange={(e) =>
                    onUpdateProfile({
                      ...profile,
                      weightKg: e.target.value === '' ? '' : Number(e.target.value),
                    })
                  }
                  placeholder="65"
                  className="w-full px-3 py-2 rounded-xl border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-950 text-sm tabular-nums text-slate-900 dark:text-slate-100"
                />
              </div>
            </div>

            {bmi && (
              <div className="rounded-xl px-4 py-3 text-xs border border-slate-200 dark:border-slate-800 bg-slate-50 dark:bg-slate-950 text-slate-700 dark:text-slate-300">
                {isFa
                  ? `شاخص توده بدنی محاسبه‌شده: ${bmi} kg/m² (صرفاً جهت اطلاع)`
                  : `Calculated BMI: ${bmi} kg/m² (informational only)`}
              </div>
            )}
          </div>
        </div>

        {/* Right: Interactive 5-Destination Android UI Preview */}
        <div className="lg:col-span-7 flex justify-center">
          <div className="w-full max-w-[420px] rounded-[28px] border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 shadow-lg overflow-hidden flex flex-col h-[690px]">
            {/* App Top Bar */}
            <div className="px-5 py-4 border-b border-slate-100 dark:border-slate-800 flex items-center justify-between">
              <div>
                <div className="text-[10px] font-medium uppercase tracking-wide text-slate-400">
                  {isFa ? 'شبیه‌ساز مرورگر (نسخه بومی در /android-app)' : 'React Browser Preview (Native App in /android-app)'}
                </div>
                <div className="text-base font-bold text-slate-900 dark:text-slate-100">
                  {isFa ? 'مربی هوشمند تناسب اندام' : 'AI Fitness Coach'}
                </div>
                <div className="text-xs text-slate-500 dark:text-slate-400">
                  {profile.name.trim()
                    ? isFa
                      ? `سلام، ${profile.name.trim()}`
                      : `Hello, ${profile.name.trim()}`
                    : isFa
                    ? 'خوش آمدید، ورزشکار'
                    : 'Welcome, Athlete'}
                </div>
              </div>
              <button
                type="button"
                onClick={onToggleLanguage}
                className="px-3 py-1.5 rounded-lg text-xs font-medium border border-slate-200 dark:border-slate-700 text-slate-700 dark:text-slate-200 whitespace-nowrap shrink-0"
              >
                {isFa ? 'English (LTR)' : 'فارسی (RTL)'}
              </button>
            </div>

            {/* Scrollable Screen Content */}
            <div className="flex-1 overflow-y-auto p-4 space-y-4">
              {tab === 'home' && (
                <>
                  <div
                    className="rounded-2xl p-4"
                    style={{ backgroundColor: 'var(--accent-soft)', color: 'var(--accent-on-soft)' }}
                  >
                    <div className="text-sm font-semibold">{genderThemeName}</div>
                    <div className="text-xs opacity-85 mt-1 tabular-nums">
                      {[
                        profile.age ? `${profile.age} ${isFa ? 'سال' : 'yrs'}` : null,
                        profile.heightCm ? `${profile.heightCm} cm` : null,
                        profile.weightKg ? `${profile.weightKg} kg` : null,
                      ]
                        .filter(Boolean)
                        .join(' · ') || (isFa ? 'مشخصات بدنی ثبت نشده' : 'Add age, height & weight')}
                    </div>
                  </div>

                  <button
                    type="button"
                    onClick={() => setTab('exercises')}
                    className="w-full py-3.5 px-4 rounded-xl text-white font-semibold text-sm flex items-center justify-center gap-2"
                    style={{ backgroundColor: 'var(--accent)' }}
                  >
                    <Activity className="w-4 h-4" />
                    {isFa ? 'شروع تمرین (انتخاب از ۳۹ حرکت)' : 'Start Workout (39 Exercises)'}
                  </button>

                  <div>
                    <div className="text-sm font-semibold text-slate-900 dark:text-slate-100 mb-2">
                      {isFa ? 'چهار دسته تمرینی' : 'Four Exercise Categories'}
                    </div>
                    <div className="grid grid-cols-2 gap-2.5">
                      {(Object.keys(CATEGORY_LABELS) as ExerciseCategory[]).map((cat) => {
                        const count = EXERCISES.filter((e) => e.category === cat).length;
                        return (
                          <button
                            key={cat}
                            type="button"
                            onClick={() => {
                              setCategoryFilter(cat);
                              setTab('exercises');
                            }}
                            className="text-start p-3 rounded-xl border border-slate-200 dark:border-slate-800 bg-slate-50 dark:bg-slate-950 hover:border-slate-300 dark:hover:border-slate-700"
                          >
                            <div className="text-xs font-semibold text-slate-900 dark:text-slate-100">
                              {isFa ? CATEGORY_LABELS[cat].fa : CATEGORY_LABELS[cat].en}
                            </div>
                            <div className="text-xs text-slate-500 mt-1 tabular-nums">
                              {count} {isFa ? 'حرکت' : 'exercises'}
                            </div>
                          </button>
                        );
                      })}
                    </div>
                  </div>

                  <div className="rounded-xl border border-slate-200 dark:border-slate-800 p-3.5">
                    <div className="text-xs text-slate-500 dark:text-slate-400">
                      {isFa ? 'خلاصه هفتگی (ذخیره محلی)' : 'Weekly Summary (Offline Local)'}
                    </div>
                    <div className="flex items-baseline justify-between mt-1.5">
                      <span className="text-xl font-bold tabular-nums" style={{ color: 'var(--accent)' }}>
                        {sessions.length} {isFa ? 'جلسه' : 'sessions'}
                      </span>
                      <span className="text-sm text-slate-600 dark:text-slate-400 tabular-nums">
                        {sessions.reduce((acc, s) => acc + s.durationMin, 0)} {isFa ? 'دقیقه' : 'min'}
                      </span>
                    </div>
                  </div>
                </>
              )}

              {tab === 'exercises' && !detailEx && (
                <>
                  <div className="flex items-center gap-2 px-3 py-2 rounded-xl border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-950">
                    <Search className="w-4 h-4 text-slate-400 shrink-0" />
                    <input
                      type="text"
                      value={search}
                      onChange={(e) => setSearch(e.target.value)}
                      placeholder={isFa ? 'جستجو در ۳۹ حرکت…' : 'Search 39 exercises…'}
                      className="w-full bg-transparent text-xs text-slate-900 dark:text-slate-100 focus:outline-none"
                    />
                  </div>

                  <div className="flex gap-1.5 overflow-x-auto pb-1">
                    <button
                      type="button"
                      onClick={() => setCategoryFilter('ALL')}
                      className={`px-2.5 py-1.5 rounded-lg text-xs font-medium whitespace-nowrap shrink-0 ${
                        categoryFilter === 'ALL'
                          ? 'text-white'
                          : 'bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-300'
                      }`}
                      style={categoryFilter === 'ALL' ? { backgroundColor: 'var(--accent)' } : undefined}
                    >
                      {isFa ? 'همه (۳۹)' : 'All (39)'}
                    </button>
                    {(Object.keys(CATEGORY_LABELS) as ExerciseCategory[]).map((cat) => (
                      <button
                        key={cat}
                        type="button"
                        onClick={() => setCategoryFilter(cat)}
                        className={`px-2.5 py-1.5 rounded-lg text-xs font-medium whitespace-nowrap shrink-0 ${
                          categoryFilter === cat
                            ? 'text-white'
                            : 'bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-300'
                        }`}
                        style={categoryFilter === cat ? { backgroundColor: 'var(--accent)' } : undefined}
                      >
                        {isFa ? CATEGORY_LABELS[cat].fa : CATEGORY_LABELS[cat].en}
                      </button>
                    ))}
                  </div>

                  <div className="space-y-2">
                    {filteredExercises.map((ex) => (
                      <button
                        key={ex.id}
                        type="button"
                        onClick={() => {
                          setDetailEx(ex);
                          if (ex.supportedBandTypes?.[0]) setBandType(ex.supportedBandTypes[0]);
                        }}
                        className="w-full text-start p-3 rounded-xl border border-slate-200 dark:border-slate-800 hover:bg-slate-50 dark:hover:bg-slate-800/60"
                      >
                        <div className="text-sm font-semibold text-slate-900 dark:text-slate-100">
                          {isFa ? ex.nameFa : ex.nameEn}
                        </div>
                        <div className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
                          {(isFa ? CATEGORY_LABELS[ex.category].fa : CATEGORY_LABELS[ex.category].en)}
                          {' · '}
                          {(isFa ? STATUS_LABELS[ex.status].fa : STATUS_LABELS[ex.status].en)}
                        </div>
                      </button>
                    ))}
                  </div>
                </>
              )}

              {tab === 'exercises' && detailEx && (
                <div className="space-y-3">
                  <button
                    type="button"
                    onClick={() => setDetailEx(null)}
                    className="text-xs font-medium hover:underline"
                    style={{ color: 'var(--accent)' }}
                  >
                    {isFa ? '← بازگشت به فهرست' : '← Back to library'}
                  </button>

                  <div>
                    <h3 className="text-base font-bold text-slate-900 dark:text-slate-100">
                      {isFa ? detailEx.nameFa : detailEx.nameEn}
                    </h3>
                    <p className="text-xs text-slate-500 mt-0.5">
                      {(isFa ? CAMERA_VIEW_LABELS[detailEx.cameraView].fa : CAMERA_VIEW_LABELS[detailEx.cameraView].en)}
                      {' · '}
                      {detailEx.demoVideoAsset}
                    </p>
                  </div>

                  <div className="rounded-xl border border-dashed border-slate-300 dark:border-slate-700 p-4 text-center text-xs text-slate-500">
                    {isFa
                      ? `جایگاه ویدئوی آفلاین: ${detailEx.demoVideoAsset}`
                      : `Offline demo media slot: ${detailEx.demoVideoAsset}`}
                  </div>

                  <div>
                    <div className="text-xs font-semibold text-slate-800 dark:text-slate-200 mb-1">
                      {isFa ? 'آموزش گام‌به‌گام' : 'Step-by-Step Instructions'}
                    </div>
                    <ol className="text-xs text-slate-600 dark:text-slate-400 space-y-1 list-decimal list-inside">
                      {(isFa ? detailEx.instructionsFa : detailEx.instructionsEn).map((line, i) => (
                        <li key={i}>{line}</li>
                      ))}
                    </ol>
                  </div>

                  {detailEx.anchorGuidanceEn && (
                    <div className="rounded-xl bg-amber-50 dark:bg-amber-950/30 border border-amber-200 dark:border-amber-800 p-3 text-xs text-amber-900 dark:text-amber-200">
                      {isFa ? detailEx.anchorGuidanceFa : detailEx.anchorGuidanceEn}
                    </div>
                  )}

                  {detailEx.category === 'WEIGHTED_TRAINING' && (
                    <div className="space-y-1.5">
                      <label className="block text-xs font-medium text-slate-700 dark:text-slate-300">
                        {isFa ? 'وزن هر دمبل (کیلوگرم)' : 'Weight per Dumbbell (kg)'}
                      </label>
                      <input
                        type="number"
                        value={weightKg}
                        onChange={(e) => setWeightKg(Number(e.target.value))}
                        className="w-full px-3 py-1.5 rounded-lg border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-950 text-xs tabular-nums"
                      />
                    </div>
                  )}

                  {detailEx.category === 'RESISTANCE_BAND_TRAINING' && (
                    <div className="space-y-2 rounded-xl border border-slate-200 dark:border-slate-800 p-3">
                      <div className="text-xs font-semibold text-slate-800 dark:text-slate-200">
                        {isFa ? 'مشخصات کش مقاومتی (فراداده)' : 'Resistance Band Metadata'}
                      </div>
                      <select
                        value={bandType}
                        onChange={(e) => setBandType(e.target.value as ResistanceBandType)}
                        className="w-full px-2.5 py-1.5 rounded-lg border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-950 text-xs"
                      >
                        {(detailEx.supportedBandTypes ?? ['LONG_LOOP']).map((bt) => (
                          <option key={bt} value={bt}>
                            {isFa ? BAND_TYPE_LABELS[bt].fa : BAND_TYPE_LABELS[bt].en}
                          </option>
                        ))}
                      </select>
                      <div className="grid grid-cols-2 gap-2">
                        <select
                          value={bandLevel}
                          onChange={(e) => setBandLevel(e.target.value)}
                          className="px-2.5 py-1.5 rounded-lg border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-950 text-xs"
                        >
                          <option value="Extra Light">Extra Light</option>
                          <option value="Light">Light</option>
                          <option value="Medium">Medium</option>
                          <option value="Heavy">Heavy</option>
                          <option value="Extra Heavy">Extra Heavy</option>
                        </select>
                        <input
                          type="text"
                          value={bandColor}
                          onChange={(e) => setBandColor(e.target.value)}
                          placeholder={isFa ? 'رنگ کش (اختیاری)' : 'Color (not standardized)'}
                          className="px-2.5 py-1.5 rounded-lg border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-950 text-xs"
                        />
                      </div>
                    </div>
                  )}

                  <div className="text-xs text-slate-500">
                    {isFa ? detailEx.limitationsFa : detailEx.limitationsEn}
                  </div>

                  <button
                    type="button"
                    onClick={() => {
                      onAddQuickSession(
                        detailEx,
                        detailEx.category === 'RESISTANCE_BAND_TRAINING' ? bandType : undefined,
                        bandColor,
                        bandLevel,
                        detailEx.category === 'WEIGHTED_TRAINING' ? weightKg : undefined
                      );
                      setDetailEx(null);
                      setTab('history');
                    }}
                    className="w-full py-2.5 rounded-xl text-white text-xs font-semibold"
                    style={{ backgroundColor: 'var(--accent)' }}
                  >
                    {isFa ? 'ثبت یک ست نمونه در تاریخچه' : 'Log Sample Set to History'}
                  </button>
                </div>
              )}

              {tab === 'history' && (
                <div className="space-y-3">
                  <div className="text-sm font-semibold text-slate-900 dark:text-slate-100">
                    {isFa ? 'تاریخچه تمرین‌های آفلاین' : 'Offline Workout History'}
                  </div>
                  {sessions.length === 0 ? (
                    <p className="text-xs text-slate-500">
                      {isFa ? 'هنوز جلسه‌ای ثبت نشده است.' : 'No workouts logged yet.'}
                    </p>
                  ) : (
                    sessions.map((s) => (
                      <div
                        key={s.id}
                        className="p-3.5 rounded-xl border border-slate-200 dark:border-slate-800 space-y-1.5"
                      >
                        <div className="text-sm font-semibold text-slate-900 dark:text-slate-100">
                          {isFa ? s.titleFa : s.titleEn}
                        </div>
                        <div className="text-xs text-slate-500 tabular-nums">
                          {new Date(s.dateIso).toLocaleDateString()} · {s.durationMin} {isFa ? 'دقیقه' : 'min'}
                        </div>
                        {s.sets.map((st, idx) => (
                          <div key={idx} className="text-xs text-slate-700 dark:text-slate-300">
                            {`Set ${st.setNumber}: ${st.holdSec > 0 ? `${st.holdSec}s` : `${st.reps} reps`}`}
                            {st.weightValue ? ` · ${st.weightValue} kg` : ''}
                            {st.bandType ? ` · ${BAND_TYPE_LABELS[st.bandType].en} (${st.bandLevel ?? 'Medium'})` : ''}
                          </div>
                        ))}
                      </div>
                    ))
                  )}
                </div>
              )}

              {tab === 'health' && (
                <div className="space-y-3">
                  <div
                    className="rounded-xl p-4"
                    style={{ backgroundColor: 'var(--accent-soft)', color: 'var(--accent-on-soft)' }}
                  >
                    <div className="text-sm font-semibold">
                      {isFa ? 'اتصال اختیاری Health Connect' : 'Optional Health Connect Integration'}
                    </div>
                    <p className="text-xs opacity-90 mt-1">
                      {isFa
                        ? 'در اندروید واقعی از طریق AndroidX Health Connect اجرا می‌شود. تمام بخش‌های تمرینی بدون آن نیز کامل کار می‌کنند.'
                        : 'Powered by AndroidX Health Connect on device. All tracking and history work offline without it.'}
                    </p>
                  </div>

                  <label className="flex items-center justify-between p-3 rounded-xl border border-slate-200 dark:border-slate-800 cursor-pointer">
                    <span className="text-xs font-medium text-slate-800 dark:text-slate-200">
                      {isFa ? 'فعال‌سازی همگام‌سازی Health Connect' : 'Enable Health Connect Sync'}
                    </span>
                    <input
                      type="checkbox"
                      checked={healthEnabled}
                      onChange={(e) => setHealthEnabled(e.target.checked)}
                    />
                  </label>

                  {healthEnabled && (
                    <div className="rounded-xl border border-slate-200 dark:border-slate-800 p-3 space-y-2">
                      <div className="text-[11px] text-slate-500 dark:text-slate-400">
                        {isFa
                          ? 'فقط مجوز قابلیت‌های فعال زیر درخواست می‌شود:'
                          : 'Permissions are requested strictly per enabled feature:'}
                      </div>
                      {([
                        {
                          checked: hcWriteWorkouts,
                          set: setHcWriteWorkouts,
                          en: 'Write Completed Workouts',
                          fa: 'ثبت تمرین‌های تکمیل‌شده',
                        },
                        {
                          checked: hcReadWorkouts,
                          set: setHcReadWorkouts,
                          en: 'Read Exercise Sessions',
                          fa: 'خواندن جلسات تمرینی',
                        },
                        {
                          checked: hcReadSteps,
                          set: setHcReadSteps,
                          en: 'Read Today Steps',
                          fa: 'خواندن قدم‌های امروز',
                        },
                        {
                          checked: hcReadSleep,
                          set: setHcReadSleep,
                          en: 'Read Sleep Duration',
                          fa: 'خواندن مدت خواب',
                        },
                        {
                          checked: hcReadHeartRate,
                          set: setHcReadHeartRate,
                          en: 'Read Heart Rate Samples',
                          fa: 'خواندن ضربان قلب',
                        },
                      ] as const).map((item, i) => (
                        <label key={i} className="flex items-center justify-between text-xs cursor-pointer">
                          <span className="text-slate-700 dark:text-slate-300">{isFa ? item.fa : item.en}</span>
                          <input
                            type="checkbox"
                            checked={item.checked}
                            onChange={(e) => item.set(e.target.checked)}
                          />
                        </label>
                      ))}
                    </div>
                  )}

                  <div className="rounded-xl border border-slate-200 dark:border-slate-800 p-3 space-y-1">
                    <div className="text-xs font-medium text-slate-800 dark:text-slate-200">
                      {isFa ? 'قدم‌های روزانه / ضربان قلب / خواب' : 'Daily Steps / Heart Rate / Sleep'}
                    </div>
                    <p className="text-xs text-slate-500">
                      {isFa
                        ? 'در شبیه‌ساز وب حسگر واقعی وجود ندارد (هیچ داده ساختگی نمایش داده نمی‌شود).'
                        : 'No real sensors in browser simulation (Android Health Connect reads run on physical Android devices).'}
                    </p>
                  </div>
                </div>
              )}

              {tab === 'settings' && (
                <div className="space-y-3 text-xs">
                  <div className="p-3.5 rounded-xl border border-slate-200 dark:border-slate-800 flex items-center justify-between">
                    <span className="font-medium text-slate-800 dark:text-slate-200">
                      {isFa ? 'زبان برنامه' : 'Language'}
                    </span>
                    <button
                      type="button"
                      onClick={onToggleLanguage}
                      className="px-3 py-1.5 rounded-lg border border-slate-200 dark:border-slate-700 font-medium"
                    >
                      {isFa ? 'فارسی (RTL)' : 'English (LTR)'}
                    </button>
                  </div>
                  <div className="p-3.5 rounded-xl border border-slate-200 dark:border-slate-800 flex items-center justify-between">
                    <span className="font-medium text-slate-800 dark:text-slate-200">
                      {isFa ? 'حالت تاریک / روشن' : 'Appearance'}
                    </span>
                    <button
                      type="button"
                      onClick={onToggleDarkMode}
                      className="px-3 py-1.5 rounded-lg border border-slate-200 dark:border-slate-700 font-medium"
                    >
                      {darkMode ? (isFa ? 'تیره' : 'Dark') : (isFa ? 'روشن' : 'Light')}
                    </button>
                  </div>
                  <div className="p-3.5 rounded-xl border border-slate-200 dark:border-slate-800 space-y-1">
                    <div className="font-semibold text-slate-800 dark:text-slate-200">
                      {isFa ? 'حریم خصوصی و پردازش محلی' : 'Privacy & On-Device Processing'}
                    </div>
                    <p className="text-slate-500 leading-relaxed">
                      {isFa
                        ? 'فریم‌های دوربین فقط در حافظه گوشی پردازش می‌شوند و هیچ ویدئویی ذخیره یا ارسال نمی‌گردد.'
                        : 'Camera frames are processed strictly in memory via MediaPipe. Zero cloud inference or video uploads.'}
                    </p>
                  </div>
                </div>
              )}
            </div>

            {/* Bottom 5-Destination Navigation Bar */}
            <nav className="grid grid-cols-5 border-t border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900">
              {([
                { id: 'home', en: 'Home', fa: 'خانه', icon: Home },
                { id: 'exercises', en: 'Exercises', fa: 'تمرین‌ها', icon: Dumbbell },
                { id: 'history', en: 'History', fa: 'تاریخچه', icon: History },
                { id: 'health', en: 'Health', fa: 'سلامت', icon: HeartPulse },
                { id: 'settings', en: 'Settings', fa: 'تنظیمات', icon: Settings },
              ] as const).map((item) => {
                const Icon = item.icon;
                const active = tab === item.id;
                return (
                  <button
                    key={item.id}
                    type="button"
                    onClick={() => {
                      setTab(item.id);
                      setDetailEx(null);
                    }}
                    className="py-2.5 flex flex-col items-center justify-center gap-1 text-[11px] font-medium"
                    style={active ? { color: 'var(--accent)' } : undefined}
                  >
                    <Icon className="w-4 h-4" />
                    <span className="truncate max-w-full px-1">{isFa ? item.fa : item.en}</span>
                  </button>
                );
              })}
            </nav>
          </div>
        </div>
      </div>
    </section>
  );
}
