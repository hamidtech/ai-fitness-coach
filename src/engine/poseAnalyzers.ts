export interface Point2D {
  x: number;
  y: number;
  confidence: number;
}

export interface SyntheticBodyFrame {
  timestampMs: number;
  shoulder: Point2D;
  elbow: Point2D;
  wrist: Point2D;
  hip: Point2D;
  knee: Point2D;
  ankle: Point2D;
}

export type AnalyzerId =
  | 'bodyweight_squat'
  | 'biceps_curl'
  | 'forearm_plank'
  | 'band_biceps_curl';

export interface AnalyzerSnapshot {
  phaseEn: string;
  phaseFa: string;
  repCount: number;
  holdDurationMs: number;
  primaryAngleDeg: number | null;
  secondaryAngleDeg: number | null;
  trackingOk: boolean;
  feedbackEn: string;
  feedbackFa: string;
  warning: boolean;
}

export function angleDegrees2D(a: Point2D, b: Point2D, c: Point2D): number {
  const bax = a.x - b.x;
  const bay = a.y - b.y;
  const bcx = c.x - b.x;
  const bcy = c.y - b.y;
  const magBa = Math.hypot(bax, bay);
  const magBc = Math.hypot(bcx, bcy);
  if (magBa < 1e-5 || magBc < 1e-5) return 0;
  const cosTheta = Math.max(-1, Math.min(1, (bax * bcx + bay * bcy) / (magBa * magBc)));
  return (Math.acos(cosTheta) * 180) / Math.PI;
}

export function segmentAngleFromVerticalDeg(a: Point2D, b: Point2D): number {
  const dx = Math.abs(b.x - a.x);
  const dy = Math.abs(b.y - a.y);
  const mag = Math.hypot(dx, dy);
  if (mag < 1e-5) return 0;
  return (Math.acos(Math.max(-1, Math.min(1, dy / mag))) * 180) / Math.PI;
}

export function generateSyntheticFrame(
  analyzerId: AnalyzerId,
  step: number,
  options: { simulatePartial?: boolean; simulateBadForm?: boolean; simulateTrackingLoss?: boolean }
): SyntheticBodyFrame {
  const conf = options.simulateTrackingLoss ? 0.18 : 0.94;
  const timestampMs = step * 100;

  if (analyzerId === 'bodyweight_squat') {
    const cycle = (step % 28) / 28; // 0 -> 1
    const depth = Math.sin(cycle * Math.PI); // 0 -> 1 -> 0
    const maxDrop = options.simulatePartial ? 0.12 : 0.28;
    const hipY = 0.48 + depth * maxDrop;
    const kneeX = 0.48 + depth * (options.simulatePartial ? 0.05 : 0.14);
    const kneeY = hipY + (options.simulatePartial ? 0.16 : 0.03 + (1 - depth) * 0.17);
    return {
      timestampMs,
      shoulder: { x: 0.44 + depth * 0.05, y: hipY - 0.24, confidence: conf },
      elbow: { x: 0.52, y: hipY - 0.16, confidence: conf },
      wrist: { x: 0.62, y: hipY - 0.18, confidence: conf },
      hip: { x: 0.42, y: hipY, confidence: conf },
      knee: { x: kneeX, y: kneeY, confidence: conf },
      ankle: { x: 0.46, y: 0.88, confidence: conf },
    };
  }

  if (analyzerId === 'biceps_curl' || analyzerId === 'band_biceps_curl') {
    const cycle = (step % 24) / 24;
    const flex = Math.sin(cycle * Math.PI);
    const maxAngleSpan = options.simulatePartial ? 0.42 : 1.0;
    const theta = (0.1 + flex * 2.1 * maxAngleSpan); // radians from straight down
    const driftX = options.simulateBadForm ? flex * 0.14 : 0.01;
    const shoulder = { x: 0.42, y: 0.28, confidence: conf };
    const elbow = { x: 0.42 + driftX, y: 0.53, confidence: conf };
    const forearmLen = 0.22;
    const wrist = {
      x: elbow.x + Math.sin(theta) * forearmLen,
      y: elbow.y + Math.cos(theta) * forearmLen,
      confidence: conf,
    };
    return {
      timestampMs,
      shoulder,
      elbow,
      wrist,
      hip: { x: 0.42, y: 0.62, confidence: conf },
      knee: { x: 0.42, y: 0.78, confidence: conf },
      ankle: { x: 0.42, y: 0.92, confidence: conf },
    };
  }

  // forearm_plank
  const hipSagOffset = options.simulateBadForm ? 0.12 : 0.0;
  return {
    timestampMs,
    shoulder: { x: 0.24, y: 0.58, confidence: conf },
    elbow: { x: 0.24, y: 0.72, confidence: conf },
    wrist: { x: 0.34, y: 0.72, confidence: conf },
    hip: { x: 0.50, y: 0.58 + hipSagOffset, confidence: conf },
    knee: { x: 0.65, y: 0.58 + hipSagOffset * 0.4, confidence: conf },
    ankle: { x: 0.78, y: 0.58, confidence: conf },
  };
}

export class MirrorExerciseAnalyzer {
  private phase = 'IDLE';
  private repCount = 0;
  private holdDurationMs = 0;
  private lastTs: number | null = null;
  private minAngle = 180;
  private phaseEnteredAt = 0;

  constructor(public readonly analyzerId: AnalyzerId) {}

  reset() {
    this.phase = 'IDLE';
    this.repCount = 0;
    this.holdDurationMs = 0;
    this.lastTs = null;
    this.minAngle = 180;
    this.phaseEnteredAt = 0;
  }

  process(frame: SyntheticBodyFrame): AnalyzerSnapshot {
    const reqConf = 0.55;
    const trackingOk =
      frame.shoulder.confidence >= reqConf &&
      frame.hip.confidence >= reqConf &&
      (this.analyzerId === 'biceps_curl' || this.analyzerId === 'band_biceps_curl'
        ? frame.elbow.confidence >= reqConf && frame.wrist.confidence >= reqConf
        : frame.knee.confidence >= reqConf && frame.ankle.confidence >= reqConf);

    if (!trackingOk) {
      this.lastTs = null;
      return {
        phaseEn: 'Paused — Tracking Lost',
        phaseFa: 'متوقف — خطای ردیابی',
        repCount: this.repCount,
        holdDurationMs: this.holdDurationMs,
        primaryAngleDeg: null,
        secondaryAngleDeg: null,
        trackingOk: false,
        feedbackEn: 'Counting paused until required landmarks are clearly visible.',
        feedbackFa: 'شمارش تا زمان دیده شدن واضح نقاط بدن متوقف شد.',
        warning: true,
      };
    }

    if (this.analyzerId === 'bodyweight_squat') {
      const kneeAngle = angleDegrees2D(frame.hip, frame.knee, frame.ankle);
      const hipAngle = angleDegrees2D(frame.shoulder, frame.hip, frame.knee);
      const elapsed = frame.timestampMs - this.phaseEnteredAt;
      let feedbackEn = 'Position ready. Begin movement.';
      let feedbackFa = 'وضعیت آماده است. حرکت را شروع کنید.';
      let warning = false;

      if (this.phase === 'IDLE' || this.phase === 'STANDING') {
        if (kneeAngle >= 160) {
          this.phase = 'STANDING';
        } else if (kneeAngle < 148 && elapsed >= 140) {
          this.phase = 'DESCENDING';
          this.phaseEnteredAt = frame.timestampMs;
          this.minAngle = kneeAngle;
          feedbackEn = 'Lowering… keep chest lifted.';
          feedbackFa = 'در حال پایین آمدن… سینه را بالا نگه دارید.';
        }
      } else if (this.phase === 'DESCENDING') {
        this.minAngle = Math.min(this.minAngle, kneeAngle);
        if (kneeAngle <= 105 && elapsed >= 140) {
          this.phase = 'BOTTOM';
          this.phaseEnteredAt = frame.timestampMs;
          feedbackEn = 'Good squat depth. Drive up smoothly.';
          feedbackFa = 'عمق اسکوات مناسب است. با کنترل بالا بیایید.';
        } else if (kneeAngle >= 160 && elapsed >= 140) {
          this.phase = 'STANDING';
          this.phaseEnteredAt = frame.timestampMs;
          feedbackEn = 'Incomplete rep — lower hips slightly deeper if comfortable.';
          feedbackFa = 'تکرار ناقص — در صورت راحتی کمی پایین‌تر بروید.';
          warning = true;
        }
      } else if (this.phase === 'BOTTOM') {
        this.minAngle = Math.min(this.minAngle, kneeAngle);
        if (kneeAngle >= 114 && elapsed >= 140) {
          this.phase = 'ASCENDING';
          this.phaseEnteredAt = frame.timestampMs;
        }
        feedbackEn = 'Good squat depth. Drive up smoothly.';
        feedbackFa = 'عمق اسکوات مناسب است. با کنترل بالا بیایید.';
      } else if (this.phase === 'ASCENDING') {
        if (kneeAngle >= 160 && elapsed >= 140) {
          this.repCount += 1;
          this.phase = 'STANDING';
          this.phaseEnteredAt = frame.timestampMs;
          this.minAngle = 180;
        }
      }

      const phaseMap: Record<string, { en: string; fa: string }> = {
        IDLE: { en: 'Ready', fa: 'آماده' },
        STANDING: { en: 'Standing', fa: 'ایستاده' },
        DESCENDING: { en: 'Descending', fa: 'پایین آمدن' },
        BOTTOM: { en: 'Bottom Position', fa: 'پایین‌ترین نقطه' },
        ASCENDING: { en: 'Ascending', fa: 'بالا آمدن' },
      };

      return {
        phaseEn: phaseMap[this.phase]?.en ?? this.phase,
        phaseFa: phaseMap[this.phase]?.fa ?? this.phase,
        repCount: this.repCount,
        holdDurationMs: 0,
        primaryAngleDeg: kneeAngle,
        secondaryAngleDeg: hipAngle,
        trackingOk: true,
        feedbackEn,
        feedbackFa,
        warning,
      };
    }

    if (this.analyzerId === 'biceps_curl' || this.analyzerId === 'band_biceps_curl') {
      const elbowAngle = angleDegrees2D(frame.shoulder, frame.elbow, frame.wrist);
      const drift = segmentAngleFromVerticalDeg(frame.shoulder, frame.elbow);
      const elapsed = frame.timestampMs - this.phaseEnteredAt;
      let feedbackEn = 'Position ready. Begin movement.';
      let feedbackFa = 'وضعیت آماده است. حرکت را شروع کنید.';
      let warning = false;

      if (this.phase === 'IDLE' || this.phase === 'EXTENDING') {
        if (elbowAngle >= 148) {
          this.phase = 'EXTENDING';
        } else if (elbowAngle <= 135 && elapsed >= 130) {
          this.phase = 'FLEXING';
          this.phaseEnteredAt = frame.timestampMs;
          this.minAngle = elbowAngle;
          feedbackEn = 'Curling up smoothly.';
          feedbackFa = 'در حال خم کردن آرنج با کنترل.';
        }
      } else if (this.phase === 'FLEXING') {
        this.minAngle = Math.min(this.minAngle, elbowAngle);
        if (drift > 22) {
          feedbackEn = 'Keep upper arm steadier near your torso.';
          feedbackFa = 'بازو را ثابت‌تر در کنار تنه نگه دارید.';
          warning = true;
        } else {
          feedbackEn = 'Curling up smoothly.';
          feedbackFa = 'در حال خم کردن آرنج با کنترل.';
        }
        if (elbowAngle <= 60 && elapsed >= 130) {
          this.phase = 'PEAK_FLEXION';
          this.phaseEnteredAt = frame.timestampMs;
          feedbackEn = 'Full flexion reached. Lower with control.';
          feedbackFa = 'بالاترین نقطه حرکت. به‌آرامی پایین بیاورید.';
        } else if (elbowAngle >= 148 && elapsed >= 130) {
          this.phase = 'EXTENDING';
          this.phaseEnteredAt = frame.timestampMs;
          feedbackEn = 'Incomplete curl — aim for full flexion and extension.';
          feedbackFa = 'تکرار ناقص — دامنه کامل خم و باز شدن آرنج را انجام دهید.';
          warning = true;
        }
      } else if (this.phase === 'PEAK_FLEXION') {
        if (elbowAngle >= 72 && elapsed >= 130) {
          this.repCount += 1;
          this.phase = 'EXTENDING';
          this.phaseEnteredAt = frame.timestampMs;
          this.minAngle = 180;
        }
        feedbackEn = 'Full flexion reached. Lower with control.';
        feedbackFa = 'بالاترین نقطه حرکت. به‌آرامی پایین بیاورید.';
      }

      const phaseMap: Record<string, { en: string; fa: string }> = {
        IDLE: { en: 'Ready', fa: 'آماده' },
        EXTENDING: { en: 'Extending', fa: 'باز کردن' },
        FLEXING: { en: 'Flexing', fa: 'خم کردن' },
        PEAK_FLEXION: { en: 'Peak Flexion', fa: 'اوج انقباض' },
      };

      return {
        phaseEn: phaseMap[this.phase]?.en ?? this.phase,
        phaseFa: phaseMap[this.phase]?.fa ?? this.phase,
        repCount: this.repCount,
        holdDurationMs: 0,
        primaryAngleDeg: elbowAngle,
        secondaryAngleDeg: drift,
        trackingOk: true,
        feedbackEn,
        feedbackFa,
        warning,
      };
    }

    // forearm_plank
    const bodyLine = angleDegrees2D(frame.shoulder, frame.hip, frame.ankle);
    if (this.lastTs !== null) {
      this.holdDurationMs += Math.min(500, Math.max(0, frame.timestampMs - this.lastTs));
    }
    this.lastTs = frame.timestampMs;
    this.phase = 'HOLDING';

    const sag = bodyLine < 158;
    return {
      phaseEn: 'Holding',
      phaseFa: 'در حال نگهداشتن',
      repCount: 0,
      holdDurationMs: this.holdDurationMs,
      primaryAngleDeg: bodyLine,
      secondaryAngleDeg: null,
      trackingOk: true,
      feedbackEn: sag
        ? 'Hips appear low — gently brace core to align shoulders, hips, ankles.'
        : 'Holding plank — keep body line steady.',
      feedbackFa: sag
        ? 'لگن کمی پایین افتاده — با انقباض شکم هم‌راستایی را حفظ کنید.'
        : 'در حال نگهداشتن پلانک — خط بدن را ثابت نگه دارید.',
      warning: sag,
    };
  }
}
