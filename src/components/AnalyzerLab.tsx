import { useEffect, useMemo, useRef, useState } from 'react';
import { Play, Pause, RotateCcw } from 'lucide-react';
import {
  AnalyzerId,
  AnalyzerSnapshot,
  MirrorExerciseAnalyzer,
  SyntheticBodyFrame,
  generateSyntheticFrame,
} from '../engine/poseAnalyzers';

interface AnalyzerLabProps {
  isFa: boolean;
}

const ANALYZER_OPTIONS: { id: AnalyzerId; labelEn: string; labelFa: string; catEn: string; catFa: string }[] = [
  {
    id: 'bodyweight_squat',
    labelEn: 'Bodyweight Squat',
    labelFa: 'اسکوات بدون وزنه',
    catEn: 'Bodyweight',
    catFa: 'وزن بدن',
  },
  {
    id: 'biceps_curl',
    labelEn: 'Dumbbell Biceps Curl',
    labelFa: 'جلو بازو دمبل',
    catEn: 'Weighted',
    catFa: 'با وزنه',
  },
  {
    id: 'forearm_plank',
    labelEn: 'Forearm Plank',
    labelFa: 'پلانک ساعد',
    catEn: 'Core',
    catFa: 'عضلات مرکزی',
  },
  {
    id: 'band_biceps_curl',
    labelEn: 'Resistance Band Biceps Curl',
    labelFa: 'جلو بازو با کش',
    catEn: 'Resistance Band',
    catFa: 'کش مقاومتی',
  },
];

export function AnalyzerLab({ isFa }: AnalyzerLabProps) {
  const [selectedId, setSelectedId] = useState<AnalyzerId>('bodyweight_squat');
  const [running, setRunning] = useState(true);
  const [simulatePartial, setSimulatePartial] = useState(false);
  const [simulateBadForm, setSimulateBadForm] = useState(false);
  const [simulateTrackingLoss, setSimulateTrackingLoss] = useState(false);
  const [step, setStep] = useState(0);

  const analyzerRef = useRef<MirrorExerciseAnalyzer>(new MirrorExerciseAnalyzer('bodyweight_squat'));

  useEffect(() => {
    analyzerRef.current = new MirrorExerciseAnalyzer(selectedId);
    setStep(0);
  }, [selectedId]);

  useEffect(() => {
    if (!running) return;
    const id = window.setInterval(() => {
      setStep((prev) => prev + 1);
    }, 100);
    return () => window.clearInterval(id);
  }, [running]);

  const { frame, snapshot }: { frame: SyntheticBodyFrame; snapshot: AnalyzerSnapshot } = useMemo(() => {
    const f = generateSyntheticFrame(selectedId, step, {
      simulatePartial,
      simulateBadForm,
      simulateTrackingLoss,
    });
    const s = analyzerRef.current.process(f);
    return { frame: f, snapshot: s };
  }, [selectedId, step, simulatePartial, simulateBadForm, simulateTrackingLoss]);

  const handleReset = () => {
    analyzerRef.current.reset();
    setStep(0);
  };

  const joints = [frame.shoulder, frame.elbow, frame.wrist, frame.hip, frame.knee, frame.ankle];
  const segments: [typeof frame.shoulder, typeof frame.shoulder][] = [
    [frame.shoulder, frame.elbow],
    [frame.elbow, frame.wrist],
    [frame.shoulder, frame.hip],
    [frame.hip, frame.knee],
    [frame.knee, frame.ankle],
  ];

  return (
    <section className="max-w-6xl mx-auto px-4 sm:px-6 py-8 space-y-6">
      <div className="flex flex-col md:flex-row md:items-end justify-between gap-4">
        <div>
          <h2 className="text-2xl font-bold text-slate-900 dark:text-slate-100">
            {isFa ? 'آزمایشگاه موتور تحلیل حرکت (شبیه‌ساز نقاط بدن)' : 'Deterministic Exercise Analyzer Lab'}
          </h2>
          <p className="text-sm text-slate-600 dark:text-slate-400 mt-1 max-w-2xl">
            {isFa
              ? 'این بخش منطق دقیق ماشین حالت محدود، هیسترزیس زوایای مفاصل و مدیریت خطای ردیابی کد کاتلین را با دنباله نقاط مصنوعی اجرا می‌کند.'
              : 'Runs the TypeScript mirror of the Kotlin finite-state machines, joint-angle hysteresis, incomplete-rep checks, and tracking-loss recovery using synthetic landmark sequences.'}
          </p>
        </div>

        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={() => setRunning((r) => !r)}
            className="inline-flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium text-white transition-colors whitespace-nowrap shrink-0"
            style={{ backgroundColor: 'var(--accent)' }}
          >
            {running ? <Pause className="w-4 h-4" /> : <Play className="w-4 h-4" />}
            {running ? (isFa ? 'توقف' : 'Pause') : (isFa ? 'اجرا' : 'Run')}
          </button>
          <button
            type="button"
            onClick={handleReset}
            className="inline-flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 text-slate-800 dark:text-slate-200 hover:bg-slate-50 dark:hover:bg-slate-800 whitespace-nowrap shrink-0"
          >
            <RotateCcw className="w-4 h-4" />
            {isFa ? 'بازنشانی' : 'Reset'}
          </button>
        </div>
      </div>

      {/* Analyzer Selector Tabs */}
      <div className="flex flex-wrap gap-2">
        {ANALYZER_OPTIONS.map((opt) => {
          const active = selectedId === opt.id;
          return (
            <button
              key={opt.id}
              type="button"
              onClick={() => setSelectedId(opt.id)}
              className={`px-3.5 py-2 rounded-lg text-sm font-medium transition-colors whitespace-nowrap shrink-0 border ${
                active
                  ? 'text-white border-transparent'
                  : 'bg-white dark:bg-slate-900 text-slate-700 dark:text-slate-300 border-slate-200 dark:border-slate-800 hover:bg-slate-50 dark:hover:bg-slate-800'
              }`}
              style={active ? { backgroundColor: 'var(--accent)' } : undefined}
            >
              {isFa ? opt.labelFa : opt.labelEn}
              <span className="opacity-75 mx-1.5">·</span>
              <span className="text-xs opacity-85">{isFa ? opt.catFa : opt.catEn}</span>
            </button>
          );
        })}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-stretch">
        {/* Skeleton Viewport */}
        <div className="lg:col-span-7 rounded-2xl border border-slate-200 dark:border-slate-800 bg-slate-950 text-white p-5 flex flex-col justify-between min-h-[380px]">
          <div className="flex items-center justify-between text-xs text-slate-400">
            <span>
              {snapshot.trackingOk
                ? isFa
                  ? 'ردیابی نقاط بدن فعال است'
                  : 'Landmark confidence nominal'
                : isFa
                ? 'خطای ردیابی — شمارش متوقف'
                : 'Low confidence — counting paused'}
            </span>
            <span className="font-mono tabular-nums">
              {snapshot.primaryAngleDeg !== null ? `${Math.round(snapshot.primaryAngleDeg)}°` : '—'}
            </span>
          </div>

          <div className="flex-1 flex items-center justify-center py-4">
            <svg viewBox="0 0 1 1" className="w-full max-w-[300px] h-[260px]">
              <line x1="0.08" y1="0.90" x2="0.92" y2="0.90" stroke="#334155" strokeWidth="0.008" />
              {snapshot.trackingOk && (
                <>
                  {segments.map(([a, b], i) => (
                    <line
                      key={i}
                      x1={a.x}
                      y1={a.y}
                      x2={b.x}
                      y2={b.y}
                      stroke="var(--accent)"
                      strokeWidth="0.022"
                      strokeLinecap="round"
                    />
                  ))}
                  {joints.map((j, i) => (
                    <circle
                      key={i}
                      cx={j.x}
                      cy={j.y}
                      r="0.024"
                      fill="#ffffff"
                      stroke="var(--accent)"
                      strokeWidth="0.01"
                    />
                  ))}
                </>
              )}
            </svg>
          </div>

          <div className="flex items-end justify-between border-t border-slate-800 pt-4">
            <div>
              <div className="text-xs text-slate-400">
                {selectedId === 'forearm_plank'
                  ? isFa
                    ? 'زمان نگهداشتن'
                    : 'Hold Duration'
                  : isFa
                  ? 'تعداد تکرار'
                  : 'Repetitions'}
              </div>
              <div className="text-4xl font-bold font-mono tabular-nums mt-0.5">
                {selectedId === 'forearm_plank'
                  ? `${(snapshot.holdDurationMs / 1000).toFixed(1)}s`
                  : snapshot.repCount}
              </div>
            </div>
            <div className="text-end">
              <div className="text-xs text-slate-400">{isFa ? 'فاز حرکت' : 'Phase'}</div>
              <div className="text-base font-semibold mt-0.5">
                {isFa ? snapshot.phaseFa : snapshot.phaseEn}
              </div>
            </div>
          </div>
        </div>

        {/* Fault Injection Controls & Coaching Output */}
        <div className="lg:col-span-5 rounded-2xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 p-5 flex flex-col justify-between gap-5">
          <div className="space-y-4">
            <h3 className="text-base font-semibold text-slate-900 dark:text-slate-100">
              {isFa ? 'شرایط آزمایشی و خطایابی' : 'Biomechanical Test Conditions'}
            </h3>

            <label className="flex items-center justify-between gap-3 py-2 border-b border-slate-100 dark:border-slate-800 cursor-pointer">
              <span className="text-sm text-slate-700 dark:text-slate-300">
                {isFa ? 'تکرار ناقص (دامنه ناکافی)' : 'Simulate Incomplete Range of Motion'}
              </span>
              <input
                type="checkbox"
                checked={simulatePartial}
                onChange={(e) => setSimulatePartial(e.target.checked)}
                className="w-4 h-4 accent-current"
                style={{ color: 'var(--accent)' }}
              />
            </label>

            <label className="flex items-center justify-between gap-3 py-2 border-b border-slate-100 dark:border-slate-800 cursor-pointer">
              <span className="text-sm text-slate-700 dark:text-slate-300">
                {isFa ? 'انحراف فرم (جابه‌جایی بازو / افتادگی لگن)' : 'Simulate Form Deviation (Arm Drift / Hip Sag)'}
              </span>
              <input
                type="checkbox"
                checked={simulateBadForm}
                onChange={(e) => setSimulateBadForm(e.target.checked)}
                className="w-4 h-4 accent-current"
                style={{ color: 'var(--accent)' }}
              />
            </label>

            <label className="flex items-center justify-between gap-3 py-2 border-b border-slate-100 dark:border-slate-800 cursor-pointer">
              <span className="text-sm text-slate-700 dark:text-slate-300">
                {isFa ? 'کاهش اطمینان نقاط بدن / قطع ردیابی' : 'Simulate Tracking Loss (Low Confidence)'}
              </span>
              <input
                type="checkbox"
                checked={simulateTrackingLoss}
                onChange={(e) => setSimulateTrackingLoss(e.target.checked)}
                className="w-4 h-4 accent-current"
                style={{ color: 'var(--accent)' }}
              />
            </label>
          </div>

          <div
            className={`rounded-xl p-4 border ${
              snapshot.warning
                ? 'bg-amber-50 dark:bg-amber-950/30 border-amber-200 dark:border-amber-800 text-amber-900 dark:text-amber-200'
                : 'bg-slate-50 dark:bg-slate-800/60 border-slate-200 dark:border-slate-700 text-slate-800 dark:text-slate-200'
            }`}
          >
            <div className="text-xs font-medium opacity-75">
              {isFa ? 'پیام مربی (قطعی و بدون امتیاز ساختگی)' : 'Deterministic Coaching Message'}
            </div>
            <p className="text-sm font-medium mt-1">
              {isFa ? snapshot.feedbackFa : snapshot.feedbackEn}
            </p>
            {selectedId === 'band_biceps_curl' && (
              <p className="text-xs opacity-80 mt-2">
                {isFa
                  ? 'تحلیلگر کش: فقط حرکت آرنج و بازو بررسی می‌شود؛ نیروی واقعی کش از تصویر تخمین زده نمی‌شود.'
                  : 'Band analyzer tracks elbow/arm kinematics only; band tension force is never inferred from video.'}
              </p>
            )}
          </div>
        </div>
      </div>
    </section>
  );
}
