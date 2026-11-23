package com.aifitnesscoach.core.exercise.definitions

import com.aifitnesscoach.core.equipment.ResistanceBandType
import com.aifitnesscoach.core.exercise.CameraSetupGuide
import com.aifitnesscoach.core.exercise.EquipmentCategoryType
import com.aifitnesscoach.core.exercise.ExerciseCategory
import com.aifitnesscoach.core.exercise.ExerciseDefinition
import com.aifitnesscoach.core.exercise.ExerciseMode
import com.aifitnesscoach.core.exercise.ImplementationStatus
import com.aifitnesscoach.core.exercise.MediaAssetRef
import com.aifitnesscoach.core.exercise.RecommendedCameraView
import com.aifitnesscoach.core.pose.PoseLandmarkIndex

/**
 * Complete 39-exercise catalog across all four categories:
 * - 8 Weighted Training
 * - 8 Bodyweight Training
 * - 8 Core Training
 * - 15 Resistance Band Training
 */
object ExerciseCatalog {

    private val upperBodyLandmarks = listOf(
        PoseLandmarkIndex.LEFT_SHOULDER, PoseLandmarkIndex.RIGHT_SHOULDER,
        PoseLandmarkIndex.LEFT_ELBOW, PoseLandmarkIndex.RIGHT_ELBOW,
        PoseLandmarkIndex.LEFT_WRIST, PoseLandmarkIndex.RIGHT_WRIST,
        PoseLandmarkIndex.LEFT_HIP, PoseLandmarkIndex.RIGHT_HIP
    )

    private val fullBodyLandmarks = listOf(
        PoseLandmarkIndex.LEFT_SHOULDER, PoseLandmarkIndex.RIGHT_SHOULDER,
        PoseLandmarkIndex.LEFT_HIP, PoseLandmarkIndex.RIGHT_HIP,
        PoseLandmarkIndex.LEFT_KNEE, PoseLandmarkIndex.RIGHT_KNEE,
        PoseLandmarkIndex.LEFT_ANKLE, PoseLandmarkIndex.RIGHT_ANKLE
    )

    private fun sideGuide(view: RecommendedCameraView = RecommendedCameraView.SIDE) = CameraSetupGuide(
        recommendedView = view,
        distanceEn = "2.0 – 2.8 meters (6.5 – 9 ft)",
        distanceFa = "۲ تا ۲.۸ متر فاصله از دوربین",
        heightEn = "Hip to chest height, stable upright mount",
        heightFa = "ارتفاع کمر تا سینه، ثابت روی پایه"
    )

    private fun floorGuide(view: RecommendedCameraView = RecommendedCameraView.SIDE) = CameraSetupGuide(
        recommendedView = view,
        distanceEn = "2.2 – 3.0 meters (7 – 10 ft)",
        distanceFa = "۲.۲ تا ۳ متر فاصله از دوربین",
        heightEn = "Low height (30–60 cm above floor)",
        heightFa = "ارتفاع پایین (۳۰ تا ۶۰ سانتی‌متر از زمین)"
    )

    private fun weightedEntry(
        id: String,
        nameEn: String,
        nameFa: String,
        descEn: String,
        descFa: String,
        musclesEn: List<String>,
        musclesFa: List<String>,
        view: RecommendedCameraView,
        landmarks: List<PoseLandmarkIndex>,
        analyzerId: String?,
        status: ImplementationStatus,
        instructionsEn: List<String>,
        instructionsFa: List<String>,
        mistakesEn: List<String>,
        mistakesFa: List<String>,
        checksEn: List<String>,
        checksFa: List<String>
    ) = ExerciseDefinition(
        id = id,
        nameEn = nameEn,
        nameFa = nameFa,
        category = ExerciseCategory.WEIGHTED_TRAINING,
        descriptionEn = descEn,
        descriptionFa = descFa,
        targetMusclesEn = musclesEn,
        targetMusclesFa = musclesFa,
        equipmentType = EquipmentCategoryType.DUMBBELL,
        exerciseMode = ExerciseMode.REPETITIONS,
        cameraGuide = sideGuide(view),
        requiredLandmarks = landmarks,
        analyzerId = analyzerId,
        demoMedia = MediaAssetRef(
            thumbnailAsset = "demos/${id}_thumb.webp",
            demoVideoAsset = "demos/${id}_demo.mp4",
            availableViews = listOf(view)
        ),
        instructionsEn = instructionsEn,
        instructionsFa = instructionsFa,
        commonMistakesEn = mistakesEn,
        commonMistakesFa = mistakesFa,
        observableFormChecksEn = checksEn,
        observableFormChecksFa = checksFa,
        limitationsEn = listOf(
            "Camera cannot estimate actual dumbbell weight.",
            "Occlusion from wide sleeves or side angles may reduce landmark confidence."
        ),
        limitationsFa = listOf(
            "دوربین قادر به تخمین وزن واقعی دمبل نیست.",
            "لباس گشاد یا زاویه نامناسب می‌تواند دقت تشخیص مفاصل را کاهش دهد."
        ),
        implementationStatus = status
    )

    private fun bodyweightEntry(
        id: String,
        nameEn: String,
        nameFa: String,
        descEn: String,
        descFa: String,
        musclesEn: List<String>,
        musclesFa: List<String>,
        mode: ExerciseMode,
        equipment: EquipmentCategoryType,
        view: RecommendedCameraView,
        landmarks: List<PoseLandmarkIndex>,
        analyzerId: String?,
        status: ImplementationStatus,
        instructionsEn: List<String>,
        instructionsFa: List<String>,
        mistakesEn: List<String>,
        mistakesFa: List<String>,
        checksEn: List<String>,
        checksFa: List<String>
    ) = ExerciseDefinition(
        id = id,
        nameEn = nameEn,
        nameFa = nameFa,
        category = ExerciseCategory.BODYWEIGHT_TRAINING,
        descriptionEn = descEn,
        descriptionFa = descFa,
        targetMusclesEn = musclesEn,
        targetMusclesFa = musclesFa,
        equipmentType = equipment,
        exerciseMode = mode,
        cameraGuide = if (id in setOf("push_up", "glute_bridge", "superman")) floorGuide(view) else sideGuide(view),
        requiredLandmarks = landmarks,
        analyzerId = analyzerId,
        demoMedia = MediaAssetRef(
            thumbnailAsset = "demos/${id}_thumb.webp",
            demoVideoAsset = "demos/${id}_demo.mp4",
            availableViews = listOf(view)
        ),
        instructionsEn = instructionsEn,
        instructionsFa = instructionsFa,
        commonMistakesEn = mistakesEn,
        commonMistakesFa = mistakesFa,
        observableFormChecksEn = checksEn,
        observableFormChecksFa = checksFa,
        limitationsEn = listOf(
            "2D camera projection cannot verify subtle spinal rotation or foot pressure."
        ),
        limitationsFa = listOf(
            "تصویر دو‌بعدی دوربین چرخش ظریف ستون فقرات یا فشار کف پا را تشخیص نمی‌دهد."
        ),
        implementationStatus = status
    )

    private fun coreEntry(
        id: String,
        nameEn: String,
        nameFa: String,
        descEn: String,
        descFa: String,
        musclesEn: List<String>,
        musclesFa: List<String>,
        mode: ExerciseMode,
        view: RecommendedCameraView,
        landmarks: List<PoseLandmarkIndex>,
        analyzerId: String?,
        status: ImplementationStatus,
        instructionsEn: List<String>,
        instructionsFa: List<String>,
        mistakesEn: List<String>,
        mistakesFa: List<String>,
        checksEn: List<String>,
        checksFa: List<String>
    ) = ExerciseDefinition(
        id = id,
        nameEn = nameEn,
        nameFa = nameFa,
        category = ExerciseCategory.CORE_TRAINING,
        descriptionEn = descEn,
        descriptionFa = descFa,
        targetMusclesEn = musclesEn,
        targetMusclesFa = musclesFa,
        equipmentType = EquipmentCategoryType.MAT_OR_FLOOR,
        exerciseMode = mode,
        cameraGuide = floorGuide(view),
        requiredLandmarks = landmarks,
        analyzerId = analyzerId,
        demoMedia = MediaAssetRef(
            thumbnailAsset = "demos/${id}_thumb.webp",
            demoVideoAsset = "demos/${id}_demo.mp4",
            availableViews = listOf(view)
        ),
        instructionsEn = instructionsEn,
        instructionsFa = instructionsFa,
        commonMistakesEn = mistakesEn,
        commonMistakesFa = mistakesFa,
        observableFormChecksEn = checksEn,
        observableFormChecksFa = checksFa,
        limitationsEn = listOf(
            "Pose tracking observes approximate trunk alignment only; it cannot measure core muscle activation."
        ),
        limitationsFa = listOf(
            "ردیابی حرکت فقط هم‌راستایی تقریبی تنه را مشاهده می‌کند و میزان درگیری عضلات مرکزی را نمی‌سنجد."
        ),
        implementationStatus = status
    )

    private fun bandEntry(
        id: String,
        nameEn: String,
        nameFa: String,
        descEn: String,
        descFa: String,
        musclesEn: List<String>,
        musclesFa: List<String>,
        bandTypes: List<ResistanceBandType>,
        requiresAnchor: Boolean,
        anchorEn: String?,
        anchorFa: String?,
        mode: ExerciseMode,
        view: RecommendedCameraView,
        landmarks: List<PoseLandmarkIndex>,
        analyzerId: String?,
        status: ImplementationStatus,
        instructionsEn: List<String>,
        instructionsFa: List<String>,
        mistakesEn: List<String>,
        mistakesFa: List<String>,
        checksEn: List<String>,
        checksFa: List<String>
    ) = ExerciseDefinition(
        id = id,
        nameEn = nameEn,
        nameFa = nameFa,
        category = ExerciseCategory.RESISTANCE_BAND_TRAINING,
        descriptionEn = descEn,
        descriptionFa = descFa,
        targetMusclesEn = musclesEn,
        targetMusclesFa = musclesFa,
        equipmentType = EquipmentCategoryType.RESISTANCE_BAND,
        supportedBandTypes = bandTypes,
        requiresAnchor = requiresAnchor,
        anchorSetupEn = anchorEn,
        anchorSetupFa = anchorFa,
        exerciseMode = mode,
        cameraGuide = if (id in setOf("band_dead_bug", "mini_band_clamshell")) floorGuide(view) else sideGuide(view),
        requiredLandmarks = landmarks,
        analyzerId = analyzerId,
        demoMedia = MediaAssetRef(
            thumbnailAsset = "demos/${id}_thumb.webp",
            demoVideoAsset = "demos/${id}_demo.mp4",
            availableViews = listOf(view, RecommendedCameraView.THREE_QUARTER)
        ),
        instructionsEn = instructionsEn,
        instructionsFa = instructionsFa,
        commonMistakesEn = mistakesEn,
        commonMistakesFa = mistakesFa,
        observableFormChecksEn = checksEn,
        observableFormChecksFa = checksFa,
        limitationsEn = listOf(
            "Analyzes body movement only — does not measure band tension, resistance force, or joint torque.",
            "Camera cannot verify band condition or anchor security."
        ),
        limitationsFa = listOf(
            "فقط حرکت بدن تحلیل می‌شود؛ کشش کش، نیروی مقاومتی یا گشتاور مفصل قابل اندازه‌گیری نیست.",
            "دوربین نمی‌تواند سلامت کش یا استحکام نقطه اتصال را بررسی کند."
        ),
        implementationStatus = status
    )

    val builtInExercises: List<ExerciseDefinition> = listOf(
        // ============================================================
        // CATEGORY A — WEIGHTED TRAINING (8 Exercises)
        // ============================================================
        weightedEntry(
            id = "dumbbell_biceps_curl",
            nameEn = "Dumbbell Biceps Curl",
            nameFa = "جلو بازو دمبل",
            descEn = "Standing elbow flexion with dumbbells focusing on controlled upper-arm stability.",
            descFa = "خم کردن کنترل‌شده آرنج با دمبل در حالت ایستاده با تمرکز بر ثبات بازو.",
            musclesEn = listOf("Biceps Brachii", "Brachialis", "Forearms"),
            musclesFa = listOf("جلو بازو", "بازویی قدامی", "ساعد"),
            view = RecommendedCameraView.SIDE,
            landmarks = upperBodyLandmarks,
            analyzerId = "biceps_curl",
            status = ImplementationStatus.IMPLEMENTED_NEEDS_VALIDATION,
            instructionsEn = listOf(
                "Stand tall with dumbbells at your sides and elbows close to your ribs.",
                "Curl the weights upward by flexing at the elbow without swinging your torso.",
                "Lower smoothly back to full controlled elbow extension."
            ),
            instructionsFa = listOf(
                "صاف بایستید، دمبل‌ها را در کنار بدن نگه دارید و آرنج‌ها را نزدیک پهلو قرار دهید.",
                "بدون تاب دادن تنه، آرنج را خم کرده و دمبل‌ها را بالا بیاورید.",
                "دمبل‌ها را با کنترل کامل تا باز شدن کامل آرنج پایین بیاورید."
            ),
            mistakesEn = listOf("Swinging the torso", "Drifting elbows forward", "Cutting bottom extension short"),
            mistakesFa = listOf("تاب دادن کمر و تنه", "جلو بردن بیش از حد آرنج", "باز نکردن کامل آرنج در پایین حرکت"),
            checksEn = listOf("Elbow flexion/extension angle", "Range of motion", "Upper-arm drift observation"),
            checksFa = listOf("زاویه خم و باز شدن آرنج", "دامنه حرکتی", "مشاهده جابه‌جایی بازو")
        ),
        weightedEntry(
            id = "dumbbell_shoulder_press",
            nameEn = "Dumbbell Shoulder Press",
            nameFa = "پرس سرشانه دمبل",
            descEn = "Overhead dumbbell press targeting deltoids and triceps.",
            descFa = "پرس دمبل بالای سر برای تقویت عضلات سرشانه و پشت بازو.",
            musclesEn = listOf("Deltoids", "Triceps", "Upper Chest"),
            musclesFa = listOf("سرشانه", "پشت بازو", "بالاسینه"),
            view = RecommendedCameraView.FRONT,
            landmarks = upperBodyLandmarks,
            analyzerId = null,
            status = ImplementationStatus.COMING_SOON,
            instructionsEn = listOf(
                "Hold dumbbells at shoulder height with forearms vertical.",
                "Press overhead until arms are extended without shrugging.",
                "Lower with control back to shoulder level."
            ),
            instructionsFa = listOf(
                "دمبل‌ها را در ارتفاع شانه با ساعدهای عمودی نگه دارید.",
                "دمبل‌ها را بدون بالا کشیدن شانه‌ها به سمت بالا پرس کنید.",
                "با کنترل به ارتفاع شانه بازگردید."
            ),
            mistakesEn = listOf("Arching lower back", "Incomplete lockout"),
            mistakesFa = listOf("قوس بیش از حد کمر", "نیمه‌کاره رها کردن حرکت"),
            checksEn = listOf("Planned: Overhead elbow extension & symmetry"),
            checksFa = listOf("در دست ساخت: بررسی باز شدن آرنج و تقارن")
        ),
        weightedEntry(
            id = "dumbbell_lateral_raise",
            nameEn = "Dumbbell Lateral Raise",
            nameFa = "نشر جانب دمبل",
            descEn = "Abducting arms laterally to shoulder height.",
            descFa = "بالا آوردن دمبل‌ها از طرفین تا سطح شانه.",
            musclesEn = listOf("Lateral Deltoid", "Supraspinatus"),
            musclesFa = listOf("دلتوئید میانی", "سرشانه جانبی"),
            view = RecommendedCameraView.FRONT,
            landmarks = upperBodyLandmarks,
            analyzerId = null,
            status = ImplementationStatus.COMING_SOON,
            instructionsEn = listOf(
                "Hold dumbbells lightly in front of thighs with soft elbows.",
                "Raise arms out to the sides until parallel to the floor.",
                "Lower slowly without dropping the weights."
            ),
            instructionsFa = listOf(
                "دمبل‌ها را با آرنج کمی خم در کنار ران نگه دارید.",
                "دست‌ها را از طرفین تا هم‌سطح شانه بالا بیاورید.",
                "به‌آرامی و با کنترل پایین بیاورید."
            ),
            mistakesEn = listOf("Shrugging traps", "Using hip momentum"),
            mistakesFa = listOf("بالا انداختن شانه‌ها", "کمک گرفتن از حرکت کمر"),
            checksEn = listOf("Planned: Shoulder abduction angle & symmetry"),
            checksFa = listOf("در دست ساخت: زاویه بالا آوردن شانه و تقارن")
        ),
        weightedEntry(
            id = "dumbbell_bent_over_row",
            nameEn = "Dumbbell Bent-Over Row",
            nameFa = "زیربغل دمبل خم",
            descEn = "Hip-hinged row pulling dumbbells toward the lower ribs.",
            descFa = "حرکت پارویی خم با دمبل برای تقویت عضلات پشت و زیربغل.",
            musclesEn = listOf("Latissimus Dorsi", "Rhomboids", "Rear Deltoids"),
            musclesFa = listOf("زیربغل", "ذوزنقه میانی", "پشت شانه"),
            view = RecommendedCameraView.SIDE,
            landmarks = fullBodyLandmarks,
            analyzerId = null,
            status = ImplementationStatus.COMING_SOON,
            instructionsEn = listOf(
                "Hinge at the hips with a neutral spine.",
                "Pull dumbbells toward your hips by driving elbows back.",
                "Lower arms fully with control."
            ),
            instructionsFa = listOf(
                "با حفظ صافی ستون فقرات از لگن به جلو خم شوید.",
                "آرنج‌ها را به سمت عقب کشیده و دمبل را کنار لگن بیاورید.",
                "با کنترل دست‌ها را پایین ببرید."
            ),
            mistakesEn = listOf("Rounding the spine", "Jerking the torso upright"),
            mistakesFa = listOf("گرد کردن کمر", "بلند کردن تنه هنگام کشش"),
            checksEn = listOf("Planned: Torso hinge angle & elbow pull depth"),
            checksFa = listOf("در دست ساخت: زاویه خم تنه و دامنه کشش")
        ),
        weightedEntry(
            id = "goblet_squat",
            nameEn = "Goblet Squat",
            nameFa = "اسکوات گابلت",
            descEn = "Front-loaded squat holding a single dumbbell at chest level.",
            descFa = "اسکوات با نگهداشتن یک دمبل در مقابل سینه.",
            musclesEn = listOf("Quadriceps", "Glutes", "Core"),
            musclesFa = listOf("چهارسر ران", "باسن", "مرکز بدن"),
            view = RecommendedCameraView.SIDE,
            landmarks = fullBodyLandmarks,
            analyzerId = null,
            status = ImplementationStatus.EXPERIMENTAL,
            instructionsEn = listOf(
                "Cup one dumbbell vertically against your upper chest.",
                "Sit hips back and down between your knees.",
                "Drive through mid-foot back to standing."
            ),
            instructionsFa = listOf(
                "یک دمبل را عمودی جلوی سینه نگه دارید.",
                "لگن را به عقب و پایین ببرید تا ران‌ها موازی زمین شوند.",
                "با فشار کف پا به حالت ایستاده برگردید."
            ),
            mistakesEn = listOf("Heels lifting", "Collapsing chest forward"),
            mistakesFa = listOf("بلند شدن پاشنه", "خم شدن سینه به جلو"),
            checksEn = listOf("Experimental: Knee/hip flexion depth"),
            checksFa = listOf("آزمایشی: عمق خم شدن زانو و لگن")
        ),
        weightedEntry(
            id = "dumbbell_triceps_extension",
            nameEn = "Dumbbell Triceps Extension",
            nameFa = "پشت بازو دمبل",
            descEn = "Overhead or bent elbow extension targeting the triceps.",
            descFa = "باز کردن آرنج بالای سر یا خم برای تقویت عضله پشت بازو.",
            musclesEn = listOf("Triceps Brachii"),
            musclesFa = listOf("پشت بازو"),
            view = RecommendedCameraView.SIDE,
            landmarks = upperBodyLandmarks,
            analyzerId = null,
            status = ImplementationStatus.COMING_SOON,
            instructionsEn = listOf(
                "Hold dumbbell overhead with upper arms steady near ears.",
                "Lower weight behind head by bending elbows.",
                "Extend elbows upward cleanly."
            ),
            instructionsFa = listOf(
                "دمبل را بالای سر نگه دارید و بازوها را ثابت کنید.",
                "با خم کردن آرنج دمبل را پشت سر پایین ببرید.",
                "آرنج‌ها را با کنترل به بالا باز کنید."
            ),
            mistakesEn = listOf("Flaring elbows wide", "Arching lower back"),
            mistakesFa = listOf("باز شدن آرنج‌ها به طرفین", "قوس کمر"),
            checksEn = listOf("Planned: Overhead elbow extension angle"),
            checksFa = listOf("در دست ساخت: زاویه باز شدن آرنج")
        ),
        weightedEntry(
            id = "dumbbell_romanian_deadlift",
            nameEn = "Dumbbell Romanian Deadlift",
            nameFa = "ددلیفت رومانیایی دمبل",
            descEn = "Hip-hinge movement focusing on hamstrings and glutes.",
            descFa = "حرکت لولای لگن با دمبل برای تقویت همسترینگ و باسن.",
            musclesEn = listOf("Hamstrings", "Glutes", "Erector Spinae"),
            musclesFa = listOf("پشت ران", "باسن", "راست‌کننده ستون فقرات"),
            view = RecommendedCameraView.SIDE,
            landmarks = fullBodyLandmarks,
            analyzerId = null,
            status = ImplementationStatus.COMING_SOON,
            instructionsEn = listOf(
                "Hold dumbbells in front of thighs with slight knee bend.",
                "Push hips backward while lowering dumbbells along legs.",
                "Drive hips forward to stand upright."
            ),
            instructionsFa = listOf(
                "دمبل‌ها را جلوی ران با زانوی کمی خم نگه دارید.",
                "لگن را به عقب هل دهید و دمبل‌ها را نزدیک پا پایین ببرید.",
                "با انقباض باسن به حالت ایستاده برگردید."
            ),
            mistakesEn = listOf("Rounding back", "Bending knees into a squat"),
            mistakesFa = listOf("قوز کردن کمر", "تبدیل حرکت به اسکوات"),
            checksEn = listOf("Planned: Hip hinge angle & torso neutrality"),
            checksFa = listOf("در دست ساخت: زاویه لولای لگن")
        ),
        weightedEntry(
            id = "dumbbell_chest_press",
            nameEn = "Dumbbell Chest Press",
            nameFa = "پرس سینه دمبل",
            descEn = "Supine pressing movement targeting chest, front delts, and triceps.",
            descFa = "پرس دمبل روی زمین یا نیمکت برای تقویت عضلات سینه.",
            musclesEn = listOf("Pectoralis Major", "Anterior Deltoid", "Triceps"),
            musclesFa = listOf("سینه", "سرشانه قدامی", "پشت بازو"),
            view = RecommendedCameraView.THREE_QUARTER,
            landmarks = upperBodyLandmarks,
            analyzerId = null,
            status = ImplementationStatus.COMING_SOON,
            instructionsEn = listOf(
                "Lie back with dumbbells above chest, wrists stacked over elbows.",
                "Lower dumbbells until upper arms lightly touch floor or bench level.",
                "Press back up smoothly."
            ),
            instructionsFa = listOf(
                "به پشت دراز بکشید و دمبل‌ها را در راستای سینه نگه دارید.",
                "آرنج‌ها را با زاویه ۴۵ درجه پایین بیاورید.",
                "دمبل‌ها را به بالا پرس کنید."
            ),
            mistakesEn = listOf("Flaring elbows 90°", "Bouncing at bottom"),
            mistakesFa = listOf("باز کردن بیش از حد آرنج‌ها", "ضربه زدن در پایین حرکت"),
            checksEn = listOf("Planned: Pressing symmetry"),
            checksFa = listOf("در دست ساخت: تقارن پرس")
        ),

        // ============================================================
        // CATEGORY B — BODYWEIGHT TRAINING (8 Exercises)
        // ============================================================
        bodyweightEntry(
            id = "bodyweight_squat",
            nameEn = "Bodyweight Squat",
            nameFa = "اسکوات بدون وزنه",
            descEn = "Fundamental lower-body squat using knee and hip flexion.",
            descFa = "اسکوات پایه با وزن بدن با تمرکز بر عمق و کنترل زانو و لگن.",
            musclesEn = listOf("Quadriceps", "Glutes", "Hamstrings"),
            musclesFa = listOf("چهارسر ران", "باسن", "پشت ران"),
            mode = ExerciseMode.REPETITIONS,
            equipment = EquipmentCategoryType.BODYWEIGHT,
            view = RecommendedCameraView.SIDE,
            landmarks = fullBodyLandmarks,
            analyzerId = "bodyweight_squat",
            status = ImplementationStatus.IMPLEMENTED_NEEDS_VALIDATION,
            instructionsEn = listOf(
                "Stand with feet shoulder-width apart and toes slightly turned out.",
                "Lower hips back and down until thighs approach parallel.",
                "Stand back up smoothly to full hip and knee extension."
            ),
            instructionsFa = listOf(
                "پاها را به اندازه عرض شانه باز کنید و پنجه‌ها کمی به بیرون باشد.",
                "لگن را به عقب و پایین ببرید تا ران‌ها به موازی زمین نزدیک شوند.",
                "با کنترل به حالت ایستاده کامل بازگردید."
            ),
            mistakesEn = listOf("Shallow depth", "Rounding lower back", "Heels lifting off floor"),
            mistakesFa = listOf("عمق ناکافی", "خم شدن بیش از حد کمر", "بلند شدن پاشنه‌ها"),
            checksEn = listOf("Standing/Descending/Bottom/Ascending phases", "Knee & hip angle depth", "Incomplete rep warnings"),
            checksFa = listOf("تشخیص فازهای ایستاده/پایین/صعود", "بررسی عمق زانو و لگن", "هشدار تکرار ناقص")
        ),
        bodyweightEntry(
            id = "push_up",
            nameEn = "Push-up",
            nameFa = "شنا سوئدی",
            descEn = "Horizontal bodyweight press strengthening chest, shoulders, triceps, and trunk.",
            descFa = "حرکت شنا با وزن بدن برای تقویت سینه، شانه، پشت بازو و تنه.",
            musclesEn = listOf("Pectorals", "Anterior Deltoids", "Triceps", "Core"),
            musclesFa = listOf("سینه", "شانه", "پشت بازو", "مرکز بدن"),
            mode = ExerciseMode.REPETITIONS,
            equipment = EquipmentCategoryType.BODYWEIGHT,
            view = RecommendedCameraView.SIDE,
            landmarks = fullBodyLandmarks,
            analyzerId = null,
            status = ImplementationStatus.EXPERIMENTAL,
            instructionsEn = listOf(
                "Start in a straight high-plank position with hands beneath shoulders.",
                "Lower chest toward the floor while keeping body line straight.",
                "Push back up to full arm extension."
            ),
            instructionsFa = listOf(
                "در وضعیت پلانک بالا با دست‌ها زیر شانه قرار بگیرید.",
                "با حفظ خط صاف بدن سینه را به زمین نزدیک کنید.",
                "با فشار کف دست‌ها بالا بیایید."
            ),
            mistakesEn = listOf("Sagging hips", "Half-range repetitions"),
            mistakesFa = listOf("افتادن لگن", "دامنه حرکتی ناقص"),
            checksEn = listOf("Experimental: Elbow angle & body alignment"),
            checksFa = listOf("آزمایشی: زاویه آرنج و خط بدن")
        ),
        bodyweightEntry(
            id = "forward_lunge",
            nameEn = "Forward Lunge",
            nameFa = "لانج جلو",
            descEn = "Alternating forward step into a split squat.",
            descFa = "گام به جلو متناوب برای تقویت ران و باسن.",
            musclesEn = listOf("Quadriceps", "Glutes", "Balance Stabilizers"),
            musclesFa = listOf("چهارسر ران", "باسن", "عضلات تعادلی"),
            mode = ExerciseMode.ALTERNATING_REPETITIONS,
            equipment = EquipmentCategoryType.BODYWEIGHT,
            view = RecommendedCameraView.SIDE,
            landmarks = fullBodyLandmarks,
            analyzerId = null,
            status = ImplementationStatus.COMING_SOON,
            instructionsEn = listOf(
                "Step one foot forward and lower both knees toward 90°.",
                "Push through the front heel to return to standing.",
                "Alternate legs."
            ),
            instructionsFa = listOf(
                "یک گام به جلو بردارید و هر دو زانو را تا حدود ۹۰ درجه خم کنید.",
                "با فشار پای جلو به حالت ایستاده برگردید.",
                "پاها را به نوبت تغییر دهید."
            ),
            mistakesEn = listOf("Front knee collapsing inward", "Overstriding"),
            mistakesFa = listOf("انحراف زانوی جلو به داخل", "گام بیش از حد بلند"),
            checksEn = listOf("Planned: Split-stance knee angles"),
            checksFa = listOf("در دست ساخت: زاویه زانوها در لانج")
        ),
        bodyweightEntry(
            id = "reverse_lunge",
            nameEn = "Reverse Lunge",
            nameFa = "لانج معکوس",
            descEn = "Stepping backward into a knee-friendly lunge.",
            descFa = "گام به عقب در حرکت لانج با فشار کمتر روی زانو.",
            musclesEn = listOf("Glutes", "Quadriceps", "Hamstrings"),
            musclesFa = listOf("باسن", "چهارسر ران", "پشت ران"),
            mode = ExerciseMode.ALTERNATING_REPETITIONS,
            equipment = EquipmentCategoryType.BODYWEIGHT,
            view = RecommendedCameraView.SIDE,
            landmarks = fullBodyLandmarks,
            analyzerId = null,
            status = ImplementationStatus.COMING_SOON,
            instructionsEn = listOf(
                "Step one foot backward onto the ball of your foot.",
                "Lower back knee smoothly toward the floor.",
                "Drive through the front foot to return."
            ),
            instructionsFa = listOf(
                "یک پا را به عقب ببرید و روی پنجه قرار دهید.",
                "زانوی عقب را به سمت زمین پایین بیاورید.",
                "با فشار پای جلو به وضعیت اولیه برگردید."
            ),
            mistakesEn = listOf("Leaning torso far forward", "Short stance"),
            mistakesFa = listOf("خم شدن زیاد تنه به جلو", "گام کوتاه"),
            checksEn = listOf("Planned: Step-back knee flexion"),
            checksFa = listOf("در دست ساخت: خم شدن زانو در گام عقب")
        ),
        bodyweightEntry(
            id = "glute_bridge",
            nameEn = "Glute Bridge",
            nameFa = "پل باسن",
            descEn = "Supine hip extension raising hips to shoulder-knee alignment.",
            descFa = "بالا آوردن لگن در حالت خوابیده به پشت برای تقویت باسن.",
            musclesEn = listOf("Gluteus Maximus", "Hamstrings"),
            musclesFa = listOf("باسن", "پشت ران"),
            mode = ExerciseMode.REPETITIONS,
            equipment = EquipmentCategoryType.MAT_OR_FLOOR,
            view = RecommendedCameraView.SIDE,
            landmarks = fullBodyLandmarks,
            analyzerId = null,
            status = ImplementationStatus.COMING_SOON,
            instructionsEn = listOf(
                "Lie on your back with knees bent and feet flat.",
                "Press through heels to lift hips until knees, hips, and shoulders align.",
                "Lower slowly to the mat."
            ),
            instructionsFa = listOf(
                "به پشت بخوابید و زانوها را خم و کف پاها را روی زمین بگذارید.",
                "لگن را بالا بیاورید تا با زانو و شانه در یک خط قرار گیرد.",
                "به‌آرامی پایین بیایید."
            ),
            mistakesEn = listOf("Hyperextending lower back at top"),
            mistakesFa = listOf("قوس بیش از حد کمر در بالا"),
            checksEn = listOf("Planned: Hip extension angle"),
            checksFa = listOf("در دست ساخت: زاویه باز شدن لگن")
        ),
        bodyweightEntry(
            id = "standing_calf_raise",
            nameEn = "Standing Calf Raise",
            nameFa = "ساق پا ایستاده",
            descEn = "Raising heels onto the balls of the feet with straight knees.",
            descFa = "بلند شدن روی پنجه پا در حالت ایستاده برای عضلات ساق.",
            musclesEn = listOf("Gastrocnemius", "Soleus"),
            musclesFa = listOf("دوقلو ساق", "نعل اسبی ساق"),
            mode = ExerciseMode.REPETITIONS,
            equipment = EquipmentCategoryType.BODYWEIGHT,
            view = RecommendedCameraView.SIDE,
            landmarks = fullBodyLandmarks,
            analyzerId = null,
            status = ImplementationStatus.COMING_SOON,
            instructionsEn = listOf(
                "Stand tall near a wall or chair for light balance if needed.",
                "Press onto the balls of your feet to lift heels high.",
                "Lower heels smoothly to the floor."
            ),
            instructionsFa = listOf(
                "صاف بایستید و در صورت نیاز برای تعادل دست را به دیوار تکیه دهید.",
                "پاشنه‌ها را تا جای ممکن بالا بیاورید.",
                "به‌آرامی پاشنه‌ها را پایین بگذارید."
            ),
            mistakesEn = listOf("Bouncing quickly", "Bending knees"),
            mistakesFa = listOf("حرکت سریع و ضربه‌ای", "خم کردن زانوها"),
            checksEn = listOf("Planned: Ankle vertical displacement"),
            checksFa = listOf("در دست ساخت: جابه‌جایی عمودی مچ پا")
        ),
        bodyweightEntry(
            id = "wall_sit",
            nameEn = "Wall Sit",
            nameFa = "وال سیت",
            descEn = "Isometric seated hold against a wall.",
            descFa = "نگهداشتن ایزومتریک وضعیت نشسته با تکیه کمر به دیوار.",
            musclesEn = listOf("Quadriceps", "Glutes"),
            musclesFa = listOf("چهارسر ران", "باسن"),
            mode = ExerciseMode.TIMED_HOLD,
            equipment = EquipmentCategoryType.WALL,
            view = RecommendedCameraView.SIDE,
            landmarks = fullBodyLandmarks,
            analyzerId = null,
            status = ImplementationStatus.COMING_SOON,
            instructionsEn = listOf(
                "Rest your back flat against a sturdy wall.",
                "Slide down until thighs are roughly parallel and knees are near 90°.",
                "Hold steadily for the target duration."
            ),
            instructionsFa = listOf(
                "کمر را کامل به دیوار صاف تکیه دهید.",
                "پایین بیایید تا زاویه زانو نزدیک ۹۰ درجه شود.",
                "وضعیت را برای مدت هدف حفظ کنید."
            ),
            mistakesEn = listOf("Resting hands on thighs", "Knees sliding well past toes"),
            mistakesFa = listOf("تکیه دادن دست‌ها روی ران", "جلو رفتن بیش از حد زانو"),
            checksEn = listOf("Planned: Isometric knee-angle hold timer"),
            checksFa = listOf("در دست ساخت: زمان‌سنج زاویه ایزومتریک زانو")
        ),
        bodyweightEntry(
            id = "superman",
            nameEn = "Superman",
            nameFa = "سوپرمن",
            descEn = "Prone back extension lifting chest and legs off the floor.",
            descFa = "بالا آوردن هم‌زمان سینه و پاها در حالت دمر برای تقویت پشت.",
            musclesEn = listOf("Erector Spinae", "Glutes", "Upper Back"),
            musclesFa = listOf("فیله کمر", "باسن", "پشت بدن"),
            mode = ExerciseMode.REPETITIONS,
            equipment = EquipmentCategoryType.MAT_OR_FLOOR,
            view = RecommendedCameraView.SIDE,
            landmarks = fullBodyLandmarks,
            analyzerId = null,
            status = ImplementationStatus.COMING_SOON,
            instructionsEn = listOf(
                "Lie face down with arms extended overhead.",
                "Gently lift arms, chest, and legs a few inches off the floor.",
                "Pause briefly and lower with control."
            ),
            instructionsFa = listOf(
                "به شکم دراز بکشید و دست‌ها را به جلو بکشید.",
                "دست‌ها، سینه و پاها را چند سانتی‌متر از زمین بلند کنید.",
                "مکث کوتاه کرده و با کنترل پایین بیایید."
            ),
            mistakesEn = listOf("Jerking neck upward", "Over-arching lumbar spine"),
            mistakesFa = listOf("بالا بردن ناگهانی گردن", "قوس شدید کمر"),
            checksEn = listOf("Planned: Trunk & hip lift timing"),
            checksFa = listOf("در دست ساخت: زمان‌بندی بالا آوردن تنه")
        ),

        // ============================================================
        // CATEGORY C — CORE TRAINING (8 Exercises)
        // ============================================================
        coreEntry(
            id = "forearm_plank",
            nameEn = "Forearm Plank",
            nameFa = "پلانک ساعد",
            descEn = "Isometric prone trunk hold supported on forearms and toes.",
            descFa = "نگهداشتن ایزومتریک بدن روی ساعدها و پنجه پا در یک خط صاف.",
            musclesEn = listOf("Transverse Abdominis", "Rectus Abdominis", "Shoulders"),
            musclesFa = listOf("عضلات عمقی شکم", "راست شکمی", "شانه"),
            mode = ExerciseMode.TIMED_HOLD,
            view = RecommendedCameraView.SIDE,
            landmarks = fullBodyLandmarks,
            analyzerId = "forearm_plank",
            status = ImplementationStatus.IMPLEMENTED_NEEDS_VALIDATION,
            instructionsEn = listOf(
                "Place forearms on the mat with elbows directly beneath shoulders.",
                "Extend legs behind you and lift hips into a straight shoulder-hip-ankle line.",
                "Hold steadily while breathing smoothly."
            ),
            instructionsFa = listOf(
                "ساعدها را روی زیرانداز بگذارید به‌طوری که آرنج زیر شانه باشد.",
                "پاها را صاف کرده و بدن را در یک خط مستقیم از شانه تا مچ پا بالا نگه دارید.",
                "با تنفس منظم وضعیت را حفظ کنید."
            ),
            mistakesEn = listOf("Hips sagging toward floor", "Hips piked too high", "Holding breath"),
            mistakesFa = listOf("افتادن لگن به سمت زمین", "بالا بردن بیش از حد لگن", "حبس کردن نفس"),
            checksEn = listOf("Hold start & duration tracking", "Shoulder-hip-ankle alignment deviation", "Tracking interruption handling"),
            checksFa = listOf("شروع و شمارش زمان پلانک", "بررسی انحراف خط شانه-لگن-مچ", "مدیریت قطع ردیابی")
        ),
        coreEntry(
            id = "side_plank",
            nameEn = "Side Plank",
            nameFa = "پلانک بغل",
            descEn = "Lateral isometric trunk hold on one forearm.",
            descFa = "نگهداشتن ایزومتریک بدن به پهلو روی یک ساعد.",
            musclesEn = listOf("Obliques", "Quadratus Lumborum", "Glute Medius"),
            musclesFa = listOf("مورب شکمی", "پهلو", "باسن میانی"),
            mode = ExerciseMode.TIMED_HOLD,
            view = RecommendedCameraView.FRONT,
            landmarks = fullBodyLandmarks,
            analyzerId = null,
            status = ImplementationStatus.COMING_SOON,
            instructionsEn = listOf(
                "Lie on one side with elbow stacked directly under shoulder.",
                "Lift hips until body forms a straight diagonal line.",
                "Hold, then switch sides."
            ),
            instructionsFa = listOf(
                "به پهلو دراز بکشید و آرنج را دقیقاً زیر شانه بگذارید.",
                "لگن را بلند کنید تا بدن خط مورب صافی بسازد.",
                "نگه دارید و سپس سمت دیگر را انجام دهید."
            ),
            mistakesEn = listOf("Dropping bottom hip", "Rotating torso forward"),
            mistakesFa = listOf("افتادن لگن", "چرخاندن تنه به جلو"),
            checksEn = listOf("Planned: Lateral alignment hold timer"),
            checksFa = listOf("در دست ساخت: زمان‌سنج هم‌راستایی جانبی")
        ),
        coreEntry(
            id = "dead_bug",
            nameEn = "Dead Bug",
            nameFa = "ددباگ",
            descEn = "Supine alternating opposite arm and leg extension while stabilizing trunk.",
            descFa = "حرکت متناوب دست و پای مخالف در حالت خوابیده به پشت با ثبات کمر.",
            musclesEn = listOf("Deep Core", "Hip Flexors", "Anterior Chain"),
            musclesFa = listOf("مرکز بدن", "خم‌کننده‌های لگن"),
            mode = ExerciseMode.ALTERNATING_REPETITIONS,
            view = RecommendedCameraView.SIDE,
            landmarks = fullBodyLandmarks,
            analyzerId = null,
            status = ImplementationStatus.COMING_SOON,
            instructionsEn = listOf(
                "Lie on your back with arms pointed up and knees bent at 90°.",
                "Slowly extend opposite arm and leg toward the floor.",
                "Return to center and alternate sides."
            ),
            instructionsFa = listOf(
                "به پشت بخوابید، دست‌ها به سمت سقف و زانوها ۹۰ درجه خم باشند.",
                "دست و پای مخالف را هم‌زمان به سمت زمین باز کنید.",
                "به مرکز برگردید و با سمت دیگر تکرار کنید."
            ),
            mistakesEn = listOf("Arching lower back off mat", "Rushing alternation"),
            mistakesFa = listOf("بلند شدن کمر از زمین", "انجام سریع بدون کنترل"),
            checksEn = listOf("Planned: Alternating limb extension symmetry"),
            checksFa = listOf("در دست ساخت: تقارن حرکت متناوب")
        ),
        coreEntry(
            id = "bird_dog",
            nameEn = "Bird Dog",
            nameFa = "برد داگ",
            descEn = "Quadruped alternating opposite arm and leg reach.",
            descFa = "باز کردن متناوب دست و پای مخالف در وضعیت چهار دست و پا.",
            musclesEn = listOf("Erector Spinae", "Glutes", "Core Stabilizers"),
            musclesFa = listOf("فیله کمر", "باسن", "ثابت‌کننده‌های تنه"),
            mode = ExerciseMode.ALTERNATING_REPETITIONS,
            view = RecommendedCameraView.SIDE,
            landmarks = fullBodyLandmarks,
            analyzerId = null,
            status = ImplementationStatus.COMING_SOON,
            instructionsEn = listOf(
                "Start on hands and knees with neutral spine.",
                "Extend right arm forward and left leg straight back.",
                "Return smoothly and alternate."
            ),
            instructionsFa = listOf(
                "در حالت چهار دست و پا با ستون فقرات صاف قرار بگیرید.",
                "دست راست را به جلو و پای چپ را به عقب بکشید.",
                "با کنترل برگردید و سمت مقابل را انجام دهید."
            ),
            mistakesEn = listOf("Twisting hips", "Hyperextending lower back"),
            mistakesFa = listOf("چرخاندن لگن", "قوس زیاد کمر"),
            checksEn = listOf("Planned: Alternating extension & trunk stability"),
            checksFa = listOf("در دست ساخت: ثبات تنه و حرکت متناوب")
        ),
        coreEntry(
            id = "abdominal_crunch",
            nameEn = "Abdominal Crunch",
            nameFa = "کرانچ شکم",
            descEn = "Controlled supine upper-trunk flexion.",
            descFa = "خم کردن کنترل‌شده بالاتنه به جلو در حالت خوابیده به پشت.",
            musclesEn = listOf("Rectus Abdominis"),
            musclesFa = listOf("راست شکمی"),
            mode = ExerciseMode.REPETITIONS,
            view = RecommendedCameraView.SIDE,
            landmarks = fullBodyLandmarks,
            analyzerId = null,
            status = ImplementationStatus.COMING_SOON,
            instructionsEn = listOf(
                "Lie on back with knees bent and hands lightly beside temples.",
                "Curl shoulder blades smoothly off the mat without pulling neck.",
                "Lower slowly with control."
            ),
            instructionsFa = listOf(
                "به پشت بخوابید، زانوها خم و دست‌ها کنار سر باشد.",
                "بدون کشیدن گردن، شانه‌ها را به‌آرامی از زمین جدا کنید.",
                "با کنترل به زمین برگردید."
            ),
            mistakesEn = listOf("Pulling on the neck", "Using momentum"),
            mistakesFa = listOf("کشیدن گردن با دست‌ها", "ضربه زدن با شتاب"),
            checksEn = listOf("Planned: Upper-torso flexion angle"),
            checksFa = listOf("در دست ساخت: زاویه خم شدن بالاتنه")
        ),
        coreEntry(
            id = "reverse_crunch",
            nameEn = "Reverse Crunch",
            nameFa = "کرانچ معکوس",
            descEn = "Controlled pelvic tilt bringing knees toward chest.",
            descFa = "بالا آوردن کنترل‌شده لگن و زانوها به سمت سینه.",
            musclesEn = listOf("Lower Rectus Abdominis", "Obliques"),
            musclesFa = listOf("زیر شکم", "مورب شکمی"),
            mode = ExerciseMode.REPETITIONS,
            view = RecommendedCameraView.SIDE,
            landmarks = fullBodyLandmarks,
            analyzerId = null,
            status = ImplementationStatus.COMING_SOON,
            instructionsEn = listOf(
                "Lie on back with knees bent and arms beside body.",
                "Curl pelvis slightly off the mat bringing knees toward chest.",
                "Lower slowly without swinging legs."
            ),
            instructionsFa = listOf(
                "به پشت بخوابید و دست‌ها را کنار بدن قرار دهید.",
                "با کنترل لگن را کمی بلند کرده و زانوها را به سینه نزدیک کنید.",
                "بدون رها کردن پاها به حالت اول برگردید."
            ),
            mistakesEn = listOf("Swinging legs for momentum"),
            mistakesFa = listOf("تاب دادن پاها"),
            checksEn = listOf("Planned: Hip-knee flexion cycles"),
            checksFa = listOf("در دست ساخت: چرخه خم شدن لگن و زانو")
        ),
        coreEntry(
            id = "mountain_climber",
            nameEn = "Mountain Climber",
            nameFa = "کوهنوردی",
            descEn = "Alternating knee drives from a high-plank stance.",
            descFa = "کشیدن متناوب زانوها به سمت سینه در وضعیت پلانک بالا.",
            musclesEn = listOf("Core", "Hip Flexors", "Shoulders"),
            musclesFa = listOf("مرکز بدن", "خم‌کننده‌های لگن", "شانه"),
            mode = ExerciseMode.ALTERNATING_REPETITIONS,
            view = RecommendedCameraView.SIDE,
            landmarks = fullBodyLandmarks,
            analyzerId = null,
            status = ImplementationStatus.COMING_SOON,
            instructionsEn = listOf(
                "Assume a steady high plank with hands under shoulders.",
                "Drive one knee toward chest, switch smoothly with the other leg.",
                "Keep hips level without bouncing."
            ),
            instructionsFa = listOf(
                "در وضعیت پلانک بالا با دست‌ها زیر شانه قرار بگیرید.",
                "زانوها را به‌صورت متناوب به سمت سینه بکشید.",
                "ارتفاع لگن را تا حد امکان ثابت نگه دارید."
            ),
            mistakesEn = listOf("Piking hips up", "Bouncing shoulders"),
            mistakesFa = listOf("بالا رفتن لگن", "تکان خوردن شانه‌ها"),
            checksEn = listOf("Planned: Alternating knee drive cadence & hip stability"),
            checksFa = listOf("در دست ساخت: ریتم زانو و ثبات لگن")
        ),
        coreEntry(
            id = "hollow_body_hold",
            nameEn = "Hollow Body Hold",
            nameFa = "هالو بادی هولد",
            descEn = "Supine isometric hold with low back pressed to the mat.",
            descFa = "نگهداشتن ایزومتریک بدن در حالت قایقی با چسباندن کمر به زمین.",
            musclesEn = listOf("Rectus Abdominis", "Transverse Abdominis", "Hip Flexors"),
            musclesFa = listOf("راست شکمی", "عمقی شکم", "لگن"),
            mode = ExerciseMode.TIMED_HOLD,
            view = RecommendedCameraView.SIDE,
            landmarks = fullBodyLandmarks,
            analyzerId = null,
            status = ImplementationStatus.COMING_SOON,
            instructionsEn = listOf(
                "Press lower back firmly into the floor.",
                "Lift shoulder blades and straight legs slightly off the mat.",
                "Hold steady without letting the lower back arch."
            ),
            instructionsFa = listOf(
                "گودی کمر را کاملاً به زمین بچسبانید.",
                "شانه‌ها و پاهای صاف را کمی از زمین بلند کرده و نگه دارید.",
                "بدون ایجاد قوس در کمر وضعیت را حفظ کنید."
            ),
            mistakesEn = listOf("Lower back lifting off floor"),
            mistakesFa = listOf("جدا شدن کمر از زمین"),
            checksEn = listOf("Planned: Supine hold timer"),
            checksFa = listOf("در دست ساخت: زمان‌سنج نگهداشتن حرکت")
        ),

        // ============================================================
        // CATEGORY D — RESISTANCE BAND TRAINING (15 Exercises)
        // ============================================================
        bandEntry(
            id = "band_biceps_curl",
            nameEn = "Resistance Band Biceps Curl",
            nameFa = "جلو بازو با کش",
            descEn = "Standing elbow curl anchoring the band under one or both feet.",
            descFa = "جلو بازو ایستاده با کش مقاومتی قرارگرفته زیر پاها.",
            musclesEn = listOf("Biceps Brachii", "Brachioradialis"),
            musclesFa = listOf("جلو بازو", "ساعد"),
            bandTypes = listOf(ResistanceBandType.LONG_LOOP, ResistanceBandType.TUBE_WITH_HANDLES, ResistanceBandType.FLAT_THERAPY),
            requiresAnchor = false,
            anchorEn = "Stand securely on the center of the band with both feet flat before curling. Inspect band for nicks before stretching.",
            anchorFa = "با هر دو پا محکم روی مرکز کش بایستید. پیش از کشش، سلامت ظاهری کش را بررسی کنید.",
            mode = ExerciseMode.REPETITIONS,
            view = RecommendedCameraView.SIDE,
            landmarks = upperBodyLandmarks,
            analyzerId = "band_biceps_curl",
            status = ImplementationStatus.IMPLEMENTED_NEEDS_VALIDATION,
            instructionsEn = listOf(
                "Step securely onto the band with feet hip-width apart.",
                "Keep elbows tucked near your sides and curl hands upward.",
                "Lower smoothly to full elbow extension."
            ),
            instructionsFa = listOf(
                "پاها را به عرض لگن روی وسط کش قرار دهید.",
                "آرنج‌ها را نزدیک پهلو ثابت نگه دارید و دست‌ها را بالا بیاورید.",
                "با کنترل تا باز شدن کامل آرنج پایین ببرید."
            ),
            mistakesEn = listOf("Pulling elbows forward", "Partial bottom extension", "Leaning back"),
            mistakesFa = listOf("جلو کشیدن آرنج", "باز نکردن کامل آرنج", "خم شدن به عقب"),
            checksEn = listOf("Elbow flexion/extension", "Repetition counting", "Upper-arm drift & partial rep checks"),
            checksFa = listOf("خم و باز شدن آرنج", "شمارش تکرار", "بررسی ثبات بازو و تکرار ناقص")
        ),
        bandEntry(
            id = "band_shoulder_press",
            nameEn = "Resistance Band Shoulder Press",
            nameFa = "پرس سرشانه با کش",
            descEn = "Overhead press standing on a long loop or tube band.",
            descFa = "پرس سرشانه ایستاده با کش حلقه‌ای بلند یا لوله‌ای.",
            musclesEn = listOf("Deltoids", "Triceps"),
            musclesFa = listOf("سرشانه", "پشت بازو"),
            bandTypes = listOf(ResistanceBandType.LONG_LOOP, ResistanceBandType.TUBE_WITH_HANDLES),
            requiresAnchor = false,
            anchorEn = "Ensure band sits flat beneath arches of feet so it cannot slip upward.",
            anchorFa = "اطمینان حاصل کنید کش زیر قوس کف پا کاملاً ثابت است و سر نمی‌خورد.",
            mode = ExerciseMode.REPETITIONS,
            view = RecommendedCameraView.FRONT,
            landmarks = upperBodyLandmarks,
            analyzerId = null,
            status = ImplementationStatus.COMING_SOON,
            instructionsEn = listOf(
                "Stand on band and bring handles or loop to shoulder level.",
                "Press overhead without flaring ribs.",
                "Lower back to shoulder height."
            ),
            instructionsFa = listOf(
                "روی کش بایستید و دسته‌ها را هم‌سطح شانه نگه دارید.",
                "بدون قوس کمر دست‌ها را به بالای سر پرس کنید.",
                "با کنترل به ارتفاع شانه برگردید."
            ),
            mistakesEn = listOf("Arching lower back", "Uneven pressing height"),
            mistakesFa = listOf("قوس کمر", "نامتقارن بودن دست‌ها"),
            checksEn = listOf("Planned: Overhead elbow extension & symmetry"),
            checksFa = listOf("در دست ساخت: تقارن و باز شدن آرنج")
        ),
        bandEntry(
            id = "band_lateral_raise",
            nameEn = "Resistance Band Lateral Raise",
            nameFa = "نشر جانب با کش",
            descEn = "Lateral shoulder abduction against band resistance.",
            descFa = "بالا آوردن دست‌ها از طرفین با مقاومت کش.",
            musclesEn = listOf("Lateral Deltoid"),
            musclesFa = listOf("سرشانه جانبی"),
            bandTypes = listOf(ResistanceBandType.TUBE_WITH_HANDLES, ResistanceBandType.FLAT_THERAPY, ResistanceBandType.LONG_LOOP),
            requiresAnchor = false,
            anchorEn = "Anchor band securely under one or both feet; check handle knots before lifting.",
            anchorFa = "کش را زیر یک یا هر دو پا ثابت کنید و از سالم بودن دستگیره‌ها مطمئن شوید.",
            mode = ExerciseMode.REPETITIONS,
            view = RecommendedCameraView.FRONT,
            landmarks = upperBodyLandmarks,
            analyzerId = null,
            status = ImplementationStatus.COMING_SOON,
            instructionsEn = listOf(
                "Stand on band with arms at sides.",
                "Raise arms out laterally to shoulder level.",
                "Lower slowly resisting recoil."
            ),
            instructionsFa = listOf(
                "روی کش بایستید و دست‌ها را کنار بدن نگه دارید.",
                "دست‌ها را تا سطح شانه از طرفین بالا بیاورید.",
                "با کنترل پایین بیاورید."
            ),
            mistakesEn = listOf("Shrugging neck/traps"),
            mistakesFa = listOf("بالا کشیدن شانه‌ها"),
            checksEn = listOf("Planned: Shoulder abduction angle"),
            checksFa = listOf("در دست ساخت: زاویه بالا آوردن دست")
        ),
        bandEntry(
            id = "band_row",
            nameEn = "Resistance Band Row",
            nameFa = "زیربغل قایقی با کش",
            descEn = "Horizontal rowing pull using a stationary anchor or seated foot wrap.",
            descFa = "کشش افقی کش با تکیه‌گاه ثابت یا در حالت نشسته.",
            musclesEn = listOf("Latissimus Dorsi", "Rhomboids", "Rear Deltoids"),
            musclesFa = listOf("زیربغل", "پشت شانه", "ذوزنقه"),
            bandTypes = listOf(ResistanceBandType.TUBE_WITH_HANDLES, ResistanceBandType.LONG_LOOP, ResistanceBandType.FLAT_THERAPY),
            requiresAnchor = true,
            anchorEn = "Attach band to a sturdy, immobile anchor at chest/navel height. Verify anchor strength before pulling; camera cannot verify anchor safety.",
            anchorFa = "کش را به یک تکیه‌گاه کاملاً محکم در ارتفاع سینه یا ناف ببندید. پیش از کشش از استحکام تکیه‌گاه مطمئن شوید.",
            mode = ExerciseMode.REPETITIONS,
            view = RecommendedCameraView.SIDE,
            landmarks = upperBodyLandmarks,
            analyzerId = null,
            status = ImplementationStatus.EXPERIMENTAL,
            instructionsEn = listOf(
                "Face your secure anchor and step back to take up slack.",
                "Pull elbows straight back beside ribs, squeezing shoulder blades.",
                "Return arms forward smoothly."
            ),
            instructionsFa = listOf(
                "رو به تکیه‌گاه محکم بایستید و کمی عقب بروید.",
                "آرنج‌ها را کنار پهلو به عقب بکشید و کتف‌ها را نزدیک کنید.",
                "دست‌ها را با کنترل به جلو برگردانید."
            ),
            mistakesEn = listOf("Leaning torso far backward", "Shrugging shoulders"),
            mistakesFa = listOf("خم شدن زیاد تنه به عقب", "بالا انداختن شانه‌ها"),
            checksEn = listOf("Experimental: Elbow pull range & torso sway"),
            checksFa = listOf("آزمایشی: دامنه کشش آرنج و تکان تنه")
        ),
        bandEntry(
            id = "band_chest_press",
            nameEn = "Resistance Band Chest Press",
            nameFa = "پرس سینه با کش",
            descEn = "Standing horizontal press away from a rear anchor.",
            descFa = "پرس سینه ایستاده با کش متصل به تکیه‌گاه پشت سر.",
            musclesEn = listOf("Pectorals", "Triceps", "Anterior Deltoids"),
            musclesFa = listOf("سینه", "پشت بازو", "شانه قدامی"),
            bandTypes = listOf(ResistanceBandType.TUBE_WITH_HANDLES, ResistanceBandType.LONG_LOOP),
            requiresAnchor = true,
            anchorEn = "Secure band behind you at chest height to a fixed, solid anchor before stepping into split stance.",
            anchorFa = "کش را پشت سر در ارتفاع سینه به تکیه‌گاه ثابت و مطمئن متصل کنید.",
            mode = ExerciseMode.REPETITIONS,
            view = RecommendedCameraView.SIDE,
            landmarks = upperBodyLandmarks,
            analyzerId = null,
            status = ImplementationStatus.COMING_SOON,
            instructionsEn = listOf(
                "Stand facing away from anchor in a staggered stance.",
                "Press handles forward to straight arms at chest height.",
                "Return elbows smoothly to torso line."
            ),
            instructionsFa = listOf(
                "پشت به تکیه‌گاه با یک پا جلوتر بایستید.",
                "دسته‌ها را در سطح سینه به جلو پرس کنید.",
                "با کنترل آرنج‌ها را به عقب برگردانید."
            ),
            mistakesEn = listOf("Flaring shoulders forward"),
            mistakesFa = listOf("جلو افتادن شانه‌ها"),
            checksEn = listOf("Planned: Elbow press extension"),
            checksFa = listOf("در دست ساخت: باز شدن آرنج در پرس")
        ),
        bandEntry(
            id = "band_triceps_extension",
            nameEn = "Resistance Band Triceps Extension",
            nameFa = "پشت بازو با کش",
            descEn = "High-anchor pushdown or overhead triceps extension.",
            descFa = "کشش پشت بازو با کش از تکیه‌گاه بالا یا بالای سر.",
            musclesEn = listOf("Triceps Brachii"),
            musclesFa = listOf("پشت بازو"),
            bandTypes = listOf(ResistanceBandType.TUBE_WITH_HANDLES, ResistanceBandType.LONG_LOOP, ResistanceBandType.FLAT_THERAPY),
            requiresAnchor = true,
            anchorEn = "When using a high door/frame anchor, confirm the door closes toward the pull or anchor is mechanically locked.",
            anchorFa = "در صورت استفاده از تکیه‌گاه بالا، از قفل بودن کامل و استحکام آن اطمینان حاصل کنید.",
            mode = ExerciseMode.REPETITIONS,
            view = RecommendedCameraView.SIDE,
            landmarks = upperBodyLandmarks,
            analyzerId = null,
            status = ImplementationStatus.COMING_SOON,
            instructionsEn = listOf(
                "Pin upper arms near your sides.",
                "Extend forearms downward until elbows straighten.",
                "Return smoothly without lifting elbows."
            ),
            instructionsFa = listOf(
                "بازوها را در کنار تنه ثابت نگه دارید.",
                "ساعدها را تا صاف شدن کامل آرنج به پایین فشار دهید.",
                "با کنترل بالا برگردید."
            ),
            mistakesEn = listOf("Swinging upper arms"),
            mistakesFa = listOf("تکان دادن بازوها"),
            checksEn = listOf("Planned: Elbow extension & upper-arm stability"),
            checksFa = listOf("در دست ساخت: باز شدن آرنج و ثبات بازو")
        ),
        bandEntry(
            id = "band_squat",
            nameEn = "Resistance Band Squat",
            nameFa = "اسکوات با کش",
            descEn = "Squat standing on a long loop band looped over shoulders or held at chest.",
            descFa = "اسکوات با کش مقاومتی حلقه‌ای زیر پا و روی شانه یا مقابل سینه.",
            musclesEn = listOf("Quadriceps", "Glutes"),
            musclesFa = listOf("چهارسر ران", "باسن"),
            bandTypes = listOf(ResistanceBandType.LONG_LOOP, ResistanceBandType.MINI_LOOP),
            requiresAnchor = false,
            anchorEn = "Stand centered on the band; inspect loop for wear before placing over shoulders.",
            anchorFa = "مرکز کش را زیر پاها ثابت کنید و قبل از قرار دادن روی شانه، سلامت کش را بررسی کنید.",
            mode = ExerciseMode.REPETITIONS,
            view = RecommendedCameraView.SIDE,
            landmarks = fullBodyLandmarks,
            analyzerId = null,
            status = ImplementationStatus.EXPERIMENTAL,
            instructionsEn = listOf(
                "Stand on band with feet shoulder-width apart.",
                "Sit hips down into a controlled squat.",
                "Drive smoothly up against band tension."
            ),
            instructionsFa = listOf(
                "پاها را به عرض شانه روی کش بگذارید.",
                "لگن را در حرکت اسکوات کنترل‌شده پایین ببرید.",
                "با کنترل به حالت ایستاده برگردید."
            ),
            mistakesEn = listOf("Knees caving inward"),
            mistakesFa = listOf("جمع شدن زانوها به داخل"),
            checksEn = listOf("Experimental: Squat knee/hip depth"),
            checksFa = listOf("آزمایشی: عمق اسکوات")
        ),
        bandEntry(
            id = "band_good_morning",
            nameEn = "Resistance Band Good Morning",
            nameFa = "گود مورنینگ با کش",
            descEn = "Hip-hinge exercise with a long loop band under feet and behind neck/shoulders.",
            descFa = "حرکت لولای لگن با کش حلقه‌ای بلند برای تقویت پشت ران و کمر.",
            musclesEn = listOf("Hamstrings", "Glutes", "Lower Back"),
            musclesFa = listOf("پشت ران", "باسن", "کمر"),
            bandTypes = listOf(ResistanceBandType.LONG_LOOP),
            requiresAnchor = false,
            anchorEn = "Position band across upper traps (not cervical spine) and stand evenly on bottom loop.",
            anchorFa = "کش را روی عضلات ذوزنقه (نه مهره‌های گردن) قرار دهید و با هر دو پا روی کش بایستید.",
            mode = ExerciseMode.REPETITIONS,
            view = RecommendedCameraView.SIDE,
            landmarks = fullBodyLandmarks,
            analyzerId = null,
            status = ImplementationStatus.COMING_SOON,
            instructionsEn = listOf(
                "Stand on band with slight knee bend.",
                "Hinge hips backward with flat back.",
                "Drive hips forward to upright."
            ),
            instructionsFa = listOf(
                "با زانوهای کمی خم روی کش بایستید.",
                "لگن را با کمر صاف به عقب بدهید.",
                "به حالت ایستاده برگردید."
            ),
            mistakesEn = listOf("Rounding spine"),
            mistakesFa = listOf("گرد کردن ستون فقرات"),
            checksEn = listOf("Planned: Hip hinge angle"),
            checksFa = listOf("در دست ساخت: زاویه خم شدن لگن")
        ),
        bandEntry(
            id = "band_pull_apart",
            nameEn = "Resistance Band Pull-Apart",
            nameFa = "باز کردن کش مقابل سینه",
            descEn = "Horizontal shoulder abduction pulling band across chest.",
            descFa = "کشیدن و باز کردن کش به طرفین در مقابل سینه برای تقویت پشت شانه.",
            musclesEn = listOf("Rear Deltoids", "Rhomboids", "Mid Traps"),
            musclesFa = listOf("پشت شانه", "ذوزنقه میانی"),
            bandTypes = listOf(ResistanceBandType.FLAT_THERAPY, ResistanceBandType.LONG_LOOP),
            requiresAnchor = false,
            anchorEn = "Keep wrists neutral and inspect flat/loop band edges before pulling near face/chest.",
            anchorFa = "مچ‌ها را صاف نگه دارید و پیش از کشش، لبه‌های کش را از نظر پارگی بررسی کنید.",
            mode = ExerciseMode.REPETITIONS,
            view = RecommendedCameraView.FRONT,
            landmarks = upperBodyLandmarks,
            analyzerId = null,
            status = ImplementationStatus.COMING_SOON,
            instructionsEn = listOf(
                "Hold band at chest height with straight arms.",
                "Pull hands apart smoothly until band touches chest.",
                "Return with control."
            ),
            instructionsFa = listOf(
                "کش را با دست‌های صاف مقابل سینه بگیرید.",
                "دست‌ها را به طرفین باز کنید تا کش به سینه برسد.",
                "با کنترل به جلو برگردید."
            ),
            mistakesEn = listOf("Shrugging shoulders", "Bending elbows excessively"),
            mistakesFa = listOf("بالا بردن شانه‌ها", "خم کردن زیاد آرنج"),
            checksEn = listOf("Planned: Wrist spread symmetry"),
            checksFa = listOf("در دست ساخت: تقارن باز شدن دست‌ها")
        ),
        bandEntry(
            id = "mini_band_lateral_walk",
            nameEn = "Mini Band Lateral Walk",
            nameFa = "راه رفتن جانبی با مینی لوپ",
            descEn = "Side-stepping in an athletic half-squat with a mini loop above knees or ankles.",
            descFa = "گام‌برداری جانبی در وضعیت نیمه‌اسکوات با کش مینی لوپ.",
            musclesEn = listOf("Gluteus Medius", "Hip Abductors"),
            musclesFa = listOf("باسن میانی", "دورکننده‌های ران"),
            bandTypes = listOf(ResistanceBandType.MINI_LOOP),
            requiresAnchor = false,
            anchorEn = "Place mini loop smoothly above knees or around ankles without rolling.",
            anchorFa = "کش مینی لوپ را صاف بالای زانو یا مچ پا قرار دهید تا پیچ نخورد.",
            mode = ExerciseMode.ALTERNATING_REPETITIONS,
            view = RecommendedCameraView.FRONT,
            landmarks = fullBodyLandmarks,
            analyzerId = null,
            status = ImplementationStatus.COMING_SOON,
            instructionsEn = listOf(
                "Sink into a shallow athletic stance.",
                "Step sideways leading with heel, keeping tension on band.",
                "Follow with trailing foot without letting knees cave in."
            ),
            instructionsFa = listOf(
                "در وضعیت نیمه‌اسکوات قرار بگیرید.",
                "گام کنترل‌شده به پهلو بردارید و کشش کش را حفظ کنید.",
                "بدون جمع شدن زانوها به سمت داخل حرکت را ادامه دهید."
            ),
            mistakesEn = listOf("Standing completely upright", "Dragging trailing foot"),
            mistakesFa = listOf("صاف ایستادن کامل", "کشیدن پا روی زمین"),
            checksEn = listOf("Planned: Step width & knee alignment"),
            checksFa = listOf("در دست ساخت: عرض گام و راستای زانو")
        ),
        bandEntry(
            id = "band_glute_kickback",
            nameEn = "Resistance Band Glute Kickback",
            nameFa = "کیک‌بک باسن با کش",
            descEn = "Standing or quadruped hip extension against a mini loop or anchored band.",
            descFa = "باز کردن پا به عقب با کش مقاومتی برای تقویت عضلات باسن.",
            musclesEn = listOf("Gluteus Maximus", "Hamstrings"),
            musclesFa = listOf("باسن", "پشت ران"),
            bandTypes = listOf(ResistanceBandType.MINI_LOOP, ResistanceBandType.TUBE_WITH_HANDLES),
            requiresAnchor = false,
            anchorEn = "Hold a stable wall/chair for balance if standing; verify band loop around ankles.",
            anchorFa = "در حالت ایستاده برای تعادل دست را به دیوار تکیه دهید و از ثبات کش دور مچ مطمئن شوید.",
            mode = ExerciseMode.ALTERNATING_REPETITIONS,
            view = RecommendedCameraView.SIDE,
            landmarks = fullBodyLandmarks,
            analyzerId = null,
            status = ImplementationStatus.COMING_SOON,
            instructionsEn = listOf(
                "Keep torso steady and core lightly braced.",
                "Extend one leg straight back squeezing glute.",
                "Return smoothly without arching lower back."
            ),
            instructionsFa = listOf(
                "تنه را ثابت و شکم را کمی منقبض نگه دارید.",
                "یک پا را با انقباض باسن به عقب بکشید.",
                "بدون قوس کمر پا را برگردانید."
            ),
            mistakesEn = listOf("Swinging lower back"),
            mistakesFa = listOf("قوس دادن کمر"),
            checksEn = listOf("Planned: Hip extension angle"),
            checksFa = listOf("در دست ساخت: زاویه باز شدن ران")
        ),
        bandEntry(
            id = "mini_band_clamshell",
            nameEn = "Mini Band Clamshell",
            nameFa = "کلمشل با مینی لوپ",
            descEn = "Side-lying external hip rotation against a mini loop above the knees.",
            descFa = "باز کردن زانوی بالایی در حالت درازکش به پهلو با کش مینی لوپ.",
            musclesEn = listOf("Gluteus Medius", "External Hip Rotators"),
            musclesFa = listOf("باسن میانی", "چرخاننده‌های خارجی لگن"),
            bandTypes = listOf(ResistanceBandType.MINI_LOOP),
            requiresAnchor = false,
            anchorEn = "Position mini loop flat just above the knees.",
            anchorFa = "کش مینی لوپ را صاف دقیقاً بالای زانوها ببندید.",
            mode = ExerciseMode.REPETITIONS,
            view = RecommendedCameraView.FRONT,
            landmarks = fullBodyLandmarks,
            analyzerId = null,
            status = ImplementationStatus.COMING_SOON,
            instructionsEn = listOf(
                "Lie on your side with hips stacked and knees bent 45°.",
                "Keep heels together and lift top knee against band.",
                "Lower slowly without rolling hips backward."
            ),
            instructionsFa = listOf(
                "به پهلو بخوابید و زانوها را ۴۵ درجه خم کنید.",
                "پاشنه‌ها را چسبیده نگه دارید و زانوی بالایی را باز کنید.",
                "بدون چرخاندن لگن به عقب، زانو را پایین بیاورید."
            ),
            mistakesEn = listOf("Rolling pelvis backward"),
            mistakesFa = listOf("چرخاندن لگن به عقب"),
            checksEn = listOf("Planned: Knee separation angle"),
            checksFa = listOf("در دست ساخت: زاویه باز شدن زانو")
        ),
        bandEntry(
            id = "band_face_pull",
            nameEn = "Resistance Band Face Pull",
            nameFa = "فیس پول با کش",
            descEn = "Upper-back and external rotation pull toward forehead level.",
            descFa = "کشیدن کش به سمت صورت برای تقویت پشت شانه و کتف.",
            musclesEn = listOf("Rear Deltoids", "Rotator Cuff", "Mid Traps"),
            musclesFa = listOf("پشت شانه", "عضلات چرخاننده شانه", "ذوزنقه"),
            bandTypes = listOf(ResistanceBandType.LONG_LOOP, ResistanceBandType.TUBE_WITH_HANDLES),
            requiresAnchor = true,
            anchorEn = "Attach band securely at face/upper-chest height. Never pull toward your face unless anchor integrity is verified.",
            anchorFa = "کش را در ارتفاع صورت به تکیه‌گاه کاملاً مطمئن ببندید. پیش از کشیدن به سمت صورت، از استحکام تکیه‌گاه مطمئن شوید.",
            mode = ExerciseMode.REPETITIONS,
            view = RecommendedCameraView.SIDE,
            landmarks = upperBodyLandmarks,
            analyzerId = null,
            status = ImplementationStatus.COMING_SOON,
            instructionsEn = listOf(
                "Hold band with thumbs pointing backward.",
                "Pull hands toward temples while keeping elbows high.",
                "Extend arms forward smoothly."
            ),
            instructionsFa = listOf(
                "کش را طوری بگیرید که شست‌ها رو به عقب باشد.",
                "دست‌ها را با آرنج بالا به سمت شقیقه‌ها بکشید.",
                "با کنترل دست‌ها را به جلو برگردانید."
            ),
            mistakesEn = listOf("Dropping elbows below wrists"),
            mistakesFa = listOf("پایین افتادن آرنج‌ها"),
            checksEn = listOf("Planned: Elbow elevation & pull range"),
            checksFa = listOf("در دست ساخت: ارتفاع آرنج و دامنه کشش")
        ),
        bandEntry(
            id = "band_pallof_press",
            nameEn = "Resistance Band Pallof Press",
            nameFa = "پالوف پرس با کش",
            descEn = "Anti-rotation core press resisting lateral band pull.",
            descFa = "پرس ضدچرخش مرکز بدن در برابر کشش جانبی کش.",
            musclesEn = listOf("Obliques", "Transverse Abdominis"),
            musclesFa = listOf("مورب شکمی", "عضلات عمقی شکم"),
            bandTypes = listOf(ResistanceBandType.LONG_LOOP, ResistanceBandType.TUBE_WITH_HANDLES),
            requiresAnchor = true,
            anchorEn = "Secure band sideways to a sturdy stationary anchor at chest height.",
            anchorFa = "کش را از پهلو در ارتفاع سینه به یک تکیه‌گاه ثابت و محکم متصل کنید.",
            mode = ExerciseMode.REPETITIONS,
            view = RecommendedCameraView.FRONT,
            landmarks = upperBodyLandmarks,
            analyzerId = null,
            status = ImplementationStatus.COMING_SOON,
            instructionsEn = listOf(
                "Stand sideways to anchor holding band at center of chest.",
                "Press hands straight out in front without letting torso twist.",
                "Pause briefly and return to chest."
            ),
            instructionsFa = listOf(
                "به پهلو کنار تکیه‌گاه بایستید و کش را مقابل مرکز سینه نگه دارید.",
                "بدون چرخاندن تنه، دست‌ها را صاف به جلو پرس کنید.",
                "مکث کوتاه کرده و به سینه برگردید."
            ),
            mistakesEn = listOf("Rotating shoulders toward anchor"),
            mistakesFa = listOf("چرخش شانه‌ها به سمت کش"),
            checksEn = listOf("Planned: Shoulder symmetry & anti-rotation alignment"),
            checksFa = listOf("در دست ساخت: ثبات شانه و عدم چرخش")
        ),
        bandEntry(
            id = "band_dead_bug",
            nameEn = "Resistance Band Dead Bug",
            nameFa = "ددباگ با کش",
            descEn = "Dead bug variation using band tension overhead or across feet.",
            descFa = "ددباگ با ایجاد تنش کش بین دست‌ها و پاها یا تکیه‌گاه ثابت.",
            musclesEn = listOf("Deep Core", "Hip Flexors", "Lats"),
            musclesFa = listOf("مرکز بدن", "زیربغل", "لگن"),
            bandTypes = listOf(ResistanceBandType.MINI_LOOP, ResistanceBandType.LONG_LOOP),
            requiresAnchor = false,
            anchorEn = "When looping band around feet, ensure soles keep the band centered.",
            anchorFa = "در صورت قرار دادن کش دور کف پا، مطمئن شوید کش در مرکز کف پا ثابت است.",
            mode = ExerciseMode.ALTERNATING_REPETITIONS,
            view = RecommendedCameraView.SIDE,
            landmarks = fullBodyLandmarks,
            analyzerId = null,
            status = ImplementationStatus.COMING_SOON,
            instructionsEn = listOf(
                "Lie supine with band tension held steadily.",
                "Alternate extending one leg slowly while keeping low back flat.",
                "Return with control."
            ),
            instructionsFa = listOf(
                "به پشت بخوابید و کشش کش را ثابت نگه دارید.",
                "در حالی که کمر به زمین چسبیده، یک پا را به‌آرامی صاف کنید.",
                "با کنترل برگردانید و پای مقابل را حرکت دهید."
            ),
            mistakesEn = listOf("Lifting lower back off floor"),
            mistakesFa = listOf("جدا شدن گودی کمر از زمین"),
            checksEn = listOf("Planned: Alternating leg extension"),
            checksFa = listOf("در دست ساخت: حرکت متناوب پاها")
        )
    )

    private val builtInById by lazy { builtInExercises.associateBy { it.id } }

    val allExercises: List<ExerciseDefinition>
        get() = builtInExercises + ExternalExerciseSources.exercises

    fun findById(id: String): ExerciseDefinition? = builtInById[id]
        ?: ExternalExerciseSources.entry(id)?.definition

    fun byCategory(category: ExerciseCategory): List<ExerciseDefinition> =
        allExercises.filter { it.category == category }
}
